package com.orange.oss.cloudfoundry.cscpi;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UserDataGeneratorImpl implements UserDataGenerator {

	private static Logger logger=LoggerFactory.getLogger(UserDataGeneratorImpl.class.getName());
	
	/**
	 * see https://github.com/cloudfoundry/bosh-agent/blob/585d2cc3a47129aa875738f09a26101ec6e0b1d1/infrastructure/http_metadata_service.go
	 * for bosh agent http user data expectations
	 * @author pierre
	 *
	 */
	public static class Server {
		Server(String name){
			this.Name=name;
		}
		public String Name;
	}
	
	public static class Registry {
		Registry(String endpoint){
			this.Endpoint=endpoint;
		}
		
		public String Endpoint;
	}
	
	public static class DNS {
		DNS(String nameServer){
			this.NameServer=nameServer;
		}
		public String NameServer;
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
		datas.server=new Server("nomduserver");
		datas.registry=new Registry("http://xx.xx.xx:8080");
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
		
		//base64 encore resulting String
        final byte[] bytes = userData.getBytes(StandardCharsets.UTF_8);
        final String encoded = Base64.getEncoder().encodeToString(bytes);
        logger.debug(userData + " => " + encoded);
		return encoded;
	}
	
}
