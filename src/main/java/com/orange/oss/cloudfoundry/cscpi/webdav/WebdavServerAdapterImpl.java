package com.orange.oss.cloudfoundry.cscpi.webdav;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;


/**
 * Web client, pushes the binary template provided by the Director to a webdav server
 * @author pierre
 *
 */
public class WebdavServerAdapterImpl implements WebdavServerAdapter {

	private static Logger logger=LoggerFactory.getLogger(WebdavServerAdapterImpl.class.getName());
	

	@Value("${cpi.webdav_host}")
	String webDavHost;
	
	@Value("${cpi.webdav_port}")
	String webDavPort;
	
	@Override
	@HystrixCommand(commandProperties = @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "300000"))	
	public String pushFile(InputStream is, String ressourceName) {
		logger.info("begin pushing file  {} to webdav server",ressourceName);
		
		Sardine sardine=SardineFactory.begin();
		
		try {
			String targetUrl = "http://"+webDavHost+":"+webDavPort+"/webdav/"+ressourceName;
			String retrieveUrl="http://"+webDavHost+":"+webDavPort+"/templates/"+ressourceName;
			logger.debug("target is {}",targetUrl);
			sardine.put(targetUrl, is);
			logger.info("done pushing file  {} to webdav server. retrieve URL is {}",ressourceName,retrieveUrl);
			return retrieveUrl;
			
		} catch (IOException e) {
			logger.error("failure putting file to webdav server {}",e.getMessage());
			throw new RuntimeException(e);
			
		}

	}

	@Override
	@HystrixCommand(commandProperties = @HystrixProperty(name = "execution.isolation.thread.timeoutInMilliseconds", value = "300000"))	
	public void delete(String ressourceName) {
		logger.info("begin deleting file  {} to webdav server",ressourceName);
		
		Sardine sardine=SardineFactory.begin();
		
		try {
			String targetUrl = "http://"+webDavHost+":"+webDavPort+"/webdav/"+ressourceName;
			logger.debug("target is {}",targetUrl);
			sardine.delete(targetUrl);
			
		} catch (IOException e) {
			logger.warn("failure deleting file from webdav server {}. Ignoring ...",e.getMessage());
		}
		logger.info("done deleting file  {} from webdav server.",ressourceName);		
		
	}
	

}
