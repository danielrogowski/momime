package momime.client.graphics.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import momime.client.ClientTestData;
import momime.server.database.ServerDatabaseConstants;
import momime.server.database.ServerXsdResourceResolver;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;

import com.ndg.multiplayer.session.MultiplayerSessionBaseConstants;

/**
 * Tests reading the graphics XSD and XML files
 */
public final class TestGraphicsDatabase
{
	/** Prefix that we expect all keyrefs in the graphics XSD to begin with */
	private static final String SERVER_NAMESPACE_PREFIX = "momimesvr:";

	/** Prefix for references to the multiplayer session XSD */
	private static final String MULTIPLAYER_SESSION_NAMESPACE_PREFIX = "mps:";

	/** Fudge prefix identifying 2nd/3rd level references */
	private static final String MULTI_LEVEL_REFERENCE_PREFIX = "multilevelFKtosecondaryDB/";

	/**
	 * Tests that the default graphics XML file conforms to the graphics XSD
	 * @throws Exception If there is an error
	 */
	@Test
	public final void testDatabaseConformsToXSD () throws Exception
	{
		final URL xsdResource = getClass ().getResource (GraphicsDatabaseConstants.GRAPHICS_XSD_LOCATION);
		assertNotNull ("MoM IME graphics XSD could not be found on classpath", xsdResource);

		// Load XSD
		final SchemaFactory schemaFactory = SchemaFactory.newInstance (XMLConstants.W3C_XML_SCHEMA_NS_URI);
		schemaFactory.setResourceResolver (new ServerXsdResourceResolver (DOMImplementationRegistry.newInstance ()));
		final Validator xsd = schemaFactory.newSchema (xsdResource).newValidator ();

		// Load both XMLs in as DOM documents
		final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance ();
		builderFactory.setNamespaceAware (true);

		final DocumentBuilder builder = builderFactory.newDocumentBuilder ();

		final Document serverXml = builder.parse (ClientTestData.locateServerXmlFile ());
		final Document graphicsXml = builder.parse (ClientTestData.locateDefaultGraphicsXmlFile ());

		// Insert the server XML into the appropriate place in the graphics XML
		// You can't transfer nodes from one document to another, so we have to make a copy (import) of the entire server XML
		final Node serverXmlCopy = graphicsXml.importNode (serverXml.getDocumentElement (), true);
		graphicsXml.getDocumentElement ().insertBefore (serverXmlCopy, graphicsXml.getDocumentElement ().getChildNodes ().item (0));

		// Validate combined XML
		xsd.validate (new DOMSource (graphicsXml));
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
	 * Checking the default graphics XML file against the graphics XSD proves that
	 * 1) Every entry defined in the default graphics XML has a corresponding entry in the server XML
	 * 2) That there are no duplicates
	 *
	 * What it doesn't prove is that all the entries in the server XML and all possible enum values are covered, i.e. that we didn't miss any, so that is what this test checks
	 *
	 * Every type of entity present in the server XML isn't in the graphics XML, and we can't assume that all types that should be present in the graphics XML are,
	 * so the only way to do this properly is to look at all the xsd:keyrefs in the graphics XSD to get a definitive list of what entities should be present
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

		final Document serverXml = builder.parse (ClientTestData.locateServerXmlFile ());
		final Document graphicsXml = builder.parse (ClientTestData.locateDefaultGraphicsXmlFile ());

		final Document serverXsd = builder.parse (getClass ().getResourceAsStream (ServerDatabaseConstants.SERVER_XSD_LOCATION));
		final Document graphicsXsd = builder.parse (getClass ().getResourceAsStream (GraphicsDatabaseConstants.GRAPHICS_XSD_LOCATION));
		final Document multiplayerSessionXsd = builder.parse (getClass ().getResourceAsStream (MultiplayerSessionBaseConstants.MULTIPLAYER_SESSION_BASE_XSD_LOCATION));

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

		// Find all the keyrefs that link from the graphics XSD into the server XSD PKs
		final List<Element> graphicsElementList = listElements (graphicsXsd.getDocumentElement ().getChildNodes (), "xsd:element");
		assertEquals ("Graphics XSD should contain exactly one top level element", 1, graphicsElementList.size ());
		final Element graphicsElement = graphicsElementList.get (0);

		final List<Element> graphicsXsdKeyRefs = listElements (graphicsElement.getChildNodes (), "xsd:keyref");
		if (graphicsXsdKeyRefs.size () == 0)
			fail ("Could not find keyrefs in graphics XSD");

		for (final Element thisKeyRef : graphicsXsdKeyRefs)
		{
			// Pull all the necessary details out of the graphics XSD keyref
			assertEquals ("KeyRefs all expected to contain 2 attributes", 2, thisKeyRef.getAttributes ().getLength ());
			String pkName = thisKeyRef.getAttribute ("refer");

			// Graphics XSD has key refs to its own animation list, so only pay attention to the references to the server XSD
			if (pkName.startsWith (SERVER_NAMESPACE_PREFIX))
			{
				pkName = pkName.substring (SERVER_NAMESPACE_PREFIX.length ());

				final List<Element> graphicsSelectorList = listElements (thisKeyRef.getChildNodes (), "xsd:selector");
				assertEquals ("KeyRef expected to have exactly 1 selector", 1, graphicsSelectorList.size ());
				assertEquals ("KeyRef selector expected to have exactly 1 attribute", 1, graphicsSelectorList.get (0).getAttributes ().getLength ());
				final String graphicsEntity = graphicsSelectorList.get (0).getAttribute ("xpath");

				final List<Element> graphicsFieldList = listElements (thisKeyRef.getChildNodes (), "xsd:field");
				assertEquals ("KeyRef expected to have exactly 1 field", 1, graphicsFieldList.size ());
				assertEquals ("KeyRef field expected to have exactly 1 attribute", 1, graphicsFieldList.get (0).getAttributes ().getLength ());
				final String graphicsField = graphicsFieldList.get (0).getAttribute ("xpath");

				// Assume only attributes are identifying fields for which we must ensure all records are present
				if (graphicsField.startsWith ("@"))
				{
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

					// By convention, all entities and identifying attributes are named the same in the graphics and server XSDs
					// We have to enforce this assumption in order for the 2nd/3rd level references to work
					// 2nd/3rd level references are the only time we deviate from this
					// If they aren't equal then its just some common reference from another field, so we don't need to enforce that every record is present
					if (serverField.equals (graphicsField))
					{
						if (!graphicsEntity.startsWith (MULTI_LEVEL_REFERENCE_PREFIX))
						{
							// If they aren't equal then its just some common reference from another field, so we don't need to enforce that every record is present
							if (serverEntity.equals (graphicsEntity))
								checkReferences (serverXml.getDocumentElement (), graphicsXml.getDocumentElement (), serverEntity, serverField, null);
						}
						else
						{
							// 2nd/3rd level reference - and have proved that the lowest level identifying field matches
							if (!graphicsEntity.endsWith ("/" + serverEntity))
								fail ("2nd/3rd level references should end with the same entity (" + serverEntity + ", " + graphicsEntity + ")");

							// This gets a list of all higher level entities, each terminated by a /, excluding the final entity
							final String higherLevelEntities = graphicsEntity.substring (MULTI_LEVEL_REFERENCE_PREFIX.length (),
								graphicsEntity.length () - serverEntity.length ());

							checkMultilevelReferences (serverXml.getDocumentElement (), graphicsXml.getDocumentElement (), higherLevelEntities, serverEntity, serverField, null);
						}
					}
				}
			}
		}

		// Get list of complex types
		final List<Element> complexTypeList = listElements (graphicsXsd.getDocumentElement ().getChildNodes (), "xsd:complexType");
		final List<Element> multiplayerSessionSimpleTypes = listElements (multiplayerSessionXsd.getDocumentElement ().getChildNodes (), "xsd:simpleType");

		// Find all the PKs in the graphics XSD to find those which are keyed by enums
		final List<Element> graphicsXsdKeys = listElements (graphicsElement.getChildNodes (), "xsd:key");
		if (graphicsXsdKeys.size () == 0)
			fail ("Could not find keys in graphics XSD");

		for (final Element thisKey : graphicsXsdKeys)
		{
			final List<Element> graphicsSelectorList = listElements (thisKey.getChildNodes (), "xsd:selector");
			assertEquals ("Key expected to have exactly 1 selector", 1, graphicsSelectorList.size ());
			assertEquals ("Key selector expected to have exactly 1 attribute", 1, graphicsSelectorList.get (0).getAttributes ().getLength ());
			final String graphicsEntity = graphicsSelectorList.get (0).getAttribute ("xpath");

			final List<Element> graphicsFieldList = listElements (thisKey.getChildNodes (), "xsd:field");
			assertEquals ("Key expected to have exactly 1 field", 1, graphicsFieldList.size ());
			assertEquals ("Key field expected to have exactly 1 attribute", 1, graphicsFieldList.get (0).getAttributes ().getLength ());
			final String graphicsField = graphicsFieldList.get (0).getAttribute ("xpath");

			if (!graphicsField.startsWith ("@"))
				fail ("Key field expected to be an attribute, prefixed with @");

			// Find the complex type for this entity
			Element thisComplexType = null;
			final Iterator<Element> complexTypeIter = complexTypeList.iterator ();
			while ((thisComplexType == null) && (complexTypeIter.hasNext ()))
			{
				final Element searchComplexType = complexTypeIter.next ();
				if (searchComplexType.getAttribute ("name").equals (graphicsEntity))
					thisComplexType = searchComplexType;
			}

			assertNotNull ("Could not find definition of complex type \"" + graphicsEntity + "\" in graphics XSD", thisComplexType);

			// Expect all entries in graphics XSD to have exactly one attribute, and should match what we expect
			final List<Element> attributeList = listElements (thisComplexType.getChildNodes (), "xsd:attribute");
			assertEquals ("Complex types expected to have exactly 1 attribute", 1, attributeList.size ());
			final Element pkAttribute = attributeList.get (0);
			assertEquals ("Expected PK of complex type " + graphicsEntity + " to be " + graphicsField, graphicsField, "@" + pkAttribute.getAttribute ("name"));

			final String pkType = pkAttribute.getAttribute ("type");

			// This is a bit of a hack to find the entities we're interested in, it assumes that anything we're pulling from the multiplayer XSD is an enum
			// So this doesn't account for a) if we referenced something from the multiplayer XSD that isn't an enum or b) if we references enums from the server XSD
			// But, it works for now
			if (pkType.startsWith (MULTIPLAYER_SESSION_NAMESPACE_PREFIX))
			{
				// Find the definition in the multiplayer session XSD for the simple type
				Element thisSimpleType = null;
				final Iterator<Element> SimpleTypeIter = multiplayerSessionSimpleTypes.iterator ();
				while ((thisSimpleType == null) && (SimpleTypeIter.hasNext ()))
				{
					final Element searchSimpleType = SimpleTypeIter.next ();
					if (searchSimpleType.getAttribute ("name").equals (pkType.substring (MULTIPLAYER_SESSION_NAMESPACE_PREFIX.length ())))
						thisSimpleType = searchSimpleType;
				}

				assertNotNull ("Could not find definition of enum \"" + pkType + "\" in multiplayer session XSD", thisSimpleType);

				// Restriction node contains all the enum values
				final List<Element> restrictionsList = listElements (thisSimpleType.getChildNodes (), "xsd:restriction");
				assertEquals ("Enums types expected to have exactly 1 restriction", 1, restrictionsList.size ());
				checkEnums (restrictionsList.get (0), graphicsXml.getDocumentElement (), graphicsEntity, graphicsField);
			}
		}
	}

	/**
	 * Checks all records under one container, ensuring every record under the server container has a corresponding record under the graphics container
	 * @param serverContainer Element in the server XML that contains the records we want to check
	 * @param graphicsContainer Element in the graphics XML that contains the records we want to check
	 * @param entityName Tag identifying the types of record we want to check
	 * @param fieldName Attribute that identifies each record
	 * @param parentValues Values of identifying attributes of any parent record we have drilled down through to reach here
	 */
	private final void checkReferences (final Element serverContainer, final Element graphicsContainer, final String entityName, final String fieldName, final String parentValues)
	{
		final String parentValuesSuffix = (parentValues == null) ? "" : ", parent key(s) = " + parentValues;
		System.out.println ("Checking default graphics XML includes all references for entity " + entityName + ", field " + fieldName + parentValuesSuffix);

		final List<Element> serverEntries = listElements (serverContainer.getChildNodes (), entityName);
		final List<Element> graphicsEntries = listElements (graphicsContainer.getChildNodes (), entityName);

		String missingRecords = null;
		for (final Element thisServerRecord : serverEntries)
		{
			final String pkValue = thisServerRecord.getAttribute (fieldName.substring (1));
			assertNotNull ("Server has a null PK, on entity \"" + entityName + "\"", pkValue);

			boolean found = false;
			final Iterator<Element> iter = graphicsEntries.iterator ();
			while ((!found) && (iter.hasNext ()))
			{
				if (iter.next ().getAttribute (fieldName.substring (1)).equals (pkValue))
					found = true;
			}

			if (!found)
				missingRecords = (missingRecords == null) ? pkValue : missingRecords + "," + pkValue;
		}

		if (missingRecords != null)
			fail ("Graphics XML is missing " + entityName + " records for the following " + fieldName.substring (1) + "s: " +  missingRecords + parentValuesSuffix);

		// So, we proved that all server entries exist in the graphics XML
		// That doesn't yet prove that the graphics XML contains no spurious additional entries
		// For top level entities, the XSD will have checked that, but do it here for the benefit of 2nd/3rd level entities
		assertEquals ("Server and graphics XML files expected to contain same number of " + entityName + " records" + parentValuesSuffix,
			serverEntries.size (), graphicsEntries.size ());
	}

	/**
	 * Checks all records under one container, ensuring every record under the server container has a corresponding record under the graphics container
	 * @param serverContainer Element in the server XML that contains the records we want to check
	 * @param graphicsContainer Element in the graphics XML that contains the records we want to check
	 * @param higherLevelEntities Entities that we have to drill down through before reaching the final records that we want to check - each terminated by a /
	 * @param finalEntityName Tag identifying the lowest level record type
	 * @param finalFieldName Attribute that identifies each final record
	 * @param parentValues Values of identifying attributes of any parent record we have drilled down through to reach here
	 */
	private final void checkMultilevelReferences (final Element serverContainer, final Element graphicsContainer,
		final String higherLevelEntities, final String finalEntityName, final String finalFieldName, final String parentValues)
	{
		System.out.println ("Searching server and default graphics XML files for parents of type " + higherLevelEntities);

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

		final List<Element> graphicsEntries = listElements (graphicsContainer.getChildNodes (), thisEntity);

		for (final Element thisServerRecord : serverEntries)
		{
			// We can't tell (not without some serious trawling through the XSD) what the name of the identifying attribute on parents is
			assertEquals ("Parent entity must have exactly 1 identifying attribute", 1, thisServerRecord.getAttributes ().getLength ());
			final String pkField = thisServerRecord.getAttributes ().item (0).getNodeName ();
			final String pkValue = thisServerRecord.getAttributes ().item (0).getNodeValue ();

			// Search for matching parent record in graphics XML
			Element thisGraphicsRecord = null;
			final Iterator<Element> iter = graphicsEntries.iterator ();
			while ((thisGraphicsRecord == null) && (iter.hasNext ()))
			{
				final Element iterRecord = iter.next ();
				if (iterRecord.getAttribute (pkField).equals (pkValue))
					thisGraphicsRecord = iterRecord;
			}

			// If matching parent exists, then drill down into it
			// If it doesn't, then throw an error
			if (thisGraphicsRecord == null)
				fail ("Parent " + thisEntity + " record with " + pkField + " = " + pkValue + " not found in default graphics XML");

			final String parentValuesIncludingThisOne = (parentValues == null) ? pkValue : parentValues + "," + pkValue;

			if (remainingHigherLevelEntities.equals (""))
				checkReferences (thisServerRecord, thisGraphicsRecord, finalEntityName, finalFieldName, parentValuesIncludingThisOne);
			else
				checkMultilevelReferences (thisServerRecord, thisGraphicsRecord, remainingHigherLevelEntities, finalEntityName, finalFieldName, parentValuesIncludingThisOne);
		}
	}

	/**
	 * Checks all defined values for an enum, ensuring every possible value has a corresponding record under the graphics container
	 * @param enumValuesContainer Element in the multiplayer session XML that contains the enum values we want to check
	 * @param graphicsContainer Element in the graphics XML that contains the records we want to check
	 * @param entityName Tag identifying the types of record we want to check
	 * @param fieldName Attribute that identifies each record
	 */
	private final void checkEnums (final Element enumValuesContainer, final Element graphicsContainer, final String entityName, final String fieldName)
	{
		System.out.println ("Checking default graphics XML includes all references for enum " + entityName + ", field " + fieldName);

		final List<Element> enumEntries = listElements (enumValuesContainer.getChildNodes (), "xsd:enumeration");
		final List<Element> graphicsEntries = listElements (graphicsContainer.getChildNodes (), entityName);

		String missingRecords = null;
		for (final Element thisEnumRecord : enumEntries)
		{
			final String enumValue = thisEnumRecord.getAttribute ("value");
			assertNotNull ("Multiplayer session XSD has a null enum value for \"" + entityName + "\"", enumValue);

			boolean found = false;
			final Iterator<Element> iter = graphicsEntries.iterator ();
			while ((!found) && (iter.hasNext ()))
			{
				if (iter.next ().getAttribute (fieldName.substring (1)).equals (enumValue))
					found = true;
			}

			if (!found)
				missingRecords = (missingRecords == null) ? enumValue : missingRecords + "," + enumValue;
		}

		if (missingRecords != null)
			fail ("Graphics XML is missing " + entityName + " records for the following " + fieldName.substring (1) + "s: " +  missingRecords);

		// So, we proved that all server entries exist in the graphics XML
		// That doesn't yet prove that the graphics XML contains no spurious additional entries
		// For top level entities, the XSD will have checked that, but do it here for the benefit of 2nd/3rd level entities
		assertEquals ("Server and graphics XML files expected to contain same number of " + entityName + " records",
			enumEntries.size (), graphicsEntries.size ());
	}
}
