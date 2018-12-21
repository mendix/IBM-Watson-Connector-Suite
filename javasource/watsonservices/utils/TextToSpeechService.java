package watsonservices.utils;

import java.io.InputStream;

import com.ibm.watson.developer_cloud.service.security.IamOptions;
import org.apache.commons.lang3.StringUtils;

import com.ibm.watson.developer_cloud.text_to_speech.v1.TextToSpeech;
import com.ibm.watson.developer_cloud.text_to_speech.v1.model.SynthesizeOptions;
import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.MendixException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import watsonservices.proxies.AudioFormats_TextToSpeech;
import watsonservices.proxies.Speech;
import watsonservices.proxies.VoiceEnum;

public class TextToSpeechService {

	private static final String WATSON_TEXT_TO_SPEECH_LOGNODE = "WatsonServices.IBM_WatsonConnector_TextToSpeech";
	private static final ILogNode LOGGER = Core.getLogger(Core.getConfiguration().getConstantValue(WATSON_TEXT_TO_SPEECH_LOGNODE).toString());

	public static IMendixObject Synthesize(IContext context, String text, VoiceEnum voiceEnumParameter, AudioFormats_TextToSpeech audioFormatEnum, String apiKey, String url) throws MendixException {
		LOGGER.debug("Executing Synthetize Connector...");

		IamOptions iamOptions = new IamOptions.Builder()
				.apiKey(apiKey)
				.build();
		final TextToSpeech service = new TextToSpeech(iamOptions);
		service.setEndPoint(url);

		final String voice = getVoice(voiceEnumParameter);

		final String accept = getAcceptAudioFormat(audioFormatEnum);

		final SynthesizeOptions options = new SynthesizeOptions.Builder()
				.text(text)
				.voice(voice)
				.accept(accept)
				.build();

		InputStream stream;
		try {
			stream = service.synthesize(options).execute();
		} catch (Exception e) {
			LOGGER.error("Watson Service Connection - Failed text to speech: " + StringUtils.abbreviate(text, 20), e);
			throw new MendixException(e);
		}

		final IMendixObject speechObject = Core.instantiate(context, Speech.entityName);
		final String filename = "Speech" + getFileExtension(audioFormatEnum);
		Core.storeFileDocumentContent(context, speechObject, filename, stream);

		return speechObject;
	}

	private static String getAcceptAudioFormat(AudioFormats_TextToSpeech audioFormatEnum) {
		if (audioFormatEnum == null) {
			return SynthesizeOptions.Accept.AUDIO_OGG;
		}
		switch (audioFormatEnum) {
		case BASIC:
			return SynthesizeOptions.Accept.AUDIO_BASIC;
		case FLAC:
			return SynthesizeOptions.Accept.AUDIO_FLAC;
		case OGG:
			return SynthesizeOptions.Accept.AUDIO_OGG;
		case OGG_VORBIS:
			return SynthesizeOptions.Accept.AUDIO_OGG_CODECS_VORBIS;
		case OGG_OPUS:
			return SynthesizeOptions.Accept.AUDIO_OGG_CODECS_OPUS;
		case WAV:
			return SynthesizeOptions.Accept.AUDIO_WAV;
		case L16:
			return SynthesizeOptions.Accept.AUDIO_L16;
		case MP3:
			return SynthesizeOptions.Accept.AUDIO_MP3;
		case MPEG:
			return SynthesizeOptions.Accept.AUDIO_MPEG;
		case WEBM:
			return SynthesizeOptions.Accept.AUDIO_WEBM;
		case WEBM_OPUS:
			return SynthesizeOptions.Accept.AUDIO_WEBM_CODECS_OPUS;
		case WEBM_VORBIS:
			return SynthesizeOptions.Accept.AUDIO_WEBM_CODECS_VORBIS;
		default:
			return SynthesizeOptions.Accept.AUDIO_OGG;
		}
	}

	private static String getFileExtension(AudioFormats_TextToSpeech audioFormatEnum) {
		if (audioFormatEnum == null) {
			return ".ogg";
		}
		switch (audioFormatEnum) {
		case BASIC:
			return ".basic";
		case FLAC:
			return ".flac";
		case OGG:
		case OGG_VORBIS:
		case OGG_OPUS:
			return ".ogg";
		case WAV:
			return ".wav";
		case L16:
			return ".aiff";
		case MP3:
			return ".mp3";
		case MPEG:
			return ".mpeg";
		case WEBM:
		case WEBM_OPUS:
		case WEBM_VORBIS:
			return ".webm";
		default:
			return ".ogg";
		}
	}

	private static String getVoice(VoiceEnum parameter) throws MendixException {
		String voice = null;
		
		switch(parameter){
			case DE_DE_BIRGIT:
				voice = SynthesizeOptions.Voice.DE_DE_BIRGITVOICE;
				break;
			case DE_DE_DIETER:
				voice = SynthesizeOptions.Voice.DE_DE_DIETERVOICE;
				break;
			case EN_GB_KATE:
				voice = SynthesizeOptions.Voice.EN_GB_KATEVOICE;
				break;
			case EN_US_ALLISON:
				voice = SynthesizeOptions.Voice.EN_US_ALLISONVOICE;
				break;
			case EN_US_LISA:
				voice = SynthesizeOptions.Voice.EN_US_LISAVOICE;
				break;
			case EN_US_MICHAEL:
				voice = SynthesizeOptions.Voice.EN_US_MICHAELVOICE;
				break;
			case ES_ES_ENRIQUE:
				voice = SynthesizeOptions.Voice.ES_ES_ENRIQUEVOICE;
				break;
			case ES_ES_LAURA:
				voice = SynthesizeOptions.Voice.ES_ES_LAURAVOICE;
				break;
			case ES_LA_SOFIA:
				voice = SynthesizeOptions.Voice.ES_LA_SOFIAVOICE;
				break;
			case ES_US_SOFIA:
				voice = SynthesizeOptions.Voice.ES_US_SOFIAVOICE;
				break;
			case FR_FR_RENEE:
				voice = SynthesizeOptions.Voice.FR_FR_RENEEVOICE;
				break;
			case IT_IT_FRANCESCA:
				voice = SynthesizeOptions.Voice.IT_IT_FRANCESCAVOICE;
				break;
			case JA_JP_EMI:
				voice = SynthesizeOptions.Voice.JA_JP_EMIVOICE;
				break;
			case PT_BR_ISABELA:
				voice = SynthesizeOptions.Voice.PT_BR_ISABELAVOICE;
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
