package watsonservices.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.ibm.watson.developer_cloud.language_translation.v2.LanguageTranslation;
import com.ibm.watson.developer_cloud.language_translation.v2.model.IdentifiableLanguage;
import com.ibm.watson.developer_cloud.language_translation.v2.model.Language;
import com.ibm.watson.developer_cloud.language_translation.v2.model.TranslationResult;
import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.MendixException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import watsonservices.proxies.Translation;

public class LanguageTranslationService {

	private static final String WATSON_TRANSLATE_LOGNODE = "WatsonServices.IBM_WatsonConnector_Translate";
	private static final ILogNode LOGGER = Core.getLogger(Core.getConfiguration().getConstantValue(WATSON_TRANSLATE_LOGNODE).toString());
	private static final LanguageTranslation service = new LanguageTranslation();

	public static List<IMendixObject> getIdentifiableLanguages(IContext context, String username, String password) throws MendixException {
		LOGGER.debug("Executing IdentifiableLanagues Connector...");

		service.setUsernameAndPassword(username, password);
		service.setEndPoint("https://gateway.watsonplatform.net/language-translator/api");

	    List<IdentifiableLanguage> identifieableLanguages;
		try{
			identifieableLanguages = service.getIdentifiableLanguages().execute();
		} catch (Exception e) {
			LOGGER.error("Watson Service Connection - Failed retrieving the identifiable languages", e);
			throw new MendixException(e);
		}

		final List<IMendixObject> results = new ArrayList<IMendixObject>();
		for(IdentifiableLanguage language : identifieableLanguages){

			IMendixObject result = Core.instantiate(context,  watsonservices.proxies.Language.entityName);

			result.setValue(context, "Name", language.getName());
			result.setValue(context, "Code", language.getLanguage());
			results.add(result);
		}

		return results;
	}

	public static IMendixObject translate(IContext context, Translation translation, String username, String password) throws MendixException, CoreException {
		LOGGER.debug("Executing Translate Connector...");

		service.setUsernameAndPassword(username, password);

		final Language source = getLanguage(translation.getTranslation_SourceLanguage().getCode());
		final Language target = getLanguage(translation.getTranslation_TargetLanguage().getCode());

		TranslationResult result;
		try {

			result = service.translate(translation.getText(), source, target).execute();
		} catch (Exception e) {
			LOGGER.error("Watson Service connection - Failed translating from" + source + " to " + target + " the text " + StringUtils.abbreviate(translation.getText(), 20), e);
			throw new MendixException(e);
		}

		translation.setWordCount(Long.valueOf(result.getWordCount()));
		translation.setCharacterCount(Long.valueOf(result.getCharacterCount()));
		translation.setOutput(result.getFirstTranslation());
		Core.commit(context, translation.getMendixObject());

		return translation.getMendixObject();
	}

	private static Language getLanguage(String lang) throws MendixException {
		if(Language.ARABIC.toString().equals(lang)){
			return Language.ARABIC;
		}
		if(Language.ENGLISH.toString().equals(lang)){
			return Language.ENGLISH;
		}
		if(Language.SPANISH.toString().equals(lang)){
			return Language.SPANISH;
		}
		if(Language.FRENCH.toString().equals(lang)){
			return Language.FRENCH;
		}
		if(Language.ITALIAN.toString().equals(lang)){
			return Language.ITALIAN;
		}
		if(Language.PORTUGUESE.toString().equals(lang)){
			return Language.PORTUGUESE;
		}
		
		throw new MendixException("The language is not supported: " + lang);
	}
}
