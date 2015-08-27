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

	@Value("${blobstore.provider}")
	public String blobstore_provider;
	
	@Value("${blobstore.path}")
	public String path;
	
	
	@Value("${blobstore.options.endpoint}")
	public String endpoint;

	@Value("${blobstore.options.user}")
	public String user;

	@Value("${blobstore.options.password}")
	public String password;
	
}
