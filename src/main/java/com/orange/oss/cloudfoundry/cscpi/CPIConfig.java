package com.orange.oss.cloudfoundry.cscpi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class CPIConfig {

	
	@Bean
	public CPI cpi(){
		return new CPIImpl();
	};
	
	
	 @Bean CPIAdapter cpiAdapter(){
		 return new CPIAdapterImpl();
	 }
	
}
