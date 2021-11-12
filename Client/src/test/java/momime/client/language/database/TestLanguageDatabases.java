package momime.client.language.database;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import momime.client.ClientTestData;
import momime.common.database.CommonXsdResourceResolver;

/**
 * Tests the language XML files in isolation from anything else, i.e. the server XML file.
 * So we can't tell whether entries are missing (and in the non-English files, they likely are missing), but we can do
 * basic checks against the XSD like optional/mandatory fields and that no unknown fields are present.
 */
@ExtendWith(MockitoExtension.class)
public final class TestLanguageDatabases extends ClientTestData
{
	/**
	 * Tests that all the new singular language XML file conforms to the its XSD
	 * @throws Exception If there is an error
	 */
	@Test
	public final void testNewDatabasesConformToXSD () throws Exception
	{
		final URL xsdResource = getClass ().getResource (LanguageDatabaseConstants.LANGUAGES_XSD_LOCATION);
		assertNotNull (xsdResource, "MoM IME Languages XSD could not be found on classpath");

		// Load XSD
		final SchemaFactory schemaFactory = SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
		schemaFactory.setResourceResolver (new CommonXsdResourceResolver (DOMImplementationRegistry.newInstance ()));
		final Validator xsd = schemaFactory.newSchema (xsdResource).newValidator ();

		// Test the XML
		xsd.validate (new StreamSource (new File (locateLanguageXmlFolder (), "Languages.momime.xml")));
	}
}