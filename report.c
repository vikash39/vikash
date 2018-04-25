/**********************************************************************************************************************************************************************
@	File					:	report.c												  
@	Purpose		            :	To Write log file after finishing workflow.
@	Description             :	It will get all workflow tasks from bomline and write the log in user --> Temp.
								
@	Created on              :	25.Sep.2017
@	Created by				:   Ravikiran P
@	Company					:	Intelizign Engineering Services.
/**********************************************************************************************************************************************************************/

/* This is for future reference. Please Sateesh don't delete*/

#include"ICOHeader.h"

#define ERROR_CALL(x) {if(x!=0) {iFail = EMH_ask_error_text(x,&sErrorString);printf("\nLINE  %d Error :%s",__LINE__,sErrorString);}}

int report(EPM_action_message_t msg)
{
	int		iFail					=	0,
			answer					=	0,
			attachmentsCount		=	0,
			bomViewCount			=	0,
			iCnt					=	0,
			iChildCnt				=	0,
			childCount				=	0,
			childOfChildCount		=	0,
			ichildOfChildCount		=	0,
			whereReferencedCount	=	0,
			*levels					=	(int*)0,
			isubProcessCount		=	0,
			subTaskCount			=	0,
			iSubtaskCount			=	0,
			subTaskOfTaskCount		=	0,
			bl_rev_attribute		=	0,
			signOff					=	0,
			signOffCnt				=	0,
			iWhereRefCount			=	0,
			numValues               =   0;

	char	*sErrorString			=	NULL,
			*date1					=	NULL,
			*date2					=	NULL,
			*ch						=	NULL,
			*ch1					=	NULL,
			*ObjName				=	NULL,
			*ObjType				=	NULL,
			*taskName				=	NULL,
			*dateReleased			=	NULL,
			**userName 				=	NULL,
			**roleName				=	NULL,
			**endDate				=	NULL,
			**decisionDate			=	NULL,
			**vehicleCreationDate	=	NULL,
			**specCreationDate		=	NULL,
			**relations				=	NULL;
	//const char* arr[] = { "fnd0Performer", "fnd0EndDate"};

	tag_t	
			*attachments			=	NULLTAG,
			*whereReferencedAssyTag	=	NULLTAG,
			*bvrs					=	NULLTAG,
			window					=	NULLTAG,
			rule					=	((tag_t)0),
			topLine					=	NULLTAG,
			*childTags				=	NULLTAG,
			*childOfChildTags		=	NULLTAG,
			*subTasks				=	NULLTAG,
			*subTasksOfTask			=	NULLTAG,
			revisionTag				=	NULLTAG,
			*signOffTags			=	NULLTAG,
			parent_process_tag		=	NULLTAG;

	
	FILE	*batchFile			=	NULL,
			*batchFile1			=	NULL;
	
	date_t  a_date1;
	date_t  a_date2;

	//EPM_state_t item				=	EPM_completed;


			date1  = (char*)MEM_alloc(sizeof(char)*32);
			date2  = (char*)MEM_alloc(sizeof(char)*32);

			fopen_s(&batchFile,"C:\\temp\\WF_Signoff_Report.log","w");
			fprintf(batchFile,"###############################################################################\n");

			fopen_s(&batchFile1,"C:\\temp\\WF_Signoff.log","w");
			fprintf(batchFile1,"User Name#Role#Completed Date \n",ObjName);

			// To get Root Task from Workflow.


			ERROR_CALL( iFail = EPM_ask_root_task(msg.task, &parent_process_tag));

			ERROR_CALL( iFail = EPM_ask_attachments(parent_process_tag, EPM_target_attachment, &attachmentsCount, &attachments));
			printf("\n Total Attachments: %d", attachmentsCount);

			for(isubProcessCount = 0;isubProcessCount<attachmentsCount;isubProcessCount++)
			{

			
				ERROR_CALL(iFail = AOM_ask_value_string(attachments[isubProcessCount],"object_type",&ObjType));
				printf("\n\nObjType :%s",ObjType);

				ERROR_CALL(iFail = AOM_ask_value_string(attachments[isubProcessCount],"object_name",&ObjName));
				printf("\n\nObjName :%s",ObjName);

				fprintf(batchFile,"Vehicle Name - %s \n",ObjName);

				if(tc_strcmp(ObjType,"RequirementSpec Revision") == 0)
				{
					ERROR_CALL(iFail = ITEM_rev_list_bom_view_revs(attachments[isubProcessCount], &bomViewCount,&bvrs ));

					ERROR_CALL(iFail = AOM_ask_displayable_values (attachments[isubProcessCount],"creation_date",&numValues,&vehicleCreationDate));
					printf("\n\n%s Creation Date :%s",ObjName,vehicleCreationDate[0]);

					for(iCnt=0;iCnt<bomViewCount;iCnt++)
					{
										
											
						ERROR_CALL(BOM_create_window(&window));

						ERROR_CALL(CFM_find("Latest Spec Released", &rule));

						ERROR_CALL(BOM_set_window_config_rule(window, rule));

						ERROR_CALL(BOM_set_window_top_line(window,NULLTAG,attachments[isubProcessCount],bvrs[iCnt],&topLine ));

						ERROR_CALL( BOM_line_ask_child_lines(topLine,&childCount,&childTags ));

						//fprintf(batchFile1,"Aggregate=%d \n",childCount);
						for(iChildCnt=0;iChildCnt<childCount;iChildCnt++)
						{
							ERROR_CALL(iFail = AOM_ask_value_string(childTags[iChildCnt],"bl_item_object_name",&ObjName));
							printf("\n\nObjName :%s",ObjName);

							fprintf(batchFile,"########################################################### \n");
							fprintf(batchFile,"Aggregate Name - %s \n",ObjName);
							fprintf(batchFile,"########################################################### \n");

							ERROR_CALL( BOM_line_ask_child_lines(childTags[iChildCnt],&childOfChildCount,&childOfChildTags ));

							/*ERROR_CALL(iFail = AOM_ask_value_string(childTags[iChildCnt],"bl_item_object_name",&ObjName));
							printf("\n\ObjName :%s",ObjName);*/
							//fprintf(batchFile1,"Spec=%d \n",childOfChildCount);
							for(ichildOfChildCount=0;ichildOfChildCount<childOfChildCount;ichildOfChildCount++)
							{
								
								ERROR_CALL(iFail = AOM_ask_value_string(childOfChildTags[ichildOfChildCount],"bl_item_object_name",&ObjName));
								printf("\n\nObjName :%s",ObjName);
								
								ERROR_CALL(BOM_line_look_up_attribute("bl_revision",&bl_rev_attribute));

								ERROR_CALL(BOM_line_ask_attribute_tag (childOfChildTags[ichildOfChildCount],bl_rev_attribute, &revisionTag));

								if(revisionTag==NULLTAG) continue;
								
								ERROR_CALL(iFail = AOM_ask_displayable_values (revisionTag,"creation_date",&numValues,&specCreationDate));
								printf("\n\n%s Creation Date :%s",ObjName,specCreationDate[0]);

								ch=tc_strtok ( vehicleCreationDate[0], " " ) ;
								tc_strcpy(date1,ch);
								printf("\nDate1 :%s",date1);

								ch1=tc_strtok ( specCreationDate[0], " " ) ;
								tc_strcpy(date2,ch1);
								printf("\nDate2 :%s",date2);


								ERROR_CALL(iFail = ITK_string_to_date  ( date1, &a_date1)) ;
								ERROR_CALL(iFail = ITK_string_to_date  ( date2, &a_date2)) ;

								ERROR_CALL(iFail = POM_compare_dates  ( a_date2, a_date1, &answer) );

								if(answer == -1) continue;

								fprintf(batchFile,"Spec Name - %s \n",ObjName);
								fprintf(batchFile,"########################################################### \n");
								fprintf(batchFile1,"########################################################### \n");

								ERROR_CALL(WSOM_where_referenced2  ( revisionTag,1, &whereReferencedCount,&levels, &whereReferencedAssyTag,&relations)); 

										
								for(iWhereRefCount = 0;iWhereRefCount<whereReferencedCount;iWhereRefCount++){

									ERROR_CALL(iFail = AOM_ask_value_string(whereReferencedAssyTag[iWhereRefCount],"object_type",&ObjType));
									printf("\n\nObjType :%s",ObjType);

									if(tc_strcmp(ObjType,"EPMTask") == 0){

										ERROR_CALL( iFail = AOM_ask_name (whereReferencedAssyTag[iWhereRefCount],&taskName));
										printf("\n Task Name: %s", taskName);

										//if(tc_strcmp(taskName,"CP_PSM_SPECIFICATION_RELEASE")== 0){
										if(tc_strcmp(taskName,"CP_SpecRelease_Sub_SubProcess")== 0){
											
											ERROR_CALL(iFail = AOM_ask_value_string(whereReferencedAssyTag[iWhereRefCount],"bl_rev_date_released",&dateReleased));
											printf("\n\nRevision Released Date :%s",dateReleased);
										
											ERROR_CALL(iFail = EPM_ask_sub_tasks(whereReferencedAssyTag[iWhereRefCount],&subTaskCount,&subTasks));
											printf("\n Total Subtask Count: %d", subTaskCount);

											fprintf(batchFile,"User Name#Role#Completed Date \n");

											for(iSubtaskCount = 0; iSubtaskCount < subTaskCount; iSubtaskCount++){

												ERROR_CALL( AOM_ask_name (subTasks[iSubtaskCount],&taskName));

												if(!tc_strcasecmp(taskName,"CDT Member")){
													
													ERROR_CALL(iFail = AOM_ask_displayable_values (subTasks[iSubtaskCount],"resp_party",&numValues,&userName ));
													printf("\n\n%s Responsible Party :%s",taskName,userName[0] );

												//	ERROR_CALL(iFail = AOM_ask_displayable_values (signOffTags[signOff],"fnd0AssigneeGroupRole",&numValues,&roleName));
												//	printf("\n\n%s Role :%s",taskName,roleName[0]);

													ERROR_CALL(iFail = AOM_ask_displayable_values (subTasks[iSubtaskCount],"fnd0EndDate",&numValues,&endDate));
													printf("\n\n%s Performer :%s",taskName,endDate[0]);


													fprintf(batchFile,"%s,%s,%s \n",userName[0],roleName[0],endDate[0]);
													fprintf(batchFile1,"%s#%s,%s,%s \n",taskName,userName[0],roleName[0],endDate[0]);
												} 
												else if(!tc_strcasecmp(taskName,"DH") || !tc_strcasecmp(taskName,"Project Head") || !tc_strcasecmp(taskName,"VA") || !tc_strcasecmp(taskName,"VA DH") || !tc_strcasecmp(taskName,"Vehicle Integration DH") || !tc_strcasecmp(taskName,"Product Planning And Marketing") || !tc_strcasecmp(taskName,"Quality")){

													ERROR_CALL(iFail = EPM_ask_sub_tasks(subTasks[iSubtaskCount],&subTaskOfTaskCount,&subTasksOfTask));
													printf("\n Total Subtask Count: %d", subTaskOfTaskCount);

																									
													ERROR_CALL( EPM_ask_attachments( subTasksOfTask[1], EPM_signoff_attachment, &signOffCnt, &signOffTags ) );

													for( signOff=0; signOff<signOffCnt; signOff++ )
													{
														ERROR_CALL(iFail = AOM_ask_displayable_values (signOffTags[signOff],"fnd0Performer",&numValues,&userName ));
														printf("\n\n%s Performer :%s",taskName,userName[0] );

														ERROR_CALL(iFail = AOM_ask_displayable_values (signOffTags[signOff],"fnd0AssigneeGroupRole",&numValues,&roleName));
														printf("\n\n%s Role :%s",taskName,roleName[0]);

														ERROR_CALL(iFail = AOM_ask_displayable_values (signOffTags[signOff],"decision_date",&numValues,&decisionDate));
														printf("\n\n%s Decision Date :%s",taskName,decisionDate[0]);

														
														fprintf(batchFile,"%s,%s,%s \n",userName[0],roleName[0],decisionDate[0]);
														fprintf(batchFile1,"%s#%s,%s,%s \n",taskName,userName[0],roleName[0],decisionDate[0]);

													}

													
												}
							
											}
											break;
										}
										
									}
									
					
								}

							}

						}
					
				ERROR_CALL(BOM_close_window(window));
			}
				
			break;
		}
	}
	
	fclose(batchFile);
	fclose(batchFile1);

	if(ObjType != NULL)							MEM_free(ObjType);
	if(ObjName != NULL)							MEM_free(ObjName);
	if(childTags != NULL)						MEM_free(childTags);
	if(bvrs != NULL)							MEM_free(bvrs);
	if(attachments != NULL)						MEM_free(attachments);
	if(taskName != NULL)						MEM_free(taskName);
	if(dateReleased != NULL)					MEM_free(dateReleased);
	if(subTasks != NULL)						MEM_free(subTasks);
	if(userName  != NULL)						MEM_free(userName);
	if(roleName != NULL)						MEM_free(roleName);
	if(decisionDate != NULL)					MEM_free(decisionDate);
	if(endDate != NULL)							MEM_free(endDate);
	if(signOffTags != NULL)						MEM_free(signOffTags);
	if(subTasksOfTask != NULL)					MEM_free(subTasksOfTask);
	if(levels != NULL)							MEM_free(levels);
	if(relations != NULL)						MEM_free(relations);
	if(whereReferencedAssyTag != NULL)			MEM_free(whereReferencedAssyTag);
	if(vehicleCreationDate != NULL)				MEM_free(vehicleCreationDate);
	if(specCreationDate != NULL)				MEM_free(specCreationDate);
	return ITK_ok;
}