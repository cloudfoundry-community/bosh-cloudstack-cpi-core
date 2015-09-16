package com.orange.oss.cloudfoundry.cscpi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.orange.oss.cloudfoundry.cscpi.CPIAdapter;
import com.orange.oss.cloudfoundry.cscpi.CPIAdapterImpl;
import com.orange.oss.cloudfoundry.cscpi.logic.CPI;
import com.orange.oss.cloudfoundry.cscpi.logic.CPIImpl;
import com.orange.oss.cloudfoundry.cscpi.logic.UserDataGenerator;
import com.orange.oss.cloudfoundry.cscpi.logic.UserDataGeneratorImpl;
import com.orange.oss.cloudfoundry.cscpi.logic.VmSettingGenerator;
import com.orange.oss.cloudfoundry.cscpi.logic.VmSettingGeneratorImpl;


@Configuration
public class CPIConfig {

	
	@Bean
	public CPI cpi(){
		return new CPIImpl();
	};
	
	
	 @Bean CPIAdapter cpiAdapter(){
		 return new CPIAdapterImpl();
	 }
	 
	 @Bean UserDataGenerator userDataGenerator(){
		 return new UserDataGeneratorImpl();
	 }
	 
	 @Bean VmSettingGenerator vmSettingGenerator(){
		 return new VmSettingGeneratorImpl();
	 }
	 
	 
	
}
