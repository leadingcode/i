package org.igov.model.document;

import org.igov.io.GeneralConfig;
import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.impl.util.json.JSONArray;
import org.activiti.engine.impl.util.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.igov.io.web.HttpRequester;

public class DocumentContentTypeUtil {
	
	private static Map<String, String> documentContentTypesIdByName = new HashMap<String, String>();
	private static final Logger oLog = LoggerFactory.getLogger(DocumentContentTypeUtil.class);
	
	public static String getDocumentContentTypeIdByName(String typeName){
		synchronized (documentContentTypesIdByName){
			String cutTypeName = StringUtils.substringBefore(typeName, ";");
			if (!documentContentTypesIdByName.keySet().contains(cutTypeName)){
				oLog.info("document map doesn't contain value for the key " + cutTypeName);
				return "";
			} 
			return documentContentTypesIdByName.get(cutTypeName);
		}
	}

	public static void init(GeneralConfig generalConfig, HttpRequester httpRequester) {
		synchronized (documentContentTypesIdByName){
			if (documentContentTypesIdByName.isEmpty()){
				String URI = "/wf/service/services/getDocumentContentTypes";
				oLog.info("Getting URL: " + generalConfig.sHostCentral() + URI);
				try {
					String soJSON_DocumentTypes = httpRequester.get(generalConfig.sHostCentral() + URI, new HashMap<String, String>());
			        oLog.info("Received answer: " + soJSON_DocumentTypes);
			        
			        JSONArray jsonArray = new JSONArray(soJSON_DocumentTypes);
			        for (int i = 0; i < jsonArray.length(); i++) {
			            JSONObject record = jsonArray.getJSONObject(i);
			            documentContentTypesIdByName.put(record.getString("sName"), record.getString("nID"));
			        }
			        oLog.info("Loaded map: " + documentContentTypesIdByName);
				} catch (Exception e) {
					oLog.info("Error occured while loading list of document content types: " + e.getMessage(), e);
				}
			}
		}
            
	}
}
