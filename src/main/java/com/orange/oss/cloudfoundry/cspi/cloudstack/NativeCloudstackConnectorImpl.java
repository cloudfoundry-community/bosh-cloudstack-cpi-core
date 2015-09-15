package com.orange.oss.cloudfoundry.cspi.cloudstack;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

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
	public String nativeCall(Map<String, String> apiParameters) {
		String secret = this.secret_access_key;

		String request = "apiKey=" + this.api_key;

		for (String key : apiParameters.keySet()) {
			request += "&" + key + "=" + apiParameters.get(key);
		}
		logger.debug("request {}", request);

		Mac mac;
		try {
			mac = Mac.getInstance("HmacSHA1");
			SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA1");
			mac.init(keySpec);
			mac.update(request.toLowerCase().getBytes());
			byte[] encryptedBytes = mac.doFinal();

			String signature = Base64.encodeBase64String(encryptedBytes);// result

			String httpRequest = this.endpoint + "?" + request + "&signature=" + signature;
			logger.info("http request {}", httpRequest);
			String result = restTemplate.getForObject(httpRequest, String.class);

			return result;
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			logger.error("Fatal :Invalid crypto keys {}", e.getMessage());
			throw new IllegalArgumentException(e);
		}

	}

}
