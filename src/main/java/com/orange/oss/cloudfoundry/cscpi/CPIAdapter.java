package com.orange.oss.cloudfoundry.cscpi;

import com.fasterxml.jackson.databind.JsonNode;
import com.orange.oss.cloudfoundry.cscpi.domain.CPIResponse;

/**
 * Adapter interface (convert REST controler payload to correct CPI Implementation)
 * @author pierre
 *
 */
public interface CPIAdapter {

	public CPIResponse execute (JsonNode json);
}
