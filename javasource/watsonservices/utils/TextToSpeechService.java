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

	public static IMendixObject synthesize(IContext context, String apiKey, String url, String text, VoiceEnum voiceEnumParameter, AudioFormats_TextToSpeech audioFormatEnum) throws MendixException {
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
			return SynthesizeOptions.Accept.AUDIO_L16 + ";rate=22050";
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

	private static String getFileExtension(AudioFormats_TextToSpeech audioFormatEnum) throws MendixException {
		if(audioFormatEnum == null) {
			LOGGER.error("getFileExtension: audio format is empty");
			throw new MendixException("audio format is empty");
		}

		switch (audioFormatEnum) {
		case BASIC:
			return ".au";
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
			LOGGER.error("getFileExtension: cannot map unsupported format " + audioFormatEnum + " to a file extension");
			throw new MendixException("The supplied parameter doesn't correspond to any audio format: " + audioFormatEnum);
		}
	}

	private static String getVoice(VoiceEnum parameter) throws MendixException {
		if(parameter == null) {
			LOGGER.error("getVoice: parameter is empty");
			throw new MendixException("parameter is empty");
		}

		switch(parameter){
			case DE_DE_BIRGIT:
				return SynthesizeOptions.Voice.DE_DE_BIRGITVOICE;
			case DE_DE_DIETER:
				return SynthesizeOptions.Voice.DE_DE_DIETERVOICE;
			case EN_GB_KATE:
				return SynthesizeOptions.Voice.EN_GB_KATEVOICE;
			case EN_US_ALLISON:
				return SynthesizeOptions.Voice.EN_US_ALLISONVOICE;
			case EN_US_LISA:
				return SynthesizeOptions.Voice.EN_US_LISAVOICE;
			case EN_US_MICHAEL:
				return SynthesizeOptions.Voice.EN_US_MICHAELVOICE;
			case ES_ES_ENRIQUE:
				return SynthesizeOptions.Voice.ES_ES_ENRIQUEVOICE;
			case ES_ES_LAURA:
				return SynthesizeOptions.Voice.ES_ES_LAURAVOICE;
			case ES_LA_SOFIA:
				return SynthesizeOptions.Voice.ES_LA_SOFIAVOICE;
			case ES_US_SOFIA:
				return SynthesizeOptions.Voice.ES_US_SOFIAVOICE;
			case FR_FR_RENEE:
				return SynthesizeOptions.Voice.FR_FR_RENEEVOICE;
			case IT_IT_FRANCESCA:
				return SynthesizeOptions.Voice.IT_IT_FRANCESCAVOICE;
			case JA_JP_EMI:
				return SynthesizeOptions.Voice.JA_JP_EMIVOICE;
			case PT_BR_ISABELA:
				return SynthesizeOptions.Voice.PT_BR_ISABELAVOICE;
			default:
				LOGGER.error("getVoice: cannot map unsupported voice " + parameter + " to a voice parameter");
				throw new MendixException("The supplied parameter doesn't correspond to any voice: " + parameter);
		}
	}

}
