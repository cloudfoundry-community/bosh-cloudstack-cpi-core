package com.orange.oss.cloudfoundry.cscpi.restapi;

import static org.mockito.Mockito.mock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.orange.oss.cloudfoundry.cscpi.BeanMock;
import com.orange.oss.cloudfoundry.cscpi.logic.CPI;
/**
 * Mock CPI impl
 * see https://dzone.com/articles/how-mock-spring-bean-without
 * @author pierre
 *
 */
@Configuration
@BeanMock
public class MockCPI {

	@Bean
	@Primary
	public CPI registerCPIMock(){
		return mock(CPI.class);
	}
	
}
