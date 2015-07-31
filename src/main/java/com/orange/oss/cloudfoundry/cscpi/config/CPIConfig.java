package com.orange.oss.cloudfoundry.cscpi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.orange.oss.cloudfoundry.cscpi.CPI;
import com.orange.oss.cloudfoundry.cscpi.CPIAdapter;
import com.orange.oss.cloudfoundry.cscpi.CPIAdapterImpl;
import com.orange.oss.cloudfoundry.cscpi.CPIImpl;


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
