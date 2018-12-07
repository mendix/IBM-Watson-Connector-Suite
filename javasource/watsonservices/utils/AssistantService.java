package watsonservices.utils;


import java.util.stream.Collectors;

import com.ibm.watson.developer_cloud.assistant.v2.model.CreateSessionOptions;
import com.ibm.watson.developer_cloud.assistant.v2.model.MessageInput;
import com.ibm.watson.developer_cloud.assistant.v2.model.MessageOptions;
import com.ibm.watson.developer_cloud.assistant.v2.model.MessageResponse;
import com.ibm.watson.developer_cloud.assistant.v2.model.RuntimeEntity;
import com.ibm.watson.developer_cloud.assistant.v2.model.RuntimeIntent;
import com.ibm.watson.developer_cloud.assistant.v2.model.SessionResponse;
import com.ibm.watson.developer_cloud.service.security.IamOptions;
import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.MendixException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import watsonservices.proxies.Assistant;
import watsonservices.proxies.ConversationEntity;
import watsonservices.proxies.ConversationIntent;
import watsonservices.proxies.ConversationMessageResponse;
import watsonservices.proxies.SessionContext;

public class AssistantService {

	private static final String WATSON_CONVERSATION_LOGNODE = "WatsonServices.IBM_WatsonConnector_Conversation";
	private static final ILogNode LOGGER = Core.getLogger((Core.getConfiguration().getConstantValue(WATSON_CONVERSATION_LOGNODE).toString()));
	private static final String WATSON_ASSISTANT_VERSION_DATE = "2018-11-08";

	public static IMendixObject createSession(IContext context, Assistant assistant, String apikey, String url) throws CoreException, MendixException {
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

	public static IMendixObject sendMessage(IContext context, SessionContext sessionContext, String input, String apikey, String url) throws CoreException, MendixException {
		LOGGER.debug("Executing Watson Send Message Connector...");

		final com.ibm.watson.developer_cloud.assistant.v2.Assistant service = createService(apikey, url);

		Assistant assistant = sessionContext.getSessionContext_Assistant();

		MessageInput messageInput = new MessageInput.Builder()
				.text(input)
				.build();

		MessageOptions options = new MessageOptions.Builder(assistant.getAssistantId(), sessionContext.getSessionId())
				  .input(messageInput)
				  .build();

		final MessageResponse response;
		try {
			response = service.message(options).execute();
		} catch(Exception ex) {
			LOGGER.error("Watson Service connection - Failed conversing with Watson with assistantID " + assistant.getAssistantId() +
					" and conversationID " + sessionContext.getSessionId(), ex);
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
		final IMendixObject messageResponseObject = Core.instantiate(context, ConversationMessageResponse.entityName);
		messageResponseObject.setValue(context, ConversationMessageResponse.MemberNames.SessionId.toString(), sessionContext.getSessionId());
		messageResponseObject.setValue(context, ConversationMessageResponse.MemberNames.Input.toString(), input);
		messageResponseObject.setValue(context, ConversationMessageResponse.MemberNames.Output.toString(),
				response.getOutput().getGeneric().stream().map(rg -> rg.getText()).collect(Collectors.joining(",")));

		Core.commit(context, messageResponseObject);

		for(RuntimeIntent intent : response.getOutput().getIntents()){
			final IMendixObject intentObject = Core.instantiate(context, ConversationIntent.entityName);
			intentObject.setValue(context, ConversationIntent.MemberNames.Name.toString(), intent.getIntent());
			intentObject.setValue(context, ConversationIntent.MemberNames.Confidence.toString(), intent.getConfidence().toString());
			intentObject.setValue(context, ConversationIntent.MemberNames.ConversationIntent_ConversationResponse.toString(), messageResponseObject.getId());

			Core.commit(context, intentObject);
		}

		for(RuntimeEntity entity : response.getOutput().getEntities()){
			final IMendixObject entityObject = Core.instantiate(context, ConversationEntity.entityName);
			entityObject.setValue(context, ConversationEntity.MemberNames.Name.toString(), entity.getEntity());
			entityObject.setValue(context, ConversationEntity.MemberNames.Value.toString(), entity.getValue());
			entityObject.setValue(context, ConversationEntity.MemberNames.ConversationEntity_ConversationResponse.toString(), messageResponseObject.getId());

			Core.commit(context, entityObject);
		}

		return messageResponseObject;
	}

}
