//==================================================
//
//  Copyright 2012 Siemens Product Lifecycle Management Software Inc. All Rights Reserved.
//
//==================================================

package com.teamcenter.hello;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import com.teamcenter.clientx.AppXSession;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;

// Include the Data Management Service Interface
import com.teamcenter.services.strong.core.DataManagementService;

// Input and output structures for the service operations
// Note: the different namespace from the service interface
import com.teamcenter.services.strong.core._2006_03.DataManagement.CreateItemsOutput;
import com.teamcenter.services.strong.core._2006_03.DataManagement.CreateItemsResponse;
import com.teamcenter.services.strong.core._2006_03.DataManagement.ExtendedAttributes;
import com.teamcenter.services.strong.core._2006_03.DataManagement.GenerateItemIdsAndInitialRevisionIdsProperties;
import com.teamcenter.services.strong.core._2006_03.DataManagement.GenerateItemIdsAndInitialRevisionIdsResponse;
import com.teamcenter.services.strong.core._2006_03.DataManagement.GenerateRevisionIdsProperties;
import com.teamcenter.services.strong.core._2006_03.DataManagement.GenerateRevisionIdsResponse;
import com.teamcenter.services.strong.core._2006_03.DataManagement.ItemIdsAndInitialRevisionIds;
import com.teamcenter.services.strong.core._2006_03.DataManagement.ItemProperties;
import com.teamcenter.services.strong.core._2006_03.DataManagement.RevisionIds;
import com.teamcenter.services.strong.core._2007_01.DataManagement.CreateOrUpdateFormsResponse;
import com.teamcenter.services.strong.core._2007_01.DataManagement.FormAttributesInfo;
import com.teamcenter.services.strong.core._2007_01.DataManagement.FormInfo;
import com.teamcenter.services.strong.core._2007_01.DataManagement.GetItemCreationRelatedInfoResponse;
import com.teamcenter.services.strong.core._2008_06.DataManagement.ReviseInfo;
import com.teamcenter.services.strong.core._2008_06.DataManagement.ReviseResponse2;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.Property;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.Item;
import com.teamcenter.soa.client.model.strong.ItemRevision;
import com.teamcenter.soa.exceptions.NotLoadedException;

/**
 * Perform different operations in the DataManagementService
 *
 */
public class DataManagement
{

    /**
     * Perform a sequence of data management operations: Create Items, Revise
     * the Items, and Delete the Items
     *
     */
    public void createReviseAndDelete()
    {
        try
        {
            int numberOfItems = 3;

            // Reserve Item IDs and Create Items with those IDs
            ItemIdsAndInitialRevisionIds[] itemIds = generateItemIds(numberOfItems, "Item");
            CreateItemsOutput[] newItems = createItems(itemIds, "Item");

            // Copy the Item and ItemRevision to separate arrays for further
            // processing
            Item[] items = new Item[newItems.length];
            ItemRevision[] itemRevs = new ItemRevision[newItems.length];
            for (int i = 0; i < items.length; i++)
            {
                items[i] = newItems[i].item;
                itemRevs[i] = newItems[i].itemRev;
            }

            // Reserve revision IDs and revise the Items
            Map<BigInteger,RevisionIds> allRevIds = generateRevisionIds(items);
            reviseItems(allRevIds, itemRevs);

            // Delete all objects created
            deleteItems(items);
        }
        catch (ServiceException e)
        {
            System.out.println(e.getMessage());
        }

    }

    /**
     * Reserve a number Item and Revision IDs
     *
     * @param numberOfIds      Number of IDs to generate
     * @param type             Type of IDs to generate
     *
     * @return An array of Item and Revision IDs. The size of the array is equal
     *         to the input numberOfIds
     *
     * @throws ServiceException   If any partial errors are returned
     */
    public ItemIdsAndInitialRevisionIds[] generateItemIds(int numberOfIds, String type)
            throws ServiceException
    {
        // Get the service stub
        DataManagementService dmService = DataManagementService.getService(AppXSession.getConnection());

        GenerateItemIdsAndInitialRevisionIdsProperties[] properties = new GenerateItemIdsAndInitialRevisionIdsProperties[1];
        GenerateItemIdsAndInitialRevisionIdsProperties property = new GenerateItemIdsAndInitialRevisionIdsProperties();

        property.count = numberOfIds;
        property.itemType = type;
        property.item = null; // Not used
        properties[0] = property;

        // *****************************
        // Execute the service operation
        // *****************************
        GenerateItemIdsAndInitialRevisionIdsResponse response = dmService.generateItemIdsAndInitialRevisionIds(properties);



        // The AppXPartialErrorListener is logging the partial errors returned
        // In this simple example if any partial errors occur we will throw a
        // ServiceException
        if (response.serviceData.sizeOfPartialErrors() > 0)
            throw new ServiceException( "DataManagementService.generateItemIdsAndInitialRevisionIds returned a partial error.");

        // The return is a map of ItemIdsAndInitialRevisionIds keyed on the
        // 0-based index of requested IDs. Since we only asked for IDs for one
        // data type, the map key is '0'
        BigInteger bIkey = new BigInteger("0");

        @SuppressWarnings("unchecked")
        Map<BigInteger,ItemIdsAndInitialRevisionIds[]> allNewIds = response.outputItemIdsAndInitialRevisionIds;
        ItemIdsAndInitialRevisionIds[] myNewIds = allNewIds.get(bIkey);

        return myNewIds;
    }

    /**
     * Create Items
     *
     * @param itemIds        Array of Item and Revision IDs
     * @param itemType       Type of item to create
     *
     * @return Set of Items and ItemRevisions
     *
     * @throws ServiceException  If any partial errors are returned
     */
    @SuppressWarnings("unchecked")
    public CreateItemsOutput[] createItems(ItemIdsAndInitialRevisionIds[] itemIds, String itemType)
            throws ServiceException
    {
        // Get the service stub
        DataManagementService dmService = DataManagementService.getService(AppXSession.getConnection());
        // Populate form type
        GetItemCreationRelatedInfoResponse relatedResponse = dmService.getItemCreationRelatedInfo(itemType, null);
        String[] formTypes = new String[0];
        if ( relatedResponse.serviceData.sizeOfPartialErrors() > 0)
            throw new ServiceException( "DataManagementService.getItemCretionRelatedInfo returned a partial error.");

        formTypes = new String[relatedResponse.formAttrs.length];
        for ( int i = 0; i < relatedResponse.formAttrs.length; i++ )
        {
            FormAttributesInfo attrInfo = relatedResponse.formAttrs[i];
            formTypes[i] = attrInfo.formType;
        }

        ItemProperties[] itemProps = new ItemProperties[itemIds.length];
        for (int i = 0; i < itemIds.length; i++)
        {
            // Create form in cache for form property population
            ModelObject[] forms = createForms(itemIds[i].newItemId, formTypes[0],
                                              itemIds[i].newRevId, formTypes[1],
                                              null, false);
            ItemProperties itemProperty = new ItemProperties();

            itemProperty.clientId = "AppX-Test";
            itemProperty.itemId = itemIds[i].newItemId;
            itemProperty.revId = itemIds[i].newRevId;
            itemProperty.name = "AppX-Test";
            itemProperty.type = itemType;
            itemProperty.description = "Test Item for the SOA AppX sample application.";
            itemProperty.uom = "";

            // Retrieve one of form attribute value from Item master form.
            ServiceData serviceData = dmService.getProperties(forms, new String[]{"project_id"});
            if ( serviceData.sizeOfPartialErrors() > 0)
                throw new ServiceException( "DataManagementService.getProperties returned a partial error.");
            Property property = null;
            try
            {
                property= forms[0].getPropertyObject("project_id");
            }
            catch ( NotLoadedException ex){}


            // Only if value is null, we set new value
            if ( property == null || property.getStringValue() == null || property.getStringValue().length() == 0)
            {
                itemProperty.extendedAttributes = new ExtendedAttributes[1];
                ExtendedAttributes theExtendedAttr = new ExtendedAttributes();
                theExtendedAttr.attributes = new HashMap<String,String>();
                theExtendedAttr.objectType = formTypes[0];
                theExtendedAttr.attributes.put("project_id", "project_id");
                itemProperty.extendedAttributes[0] = theExtendedAttr;
            }
            itemProps[i] = itemProperty;
        }


        // *****************************
        // Execute the service operation
        // *****************************
        CreateItemsResponse response = dmService.createItems(itemProps, null, "");
        // before control is returned the ChangedHandler will be called with
        // newly created Item and ItemRevisions



        // The AppXPartialErrorListener is logging the partial errors returned
        // In this simple example if any partial errors occur we will throw a
        // ServiceException
        if (response.serviceData.sizeOfPartialErrors() > 0)
            throw new ServiceException( "DataManagementService.createItems returned a partial error.");

        return response.output;
    }

    /**
     * Reserve Revision IDs
     *
     * @param items       Array of Items to reserve IDs for
     *
     * @return Map of RevisionIds
     *
     * @throws ServiceException  If any partial errors are returned
     */
    @SuppressWarnings("unchecked")
    public Map<BigInteger,RevisionIds> generateRevisionIds(Item[] items) throws ServiceException
    {
        // Get the service stub
        DataManagementService dmService = DataManagementService.getService(AppXSession.getConnection());

        GenerateRevisionIdsResponse response = null;
        GenerateRevisionIdsProperties[] input = null;
        input = new GenerateRevisionIdsProperties[items.length];
        for (int i = 0; i < items.length; i++)
        {
            GenerateRevisionIdsProperties property = new GenerateRevisionIdsProperties();
            property.item = items[i];
            property.itemType = "";
            input[i] = property;
        }

        // *****************************
        // Execute the service operation
        // *****************************
        response = dmService.generateRevisionIds(input);

        // The AppXPartialErrorListener is logging the partial errors returned
        // In this simple example if any partial errors occur we will throw a
        // ServiceException
        if (response.serviceData.sizeOfPartialErrors() > 0)
            throw new ServiceException( "DataManagementService.generateRevisionIds returned a partial error.");

        return response.outputRevisionIds;
    }

    /**
     * Revise Items
     *
     * @param revisionIds     Map of Revision IDs
     * @param itemRevs        Array of ItemRevisons
     *
     * @return Map of Old ItemRevsion(key) to new ItemRevision(value)
     *
     * @throws ServiceException         If any partial errors are returned
     */
    public void reviseItems(Map<BigInteger,RevisionIds> revisionIds, ItemRevision[] itemRevs) throws ServiceException
    {
        // Get the service stub
        DataManagementService dmService = DataManagementService.getService(AppXSession.getConnection());
        ReviseInfo[] reviseInfo = new ReviseInfo[itemRevs.length];
         for (int i = 0; i < itemRevs.length; i++)
        {
            String key = Integer.toString(i);
            BigInteger bIkey = new BigInteger(key);
            RevisionIds rev = revisionIds.get(bIkey);

            reviseInfo[i] = new ReviseInfo();
            reviseInfo[i].baseItemRevision = itemRevs[i];
            reviseInfo[i].clientId         = itemRevs[i].getUid()+ "--" + i;
            reviseInfo[i].description      = "describe testRevise";
            reviseInfo[i].name             = "testRevise";
            reviseInfo[i].newRevId          = rev.newRevId;

        }

        // *****************************
        // Execute the service operation
        // *****************************
        ReviseResponse2 revised = dmService.revise2(reviseInfo);
        // before control is returned the ChangedHandler will be called with
        // newly created Item and ItemRevisions



        // The AppXPartialErrorListener is logging the partial errors returned
        // In this simple example if any partial errors occur we will throw a
        // ServiceException
        if (revised.serviceData.sizeOfPartialErrors() > 0)
            throw new ServiceException("DataManagementService.revise returned a partial error.");


    }

    /**
     * Delete the Items
     *
     * @param items     Array of Items to delete
     *
     * @throws ServiceException    If any partial errors are returned
     */
    public void deleteItems(Item[] items) throws ServiceException
    {
        // Get the service stub
        DataManagementService dmService = DataManagementService.getService(AppXSession.getConnection());

        // *****************************
        // Execute the service operation
        // *****************************
        ServiceData serviceData = dmService.deleteObjects(items);

        // The AppXPartialErrorListener is logging the partial errors returned
        // In this simple example if any partial errors occur we will throw a
        // ServiceException
        if (serviceData.sizeOfPartialErrors() > 0)
            throw new ServiceException("DataManagementService.deleteObjects returned a partial error.");

    }

    /**
     * Create ItemMasterForm and ItemRevisionMasterForm
     *
     * @param IMFormName      Name of ItemMasterForm
     * @param IMFormType      Type of ItemMasterForm
     * @param IRMFormName     Name of ItemRevisionMasterForm
     * @param IRMFormType     Type of ItemRevisionMasterForm
     * @param parent          The container object that two
     *                        newly-created forms will be added into.
     * @return ModelObject[]  Array of forms
     *
     * @throws ServiceException         If any partial errors are returned
     */
    public ModelObject[] createForms ( String IMFormName, String IMFormType,
                                String IRMFormName, String IRMFormType,
                                ModelObject parent, boolean saveDB ) throws ServiceException
    {
        //Get the service stub
        DataManagementService dmService = DataManagementService.getService(AppXSession.getConnection());
        FormInfo[] inputs = new FormInfo[2];
        inputs[0] = new FormInfo();
        inputs[0].clientId = "1";
        inputs[0].description="";
        inputs[0].name = IMFormName;
        inputs[0].formType=IMFormType;
        inputs[0].saveDB = saveDB;
        inputs[0].parentObject = parent ;
        inputs[1] = new FormInfo();
        inputs[1].clientId = "2";
        inputs[1].description="";
        inputs[1].name = IRMFormName;
        inputs[1].formType=IRMFormType;
        inputs[1].saveDB = saveDB;
        inputs[1].parentObject = parent;
        CreateOrUpdateFormsResponse response = dmService.createOrUpdateForms(inputs);
        if ( response.serviceData.sizeOfPartialErrors() > 0)
            throw new ServiceException("DataManagementService.createForms returned a partial error.");
        ModelObject[] forms = new ModelObject [inputs.length];
        for (int i=0; i<inputs.length; ++i)
        {
            forms[i] = response.outputs[i].form;
        }
        return forms;
    }

}
