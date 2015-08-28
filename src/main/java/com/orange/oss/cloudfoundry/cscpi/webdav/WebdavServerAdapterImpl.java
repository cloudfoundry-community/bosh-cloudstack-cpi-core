package com.orange.oss.cloudfoundry.cscpi.webdav;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.github.sardine.Sardine;
import com.github.sardine.SardineFactory;


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
	

}
