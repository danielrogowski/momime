package momime.server.database;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import momime.server.ServerTestData;

import org.junit.Test;

/**
 * Tests the ServerDatabaseFactory class
 */
public final class TestServerDatabaseFactory
{
	/**
	 * Tests that the ServerDatabaseFactory results in JAXB giving us a ServerDatabaseEx object
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 */
	@Test
	public final void testServerDatabaseFactory () throws IOException, JAXBException
	{
		final Unmarshaller unmarshaller = JAXBContextCreator.createServerDatabaseContext ().createUnmarshaller ();
		unmarshaller.setProperty ("com.sun.xml.bind.ObjectFactory", new Object [] {new ServerDatabaseFactory ()});

		final Object serverDB = unmarshaller.unmarshal (ServerTestData.locateServerXmlFile ());
		assertEquals (ServerDatabaseEx.class.getName (), serverDB.getClass ().getName ());
	}
}
