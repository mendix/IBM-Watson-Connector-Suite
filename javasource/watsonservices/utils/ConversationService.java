package watsonservices.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.ibm.watson.developer_cloud.conversation.v1.*;
import com.ibm.watson.developer_cloud.conversation.v1.model.*;
import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.MendixException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixIdentifier;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import watsonservices.proxies.Conversation;
import watsonservices.proxies.ConversationContext;
import watsonservices.proxies.ConversationEntity;
import watsonservices.proxies.ConversationIntent;
import watsonservices.proxies.ConversationMessageResponse;
import watsonservices.proxies.DialogNode;

public class ConversationService {

	private static final String WATSON_CONVERSATION_LOGNODE = "WatsonServices.IBM_WatsonConnector_Conversation";
	private static final ILogNode LOGGER = Core.getLogger((Core.getConfiguration().getConstantValue(WATSON_CONVERSATION_LOGNODE).toString()));
	private static final com.ibm.watson.developer_cloud.conversation.v1.ConversationService service = new com.ibm.watson.developer_cloud.conversation.v1.ConversationService("2016-07-11");

	public static IMendixObject sendMessage(IContext context, ConversationContext conversationContext, String input, String username, String password) throws CoreException, MendixException {
		LOGGER.debug("Executing Watson Send Message Connector...");

		service.setUsernameAndPassword(username, password);
		service.setEndPoint("https://gateway.watsonplatform.net/conversation/api");

		MessageRequest messageRequest = createMessageRequest(conversationContext, input);

		Conversation conversation = conversationContext.getConversationContext_Conversation();
		if(conversation == null){
			throw new MendixException("There is no Conversation entity associated to the input ConversationContext");
		}

		MessageResponse response;
		try {
			response = service
					  .message(conversation.getWorkspaceId(), messageRequest)
					  .execute();
		} catch (Exception e) {
			
			if(StringUtils.isNotEmpty(conversationContext.getConversationId())){
				LOGGER.error("Watson Service connection - Failed conversing with Watson with workspaceID " + conversation.getWorkspaceId() +
						" and conversationID " + conversationContext.getConversationId(), e);
			}
			else{
				LOGGER.error("Watson Service connection - Failed conversing with Watson in the workspaceID " + conversation.getWorkspaceId(), e);
			}
			throw new MendixException(e);
		}

		return createMessageResponse(context, conversationContext, response);
	}

	private static MessageRequest createMessageRequest(ConversationContext conversationContext, String input) throws CoreException {
		MessageRequest messageRequest = null;
		if(StringUtils.isNotEmpty(conversationContext.getConversationId())){
			final Map<String, Object> conversationContextInput = new HashMap<String, Object>();

			List<String> dialogNodes = new ArrayList<String>();
			for(DialogNode node : conversationContext.getDialog_Stack()){
				dialogNodes.add(node.getName());
			}

			final Map<String, Object> contextSystemInput = new HashMap<String, Object>();
			contextSystemInput.put("dialog_stack", dialogNodes);		
			conversationContextInput.put("system", contextSystemInput);
			conversationContextInput.put("conversation_id", conversationContext.getConversationId());
			conversationContextInput.put("dialog_turn_counter", conversationContext.getDialogTurnCounter());
			conversationContextInput.put("dialog_request_counter", conversationContext.getDialogRequestCounter());

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

	private static IMendixObject createMessageResponse(IContext context, ConversationContext conversationContext, MessageResponse response) throws CoreException {
		updateConversationContext(context, conversationContext, response);

		final IMendixObject messageResponseObject = Core.instantiate(context, ConversationMessageResponse.entityName);
		messageResponseObject.setValue(context, ConversationMessageResponse.MemberNames.ConversationId.toString(), response.getContext().get("conversation_id"));
		messageResponseObject.setValue(context, ConversationMessageResponse.MemberNames.Input.toString(), response.getInputText());
		messageResponseObject.setValue(context, ConversationMessageResponse.MemberNames.Output.toString(), response.getTextConcatenated(","));

		Core.commit(context, messageResponseObject);

		

		for(Intent intent : response.getIntents()){
			final IMendixObject intentObject = Core.instantiate(context, ConversationIntent.entityName);
			intentObject.setValue(context, ConversationIntent.MemberNames.Name.toString(), intent.getIntent());
			intentObject.setValue(context, ConversationIntent.MemberNames.Confidence.toString(), intent.getConfidence().toString());
			intentObject.setValue(context, ConversationIntent.MemberNames.ConversationIntent_ConversationResponse.toString(), messageResponseObject.getId());

			Core.commit(context, intentObject);
		}
		
		for(Entity entity : response.getEntities()){
			final IMendixObject entityObject = Core.instantiate(context, ConversationEntity.entityName);
			entityObject.setValue(context, ConversationEntity.MemberNames.Name.toString(), entity.getEntity());
			entityObject.setValue(context, ConversationEntity.MemberNames.Value.toString(), entity.getValue());
			entityObject.setValue(context, ConversationEntity.MemberNames.ConversationEntity_ConversationResponse.toString(), messageResponseObject.getId());

			Core.commit(context, entityObject);
		}

		return messageResponseObject;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> updateConversationContext(IContext context, ConversationContext conversationContext, MessageResponse response) throws CoreException {
		deleteCurrentDialogStack(conversationContext);

		conversationContext.setConversationId(context, response.getContext().get("conversation_id").toString());
		Map<String, Object> responseContext = response.getContext();
		Map<String, Object> resposeSystemContext = (Map<String, Object>) responseContext.get("system");
		conversationContext.setDialogTurnCounter(new BigDecimal(resposeSystemContext.get("dialog_turn_counter").toString()));
		conversationContext.setDialogRequestCounter(new BigDecimal(resposeSystemContext.get("dialog_request_counter").toString()));

		List<String> dialogStack = (List<String>) resposeSystemContext.get("dialog_stack");
		for(String dialogNode : dialogStack){
			final IMendixObject dialogNodeObject = Core.instantiate(context, DialogNode.entityName );
			dialogNodeObject.setValue(context, DialogNode.MemberNames.Name.toString(), dialogNode);
			final List<IMendixIdentifier> conversationContextList = new ArrayList<IMendixIdentifier>();
			conversationContextList.add(conversationContext.getMendixObject().getId());
			dialogNodeObject.setValue(context, DialogNode.MemberNames.Dialog_Stack.toString(), conversationContextList);

			Core.commit(context, dialogNodeObject);
		}

		conversationContext.commit();
		return resposeSystemContext;
	}

	private static void deleteCurrentDialogStack(ConversationContext conversationContext) throws CoreException {
		for(DialogNode dialogNode : conversationContext.getDialog_Stack()){
			dialogNode.delete();
		}
	}
}
