package com.orange.oss.cloudfoundry.cspi.cloudstack;

import java.util.Map;

public interface NativeCloudstackConnector {

	String nativeCall(String command,Map<String, String> apiParameters);

}
