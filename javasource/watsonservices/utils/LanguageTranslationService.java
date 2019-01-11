package watsonservices.utils;

import java.util.ArrayList;
import java.util.List;

import com.ibm.watson.developer_cloud.service.security.IamOptions;
import org.apache.commons.lang3.StringUtils;

import com.ibm.watson.developer_cloud.language_translator.v3.model.IdentifiableLanguage;
import com.ibm.watson.developer_cloud.language_translator.v3.model.TranslationResult;
import com.ibm.watson.developer_cloud.language_translator.v3.LanguageTranslator;
import com.ibm.watson.developer_cloud.language_translator.v3.model.IdentifiableLanguages;
import com.ibm.watson.developer_cloud.language_translator.v3.model.TranslateOptions;
import com.ibm.watson.developer_cloud.language_translator.v3.model.TranslationModels;
import com.mendix.core.Core;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.MendixException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import watsonservices.proxies.Language;
import watsonservices.proxies.Translation;
import watsonservices.proxies.TranslationModel;

public class LanguageTranslationService {

	private static final String WATSON_TRANSLATE_LOGNODE = "WatsonServices.IBM_WatsonConnector_Translate";
	private static final ILogNode LOGGER = Core.getLogger(Core.getConfiguration().getConstantValue(WATSON_TRANSLATE_LOGNODE).toString());
	private static final String WATSON_LANGUAGE_TRANSLATOR_VERSION_DATE = "2018-05-01";

	public static List<IMendixObject> getIdentifiableLanguages(IContext context, String apiKey, String url) throws MendixException {
		LOGGER.debug("Executing IdentifiableLanagues Connector...");

		IamOptions iamOptions = new IamOptions.Builder()
				.apiKey(apiKey)
				.build();
		final LanguageTranslator service = new LanguageTranslator(WATSON_LANGUAGE_TRANSLATOR_VERSION_DATE, iamOptions);
		service.setEndPoint(url);

	    final IdentifiableLanguages identifieableLanguages;

		try{
			identifieableLanguages = service.listIdentifiableLanguages().execute();
		} catch (Exception e) {
			LOGGER.error("Watson Service Connection - Failed retrieving the identifiable languages", e);
			throw new MendixException(e);
		}

		final List<IMendixObject> results = new ArrayList<IMendixObject>();
		for(IdentifiableLanguage language : identifieableLanguages.getLanguages()){

			IMendixObject result = Core.instantiate(context,  watsonservices.proxies.Language.entityName);

			result.setValue(context, "Name", language.getName());
			result.setValue(context, "Code", language.getLanguage());
			results.add(result);
		}

		return results;
	}

	public static IMendixObject translate(IContext context, Translation translation, String apiKey, String url) throws MendixException {
		LOGGER.debug("Executing Translate Connector...");

		IamOptions iamOptions = new IamOptions.Builder()
				.apiKey(apiKey)
				.build();
		final LanguageTranslator service = new LanguageTranslator(WATSON_LANGUAGE_TRANSLATOR_VERSION_DATE, iamOptions);
		service.setEndPoint(url);

		final String source = translation.getTranslation_SourceLanguage().getCode();
		final String target = translation.getTranslation_TargetLanguage().getCode();

		TranslateOptions translateOptions = new TranslateOptions.Builder()
				  .addText(translation.getText())
				  .source(source)
				  .target(target)
				  .build();

		final TranslationResult result;
		try {

			result = service.translate(translateOptions).execute();
		} catch (Exception e) {
			LOGGER.error("Watson Service connection - Failed translating from " + source + " to " + target + " the text " + StringUtils.abbreviate(translation.getText(), 20), e);
			throw new MendixException(e);
		}

		translation.setWordCount(Long.valueOf(result.getWordCount()));
		translation.setCharacterCount(Long.valueOf(result.getCharacterCount()));
		translation.setOutput(result.getTranslations().get(0).getTranslationOutput());
		Core.commit(context, translation.getMendixObject());

		return translation.getMendixObject();
	}

	public static List<IMendixObject> getModels(IContext context, List<Language> languages, String apiKey, String url) throws MendixException {
		LOGGER.debug("Executing GetModels Connector...");

		if (languages == null || languages.isEmpty()) {
			LOGGER.error("GetModels needs a complete list of languages to create associations");
			throw new MendixException("No languages provided");
		}

		IamOptions iamOptions = new IamOptions.Builder()
				.apiKey(apiKey)
				.build();
		final LanguageTranslator service = new LanguageTranslator(WATSON_LANGUAGE_TRANSLATOR_VERSION_DATE, iamOptions);
		service.setEndPoint(url);

		final TranslationModels models;
		try {
			models = service.listModels().execute();
		} catch(Exception ex) {
			LOGGER.error("Watson Service connection - Failed to retrieve the translation models", ex);
			throw new MendixException(ex);
		}

		List<IMendixObject> mendixModels = new ArrayList<>(models.getModels().size());

		for (com.ibm.watson.developer_cloud.language_translator.v3.model.TranslationModel tm : models.getModels()) {
			TranslationModel mendixTM = new TranslationModel(context);
			mendixTM.setModelId(tm.getModelId());
			mendixTM.setTranslationModel_SourceLanguage(findLanguage(languages, tm.getSource()));
			mendixTM.setTranslationModel_TargetLanguage(findLanguage(languages, tm.getTarget()));
			mendixModels.add(mendixTM.getMendixObject());
		}

		return mendixModels;
	}

	private static Language findLanguage(List<Language> languages, String code) throws MendixException {
		if (code == null) {
			LOGGER.error("findLanguage language code is null");
			throw new MendixException("Language code is null");
		}
		for (Language language : languages) {
			if (code.equals(language.getCode())) {
				return language;
			}
		}
		LOGGER.error("findLanguage language not found: " + code);
		throw new MendixException("Language code not found: "+ code);
	}
}
