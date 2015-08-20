package com.orange.oss.cloudfoundry.cscpi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * class to group properties from Director (ie: not passed to CPI verbs, got those props from director bosh release)
 *
 */
//@Configuration
public class DirectorConfig {

	@Value("${director.mbus}")
	public String mbus;

	
	
	
}
