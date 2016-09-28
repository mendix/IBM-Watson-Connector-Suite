package watsonservices.utils;

import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;

import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.AudioFormat;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.Voice;
import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.MendixException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import watsonservices.proxies.Speech;
import watsonservices.proxies.VoiceEnum;

public class TextToSpeechService {

	private static final String WATSON_TEXT_TO_SPEECH_LOGNODE = "WatsonServices.IBM_WatsonConnector_TextToSpeech";
	private static final ILogNode LOGGER = Core.getLogger(Core.getConfiguration().getConstantValue(WATSON_TEXT_TO_SPEECH_LOGNODE).toString());
	private static final TextToSpeech service = new TextToSpeech();

	public static IMendixObject Synthesize(IContext context, String text, VoiceEnum voiceEnumParameter, String username, String password) throws MendixException {
		LOGGER.debug("Executing Synthetize Connector...");

		service.setUsernameAndPassword(username, password);

		final Voice voice = getVoice(voiceEnumParameter);

		InputStream stream;
		try {
			stream = service.synthesize(text, voice, AudioFormat.OGG).execute();
		} catch (Exception e) {
			LOGGER.error("Watson Service Connection - Failed text to speech: " + StringUtils.abbreviate(text, 20), e);
			throw new MendixException(e);
		}

		final IMendixObject speechObject = Core.instantiate(context, Speech.entityName);
		Core.storeFileDocumentContent(context, speechObject, stream);

		return speechObject;
	}

	private static Voice getVoice(VoiceEnum parameter) throws MendixException {
		Voice voice = null;
		
		switch(parameter){
			case DE_DIETER:
				voice = Voice.DE_DIETER;
				break;
			case EN_ALLISON:
				voice = Voice.EN_ALLISON;
				break;
			case EN_LISA:
				voice = Voice.EN_LISA;
				break;
			case ES_ENRIQUE:
				voice = Voice.ES_ENRIQUE;
				break;
			case ES_LAURA:
				voice = Voice.ES_LAURA;
				break;
			case ES_SOFIA:
				voice = Voice.ES_SOFIA;
			case FR_RENEE:
				voice = Voice.FR_RENEE;
				break;
			case GB_KATE:
				voice = Voice.GB_KATE;
				break;
			case IT_FRANCESCA:
				voice = Voice.IT_FRANCESCA;
				break;
		default:
			break;
		}

		if(voice == null){
			throw new MendixException("The supplied parameter doesn't correspond to any voice: " + parameter);
		}

		return voice;
	}

}
