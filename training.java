package com.training;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.teamcenter.clientx.AppXSession;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.loose.core._2006_03.FileManagement.DatasetFileInfo;
import com.teamcenter.services.loose.core._2006_03.FileManagement.GetDatasetWriteTicketsInputData;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core.SessionService;
import com.teamcenter.services.strong.core._2006_03.DataManagement.CreateDatasetsOutput;
import com.teamcenter.services.strong.core._2006_03.DataManagement.CreateDatasetsResponse;
import com.teamcenter.services.strong.core._2006_03.DataManagement.CreateItemsOutput;
import com.teamcenter.services.strong.core._2006_03.DataManagement.CreateItemsResponse;
import com.teamcenter.services.strong.core._2006_03.DataManagement.ItemProperties;
import com.teamcenter.services.strong.core._2007_01.DataManagement.VecStruct;
import com.teamcenter.services.strong.core._2008_06.DataManagement.DatasetProperties2;
import com.teamcenter.services.strong.core._2008_06.DataManagement.GetNextIdsResponse;
import com.teamcenter.services.strong.core._2008_06.DataManagement.InfoForNextId;
import com.teamcenter.services.strong.query.SavedQueryService;
import com.teamcenter.services.strong.query._2006_03.SavedQuery.GetSavedQueriesResponse;
import com.teamcenter.services.strong.query._2006_03.SavedQuery.SavedQueryObject;
import com.teamcenter.services.strong.query._2007_06.SavedQuery.ExecuteSavedQueriesResponse;
import com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryInput;
import com.teamcenter.services.strong.query._2007_06.SavedQuery.SavedQueryResults;
import com.teamcenter.soa.client.FileManagementUtility;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.ServiceData;
import com.teamcenter.soa.client.model.strong.Item;
import com.teamcenter.soa.client.model.strong.SavedQueryCriteria;
import com.teamcenter.soa.common.ObjectPropertyPolicy;
import com.teamcenter.soa.exceptions.NotLoadedException;

public class training {

	public static void main(String[] args) throws ServiceException {
		// TODO Auto-generated method stub
		
		AppXSession apxsession = new AppXSession("url");
		apxsession.login();
		
		DataManagementService drvSrv = DataManagementService.getService(AppXSession.getConnection());
		InfoForNextId[] nextIds = new InfoForNextId[1];
		nextIds[0] = new InfoForNextId();
		nextIds[0].typeName="Item";
		GetNextIdsResponse resp = drvSrv.getNextIds(nextIds);
		ServiceData srvData = resp.serviceData;
		for (int i = 0; i < srvData.sizeOfPartialErrors(); i++) {
			srvData.getPartialError(i).getMessages();
		}
		String[] ids = resp.nextIds;
		
		
		ItemProperties[] itemProp = new ItemProperties[1];
		itemProp[0]= new ItemProperties();
		itemProp[0].clientId = "1";
		itemProp[0].type = "Item";
		itemProp[0].itemId = ids[0];
		CreateItemsResponse rev = drvSrv.createItems(itemProp , null, "");
		CreateItemsOutput[] out = rev.output;
		Item item = out[0].item;
		
		ServiceData propResp = drvSrv.getProperties(new ModelObject[]{item}, new String[]{"project_id",""});
		try {
			String id = item.get_project_ids();
		} catch (NotLoadedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		SavedQueryService savedQurySrv = SavedQueryService.getService(AppXSession.getConnection());
		GetSavedQueriesResponse savedQueryResp = savedQurySrv.getSavedQueries();
		SavedQueryObject[] queryList = savedQueryResp.queries;
		SavedQueryObject requiredquryObj = null;
		for (int i = 0; i < queryList.length; i++) {
			if(queryList[i].name.equals("Item...")){
				requiredquryObj = queryList[i];
			}
		}
		SavedQueryInput[] input = new SavedQueryInput[1];
		input[0] = new SavedQueryInput();
		input[0].query = requiredquryObj.query;
		input[0].entries = new String[]{"Item ID"};
		input[0].values = new String[]{"12345"};
		ExecuteSavedQueriesResponse savedquesryResp = savedQurySrv.executeSavedQueries(input );
		SavedQueryResults[] arrayResp = savedquesryResp.arrayOfResults;
		ModelObject[] obj = arrayResp[0].objects;
		
		SessionService sessionSrv = SessionService.getService(AppXSession.getConnection());
		ObjectPropertyPolicy objPolicy = new ObjectPropertyPolicy();
		objPolicy.addType("Item", new String[]{"item_id","object_desc"});
		
		sessionSrv.setObjectPropertyPolicy(objPolicy);
		Map<String, VecStruct> map  = new HashMap<String, VecStruct>();
		VecStruct vect = new VecStruct();
		vect.stringVec= new String[]{"1223Test"};
		VecStruct vecttype = new VecStruct();
		vecttype.stringVec= new String[]{"123"};
		map.put("m2_type", vecttype);
		map.put("object_desc", vect);
		ServiceData respdm = drvSrv.setProperties(obj, map);
		respdm.getUpdatedObject(0);
		
		ServiceData deleteserv = drvSrv.deleteObjects(obj);
		deleteserv.getDeletedObject(0);
		File newFile = new File("tets.txt");
		DatasetProperties2[] inputDataset =new DatasetProperties2[1];
		inputDataset[0] = new DatasetProperties2();
		inputDataset[0].name = "training dataset";
		inputDataset[0].clientId= "01";
		inputDataset[0].container =obj[0];
		inputDataset[0].relationType = "iman_reference";
		CreateDatasetsResponse redsp = drvSrv.createDatasets2(inputDataset);
		
		CreateDatasetsOutput[] outDataset = redsp.output;
		FileManagementUtility fmu = new FileManagementUtility(AppXSession.getConnection());
		GetDatasetWriteTicketsInputData[] getDatasetWitreTicket = new GetDatasetWriteTicketsInputData[1];
		getDatasetWitreTicket[0] = new GetDatasetWriteTicketsInputData();
		getDatasetWitreTicket[0].dataset=outDataset[0].dataset;
		DatasetFileInfo[] filesInfo = new DatasetFileInfo[1];
		filesInfo[0] = new DatasetFileInfo();
		filesInfo[0].fileName=newFile.getAbsolutePath();
		getDatasetWitreTicket[0].datasetFileInfos = filesInfo ;
		fmu.putFiles(getDatasetWitreTicket );
		
		apxsession.logout();
		
	}

}
