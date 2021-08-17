package com.INFO7255.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

@Service
public class JSONParsePath {

//	 private List<String> pathList;
//	 private String json;
//
//	    public JSONParsePath(String json) {
//	        this.json = json;
////	        pathList = new ArrayList<>();
//	    }

	    public void deleteJSONObject(JSONObject json, String objectId) {
	    	List<String> ls =  new ArrayList<>();
	        setJsonPaths(json, ls, objectId);
	    }

	    private void setJsonPaths(JSONObject object, List<String> ls, String objectId) {
//	        this.pathList = new ArrayList<String>();
//	        JSONObject object = new JSONObject(json);
	        String jsonPath = "";
//	        if(json != JSONObject.NULL) {
	        readObject(object, jsonPath, ls, objectId);
//	        }   
	    }

	    private void readObject(JSONObject object, String jsonPath, List<String> ls, String objectId) {
	        Iterator<String> keysItr = object.keys();
	        String parentPath = jsonPath;
	        if(ls.size() > 0) {
	        	return;
	        }
	        while(keysItr.hasNext()) {
	            String key = keysItr.next();
	            Object value = object.get(key);
	            jsonPath = parentPath + "." + key;

	            if(value instanceof JSONArray) {            
	                readArray((JSONArray) value, jsonPath, ls, objectId);
	            }
	            else if(value instanceof JSONObject) {
	            	if(((JSONObject) value).getString("objectId").equals(objectId)) {
	                	ls.add(jsonPath);
	                	object.remove(key);
	                	break;
	                }
	                readObject((JSONObject) value, jsonPath, ls, objectId);
	            } else { // is a value
//	                this.pathList.add(jsonPath);    
//	                ls.add(jsonPath);
	            }          
	        }  
	    }

	    private void readArray(JSONArray array, String jsonPath, List<String> ls, String objectId) {      
	        String parentPath = jsonPath;
	        if(ls.size() > 0) {
	        	return;
	        }
	        for(int i = 0; i < array.length(); i++) {
	            Object value = array.get(i);        
	            jsonPath = parentPath + "[" + i + "]";

	            if(value instanceof JSONArray) {
	                readArray((JSONArray) value, jsonPath, ls, objectId);
	            } else if(value instanceof JSONObject) { 
	            	if(((JSONObject) value).getString("objectId").equals(objectId)) {
	                	ls.add(jsonPath);
	                	array.remove(i);
	                	break;
	                }
	                readObject((JSONObject) value, jsonPath, ls, objectId);
	                
	            } else { // is a value
//	                this.pathList.add(jsonPath);
//	                ls.add(jsonPath);

	            }       
	        }
	    }

}
