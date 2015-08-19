package com.orange.oss.cloudfoundry.cscpi;

import com.orange.oss.cloudfoundry.cscpi.domain.Networks;

public interface UserDataGenerator {

	String vmMetaData(String vmName, Networks networks);

}
