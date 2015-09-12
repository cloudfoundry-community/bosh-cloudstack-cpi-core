package com.orange.oss.cloudfoundry.cscpi.cep;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CEPConfig {

	@Bean
	CEPInterface cep(){
	
		CEPImpl cep=new CEPImpl();
		return cep;
	}
	
	
}
