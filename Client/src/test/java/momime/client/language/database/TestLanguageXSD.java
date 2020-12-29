package momime.client.language.database;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

/**
 * Does consistency checks on the XSD itself
 */
public final class TestLanguageXSD
{
	/**
	 * Ensures all PKs are defined.  Not really clever enough to know which element the PK should be defined under, just whether it is there or not.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testPrimaryKeysExist () throws Exception
	{
		// First parse the whole XSD, finding all unbounded entities and all primary keys
		final List<String> unboundedEntities = new ArrayList<String> ();
		final List<String> keyDefinitions = new ArrayList<String> ();
		
		try (final InputStream in = getClass ().getResourceAsStream (LanguageDatabaseConstants.LANGUAGES_XSD_LOCATION))
		{
			try (final InputStreamReader reader = new InputStreamReader (in))
			{
				try (final BufferedReader buf = new BufferedReader (reader))
				{
					// We need to know the name of the complex type that each unbounded element is part of
					String complexTypeName = null;
					
					String line = buf.readLine ();
					while (line != null)
					{
						// Keep track of start/end of complex types
						if (line.contains ("<xsd:complexType"))
						{
							final int pos = line.indexOf ("<xsd:complexType name=\"");
							if (pos >= 0)
							{
								final int pos2 = line.indexOf ("\"", pos + 23);
								if (pos2 >= 0)
									complexTypeName = line.substring (pos + 23, pos2);
							}
							else
								// Its the top level complex type, which has no name
								complexTypeName = null;
						}
						
						else if (line.contains ("</xsd:complexType>"))
							complexTypeName = null;
						
						// Unbounded elements should all have primary keys defined
						else if (line.contains ("maxOccurs=\"unbounded\""))
						{
							// Find the name
							final int pos = line.indexOf ("name=\"");
							if (pos >= 0)
							{
								final int pos2 = line.indexOf ("\"", pos + 6);
								if (pos2 >= 0)
								{
									final String unboundedEntity = line.substring (pos + 6, pos2);
									
									final String keyName;
									if (complexTypeName == null)
										keyName = unboundedEntity;

									// Complex type name prefix is already there, e.g. don't turn knownServerDescription into knownServerKnownServerDescription
									else if (unboundedEntity.startsWith (complexTypeName))
										keyName = unboundedEntity;

									// Unlike common XSD, there's a lot of identically named elements in different types in the language XSD, e.g. "title"
									// so need to prefix it by the complex type name to generate key names that are unique
									else
										keyName = complexTypeName + unboundedEntity.substring (0, 1).toUpperCase () + unboundedEntity.substring (1);
										
									unboundedEntities.add (keyName);
								}
							}
						}
						
						else if (line.contains ("<xsd:key name=\""))
						{
							final int pos = line.indexOf ("<xsd:key name=\"");
							if (pos >= 0)
							{
								final int pos2 = line.indexOf ("\"", pos + 15);
								if (pos2 >= 0)
									keyDefinitions.add (line.substring (pos + 15, pos2));
							}
						}

						else if (line.contains ("<xsd:unique name=\""))
						{
							final int pos = line.indexOf ("<xsd:unique name=\"");
							if (pos >= 0)
							{
								final int pos2 = line.indexOf ("\"", pos + 18);
								if (pos2 >= 0)
									keyDefinitions.add (line.substring (pos + 18, pos2));
							}
						}
						
						// Next line
						line = buf.readLine ();
					}
				}
			}
		}
		
		// Now find list of all unbounded entities without PKs
		final StringBuilder s = new StringBuilder ();
		for (final String unboundedEntity : unboundedEntities)
		{
			final String primaryKeyName = unboundedEntity + "PK";
			if (!keyDefinitions.contains (primaryKeyName))
			{
				if (s.length () > 0)
					s.append (", ");
				
				s.append (primaryKeyName);
			}
		}
		
		if (s.length () > 0)
			fail ("Language XSD is missing the following primary keys: " + s);
	}

	/**
	 * Ensures all entities are ordered like:
	 * 1) Special descriptions, where the language text strings don't hold enough detailed information
	 * 2) Language text strings
	 * 3) Mandatory elements
	 * 4) Optional elements
	 * 5) Multi-valued elements
	 * 6) Child entities
	 * 
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testFieldOrdering () throws Exception
	{
		// First parse the whole XSD, finding all elements of all complex types
		final Map<String, List<String>> complexTypes = new HashMap<String, List<String>> ();
		
		// Indicates if we're currently inside a complex type definition
		String complexTypeName = null;
		
		try (final InputStream in = getClass ().getResourceAsStream (LanguageDatabaseConstants.LANGUAGES_XSD_LOCATION))
		{
			try (final InputStreamReader reader = new InputStreamReader (in))
			{
				try (final BufferedReader buf = new BufferedReader (reader))
				{
					String line = buf.readLine ();
					while (line != null)
					{
						// Want all complex types except the main unnamed one
						if (line.contains ("<xsd:complexType name=\""))
						{
							// Find the name
							final int pos = line.indexOf ("<xsd:complexType name=\"");
							if (pos >= 0)
							{
								final int pos2 = line.indexOf ("\"", pos + 23);
								if (pos2 >= 0)
								{
									complexTypeName = line.substring (pos + 23, pos2);
									complexTypes.put (complexTypeName, new ArrayList<String> ());
								}
							}
						}
						
						// Notice when we are at the end of a complex type
						else if (line.contains ("</xsd:complexType>"))
							complexTypeName = null;
						
						// Anything else we only care about if we're currently inside a complex type definition, and then only care about elements
						else if ((complexTypeName != null) && (line.contains ("<xsd:element ")))
							complexTypes.get (complexTypeName).add (line);
						
						// Next line
						line = buf.readLine ();
					}
				}
			}
		}

		// Now check each complex type
		for (final Entry<String, List<String>> complexType : complexTypes.entrySet ())
			
			// This is ordered like "if condition AND condition AND condition THEN action AND action" so forcing it into a different order would make it confusing
			if (!complexType.getKey ().equals ("smoothingReduction")) 
			{
				// Indicates last kind of child element encountered
				Integer lastElementType = null;
	
				for (final String line : complexType.getValue ())
				{
					// Get all the values from the line
					final int minOccursPos = line.indexOf ("minOccurs=\"");
					final int maxOccursPos = line.indexOf ("maxOccurs=\"");
					final int namePos = line.indexOf ("name=\"");
					final int typePos = line.indexOf ("type=\"");
					
					if ((minOccursPos < 0) || (maxOccursPos < 0) || (namePos < 0) || (typePos < 0))
						fail ("Element line of type " + complexType.getKey () + " missing expected value: " + line);
					
					final int minOccursPos2 = line.indexOf ("\"", minOccursPos + 11);
					final int maxOccursPos2 = line.indexOf ("\"", maxOccursPos + 11);
					final int namePos2 = line.indexOf ("\"", namePos + 6);
					final int typePos2 = line.indexOf ("\"", typePos + 6);
	
					if ((minOccursPos2 < 0) || (maxOccursPos2 < 0) || (namePos2 < 0) || (typePos2 < 0))
						fail ("Element line of type " + complexType.getKey () + " has value missing closing quote: " + line);
					
					final String minOccurs = line.substring (minOccursPos + 11, minOccursPos2);
					final String maxOccurs = line.substring (maxOccursPos + 11, maxOccursPos2);
					final String name = line.substring (namePos + 6, namePos2);
					final String type = line.substring (typePos + 6, typePos2);
					
					// Check values are sensible
					if ((!minOccurs.equals ("0")) && (!minOccurs.equals ("1")))
						fail ("Element line of type " + complexType.getKey () + " name " + name + " has minOccurs value of " + minOccurs);
					
					if ((!maxOccurs.equals ("1")) && (!maxOccurs.equals ("unbounded")))
						fail ("Element line of type " + complexType.getKey () + " name " + name + " has maxOccurs value of " + maxOccurs);
					
					// Use the other values to work out what type of element it is
					final int thisElementType;
					if ((name.contains ("DetailedDescription")) || (name.equals ("unitTypeDescription")))
					{
						// Special "description" type values that are just for annotating the XML and are never actually used, so they should go above even the language text entries
						thisElementType = 1;
					}
					else if (type.equals ("momimecommon:languageText"))
						thisElementType = 2;
					else if (maxOccurs.equals ("1"))
						thisElementType = minOccurs.equals ("1") ? 3 : 4;
					else if (maxOccurs.equals ("unbounded"))
					{
						// If its the name of another complex type then its a child entity; if not then assume its just a multi-value field
						if ((type.startsWith ("momimecommon:")) && (complexTypes.containsKey (type.substring (13))))
							thisElementType = 6;
						else
							thisElementType = 5;
					}
					else
					{
						fail ("Element line of type " + complexType.getKey () + " name " + name + " has maxOccurs value of " + maxOccurs);
						thisElementType = 0;
					}
					
					// Check value is same or later
					if ((lastElementType != null) && (thisElementType < lastElementType))
						fail ("Element line of type " + complexType.getKey () + " name " + name + " is type " + thisElementType + " but appears after a type " + lastElementType + " element");
					
					lastElementType = thisElementType;
				}
			}
	}
}