package momime.common.database;

import static org.junit.Assert.assertNotNull;

import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;

import org.junit.Test;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Tests the CommonXsdResourceResolver class
 */
public final class TestCommonXsdResourceResolver
{
	/** Path and name to locate the dummy XSD file on the classpath */
	private final static String DUMMY_XSD_LOCATION = "/MoMIMEDummyCommonDatabase.xsd";

	/**
	 * Tests that when loading an XSD that includes an import to the common XSD, that the resource resolver is able to locate the common XSD
	 * @throws Exception If there is an error reading the XSDs
	 */
	@Test
	public final void testMoMIMEXsdResourceResolver_Success () throws Exception
	{
		final URL xsdResource = getClass ().getResource (DUMMY_XSD_LOCATION);
		assertNotNull ("Dummy XSD could not be found on classpath", xsdResource);

		final SchemaFactory schemaFactory = SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
		schemaFactory.setResourceResolver (new CommonXsdResourceResolver (DOMImplementationRegistry.newInstance ()));
		schemaFactory.newSchema (xsdResource).newValidator ();
	}

	/**
	 * Proves that the resource resolver is actually necessary - that by not using it, we get an error
	 * This is really here to prove that the dummy XSD contains the necessary import
	 * @throws SAXException If there is an error reading the XSDs
	 */
	@Test(expected=SAXParseException.class)
	public final void testMoMIMEXsdResourceResolver_Fail () throws SAXException
	{
		final URL xsdResource = getClass ().getResource (DUMMY_XSD_LOCATION);
		assertNotNull ("Dummy XSD could not be found on classpath", xsdResource);

		final SchemaFactory schemaFactory = SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
		schemaFactory.newSchema (xsdResource).newValidator ();
	}
}
