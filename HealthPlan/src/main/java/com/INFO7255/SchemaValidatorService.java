package com.INFO7255;

import java.io.File;
import java.util.Scanner;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

@Service
public class SchemaValidatorService {

	Schema schema;
	public SchemaValidatorService() {
		StringBuilder responseStrBuilder = new StringBuilder();
		try {
            File file = ResourceUtils.getFile("classpath:model/planSchema.json");
		      Scanner myReader = new Scanner(file);
		      while (myReader.hasNextLine()) {
		        String data = myReader.nextLine();
		        responseStrBuilder.append(data);
		      }
		      myReader.close();
			  JSONObject rawSchema = new JSONObject(new JSONTokener(responseStrBuilder.toString()));
			  schema = SchemaLoader.load(rawSchema);

		    } catch (Exception e) {
		    	System.out.println(e.getMessage());
		    	ValidationException vException = (ValidationException)e;
		    	  vException.getCausingExceptions().stream()
		    	      .map(ValidationException::getMessage)
		    	      .forEach(System.out::println);
		    }
	}
	
	public void validateJSON(String jsonString) throws ValidationException {
		  schema.validate(new JSONObject(jsonString)); // throws a ValidationException if this object is invalid
	}
}
