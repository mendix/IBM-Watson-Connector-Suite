package watsonservices.utils.tests;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.ibm.watson.developer_cloud.conversation.v1_experimental.model.MessageRequest;
import com.ibm.watson.developer_cloud.conversation.v1_experimental.model.MessageResponse;
import com.ibm.watson.developer_cloud.language_translation.v2.LanguageTranslation;
import com.ibm.watson.developer_cloud.language_translation.v2.model.IdentifiableLanguage;
import com.mendix.core.Core;
import com.mendix.systemwideinterfaces.MendixException;
import com.mendix.systemwideinterfaces.core.IMendixObject;

public class LanguageTranslationServiceTests {

	private static final LanguageTranslation service = new LanguageTranslation();

	@Test
	public void test() {
		//given
		String username = System.getenv("TRANSLATOR_USERNAME");
		String password = System.getenv("TRANSLATOR_PASSWORD");
		service.setUsernameAndPassword(username, password);
		service.setEndPoint("https://gateway.watsonplatform.net/language-translator/api");

		// when
		List<IdentifiableLanguage> identifieableLanguages = service.getIdentifiableLanguages().execute();

		// then
		assertFalse(identifieableLanguages.isEmpty());
	}

}
