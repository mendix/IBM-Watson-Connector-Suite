package watsonservices.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import com.ibm.watson.developer_cloud.dialog.v1.model.Dialog;
import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.MendixException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import system.proxies.FileDocument;
import watsonservices.proxies.Conversation;
import watsonservices.proxies.ExistingDialog;
import watsonservices.proxies.Message;

public class DialogService {

	private static final String WATSON_DIALOG_LOGNODE = "WatsonServices.IBM_WatsonConnector_Dialog";
	private static final ILogNode LOGGER = Core.getLogger((Core.getConfiguration().getConstantValue(WATSON_DIALOG_LOGNODE).toString()));

	public static String createDialog(IContext context, String dialogName, FileDocument dialogContent, String username, String password) throws MendixException {
		LOGGER.debug("Executing CreateDialog Connector...");

		final com.ibm.watson.developer_cloud.dialog.v1.DialogService dialogService = new com.ibm.watson.developer_cloud.dialog.v1.DialogService();
		dialogService.setUsernameAndPassword(username, password);

		final File dialogTemplateFile = new File(Core.getConfiguration().getTempPath() + dialogName);
		try(final InputStream is = Core.getFileDocumentContent(context, dialogContent.getMendixObject())){

			Files.copy(is, dialogTemplateFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}catch(IOException e){
			LOGGER.error("There was a problem with the template: " + dialogTemplateFile.getPath(), e);
			throw new MendixException(e);
		}

		Dialog dialog = null;
		try{
			dialog = dialogService.createDialog(dialogName, dialogTemplateFile).execute();
			
		}catch(Exception e){
			LOGGER.error("Watson Service connection - Failed creating the template: " + dialogName, e);	
			throw new MendixException(e);
		}finally{
			dialogTemplateFile.delete();
		}

		return dialog.getId();
	}

	public static String updateDialog(IContext context, String dialogId, String dialogName, FileDocument dialogContent, String username, String password) throws MendixException {
		LOGGER.debug("Executing CreateDialog Connector...");
		
		final com.ibm.watson.developer_cloud.dialog.v1.DialogService service = new com.ibm.watson.developer_cloud.dialog.v1.DialogService();
		service.setUsernameAndPassword(username, password);
		
		//Create temporary file to easily upload the script
		final File dialogTemplateFile = new File(Core.getConfiguration().getTempPath() + dialogName);
		try(InputStream is = Core.getFileDocumentContent(context, dialogContent.getMendixObject())){
			
			Files.copy(is, dialogTemplateFile.toPath(), StandardCopyOption.REPLACE_EXISTING);	
		}catch(IOException e){
			LOGGER.error("There was a problem with the template: " + dialogTemplateFile.getPath(), e);
			throw new MendixException(e);
		}

		try{

			service.updateDialog(dialogId, dialogTemplateFile).execute();
		}catch(Exception e){
			LOGGER.error("Watson Service connection - Failed updating the template: " + dialogName, e);	
			throw new MendixException(e);
		}finally{
			dialogTemplateFile.delete();
		}		

		return null;
	}

	public static List<IMendixObject> getDialogs(IContext context, String username, String password) throws MendixException, CoreException {
		LOGGER.debug("Executing GetDialogs Connector...");

		final com.ibm.watson.developer_cloud.dialog.v1.DialogService service = new com.ibm.watson.developer_cloud.dialog.v1.DialogService();
		service.setUsernameAndPassword(username, password);

		List<Dialog> dialogs = null;
		try{

			dialogs = service.getDialogs().execute();			
		}catch(Exception e){
			LOGGER.error("Watson Service connection - Failed fetching the list of dialogs", e);
			throw new MendixException(e);
		}

		//Create output
		final List<IMendixObject> result = new ArrayList<IMendixObject>();
		for (Dialog dialog : dialogs) {
			IMendixObject existingDialogObject = Core.instantiate(context, ExistingDialog.entityName);
			ExistingDialog newExistingDialogObject = ExistingDialog.load(context, existingDialogObject.getId());
			newExistingDialogObject.setDialogID(dialog.getId());
			newExistingDialogObject.setDialogName(dialog.getName());
			result.add(newExistingDialogObject.getMendixObject());
		}
		return result;
	}

	public static IMendixObject converse(IContext context, String message, Conversation conversation, String username, String password) throws CoreException, MendixException {
		LOGGER.debug("Executing Converse Connector...");
		
		final com.ibm.watson.developer_cloud.dialog.v1.DialogService service = new com.ibm.watson.developer_cloud.dialog.v1.DialogService();
		service.setUsernameAndPassword(username, password);
		
		final com.ibm.watson.developer_cloud.dialog.v1.model.Conversation conv = new com.ibm.watson.developer_cloud.dialog.v1.model.Conversation();
		conv.setDialogId(conversation.getConversation_Dialog().getDialogID());
		
		if(conversation.getClientID() != null){
			conv.setClientId(conversation.getClientID());
		}
		if(conversation.getConversationID() != null){
			conv.setId(conversation.getConversationID());
		}
		
		com.ibm.watson.developer_cloud.dialog.v1.model.Conversation response = null;
		try{
			
			 response = service.converse(conv, message).execute();
		}catch(Exception e){
			LOGGER.error("Watson Service connection - Failed conversing with Watson in the conversation: " + conv.getId(), e);
			throw new MendixException(e);
		}
		//Update conversation if this is a new conversation
		if (conversation.getConversationID() == null) {
			conversation.setClientID(response.getClientId());
			conversation.setConversationID(response.getId());
			conversation.commit();
		}	

		// Create a message object
		final IMendixObject messageObject = Core.instantiate(context, Message.entityName);
		
		//Update message
		String completeString = "";
		List<String> output = response.getResponse();
		for (String string : output) {
			if (completeString == "") {
				completeString = string;
			} else {
				completeString = completeString + "\r\n" + string;
			}
		}

		messageObject.setValue(context, Message.MemberNames.Output.toString(), completeString);
		messageObject.setValue(context, Message.MemberNames.Input.toString(), message);
		messageObject.setValue(context, Message.MemberNames.Message_Conversation.toString(), conversation.getMendixObject().getId());

		Core.commit(context, messageObject);

		return messageObject;
	}
}
