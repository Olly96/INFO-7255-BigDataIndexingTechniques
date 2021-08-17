package com.INFO7255.Service;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JSONValidator {
	
	@Autowired
	SchemaService schemaService;
	 public void validateJson(JSONObject object) throws ValidationException{
		String schemaString = schemaService.getSchema();
	    Schema schema = SchemaLoader.load(new JSONObject(schemaString));
	    schema.validate(object);
	    }
}
