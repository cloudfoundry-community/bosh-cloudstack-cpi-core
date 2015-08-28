package com.orange.oss.cloudfoundry.cscpi;

import com.orange.oss.cloudfoundry.cscpi.domain.Networks;

public interface UserDataGenerator {

	String userMetadata(String vmName, Networks networks);

}
