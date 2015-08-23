package com.orange.oss.cloudfoundry.cscpi.boshregistry;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class RegistryInstance {

	@Id
	private String id;
	
	@Column(length = Integer.MAX_VALUE)
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
	
	public void setSettings(String settings){
		this.settings=settings;
	}
	

}
