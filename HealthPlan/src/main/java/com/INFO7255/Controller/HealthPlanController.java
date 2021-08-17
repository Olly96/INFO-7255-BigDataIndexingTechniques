package com.INFO7255.Controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.everit.json.schema.ValidationException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import com.INFO7255.Service.HealthPlanService;
import com.INFO7255.Service.JSONValidator;

@RestController
public class HealthPlanController {
	
	@Autowired
	private HealthPlanService healthPlanService;
	
	@Autowired
	private JSONValidator jsonValidator;
	

	@GetMapping(path = "/plans", produces = "application/json")
	public ResponseEntity<String> getAllPlans() {
		Map<String, String> plansJsonObjects = healthPlanService.getAlLPlans();
		System.out.print(plansJsonObjects);
		return ResponseEntity.ok().body(plansJsonObjects.values().toString());
	}
	
	@GetMapping(path = "/plans/{objectId}", produces = "application/json")
	public ResponseEntity<String> getPlan(@RequestHeader HttpHeaders headers, @PathVariable String objectId) {
		if(!healthPlanService.checkIfPlanIdExists(objectId)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONObject().put("message", "Plan with objectId " + objectId + " does not exist.").toString());
		}  
		String storedEtag = healthPlanService.getEtag(objectId);
	      String receivedETag = headers.getFirst("If-None-Match");
	      System.out.println("EtagVal" + receivedETag.equals(storedEtag) + storedEtag +  receivedETag);
	      if (receivedETag != null && receivedETag.equals(storedEtag)) {
	          return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(storedEtag).body(new JSONObject().toString());
	       }
	        
		JSONObject planObject = healthPlanService.getPlan(objectId);
		System.out.print(planObject);
		return ResponseEntity.ok().eTag(storedEtag).body(planObject.toString());
	}
	
	@PostMapping(path = "/plan", produces = "application/json")
	public ResponseEntity<String> postPlan(@RequestHeader HttpHeaders headers, @RequestBody String plan){
		JSONObject planObject = new JSONObject(plan);
		System.out.println("here1");
		try {
			jsonValidator.validateJson(planObject);
		} catch(ValidationException e) {
			System.out.println(e.getMessage());
			Object[] errorList = e.getCausingExceptions().stream()
	    	      .map(ValidationException::getMessage).toArray();
			List<Object> ls = new ArrayList<>(Arrays.asList(errorList));
			if(ls.size() == 0) {
				ls.add(e.getMessage());
			}
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONObject().put("message", "Found following errors in plan").put("errors", ls).toString());

		}
		System.out.println("here2");
		String objectId = planObject.getString("objectId");
		String etagValString;
		if(healthPlanService.checkIfPlanIdExists(objectId)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONObject().put("message", "Plan with objectId " + objectId + " already exists. Please use different objectId").toString());
		}
		System.out.println("here3");
		try {
			etagValString = healthPlanService.savePlan(objectId, plan);
		} catch(Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new JSONObject().put("message", "Plan with objectId " + objectId + " already exists. Please use different objectId").toString());
		}
		return ResponseEntity.status(HttpStatus.CREATED).eTag(etagValString).body(plan);

	}

	@PatchMapping(path = "/plan", produces = "application/json")
	public ResponseEntity<String> updatePlan(@RequestHeader HttpHeaders headers, @RequestBody String plan){
		JSONObject planObject = new JSONObject(plan);
		String objectId = planObject.getString("objectId");
		System.out.println("here1");
		if(!healthPlanService.checkIfPlanIdExists(objectId)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONObject().put("message", "Plan with objectId " + objectId + " does not exist").toString());
		}
		String storedEtag = healthPlanService.getEtag(objectId);
	    String receivedETag = headers.getFirst("If-Match");
	    if (receivedETag!= null && !receivedETag.equals(storedEtag)) {
			System.out.println("EtagVal " + receivedETag.equals(storedEtag) + " " + storedEtag +  " " + receivedETag);
	        return ResponseEntity.status(HttpStatus.FORBIDDEN).eTag(storedEtag).body(new JSONObject().put("message", "Plan has been modified. Please get the updated version and retry.").toString());
	    }
		JSONObject updatedPlan;
		try {
			 updatedPlan =  healthPlanService.getUpdatedPlan(objectId, planObject);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONObject().put("message", e1.getMessage()).toString());
		}
		try {
			jsonValidator.validateJson(updatedPlan);
		} catch(ValidationException e) {
			System.out.println(e.getMessage());
			Object[] errorList = e.getCausingExceptions().stream()
	    	      .map(ValidationException::getMessage).toArray();
			List<Object> ls = new ArrayList<>(Arrays.asList(errorList));
			if(ls.size() == 0) {
				ls.add(e.getMessage());
			}
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONObject().put("message", "Found following errors in plan").put("errors", ls).toString());
		}
		System.out.println("here2");
		String etagValString;
	
		System.out.println("here3");
		try {
			etagValString = healthPlanService.savePlan(objectId, updatedPlan.toString());
		} catch(Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new JSONObject().put("message", e.getMessage()).toString());
		}
		return ResponseEntity.ok().eTag(etagValString).body(updatedPlan.toString());

	}


	@PutMapping(path = "/plan", produces = "application/json")
	public ResponseEntity<String> putPlan(@RequestHeader HttpHeaders headers, @RequestBody String plan){
		JSONObject planObject = new JSONObject(plan);
		String objectId = planObject.getString("objectId");
		System.out.println("here1");
		if(!healthPlanService.checkIfPlanIdExists(objectId)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONObject().put("message", "Plan with objectId " + objectId + " does not exist").toString());
		}
		String storedEtag = healthPlanService.getEtag(objectId);
	    String receivedETag = headers.getFirst("If-Match");
	    if (receivedETag!= null && !receivedETag.equals(storedEtag)) {
			System.out.println("EtagVal " + receivedETag.equals(storedEtag) + " " + storedEtag +  " " + receivedETag);
	        return ResponseEntity.status(HttpStatus.FORBIDDEN).eTag(storedEtag).body(new JSONObject().put("message", "Plan has been modified. Please get the updated version and retry.").toString());
	    }
		// JSONObject updatedPlan;
		// try {
		// 	 updatedPlan =  healthPlanService.getUpdatedPlan(objectId, planObject);
		// } catch (Exception e1) {
		// 	// TODO Auto-generated catch block
		// 	e1.printStackTrace();
		// 	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONObject().put("message", e1.getMessage()).toString());
		// }
		try {
			jsonValidator.validateJson(planObject);
		} catch(ValidationException e) {
			System.out.println(e.getMessage());
			Object[] errorList = e.getCausingExceptions().stream()
	    	      .map(ValidationException::getMessage).toArray();
			List<Object> ls = new ArrayList<>(Arrays.asList(errorList));
			if(ls.size() == 0) {
				ls.add(e.getMessage());
			}
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONObject().put("message", "Found following errors in plan").put("errors", ls).toString());
		}
		System.out.println("here2");
		String etagValString;
	
		System.out.println("here3");
		try {
			healthPlanService.deletePlan(objectId);
			etagValString = healthPlanService.savePlan(objectId, planObject.toString());
		} catch(Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new JSONObject().put("message", e.getMessage()).toString());
		}
		return ResponseEntity.ok().eTag(etagValString).body(planObject.toString());

	}
	
	
	@DeleteMapping(path = "/plan/{objectId}", produces = "application/json")
	public ResponseEntity<String> deleteObject(@RequestHeader HttpHeaders headers, @PathVariable String objectId){
		// String delObjectId = delObject.getString("deleteObjectId");
		// String delObjectType = delObject.getString("deleteObjectType");
		// String etagValString;
		if(!healthPlanService.checkIfPlanIdExists(objectId)) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new JSONObject().put("message", "Plan with objectId " + objectId + " does not exist.").toString());
		}
		// if(delObjectId.equals(planObjectId)) {
			healthPlanService.deletePlan(objectId);
			return ResponseEntity.ok().body(new JSONObject().put("message", "Plan with objectId " + objectId + " deleted successfully").toString());
		// }
		// healthPlanService.deleteObject(planObjectId, delObjectId, delObjectType);
		
		// return ResponseEntity.ok().body(new JSONObject().put("message", "Plan " + planObjectId + " deleted successfully").toString());

	}
}
