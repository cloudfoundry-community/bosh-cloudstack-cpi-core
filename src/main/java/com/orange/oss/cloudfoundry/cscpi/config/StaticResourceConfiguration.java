package com.orange.oss.cloudfoundry.cscpi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Spring boot static resource config, to serve http (webdav servlet does not support chunked download)
 *
 */
@Configuration
public class StaticResourceConfiguration extends WebMvcConfigurerAdapter {

	private static final String TEMPLATE_CONTEXT = "/templates/**";
	@Value("${cpi.webdav_directory}")
	String webDavDirectory;

	
	@Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
    	//need a trailing slash
        registry.addResourceHandler(TEMPLATE_CONTEXT).addResourceLocations("file:"+webDavDirectory+"/");
    }
}	
