package watsonservices.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.ibm.watson.developer_cloud.conversation.v1_experimental.model.MessageRequest;
import com.ibm.watson.developer_cloud.conversation.v1_experimental.model.MessageResponse;
import com.ibm.watson.developer_cloud.conversation.v1_experimental.model.MessageResponse.Entity;
import com.ibm.watson.developer_cloud.conversation.v1_experimental.model.MessageResponse.Intent;
import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.MendixException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixIdentifier;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import watsonservices.proxies.ConversationContext;
import watsonservices.proxies.ConversationEntity;
import watsonservices.proxies.ConversationIntent;
import watsonservices.proxies.ConversationMessageResponse;
import watsonservices.proxies.ConversationSetup;
import watsonservices.proxies.DialogNode;

public class ConversationService {

	private static final String WATSON_CONVERSATION_LOGNODE = "WatsonServices.IBM_WatsonConnector_Conversation";
	private static final ILogNode LOGGER = Core.getLogger((Core.getConfiguration().getConstantValue(WATSON_CONVERSATION_LOGNODE).toString()));

	static public IMendixObject sendMessage(IContext context, ConversationContext conversationContext, String input, String username, String password) throws CoreException, MendixException {
		LOGGER.debug("Executing Watson Send Message Connector...");
		
		final  com.ibm.watson.developer_cloud.conversation.v1_experimental.ConversationService service = new com.ibm.watson.developer_cloud.conversation.v1_experimental.ConversationService("2016-07-11");
		service.setUsernameAndPassword(username, password);
		service.setEndPoint("https://gateway.watsonplatform.net/conversation/api");

		MessageRequest messageRequest = createMessageRequest(conversationContext, input);

		ConversationSetup conversationSetup = conversationContext.getConversationContext_ConversationSetup();
		if(conversationSetup == null){
			throw new MendixException("There is no ConversationSetup entity associated to the input ConversationContext");
		}

		MessageResponse response;
		try {
			response = service
					  .message(conversationSetup.getWorkspaceId(), messageRequest)
					  .execute();
		} catch (Exception e) {
			LOGGER.error("Watson Service connection - Failed conversing with Watson in the workspace: " + conversationSetup.getWorkspaceId(), e);
			throw new MendixException(e);
		}

		return createMessageResponse(context, conversationContext, response);
	}

	static private MessageRequest createMessageRequest(ConversationContext conversationContext, String input) throws CoreException {
		MessageRequest messageRequest = null;
		if(StringUtils.isNotEmpty(conversationContext.getConversationId())){
			final Map<String, Object> conversationContextInput = new HashMap<String, Object>();

			List<String> dialogNodes = new ArrayList<String>();
			for(DialogNode node : conversationContext.getDialog_Stack()){
				dialogNodes.add(node.getName());
			}

			final Map<String, Object> contextSystemInput = new HashMap<String, Object>();
			contextSystemInput.put("dialog_stack", dialogNodes);
			contextSystemInput.put("dialog_turn_counter", conversationContext.getDialogTurnCounter());
			contextSystemInput.put("dialog_request_counter", conversationContext.getDialogRequestCounter());

			conversationContextInput.put("system", contextSystemInput);
			conversationContextInput.put("conversation_id", conversationContext.getConversationId());

			messageRequest = new MessageRequest.Builder()
					  .inputText(input)
					  .context(conversationContextInput)
					  .build();
		}
		else{
			messageRequest = new MessageRequest.Builder()
					  .inputText(input)
					  .build();
		}
		return messageRequest;
	}

	static private IMendixObject createMessageResponse(IContext context, ConversationContext conversationContext, MessageResponse response) throws CoreException {
		Map<String, Object> resposeSystemContext = updateConversationContext(context, conversationContext, response);

		final IMendixObject conversationMessageObject = Core.instantiate(context, ConversationMessageResponse.entityName);
		conversationMessageObject.setValue(context, ConversationMessageResponse.MemberNames.ConversationId.toString(), response.getContext().get("conversation_id"));
		conversationMessageObject.setValue(context, ConversationMessageResponse.MemberNames.Input.toString(), response.getInputText());
		conversationMessageObject.setValue(context, ConversationMessageResponse.MemberNames.Output.toString(), response.getTextConcatenated(","));

		Core.commit(context, conversationMessageObject);

		
		List<String> dialogStack = (List<String>) resposeSystemContext.get("dialog_stack");
		for(String dialogNode : dialogStack){
			final IMendixObject dialogNodeObject = Core.instantiate(context, DialogNode.entityName );
			dialogNodeObject.setValue(context, DialogNode.MemberNames.Name.toString(), dialogNode);
			final List<IMendixIdentifier> conversationContextList = new ArrayList<IMendixIdentifier>();
			conversationContextList.add(conversationContext.getMendixObject().getId());
			dialogNodeObject.setValue(context, DialogNode.MemberNames.Dialog_Stack.toString(), conversationContextList);

			Core.commit(context, dialogNodeObject);
		}
		
		

		for(Intent intent : response.getIntents()){
			final IMendixObject conversationIntentObject = Core.instantiate(context, ConversationIntent.entityName);
			conversationIntentObject.setValue(context, ConversationIntent.MemberNames.Name.toString(), intent.getIntent());
			conversationIntentObject.setValue(context, ConversationIntent.MemberNames.Confidence.toString(), intent.getConfidence().toString());
			conversationIntentObject.setValue(context, ConversationIntent.MemberNames.ConversationIntent_ConversationResponse.toString(), conversationMessageObject.getId());

			Core.commit(context, conversationIntentObject);
		}
		
		for(Entity entity : response.getEntities()){
			final IMendixObject conversationEntityObject = Core.instantiate(context, ConversationEntity.entityName);
			conversationEntityObject.setValue(context, ConversationEntity.MemberNames.Name.toString(), entity.getEntity());
			conversationEntityObject.setValue(context, ConversationEntity.MemberNames.Value.toString(), entity.getValue());
			conversationEntityObject.setValue(context, ConversationEntity.MemberNames.ConversationEntity_ConversationResponse.toString(), conversationMessageObject.getId());

			Core.commit(context, conversationEntityObject);
		}

		return conversationMessageObject;
	}

	static private Map<String, Object> updateConversationContext(IContext context, ConversationContext conversationContext, MessageResponse response) throws CoreException {
		conversationContext.setConversationId(context, response.getContext().get("conversation_id").toString());
		Map<String, Object> responseContext = response.getContext();
		Map<String, Object> resposeSystemContext = (Map<String, Object>) responseContext.get("system");
		conversationContext.setDialogTurnCounter(new BigDecimal(resposeSystemContext.get("dialog_turn_counter").toString()));
		conversationContext.setDialogRequestCounter(new BigDecimal(resposeSystemContext.get("dialog_request_counter").toString()));

		conversationContext.commit();
		return resposeSystemContext;
	}
}
