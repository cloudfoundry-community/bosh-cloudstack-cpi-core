package com.orange.oss.cloudfoundry.cscpi.cep;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.neo4j.cypher.internal.compiler.v2_1.perty.docbuilders.toStringDocBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.espertech.esper.client.soda.CountEverProjectionExpression;


/**
 * AspectJ mechanism.
 * Intercepts cpi logic invocation, collects events, and enrich some cpi invocation accordingly.
 * The aim is to give some context to cloudstack call (eg : try to colocate vm and disk on same host, for localstorage).
 * This mechanism compensate the lock of hint from  bosh director  see 
 * @author pierre
 *
 */
@Component
@Aspect
public class CEPHintAspect {

	private static Logger logger = LoggerFactory.getLogger(CEPHintAspect.class.getName());

	@Autowired
	private CEPInterface cep;
	
	
	/**
	 * catch successfull action when done by CPI Logic (no exception)
	 * @param joinPoint
	 * @param result
	 */
	@AfterReturning(pointcut = "execution(* com.orange.oss.cloudfoundry..*CPIImpl.*(..))", returning = "result")
	public void logAfterReturning(JoinPoint joinPoint, Object result) {
		// get disk_id / vm_id
		if (joinPoint.getSignature().getName().equals("create_disk")){
			logger.info("intercept event create_disk");
			
			CPIEvent event=new CPIEvent("create_disk",joinPoint.getArgs()[0].toString(), null);
			cep.sendEvent(event);
		}

	}

	/**
	 * enrich CPI order with some disk/vm hint
	 * 
	 * @param joinPoint
	 * @param result
	 */
//
//	@Before(pointcut="execution(* com.orange.oss.cloudfoundry..*CPIImpl.*(..))")
//	public void addHint(JoinPoint joinPoint, Object result) {
//
//	}

	
	
	
	
	
	
}
