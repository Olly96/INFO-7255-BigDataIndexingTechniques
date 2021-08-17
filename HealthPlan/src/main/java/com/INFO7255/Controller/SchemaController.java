package com.INFO7255.Controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.INFO7255.Service.SchemaService;

@RestController
public class SchemaController {

	@Autowired
	private SchemaService schemaService;


	@PostMapping(path = "/schema", produces = "application/json")
	public ResponseEntity<String> saveSchema(@RequestBody String plan) {
		schemaService.saveSchema(plan);
		return ResponseEntity.ok().body(plan);
	}
	
}
