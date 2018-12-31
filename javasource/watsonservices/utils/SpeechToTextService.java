package watsonservices.utils;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;

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

	public static IMendixObject transcribe(IContext context, FileDocument audioFileParameter1, AudioFormats_SpeechToText audioFormat, AudioLanguage audioLanguage,
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
		if (audioFormat == null) {
			return RecognizeOptions.ContentType.APPLICATION_OCTET_STREAM;
		}
        switch (audioFormat) {
	        case FLAC:
	            return RecognizeOptions.ContentType.AUDIO_FLAC;
	        case BASIC:
	            return RecognizeOptions.ContentType.AUDIO_BASIC;
	        case OGG:
	            return RecognizeOptions.ContentType.AUDIO_OGG;
	        case OGG_VORBIS:
	            return RecognizeOptions.ContentType.AUDIO_OGG_CODECS_VORBIS;
	        case OGG_OPUS:
	            return RecognizeOptions.ContentType.AUDIO_OGG_CODECS_OPUS;
	        case L16:
	            return RecognizeOptions.ContentType.AUDIO_L16 + ";rate=22050";
	        case WAV:
	            return RecognizeOptions.ContentType.AUDIO_WAV;
	        case WEBM:
	            return RecognizeOptions.ContentType.AUDIO_WEBM;
	        case WEBM_OPUS:
	            return RecognizeOptions.ContentType.AUDIO_WEBM_CODECS_OPUS;
	        case WEBM_VORBIS:
	            return RecognizeOptions.ContentType.AUDIO_WEBM_CODECS_VORBIS;
	        case MP3:
	            return RecognizeOptions.ContentType.AUDIO_MP3;
	        case MPEG:
	            return RecognizeOptions.ContentType.AUDIO_MPEG;
	        default:
	            return RecognizeOptions.ContentType.APPLICATION_OCTET_STREAM;
        }
	}
	private static String getAudioLanguage(AudioLanguage audioLanguage){
        switch (audioLanguage){
        	case Brazillian_Portuguese:
	            return RecognizeOptions.Model.PT_BR_NARROWBANDMODEL;
		    case Brazillian_Portuguese_Broadband:
	            return RecognizeOptions.Model.PT_BR_BROADBANDMODEL;
        	case French:
	            return RecognizeOptions.Model.FR_FR_BROADBANDMODEL;
        	case Japanese:
	            return RecognizeOptions.Model.JA_JP_NARROWBANDMODEL;
        	case Japanese_Broadband:
	            return RecognizeOptions.Model.JA_JP_BROADBANDMODEL;
        	case Mandarin_Chinese:
	            return RecognizeOptions.Model.ZH_CN_NARROWBANDMODEL;
        	case Mandarin_Chinese_Broadband:
	            return RecognizeOptions.Model.ZH_CN_BROADBANDMODEL;
        	case Modern_Standard_Arabic:
	            return RecognizeOptions.Model.AR_AR_BROADBANDMODEL;
        	case Spanish:
	            return RecognizeOptions.Model.ES_ES_NARROWBANDMODEL;
        	case Spanish_Broadband:
	            return RecognizeOptions.Model.ES_ES_BROADBANDMODEL;
        	case UK_English:
	            return RecognizeOptions.Model.EN_GB_NARROWBANDMODEL;
        	case UK_English_Broadband:
	            return RecognizeOptions.Model.EN_GB_BROADBANDMODEL;
        	case US_English:
	            return RecognizeOptions.Model.EN_US_NARROWBANDMODEL;
        	case US_English_Broadband:
	            return RecognizeOptions.Model.EN_US_BROADBANDMODEL;
	        case German:
	            return RecognizeOptions.Model.DE_DE_BROADBANDMODEL;
	        case Korean:
	            return RecognizeOptions.Model.KO_KR_NARROWBANDMODEL;
	        case Korean_Broadband:
	            return RecognizeOptions.Model.KO_KR_BROADBANDMODEL;
        	default:
	            return RecognizeOptions.Model.EN_US_BROADBANDMODEL;
        }
	}
}
