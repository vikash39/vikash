#include <bom/bom.h>
#include <epm/epm.h>
#include <epm\epm_toolkit_tc_utils.h>
#include <tccore/aom.h>
#include <tccore/aom_prop.h>
#include <user_exits/epm_toolkit_utils.h>

static void get_all_target_revs(tag_t tTopLine, counted_tag_list_t *ptlNewTargets)
{
    int ifail = ITK_ok;
    int iNumChildren = 0;
    tag_t *ptChildren = NULL;
    ifail = BOM_line_ask_child_lines(tTopLine, &iNumChildren, &ptChildren); 
    if (ifail != ITK_ok) { /* your error logic here */ }

    for(int ii = 0; ii < iNumChildren; ii++)
    {
        tag_t tRev = NULLTAG;
        ifail = AOM_ask_value_tag(ptChildren[ii], "bl_revision", &tRev);
        if (ifail != ITK_ok) { /* your error logic here */ }
        
        ifail = EPM__add_to_tag_list(tRev, ptlNewTargets);
        if (ifail != ITK_ok) { /* your error logic here */ }
        
        /* call recursively */
        get_all_target_revs(ptChildren[ii], ptlNewTargets);
    }
    MEM_free(ptChildren);
}

extern int add_all_components_to_target_list(EPM_action_message_t msg)
{
    int ifail = ITK_ok;

    tag_t tRootTask = NULLTAG;
    ifail = EPM_ask_root_task(msg.task, &tRootTask); 

    int iNumAttachs = 0;
    tag_t *ptAttachs = NULL;
    ifail = EPM_ask_attachments(tRootTask, EPM_target_attachment, &iNumAttachs, &ptAttachs); 
    if (ifail != ITK_ok) { /* your error logic here */ }
    
    tag_t tTopRev = ptAttachs[0]; /* assuming just one */

    tag_t window = NULLTAG;
    ifail = BOM_create_window (&window);
    if (ifail != ITK_ok) { /* your error logic here */ }
    
    tag_t tTopLine = NULLTAG;
    ifail = BOM_set_window_top_line(window, NULLTAG, tTopRev, NULLTAG, &tTopLine); 
    if (ifail != ITK_ok) { /* your error logic here */ }
    
    counted_tag_list_t  tlNewTargets = {0};
    int initial_tag_list_size = 16;  // Reference PR-8968371
    tlNewTargets.list = (tag_t *)MEM_alloc(initial_tag_list_size * (sizeof(tag_t)));
    get_all_target_revs(tTopLine, &tlNewTargets);

    ifail = BOM_close_window(window);
    if (ifail != ITK_ok) { /* your error logic here */ }

    int *piAttachTypes = NULL;
    piAttachTypes = (int *) MEM_alloc (tlNewTargets.count * sizeof(int));
    for (int ii = 0; ii < tlNewTargets.count; ii++)
    {
        piAttachTypes[ii] = EPM_target_attachment;
    }

    ifail = AOM_refresh(tRootTask, TRUE); 
    if (ifail != ITK_ok) { /* your error logic here */ }
    
    ifail = EPM_add_attachments(tRootTask, tlNewTargets.count, tlNewTargets.list, piAttachTypes);
    if (ifail != ITK_ok) { /* your error logic here */ }
    
    if(piAttachTypes) MEM_free(piAttachTypes);  
    
    ifail = AOM_save(tRootTask);
    if (ifail != ITK_ok) { /* your error logic here */ }
    
    return ifail;
}