package momime.common.database;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

/**
 * No Java classes are generated from the Mod XSD, so this just checks it is valid 
 */
@ExtendWith(MockitoExtension.class)
public final class TestModXSD
{
	/**
	 * Tests creating a schema from the Mod XSD
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testModXSD () throws Exception
	{
		final URL xsdResource = getClass ().getResource (CommonDatabaseConstants.MOD_XSD_LOCATION);
		assertNotNull (xsdResource, "MoM IME Mod XSD could not be found on classpath");

		final SchemaFactory schemaFactory = SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
		schemaFactory.setResourceResolver (new CommonXsdResourceResolver (DOMImplementationRegistry.newInstance ()));
		
		schemaFactory.newSchema (xsdResource);
	}
}