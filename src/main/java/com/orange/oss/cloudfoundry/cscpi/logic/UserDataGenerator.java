package com.orange.oss.cloudfoundry.cscpi.logic;

import com.orange.oss.cloudfoundry.cscpi.domain.Networks;

public interface UserDataGenerator {

	String userMetadata(String vmName, Networks networks);

}
