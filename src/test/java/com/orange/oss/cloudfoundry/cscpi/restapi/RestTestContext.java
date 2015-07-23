package com.orange.oss.cloudfoundry.cscpi.restapi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;


@Configuration
public class RestTestContext {

	@Bean
	public RestTemplate client(){
		
		
		RestTemplate restTemplate=new RestTemplate();
		//restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
		return restTemplate;
	}
	
}
