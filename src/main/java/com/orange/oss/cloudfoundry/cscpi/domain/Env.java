package com.orange.oss.cloudfoundry.cscpi.domain;

import com.fasterxml.jackson.databind.JsonNode;

public class Env {

	private JsonNode envNode;
	 
	//public Map<String, Object> env=new HashMap<String, Object>();
//	public Env(){
//		this.env=new HashMap<String,Object>();
//	}
	
	public Env(){
		
	}
	
	public Env(JsonNode node){
		this.envNode=node;
	}

}
