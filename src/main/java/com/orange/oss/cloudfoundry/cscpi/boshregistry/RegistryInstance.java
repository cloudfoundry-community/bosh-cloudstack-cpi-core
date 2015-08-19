package com.orange.oss.cloudfoundry.cscpi.boshregistry;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class RegistryInstance {

	@Id
	private String id;
	private String settings;

	protected RegistryInstance() {
	}

	public RegistryInstance(String id, String settings) {
		this.id = id;
		this.settings = settings;
	}
	
	public String getSettings(){
		return this.settings;
	}

}
