package com.orange.oss.cloudfoundry.cscpi.cep;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.espertech.esper.client.Configuration;
import com.espertech.esper.client.EPServiceProvider;
import com.espertech.esper.client.EPServiceProviderManager;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;


/**
 * CEP Implementation
 * add analysis to cpi events stream
 * detect common patterns (eg: update vm implies sequence detach disk / delete vm / createvm / attach orig disk)
 * @author pierre
 *
 */
public class CEPImpl implements CEPInterface {
	
	private static Logger logger=LoggerFactory.getLogger(CEPImpl.class.getName());
	
	private EPServiceProvider epService;
	
	
	/**
	 * default constructor
	 */
	public CEPImpl(){
		Configuration config = new Configuration();
		config.addEventTypeAutoName("com.orange.oss.cloudfoundry.cscpi.cep");
		
		this.epService = EPServiceProviderManager.getDefaultProvider(config);		

		String expression = "select count(*) from CPIEvent.win:time(30 sec)";
		EPStatement statement = epService.getEPAdministrator().createEPL(expression);
		
		statement.addListener(new UpdateListener(){
			@Override
			public void update(EventBean[] newEvents, EventBean[] oldEvents) {
				logger.info("statement notif new {} olds {}",newEvents.toString());
				
			}
		});
	}

	@Override
	public void sendEvent(CPIEvent event) {
		this.epService.getEPRuntime().sendEvent(event);
	}
	
}
