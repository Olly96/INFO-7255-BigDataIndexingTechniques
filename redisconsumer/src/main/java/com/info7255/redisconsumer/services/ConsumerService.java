package com.info7255.redisconsumer.services;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;

import com.info7255.redisconsumer.RedisconsumerApplication;
import com.info7255.redisconsumer.repository.RedisRepository;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties.Jedis;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Component
public class ConsumerService {
    private Jedis jedis;
    @Autowired
    private RedisRepository redisRepository;
    // @Autowired
    // IndexerService indexerService;
    String listName = "IndexQueue";
    int counter = 0;
    private String delimiter = "\\|";

    // JSONObject typeMapping;
    // ConsumerService(){
    // typeMapping = new JSONObject();
    // typeMapping.put();
    // }
    @PostConstruct
    public void startup() {
        subscribeQueue();

    }

    @Scheduled(fixedRate=3000)
    public void subscribeQueue() {
        // while (true) {
            String message = redisRepository.popFromList(listName);
            System.out.println(message);
            if (message != null) {
                String[] arr = message.split(delimiter);
                String planId = arr[0];
                System.out.println("plan id " +  planId);
                if (arr[1].equals("delete")) {
                    deletePlan(planId);
                } else {
                    JSONObject schemaObj = new JSONObject(redisRepository.getKey("SCHEMA"));
                    JSONObject schemaDefinitionObject = schemaObj.getJSONObject("$defs");
                    // JSONObject plan = new JSONObject(redisRepository.getKey(planId));
                    JSONObject propertiesObject = schemaObj;
                    JSONObject metaObj = new JSONObject();
                    metaObj.put("parent", "");
                    metaObj.put("child", planId);
                    metaObj.put("type", "obj");
                    metaObj.put("propertiesObj", propertiesObject);
                    // System.out.println("meta obj" + metaObj);
                    indexPlan(metaObj, schemaDefinitionObject);
                }
            }
    }

    private void decouplePlanChildren(String planId){
        System.out.println("decouple called");
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JSONObject queryObj = new JSONObject("{\"query\":{\"has_parent\":{\"parent_type\":\"plan\",\"query\":{\"match\":{\"_id\":\"\"}}}}}");
        // System.out.println("queryobj" + queryObj);
        queryObj.getJSONObject("query").getJSONObject("has_parent").getJSONObject("query").getJSONObject("match").put("_id", planId);
        HttpEntity<String> request = new HttpEntity<String>(queryObj.toString(), headers);
        ResponseEntity<String> response = restTemplate.postForEntity("http://localhost:9200/plan/_search", request, String.class);       
        // System.out.println("resp" + new JSONObject(response.getBody()).getJSONObject("hits").getJSONArray("hits"));
        JSONArray hits = new JSONObject(response.getBody()).getJSONObject("hits").getJSONArray("hits");

        for(int i=0; i<hits.length();i++){
            // if(hits.length() > 0){
                JSONObject temp = hits.getJSONObject(i).getJSONObject("_source");
                temp.getJSONObject("my_join_field").remove("parent");
                System.out.println("put obj" + temp + " " + "http://localhost:9200/plan/_doc/" + hits.getJSONObject(i).getString("_id"));
                // saveDocument(temp, hits.getJSONObject(1).getString("_id"));
                // decouplePlanChildren(hits.getJSONObject(i).getString("_id"), hits.getJSONObject(i).getJSONObject("_source").getString(key));
                deletePlan(hits.getJSONObject(i).getString("_id"));
                saveDocument(temp, hits.getJSONObject(i).getString("_id"));
                // HttpEntity<JSONObject> requestEntity = new HttpEntity<JSONObject>(temp, headers);
                // HttpEntity<String> response2 = restTemplate.exchange("http://localhost:9200/plan/_doc/" + hits.getJSONObject(i).getString("_id"), HttpMethod.PUT, requestEntity, String.class);
                // System.out.println("response:" + response2);
            // }
            
        }

    }

    private void indexPlan(JSONObject metaObj, JSONObject schemaDefinitionObject) {
        JSONObject obj = new JSONObject();
        String typeKey = "type";
        String numberTypeKey = "number";
        String integerTypekey = "integer";
        String delimiter = "%";
        Queue<JSONObject> objQueue = new LinkedList<>();
        objQueue.add(metaObj);
        while (objQueue.size() != 0) {
            int size = objQueue.size();
            while (size != 0) {
                metaObj = objQueue.remove();
                if (metaObj.get("type") != "arr") {
                    JSONObject propertiesObject = metaObj.getJSONObject("propertiesObj").getJSONObject("properties");
                    for (String property : propertiesObject.keySet()) {
                        JSONObject propertyObject;
                        try {
                            String type = propertiesObject.getJSONObject(property).getString("type");
                            propertyObject = propertiesObject.getJSONObject(property);
                        } catch (Exception e) {
                            // System.out.println("prop obj" + propertiesObject.getJSONObject(property));
                            String definitionRef = propertiesObject.getJSONObject(property).getString("$ref");
                            propertyObject = schemaDefinitionObject.getJSONObject(definitionRef.split("/")[2]);
                        }
                        String type = propertyObject.getString(typeKey);
                        if (type.equals("object")) {
                            String referenceKey = redisRepository.getHashKey(metaObj.getString("child"), property);
                            JSONObject tempObj = new JSONObject();
                            tempObj.put("parent", metaObj.getString("child"));
                            tempObj.put("child", referenceKey);
                            tempObj.put("type", "obj");
                            tempObj.put("propertiesObj", propertyObject);
                            objQueue.add(tempObj);
                            // obj.put(property, indexPlan(referenceKey, propertyObject,
                            // schemaDefinitionObject));
                        } else if (type.equals("array")) {
                            String referenceKey = redisRepository.getHashKey(metaObj.getString("child"), property);
                            if (!propertyObject.getJSONObject("items").getString("type").equals("object")) {
                                obj.put(property, redisRepository.getAllKeysInSet(referenceKey));
                            } else {
                                JSONObject tempObj = new JSONObject();
                                tempObj.put("parent", metaObj.getString("child"));
                                tempObj.put("child", referenceKey);
                                tempObj.put("type", "arr");
                                tempObj.put("propertiesObj", propertyObject);
                                objQueue.add(tempObj);
                            }
                            // obj.put(property, indexJSONArray(referenceKey, propertyObject,
                            // schemaDefinitionObject));
                        } else {
                            if (type.equals(numberTypeKey) | type.equals(integerTypekey)) {
                                obj.put(property, Double
                                        .parseDouble(redisRepository.getHashKey(metaObj.getString("child"), property)));
                            } else {
                                obj.put(property, redisRepository.getHashKey(metaObj.getString("child"), property));
                            }
                        }
                    }
                    JSONObject tempJSON = new JSONObject();
                    String objType = "";
                    if (metaObj.getString("parent") != "") {
                        tempJSON.put("parent", metaObj.getString("parent"));
                        objType = redisRepository.getHashKey(metaObj.getString("parent"), "objectType") + "_"
                                + redisRepository.getHashKey(metaObj.getString("child"), "objectType");
                    } else {
                        objType = "" + redisRepository.getHashKey(metaObj.getString("child"), "objectType");
                    }
                    tempJSON.put("name", objType);
                    obj.put("my_join_field", tempJSON);
                    saveDocument(obj, metaObj.getString("child"));
                } else {
                    indexJSONArray(metaObj, schemaDefinitionObject);
                }
                size--;
            }
        }
    }

    public void indexJSONArray(JSONObject metaObj, JSONObject schemaDefinitionObject) {
        JSONObject itemsObject;
        String itemsKey = "items";
        String typeKey = "type";
        JSONObject propertiesObject = metaObj.getJSONObject("propertiesObj");
        try {
            // String type = propertiesObject.getJSONObject(itemsKey).getString("type");
            itemsObject = propertiesObject.getJSONObject(itemsKey);
        } catch (Exception e) {
            String definitionRef = propertiesObject.getJSONObject(itemsKey).getString("$ref");
            itemsObject = schemaDefinitionObject.getJSONObject(definitionRef.split("/")[2]);
        }
        Set<String> setValues = redisRepository.getAllKeysInSet(metaObj.getString("child"));
        String type = itemsObject.getString(typeKey);
        // if(type.equals("array")){
        // arrObj.put(indexJSONArray(setVal, itemsObject, schemaDefinitionObject));
        // }

        if (type.equals("object")) {
            for (String setVal : setValues) {
                JSONObject tempObj = new JSONObject();
                tempObj.put("parent", metaObj.getString("parent"));
                tempObj.put("child", setVal);
                tempObj.put("type", "obj");
                tempObj.put("propertiesObj", itemsObject);
                indexPlan(tempObj, schemaDefinitionObject);
            }
        }
    }

    private void saveDocument(JSONObject document, String id) {
        RestTemplate restTemplate = new RestTemplate();
        String fooResourceUrl = "http://localhost:9200/plan/_search";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<String>(document.toString(), headers);
        restTemplate.put("http://localhost:9200/plan/_doc/" + id + "?routing=1", request);
        System.out.println("here");
        // System.out.println("response " + document);
        // assertThat(response.getStatusCode(), equal(HttpStatus.OK));
        // System.out.println("Document: " + counter + ":" +
        // document.getJSONObject("my_join_field") + " " + response.getStatusCode());
    }

    private void deletePlan(String planId) {
        RestTemplate restTemplate = new RestTemplate();
        // HttpHeaders headers = new HttpHeaders();
        // headers.setContentType(MediaType.APPLICATION_JSON);
        // HttpEntity<String> request = new HttpEntity<String>(headers);
        restTemplate.delete("http://localhost:9200/plan/_doc/" + planId);
    }
}
