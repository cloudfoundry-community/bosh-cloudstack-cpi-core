package com.orange.oss.cloudfoundry.cscpi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;

import com.orange.oss.cloudfoundry.cscpi.domain.CPIRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.core.Pollers;
import org.springframework.integration.json.JsonToObjectTransformer;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.integration.stream.CharacterStreamReadingMessageSource;
import org.springframework.integration.stream.CharacterStreamWritingMessageHandler;
import org.springframework.messaging.MessageChannel;

@Configuration
@EnableIntegration
public class CLIIntegrationConfiguration {

	@Bean
	public MessageSource<String> stdinMessageSource() {
		Reader reader=new BufferedReader(new InputStreamReader(System.in));    
		return new CharacterStreamReadingMessageSource(reader);
	}

	@Bean
	public IntegrationFlow pollingFlow() {
		return IntegrationFlows
				.from(stdinMessageSource(),
						c -> c.poller(Pollers.fixedRate(100)
								.maxMessagesPerPoll(1)))
				.transform(new JsonToObjectTransformer(CPIRequest.class))
				.transform(new ObjectToJsonTransformer())
				.handle(CharacterStreamWritingMessageHandler.stdout())
				.get();
	}

	@Bean
	@Description("Entry to the messaging system through the gateway.")
	public MessageChannel requestChannel() {
		return new DirectChannel();
	}


}
