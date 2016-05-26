package com.orange.oss.cloudfoundry.cscpi.logic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;



/**
 * bosh registry client see
 * https://github.com/cloudfoundry/bosh/blob/master/bosh
 * -registry/spec/unit/bosh/registry/api_controller_spec.rb
 * 
 * @author poblin
 *
 */
public class BoshRegistryClientImpl implements BoshRegistryClient {

	private static Logger logger = LoggerFactory
			.getLogger(BoshRegistryClientImpl.class.getName());

	@Value("${cpi.registry.endpoint}")
	String endpoint;

	@Value("${cpi.registry.user}")
	String user;

	@Value("${cpi.registry.password}")
	String password;

	@Override
	@HystrixCommand	
	public void put(String vm_id, String settings) {

		String uri = this.endpoint + "/instances/" + vm_id;
		HttpHeaders headers=this.basicAuthEntityHeader();
		
		HttpEntity<String> request = new HttpEntity<String>(settings,headers);
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.exchange(uri,HttpMethod.POST,request, String.class);
	}

	
	@Override
	@HystrixCommand	
	public String get(String vm_id) {
		String uri = this.endpoint + "/instances/" + vm_id + "/settings";
		HttpHeaders headers=this.basicAuthEntityHeader();
		
		HttpEntity<String> request = new HttpEntity<String>(vm_id,headers);
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> rep=restTemplate.exchange(uri,HttpMethod.GET,request, String.class);
		return rep.getBody();
	}

	@Override
	@HystrixCommand	
	public String getRaw(String vm_id) {
		String uri = this.endpoint + "/instances/" + vm_id + "/rawsettings";

		HttpHeaders headers=this.basicAuthEntityHeader();
		
		HttpEntity<String> request = new HttpEntity<String>(vm_id,headers);
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> rep=restTemplate.exchange(uri,HttpMethod.GET,request, String.class);
		return rep.getBody();
	}
	
	@Override
	@HystrixCommand	
	public void delete(String vm_id) {
		String uri = this.endpoint + "instances/" + vm_id;
		HttpHeaders headers=this.basicAuthEntityHeader();
		
		HttpEntity<String> request = new HttpEntity<String>(vm_id,headers);
		RestTemplate restTemplate = new RestTemplate();
		
		// Dont fail on exception (registry corrupt)
		try {
			restTemplate.exchange(uri,HttpMethod.DELETE,request, String.class);
		} catch (RestClientException e) {
			logger.warn("could not delete vm {} from registry with exception {}. Ignoring ...",vm_id, e.getMessage());
		}

	}

	/**
	 * http header for registry basic authentication
	 * @return
	 */
	private HttpHeaders basicAuthEntityHeader() {
		byte[] bytes = String.format("%s:%s", this.user, this.password).getBytes();
		String authHeader = String.format("Basic %s", Base64Utils.encodeToString(bytes));
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.add("AUTHORIZATION", authHeader);
		
		return headers;


	}

	
	
	
}
