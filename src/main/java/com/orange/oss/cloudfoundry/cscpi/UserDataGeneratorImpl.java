package com.orange.oss.cloudfoundry.cscpi;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orange.oss.cloudfoundry.cscpi.domain.Networks;


/**
 * 
 * @author pierre
 *
 */
public class UserDataGeneratorImpl implements UserDataGenerator {

	private static Logger logger=LoggerFactory.getLogger(UserDataGeneratorImpl.class.getName());

	
	@Value("${cpi.registry.endpoint}")
	String endpoint;
	
	
	
	/**
	 * see https://github.com/cloudfoundry/bosh-agent/blob/585d2cc3a47129aa875738f09a26101ec6e0b1d1/infrastructure/http_metadata_service.go
	 * see https://github.com/cloudfoundry/bosh/blob/master/bosh_openstack_cpi/lib/cloud/openstack/cloud.rb#L684-L697
	 * see  https://github.com/cloudfoundry/bosh-agent/blob/master/infrastructure/metadata_service_interface.go
	 * for bosh agent http user data expectations
	 * @author poblin
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
			this.nameserver=new ArrayList<String>();
			this.nameserver.add(nameServer);
		}
		public List<String> nameserver;
	}
	
	
	public class UserData{
		Server server;
		Registry registry;
		DNS dns;
	}
	
	
	@Override
	public String userMetadata(String vmName,Networks networks){
		
		UserData datas=new UserData();
		//compose the user data from cpi create vm call
		
		
		try {
			URL url=new URL(this.endpoint);
		} catch (MalformedURLException e) {
			logger.error("Registry endpoint is malformed {} : {}",this.endpoint,e.getMessage());
			throw new IllegalArgumentException("Registry Endpoint is incorrect : "+this.endpoint, e);
		}
		
		datas.registry=new Registry(this.endpoint);
		

		datas.server=new Server(vmName);
		
		List<String> dnsServer=networks.networks.values().iterator().next().dns; //Only 1  network managed
		if (dnsServer.size()==0){
			logger.warn("No DNS configured for vm creation ?");
		} else {
			logger.info("set userData with the following Dns {}",dnsServer.toString() );
		}
		datas.dns=new DNS(dnsServer.get(0)); //FIXME: only set a single DNS server
		
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
