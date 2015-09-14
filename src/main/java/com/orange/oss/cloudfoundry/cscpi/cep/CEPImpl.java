package com.orange.oss.cloudfoundry.cscpi.cep;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;

/**
 * CEP Implementation add analysis to cpi events stream detect common patterns
 * (eg: update vm implies sequence detach disk / delete vm / createvm / attach
 * orig disk)
 * 
 * @author poblin
 *
 */
public class CEPImpl implements CEPInterface {

	private static Logger logger = LoggerFactory.getLogger(CEPImpl.class.getName());

	private EPServiceProvider epService;

	/**
	 * default constructor
	 */
	public CEPImpl() {
		Configuration config = new Configuration();
		config.addEventTypeAutoName("com.orange.oss.cloudfoundry.cscpi.cep");

		this.epService = EPServiceProviderManager.getDefaultProvider(config);
		String patternRecreateVmExpression = "select detach.vmId,detach.diskId from pattern [every detach=CPIDetachDiskOK -> (CPIDeleteVmOK(vmId=detach.vmId)) where timer:within(5 min)]";

		EPStatement statement = epService.getEPAdministrator().createEPL(patternRecreateVmExpression);

		statement.addListener(new UpdateListener() {
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {

				for (EventBean event : newEvents) {
					// this reads the element selected in the esper cep request
					Map<String, Object> underlying = (Map<String, Object>) event.getUnderlying();
					String diskId = (String) underlying.get("detach.diskId");
					String origVmId = (String) underlying.get("detach.vmId");
					logger.info("pattern detected : vm recreation from {} with persistent disk {}", origVmId, diskId);
				}

			}
		});
	}

	@Override
	public void sendEvent(CPIEvent event) {
		this.epService.getEPRuntime().sendEvent(event);
	}

}
