package momime.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageResolutionTypeID;
import momime.common.database.DamageType;
import momime.common.database.GenerateTestData;
import momime.common.database.Pick;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.TileTypeEx;
import momime.common.database.UnitCombatSideID;
import momime.common.database.UnitSpellEffect;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.FogOfWarStateID;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MapVolumeOfFogOfWarStates;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;

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
	@Test(expected=RecordNotFoundException.class)
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
		utils.findSpellURN (4, spells, "testFindSpellURN_NotExists");
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
	@Test(expected=RecordNotFoundException.class)
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
		utils.removeSpellURN (4, spells);
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
		
		final List<UnitSpellEffect> listOne = utils.listUnitSpellEffectsNotYetCastOnUnit (spells, spell, 1, 10);
		assertEquals (1, listOne.size ());
		assertEquals ("A", listOne.get (0).getUnitSkillID ());

		// Spell with exactly one unitSpellEffectID, that is already cast yet
		final MemoryMaintainedSpell existingEffectA = new MemoryMaintainedSpell ();
		existingEffectA.setSpellID ("SP001");
		existingEffectA.setCastingPlayerID (1);
		existingEffectA.setUnitSkillID ("A");
		existingEffectA.setUnitURN (10);
		spells.add (existingEffectA);
		
		final List<UnitSpellEffect> listZero = utils.listUnitSpellEffectsNotYetCastOnUnit (spells, spell, 1, 10);
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
		final List<UnitSpellEffect> listThree = utils.listUnitSpellEffectsNotYetCastOnUnit (spells, spell, 1, 10);
		assertEquals (3, listThree.size ());
		assertEquals ("B", listThree.get (0).getUnitSkillID ());
		assertEquals ("C", listThree.get (1).getUnitSkillID ());
		assertEquals ("D", listThree.get (2).getUnitSkillID ());
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
	 * Tests the isUnitValidTargetForSpell method on a summoning spell trying to raise our own unit
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Summoning_Ours () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);
		spell.setResurrectedHealthPercentage (50);
	
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.DEAD);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method on a summoning spell trying to raise a unit that isn't in combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Summoning_NotInCombat () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);
		spell.setResurrectedHealthPercentage (50);
	
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getStatus ()).thenReturn (UnitStatusID.DEAD);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on a summoning spell trying to raise a unit that's in the wrong combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Summoning_WrongCombat () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);
		spell.setResurrectedHealthPercentage (50);
	
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (21, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.DEAD);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method on a summoning spell trying to raise a summoned unit
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Summoning_WrongLifeformType () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);
		spell.setResurrectedHealthPercentage (50);
	
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.DEAD);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick lifeSummons = new Pick ();
		lifeSummons.setPickID ("MB01");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (lifeSummons);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "MB01")).thenReturn (false);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_INVALID_MAGIC_REALM_LIFEFORM_TYPE, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method on a summoning spell trying to raise a dead enemy and steal it, on a spell that allows this
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Summoning_EnemyAllowed () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);
		spell.setResurrectedHealthPercentage (50);
		spell.setResurrectEnemyUnits (true);
	
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.DEAD);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on a summoning spell trying to raise a dead enemy and steal it, on a spell that doesn't allow this
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Summoning_EnemyDisallowed () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);
		spell.setResurrectedHealthPercentage (50);
	
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.DEAD);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.RAISING_ENEMY, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on a unit enchantment overland on our own unit which doesn't yet have it
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_UnitEnchantment_Overland () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_ENCHANTMENTS);
	
		final UnitSpellEffect effect = new UnitSpellEffect ();
		effect.setUnitSkillID ("US001");
		spell.getUnitSpellEffect ().add (effect);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, null, 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method on a unit enchantment in combat on our own unit which doesn't yet have it
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_UnitEnchantment_Combat () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_ENCHANTMENTS);
	
		final UnitSpellEffect effect = new UnitSpellEffect ();
		effect.setUnitSkillID ("US001");
		spell.getUnitSpellEffect ().add (effect);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method on a unit enchantment in combat, but the unit isn't in combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_UnitEnchantment_NotInCombat () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_ENCHANTMENTS);
	
		final UnitSpellEffect effect = new UnitSpellEffect ();
		effect.setUnitSkillID ("US001");
		spell.getUnitSpellEffect ().add (effect);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method on a unit enchantment in combat, but the unit is in the wrong combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_UnitEnchantment_WrongCombat () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_ENCHANTMENTS);
	
		final UnitSpellEffect effect = new UnitSpellEffect ();
		effect.setUnitSkillID ("US001");
		spell.getUnitSpellEffect ().add (effect);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (21, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method on a unit enchantment on an enemy unit
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_UnitEnchantment_Enemy () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_ENCHANTMENTS);
	
		final UnitSpellEffect effect = new UnitSpellEffect ();
		effect.setUnitSkillID ("US001");
		spell.getUnitSpellEffect ().add (effect);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.ENCHANTING_OR_HEALING_ENEMY, utils.isUnitValidTargetForSpell (spell, null, 1, null, null, xu, fow, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on a unit enchantment but the spell has no effects defined
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_UnitEnchantment_NoEffectsDefined () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_ENCHANTMENTS);
	
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.NO_SPELL_EFFECT_IDS_DEFINED, utils.isUnitValidTargetForSpell (spell, null, 1, null, null, xu, fow, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on a unit enchantment overland but we already have all possible effects
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_UnitEnchantment_HaveAllEffects () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MemoryMaintainedSpell existingEffect = new MemoryMaintainedSpell ();
		existingEffect.setUnitURN (50);
		existingEffect.setSpellID ("SP001");
		existingEffect.setUnitSkillID ("US001");
		existingEffect.setCastingPlayerID (1);
		fow.getMaintainedSpell ().add (existingEffect);
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_ENCHANTMENTS);
	
		final UnitSpellEffect effect = new UnitSpellEffect ();
		effect.setUnitSkillID ("US001");
		spell.getUnitSpellEffect ().add (effect);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.ALREADY_HAS_ALL_POSSIBLE_SPELL_EFFECTS, utils.isUnitValidTargetForSpell (spell, null, 1, null, null, xu, fow, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on a unit enchantment overland on the wrong lifeform type
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_UnitEnchantment_WrongLifeformType () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_ENCHANTMENTS);
	
		final UnitSpellEffect effect = new UnitSpellEffect ();
		effect.setUnitSkillID ("US001");
		spell.getUnitSpellEffect ().add (effect);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (false);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_INVALID_MAGIC_REALM_LIFEFORM_TYPE, utils.isUnitValidTargetForSpell (spell, null, 1, null, null, xu, fow, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on a unit enchantment in combat on our unit, but its dead
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_UnitEnchantment_Dead () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_ENCHANTMENTS);
	
		final UnitSpellEffect effect = new UnitSpellEffect ();
		effect.setUnitSkillID ("US001");
		spell.getUnitSpellEffect ().add (effect);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.DEAD);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_DEAD, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on a unit curse in combat on an enemy unit which doesn't yet have it
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_UnitCurse_Combat () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_CURSES);
	
		final UnitSpellEffect effect = new UnitSpellEffect ();
		effect.setUnitSkillID ("US001");
		spell.getUnitSpellEffect ().add (effect);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method on a unit curse in combat on an enemy unit who isn't in combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_UnitCurse_NotInCombat () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_CURSES);
	
		final UnitSpellEffect effect = new UnitSpellEffect ();
		effect.setUnitSkillID ("US001");
		spell.getUnitSpellEffect ().add (effect);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method on a unit curse in combat on an enemy who is in the wrong combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_UnitCurse_WrongCombat () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_CURSES);
	
		final UnitSpellEffect effect = new UnitSpellEffect ();
		effect.setUnitSkillID ("US001");
		spell.getUnitSpellEffect ().add (effect);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (21, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on a unit curse in combat on our own unit
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_UnitCurse_OurUnit () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_CURSES);
	
		final UnitSpellEffect effect = new UnitSpellEffect ();
		effect.setUnitSkillID ("US001");
		spell.getUnitSpellEffect ().add (effect);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.CURSING_OR_ATTACKING_OWN, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on a unit curse in combat on an enemy unit but there's no effects defined on the spell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_UnitCurse_NoEffectsDefined () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_CURSES);
	
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.NO_SPELL_EFFECT_IDS_DEFINED, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method on a unit curse but we already have all possible effects
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_UnitCurse_HaveAllEffects () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MemoryMaintainedSpell existingEffect = new MemoryMaintainedSpell ();
		existingEffect.setUnitURN (50);
		existingEffect.setSpellID ("SP001");
		existingEffect.setUnitSkillID ("US001");
		existingEffect.setCastingPlayerID (1);
		fow.getMaintainedSpell ().add (existingEffect);
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_CURSES);
	
		final UnitSpellEffect effect = new UnitSpellEffect ();
		effect.setUnitSkillID ("US001");
		spell.getUnitSpellEffect ().add (effect);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.ALREADY_HAS_ALL_POSSIBLE_SPELL_EFFECTS, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on a unit curse in combat on an enemy unit which is the wrong lifeform type
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_UnitCurse_InvalidLifeformType () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_CURSES);
	
		final UnitSpellEffect effect = new UnitSpellEffect ();
		effect.setUnitSkillID ("US001");
		spell.getUnitSpellEffect ().add (effect);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (false);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_INVALID_MAGIC_REALM_LIFEFORM_TYPE, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method on a unit curse in combat on an enemy unit, but its dead
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_UnitCurse_Dead () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_CURSES);
	
		final UnitSpellEffect effect = new UnitSpellEffect ();
		effect.setUnitSkillID ("US001");
		spell.getUnitSpellEffect ().add (effect);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.DEAD);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_DEAD, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method on making a normal attack against an enemy unit in combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_NormalAttack_Combat () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.ATTACK_SPELLS);
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.SINGLE_FIGURE);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method on making a normal attack against an enemy unit, but it isn't in combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_NormalAttack_NotInCombat () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.ATTACK_SPELLS);
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.SINGLE_FIGURE);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method on making a normal attack against an enemy unit, but its in the wrong combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_NormalAttack_WrongCombat () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.ATTACK_SPELLS);
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.SINGLE_FIGURE);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (21, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on making a normal attack against our own unit
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_NormalAttack_OurUnit () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.ATTACK_SPELLS);
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.SINGLE_FIGURE);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.CURSING_OR_ATTACKING_OWN, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on making a normal attack against an enemy unit that's a lifeform type we're not allowed to target with this spell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_NormalAttack_WrongLifeformType () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.ATTACK_SPELLS);
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.SINGLE_FIGURE);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (false);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_INVALID_MAGIC_REALM_LIFEFORM_TYPE, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on making a normal attack against an enemy unit that's already dead
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_NormalAttack_Dead () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.ATTACK_SPELLS);
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.SINGLE_FIGURE);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.DEAD);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_DEAD, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method on making a normal attack against an enemy unit, but its immune to it.
	 * For example hitting a fire immune unit with fire bolt - not a "normal attack" in the sense of swords or bows, but it
	 * still rolls for damage and not a resistance roll so it in this sense its still considered a "normal attack" and we can be immune to it.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_NormalAttack_Immune () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final DamageType damageType = new DamageType ();
		when (db.findDamageType ("DT01", "isUnitValidTargetForSpell")).thenReturn (damageType);
		
		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.ATTACK_SPELLS);
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.SINGLE_FIGURE);
		spell.setAttackSpellDamageTypeID ("DT01");
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		when (xu.isUnitImmuneToDamageType (damageType)).thenReturn (true);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.IMMUNE, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method making an enemy unit making a resistance roll in combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_ResistanceRoll_Combat () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.ATTACK_SPELLS);
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.EACH_FIGURE_RESIST_OR_DIE);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		when (xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE)).thenReturn (8);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method making an enemy unit making a resistance roll in combat.
	 * For example you can't cast Petrify on a unit with Stoning Immunity.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_ResistanceRoll_Immune () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		final DamageType damageType = new DamageType ();
		when (db.findDamageType ("DT01", "isUnitValidTargetForSpell")).thenReturn (damageType);
		
		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.ATTACK_SPELLS);
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.EACH_FIGURE_RESIST_OR_DIE);
		spell.setAttackSpellDamageTypeID ("DT01");
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		when (xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE)).thenReturn (8);
		when (xu.isUnitImmuneToDamageType (damageType)).thenReturn (true);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.IMMUNE, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method making an enemy unit making a resistance roll in combat, but its resistance is high enough to avoid the effect
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_ResistanceRoll_ResistanceTooHigh () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.ATTACK_SPELLS);
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.EACH_FIGURE_RESIST_OR_DIE);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		when (xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE)).thenReturn (12);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.TOO_HIGH_RESISTANCE, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method making an enemy unit making a resistance roll in combat;
	 * its resistance is high enough to avoid the effect normally, except its a particularly nasty spell with a saving throw modifier.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_ResistanceRoll_SavingThrowModifier () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.ATTACK_SPELLS);
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.EACH_FIGURE_RESIST_OR_DIE);
		spell.setCombatBaseDamage (4);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		when (xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE)).thenReturn (12);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method making an enemy unit making a resistance roll in combat;
	 * its resistance is high enough to avoid the effect normally, except we're pumping in extra MP to make the spell more potent.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_ResistanceRoll_VariableSavingThrowModifier () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.ATTACK_SPELLS);
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.EACH_FIGURE_RESIST_OR_DIE);
		spell.setCombatBaseDamage (1);
		spell.setCombatMaxDamage (6);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		when (xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE)).thenReturn (12);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, 4, xu, fow, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method making an enemy unit making a chance roll in combat that makes
	 * no difference how high resistance it has, such as Cracks Call.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_CracksCall () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.ATTACK_SPELLS);
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.CHANCE_OF_DEATH);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		when (xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE)).thenReturn (12);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on a healing spell cast on our unit in combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Healing_Combat () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_UNIT_SPELLS);
		spell.setCombatBaseDamage (5);
	
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.getTotalDamageTaken (xu.getUnitDamage ())).thenReturn (2);
		when (unitUtils.getHealableDamageTaken (xu.getUnitDamage ())).thenReturn (2);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setUnitUtils (unitUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method on a healing spell cast on our unit, but they aren't in combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Healing_NotInCombat () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_UNIT_SPELLS);
		spell.setCombatBaseDamage (5);
	
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.getTotalDamageTaken (xu.getUnitDamage ())).thenReturn (2);
		when (unitUtils.getHealableDamageTaken (xu.getUnitDamage ())).thenReturn (2);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setUnitUtils (unitUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method on a healing spell cast on our unit, but they're in the wrong combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Healing_WrongCombat () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_UNIT_SPELLS);
		spell.setCombatBaseDamage (5);
	
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (21, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.getTotalDamageTaken (xu.getUnitDamage ())).thenReturn (2);
		when (unitUtils.getHealableDamageTaken (xu.getUnitDamage ())).thenReturn (2);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setUnitUtils (unitUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method on a healing spell cast on an enemy unit in combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Healing_Enemy () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_UNIT_SPELLS);
		spell.setCombatBaseDamage (5);
	
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.getTotalDamageTaken (xu.getUnitDamage ())).thenReturn (2);
		when (unitUtils.getHealableDamageTaken (xu.getUnitDamage ())).thenReturn (2);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setUnitUtils (unitUtils);

		// Run method
		assertEquals (TargetSpellResult.ENCHANTING_OR_HEALING_ENEMY, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method on a healing spell cast on our unit in combat, but they are the wrong lifeform type
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Healing_WrongLifeformType () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_UNIT_SPELLS);
		spell.setCombatBaseDamage (5);
	
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.getTotalDamageTaken (xu.getUnitDamage ())).thenReturn (2);
		when (unitUtils.getHealableDamageTaken (xu.getUnitDamage ())).thenReturn (2);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setUnitUtils (unitUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on a healing spell cast on our unit in combat but its already dead
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Healing_Dead () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_UNIT_SPELLS);
		spell.setCombatBaseDamage (5);
	
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.DEAD);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.getTotalDamageTaken (xu.getUnitDamage ())).thenReturn (2);
		when (unitUtils.getHealableDamageTaken (xu.getUnitDamage ())).thenReturn (2);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setUnitUtils (unitUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_DEAD, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method on a healing spell cast on our unit in combat, but its taken no damage
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Healing_Undamaged () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_UNIT_SPELLS);
		spell.setCombatBaseDamage (5);
	
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setUnitUtils (unitUtils);

		// Run method
		assertEquals (TargetSpellResult.UNDAMAGED, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method on a healing spell cast on our unit in combat, but it has only taken permanent unhealable damage
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Healing_PermanentDamage () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_UNIT_SPELLS);
		spell.setCombatBaseDamage (5);
	
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.getTotalDamageTaken (xu.getUnitDamage ())).thenReturn (2);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setUnitUtils (unitUtils);

		// Run method
		assertEquals (TargetSpellResult.PERMANENTLY_DAMAGED, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method recalling our unit from combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Recall_Combat () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_UNIT_SPELLS);
	
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method recalling our unit from combat, except that it isn't in a combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Recall_NotInCombat () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_UNIT_SPELLS);
	
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method recalling our unit from combat, except that its in the wrong combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Recall_WrongCombat () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_UNIT_SPELLS);
	
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (21, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method recalling an enemy unit from combat
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Recall_Enemy () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_UNIT_SPELLS);
	
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.ENCHANTING_OR_HEALING_ENEMY, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method recalling our unit from combat, but its the wrong lifeform type (e.g. try to recall normal unit with Recall Hero)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Recall_WrongLifeformType () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_UNIT_SPELLS);
	
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (false);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_INVALID_MAGIC_REALM_LIFEFORM_TYPE, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method recalling our unit from combat, but its dead
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Recall_Dead () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_UNIT_SPELLS);
	
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.DEAD);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_DEAD, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method dispelling a curse on our unit, or an enchantment on their unit.
	 * There's really no difference - simply that the player IDs on the spell and unit are different.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Dispel_OursCursed_EnemyEnchanted () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MemoryMaintainedSpell curse = new MemoryMaintainedSpell ();
		curse.setUnitURN (50);
		curse.setCastingPlayerID (2);
		fow.getMaintainedSpell ().add (curse);
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.DISPEL_SPELLS);
	
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method dispelling an enchantment on our unit, or a curse on an enemy unit
	 * There's really no difference - simply that the player IDs on the spell and unit are the same..
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Dispel_OursEnchanted_EnemyCursed () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MemoryMaintainedSpell curse = new MemoryMaintainedSpell ();
		curse.setUnitURN (50);
		curse.setCastingPlayerID (1);
		fow.getMaintainedSpell ().add (curse);
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.DISPEL_SPELLS);
	
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.NOTHING_TO_DISPEL, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method dispelling a curse on our unit, or an enchantment on their unit, but the unit isn't in a combat.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Dispel_NotInCombat () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MemoryMaintainedSpell curse = new MemoryMaintainedSpell ();
		curse.setUnitURN (50);
		curse.setCastingPlayerID (2);
		fow.getMaintainedSpell ().add (curse);
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.DISPEL_SPELLS);
	
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method dispelling a curse on our unit, or an enchantment on their unit, but the unit is in the wrong combat.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Dispel_WrongCombat () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MemoryMaintainedSpell curse = new MemoryMaintainedSpell ();
		curse.setUnitURN (50);
		curse.setCastingPlayerID (2);
		fow.getMaintainedSpell ().add (curse);
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.DISPEL_SPELLS);
	
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (21, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method dispelling a curse on our unit, or an enchantment on their unit.
	 * There's really no difference - simply that the player IDs on the spell and unit are different.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Dispel_Dead () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		final MemoryMaintainedSpell curse = new MemoryMaintainedSpell ();
		curse.setUnitURN (50);
		curse.setCastingPlayerID (2);
		fow.getMaintainedSpell ().add (curse);
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.DISPEL_SPELLS);
	
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.DEAD);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_DEAD, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method casting dispel, but there's nothing to dispel.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Dispel_NothingToDispel () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.DISPEL_SPELLS);
	
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);

		// Run method
		assertEquals (TargetSpellResult.NOTHING_TO_DISPEL, utils.isUnitValidTargetForSpell (spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final DamageType damageType = new DamageType ();
		when (db.findDamageType ("DT01", "isUnitValidTargetForSpell")).thenReturn (damageType);
		
		final Pick magicRealm = new Pick ();
		
		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setAttackSpellDamageTypeID ("DT01");

		// Intended target
		final UnitUtils unitUtils = mock (UnitUtils.class);

		final MemoryUnit unit = new MemoryUnit ();
		
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getMemoryUnit ()).thenReturn (unit);
		when (xu.getUnitURN ()).thenReturn (10);
		when (xu.getStatus ()).thenReturn (UnitStatusID.DEAD);
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (magicRealm);
		
		when (xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RESISTANCE)).thenReturn (12);
		
		// Set up object to test
		final SpellUtils spellUtils = mock (SpellUtils.class);
		
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setUnitUtils (unitUtils);
		
		// Dead unit
		assertEquals (TargetSpellResult.UNIT_DEAD, utils.isUnitValidTargetForSpell
			(spell, null, 1, null, null, xu, fow, db));
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
	
		// Enchanting enemy unit
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_ENCHANTMENTS);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		assertEquals (TargetSpellResult.ENCHANTING_OR_HEALING_ENEMY, utils.isUnitValidTargetForSpell
			(spell, null, 1, null, null, xu, fow, db));
		
		// Healing enemy unit
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_UNIT_SPELLS);
		assertEquals (TargetSpellResult.ENCHANTING_OR_HEALING_ENEMY, utils.isUnitValidTargetForSpell
			(spell, null, 1, null, null, xu, fow, db));
		
		// Cursing own uint
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_CURSES);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		assertEquals (TargetSpellResult.CURSING_OR_ATTACKING_OWN, utils.isUnitValidTargetForSpell
			(spell, null, 1, null, null, xu, fow, db));
		
		// Spell has no effects defined
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_ENCHANTMENTS);
		assertEquals (TargetSpellResult.NO_SPELL_EFFECT_IDS_DEFINED, utils.isUnitValidTargetForSpell
			(spell, null, 1, null, null, xu, fow, db));
		
		// All effects already cast on this unit
		final UnitSpellEffect effectA = new UnitSpellEffect ();
		effectA.setUnitSkillID ("A");
		spell.getUnitSpellEffect ().add (effectA);

		final MemoryMaintainedSpell existingEffectA = new MemoryMaintainedSpell ();
		existingEffectA.setSpellID ("SP001");
		existingEffectA.setCastingPlayerID (1);
		existingEffectA.setUnitSkillID ("A");
		existingEffectA.setUnitURN (10);
		fow.getMaintainedSpell ().add (existingEffectA);

		assertEquals (TargetSpellResult.ALREADY_HAS_ALL_POSSIBLE_SPELL_EFFECTS, utils.isUnitValidTargetForSpell
			(spell, null, 1, null, null, xu, fow, db));
		
		// Invalid magic realm/lifeform type
		final UnitSpellEffect effectB = new UnitSpellEffect ();
		effectA.setUnitSkillID ("B");
		spell.getUnitSpellEffect ().add (effectB);

		magicRealm.setPickID ("X");
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "X")).thenReturn (false);
		assertEquals (TargetSpellResult.UNIT_INVALID_MAGIC_REALM_LIFEFORM_TYPE, utils.isUnitValidTargetForSpell
			(spell, null, 1, null, null, xu, fow, db));
		
		// Valid target overland
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "X")).thenReturn (true);
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell
			(spell, null, 1, null, null, xu, fow, db));
		
		// Currently the unit has combat location + side, but not heading + position, so its s land unit in a naval combat and can't fight, and can't be targetted
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell
			(spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
		
		// Combat non-attack spell
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (5, 6));
		when (xu.getCombatHeading ()).thenReturn (1);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell
			(spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));

		// Combat attack spell that rolls against something other than resistance
		spell.setSpellBookSectionID (SpellBookSectionID.ATTACK_SPELLS);
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.SINGLE_FIGURE);
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell
			(spell, new MapCoordinates3DEx (20, 10, 1), 2, null, null, xu, fow, db));

		// Immune to the type of damage
		when (xu.isUnitImmuneToDamageType (damageType)).thenReturn (true);
		assertEquals (TargetSpellResult.IMMUNE, utils.isUnitValidTargetForSpell
			(spell, new MapCoordinates3DEx (20, 10, 1), 2, null, null, xu, fow, db));
		when (xu.isUnitImmuneToDamageType (damageType)).thenReturn (false);
		
		// Combat attack spell that rolls against resistance, but its resistance is really high
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.RESIST_OR_TAKE_DAMAGE);
		assertEquals (TargetSpellResult.TOO_HIGH_RESISTANCE, utils.isUnitValidTargetForSpell
			(spell, new MapCoordinates3DEx (20, 10, 1), 2, null, null, xu, fow, db));
		
		// Spell gives -1 modifier, but its still not enough
		spell.setCombatBaseDamage (1);
		assertEquals (TargetSpellResult.TOO_HIGH_RESISTANCE, utils.isUnitValidTargetForSpell
			(spell, new MapCoordinates3DEx (20, 10, 1), 2, null, null, xu, fow, db));
		
		// Spell gives variable modifier, and we're setting it to -2 - still not enough
		spell.setCombatMaxDamage (5);
		assertEquals (TargetSpellResult.TOO_HIGH_RESISTANCE, utils.isUnitValidTargetForSpell
			(spell, new MapCoordinates3DEx (20, 10, 1), 2, null, 2, xu, fow, db));

		// Upping it to a -3 modifier is enough
		spell.setCombatMaxDamage (5);
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell
			(spell, new MapCoordinates3DEx (20, 10, 1), 2, null, 3, xu, fow, db));
		
		// Unit is in a different combat
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell
			(spell, new MapCoordinates3DEx (20, 11, 1), 2, null, 3, xu, fow, db));
		
		// Healing spell
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_UNIT_SPELLS);
		assertEquals (TargetSpellResult.UNDAMAGED, utils.isUnitValidTargetForSpell
			(spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));

		when (unitUtils.getTotalDamageTaken (unit.getUnitDamage ())).thenReturn (5);
		assertEquals (TargetSpellResult.PERMANENTLY_DAMAGED, utils.isUnitValidTargetForSpell
			(spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));

		when (unitUtils.getHealableDamageTaken (unit.getUnitDamage ())).thenReturn (5);
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell
			(spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
		
		// Dispel magic
		spell.setSpellBookSectionID (SpellBookSectionID.DISPEL_SPELLS);
		assertEquals (TargetSpellResult.NOTHING_TO_DISPEL, utils.isUnitValidTargetForSpell
			(spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
		
		final MemoryMaintainedSpell spell1 = new MemoryMaintainedSpell ();
		spell1.setUnitURN (xu.getUnitURN ());
		spell1.setCastingPlayerID (1);
		fow.getMaintainedSpell ().add (spell1);

		final MemoryMaintainedSpell spell2 = new MemoryMaintainedSpell ();
		spell2.setUnitURN (999);
		spell2.setCastingPlayerID (2);
		fow.getMaintainedSpell ().add (spell2);

		assertEquals (TargetSpellResult.NOTHING_TO_DISPEL, utils.isUnitValidTargetForSpell
			(spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));

		final MemoryMaintainedSpell spell3 = new MemoryMaintainedSpell ();
		spell3.setUnitURN (xu.getUnitURN ());
		spell3.setCastingPlayerID (2);
		fow.getMaintainedSpell ().add (spell3);

		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell
			(spell, new MapCoordinates3DEx (20, 10, 1), 1, null, null, xu, fow, db));
	}
	
	/**
	 * Tests the isCityValidTargetForSpell method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsCityValidTargetForSpell () throws Exception
	{
		// Mock database entries
		final Spell enchantment = new Spell ();
		enchantment.setSpellID ("SP001");
		enchantment.setSpellBookSectionID (SpellBookSectionID.CITY_ENCHANTMENTS);

		final Spell curse = new Spell ();
		curse.setSpellID ("SP002");
		curse.setSpellBookSectionID (SpellBookSectionID.CITY_CURSES);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		// Our city
		final OverlandMapCityData city3 = new OverlandMapCityData ();
		city3.setCityPopulation (10000);
		city3.setCityOwnerID (1);
		map.getPlane ().get (0).getRow ().get (20).getCell ().get (23).setCityData (city3);
		fow.getPlane ().get (0).getRow ().get (20).getCell ().set (23, FogOfWarStateID.CAN_SEE);
		
		// Enemy city
		final OverlandMapCityData city4 = new OverlandMapCityData ();
		city4.setCityPopulation (10000);
		city4.setCityOwnerID (2);
		map.getPlane ().get (0).getRow ().get (20).getCell ().get (24).setCityData (city4);
		fow.getPlane ().get (0).getRow ().get (20).getCell ().set (24, FogOfWarStateID.CAN_SEE);
		
		// Existing spells
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		// Existing buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setMemoryBuildingUtils (memoryBuildingUtils);
		
		// Can't see location
		assertEquals (TargetSpellResult.CANNOT_SEE_TARGET, utils.isCityValidTargetForSpell (spells, enchantment, 1, new MapCoordinates3DEx (20, 20, 0), map, fow, buildings));
		
		// No city
		fow.getPlane ().get (0).getRow ().get (20).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		assertEquals (TargetSpellResult.NO_CITY_HERE, utils.isCityValidTargetForSpell (spells, enchantment, 1, new MapCoordinates3DEx (20, 20, 0), map, fow, buildings));
		
		// Wrong owner
		assertEquals (TargetSpellResult.CURSING_OR_ATTACKING_OWN, utils.isCityValidTargetForSpell (spells, curse, 1, new MapCoordinates3DEx (23, 20, 0), map, fow, buildings));
		assertEquals (TargetSpellResult.ENCHANTING_OR_HEALING_ENEMY, utils.isCityValidTargetForSpell (spells, enchantment, 1, new MapCoordinates3DEx (24, 20, 0), map, fow, buildings));
		
		// No spell effects defined
		assertEquals (TargetSpellResult.NO_SPELL_EFFECT_IDS_DEFINED, utils.isCityValidTargetForSpell (spells, enchantment, 1, new MapCoordinates3DEx (23, 20, 0), map, fow, buildings));
		
		// Spell that creates a building
		enchantment.setBuildingID ("BL01");
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isCityValidTargetForSpell (spells, enchantment, 1, new MapCoordinates3DEx (23, 20, 0), map, fow, buildings));
		
		when (memoryBuildingUtils.findBuilding (buildings, new MapCoordinates3DEx (23, 20, 0), "BL01")).thenReturn (new MemoryBuilding ());
		assertEquals (TargetSpellResult.CITY_ALREADY_HAS_BUILDING, utils.isCityValidTargetForSpell (spells, enchantment, 1, new MapCoordinates3DEx (23, 20, 0), map, fow, buildings));
		
		// Spell that creates one of two spell effects
		enchantment.setBuildingID (null);
		for (int n = 1; n <= 2; n++)
			enchantment.getSpellHasCityEffect ().add ("CSE0" + n);
		
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isCityValidTargetForSpell (spells, enchantment, 1, new MapCoordinates3DEx (23, 20, 0), map, fow, buildings));
		
		final MemoryMaintainedSpell effect1 = new MemoryMaintainedSpell ();
		effect1.setCityLocation (new MapCoordinates3DEx (23, 20, 0));
		effect1.setSpellID (enchantment.getSpellID ());
		effect1.setCitySpellEffectID ("CSE01");
		effect1.setCastingPlayerID (1);
		spells.add (effect1);
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isCityValidTargetForSpell (spells, enchantment, 1, new MapCoordinates3DEx (23, 20, 0), map, fow, buildings));

		final MemoryMaintainedSpell effect2 = new MemoryMaintainedSpell ();
		effect2.setCityLocation (new MapCoordinates3DEx (23, 20, 0));
		effect2.setSpellID (enchantment.getSpellID ());
		effect2.setCitySpellEffectID ("CSE02");
		effect2.setCastingPlayerID (1);
		spells.add (effect2);
		assertEquals (TargetSpellResult.ALREADY_HAS_ALL_POSSIBLE_SPELL_EFFECTS, utils.isCityValidTargetForSpell (spells, enchantment, 1, new MapCoordinates3DEx (23, 20, 0), map, fow, buildings));
	}
	
	/**
	 * Tests the testIsOverlandLocationValidTargetForSpell method on Earth Lore or Enchant Road, which have a spell radius defined
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsOverlandLocationValidTargetForSpell_EarthLore_EnchantRoad () throws Exception
	{
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.setSpellRadius (10);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Any location is valid
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isOverlandLocationValidTargetForSpell (spell, 2, null, null, null, null));
	}
	
	/**
	 * Tests the testIsOverlandLocationValidTargetForSpell method on a location we can't see
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsOverlandLocationValidTargetForSpell_CantSeeLocation () throws Exception
	{
		// Spell being targetted
		final Spell spell = new Spell ();
		
		// Visible area
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Any location is valid
		assertEquals (TargetSpellResult.CANNOT_SEE_TARGET, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), null, fow, null));
	}
	
	/**
	 * Tests the testIsOverlandLocationValidTargetForSpell method on a location with no terrain data (which is odd, since we can see it)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsOverlandLocationValidTargetForSpell_Corrupt_NoTerrainData () throws Exception
	{
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_OVERLAND_SPELLS);
		
		// Visible area
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// Map
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (map);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Any location is valid
		assertEquals (TargetSpellResult.MUST_TARGET_LAND, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, null));
	}

	/**
	 * Tests the testIsOverlandLocationValidTargetForSpell method on a location that's already corrupted
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsOverlandLocationValidTargetForSpell_Corrupt_AlreadyCorrupted () throws Exception
	{
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_OVERLAND_SPELLS);
		
		// Visible area
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// Map
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");
		terrainData.setCorrupted (1);
		
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (map);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Any location is valid
		assertEquals (TargetSpellResult.ALREADY_HAS_ALL_POSSIBLE_SPELL_EFFECTS, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, null));
	}

	/**
	 * Tests the testIsOverlandLocationValidTargetForSpell method on a sea tile
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsOverlandLocationValidTargetForSpell_Corrupt_Sea () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final TileTypeEx ocean = new TileTypeEx ();
		when (db.findTileType ("TT01", "isOverlandLocationValidTargetForSpell")).thenReturn (ocean);
		
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_OVERLAND_SPELLS);
		
		// Visible area
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// Map
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");
		
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (map);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Any location is valid
		assertEquals (TargetSpellResult.MUST_TARGET_LAND, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, db));
	}

	/**
	 * Tests the testIsOverlandLocationValidTargetForSpell method on a node
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsOverlandLocationValidTargetForSpell_Corrupt_Node () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final TileTypeEx node = new TileTypeEx ();
		node.setMagicRealmID ("MB01");
		node.setLand (true);
		when (db.findTileType ("TT01", "isOverlandLocationValidTargetForSpell")).thenReturn (node);
		
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_OVERLAND_SPELLS);
		
		// Visible area
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// Map
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");
		
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (map);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Any location is valid
		assertEquals (TargetSpellResult.INVALID_TILE_TYPE, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, db));
	}
	
	/**
	 * Tests the testIsOverlandLocationValidTargetForSpell method on a valid land tile
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsOverlandLocationValidTargetForSpell_Corrupt_Land () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final TileTypeEx land = new TileTypeEx ();
		land.setLand (true);
		when (db.findTileType ("TT01", "isOverlandLocationValidTargetForSpell")).thenReturn (land);
		
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_OVERLAND_SPELLS);
		
		// Visible area
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// Map
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");
		
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (map);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Any location is valid
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, db));
	}
	
	/**
	 * Tests the testIsOverlandLocationValidTargetForSpell method with disenchant area, but there's nothing to dispel
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsOverlandLocationValidTargetForSpell_Dispel_Nothing () throws Exception
	{
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.DISPEL_SPELLS);
		
		// Visible area
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);

		// Spells and units
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Any location is valid
		assertEquals (TargetSpellResult.NOTHING_TO_DISPEL, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, null));
	}
	
	/**
	 * Tests the testIsOverlandLocationValidTargetForSpell method with disenchant area and there's a spell cast on a city
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsOverlandLocationValidTargetForSpell_Dispel_CitySpell () throws Exception
	{
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.DISPEL_SPELLS);
		
		// Visible area
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);

		// Spells and units
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MemoryMaintainedSpell citySpell = new MemoryMaintainedSpell ();
		citySpell.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		citySpell.setCastingPlayerID (3);
		
		mem.getMaintainedSpell ().add (citySpell);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Any location is valid
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, null));
	}
		
	/**
	 * Tests the testIsOverlandLocationValidTargetForSpell method with disenchant area and there's a spell cast on a unit there
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsOverlandLocationValidTargetForSpell_Dispel_UnitSpell () throws Exception
	{
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.DISPEL_SPELLS);
		
		// Visible area
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);

		// Spells and units
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MemoryUnit unit = new MemoryUnit ();
		unit.setUnitURN (8);
		unit.setUnitLocation (new MapCoordinates3DEx (20, 10, 1));
		
		final MemoryMaintainedSpell unitSpell = new MemoryMaintainedSpell ();
		unitSpell.setUnitURN (8);
		unitSpell.setCastingPlayerID (3);

		mem.getUnit ().add (unit);
		mem.getMaintainedSpell ().add (unitSpell);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Any location is valid
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, null));
	}
	
	/**
	 * Tests the isSpellValidTargetForSpell method on a valid enemy overland enchantment
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsSpellValidTargetForSpell_Valid () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Spell targetSpellDef = new Spell ();
		targetSpellDef.setSpellBookSectionID (SpellBookSectionID.OVERLAND_ENCHANTMENTS);
		when (db.findSpell ("SP001", "isSpellValidTargetForSpell")).thenReturn (targetSpellDef);
		
		// Overland enchantment
		final MemoryMaintainedSpell targetSpell = new MemoryMaintainedSpell ();
		targetSpell.setSpellID ("SP001");
		targetSpell.setCastingPlayerID (2);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Call method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isSpellValidTargetForSpell (1, targetSpell, db));
	}
		
	/**
	 * Tests the isSpellValidTargetForSpell method on enemy spell that isn't an overland enchantment
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsSpellValidTargetForSpell_WrongSection () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Spell targetSpellDef = new Spell ();
		targetSpellDef.setSpellBookSectionID (SpellBookSectionID.SUMMONING);
		when (db.findSpell ("SP001", "isSpellValidTargetForSpell")).thenReturn (targetSpellDef);
		
		// Overland enchantment
		final MemoryMaintainedSpell targetSpell = new MemoryMaintainedSpell ();
		targetSpell.setSpellID ("SP001");
		targetSpell.setCastingPlayerID (2);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Call method
		assertEquals (TargetSpellResult.OVERLAND_ENCHANTMENTS_ONLY, utils.isSpellValidTargetForSpell (1, targetSpell, db));
	}
	
	/**
	 * Tests the isSpellValidTargetForSpell method on our own spell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsSpellValidTargetForSpell_Ours () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final Spell targetSpellDef = new Spell ();
		targetSpellDef.setSpellBookSectionID (SpellBookSectionID.OVERLAND_ENCHANTMENTS);
		when (db.findSpell ("SP001", "isSpellValidTargetForSpell")).thenReturn (targetSpellDef);
		
		// Overland enchantment
		final MemoryMaintainedSpell targetSpell = new MemoryMaintainedSpell ();
		targetSpell.setSpellID ("SP001");
		targetSpell.setCastingPlayerID (1);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Call method
		assertEquals (TargetSpellResult.CURSING_OR_ATTACKING_OWN, utils.isSpellValidTargetForSpell (1, targetSpell, db));
	}
	
	/**
	 * Tests the isCombatLocationValidTargetForSpell method
	 */
	@Test
	public final void testIsCombatLocationValidTargetForSpell ()
	{
		// Spell being targetted
		final Spell spell = new Spell ();
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles map = GenerateTestData.createCombatMap (sys);

		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Spells not targetted at borders can hit anywhere
		assertTrue (utils.isCombatLocationValidTargetForSpell (spell, new MapCoordinates2DEx (10, 5), map));
		
		// Unless its off the map edge
		map.getRow ().get (1).getCell ().get (2).setOffMapEdge (true);
		assertFalse (utils.isCombatLocationValidTargetForSpell (spell, new MapCoordinates2DEx (2, 1), map));
		
		// Now we need a specific border
		spell.getSpellValidBorderTarget ().add ("CTB01");
		assertFalse (utils.isCombatLocationValidTargetForSpell (spell, new MapCoordinates2DEx (10, 5), map));
		
		// Put the wrong kind of border there
		map.getRow ().get (5).getCell ().get (10).getBorderID ().add ("CTB02");
		assertFalse (utils.isCombatLocationValidTargetForSpell (spell, new MapCoordinates2DEx (10, 5), map));
		
		// Now the spell can hit either
		spell.getSpellValidBorderTarget ().add ("CTB02");
		assertTrue (utils.isCombatLocationValidTargetForSpell (spell, new MapCoordinates2DEx (10, 5), map));
		
		// Border already destroyed
		map.getRow ().get (5).getCell ().get (10).setWrecked (true);
		assertFalse (utils.isCombatLocationValidTargetForSpell (spell, new MapCoordinates2DEx (10, 5), map));
	}
}