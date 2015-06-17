package com.orange.oss.cloudfoundry.cscpi;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = BoshCloudstackCpiCoreApplication.class)
@ConfigurationProperties

public class CPIImplITTest {

	@Test
	public void testCreate_vm() {
		fail("Not yet implemented");
	}

	@Test
	public void testInitialize() {
		fail("Not yet implemented");
	}

	@Test
	public void testCurrent_vm_id() {
		fail("Not yet implemented");
	}

	@Test
	public void testCreate_stemcell() {
		fail("Not yet implemented");
	}

	@Test
	public void testDelete_stemcell() {
		fail("Not yet implemented");
	}

	@Test
	public void testDelete_vm() {
		fail("Not yet implemented");
	}

	@Test
	public void testHas_vm() {
		fail("Not yet implemented");
	}

	@Test
	public void testHas_disk() {
		fail("Not yet implemented");
	}

	@Test
	public void testReboot_vm() {
		fail("Not yet implemented");
	}

	@Test
	public void testSet_vm_metadata() {
		fail("Not yet implemented");
	}

	@Test
	public void testConfigure_networks() {
		fail("Not yet implemented");
	}

	@Test
	public void testCreate_disk() {
		fail("Not yet implemented");
	}

	@Test
	public void testDelete_disk() {
		fail("Not yet implemented");
	}

	@Test
	public void testAttach_disk() {
		fail("Not yet implemented");
	}

	@Test
	public void testSnapshot_disk() {
		fail("Not yet implemented");
	}

	@Test
	public void testDelete_snapshot() {
		fail("Not yet implemented");
	}

	@Test
	public void testDetach_disk() {
		fail("Not yet implemented");
	}

	@Test
	public void testGet_disks() {
		fail("Not yet implemented");
	}

}
