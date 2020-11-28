package momime.common.database;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Does consistency checks on the XSD itself
 */
public final class TestCommonXSD
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
		
		try (final InputStream in = getClass ().getResourceAsStream (CommonDatabaseConstants.COMMON_XSD_LOCATION))
		{
			try (final InputStreamReader reader = new InputStreamReader (in))
			{
				try (final BufferedReader buf = new BufferedReader (reader))
				{
					String line = buf.readLine ();
					while (line != null)
					{
						// Unbounded elements should all have primary keys defined
						// For now, leave out languageText, but really even those should stop duplicate entries
						if ((line.contains ("maxOccurs=\"unbounded\"")) && (!line.contains ("type=\"momimecommon:languageText\"")))
						{
							// Find the name
							final int pos = line.indexOf ("name=\"");
							if (pos >= 0)
							{
								final int pos2 = line.indexOf ("\"", pos + 6);
								if (pos2 >= 0)
									unboundedEntities.add (line.substring (pos + 6, pos2));
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
		
		// Remove any which are fine without a primary key
		unboundedEntities.remove ("movementRateRule");				// Ordered list of rules, so there's no key, just first matching rule gets chosen
		unboundedEntities.remove ("cityImage");								// Ordered list of images and associated conditions, so there's no key, just first matching image gets chosen
		unboundedEntities.remove ("cityViewElement");					// Ordered list of images and rules for whether to display each, so there's no key, drawn in order given
		unboundedEntities.remove ("combatTileBorderImage");			// Ordered list of images and associated conditions, so there's no key, just first matching image gets chosen
		unboundedEntities.remove ("combatMapElement");				// Ordered list of images and rules for whether to display each, so there's no key, drawn in order given
		unboundedEntities.remove ("attackResolutionStep");				// Ordered list of steps to take to resolve the combat, valid for same step can be repeated twice for Haste 
		unboundedEntities.remove ("smoothingReduction");				// Ordered list of rules to apply to bitmasks, so there's no key, all rules get evaluated
		unboundedEntities.remove ("addsToSkill");							// This is really a list of rules - the same attribute can be specified twice, e.g. see Resist Elements
		unboundedEntities.remove ("combatAreaEffectSkillBonus");	// Not unique without effectMagicRealm as some bonuses apply to 2 magic realms, but effectMagicRealm is optional
		unboundedEntities.remove ("heroItemSlot");							// Ordered list of 3 slots.  Mage type heroes tend to have 2 accessory slots, so the type of item isn't unique.
		unboundedEntities.remove ("smoothedTile");						// Bitmasks are intentionally not unique, so there can be multiple options for rendering the same tile
		unboundedEntities.remove ("frame");									// Animation frames sometimes repeat, e.g. there's many 1-2-3-2 patterns
		unboundedEntities.remove ("audioFile");								// Similar to animations, there's no real need for tracks in a playlist to be blocked from repeating
		
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
			fail ("Common XSD is missing the following primary keys: " + s);
	}
}