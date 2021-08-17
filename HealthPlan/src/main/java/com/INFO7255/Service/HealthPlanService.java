package com.INFO7255.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.INFO7255.HealthPlanApplication;
import com.INFO7255.Repository.HealthPlanRepository;

@Service
public class HealthPlanService {


	@Autowired
	private HealthPlanRepository healthPlanRepository;
	@Autowired
	private SchemaService schemaService;
	@Autowired
	private PublisherService publisherService;

	private String delimiter = "|";
	
	public Map<String, String> getAlLPlans(){
		Map<String, String> plansMap;
		plansMap =  healthPlanRepository.getAllPlans("PLANS");
		
		return plansMap;
	}

	public String savePlan(String objectId, String plan) {
		
		// return healthPlanRepository.setHashValue("PLANS", objectId, plan );
		JSONObject planObj = new JSONObject(plan);
		String etagVal = DigestUtils.md5Hex(plan);
		planObj.put("ETAG", etagVal);
		indexPlan(objectId, planObj);
		String planID = planObj.getString("objectType") + ":" + planObj.getString("objectId");
		try{
			System.out.println("published");
			publisherService.publishMessage(planID + delimiter + "save");
		} catch(Exception e){
			System.out.println("publish exception occured" + e.getMessage());
		}
		System.out.println("here4");
		System.out.println("etagval "+ etagVal);
		return etagVal;
	}
	
	public void indexPlan(String objectId, JSONObject planObj){
		// JSONObject planObj = new JSONObject(plan);
		String targetKey = planObj.getString("objectType") + ":" + planObj.getString("objectId");
		indexJSONObject(planObj, targetKey);
	}

	public void indexJSONObject(JSONObject planObj, String objectKey){
		for(String property: planObj.keySet()){
			if(planObj.get(property) instanceof JSONObject ){
				JSONObject targetObject = planObj.getJSONObject(property);
				String targetKey = targetObject.getString("objectType") + ":" + targetObject.getString("objectId");
				healthPlanRepository.setHashValue(objectKey, property, targetKey);
				indexJSONObject(targetObject, targetKey);
			} else if(planObj.get(property) instanceof JSONArray){
				JSONArray targetArray = planObj.getJSONArray(property);
				String targetKey = objectKey + "-" + property;
				healthPlanRepository.setHashValue(objectKey, property, targetKey);
				indexJSONArray(targetArray, targetKey);
			} else{
				healthPlanRepository.setHashValue(objectKey, property, planObj.get(property).toString());
			}
		}
	}

	public void indexJSONArray(JSONArray arr, String objectKey){
		healthPlanRepository.deleteHashKey(objectKey);
		System.out.println("arr val" + arr);
		for(int i=0; i<arr.length(); i++){
			if(arr.get(i) instanceof JSONArray){
				String targetKey = objectKey + "-" + i;
				healthPlanRepository.addToSet(objectKey, targetKey);
				indexJSONArray(arr.getJSONArray(i), targetKey);
			} else if(arr.get(i) instanceof JSONObject){
				JSONObject targetObject = arr.getJSONObject(i);
				String targetKey = targetObject.getString("objectType") + ":" + targetObject.getString("objectId");
				healthPlanRepository.addToSet(objectKey, targetKey);
				indexJSONObject(targetObject, targetKey);
			} else{
				healthPlanRepository.addToSet(objectKey, (String)arr.get(i));
			}
		}
	}

	public Boolean checkIfPlanIdExists(String objectId) {
		System.out.println("node exists?" + healthPlanRepository.checkIfHashNodeExists("plan:" + objectId));
		return healthPlanRepository.checkIfHashNodeExists("plan:" + objectId );
	}

	public JSONObject getPlan(String objectId) {
		// return healthPlanRepository.getPlan("PLANS", objectId);
		JSONObject schema = new JSONObject(schemaService.getSchema());
		System.out.println("schema json" + schema);
		JSONObject schemaDefinitionObject = schema.getJSONObject("$defs");
		return constructJSONObject("plan:" + objectId, schema, schemaDefinitionObject);
	}

	public JSONObject getUpdatedPlan(String objectId, JSONObject updateObj) throws Exception{
		JSONObject planObj = getPlan(objectId);
		mergeJSONObjects(planObj, updateObj);
		return planObj;
	}

	public void mergeJSONObjects(JSONObject planObj, JSONObject updateObj) throws Exception{
		String objectIdStr = "objectId";
		for(String updateKey: updateObj.keySet()){
			if(!planObj.has(updateKey)){
				planObj.put(updateKey, updateObj.get(updateKey));
			} else{
				if(updateObj.get(updateKey) instanceof JSONObject){
					if(!(planObj.get(updateKey) instanceof JSONObject)){
						throw new Exception("Type mismatch:" + updateKey +  "of plan is not of type Object");
					}	
					mergeJSONObjects(planObj.getJSONObject(updateKey), updateObj.getJSONObject(updateKey));
				} else if(updateObj.get(updateKey) instanceof JSONArray){
					if(!(planObj.get(updateKey) instanceof JSONArray)){
						throw new Exception("Type mismatch:" + updateKey +  "of plan is not of type Array");
					} 
					JSONArray storedArr = planObj.getJSONArray(updateKey);
					JSONArray updateArr = updateObj.getJSONArray(updateKey);
					if(storedArr.get(0) instanceof  JSONObject &&
					updateArr.get(0) instanceof JSONObject){
						mergeJSONArrays(storedArr, updateArr);
					} else{
						planObj.put(updateKey, updateArr);
					}
				} else{
					if(!updateKey.equals(objectIdStr)){
						System.out.println("keykey"+updateKey);
						planObj.put(updateKey, updateObj.get(updateKey));
					}
				}
			}
		}
	}

	public void mergeJSONArrays(JSONArray storedArr, JSONArray updateArr) throws Exception{
		String objectIdString = "objectId";
		for(int i=0; i<updateArr.length(); i++){
				JSONObject updateObj = updateArr.getJSONObject(i);
				String objectId = updateObj.getString(objectIdString);
				JSONObject storedObj = searchArrayForJSON(storedArr, objectId);
				if(storedObj != null){
					mergeJSONObjects(storedObj, updateObj);
				} else{
					storedArr.put(updateObj);
				}
		}
	}

	public JSONObject searchArrayForJSON(JSONArray storedArray ,String objectId){
		String objectIdString = "objectId";
		for(int i=0; i<storedArray.length(); i++){
			if(storedArray.get(i) instanceof JSONObject){
				if(storedArray.getJSONObject(i).getString(objectIdString).equals(objectId)){
					return storedArray.getJSONObject(i);
				}
			}
		}
		return null;
	}

	
	public JSONObject constructJSONObject(String key, JSONObject objSchema, JSONObject schemaDefinitionObject){
		JSONObject obj = new JSONObject();
		String typeKey = "type";
		String numberTypeKey = "number";
		String integerTypekey ="integer";
		JSONObject propertiesObject = objSchema.getJSONObject("properties");
		for(String property: propertiesObject.keySet()){
			JSONObject propertyObject;
			try{
				String type = propertiesObject.getJSONObject(property).getString("type");
				propertyObject = propertiesObject.getJSONObject(property);
			} catch(Exception e){
				System.out.println("prop obj"+propertiesObject.getJSONObject(property));
				String definitionRef = propertiesObject.getJSONObject(property).getString("$ref");
				propertyObject = schemaDefinitionObject.getJSONObject(definitionRef.split("/")[2]);
			}
			String type = propertyObject.getString(typeKey);
			if(type.equals("object")){
				String referenceKey = healthPlanRepository.getHashKey(key, property);
				obj.put(property, constructJSONObject(referenceKey, propertyObject, schemaDefinitionObject));
			} else if(type.equals("array")){
				String referenceKey = healthPlanRepository.getHashKey(key, property);
				obj.put(property, constructJSONArray(referenceKey, propertyObject, schemaDefinitionObject));
			} else{
				if(type.equals(numberTypeKey) | type.equals(integerTypekey)){
					obj.put(property, Double.parseDouble(healthPlanRepository.getHashKey(key, property)));
				} else{
					obj.put(property, healthPlanRepository.getHashKey(key, property));
				}
			}
		}

		return obj;

	}

	public JSONArray constructJSONArray(String key, JSONObject objSchema, JSONObject schemaDefinitionObject){
		JSONObject itemsObject;
		JSONArray arrObj = new JSONArray();
		String itemsKey = "items";
		String typeKey = "type";
		try{
			String type = objSchema.getJSONObject(itemsKey).getString("type");
			itemsObject = objSchema.getJSONObject(itemsKey);
		} catch(Exception e){
			String definitionRef = objSchema.getJSONObject(itemsKey).getString("$ref");
			itemsObject = schemaDefinitionObject.getJSONObject(definitionRef.split("/")[2]);
		}
		Set<String> setValues = healthPlanRepository.getAllKeysInSet(key);
		String type = itemsObject.getString(typeKey);
		for(String setVal: setValues){
			if(type.equals("array")){
				arrObj.put(constructJSONArray(setVal, itemsObject, schemaDefinitionObject));
			} else if(type.equals("object")){
				arrObj.put(constructJSONObject(setVal, itemsObject, schemaDefinitionObject));
			} else{
				arrObj.put(setVal);
			}
		}
		
		return arrObj;
}
	public String getEtag(String objectId) {
		System.out.println("foo" + getPlan(objectId));

		return healthPlanRepository.getHashKey("plan:" + objectId, "ETAG");
	}

	// public void deleteObject(String planObjectId) {
		
	// 	String planString = healthPlanRepository.getPlan("PLANS", planObjectId);
	// 	System.out.println("My path string" + planString);
	// 	JSONObject planObject = new JSONObject(planString);
	// 	jsonParsePath.deleteJSONObject(planObject, delObjectId);
	// 	System.out.println("My path list" + planObject);
	// 	savePlan(planObjectId, planObject.toString());
	// }

	public void deletePlan(String planObjectId) {
		String planReferenceKey = "plan:" + planObjectId;
		healthPlanRepository.deleteHashKey(planReferenceKey);
		try{
			publisherService.publishMessage(planReferenceKey + delimiter + "delete");
		} catch(Exception e){
			System.out.println("publish exception occured" + e.getMessage());
		}
	}
}
