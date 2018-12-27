package watsonservices.utils;


import java.util.stream.Collectors;

import com.ibm.watson.developer_cloud.assistant.v2.model.CreateSessionOptions;
import com.ibm.watson.developer_cloud.assistant.v2.model.MessageInput;
import com.ibm.watson.developer_cloud.assistant.v2.model.MessageOptions;
import com.ibm.watson.developer_cloud.assistant.v2.model.MessageResponse;
import com.ibm.watson.developer_cloud.assistant.v2.model.RuntimeEntity;
import com.ibm.watson.developer_cloud.assistant.v2.model.RuntimeIntent;
import com.ibm.watson.developer_cloud.assistant.v2.model.SessionResponse;
import com.ibm.watson.developer_cloud.service.exception.NotFoundException;
import com.ibm.watson.developer_cloud.service.security.IamOptions;
import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.MendixException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import watsonservices.proxies.Assistant;
import watsonservices.proxies.AssistantEntity;
import watsonservices.proxies.AssistantIntent;
import watsonservices.proxies.AssistantMessageResponse;
import watsonservices.proxies.SessionContext;

public class AssistantService {

	private static final String WATSON_ASSISTANT_LOGNODE = "WatsonServices.IBM_WatsonConnector_Assistant";
	private static final ILogNode LOGGER = Core.getLogger((Core.getConfiguration().getConstantValue(WATSON_ASSISTANT_LOGNODE).toString()));
	private static final String WATSON_ASSISTANT_VERSION_DATE = "2018-11-08";

	public static IMendixObject createSession(IContext context, Assistant assistant, String apikey, String url) throws MendixException {
		LOGGER.debug("Executing Watson Create Session Connector...");

		final com.ibm.watson.developer_cloud.assistant.v2.Assistant service = createService(apikey, url);

		final CreateSessionOptions options = new CreateSessionOptions.Builder()
				.assistantId(assistant.getAssistantId())
				.build();

		final SessionResponse session;
		try {
			session = service
					.createSession(options)
					.execute();
		} catch(Exception ex) {
			LOGGER.error("Failed to create a new Assistant Session", ex);
			throw new MendixException(ex);
		}

		final SessionContext result = new SessionContext(context);
		result.setSessionId(session.getSessionId());

		return result.getMendixObject();
	}

	public static IMendixObject sendMessage(IContext context, SessionContext sessionContext, String input, String apikey, String url) throws MendixException {
		LOGGER.debug("Executing Watson Send Message Connector...");

		final com.ibm.watson.developer_cloud.assistant.v2.Assistant service = createService(apikey, url);

		Assistant assistant = sessionContext.getSessionContext_Assistant();

		MessageInput messageInput = new MessageInput.Builder()
				.text(input)
				.build();

		MessageOptions options = new MessageOptions.Builder(assistant.getAssistantId(), sessionContext.getSessionId())
				  .input(messageInput)
				  .build();

		MessageResponse response;
		try {
			response = service.message(options).execute();
		} catch (NotFoundException ex) {
			LOGGER.error("Watson Service connection - Session with Watson with assistantID " + assistant.getAssistantId() +
					" and sessionID " + sessionContext.getSessionId() + " not found", ex);
			response = null;
		} catch(Exception ex) {
			LOGGER.error("Watson Service connection - Failed conversing with Watson with assistantID " + assistant.getAssistantId() +
					" and sessionID " + sessionContext.getSessionId(), ex);
			throw new MendixException(ex);
		}

		try {
			return createMessageResponse(context, sessionContext, input, response);
		} catch(Exception ex) {
			LOGGER.error("Watson Service connection - Failed to build response message", ex);
			throw new MendixException(ex);
		}
	}

	private static com.ibm.watson.developer_cloud.assistant.v2.Assistant createService(String apikey, String url) {
		final IamOptions iamOptions = new IamOptions.Builder()
				.apiKey(apikey)
				.build();
		final com.ibm.watson.developer_cloud.assistant.v2.Assistant service = new com.ibm.watson.developer_cloud.assistant.v2.Assistant(WATSON_ASSISTANT_VERSION_DATE, iamOptions);
		service.setEndPoint(url);
		return service;
	}

	private static IMendixObject createMessageResponse(IContext context, SessionContext sessionContext, String input, MessageResponse response) throws CoreException {
		final IMendixObject messageResponseObject = Core.instantiate(context, AssistantMessageResponse.entityName);

		if (response == null) {
			Core.commit(context, messageResponseObject);
			return messageResponseObject;
		}

		messageResponseObject.setValue(context, AssistantMessageResponse.MemberNames.SessionId.toString(), sessionContext.getSessionId());
		messageResponseObject.setValue(context, AssistantMessageResponse.MemberNames.Input.toString(), input);
		messageResponseObject.setValue(context, AssistantMessageResponse.MemberNames.Output.toString(),
				response.getOutput().getGeneric().stream().map(rg -> rg.getText()).collect(Collectors.joining(",")));

		Core.commit(context, messageResponseObject);

		for(RuntimeIntent intent : response.getOutput().getIntents()){
			final IMendixObject intentObject = Core.instantiate(context, AssistantIntent.entityName);
			intentObject.setValue(context, AssistantIntent.MemberNames.Name.toString(), intent.getIntent());
			intentObject.setValue(context, AssistantIntent.MemberNames.Confidence.toString(), intent.getConfidence().toString());
			intentObject.setValue(context, AssistantIntent.MemberNames.AssistantIntent_AssistantResponse.toString(), messageResponseObject.getId());

			Core.commit(context, intentObject);
		}

		for(RuntimeEntity entity : response.getOutput().getEntities()){
			final IMendixObject entityObject = Core.instantiate(context, AssistantEntity.entityName);
			entityObject.setValue(context, AssistantEntity.MemberNames.Name.toString(), entity.getEntity());
			entityObject.setValue(context, AssistantEntity.MemberNames.Value.toString(), entity.getValue());
			entityObject.setValue(context, AssistantEntity.MemberNames.AssistantEntity_AssistantResponse.toString(), messageResponseObject.getId());

			Core.commit(context, entityObject);
		}

		return messageResponseObject;
	}

}
