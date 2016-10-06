package watsonservices.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import com.ibm.watson.developer_cloud.alchemy.v1.AlchemyLanguage;
import com.ibm.watson.developer_cloud.alchemy.v1.model.Keyword;
import com.ibm.watson.developer_cloud.alchemy.v1.model.Keywords;
import com.mendix.core.Core;
import com.mendix.core.CoreException;
import com.mendix.logging.ILogNode;
import com.mendix.systemwideinterfaces.MendixException;
import com.mendix.systemwideinterfaces.core.IContext;
import com.mendix.systemwideinterfaces.core.IMendixObject;

import watsonservices.proxies.KeywordExtraction;
import watsonservices.proxies.KeywordRequest;

public class AlchemyLanguageService {

	private static final String WATSON_ALCHEMY_LOGNODE = "WatsonServices.IBM_WatsonConnector_Alchemy";
	private static ILogNode LOGGER = Core.getLogger(Core.getConfiguration().getConstantValue(WATSON_ALCHEMY_LOGNODE).toString());
	private static final AlchemyLanguage service = new AlchemyLanguage();

	public static IMendixObject getKeywords(IContext context, KeywordRequest request, String apikey) throws MendixException, CoreException {
		LOGGER.debug("executing Keywords Connector...");

		final StringBuilder htmlContent = new StringBuilder();
		try {
			final URL url = new URL(request.getUrl());
			final URLConnection connection = url.openConnection();
			connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:19.0) Gecko/20100101 Firefox/19.0");
			connection.connect();

			try(final BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {

				String line;
				while ((line = br.readLine()) != null) {
					htmlContent.append(line);
				}
			} catch (Exception e) {
				LOGGER.error("Error parsing the content of the url parameter " + request.getUrl());
				throw new MendixException(e);
			}
		} catch(Exception e){
			LOGGER.error("There was a problem with the with the url parameter " + request.getUrl());
			throw new MendixException(e);
		}

		service.setApiKey(apikey);

		final Map<String, Object> params = new HashMap<String, Object>();
		params.put(AlchemyLanguage.HTML, htmlContent.toString());
		Keywords keywords;
		try {
			keywords = service.getKeywords(params).execute();
		} catch (Exception e) {
			LOGGER.error("Watson Service connection - Failed getting keyword from url: " + request.getUrl(), e);
			throw new MendixException(e);
		}

		final IMendixObject keywordExtractionObject = Core.instantiate(context, KeywordExtraction.entityName);
		keywordExtractionObject.setValue(context, KeywordExtraction.MemberNames.Usage.toString(), keywords.getText());
		keywordExtractionObject.setValue(context, KeywordExtraction.MemberNames.Url.toString(), keywords.getUrl());
		keywordExtractionObject.setValue(context, KeywordExtraction.MemberNames.Language.toString(), keywords.getLanguage());

		Core.commit(context, keywordExtractionObject);

		for (Keyword keyword : keywords.getKeywords()) {

			final IMendixObject keywordObject = Core.instantiate(context, watsonservices.proxies.Keyword.entityName);
			keywordObject.setValue(context, watsonservices.proxies.Keyword.MemberNames.Text.toString(), keyword.getText());
			keywordObject.setValue(context, watsonservices.proxies.Keyword.MemberNames.Relevance.toString(), keyword.getRelevance().toString());
			keywordObject.setValue(context, watsonservices.proxies.Keyword.MemberNames.Keyword_KeywordExtraction.toString(), keywordExtractionObject.getId());

			Core.commit(context, keywordObject);
		}

		return keywordExtractionObject;
	}
}
