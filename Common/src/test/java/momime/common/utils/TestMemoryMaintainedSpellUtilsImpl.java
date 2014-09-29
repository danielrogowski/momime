package momime.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import momime.common.database.CommonDatabase;
import momime.common.database.GenerateTestData;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_5.Spell;
import momime.common.database.v0_9_5.SpellBookSectionID;
import momime.common.database.v0_9_5.SpellHasCityEffect;
import momime.common.database.v0_9_5.UnitSpellEffect;
import momime.common.messages.v0_9_5.MapVolumeOfMemoryGridCells;
import momime.common.messages.v0_9_5.MemoryBuilding;
import momime.common.messages.v0_9_5.MemoryMaintainedSpell;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.messages.v0_9_5.OverlandMapCityData;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates3DEx;

/**
 * Tests the MemoryMaintainedSpellUtils class
 */
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
	 * Tests the switchOffMaintainedSpell method looking for an overland enchantment
	 * @throws RecordNotFoundException If we can't find the requested spell
	 */
	@Test
	public final void testSwitchOffMaintainedSpell_OverlandEnchantment () throws RecordNotFoundException
	{
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		for (int n = 1; n <= 5; n++)
		{
			final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
			spell.setSpellID ("SP00" + n);
			spell.setCastingPlayerID (10 + n);

			spells.add (spell);
		}

		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.switchOffMaintainedSpell (spells, 14, "SP004", null, null, null, null);
		assertEquals (4, spells.size ());
		assertEquals ("SP001", spells.get (0).getSpellID ());
		assertEquals ("SP002", spells.get (1).getSpellID ());
		assertEquals ("SP003", spells.get (2).getSpellID ());
		assertEquals ("SP005", spells.get (3).getSpellID ());
	}

	/**
	 * Tests the switchOffMaintainedSpell method looking for a city spell
	 * @throws RecordNotFoundException If we can't find the requested spell
	 */
	@Test
	public final void testSwitchOffMaintainedSpell_CitySpell () throws RecordNotFoundException
	{
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		for (int n = 1; n <= 5; n++)
		{
			final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
			spell.setSpellID ("SP00" + n);
			spell.setCastingPlayerID (10 + n);
			spell.setCitySpellEffectID ("CSE00" + n);
			spell.setCityLocation (new MapCoordinates3DEx (20 + n, 10 + n, n));

			spells.add (spell);
		}

		final MapCoordinates3DEx switchOffLocation = new MapCoordinates3DEx (24, 14, 4);

		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.switchOffMaintainedSpell (spells, 14, "SP004", null, null, switchOffLocation, "CSE004");
		assertEquals (4, spells.size ());
		assertEquals ("SP001", spells.get (0).getSpellID ());
		assertEquals ("SP002", spells.get (1).getSpellID ());
		assertEquals ("SP003", spells.get (2).getSpellID ());
		assertEquals ("SP005", spells.get (3).getSpellID ());
	}

	/**
	 * Tests the switchOffMaintainedSpell method looking for a unit spell
	 * @throws RecordNotFoundException If we can't find the requested spell
	 */
	@Test
	public final void testSwitchOffMaintainedSpell_UnitSpell () throws RecordNotFoundException
	{
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		for (int n = 1; n <= 5; n++)
		{
			final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
			spell.setSpellID ("SP00" + n);
			spell.setCastingPlayerID (10 + n);
			spell.setUnitSkillID ("US00" + n);
			spell.setUnitURN (100 + n);

			spells.add (spell);
		}

		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.switchOffMaintainedSpell (spells, 14, "SP004", 104, "US004", null, null);
		assertEquals (4, spells.size ());
		assertEquals ("SP001", spells.get (0).getSpellID ());
		assertEquals ("SP002", spells.get (1).getSpellID ());
		assertEquals ("SP003", spells.get (2).getSpellID ());
		assertEquals ("SP005", spells.get (3).getSpellID ());
	}

	/**
	 * Tests the switchOffMaintainedSpell method looking for spell that doesn't exist
	 * @throws RecordNotFoundException If we can't find the requested spell
	 */
	@Test(expected=RecordNotFoundException.class)
	public final void testSwitchOffMaintainedSpell_NotExists () throws RecordNotFoundException
	{
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();

		for (int n = 1; n <= 5; n++)
		{
			final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
			spell.setSpellID ("SP00" + n);
			spell.setCastingPlayerID (10 + n);

			spells.add (spell);
		}

		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.switchOffMaintainedSpell (spells, 15, "SP004", null, null, null, null);
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
	 * Tests the listUnitSpellEffectsNotYetCastOnUnit method
	 */
	@Test
	public final void testListUnitSpellEffectsNotYetCastOnUnit ()
	{
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		
		// Spell has no unitSpellEffectIDs defined
		assertNull (utils.listUnitSpellEffectsNotYetCastOnUnit (spells, spell, 1, 10));
		
		// Spell with exactly one unitSpellEffectID, that isn't cast yet
		final UnitSpellEffect effectA = new UnitSpellEffect ();
		effectA.setUnitSkillID ("A");
		spell.getUnitSpellEffect ().add (effectA);
		
		final List<String> listOne = utils.listUnitSpellEffectsNotYetCastOnUnit (spells, spell, 1, 10);
		assertEquals (1, listOne.size ());
		assertEquals ("A", listOne.get (0));

		// Spell with exactly one unitSpellEffectID, that is already cast yet
		final MemoryMaintainedSpell existingEffectA = new MemoryMaintainedSpell ();
		existingEffectA.setSpellID ("SP001");
		existingEffectA.setCastingPlayerID (1);
		existingEffectA.setUnitSkillID ("A");
		existingEffectA.setUnitURN (10);
		spells.add (existingEffectA);
		
		final List<String> listZero = utils.listUnitSpellEffectsNotYetCastOnUnit (spells, spell, 1, 10);
		assertEquals (0, listZero.size ());
		
		// Add three more effects
		for (final String effectID : new String [] {"B", "C", "D"})
		{
			final UnitSpellEffect effectB = new UnitSpellEffect ();
			effectB.setUnitSkillID (effectID);
			spell.getUnitSpellEffect ().add (effectB);
		}
		
		// One with wrong spell ID
		final MemoryMaintainedSpell existingEffectB = new MemoryMaintainedSpell ();
		existingEffectB.setSpellID ("SP002");
		existingEffectB.setCastingPlayerID (1);
		existingEffectB.setUnitSkillID ("B");
		existingEffectB.setUnitURN (10);
		spells.add (existingEffectB);
		
		// One for wrong player
		final MemoryMaintainedSpell existingEffectC = new MemoryMaintainedSpell ();
		existingEffectC.setSpellID ("SP001");
		existingEffectC.setCastingPlayerID (2);
		existingEffectC.setUnitSkillID ("C");
		existingEffectC.setUnitURN (10);
		spells.add (existingEffectC);
		
		// One in wrong unit
		final MemoryMaintainedSpell existingEffectD = new MemoryMaintainedSpell ();
		existingEffectD.setSpellID ("SP001");
		existingEffectD.setCastingPlayerID (1);
		existingEffectD.setUnitSkillID ("D");
		existingEffectD.setUnitURN (11);
		spells.add (existingEffectD);
		
		// All three effect should still be listed
		final List<String> listThree = utils.listUnitSpellEffectsNotYetCastOnUnit (spells, spell, 1, 10);
		assertEquals (3, listThree.size ());
		assertEquals ("B", listThree.get (0));
		assertEquals ("C", listThree.get (1));
		assertEquals ("D", listThree.get (2));
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
		final SpellHasCityEffect effectA = new SpellHasCityEffect ();
		effectA.setCitySpellEffectID ("A");
		spell.getSpellHasCityEffect ().add (effectA);
		
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
		{
			final SpellHasCityEffect effectB = new SpellHasCityEffect ();
			effectB.setCitySpellEffectID (effectID);
			spell.getSpellHasCityEffect ().add (effectB);
		}
		
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
	 * Tests the isUnitValidTargetForSpell method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell () throws Exception
	{
		final CommonDatabase db = GenerateTestData.createDB ();
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitURN (10);
		
		// Set up object to test
		final SpellUtils spellUtils = mock (SpellUtils.class);
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setUnitUtils (unitUtils);
	
		// Enchanting enemy unit
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_ENCHANTMENTS);
		unit.setOwningPlayerID (2);
		assertEquals (TargetSpellResult.ENCHANTING_ENEMY, utils.isUnitValidTargetForSpell (spells, spell, 1, unit, db));
		
		// Cursing own uint
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_CURSES);
		unit.setOwningPlayerID (1);
		assertEquals (TargetSpellResult.CURSING_OWN, utils.isUnitValidTargetForSpell (spells, spell, 1, unit, db));
		
		// Spell has no effects defined
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_ENCHANTMENTS);
		assertEquals (TargetSpellResult.NO_SPELL_EFFECT_IDS_DEFINED, utils.isUnitValidTargetForSpell (spells, spell, 1, unit, db));
		
		// All effects already cast on this unit
		final UnitSpellEffect effectA = new UnitSpellEffect ();
		effectA.setUnitSkillID ("A");
		spell.getUnitSpellEffect ().add (effectA);

		final MemoryMaintainedSpell existingEffectA = new MemoryMaintainedSpell ();
		existingEffectA.setSpellID ("SP001");
		existingEffectA.setCastingPlayerID (1);
		existingEffectA.setUnitSkillID ("A");
		existingEffectA.setUnitURN (10);
		spells.add (existingEffectA);

		assertEquals (TargetSpellResult.ALREADY_HAS_ALL_POSSIBLE_SPELL_EFFECTS, utils.isUnitValidTargetForSpell (spells, spell, 1, unit, db));
		
		// Invalid magic realm/lifeform type
		final UnitSpellEffect effectB = new UnitSpellEffect ();
		effectA.setUnitSkillID ("B");
		spell.getUnitSpellEffect ().add (effectB);

		when (unitUtils.getModifiedUnitMagicRealmLifeformTypeID (unit, unit.getUnitHasSkill (), spells, db)).thenReturn ("X");
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "X")).thenReturn (false);
		assertEquals (TargetSpellResult.UNIT_INVALID_MAGIC_REALM_LIFEFORM_TYPE, utils.isUnitValidTargetForSpell (spells, spell, 1, unit, db));
		
		// Valid target
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "X")).thenReturn (true);
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spells, spell, 1, unit, db));
	}
	
	/**
	 * Tests the isCityValidTargetForSpell method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsCityValidTargetForSpell () throws Exception
	{
		// Mock database entries
		final CommonDatabase db = mock (CommonDatabase.class);

		final Spell enchantment = new Spell ();
		enchantment.setSpellID ("SP001");
		enchantment.setSpellBookSectionID (SpellBookSectionID.CITY_ENCHANTMENTS);

		final Spell curse = new Spell ();
		curse.setSpellID ("SP002");
		curse.setSpellBookSectionID (SpellBookSectionID.CITY_CURSES);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);
		
		// City data but no actual city
		final OverlandMapCityData city1 = new OverlandMapCityData ();
		map.getPlane ().get (0).getRow ().get (20).getCell ().get (21).setCityData (city1);
		
		// City data but zero population
		final OverlandMapCityData city2 = new OverlandMapCityData ();
		city2.setCityPopulation (0);
		map.getPlane ().get (0).getRow ().get (20).getCell ().get (22).setCityData (city2);
		
		// Our city
		final OverlandMapCityData city3 = new OverlandMapCityData ();
		city3.setCityPopulation (10000);
		city3.setCityOwnerID (1);
		map.getPlane ().get (0).getRow ().get (20).getCell ().get (23).setCityData (city3);
		
		// Enemy city
		final OverlandMapCityData city4 = new OverlandMapCityData ();
		city4.setCityPopulation (10000);
		city4.setCityOwnerID (2);
		map.getPlane ().get (0).getRow ().get (20).getCell ().get (24).setCityData (city4);
		
		// Existing spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		// Existing buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setMemoryBuildingUtils (memoryBuildingUtils);
		
		// No city, or can't see location
		assertEquals (TargetSpellResult.NO_CITY_HERE, utils.isCityValidTargetForSpell (spells, enchantment, 1, new MapCoordinates3DEx (20, 20, 0), map, buildings, db));
		assertEquals (TargetSpellResult.NO_CITY_HERE, utils.isCityValidTargetForSpell (spells, enchantment, 1, new MapCoordinates3DEx (21, 20, 0), map, buildings, db));
		assertEquals (TargetSpellResult.NO_CITY_HERE, utils.isCityValidTargetForSpell (spells, enchantment, 1, new MapCoordinates3DEx (22, 20, 0), map, buildings, db));
		
		// Wrong owner
		assertEquals (TargetSpellResult.CURSING_OWN, utils.isCityValidTargetForSpell (spells, curse, 1, new MapCoordinates3DEx (23, 20, 0), map, buildings, db));
		assertEquals (TargetSpellResult.ENCHANTING_ENEMY, utils.isCityValidTargetForSpell (spells, enchantment, 1, new MapCoordinates3DEx (24, 20, 0), map, buildings, db));
		
		// No spell effects defined
		assertEquals (TargetSpellResult.NO_SPELL_EFFECT_IDS_DEFINED, utils.isCityValidTargetForSpell (spells, enchantment, 1, new MapCoordinates3DEx (23, 20, 0), map, buildings, db));
		
		// Spell that creates a building
		enchantment.setBuildingID ("BL01");
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isCityValidTargetForSpell (spells, enchantment, 1, new MapCoordinates3DEx (23, 20, 0), map, buildings, db));
		
		when (memoryBuildingUtils.findBuilding (buildings, new MapCoordinates3DEx (23, 20, 0), "BL01")).thenReturn (true);
		assertEquals (TargetSpellResult.CITY_ALREADY_HAS_BUILDING, utils.isCityValidTargetForSpell (spells, enchantment, 1, new MapCoordinates3DEx (23, 20, 0), map, buildings, db));
		
		// Spell that creates one of two spell effects
		enchantment.setBuildingID (null);
		for (int n = 1; n <= 2; n++)
		{
			final SpellHasCityEffect effect = new SpellHasCityEffect ();
			effect.setCitySpellEffectID ("CSE0" + n);
			enchantment.getSpellHasCityEffect ().add (effect);
		}
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isCityValidTargetForSpell (spells, enchantment, 1, new MapCoordinates3DEx (23, 20, 0), map, buildings, db));
		
		final MemoryMaintainedSpell effect1 = new MemoryMaintainedSpell ();
		effect1.setCityLocation (new MapCoordinates3DEx (23, 20, 0));
		effect1.setSpellID (enchantment.getSpellID ());
		effect1.setCitySpellEffectID ("CSE01");
		effect1.setCastingPlayerID (1);
		spells.add (effect1);
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isCityValidTargetForSpell (spells, enchantment, 1, new MapCoordinates3DEx (23, 20, 0), map, buildings, db));

		final MemoryMaintainedSpell effect2 = new MemoryMaintainedSpell ();
		effect2.setCityLocation (new MapCoordinates3DEx (23, 20, 0));
		effect2.setSpellID (enchantment.getSpellID ());
		effect2.setCitySpellEffectID ("CSE02");
		effect2.setCastingPlayerID (1);
		spells.add (effect2);
		assertEquals (TargetSpellResult.ALREADY_HAS_ALL_POSSIBLE_SPELL_EFFECTS, utils.isCityValidTargetForSpell (spells, enchantment, 1, new MapCoordinates3DEx (23, 20, 0), map, buildings, db));
	}
}