package watsonservices.utils;

import org.apache.commons.lang3.StringUtils;

import com.ibm.watson.developer_cloud.tone_analyzer.v3.ToneAnalyzer;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ElementTone;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.SentenceTone;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneAnalysis;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneCategory;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneScore;
import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.MendixException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import watsonservices.proxies.Tone;
import watsonservices.proxies.ToneAnalyzerResponse;

public class ToneAnalyzerService {
	
	private static final String WATSON_TONE_ANALYZER_LOGNODE = "WatsonServices.IBM_WatsonConnector_ToneAnalyzer";
	private static final ILogNode LOGGER = Core.getLogger((Core.getConfiguration().getConstantValue(WATSON_TONE_ANALYZER_LOGNODE).toString()));
	private static final ToneAnalyzer service = new ToneAnalyzer(ToneAnalyzer.VERSION_DATE_2016_05_19);

	public static IMendixObject analyzeTone(IContext context, String text, String username, String password) throws MendixException {
		LOGGER.debug("Executing Watson AnalyzeTone Connector...");

		service.setUsernameAndPassword(username, password);

		// Call the service and get the tone
		ToneAnalysis response;
		try
		{
			response = service.getTone(text, null).execute();
		} catch (Exception e) {
			LOGGER.error("Watson Service connection - Failed analyzing the tone of the text " + StringUtils.abbreviate(text, 20), e);
			throw new MendixException(e);
		}

		return CreateDocumentTone(context, response);
	}

	private static IMendixObject CreateDocumentTone(IContext context, ToneAnalysis response) {
		final IMendixObject toneAnalyzerResponse = Core.instantiate(context, ToneAnalyzerResponse.entityName);

		response.getDocumentTone().getTones().forEach(toneCategory -> buildToneCategory(context, toneAnalyzerResponse, toneCategory));

		if(response.getSentencesTone() != null && !response.getSentencesTone().isEmpty())
		{
			response.getSentencesTone().forEach(sentenceTone -> buildSentenceTone(context, toneAnalyzerResponse, sentenceTone));
		} 
		

		return toneAnalyzerResponse;
	}

	private static void buildToneCategory(IContext context, final IMendixObject toneAnalyzerResponse,
			ToneCategory toneCategory) 
	{
		final IMendixObject toneCategoryObject = Core.instantiate(context, watsonservices.proxies.ToneCategory.entityName);
		toneCategoryObject.setValue(context, watsonservices.proxies.ToneCategory.MemberNames.CategoryId.toString(), toneCategory.getId());
		toneCategoryObject.setValue(context, watsonservices.proxies.ToneCategory.MemberNames.Name.toString(), toneCategory.getName());

		toneCategory.getTones().forEach(toneScore -> buildTone(context, toneCategoryObject, toneScore));
		toneCategoryObject.setValue(context, watsonservices.proxies.ToneCategory.MemberNames.Tone_Categories.toString(), toneAnalyzerResponse.getId());
	}

	private static void buildTone(IContext context, final IMendixObject toneCategoryObject, ToneScore toneScore)
	{
		final IMendixObject toneObject = Core.instantiate(context, Tone.entityName);
		toneObject.setValue(context, Tone.MemberNames.ToneId.toString(), toneScore.getId());
		toneObject.setValue(context, Tone.MemberNames.Name.toString(), toneScore.getName());
		toneObject.setValue(context, Tone.MemberNames.Score.toString(), toneScore.getScore().toString());
		toneObject.setValue(context, Tone.MemberNames.Tones.toString(), toneCategoryObject.getId());
	}

	private static void buildSentenceTone(IContext context, final IMendixObject toneAnalyzerResponse,
			SentenceTone sentenceTone)
	{
		final IMendixObject sentenceToneObject = Core.instantiate(context, watsonservices.proxies.SentenceTone.entityName);
		sentenceToneObject.setValue(context, watsonservices.proxies.SentenceTone.MemberNames.SentenceId.toString(), sentenceTone.getId());
		sentenceToneObject.setValue(context, watsonservices.proxies.SentenceTone.MemberNames.InputFrom.toString(), sentenceTone.getInputFrom());
		sentenceToneObject.setValue(context, watsonservices.proxies.SentenceTone.MemberNames.InputTo.toString(), sentenceTone.getInputTo());
		sentenceToneObject.setValue(context, watsonservices.proxies.SentenceTone.MemberNames.Text.toString(), sentenceTone.getText());

		sentenceTone.getTones().forEach(toneCategory -> buildToneCategoryPerSentence(context, sentenceToneObject, toneCategory));

		sentenceToneObject.setValue(context, watsonservices.proxies.SentenceTone.MemberNames.Sentence_Tones.toString(), toneAnalyzerResponse.getId());
	}

	private static void buildToneCategoryPerSentence(IContext context, final IMendixObject sentenceToneObject,
			ToneCategory toneCategory) {
		final IMendixObject toneCategoryObject = Core.instantiate(context, watsonservices.proxies.ToneCategory.entityName);
		toneCategoryObject.setValue(context, watsonservices.proxies.ToneCategory.MemberNames.CategoryId.toString(), toneCategory.getId());
		toneCategoryObject.setValue(context, watsonservices.proxies.ToneCategory.MemberNames.Name.toString(), toneCategory.getName());

		for(ToneScore toneScore : toneCategory.getTones()){
			buildTone(context, toneCategoryObject, toneScore);
		}
		
		toneCategoryObject.setValue(context, watsonservices.proxies.ToneCategory.MemberNames.Sentence_Tone_Categories.toString(), sentenceToneObject.getId());
	}

}
