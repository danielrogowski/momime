package momime.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.database.CitySpellEffect;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.UnitSpellEffect;
import momime.common.messages.MemoryMaintainedSpell;

/**
 * Tests the MemoryMaintainedSpellUtils class
 */
@ExtendWith(MockitoExtension.class)
public final class TestMemoryMaintainedSpellUtilsImpl
{
	/**
	 * Tests the findMaintainedSpell method with only the spell ID specified
	 */
	@Test
	public final void testFindMaintainedSpell_SpellIDOnly ()
	{
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		for (int n = 1; n <= 5; n++)
		{
			final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
			spell.setSpellID ("SP00" + n);

			// Fill in a bunch of other stuff against the records to prove that all these other search values get ignored because we pass in nulls
			spell.setCastingPlayerID (10 + n);
			spell.setUnitSkillID ("SS02" + n);
			spell.setCitySpellEffectID ("CSE03" + n);

			spells.add (spell);
		}

		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		assertEquals ("SP004", utils.findMaintainedSpell (spells, null, "SP004", null, null, null, null).getSpellID ());
		assertNull (utils.findMaintainedSpell (spells, null, "SP006", null, null, null, null));
	}

	/**
	 * Tests the findMaintainedSpell method with only the player ID specified
	 */
	@Test
	public final void testFindMaintainedSpell_PlayerIDOnly ()
	{
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		for (int n = 1; n <= 5; n++)
		{
			final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
			spell.setSpellID ("SP00" + n);
			spell.setCastingPlayerID (10 + n);
			spell.setUnitSkillID ("SS02" + n);
			spell.setCitySpellEffectID ("CSE03" + n);

			spells.add (spell);
		}

		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		assertEquals (14, utils.findMaintainedSpell (spells, 14, null, null, null, null, null).getCastingPlayerID ());
		assertNull (utils.findMaintainedSpell (spells, 16, null, null, null, null, null));
	}

	/**
	 * Tests the findMaintainedSpell method with the spell ID and player ID specified
	 */
	@Test
	public final void testFindMaintainedSpell_SpellIDAndPlayer ()
	{
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		for (int n = 1; n <= 5; n++)
		{
			final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
			spell.setSpellID ("SP001");		// Note they're all the same spell
			spell.setCastingPlayerID (10 + n);
			spell.setUnitSkillID ("SS02" + n);
			spell.setCitySpellEffectID ("CSE03" + n);

			spells.add (spell);
		}

		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		assertEquals (14, utils.findMaintainedSpell (spells, 14, "SP001", null, null, null, null).getCastingPlayerID ());
		assertNull (utils.findMaintainedSpell (spells, 16, "SP001", null, null, null, null));
	}

	/**
	 * We're proved that searching on null player acts as a wildcard and finds spells with any player
	 * Now prove that searching on null unitURN works differently and won't find spells with a unitURN specified
	 */
	@Test
	public final void testFindMaintainedSpell_UnitUrnMandatory ()
	{
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		for (int n = 1; n <= 5; n++)
		{
			final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
			spell.setSpellID ("SP00" + n);
			spell.setCastingPlayerID (10 + n);
			spell.setUnitSkillID ("SS02" + n);
			spell.setCitySpellEffectID ("CSE03" + n);
			spell.setUnitURN (40 + n);

			spells.add (spell);
		}

		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		assertNull (utils.findMaintainedSpell (spells, null, "SP004", null, null, null, null));
	}

	/**
	 * We're proved that searching on null player acts as a wildcard and finds spells with any player
	 * Now prove that searching on null cityLocation works differently and won't find spells with a cityLocation specified
	 */
	@Test
	public final void testFindMaintainedSpell_CityLocationMandatory ()
	{
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		for (int n = 1; n <= 5; n++)
		{
			final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
			spell.setSpellID ("SP00" + n);
			spell.setCastingPlayerID (10 + n);
			spell.setUnitSkillID ("SS02" + n);
			spell.setCitySpellEffectID ("CSE03" + n);
			spell.setCityLocation (new MapCoordinates3DEx (25, 10, 1));

			spells.add (spell);
		}

		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		assertNull (utils.findMaintainedSpell (spells, null, "SP004", null, null, null, null));
	}

	/**
	 * Tests the findMaintainedSpell method with only the unit URN specified
	 */
	@Test
	public final void testFindMaintainedSpell_UnitOnly ()
	{
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		for (int n = 1; n <= 5; n++)
		{
			final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
			spell.setSpellID ("SP00" + n);
			spell.setCastingPlayerID (10 + n);
			spell.setUnitSkillID ("SS02" + n);
			spell.setCitySpellEffectID ("CSE03" + n);
			spell.setUnitURN (40 + n);

			spells.add (spell);
		}

		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		assertEquals (44, utils.findMaintainedSpell (spells, null, null, 44, null, null, null).getUnitURN ().intValue ());
		assertNull (utils.findMaintainedSpell (spells, null, null, 46, null, null, null));
	}

	/**
	 * Tests the findMaintainedSpell method to look for a specific skill granted by a specific spell
	 */
	@Test
	public final void testFindMaintainedSpell_UnitAndSkill ()
	{
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		for (int n = 1; n <= 5; n++)
		{
			final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
			spell.setSpellID ("SP00" + n);
			spell.setCastingPlayerID (10 + n);
			spell.setUnitSkillID ("SS02" + n);
			spell.setCitySpellEffectID ("CSE03" + n);
			spell.setUnitURN (40 + n);

			spells.add (spell);
		}

		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		assertEquals ("SS024", utils.findMaintainedSpell (spells, null, "SP004", 44, "SS024", null, null).getUnitSkillID ());
		assertNull (utils.findMaintainedSpell (spells, null, "SP004", 44, "SS025", null, null));
	}

	/**
	 * Tests the findMaintainedSpell method to look for a specific skill but we don't care what spell granted it
	 */
	@Test
	public final void testFindMaintainedSpell_UnitSkillOnly ()
	{
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		for (int n = 1; n <= 5; n++)
		{
			final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
			spell.setSpellID ("SP00" + n);
			spell.setCastingPlayerID (10 + n);
			spell.setUnitSkillID ("SS02" + n);
			spell.setCitySpellEffectID ("CSE03" + n);
			spell.setUnitURN (40 + n);

			spells.add (spell);
		}

		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		assertEquals ("SS024", utils.findMaintainedSpell (spells, null, null, 44, "SS024", null, null).getUnitSkillID ());
		assertNull (utils.findMaintainedSpell (spells, null, null, 44, "SS025", null, null));
	}

	/**
	 * Tests the findMaintainedSpell method with only the city location specified
	 */
	@Test
	public final void testFindMaintainedSpell_CityLocationOnly ()
	{
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		for (int n = 1; n <= 5; n++)
		{
			final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
			spell.setSpellID ("SP00" + n);
			spell.setCastingPlayerID (10 + n);
			spell.setUnitSkillID ("SS02" + n);
			spell.setCitySpellEffectID ("CSE03" + n);
			spell.setCityLocation (new MapCoordinates3DEx (100 + n, 200 + n, 300 + n));

			spells.add (spell);
		}

		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (104, 204, 304);
		cityLocation.setX (104);
		cityLocation.setY (204);
		cityLocation.setZ (304);

		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		assertEquals ("SP004", utils.findMaintainedSpell (spells, null, null, null, null, cityLocation, null).getSpellID ());

		cityLocation.setZ (305);
		assertNull (utils.findMaintainedSpell (spells, null, null, null, null, cityLocation, null));
	}

	/**
	 * Tests the findMaintainedSpell method looking for a specific city effect granted by a specific spell
	 */
	@Test
	public final void testFindMaintainedSpell_CityAndSpecificEffect ()
	{
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		for (int n = 1; n <= 5; n++)
		{
			final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
			spell.setSpellID ("SP00" + n);
			spell.setCastingPlayerID (10 + n);
			spell.setUnitSkillID ("SS02" + n);
			spell.setCitySpellEffectID ("CSE03" + n);
			spell.setCityLocation (new MapCoordinates3DEx (100 + n, 200 + n, 300 + n));

			spells.add (spell);
		}

		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (104, 204, 304);

		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		assertEquals ("SP004", utils.findMaintainedSpell (spells, null, "SP004", null, null, cityLocation, "CSE034").getSpellID ());
		assertNull (utils.findMaintainedSpell (spells, null, "SP004", null, null, cityLocation, "CSE035"));
	}

	/**
	 * Tests the findMaintainedSpell method looking for a specific city effect but we don't care which spell granted it
	 */
	@Test
	public final void testFindMaintainedSpell_CityEffectOnly ()
	{
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		for (int n = 1; n <= 5; n++)
		{
			final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
			spell.setSpellID ("SP00" + n);
			spell.setCastingPlayerID (10 + n);
			spell.setUnitSkillID ("SS02" + n);
			spell.setCitySpellEffectID ("CSE03" + n);
			spell.setCityLocation (new MapCoordinates3DEx (100 + n, 200 + n, 300 + n));

			spells.add (spell);
		}

		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (104, 204, 304);

		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		assertEquals ("SP004", utils.findMaintainedSpell (spells, null, null, null, null, cityLocation, "CSE034").getSpellID ());
		assertNull (utils.findMaintainedSpell (spells, null, null, null, null, cityLocation, "CSE035"));
	}

	/**
	 * Tests the findSpellURN method on a spell that does exist
	 * @throws RecordNotFoundException If spell with requested URN is not found
	 */
	@Test
	public final void testFindSpellURN_Exists () throws RecordNotFoundException
	{
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		for (int n = 1; n <= 3; n++)
		{
			final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
			spell.setSpellURN (n);
			spells.add (spell);
		}

		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		assertEquals (2, utils.findSpellURN (2, spells).getSpellURN ());
		assertEquals (2, utils.findSpellURN (2, spells, "testFindSpellURN_Exists").getSpellURN ());
	}

	/**
	 * Tests the findSpellURN method on a spell that doesn't exist
	 * @throws RecordNotFoundException If spell with requested URN is not found
	 */
	@Test
	public final void testFindSpellURN_NotExists () throws RecordNotFoundException
	{
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		for (int n = 1; n <= 3; n++)
		{
			final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
			spell.setSpellURN (n);
			spells.add (spell);
		}

		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		assertNull (utils.findSpellURN (4, spells));
		
		assertThrows (RecordNotFoundException.class, () ->
		{
			utils.findSpellURN (4, spells, "testFindSpellURN_NotExists");
		});
	}

	/**
	 * Tests the removeSpellURN method on a spell that does exist
	 * @throws RecordNotFoundException If spell with requested URN is not found
	 */
	@Test
	public final void testRemoveSpellURN_Exists () throws RecordNotFoundException
	{
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		for (int n = 1; n <= 3; n++)
		{
			final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
			spell.setSpellURN (n);
			spells.add (spell);
		}

		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.removeSpellURN (2, spells);
		assertEquals (2, spells.size ());
		assertEquals (1, spells.get (0).getSpellURN ());
		assertEquals (3, spells.get (1).getSpellURN ());
	}

	/**
	 * Tests the removeSpellURN method on a spell that doesn't exist
	 * @throws RecordNotFoundException If spell with requested URN is not found
	 */
	@Test
	public final void testRemoveSpellURN_NotExists () throws RecordNotFoundException
	{
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		for (int n = 1; n <= 3; n++)
		{
			final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
			spell.setSpellURN (n);
			spells.add (spell);
		}

		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		assertThrows (RecordNotFoundException.class, () ->
		{
			utils.removeSpellURN (4, spells);
		});
	}

	/**
	 * Tests the removeSpellsCastOnUnitStack method
	 */
	@Test
	public final void testRemoveSpellsCastOnUnitStack ()
	{
		// Generate a sample list of spells, including some non-unit spells (UnitURN is null),
		// some cast on units in the stack and some cast on units not in the stack

		// This builds a list like null, 1, null, 2, null, 3... null, 5
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		for (int n = 1; n <= 5; n++)
		{
			final MemoryMaintainedSpell nonUnitSpell = new MemoryMaintainedSpell ();
			nonUnitSpell.setSpellID ("SP00" + n + "NonUnit");
			spells.add (nonUnitSpell);

			final MemoryMaintainedSpell unitSpell = new MemoryMaintainedSpell ();
			unitSpell.setSpellID ("SP00" + n);
			unitSpell.setUnitURN (n);
			spells.add (unitSpell);
		}

		// Then remove just the spells with unit URNs 2 and 4
		final List<Integer> unitURNs = new ArrayList<Integer> ();
		unitURNs.add (2);
		unitURNs.add (4);

		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.removeSpellsCastOnUnitStack (spells, unitURNs);

		assertEquals (8, spells.size ());
		assertEquals ("SP001NonUnit", spells.get (0).getSpellID ());
		assertEquals ("SP001", spells.get (1).getSpellID ());
		assertEquals ("SP002NonUnit", spells.get (2).getSpellID ());
		assertEquals ("SP003NonUnit", spells.get (3).getSpellID ());
		assertEquals ("SP003", spells.get (4).getSpellID ());
		assertEquals ("SP004NonUnit", spells.get (5).getSpellID ());
		assertEquals ("SP005NonUnit", spells.get (6).getSpellID ());
		assertEquals ("SP005", spells.get (7).getSpellID ());
	}
	
	/**
	 * Tests the listUnitSpellEffectsNotYetCastOnUnit method when the spell has no effects listed
	 */
	@Test
	public final void testListUnitSpellEffectsNotYetCastOnUnit_noSpellEffects ()
	{
		// Spell definition
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");

		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Call method
		assertNull (utils.listUnitSpellEffectsNotYetCastOnUnit (null, spell, 1, 10));
	}

	/**
	 * Tests the listUnitSpellEffectsNotYetCastOnUnit method when the spell has a single effect, and the unit doesn't have that effect yet
	 */
	@Test
	public final void testListUnitSpellEffectsNotYetCastOnUnit_effectNotYetOnUnit ()
	{
		// Spell definition
		final UnitSpellEffect effectA = new UnitSpellEffect ();
		effectA.setUnitSkillID ("A");

		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.getUnitSpellEffect ().add (effectA);

		// Existing spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Call method
		final List<UnitSpellEffect> list = utils.listUnitSpellEffectsNotYetCastOnUnit (spells, spell, 1, 10);
		
		// Check results
		assertEquals (1, list.size ());
		assertEquals ("A", list.get (0).getUnitSkillID ());
	}

	/**
	 * Tests the listUnitSpellEffectsNotYetCastOnUnit method when the spell has a single effect, and the unit doesn't have that effect yet
	 */
	@Test
	public final void testListUnitSpellEffectsNotYetCastOnUnit_effectAlreadyOnUnit ()
	{
		// Spell definition
		final UnitSpellEffect effectA = new UnitSpellEffect ();
		effectA.setUnitSkillID ("A");

		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.getUnitSpellEffect ().add (effectA);

		// Existing spells
		final MemoryMaintainedSpell existingEffectA = new MemoryMaintainedSpell ();
		existingEffectA.setSpellID ("SP001");
		existingEffectA.setCastingPlayerID (1);
		existingEffectA.setUnitSkillID ("A");
		existingEffectA.setUnitURN (10);

		final List<MemoryMaintainedSpell> spells = Arrays.asList (existingEffectA);

		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Call method
		assertEquals (0, utils.listUnitSpellEffectsNotYetCastOnUnit (spells, spell, 1, 10).size ());
	}

	/**
	 * Tests the listUnitSpellEffectsNotYetCastOnUnit method when the spell has multiple effects, and the unit only has one of them already
	 */
	@Test
	public final void testListUnitSpellEffectsNotYetCastOnUnit_multipleEffects ()
	{
		// Spell definition
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");

		for (final String effectID : new String [] {"A", "B", "C", "D"})
		{
			final UnitSpellEffect effectB = new UnitSpellEffect ();
			effectB.setUnitSkillID (effectID);
			spell.getUnitSpellEffect ().add (effectB);
		}
		
		// Existing spells
		final MemoryMaintainedSpell existingEffectA = new MemoryMaintainedSpell ();
		existingEffectA.setSpellID ("SP001");
		existingEffectA.setCastingPlayerID (1);
		existingEffectA.setUnitSkillID ("A");
		existingEffectA.setUnitURN (10);

		// One with wrong spell ID
		final MemoryMaintainedSpell existingEffectB = new MemoryMaintainedSpell ();
		existingEffectB.setSpellID ("SP002");
		existingEffectB.setCastingPlayerID (1);
		existingEffectB.setUnitSkillID ("B");
		existingEffectB.setUnitURN (10);
		
		// One for wrong player
		final MemoryMaintainedSpell existingEffectC = new MemoryMaintainedSpell ();
		existingEffectC.setSpellID ("SP001");
		existingEffectC.setCastingPlayerID (2);
		existingEffectC.setUnitSkillID ("C");
		existingEffectC.setUnitURN (10);
		
		// One in wrong unit
		final MemoryMaintainedSpell existingEffectD = new MemoryMaintainedSpell ();
		existingEffectD.setSpellID ("SP001");
		existingEffectD.setCastingPlayerID (1);
		existingEffectD.setUnitSkillID ("D");
		existingEffectD.setUnitURN (11);
		
		final List<MemoryMaintainedSpell> spells = Arrays.asList (existingEffectA, existingEffectB, existingEffectC, existingEffectD);

		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Call method
		final List<UnitSpellEffect> list = utils.listUnitSpellEffectsNotYetCastOnUnit (spells, spell, 1, 10);
		
		// Check results
		assertEquals (3, list.size ());
		assertEquals ("B", list.get (0).getUnitSkillID ());
		assertEquals ("C", list.get (1).getUnitSkillID ());
		assertEquals ("D", list.get (2).getUnitSkillID ());
	}

	/**
	 * Tests the listUnitSpellEffectsNotYetCastOnUnit method when the spell has a permanent effect, so won't be listed
	 */
	@Test
	public final void testListUnitSpellEffectsNotYetCastOnUnit_permanent ()
	{
		// Spell definition
		final UnitSpellEffect effectA = new UnitSpellEffect ();
		effectA.setUnitSkillID ("A");
		effectA.setPermanent (true);

		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.getUnitSpellEffect ().add (effectA);

		// Existing spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Call method
		assertEquals (0, utils.listUnitSpellEffectsNotYetCastOnUnit (spells, spell, 1, 10).size ());
	}

	/**
	 * Tests the listUnitSpellEffectsNotYetCastOnUnit method on the stasis spell, where the effect on the unit may have a different ID that the spell definition
	 */
	@Test
	public final void testListUnitSpellEffectsNotYetCastOnUnit_stasis ()
	{
		// Spell definition
		final UnitSpellEffect effectA = new UnitSpellEffect ();
		effectA.setUnitSkillID (CommonDatabaseConstants.UNIT_SKILL_ID_STASIS_FIRST_TURN);

		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.getUnitSpellEffect ().add (effectA);

		// Existing spells
		final MemoryMaintainedSpell existingEffectA = new MemoryMaintainedSpell ();
		existingEffectA.setSpellID ("SP001");
		existingEffectA.setCastingPlayerID (1);
		existingEffectA.setUnitSkillID (CommonDatabaseConstants.UNIT_SKILL_ID_STASIS_LATER_TURNS);
		existingEffectA.setUnitURN (10);

		final List<MemoryMaintainedSpell> spells = Arrays.asList (existingEffectA);

		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Call method
		assertEquals (0, utils.listUnitSpellEffectsNotYetCastOnUnit (spells, spell, 1, 10).size ());
	}
	
	/**
	 * Tests the listUnitSpellEffectsNotYetCastOnUnit method with web, where you CAN web a unit again even if it already has a web on it, as long as the web has been cut through
	 */
	@Test
	public final void testListUnitSpellEffectsNotYetCastOnUnit_webCutThrough ()
	{
		// Spell definition
		final UnitSpellEffect effectA = new UnitSpellEffect ();
		effectA.setUnitSkillID (CommonDatabaseConstants.UNIT_SKILL_ID_WEB);

		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.getUnitSpellEffect ().add (effectA);

		// Existing spells
		final MemoryMaintainedSpell existingEffectA = new MemoryMaintainedSpell ();
		existingEffectA.setSpellID ("SP001");
		existingEffectA.setCastingPlayerID (1);
		existingEffectA.setUnitSkillID (CommonDatabaseConstants.UNIT_SKILL_ID_WEB);
		existingEffectA.setUnitURN (10);

		final List<MemoryMaintainedSpell> spells = Arrays.asList (existingEffectA);

		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Call method
		final List<UnitSpellEffect> list = utils.listUnitSpellEffectsNotYetCastOnUnit (spells, spell, 1, 10);
		
		// Check results
		assertEquals (1, list.size ());
		assertEquals (CommonDatabaseConstants.UNIT_SKILL_ID_WEB, list.get (0).getUnitSkillID ());
	}
	
	/**
	 * Tests the listUnitSpellEffectsNotYetCastOnUnit method with web, where the existing web is still intact so can't cast another one
	 */
	@Test
	public final void testListUnitSpellEffectsNotYetCastOnUnit_webIntact ()
	{
		// Spell definition
		final UnitSpellEffect effectA = new UnitSpellEffect ();
		effectA.setUnitSkillID (CommonDatabaseConstants.UNIT_SKILL_ID_WEB);

		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.getUnitSpellEffect ().add (effectA);

		// Existing spells
		final MemoryMaintainedSpell existingEffectA = new MemoryMaintainedSpell ();
		existingEffectA.setSpellID ("SP001");
		existingEffectA.setCastingPlayerID (1);
		existingEffectA.setUnitSkillID (CommonDatabaseConstants.UNIT_SKILL_ID_WEB);
		existingEffectA.setUnitURN (10);
		existingEffectA.setVariableDamage (1);	// HP of the web

		final List<MemoryMaintainedSpell> spells = Arrays.asList (existingEffectA);

		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Call method
		assertEquals (0, utils.listUnitSpellEffectsNotYetCastOnUnit (spells, spell, 1, 10).size ());
	}
	
	/**
	 * Tests the listCitySpellEffectsNotYetCastAtLocation method
	 */
	@Test
	public final void testListCitySpellEffectsNotYetCastAtLocation ()
	{
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		
		final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Spell has no citySpellEffectIDs defined
		assertNull (utils.listCitySpellEffectsNotYetCastAtLocation (spells, spell, 1, cityLocation));
		
		// Spell with exactly one citySpellEffectID, that isn't cast yet
		spell.getSpellHasCityEffect ().add ("A");
		
		final List<String> listOne = utils.listCitySpellEffectsNotYetCastAtLocation (spells, spell, 1, cityLocation);
		assertEquals (1, listOne.size ());
		assertEquals ("A", listOne.get (0));

		// Spell with exactly one citySpellEffectID, that is already cast yet
		final MemoryMaintainedSpell existingEffectA = new MemoryMaintainedSpell ();
		existingEffectA.setSpellID ("SP001");
		existingEffectA.setCastingPlayerID (1);
		existingEffectA.setCitySpellEffectID ("A");
		existingEffectA.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		spells.add (existingEffectA);
		
		final List<String> listZero = utils.listCitySpellEffectsNotYetCastAtLocation (spells, spell, 1, cityLocation);
		assertEquals (0, listZero.size ());
		
		// Add three more effects
		for (final String effectID : new String [] {"B", "C", "D"})
			spell.getSpellHasCityEffect ().add (effectID);
		
		// One with wrong spell ID
		final MemoryMaintainedSpell existingEffectB = new MemoryMaintainedSpell ();
		existingEffectB.setSpellID ("SP002");
		existingEffectB.setCastingPlayerID (1);
		existingEffectB.setCitySpellEffectID ("B");
		existingEffectB.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		spells.add (existingEffectB);
		
		// One for wrong player
		final MemoryMaintainedSpell existingEffectC = new MemoryMaintainedSpell ();
		existingEffectC.setSpellID ("SP001");
		existingEffectC.setCastingPlayerID (2);
		existingEffectC.setCitySpellEffectID ("C");
		existingEffectC.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		spells.add (existingEffectC);
		
		// One in wrong location
		final MemoryMaintainedSpell existingEffectD = new MemoryMaintainedSpell ();
		existingEffectD.setSpellID ("SP001");
		existingEffectD.setCastingPlayerID (1);
		existingEffectD.setCitySpellEffectID ("D");
		existingEffectD.setCityLocation (new MapCoordinates3DEx (20, 11, 1));
		spells.add (existingEffectD);
		
		// All three effect should still be listed
		final List<String> listThree = utils.listCitySpellEffectsNotYetCastAtLocation (spells, spell, 1, cityLocation);
		assertEquals (3, listThree.size ());
		assertEquals ("B", listThree.get (0));
		assertEquals ("C", listThree.get (1));
		assertEquals ("D", listThree.get (2));
	}
	
	/**
	 * Tests the isCityProtectedAgainstSpellRealm method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsCityProtectedAgainstSpellRealm () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CitySpellEffect effect = new CitySpellEffect ();
		effect.getProtectsAgainstSpellRealm ().add ("MB01");
		when (db.findCitySpellEffect ("SE001", "isCityProtectedAgainstSpellRealm")).thenReturn (effect);
		
		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		final MemoryMaintainedSpell protectionSpell = new MemoryMaintainedSpell ();
		protectionSpell.setCityLocation (new MapCoordinates3DEx (20, 11, 1));
		protectionSpell.setCitySpellEffectID ("SE001");
		protectionSpell.setCastingPlayerID (3);
		spells.add (protectionSpell);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Run method
		assertTrue (utils.isCityProtectedAgainstSpellRealm (new MapCoordinates3DEx (20, 11, 1), "MB01", 2, spells, db));
	}
	
	/**
	 * Tests the isCityProtectedAgainstSpellRealm method when the protection is against a different magic realm
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsCityProtectedAgainstSpellRealm_WrongMagicRealm () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CitySpellEffect effect = new CitySpellEffect ();
		effect.getProtectsAgainstSpellRealm ().add ("MB01");
		when (db.findCitySpellEffect ("SE001", "isCityProtectedAgainstSpellRealm")).thenReturn (effect);
		
		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		final MemoryMaintainedSpell protectionSpell = new MemoryMaintainedSpell ();
		protectionSpell.setCityLocation (new MapCoordinates3DEx (20, 11, 1));
		protectionSpell.setCitySpellEffectID ("SE001");
		protectionSpell.setCastingPlayerID (3);
		spells.add (protectionSpell);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Run method
		assertFalse (utils.isCityProtectedAgainstSpellRealm (new MapCoordinates3DEx (20, 11, 1), "MB02", 2, spells, db));
	}
	
	/**
	 * Tests the isCityProtectedAgainstSpellRealm method when the protection is at a different location
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsCityProtectedAgainstSpellRealm_DifferentLocation () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		final MemoryMaintainedSpell protectionSpell = new MemoryMaintainedSpell ();
		protectionSpell.setCityLocation (new MapCoordinates3DEx (20, 11, 1));
		protectionSpell.setCitySpellEffectID ("SE001");
		protectionSpell.setCastingPlayerID (3);
		spells.add (protectionSpell);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Run method
		assertFalse (utils.isCityProtectedAgainstSpellRealm (new MapCoordinates3DEx (20, 12, 1), "MB01", 2, spells, db));
	}
	
	/**
	 * Tests the isCityProtectedAgainstSpellRealm method when the protection was cast by ourselves, e.g. we can cast Chaos Ward and then still cast Wall of Fire afterwards
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsCityProtectedAgainstSpellRealm_OurOwnProtection () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		final MemoryMaintainedSpell protectionSpell = new MemoryMaintainedSpell ();
		protectionSpell.setCityLocation (new MapCoordinates3DEx (20, 11, 1));
		protectionSpell.setCitySpellEffectID ("SE001");
		protectionSpell.setCastingPlayerID (3);
		spells.add (protectionSpell);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Run method
		assertFalse (utils.isCityProtectedAgainstSpellRealm (new MapCoordinates3DEx (20, 11, 1), "MB01", 3, spells, db));
	}
	
	/**
	 * Tests the isBlockedCastingCombatSpellsOfRealm method when it is blocked
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsBlockedCastingCombatSpellsOfRealm_Yes () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CitySpellEffect effect = new CitySpellEffect ();
		effect.setBlockCastingCombatSpellsOfRealm (true);
		effect.getProtectsAgainstSpellRealm ().add ("MB01");
		when (db.findCitySpellEffect ("CSE01", "isBlockedCastingCombatSpellsOfRealm")).thenReturn (effect);

		// Spell Ward
		final MemoryMaintainedSpell spellWard = new MemoryMaintainedSpell ();
		spellWard.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		spellWard.setCitySpellEffectID ("CSE01");
		spellWard.setCastingPlayerID (2);

		final List<MemoryMaintainedSpell> spells = Arrays.asList (spellWard);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Call method
		assertTrue (utils.isBlockedCastingCombatSpellsOfRealm (spells, 1, new MapCoordinates3DEx (20, 10, 1), "MB01", db));
	}

	/**
	 * Tests the isBlockedCastingCombatSpellsOfRealm method when it blocks a different magic realm
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsBlockedCastingCombatSpellsOfRealm_WrongMagicRealm () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CitySpellEffect effect = new CitySpellEffect ();
		effect.setBlockCastingCombatSpellsOfRealm (true);
		effect.getProtectsAgainstSpellRealm ().add ("MB02");
		when (db.findCitySpellEffect ("CSE01", "isBlockedCastingCombatSpellsOfRealm")).thenReturn (effect);

		// Spell Ward
		final MemoryMaintainedSpell spellWard = new MemoryMaintainedSpell ();
		spellWard.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		spellWard.setCitySpellEffectID ("CSE01");
		spellWard.setCastingPlayerID (2);

		final List<MemoryMaintainedSpell> spells = Arrays.asList (spellWard);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Call method
		assertFalse (utils.isBlockedCastingCombatSpellsOfRealm (spells, 1, new MapCoordinates3DEx (20, 10, 1), "MB01", db));
	}

	/**
	 * Tests the isBlockedCastingCombatSpellsOfRealm method when blockCastingCombatSpellsOfRealm isn't set to true (Consecration blocks overland spells but not combat spells)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsBlockedCastingCombatSpellsOfRealm_DoesntBlockCombatSpells () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CitySpellEffect effect = new CitySpellEffect ();
		effect.setBlockCastingCombatSpellsOfRealm (false);
		effect.getProtectsAgainstSpellRealm ().add ("MB01");
		when (db.findCitySpellEffect ("CSE01", "isBlockedCastingCombatSpellsOfRealm")).thenReturn (effect);

		// Spell Ward
		final MemoryMaintainedSpell spellWard = new MemoryMaintainedSpell ();
		spellWard.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		spellWard.setCitySpellEffectID ("CSE01");
		spellWard.setCastingPlayerID (2);

		final List<MemoryMaintainedSpell> spells = Arrays.asList (spellWard);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Call method
		assertFalse (utils.isBlockedCastingCombatSpellsOfRealm (spells, 1, new MapCoordinates3DEx (20, 10, 1), "MB01", db));
	}

	/**
	 * Tests the listMagicRealmsBlockedAsCombatSpells method when it is blocked
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListMagicRealmsBlockedAsCombatSpells_Yes () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CitySpellEffect effect = new CitySpellEffect ();
		effect.setBlockCastingCombatSpellsOfRealm (true);
		effect.getProtectsAgainstSpellRealm ().add ("MB01");
		when (db.findCitySpellEffect ("CSE01", "listMagicRealmsBlockedAsCombatSpells")).thenReturn (effect);

		// Spell Ward
		final MemoryMaintainedSpell spellWard = new MemoryMaintainedSpell ();
		spellWard.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		spellWard.setCitySpellEffectID ("CSE01");
		spellWard.setCastingPlayerID (2);

		final List<MemoryMaintainedSpell> spells = Arrays.asList (spellWard);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Call method
		final Set<String> set = utils.listMagicRealmsBlockedAsCombatSpells (spells, 1, new MapCoordinates3DEx (20, 10, 1), db);

		// Check results
		assertEquals (1, set.size ());
		assertTrue (set.contains ("MB01"));
	}

	/**
	 * Tests the listMagicRealmsBlockedAsCombatSpells method when blockCastingCombatSpellsOfRealm isn't set to true (Consecration blocks overland spells but not combat spells)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListMagicRealmsBlockedAsCombatSpells_DoesntBlockCombatSpells () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CitySpellEffect effect = new CitySpellEffect ();
		effect.setBlockCastingCombatSpellsOfRealm (false);
		effect.getProtectsAgainstSpellRealm ().add ("MB01");
		when (db.findCitySpellEffect ("CSE01", "listMagicRealmsBlockedAsCombatSpells")).thenReturn (effect);

		// Spell Ward
		final MemoryMaintainedSpell spellWard = new MemoryMaintainedSpell ();
		spellWard.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		spellWard.setCitySpellEffectID ("CSE01");
		spellWard.setCastingPlayerID (2);

		final List<MemoryMaintainedSpell> spells = Arrays.asList (spellWard);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Call method
		final Set<String> set = utils.listMagicRealmsBlockedAsCombatSpells (spells, 1, new MapCoordinates3DEx (20, 10, 1), db);
		
		// Check results
		assertTrue (set.isEmpty ());
	}
}