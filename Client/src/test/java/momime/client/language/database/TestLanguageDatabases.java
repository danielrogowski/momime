package momime.client.language.database;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import momime.client.ClientTestData;
import momime.common.database.CommonXsdResourceResolver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

/**
 * Tests the language XML files in isolation from anything else, i.e. the server XML file.
 * So we can't tell whether entries are missing (and in the non-English files, they likely are missing), but we can do
 * basic checks against the XSD like optional/mandatory fields and that no unknown fields are present.
 */
public final class TestLanguageDatabases
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (TestLanguageDatabases.class);
	
	/**
	 * Tests that all available language XML files conform to the language XSD, excluding all the links to the server XSD
	 * @throws Exception If there is an error
	 */
	@Test
	public final void testDatabasesConformToXSD () throws Exception
	{
		final URL xsdResource = getClass ().getResource (LanguageDatabaseConstants.LANGUAGE_XSD_LOCATION_NO_SERVER_XSD_LINK);
		assertNotNull ("MoM IME Language XSD could not be found on classpath", xsdResource);

		// Load XSD
		final SchemaFactory schemaFactory = SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
		schemaFactory.setResourceResolver (new CommonXsdResourceResolver (DOMImplementationRegistry.newInstance ()));
		final Validator xsd = schemaFactory.newSchema (xsdResource).newValidator ();

		// Search for language files
		for (final File languageXml : ClientTestData.locateLanguageXmlFolder ().listFiles ())
			if (languageXml.isFile ())		// Ingore the "Backups" folder
			{
				log.info ("Testing language file \"" + languageXml.getName () + "\"");
				xsd.validate (new StreamSource (languageXml));
			}
	}
}