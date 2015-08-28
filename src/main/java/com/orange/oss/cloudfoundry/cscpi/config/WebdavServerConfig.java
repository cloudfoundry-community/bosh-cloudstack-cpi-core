package com.orange.oss.cloudfoundry.cscpi.config;

import net.sf.webdav.WebdavServlet;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Spring Boot configuration. Set up a WebDAV servlet
 * @author pierre
 *
 */

@Configuration
public class WebdavServerConfig {
	
	
	
	private static final String WEBDAV_CONTEXT = "/webdav/*";
	
	@Value("${cpi.webdav_directory}")
	String webDavDirectory;
	
	
	
	//add a WebDavServlet for cloudstack template http exposition (webdav exposes a local filesystem in http protocol. 
	//known client : windows / sharepoint, bitkinex, linux => cadaver, konqueror, java => sardine
	@Bean
	public ServletRegistrationBean servletRegistrationBean(){
		WebdavServlet webdav=new WebdavServlet();
		ServletRegistrationBean srb=new ServletRegistrationBean(webdav,WEBDAV_CONTEXT);

		//name of the class that implements net.sf.webdav.WebdavStore
		srb.addInitParameter("ResourceHandlerImplementation", "net.sf.webdav.LocalFileSystemStore");
		
		//local fs exposition
		srb.addInitParameter("rootpath", webDavDirectory);
		
		//Overriding RFC 2518, the folders of resources being created, can be created too if they do not exist.
		srb.addInitParameter("lazyFolderCreationOnPut", "0");
		
		
		srb.addInitParameter("default-index-file", "");
		srb.addInitParameter("instead-of-404", "");
		//set to 2G
		srb.addInitParameter("maxUploadSize", "2000000000");
	    return srb ;
	}
}
