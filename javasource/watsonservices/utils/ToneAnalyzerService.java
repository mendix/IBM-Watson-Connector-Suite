package watsonservices.utils;

import com.ibm.watson.developer_cloud.service.security.IamOptions;
import org.apache.commons.lang3.StringUtils;

import com.ibm.watson.developer_cloud.tone_analyzer.v3.ToneAnalyzer;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.SentenceAnalysis;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneAnalysis;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneCategory;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneInput;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneOptions;
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
	private static final String WATSON_TONE_ANALYZER_VERSION_DATE = "2016-05-19";

	public static IMendixObject analyzeTone(IContext context, String text, String apiKey, String url) throws MendixException {
		LOGGER.debug("Executing Watson AnalyzeTone Connector...");

		IamOptions iamOptions = new IamOptions.Builder()
				.apiKey(apiKey)
				.build();
		final ToneAnalyzer service = new ToneAnalyzer(WATSON_TONE_ANALYZER_VERSION_DATE, iamOptions);
		service.setEndPoint(url);

		final ToneInput input = new ToneInput.Builder()
				.text(text)
				.build();

		final ToneOptions options = new ToneOptions.Builder()
				.toneInput(input)
				.build();

		// Call the service and get the tone
		final ToneAnalysis response;
		try
		{
			response = service.tone(options).execute();
		} catch (Exception e) {
			LOGGER.error("Watson Service connection - Failed analyzing the tone of the text " + StringUtils.abbreviate(text, 20), e);
			throw new MendixException(e);
		}

		return CreateDocumentTone(context, response);
	}

	private static IMendixObject CreateDocumentTone(IContext context, ToneAnalysis response) {
		final IMendixObject toneAnalyzerResponse = Core.instantiate(context, ToneAnalyzerResponse.entityName);

		response.getDocumentTone().getToneCategories().forEach(toneCategory -> buildToneCategory(context, toneAnalyzerResponse, toneCategory));

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
		toneCategoryObject.setValue(context, watsonservices.proxies.ToneCategory.MemberNames.CategoryId.toString(), toneCategory.getCategoryId());
		toneCategoryObject.setValue(context, watsonservices.proxies.ToneCategory.MemberNames.Name.toString(), toneCategory.getCategoryName());

		toneCategory.getTones().forEach(toneScore -> buildTone(context, toneCategoryObject, toneScore));
		toneCategoryObject.setValue(context, watsonservices.proxies.ToneCategory.MemberNames.Tone_Categories.toString(), toneAnalyzerResponse.getId());
	}

	private static void buildTone(IContext context, final IMendixObject toneCategoryObject, ToneScore toneScore)
	{
		final IMendixObject toneObject = Core.instantiate(context, Tone.entityName);
		toneObject.setValue(context, Tone.MemberNames.ToneId.toString(), toneScore.getToneId());
		toneObject.setValue(context, Tone.MemberNames.Name.toString(), toneScore.getToneName());
		toneObject.setValue(context, Tone.MemberNames.Score.toString(), toneScore.getScore().toString());
		toneObject.setValue(context, Tone.MemberNames.Tones.toString(), toneCategoryObject.getId());
	}

	private static void buildSentenceTone(IContext context, final IMendixObject toneAnalyzerResponse,
			SentenceAnalysis sentenceAnalysis)
	{
		final IMendixObject sentenceToneObject = Core.instantiate(context, watsonservices.proxies.SentenceTone.entityName);
		sentenceToneObject.setValue(context, watsonservices.proxies.SentenceTone.MemberNames.SentenceId.toString(), sentenceAnalysis.getSentenceId());
		sentenceToneObject.setValue(context, watsonservices.proxies.SentenceTone.MemberNames.InputFrom.toString(), sentenceAnalysis.getInputFrom());
		sentenceToneObject.setValue(context, watsonservices.proxies.SentenceTone.MemberNames.InputTo.toString(), sentenceAnalysis.getInputTo());
		sentenceToneObject.setValue(context, watsonservices.proxies.SentenceTone.MemberNames.Text.toString(), sentenceAnalysis.getText());

		sentenceAnalysis.getToneCategories().forEach(toneCategory -> buildToneCategoryPerSentence(context, sentenceToneObject, toneCategory));

		sentenceToneObject.setValue(context, watsonservices.proxies.SentenceTone.MemberNames.Sentence_Tones.toString(), toneAnalyzerResponse.getId());
	}

	private static void buildToneCategoryPerSentence(IContext context, final IMendixObject sentenceToneObject,
			ToneCategory toneCategory) {
		final IMendixObject toneCategoryObject = Core.instantiate(context, watsonservices.proxies.ToneCategory.entityName);
		toneCategoryObject.setValue(context, watsonservices.proxies.ToneCategory.MemberNames.CategoryId.toString(), toneCategory.getCategoryId());
		toneCategoryObject.setValue(context, watsonservices.proxies.ToneCategory.MemberNames.Name.toString(), toneCategory.getCategoryName());

		for(ToneScore toneScore : toneCategory.getTones()){
			buildTone(context, toneCategoryObject, toneScore);
		}
		
		toneCategoryObject.setValue(context, watsonservices.proxies.ToneCategory.MemberNames.Sentence_Tone_Categories.toString(), sentenceToneObject.getId());
	}

}
