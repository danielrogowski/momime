package momime.client.language.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import momime.client.database.GenerateTestData;
import momime.server.database.ServerDatabaseConstants;
import momime.server.database.ServerXsdResourceResolver;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

/**
 * Tests reading the language XSD and XML files
 */
public final class TestLanguageDatabase
{
	/** Dummy logger to use during unit tests */
	private final Logger debugLogger = Logger.getLogger ("MoMIMEClientUnitTests");

	/** Prefix that we expect all keyrefs in the language XSD to begin with */
	private static final String KEYREF_NAMESPACE_PREFIX = "momimesvr:";

	/** Fudge prefix identifying 2nd/3rd level references */
	private static final String MULTI_LEVEL_REFERENCE_PREFIX = "multilevelFKtosecondaryDB/";

	/**
	 * Tests that the English XML file conforms to the language XSD
	 * @throws Exception If there is an error
	 */
	@Test
	public final void testDatabaseConformsToXSD () throws Exception
	{
		final URL xsdResource = getClass ().getResource (LanguageDatabaseConstants.LANGUAGE_XSD_LOCATION);
		assertNotNull ("MoM IME Language XSD could not be found on classpath", xsdResource);

		// Load XSD
		final SchemaFactory schemaFactory = SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
		schemaFactory.setResourceResolver (new ServerXsdResourceResolver (DOMImplementationRegistry.newInstance (), debugLogger));
		final Validator xsd = schemaFactory.newSchema (xsdResource).newValidator ();

		// Load both XMLs in as DOM documents
		final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance ();
		builderFactory.setNamespaceAware (true);

		final DocumentBuilder builder = builderFactory.newDocumentBuilder ();

		final Document serverXml = builder.parse (GenerateTestData.locateServerXmlFile ());
		final Document languageXml = builder.parse (GenerateTestData.locateEnglishXmlFile ());

		// Insert the server XML into the appropriate place in the language XML
		// You can't transfer nodes from one document to another, so we have to make a copy (import) of the entire server XML
		final Node serverXmlCopy = languageXml.importNode (serverXml.getDocumentElement (), true);
		languageXml.getDocumentElement ().insertBefore (serverXmlCopy, languageXml.getDocumentElement ().getChildNodes ().item (0));

		// Validate combined XML
		xsd.validate (new DOMSource (languageXml));
	}

	/**
	 * @param nodeList Node list to search
	 * @param elementName Element name to search for
	 * @return List of matching elements
	 */
	private final List<Element> listElements (final NodeList nodeList, final String elementName)
	{
		final List<Element> result = new ArrayList<Element> ();
		for (int nodeNo = 0; nodeNo < nodeList.getLength (); nodeNo++)
		{
			final Node thisNode = nodeList.item (nodeNo);
			if ((thisNode.getNodeType () == Node.ELEMENT_NODE) && (thisNode.getNodeName ().equals (elementName)))
				result.add ((Element) thisNode);
		}

		return result;
	}

	/**
	 * Checking the English XML file against the langauge XSD proves that
	 * 1) Every entry defined in the English XML has a corresponding entry in the server XML
	 * 2) That there are no duplicates
	 *
	 * What it doesn't prove is that all the entries in the server XML are covered, i.e. that we didn't miss any, so that is what this test checks
	 * Every type of entity present in the server XML isn't in the language XML, and we can't assume that all types that should be present in the language XML are,
	 * so the only way to do this properly is to look at all the xsd:keyrefs in the language XSD to get a definitive list of what entities should be present
	 *
	 * @throws Exception If there is an error
	 */
	@Test
	public final void ensureNoMissingEntries () throws Exception
	{
		// Load the two XSDs and the two XML files
		final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance ();
		builderFactory.setNamespaceAware (true);

		final DocumentBuilder builder = builderFactory.newDocumentBuilder ();

		final Document serverXml = builder.parse (GenerateTestData.locateServerXmlFile ());
		final Document languageXml = builder.parse (GenerateTestData.locateEnglishXmlFile ());
		final Document serverXsd = builder.parse (getClass ().getResourceAsStream (ServerDatabaseConstants.SERVER_XSD_LOCATION));
		final Document languageXsd = builder.parse (getClass ().getResourceAsStream (LanguageDatabaseConstants.LANGUAGE_XSD_LOCATION));

		// Find all the PKs defined in the server XSD
		final List<Element> serverElementList = listElements (serverXsd.getDocumentElement ().getChildNodes (), "xsd:element");
		assertEquals ("Server XSD should contain exactly one top level element", 1, serverElementList.size ());
		final Element serverElement = serverElementList.get (0);

		// getElementsByTagName (rather than getChildNodes) does a deep search, and so *does* find 2nd/3rd level PKs
		final NodeList serverPKsNodeList = serverElement.getElementsByTagName ("xsd:key");
		final List<Element> serverPKs = new ArrayList<Element> ();
		for (int nodeNo = 0; nodeNo < serverPKsNodeList.getLength (); nodeNo++)
			serverPKs.add (((Element) serverPKsNodeList.item (nodeNo)));

		if (serverPKs.size () == 0)
			fail ("Could not find PKs in Server XSD");

		for (final Element thisPK : serverPKs)
			assertEquals ("ServerPKs expected to have exactly 1 attribute", 1, thisPK.getAttributes ().getLength ());

		// Find all the keyrefs that link from the language XSD into the server XSD PKs
		final List<Element> languageElementList = listElements (languageXsd.getDocumentElement ().getChildNodes (), "xsd:element");
		assertEquals ("Language XSD should contain exactly one top level element", 1, languageElementList.size ());
		final Element languageElement = languageElementList.get (0);

		final List<Element> keyRefs = listElements (languageElement.getChildNodes (), "xsd:keyref");
		if (keyRefs.size () == 0)
			fail ("Could not find keyrefs in Language XSD");

		for (final Element thisKeyRef : keyRefs)
		{
			// Pull all the necessary details out of the language XSD keyref
			assertEquals ("KeyRefs all expected to contain 2 attributes", 2, thisKeyRef.getAttributes ().getLength ());
			String pkName = thisKeyRef.getAttribute ("refer");

			if (!pkName.startsWith (KEYREF_NAMESPACE_PREFIX))
				fail ("All keyrefs are expected to start with \"" + KEYREF_NAMESPACE_PREFIX + "\"");

			pkName = pkName.substring (KEYREF_NAMESPACE_PREFIX.length ());

			final List<Element> languageSelectorList = listElements (thisKeyRef.getChildNodes (), "xsd:selector");
			assertEquals ("KeyRef expected to have exactly 1 selector", 1, languageSelectorList.size ());
			assertEquals ("KeyRef selector expected to have exactly 1 attribute", 1, languageSelectorList.get (0).getAttributes ().getLength ());
			final String languageEntity = languageSelectorList.get (0).getAttribute ("xpath");

			final List<Element> languageFieldList = listElements (thisKeyRef.getChildNodes (), "xsd:field");
			assertEquals ("KeyRef expected to have exactly 1 field", 1, languageFieldList.size ());
			assertEquals ("KeyRef field expected to have exactly 1 attribute", 1, languageFieldList.get (0).getAttributes ().getLength ());
			final String languageField = languageFieldList.get (0).getAttribute ("xpath");

			if (!languageField.startsWith ("@"))
				fail ("KeyRef field expected to be an attribute, prefixed with @");

			// Pull all the necessary details out of the server XSD PK
			Element thisPK = null;
			final Iterator<Element> pkIter = serverPKs.iterator ();
			while ((thisPK == null) && (pkIter.hasNext ()))
			{
				final Element searchPK = pkIter.next ();
				if (searchPK.getAttribute ("name").equals (pkName))
					thisPK = searchPK;
			}

			assertNotNull ("Could not find definition of PK \"" + pkName + "\" in server XSD", thisPK);

			final List<Element> serverSelectorList = listElements (thisPK.getChildNodes (), "xsd:selector");
			assertEquals ("PK expected to have exactly 1 selector", 1, serverSelectorList.size ());
			assertEquals ("PK selector expected to have exactly 1 attribute", 1, serverSelectorList.get (0).getAttributes ().getLength ());
			final String serverEntity = serverSelectorList.get (0).getAttribute ("xpath");

			final List<Element> serverFieldList = listElements (thisPK.getChildNodes (), "xsd:field");
			assertEquals ("PK expected to have exactly 1 field", 1, serverFieldList.size ());
			assertEquals ("PK field expected to have exactly 1 attribute", 1, serverFieldList.get (0).getAttributes ().getLength ());
			final String serverField = serverFieldList.get (0).getAttribute ("xpath");

			if (!serverField.startsWith ("@"))
				fail ("PK field expected to be an attribute, prefixed with @");

			// By convention, all entities and identifying attributes are named the same in the language and server XSDs
			// We have to enforce this assumption in order for the 2nd/3rd level references to work
			// 2nd/3rd level references are the only time we deviate from this
			assertEquals ("Expect server and language identifying attributes to be named the same", serverField, languageField);

			if (!languageEntity.startsWith (MULTI_LEVEL_REFERENCE_PREFIX))
			{
				assertEquals ("Expect server and language entities to be named the same", serverEntity, languageEntity);
				checkReferences (serverXml.getDocumentElement (), languageXml.getDocumentElement (), serverEntity, serverField, null);
			}
			else
			{
				// 2nd/3rd level reference - and have proved that the lowest level identifying field matches
				if (!languageEntity.endsWith ("/" + serverEntity))
					fail ("2nd/3rd level references should end with the same entity (" + serverEntity + ", " + languageEntity + ")");

				// This gets a list of all higher level entities, each terminated by a /, excluding the final entity
				final String higherLevelEntities = languageEntity.substring (MULTI_LEVEL_REFERENCE_PREFIX.length (),
					languageEntity.length () - serverEntity.length ());

				checkMultilevelReferences (serverXml.getDocumentElement (), languageXml.getDocumentElement (), higherLevelEntities, serverEntity, serverField, null);
			}
		}
	}

	/**
	 * Checks all records under one container, ensuring every record under the server container has a corresponding record under the language container
	 * @param serverContainer Element in the server XML that contains the records we want to check
	 * @param languageContainer Element in the language XML that contains the records we want to check
	 * @param entityName Tag identifying the types of record we want to check
	 * @param fieldName Attribute that identifies each record
	 * @param parentValues Values of identifying attributes of any parent record we have drilled down through to reach here
	 */
	private final void checkReferences (final Element serverContainer, final Element languageContainer, final String entityName, final String fieldName, final String parentValues)
	{
		final String parentValuesSuffix = (parentValues == null) ? "" : ", parent key(s) = " + parentValues;
		System.out.println ("Checking English XML includes all references for entity " + entityName + ", field " + fieldName + parentValuesSuffix);

		final List<Element> serverEntries = listElements (serverContainer.getChildNodes (), entityName);
		final List<Element> languageEntries = listElements (languageContainer.getChildNodes (), entityName);

		String missingRecords = null;
		for (final Element thisServerRecord : serverEntries)
		{
			final String pkValue = thisServerRecord.getAttribute (fieldName.substring (1));
			assertNotNull ("Server has a null PK, on entity \"" + entityName + "\"", pkValue);

			boolean found = false;
			final Iterator<Element> iter = languageEntries.iterator ();
			while ((!found) && (iter.hasNext ()))
			{
				if (iter.next ().getAttribute (fieldName.substring (1)).equals (pkValue))
					found = true;
			}

			if (!found)
				missingRecords = (missingRecords == null) ? pkValue : missingRecords + "," + pkValue;
		}

		if (missingRecords != null)
			fail ("Language XML is missing " + entityName + " records for the following " + fieldName.substring (1) + "s: " +  missingRecords + parentValuesSuffix);

		// So, we proved that all server entries exist in the langauge XML
		// That doesn't yet prove that the language XML contains no spurious additional entries
		// For top level entities, the XSD will have checked that, but do it here for the benefit of 2nd/3rd level entities
		assertEquals ("Server and language XML files expected to contain same number of " + entityName + " records" + parentValuesSuffix,
			serverEntries.size (), languageEntries.size ());
	}

	/**
	 * Checks all records under one container, ensuring every record under the server container has a corresponding record under the language container
	 * @param serverContainer Element in the server XML that contains the records we want to check
	 * @param languageContainer Element in the language XML that contains the records we want to check
	 * @param higherLevelEntities Entities that we have to drill down through before reaching the final records that we want to check - each terminated by a /
	 * @param finalEntityName Tag identifying the lowest level record type
	 * @param finalFieldName Attribute that identifies each final record
	 * @param parentValues Values of identifying attributes of any parent record we have drilled down through to reach here
	 */
	private final void checkMultilevelReferences (final Element serverContainer, final Element languageContainer,
		final String higherLevelEntities, final String finalEntityName, final String finalFieldName, final String parentValues)
	{
		System.out.println ("Searching server and English XML files for parents of type " + higherLevelEntities);

		// First get name of just this higher level entity
		final int pos = higherLevelEntities.indexOf ("/");
		if (pos < 0)
			fail ("Higher level entities should always be terminated by a slash: \"" + higherLevelEntities + "\"");

		final String thisEntity = higherLevelEntities.substring (0, pos);
		final String remainingHigherLevelEntities = higherLevelEntities.substring (pos + 1);

		// Find all occurrences of this entity
		final List<Element> serverEntries = listElements (serverContainer.getChildNodes (), thisEntity);
		if (serverEntries.size () == 0)
			fail ("Server XML has no occurrences of entity \"" + thisEntity + "\" when searching multilevel reference");

		final List<Element> languageEntries = listElements (languageContainer.getChildNodes (), thisEntity);

		for (final Element thisServerRecord : serverEntries)
		{
			// We can't tell (not without some serious trawling through the XSD) what the name of the identifying attribute on parents is
			assertEquals ("Parent entity must have exactly 1 identifying attribute", 1, thisServerRecord.getAttributes ().getLength ());
			final String pkField = thisServerRecord.getAttributes ().item (0).getNodeName ();
			final String pkValue = thisServerRecord.getAttributes ().item (0).getNodeValue ();

			// Search for matching parent record in language XML
			Element thisLanguageRecord = null;
			final Iterator<Element> iter = languageEntries.iterator ();
			while ((thisLanguageRecord == null) && (iter.hasNext ()))
			{
				final Element iterRecord = iter.next ();
				if (iterRecord.getAttribute (pkField).equals (pkValue))
					thisLanguageRecord = iterRecord;
			}

			// If matching parent exists, then drill down into it
			// If it doesn't, then throw an error
			if (thisLanguageRecord == null)
				fail ("Parent " + thisEntity + " record with " + pkField + " = " + pkValue + " not found in English XML");

			final String parentValuesIncludingThisOne = (parentValues == null) ? pkValue : parentValues + "," + pkValue;

			if (remainingHigherLevelEntities.equals (""))
				checkReferences (thisServerRecord, thisLanguageRecord, finalEntityName, finalFieldName, parentValuesIncludingThisOne);
			else
				checkMultilevelReferences (thisServerRecord, thisLanguageRecord, remainingHigherLevelEntities, finalEntityName, finalFieldName, parentValuesIncludingThisOne);
		}
	}
}
