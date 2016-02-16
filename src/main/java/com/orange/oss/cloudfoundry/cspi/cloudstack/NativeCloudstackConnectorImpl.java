package com.orange.oss.cloudfoundry.cspi.cloudstack;

import java.net.URI;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;


/**
 * Direct REST connector to CloudStack
 * Bypasses jclouds cloudstack current limitations
 * @author poblin-orange
 *
 */
public class NativeCloudstackConnectorImpl implements NativeCloudstackConnector {

	private static Logger logger = LoggerFactory.getLogger(NativeCloudstackConnectorImpl.class.getName());

	
	//FIXME : try to factorize these property injection in a dedicated bean
	@Value("${cloudstack.endpoint}")	
	public String endpoint;

	@Value("${cloudstack.api_key}")	
	public  String api_key;

	@Value("${cloudstack.secret_access_key}")	
	public  String secret_access_key;
	
	@Value("${cloudstack.proxy_host}")	
	public String proxy_host;

	@Value("${cloudstack.proxy_port}")	
	public String proxy_port;

	@Value("${cloudstack.proxy_user}")	
	public String proxy_user;

	@Value("${cloudstack.proxy_password}")	
	public String proxy_password;

	
	

	private RestTemplate restTemplate = new RestTemplate();

	
	public NativeCloudstackConnectorImpl() {
	}
	
	@PostConstruct
	public void init(){

		if ((this.proxy_host!=null)&&(this.proxy_host.length() > 0)) {
			logger.info("using proxy for native connector");
			HttpClientBuilder clientBuilder = HttpClientBuilder.create();
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(new AuthScope(this.proxy_host, Integer.valueOf(this.proxy_port)), new UsernamePasswordCredentials(
					this.proxy_user, this.proxy_password));
			HttpHost myProxy = new HttpHost(this.proxy_host, Integer.valueOf(this.proxy_port));
			clientBuilder.setProxy(myProxy).setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy()).setDefaultCredentialsProvider(credsProvider)
					.disableCookieManagement();
			CloseableHttpClient httpClient = clientBuilder.build();
			ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

			restTemplate.setRequestFactory(requestFactory);
		}
	}
	
	@Override
	public String nativeCall(String command,Map<String, String> apiParameters) {
		
		apiParameters.put("response", "json");
		apiParameters.put("apiKey",this.api_key);
		apiParameters.put("command",command);
		
		String request="";
		for (String key : apiParameters.keySet()) {
			request += "&" + key + "=" + apiParameters.get(key);
		}
		
		logger.debug("request {}", request);;
		String signature = computeSignature(command, apiParameters);		
		String httpRequest = this.endpoint + "?" + request + "&signature=" + signature;
		logger.info("http request {}", httpRequest);
		String result = restTemplate.getForObject(httpRequest, String.class);
		return result;
}
	

	//@Override
	public String nativeCallPost(String command, Map<String, String> apiParameters) {

		Map<String,String> urlVariables=new HashMap<String, String>();
		urlVariables.put("apiKey", this.api_key);
		urlVariables.put("command", command);		

		for (String key : apiParameters.keySet()) {
			urlVariables.put(key, apiParameters.get(key));
		}
		String signature = computeSignature(command, apiParameters);		
		urlVariables.put("signature",signature);
		
		logger.debug("request {}", urlVariables.toString());


		//String httpRequest = this.endpoint + "?" + request + "&signature=" + signature;
		//logger.info("http request {}", httpRequest);
		//String result = restTemplate.postForObject(httpRequest,"", String.class);
		
		
		ResponseEntity<String> rep= restTemplate.postForEntity(this.endpoint, "", String.class, urlVariables);
		rep.getStatusCode();
		
		
		
		
		String result="xx";
		return result;

	}

	private String computeSignature(String command,
			Map<String, String> apiParameters ) {
		
		String signature="";
		try {
			
			TreeMap<String, String> sortedLowerCaseCommand=new TreeMap<String, String>();
			for (String key:apiParameters.keySet()){
				sortedLowerCaseCommand.put(key.toLowerCase(),apiParameters.get(key).toLowerCase().replace("+", "%20"));
			}
			sortedLowerCaseCommand.put("command", command.toLowerCase()); 
			sortedLowerCaseCommand.put("apikey", this.api_key.toLowerCase());
			
			List<String> params = new ArrayList<String>();
			
			for (String key : sortedLowerCaseCommand.keySet()) {
				params.add(key + "=" + sortedLowerCaseCommand.get(key));
			}
			String[] par=new String[sortedLowerCaseCommand.keySet().size()];
			params.toArray(par);
			String sortedCommand=String.join("&", par);
			logger.debug("sorted command {}", sortedCommand);
			
			Mac mac;
			
			mac = Mac.getInstance("HmacSHA1");
			SecretKeySpec keySpec = new SecretKeySpec(this.secret_access_key.getBytes(), "HmacSHA1");
			mac.init(keySpec);
			
			mac.update(sortedCommand.getBytes());
			byte[] encryptedBytes = mac.doFinal();
			signature = Base64.encodeBase64String(encryptedBytes);// result
		} catch (NoSuchAlgorithmException | InvalidKeyException e) { 
			logger.error("Fatal :Invalid crypto keys {}", e.getMessage());
			throw new IllegalArgumentException(e);
		}
		return signature;
	}

}
