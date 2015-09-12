package com.orange.oss.cloudfoundry.cscpi;

import java.util.Set;

import org.jclouds.cloudstack.CloudStackApi;
import org.jclouds.cloudstack.domain.NIC;
import org.jclouds.cloudstack.domain.Template;
import org.jclouds.cloudstack.domain.VirtualMachine;
import org.jclouds.cloudstack.domain.Zone;
import org.jclouds.cloudstack.features.TemplateApi;
import org.jclouds.cloudstack.options.ListVirtualMachinesOptions;
import org.jclouds.cloudstack.options.ListZonesOptions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import com.orange.oss.cloudfoundry.cscpi.config.CloudStackConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = BoshCloudstackCpiCoreApplication.class)
@ConfigurationProperties
public class CloudStackIT {

	private static Logger logger=LoggerFactory.getLogger(CloudStackIT.class);
	
	@Autowired
	CloudStackApi api;
	
	@Autowired
	CloudStackConfiguration cloudstackConfig;
	
	@Test
	public void testLogin(){

		TemplateApi templateApi = api.getTemplateApi();
		Set<Template> templates = templateApi.listTemplates();
		for (Template t:templates){
			logger.debug("found template {} ",t.getDisplayText());
			logger.debug("template id {}",t.getId());
		}
	}
	
	@Test
	public void testVmIpList(){
		String zoneId=this.findZoneId();
		Set<VirtualMachine> vms = api.getVirtualMachineApi().listVirtualMachines(ListVirtualMachinesOptions.Builder.zoneId(zoneId));
		for (VirtualMachine vm:vms){
			String name=vm.getName();
			NIC nic=vm.getNICs().iterator().next();
			String ip=nic.getIPAddress();
			String mac=nic.getMacAddress();
			logger.info("vm {}, ip {}, mac {}",name, ip,mac);
		}
	}
	
	private String findZoneId() {
		//TODO: select the exact zone if multiple available
        ListZonesOptions zoneOptions=ListZonesOptions.Builder.available(true);
		Set<Zone> zones = api.getZoneApi().listZones(zoneOptions);
		Assert.notEmpty(zones, "No Zone available");
		Zone zone=zones.iterator().next();
		String zoneId = zone.getId();
		
		Assert.isTrue(zone.getName().equals(this.cloudstackConfig.default_zone));
		
		return zoneId;
	}

	
}

