package watsonservices.utils;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;

import com.ibm.watson.developer_cloud.http.HttpMediaType;
import com.ibm.watson.developer_cloud.service.security.IamOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechRecognitionAlternative;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechRecognitionResult;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechRecognitionResults;
import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import system.proxies.FileDocument;
import watsonservices.proxies.SpeechReturn;
import watsonservices.proxies.Result;
import watsonservices.proxies.Alternative;
import watsonservices.proxies.AudioFormats_SpeechToText;
import watsonservices.proxies.AudioLanguage;


public class SpeechToTextService {

	private static final ILogNode LOGGER = Core.getLogger("SpeechToTextService");

	public static IMendixObject Transcribe(IContext context, FileDocument audioFileParameter1, AudioFormats_SpeechToText audioFormat, AudioLanguage audioLanguage,
			String apiKey, String url) throws Exception {

		IamOptions iamOptions = new IamOptions.Builder()
				.apiKey(apiKey)
				.build();
		final SpeechToText service = new SpeechToText(iamOptions);
		service.setEndPoint(url);
		
		final InputStream audioFileInputStream = new RestartableInputStream(context, audioFileParameter1.getMendixObject());

		RecognizeOptions options = new RecognizeOptions.Builder().audio(audioFileInputStream).interimResults(true)
				.contentType(getAudioFormat(audioFormat)).model(getAudioLanguage(audioLanguage)).build();

		SpeechRecognitionResults transcript = service.recognize(options).execute();

		SpeechReturn speechToTextObj = new SpeechReturn(context);
		speechToTextObj.setresults_index(transcript.getResultIndex());
		ArrayList<IMendixObject> mxObjs = new ArrayList<IMendixObject>();
		for (SpeechRecognitionResult result : transcript.getResults()) {
			Result res = new Result(context);
			res.set_final(result.isFinalResults());
			res.setresults(speechToTextObj);
			mxObjs.add(res.getMendixObject());
			for (SpeechRecognitionAlternative alt : result.getAlternatives()) {
				Alternative altMx = new Alternative(context);
				BigDecimal confidence = new BigDecimal(alt.getConfidence(), MathContext.DECIMAL64);
				altMx.setconfidence(confidence);
				altMx.settranscript(alt.getTranscript());
				altMx.setalternatives(res);
				mxObjs.add(altMx.getMendixObject());
			}
		}
		Core.commit(context, mxObjs);
		Core.commit(context, speechToTextObj.getMendixObject());
		return speechToTextObj.getMendixObject();
	}
	private static String getAudioFormat(AudioFormats_SpeechToText audioFormat){
		String audio = null;
        switch (audioFormat) {
	        case FLAC:
	            audio = HttpMediaType.AUDIO_FLAC;
	            break;
	        case BASIC:
	            audio = HttpMediaType.AUDIO_BASIC;
	            break;
	        case OGG:
	        	audio = HttpMediaType.AUDIO_OGG;
	        	break;
	        case OGG_VORBIS:
	            audio = HttpMediaType.AUDIO_OGG_VORBIS;
	            break;
	        case RAW:
	        	audio = HttpMediaType.AUDIO_RAW;
	            break;
	        case WAV:
	        	audio = HttpMediaType.AUDIO_WAV;
	            break;
	        case PCM:
	        	audio = HttpMediaType.AUDIO_PCM;
	        	break;
	        default :
	        	audio = HttpMediaType.AUDIO_RAW;
	            break;
        }
        return audio;
	}
	private static String getAudioLanguage(AudioLanguage audioLanguage){
        String model = null;
        switch (audioLanguage){
        	case Brazillian_Portuguese:
        		model = "pt-BR_NarrowbandModel";
        		break;
        	case French:
        		model = "fr-FR_BroadbandModel";
        		break;
        	case Japanese:
        		model = "ja-JP_NarrowbandModel";
        		break;
        	case Japanese_Broadband:
        		model = "ja-JP_BroadbandModel";
        		break;
        	case Mandarin_Chinese:
        		model = "zh-CN_NarrowbandModel";
        		break;
        	case Mandarin_Chinese_Broadband:
        		model = "zh-CN_BroadbandModel";
        		break;
        	case Modern_Standard_Arabic:
        		model = "ar-AR_BroadbandModel";
        		break;
        	case Spanish:
        		model = "es-ES_NarrowbandModel";
        		break;
        	case Spanish_Broadband:
        		model = "es-ES_BroadbandModel";
        		break;
        	case UK_English:
        		model = "en-UK_NarrowbandModel";
        		break;
        	case UK_English_Broadband:
        		model = "en-UK_BroadbandModel";
        		break;
        	case US_English:
        		model = "en-US_NarrowbandModel";
        		break;
        	case US_English_Broadband:
        		model = "en-US_BroadbandModel";
        		break;

        	default:
        		model = "en-US_BroadbandModel";
        		break;
        		
        		
        }
        return model;
	}
}
