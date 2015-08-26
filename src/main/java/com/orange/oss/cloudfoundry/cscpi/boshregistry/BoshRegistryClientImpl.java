package com.orange.oss.cloudfoundry.cscpi.boshregistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

/**
 * bosh registry client see
 * https://github.com/cloudfoundry/bosh/blob/master/bosh
 * -registry/spec/unit/bosh/registry/api_controller_spec.rb
 * 
 * @author poblin
 *
 */
public class BoshRegistryClientImpl implements BoshRegistryClient {

	private static Logger logger = LoggerFactory.getLogger(BoshRegistryClientImpl.class.getName());

	@Value("${cpi.registry.endpoint}")
	String endpoint;

	@Value("${cpi.registry.user}")
	String user;

	@Value("${cpi.registry.password}")
	String password;

	@Override
	public void put(String vm_id, String settings) {

		String uri = this.endpoint+"/instances/" + vm_id;
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.postForObject(uri, settings, String.class);

	}

	@Override
	public String get(String vm_id) {
		String uri = this.endpoint+"/instances/" + vm_id + "/settings";
		RestTemplate restTemplate = new RestTemplate();
		String result = restTemplate.getForObject(uri, String.class);
		return result;
	}
	
	@Override
	public String getRaw(String vm_id) {
		String uri = this.endpoint+"/instances/" + vm_id + "/rawsettings";
		RestTemplate restTemplate = new RestTemplate();
		String result = restTemplate.getForObject(uri, String.class);
		return result;
	}

	

	@Override
	public void delete(String vm_id) {
		String uri = this.endpoint+"instances/" + vm_id;
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.delete(uri);
		
		
	}

}
