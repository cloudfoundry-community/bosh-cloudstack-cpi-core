package com.orange.oss.cloudfoundry.cscpi;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UserDataGeneratorImpl implements UserDataGenerator {

	private static Logger logger=LoggerFactory.getLogger(UserDataGeneratorImpl.class.getName());

	
	@Value("${registry.endpoint}")
	String endpoint;
	
	
	
	/**
	 * see https://github.com/cloudfoundry/bosh-agent/blob/585d2cc3a47129aa875738f09a26101ec6e0b1d1/infrastructure/http_metadata_service.go
	 * for bosh agent http user data expectations
	 * @author pierre
	 *
	 */
	public static class Server {
		Server(String name){
			this.name=name;
		}
		public String name;
	}
	
	public static class Registry {
		Registry(String endpoint){
			this.endpoint=endpoint;
		}
		
		public String endpoint;
	}
	
	public static class DNS {
		DNS(String nameServer){
			this.nameserver=nameServer;
		}
		public String nameserver;
	}
	
	
	public class UserData{
		Server server;
		Registry registry;
		DNS dns;
	}
	
	
	@Override
	public String vmMetaData(){
		
		UserData datas=new UserData();
		//compose the user data from cpi create vm call
		
		
		try {
			URL url=new URL(this.endpoint);
		} catch (MalformedURLException e) {
			logger.error("Registry endpoint is malformed {} : {}",this.endpoint,e.getMessage());
			throw new IllegalArgumentException("Registry Endpoint is incorrect : "+this.endpoint, e);
		}
		
		datas.registry=new Registry(this.endpoint);
		
		
		
		//FIXME : get DNS from cloud properties, and serverName from ?
		datas.server=new Server("nomduserver");
		datas.dns=new DNS("10.0.0.1");
		
		//serialize to json
		ObjectMapper mapper = new ObjectMapper();
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
		String userData;
		try {
			userData = mapper.writeValueAsString(datas);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Cant serialize JSON userData", e);
		}
		
		logger.info("generated user data : \n{}",userData);
		return userData;
	}
	
}
