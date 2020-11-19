package momime.client.graphics.database;

import static org.junit.Assert.assertNotNull;

import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.Test;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import momime.client.ClientTestData;
import momime.common.database.CommonXsdResourceResolver;

/**
 * Tests the graphics XML file in isolation from anything else, i.e. the server XML file.
 * So we can't tell whether entries are missing, but we can do
 * basic checks against the XSD like optional/mandatory fields and that no unknown fields are present.
 */
public final class TestGraphicsDatabase extends ClientTestData
{
	/**
	 * Tests that all available language XML files conform to the language XSD, excluding all the links to the server XSD
	 * @throws Exception If there is an error
	 */
	@Test
	public final void testDatabaseConformToXSD () throws Exception
	{
		final URL xsdResource = getClass ().getResource (GraphicsDatabaseConstants.GRAPHICS_XSD_LOCATION);
		assertNotNull ("MoM IME Graphics XSD could not be found on classpath", xsdResource);

		// Load XSD
		final SchemaFactory schemaFactory = SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
		schemaFactory.setResourceResolver (new CommonXsdResourceResolver (DOMImplementationRegistry.newInstance ()));
		final Validator xsd = schemaFactory.newSchema (xsdResource).newValidator ();

		// Validate XML
		xsd.validate (new StreamSource (locateDefaultGraphicsXmlFile ()));
	}
}