package com.orange.oss.cloudfoundry.cscpi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * class to group properties from Director (ie: not passed to CPI verbs, got those props from director bosh release)
 *
 */
@Configuration
public class DirectorConfig {

//	@Value("${director.mbus}")
//	public String mbus;

/**
 * director props mapping
 * blobstore:
  provider: dav
  options:
    endpoint: http://10.234.228.157:25250
    user: director
    password: director-password
	
 */

	@Value("${cpi.blobstore.provider}")
	public String blobstore_provider;
	
	@Value("${cpi.blobstore.path}")
	public String path;

	@Value("${cpi.blobstore.address}")
	public String address;

	@Value("${cpi.blobstore.port}")
	public String port;

	@Value("${cpi.blobstore.options.user}")
	public String user;

	@Value("${cpi.blobstore.options.password}")
	public String password;
	
	@Value("${cpi.agent.mbus}")
	public String mbus;
	
	@Value("${cpi.ntp}")
	public String ntp;
	
	
}
