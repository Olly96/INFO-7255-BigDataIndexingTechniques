package com.INFO7255.Service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.INFO7255.Repository.HealthPlanRepository;

@Service
public class SchemaService {

	@Autowired
	private HealthPlanRepository healthPlanRepository;
	@Autowired
	private JSONParsePath jsonParsePath;
	

	public void saveSchema(String schema) {
		healthPlanRepository.saveKeyVal("SCHEMA", schema);
	}
	
	public String getSchema() {
		return healthPlanRepository.getKey("SCHEMA");
	}

}






	