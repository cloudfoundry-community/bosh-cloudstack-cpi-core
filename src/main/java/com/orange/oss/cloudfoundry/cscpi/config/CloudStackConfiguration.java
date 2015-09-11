package com.orange.oss.cloudfoundry.cscpi.config;


import java.util.Properties;

import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.cloudstack.CloudStackApi;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;



@Configuration
public class CloudStackConfiguration {

	private final static Logger logger=LoggerFactory.getLogger(CloudStackConfiguration.class.getName());
	
	
	
	@Value("${cloudstack.endpoint}")	
	public String endpoint;

	@Value("${cloudstack.api_key}")	
	public  String api_key;

	@Value("${cloudstack.secret_access_key}")	
	public  String secret_access_key;

	@Value("${cloudstack.default_key_name}")	
	public String default_key_name;

	@Value("${cloudstack.private_key}")	
	public String private_key;


	
	@Value("${cloudstack.proxy_host}")	
	public String proxy_host;

	@Value("${cloudstack.proxy_port}")	
	public String proxy_port;

	@Value("${cloudstack.proxy_user}")	
	public String proxy_user;

	@Value("${cloudstack.proxy_password}")	
	public String proxy_password;

	@Value("${cloudstack.state_timeout}")	
	public int state_timeout;

	@Value("${cloudstack.state_timeout_volume}")	
	public int state_timeout_volume;

	@Value("${cloudstack.stemcell_public_visibility}")	
	public boolean stemcell_public_visibility;

	@Value("${cloudstack.default_zone}")	
	public String  default_zone;
	
	@Value("${cpi.mock_create_stemcell}")
	public boolean mockCreateStemcell;

	
	@Value("${cpi.vm_expunge_delay}")
	public int vmExpungeDelaySeconds;
	
	//initial preexisting template (to mock stemcell upload before template generation)
	@Value("${cpi.existing_template_name}")
	public String existingTemplateName;
	
	
	
	@Value("${cpi.default_disk_offering}")
	public String defaultDiskOffering;

	@Value("${cpi.default_ephemeral_disk_offering}")	
	public String defaultEphemeralDiskOffering;
	
	
	@Bean
		public CloudStackApi cloudStackAdapter(){
		
		String username=this.api_key;
		String password=this.secret_access_key;
		
		logger.debug("cloudstack adapter. endpoint {} \n username {} \n",endpoint,username);
		
        Iterable<Module> modules = ImmutableSet.<Module>of(new SLF4JLoggingModule());

        logger.debug("initialize jclouds compute API");
        String provider = "cloudstack";
        
        logger.debug("logging as {}",username);
        
       Properties overrides = new Properties();
        overrides.setProperty(Constants.PROPERTY_RELAX_HOSTNAME, "true");
        overrides.setProperty(Constants.PROPERTY_TRUST_ALL_CERTS, "true");

        //see https://raw.githubusercontent.com/abayer/cloudcat/master/src/groovy/cloudstack/reporting/JCloudsConnection.groovy
        overrides.setProperty(Constants.PROPERTY_CONNECTION_TIMEOUT, "5000");
        overrides.setProperty(Constants.PROPERTY_TIMEOUTS_PREFIX + "VirtualMachineClient", "360000");
        overrides.setProperty(Constants.PROPERTY_TIMEOUTS_PREFIX + "TemplateClient", "360000");
        overrides.setProperty(Constants.PROPERTY_TIMEOUTS_PREFIX + "GlobalHostClient", "360000");
        overrides.setProperty(Constants.PROPERTY_TIMEOUTS_PREFIX + "HostClient", "360000");
        overrides.setProperty(Constants.PROPERTY_TIMEOUTS_PREFIX + "GlobalAlertClient", "360000");
        overrides.setProperty(Constants.PROPERTY_TIMEOUTS_PREFIX + "AlertClient", "360000");
        overrides.setProperty(Constants.PROPERTY_TIMEOUTS_PREFIX + "GlobalAccountClient", "360000");
        overrides.setProperty(Constants.PROPERTY_TIMEOUTS_PREFIX + "AccountClient", "360000");

		if (proxy_host.length() > 0) {
			logger.info("using proxy {}:{} with user {}", proxy_host,proxy_port, proxy_user);

			overrides.setProperty(Constants.PROPERTY_PROXY_HOST, proxy_host);
			overrides.setProperty(Constants.PROPERTY_PROXY_PORT, proxy_port);
			overrides.setProperty(Constants.PROPERTY_PROXY_USER, proxy_user);
			overrides.setProperty(Constants.PROPERTY_PROXY_PASSWORD,
					proxy_password);
		}
	
        overrides.setProperty("jclouds.retries-delay-start", "1000");
        
        
        
        CloudStackApi api = ContextBuilder.newBuilder(provider)
                .endpoint(endpoint)
                .credentials(username, password)
                .modules(modules)
                .overrides(overrides)
                .buildApi(CloudStackApi.class);
        
        
        
        return api;

	}
	
	
}
