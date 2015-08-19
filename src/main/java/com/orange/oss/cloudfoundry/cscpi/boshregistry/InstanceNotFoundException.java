package com.orange.oss.cloudfoundry.cscpi.boshregistry;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class InstanceNotFoundException extends RuntimeException {
}
