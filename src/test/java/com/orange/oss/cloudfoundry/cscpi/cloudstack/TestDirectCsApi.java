package com.orange.oss.cloudfoundry.cscpi.cloudstack;


import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

import com.orange.oss.cloudfoundry.cscpi.BoshCloudstackCpiCoreApplication;
import com.orange.oss.cloudfoundry.cscpi.config.CloudStackConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = BoshCloudstackCpiCoreApplication.class)
public class TestDirectCsApi {

	private static Logger logger=LoggerFactory.getLogger(TestDirectCsApi.class.getName());
	
	@Autowired
	CloudStackConfiguration config;

	@Test
	public void testRawRestApiCall() throws NoSuchAlgorithmException, InvalidKeyException {

		RestTemplate restTemplate = new RestTemplate();

		if (config.proxy_host.length() > 0) {

			HttpClientBuilder clientBuilder = HttpClientBuilder.create();
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(new AuthScope(config.proxy_host, Integer.valueOf(config.proxy_port)), new UsernamePasswordCredentials(
					config.proxy_user, config.proxy_password));
			HttpHost myProxy = new HttpHost(config.proxy_host, Integer.valueOf(config.proxy_port));
			clientBuilder.setProxy(myProxy).setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy()).setDefaultCredentialsProvider(credsProvider)
					.disableCookieManagement();
			CloseableHttpClient httpClient = clientBuilder.build();

			ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

			restTemplate.setRequestFactory(requestFactory);
		}
		
		Map<String,String> apiParameters=new TreeMap<String, String>();

		apiParameters.put("command","listZones");
		
		
        String secret=config.secret_access_key;
        
        String request="apiKey="+this.config.api_key;
        
        for(String key:apiParameters.keySet()){
        	request+="&"+key+"="+apiParameters.get(key);
        }
        logger.debug("request {}",request);

        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(),"HmacSHA1");
        mac.init(keySpec);
        mac.update(request.toLowerCase().getBytes());
        byte[] encryptedBytes = mac.doFinal();

        String signature=Base64.encodeBase64String(encryptedBytes);//result		
		
		String httpRequest = config.endpoint+"?"+request+"&signature="+signature;
		logger.info("http request {}",httpRequest);
		String result = restTemplate.getForObject(httpRequest, String.class);

	}
}
