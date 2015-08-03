package com.orange.oss.cloudfoundry.cscpi.webdav;

import java.io.InputStream;



public interface WebdavServerAdapter {

	String pushFile(InputStream is,String name);
}
