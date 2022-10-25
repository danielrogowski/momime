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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.common.calculations.UnitCalculations;
import momime.common.database.CitySpellEffect;
import momime.common.database.CombatMapLayerID;
import momime.common.database.CombatTileType;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageResolutionTypeID;
import momime.common.database.DamageType;
import momime.common.database.GenerateTestData;
import momime.common.database.Pick;
import momime.common.database.RangedAttackTypeEx;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.SpellValidMapFeatureTarget;
import momime.common.database.SpellValidTileTypeTarget;
import momime.common.database.TileTypeEx;
import momime.common.database.UnitCombatSideID;
import momime.common.database.UnitSpellEffect;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.FogOfWarStateID;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MapVolumeOfFogOfWarStates;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomCombatTile;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.UnitStatusID;
import momime.common.messages.WizardState;
import momime.common.messages.servertoclient.OverlandCastingInfo;

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
	
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.RAISE_DEAD);
		
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
	
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.RAISE_DEAD);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
	
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.RAISE_DEAD);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (21, 10, 1));
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
	
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.RAISE_DEAD);
		
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_INVALID_MAGIC_REALM_LIFEFORM_TYPE, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
	
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.RAISE_DEAD);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.DEAD);
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
	
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.RAISE_DEAD);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.DEAD);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.RAISING_ENEMY, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on a summoning spell, trying to raise a unit from the dead that isn't dead
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Summoning_NotDead () throws Exception
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
	
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.RAISE_DEAD);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.GENERATED);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_DEAD, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.UNIT_ENCHANTMENTS);
		
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
		utils.setUnitUtils (mock (UnitUtils.class));
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, null, null, null, 1, null, null, xu, false, fow, null, null, db));
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.UNIT_ENCHANTMENTS);
		
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
		utils.setUnitUtils (mock (UnitUtils.class));
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.UNIT_ENCHANTMENTS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.UNIT_ENCHANTMENTS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (21, 10, 1));
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.UNIT_ENCHANTMENTS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.ENCHANTING_OR_HEALING_ENEMY, utils.isUnitValidTargetForSpell (spell, null, null, null, 1, null, null, xu, false, fow, null, null, db));
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
	
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.UNIT_ENCHANTMENTS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.NO_SPELL_EFFECT_IDS_DEFINED, utils.isUnitValidTargetForSpell (spell, null, null, null, 1, null, null, xu, false, fow, null, null, db));
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.UNIT_ENCHANTMENTS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.ALREADY_HAS_ALL_POSSIBLE_SPELL_EFFECTS, utils.isUnitValidTargetForSpell (spell, null, null, null, 1, null, null, xu, false, fow, null, null, db));
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
	
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.UNIT_ENCHANTMENTS);
		
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
		utils.setUnitUtils (mock (UnitUtils.class));
		utils.setSpellUtils (spellUtils);
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_INVALID_MAGIC_REALM_LIFEFORM_TYPE, utils.isUnitValidTargetForSpell (spell, null, null, null, 1, null, null, xu, false, fow, null, null, db));
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.UNIT_ENCHANTMENTS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.DEAD);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_DEAD, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method trying to cast Heroism on a unit that already has 120 exp or more
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_UnitEnchantment_TooMuchExperience () throws Exception
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.UNIT_ENCHANTMENTS);
		
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
		
		// Too much experience
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.isExperienceBonusAndWeAlreadyHaveTooMuch (xu, spell.getUnitSpellEffect (), db)).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setUnitUtils (unitUtils);
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.TOO_MUCH_EXPERIENCE, utils.isUnitValidTargetForSpell (spell, null, null, null, 1, null, null, xu, false, fow, null, null, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method on Lycanthropy, which is a special kind of unit enchantment
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Lycanthropy () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_ENCHANTMENTS);
		spell.getSummonedUnit ().add ("UN001");
	
		final UnitSpellEffect effect = new UnitSpellEffect ();
		effect.setUnitSkillID ("US001");
		spell.getUnitSpellEffect ().add (effect);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.CHANGE_UNIT_ID);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getUnitLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Terrain the unit is standing on
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");
		
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem (); 
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		mem.setMap (terrain);
		terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		// Mock creating the new unit
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final SampleUnitUtils sampleUnitUtils = mock (SampleUnitUtils.class);

		final Set<String> modifiedSkillIDs = new HashSet<String> ();
		final ExpandedUnitDetails replacementUnit = mock (ExpandedUnitDetails.class);
		when (replacementUnit.listModifiedSkillIDs ()).thenReturn (modifiedSkillIDs);
		
		when (sampleUnitUtils.createSampleUnit ("UN001", 1, null, players, mem, db)).thenReturn (replacementUnit);
		
		// Terrain is passable to the new unit
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		when (unitCalculations.calculateDoubleMovementToEnterTileType (replacementUnit, modifiedSkillIDs, "TT01", db)).thenReturn (1);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setUnitUtils (mock (UnitUtils.class));
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		utils.setSampleUnitUtils (sampleUnitUtils);
		utils.setUnitCalculations (unitCalculations);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, null, null, null, 1, null, null, xu, false, mem, null, players, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on Lycanthropy, where the terrain will be impassable to the new unit
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Lycanthropy_Impassable () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.UNIT_ENCHANTMENTS);
		spell.getSummonedUnit ().add ("UN001");
	
		final UnitSpellEffect effect = new UnitSpellEffect ();
		effect.setUnitSkillID ("US001");
		spell.getUnitSpellEffect ().add (effect);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.CHANGE_UNIT_ID);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getUnitLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Terrain the unit is standing on
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");
		
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem (); 
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		mem.setMap (terrain);
		terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		// Mock creating the new unit
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		final SampleUnitUtils sampleUnitUtils = mock (SampleUnitUtils.class);

		final Set<String> modifiedSkillIDs = new HashSet<String> ();
		final ExpandedUnitDetails replacementUnit = mock (ExpandedUnitDetails.class);
		when (replacementUnit.listModifiedSkillIDs ()).thenReturn (modifiedSkillIDs);
		
		when (sampleUnitUtils.createSampleUnit ("UN001", 1, null, players, mem, db)).thenReturn (replacementUnit);
		
		// Terrain is passable to the new unit
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		when (unitCalculations.calculateDoubleMovementToEnterTileType (replacementUnit, modifiedSkillIDs, "TT01", db)).thenReturn (null);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setUnitUtils (mock (UnitUtils.class));
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		utils.setSampleUnitUtils (sampleUnitUtils);
		utils.setUnitCalculations (unitCalculations);

		// Run method
		assertEquals (TargetSpellResult.TERRAIN_IMPASSABLE, utils.isUnitValidTargetForSpell (spell, null, null, null, 1, null, null, xu, false, mem, null, players, db));
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.UNIT_CURSES);
		
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
		utils.setUnitUtils (mock (UnitUtils.class));
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.UNIT_CURSES);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.UNIT_CURSES);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (21, 10, 1));
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.UNIT_CURSES);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.CURSING_OR_ATTACKING_OWN, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
	
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.UNIT_CURSES);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.NO_SPELL_EFFECT_IDS_DEFINED, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.UNIT_CURSES);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.ALREADY_HAS_ALL_POSSIBLE_SPELL_EFFECTS, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.UNIT_CURSES);
		
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
		utils.setUnitUtils (mock (UnitUtils.class));
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_INVALID_MAGIC_REALM_LIFEFORM_TYPE, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.UNIT_CURSES);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.DEAD);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_DEAD, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method on a unit curse in combat on an enemy unit that's immune to the curse
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_UnitCurse_Immune () throws Exception
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.UNIT_CURSES);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Immune to curse
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.isUnitImmuneToSpellEffects (xu, spell.getUnitSpellEffect (), db)).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setUnitUtils (unitUtils);
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.IMMUNE, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (21, 10, 1));
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.CURSING_OR_ATTACKING_OWN, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_INVALID_MAGIC_REALM_LIFEFORM_TYPE, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.DEAD);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_DEAD, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.IMMUNE, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on targeting a normal attack against an enemy unit overland, e.g. Ice Storm
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_TargetingNormalAttack_Overland () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.ATTACK_SPELLS);
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.SINGLE_FIGURE);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
		// Intended target
		final MemoryUnit mu = new MemoryUnit ();
		
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getUnitLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		when (xu.getMemoryUnit ()).thenReturn (mu);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Can see the location
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem (); 
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// Can see the unit
		final UnitVisibilityUtils unitVisibilityUtils = mock (UnitVisibilityUtils.class);
		when (unitVisibilityUtils.canSeeUnitOverland (mu, 1, mem.getMaintainedSpell (), db)).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		utils.setUnitVisibilityUtils (unitVisibilityUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, null, null, null, 1, null, null, xu, true, mem, fow, null, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on targeting a normal attack against an enemy unit overland, e.g. Ice Storm, but we can't see the location
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_TargetingNormalAttack_Overland_CantSeeLocation () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.ATTACK_SPELLS);
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.SINGLE_FIGURE);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getUnitLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Can see the location
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem (); 
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.HAVE_SEEN);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.CANNOT_SEE_TARGET, utils.isUnitValidTargetForSpell (spell, null, null, null, 1, null, null, xu, true, mem, fow, null, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on targeting a normal attack against an enemy unit overland, e.g. Ice Storm, but the unit is invisible
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_TargetingNormalAttack_Overland_Invisible () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.ATTACK_SPELLS);
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.SINGLE_FIGURE);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
		// Intended target
		final MemoryUnit mu = new MemoryUnit ();
		
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getUnitLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getUnitURN ()).thenReturn (50);
		when (xu.getMemoryUnit ()).thenReturn (mu);
		
		// Can see the location
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem (); 
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// Can see the unit
		final UnitVisibilityUtils unitVisibilityUtils = mock (UnitVisibilityUtils.class);
		when (unitVisibilityUtils.canSeeUnitOverland (mu, 1, mem.getMaintainedSpell (), db)).thenReturn (false);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		utils.setUnitVisibilityUtils (unitVisibilityUtils);

		// Run method
		assertEquals (TargetSpellResult.INVISIBLE, utils.isUnitValidTargetForSpell (spell, null, null, null, 1, null, null, xu, true, mem, fow, null, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on resolving a normal attack against an enemy unit overland, e.g. Ice Storm, so its valid even if we can't see the unit
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_ResolvingNormalAttack_Overland () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.ATTACK_SPELLS);
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.SINGLE_FIGURE);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
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
		
		// Can see the location
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem (); 
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.HAVE_SEEN);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, null, null, null, 1, null, null, xu, false, mem, fow, null, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on making a normal attack against our own unit
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_NormalAttack_Overland_OurUnit () throws Exception
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.CURSING_OR_ATTACKING_OWN, utils.isUnitValidTargetForSpell (spell, null, null, null, 1, null, null, xu, false, fow, null, null, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on making a normal attack against an enemy unit that's a lifeform type we're not allowed to target with this spell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_NormalAttack_Overland_WrongLifeformType () throws Exception
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
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
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (false);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_INVALID_MAGIC_REALM_LIFEFORM_TYPE, utils.isUnitValidTargetForSpell (spell, null, null, null, 1, null, null, xu, false, fow, null, null, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on making a normal attack against an enemy unit that's already dead
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_NormalAttack_Overland_Dead () throws Exception
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getStatus ()).thenReturn (UnitStatusID.DEAD);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_DEAD, utils.isUnitValidTargetForSpell (spell, null, null, null, 1, null, null, xu, false, fow, null, null, db));
	}

	/**
	 * Tests the isUnitValidTargetForSpell method on making a normal attack against an enemy unit, but its immune to it.
	 * For example hitting a fire immune unit with fire bolt - not a "normal attack" in the sense of swords or bows, but it
	 * still rolls for damage and not a resistance roll so it in this sense its still considered a "normal attack" and we can be immune to it.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_NormalAttack_Overland_Immune () throws Exception
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.IMMUNE, utils.isUnitValidTargetForSpell (spell, null, null, null, 1, null, null, xu, false, fow, null, null, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on targeting a plane shift spell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_PlaneShift () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_UNIT_SPELLS);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.PLANE_SHIFT);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getUnitLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// It isn't a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (terrainData)).thenReturn (false);
		
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem (); 
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		mem.setMap (terrain);
		terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		utils.setMemoryGridCellUtils (memoryGridCellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, null, null, null, 1, null, null, xu, false, mem, null, null, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on targeting a plane shift spell when planar seal is in effect
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_PlaneShift_PlanarSeal () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_UNIT_SPELLS);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.PLANE_SHIFT);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Planar seal
		final MemoryMaintainedSpell planarSeal = new MemoryMaintainedSpell ();
		planarSeal.setSpellID (CommonDatabaseConstants.SPELL_ID_PLANAR_SEAL);
		mem.getMaintainedSpell ().add (planarSeal);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.PLANAR_SEAL, utils.isUnitValidTargetForSpell (spell, null, null, null, 1, null, null, xu, false, mem, null, null, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on targeting a plane shift spell on units in a tower
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_PlaneShift_Tower () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_UNIT_SPELLS);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.PLANE_SHIFT);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getUnitLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// It isn't a tower
		final MemoryGridCellUtils memoryGridCellUtils = mock (MemoryGridCellUtils.class);
		
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		when (memoryGridCellUtils.isTerrainTowerOfWizardry (terrainData)).thenReturn (true);
		
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem (); 
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		mem.setMap (terrain);
		terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		utils.setMemoryGridCellUtils (memoryGridCellUtils);

		// Run method
		assertEquals (TargetSpellResult.INVALID_MAP_FEATURE, utils.isUnitValidTargetForSpell (spell, null, null, null, 1, null, null, xu, false, mem, null, null, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on targeting a plane shift spell on an enemy unit
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_PlaneShift_EnemyUnit () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_UNIT_SPELLS);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.PLANE_SHIFT);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.ENCHANTING_OR_HEALING_ENEMY, utils.isUnitValidTargetForSpell (spell, null, null, null, 1, null, null, xu, false, fow, null, null, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on targeting a plane shift spell on a dead unit
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_PlaneShift_Dead () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_UNIT_SPELLS);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.PLANE_SHIFT);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getStatus ()).thenReturn (UnitStatusID.DEAD);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_DEAD, utils.isUnitValidTargetForSpell (spell, null, null, null, 1, null, null, xu, false, fow, null, null, db));
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.IMMUNE, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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

		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.TOO_HIGH_RESISTANCE, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, 4, xu, false, fow, null, null, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method making an enemy unit making a resistance roll in combat;
	 * its resistance is high enough to avoid the effect normally, except its being cast by a hero using an item with a -spell save modifier.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_ResistanceRoll_SavingThrowModifier_HeroSpellSaveItem () throws Exception
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
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
		
		// Hero casting the spell
		final ExpandedUnitDetails castingUnit = mock (ExpandedUnitDetails.class); 
		when (castingUnit.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_SAVING_THROW_PENALTY)).thenReturn (true);
		when (castingUnit.getModifiedSkillValue (CommonDatabaseConstants.UNIT_SKILL_ID_SAVING_THROW_PENALTY)).thenReturn (2);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, castingUnit, null, xu, false, fow, null, null, db));
	}
		
	/**
	 * Tests the isUnitValidTargetForSpell method making an enemy unit making a resistance roll overland (for example Black Wind)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_ResistanceRoll_Overland () throws Exception
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, null, null, null, 1, null, null, xu, false, fow, null, null, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method making an enemy unit making a resistance roll in overland.
	 * For example you can't cast Petrify on a unit with Stoning Immunity.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_ResistanceRoll_Overland_Immune () throws Exception
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.IMMUNE, utils.isUnitValidTargetForSpell (spell, null, null, null, 1, null, null, xu, false, fow, null, null, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method making an enemy unit making a resistance roll overland, but its resistance is high enough to avoid the effect
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_ResistanceRoll_Overland_ResistanceTooHigh () throws Exception
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

		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.TOO_HIGH_RESISTANCE, utils.isUnitValidTargetForSpell (spell, null, null, null, 1, null, null, xu, false, fow, null, null, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method making an enemy unit making a resistance roll overland;
	 * its resistance is high enough to avoid the effect normally, except its a particularly nasty spell with a saving throw modifier.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_ResistanceRoll_Overland_SavingThrowModifier () throws Exception
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
		spell.setOverlandBaseDamage (4);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, null, null, null, 1, null, null, xu, false, fow, null, null, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method making an enemy unit making a resistance roll overland;
	 * its resistance is high enough to avoid the effect normally, except we're pumping in extra MP to make the spell more potent.
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_ResistanceRoll_Overland_VariableSavingThrowModifier () throws Exception
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
		spell.setOverlandBaseDamage (1);
		spell.setOverlandMaxDamage (6);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, null, null, null, 1, null, 4, xu, false, fow, null, null, db));
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
		
		final CombatTileType grass = new CombatTileType ();
		grass.setLand (true);
		when (db.findCombatTileType ("CTL01", "isUnitValidTargetForSpell")).thenReturn (grass);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.ATTACK_SPELLS);
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.CHANCE_OF_DEATH);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS_AND_WALLS);
		
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
		
		// Combat terrain
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles combatTerrain = GenerateTestData.createCombatMap (sys);
		final MomCombatTile tile = combatTerrain.getRow ().get (5).getCell ().get (4);
		
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.getCombatTileTypeForLayer (tile, CombatMapLayerID.TERRAIN)).thenReturn ("CTL01");
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		utils.setCombatMapUtils (combatMapUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), combatTerrain, 1, null, null, xu, false, fow, null, null, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method trying to target Cracks Call on a water or air tile
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_CracksCall_Water () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CombatTileType grass = new CombatTileType ();
		grass.setLand (false);
		when (db.findCombatTileType ("CTL01", "isUnitValidTargetForSpell")).thenReturn (grass);

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.ATTACK_SPELLS);
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.CHANCE_OF_DEATH);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS_AND_WALLS);
		
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
		
		// Combat terrain
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles combatTerrain = GenerateTestData.createCombatMap (sys);
		final MomCombatTile tile = combatTerrain.getRow ().get (5).getCell ().get (4);
		
		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.getCombatTileTypeForLayer (tile, CombatMapLayerID.TERRAIN)).thenReturn ("CTL01");
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		utils.setCombatMapUtils (combatMapUtils);

		// Run method
		assertEquals (TargetSpellResult.INVALID_TILE_TYPE, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), combatTerrain, 1, null, null, xu, false, fow, null, null, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on the Warp Wood spell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_WarpWood () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final RangedAttackTypeEx rat = new RangedAttackTypeEx ();
		rat.setWooden (true);
		
		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.ATTACK_SPELLS);
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.ZEROES_AMMO);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK)).thenReturn (true);
		when (xu.getRangedAttackType ()).thenReturn (rat);
		when (xu.getAmmoRemaining ()).thenReturn (4);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on the Warp Wood spell when the target uniit has no ranged attack
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_WarpWood_NoRangedAttack () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final RangedAttackTypeEx rat = new RangedAttackTypeEx ();
		rat.setWooden (true);
		
		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.ATTACK_SPELLS);
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.ZEROES_AMMO);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK)).thenReturn (false);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.NO_RANGED_ATTACK, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on the Warp Wood spell when the target has a magical ranged attack (or boulders, anything non-wooden)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_WarpWood_MagicRangedAttack () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final RangedAttackTypeEx rat = new RangedAttackTypeEx ();
		rat.setWooden (false);
		
		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.ATTACK_SPELLS);
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.ZEROES_AMMO);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK)).thenReturn (true);
		when (xu.getRangedAttackType ()).thenReturn (rat);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.INVALID_RANGED_ATTACK_TYPE, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method on the Warp Wood spell on a unit that's already ran out of ammo
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_WarpWood_NoAmmo () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final RangedAttackTypeEx rat = new RangedAttackTypeEx ();
		rat.setWooden (true);
		
		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.ATTACK_SPELLS);
		spell.setAttackSpellDamageResolutionTypeID (DamageResolutionTypeID.ZEROES_AMMO);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_UNITS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		when (xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK)).thenReturn (true);
		when (xu.getRangedAttackType ()).thenReturn (rat);
		when (xu.getAmmoRemaining ()).thenReturn (0);
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.NO_AMMUNITION, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
	
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.HEALING);
		
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
		normalUnits.setHealEachTurn (true);
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
	
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.HEALING);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
	
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.HEALING);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (21, 10, 1));
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
	
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.HEALING);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.ENCHANTING_OR_HEALING_ENEMY, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
	
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.HEALING);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (1);
		when (xu.getUnitURN ()).thenReturn (50);
		
		final Pick undeadUnits = new Pick ();
		undeadUnits.setPickID ("LTU");
		undeadUnits.setHealEachTurn (false);
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (undeadUnits);
		
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.getTotalDamageTaken (xu.getUnitDamage ())).thenReturn (2);
		when (unitUtils.getHealableDamageTaken (xu.getUnitDamage ())).thenReturn (2);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTU")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setUnitUtils (unitUtils);
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNHEALABLE_LIFEFORM_TYPE, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
	
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.HEALING);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.DEAD);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_DEAD, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
	
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.HEALING);
		
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNDAMAGED, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
	
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.HEALING);
		
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.PERMANENTLY_DAMAGED, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
	
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.RECALL);
		
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
	
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.RECALL);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
	
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.RECALL);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (21, 10, 1));
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
	
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.RECALL);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getOwningPlayerID ()).thenReturn (2);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.ENCHANTING_OR_HEALING_ENEMY, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.RECALL);
	
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_INVALID_MAGIC_REALM_LIFEFORM_TYPE, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
	
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.RECALL);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.DEAD);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_DEAD, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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

		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.DISPEL_UNIT_CITY_COMBAT_SPELLS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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

		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.DISPEL_UNIT_CITY_COMBAT_SPELLS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.NOTHING_TO_DISPEL, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
	
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.DISPEL_UNIT_CITY_COMBAT_SPELLS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
	
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.DISPEL_UNIT_CITY_COMBAT_SPELLS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (21, 10, 1));
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_NOT_IN_EXPECTED_COMBAT, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
	
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.DISPEL_UNIT_CITY_COMBAT_SPELLS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.DEAD);
		when (xu.getUnitURN ()).thenReturn (50);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.UNIT_DEAD, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
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
	
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.DISPEL_UNIT_CITY_COMBAT_SPELLS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.NOTHING_TO_DISPEL, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
	}
	
	/**
	 * Tests the isUnitValidTargetForSpell method dispelling a vortex, where we're dispelling the unit itself rather than a spell cast on it
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsUnitValidTargetForSpell_Dispel_Vortex () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getUnitsThatMoveThroughOtherUnits ()).thenReturn (Arrays.asList ("UN001"));

		// Set up other lists
		final FogOfWarMemory fow = new FogOfWarMemory ();
		
		// Spell
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		spell.setSpellBookSectionID (SpellBookSectionID.DISPEL_SPELLS);

		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.DISPEL_UNIT_CITY_COMBAT_SPELLS);
		
		// Intended target
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (xu.getCombatLocation ()).thenReturn (new MapCoordinates3DEx (20, 10, 1));
		when (xu.getCombatPosition ()).thenReturn (new MapCoordinates2DEx (4, 5));
		when (xu.getCombatSide ()).thenReturn (UnitCombatSideID.ATTACKER);
		when (xu.getStatus ()).thenReturn (UnitStatusID.ALIVE);
		when (xu.getUnitURN ()).thenReturn (50);
		when (xu.getUnitID ()).thenReturn ("UN001");
		
		final Pick normalUnits = new Pick ();
		normalUnits.setPickID ("LTN");
		when (xu.getModifiedUnitMagicRealmLifeformType ()).thenReturn (normalUnits);
		
		// Correct lifeform type?
		final SpellUtils spellUtils = mock (SpellUtils.class);
		when (spellUtils.spellCanTargetMagicRealmLifeformType (spell, "LTN")).thenReturn (true);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setSpellUtils (spellUtils);
		utils.setKindOfSpellUtils (kindOfSpellUtils);

		// Run method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isUnitValidTargetForSpell (spell, null, new MapCoordinates3DEx (20, 10, 1), null, 1, null, null, xu, false, fow, null, null, db));
	}
	
	/**
	 * Tests the isCityValidTargetForSpell method when we can't see the location being targeted
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsCityValidTargetForSpell_CantSeeLocation () throws Exception
	{
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.HAVE_SEEN);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Can't see location
		assertEquals (TargetSpellResult.CANNOT_SEE_TARGET, utils.isCityValidTargetForSpell
			(null, null, 1, new MapCoordinates3DEx (20, 10, 1), terrain, fow, null, null, null));
	}
	
	/**
	 * Tests the isCityValidTargetForSpell method when we can see the location, but there's no city there
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsCityValidTargetForSpell_NoCity () throws Exception
	{
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Can't see location
		assertEquals (TargetSpellResult.NO_CITY_HERE, utils.isCityValidTargetForSpell
			(null, null, 1, new MapCoordinates3DEx (20, 10, 1), terrain, fow, null, null, null));
	}
	
	/**
	 * Tests the isCityValidTargetForSpell method when we try to cast a beneficial enchantment on an enemy city
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsCityValidTargetForSpell_CityEnchantment_EnemyCity () throws Exception
	{
		// Spell being targeted
		final Spell spellDef = new Spell ();
		spellDef.setSpellBookSectionID (SpellBookSectionID.CITY_ENCHANTMENTS);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (2);
		
		terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Can't see location
		assertEquals (TargetSpellResult.ENCHANTING_OR_HEALING_ENEMY, utils.isCityValidTargetForSpell
			(null, spellDef, 1, new MapCoordinates3DEx (20, 10, 1), terrain, fow, null, null, null));
	}

	/**
	 * Tests the isCityValidTargetForSpell method when try to cast a curse on our own city
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsCityValidTargetForSpell_CityCurse_OurCity () throws Exception
	{
		// Spell being targeted
		final Spell spellDef = new Spell ();
		spellDef.setSpellBookSectionID (SpellBookSectionID.CITY_CURSES);

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (1);
		
		terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Can't see location
		assertEquals (TargetSpellResult.CURSING_OR_ATTACKING_OWN, utils.isCityValidTargetForSpell
			(null, spellDef, 1, new MapCoordinates3DEx (20, 10, 1), terrain, fow, null, null, null));
	}
	
	/**
	 * Tests the isCityValidTargetForSpell method when try to use an attack spell (e.g. Earthquake) on our own city
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsCityValidTargetForSpell_Attack_OurCity () throws Exception
	{
		// Spell being targeted
		final Spell spellDef = new Spell ();
		spellDef.setSpellBookSectionID (SpellBookSectionID.ATTACK_SPELLS);

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (1);
		
		terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Can't see location
		assertEquals (TargetSpellResult.CURSING_OR_ATTACKING_OWN, utils.isCityValidTargetForSpell
			(null, spellDef, 1, new MapCoordinates3DEx (20, 10, 1), terrain, fow, null, null, null));
	}
	
	/**
	 * Tests the isCityValidTargetForSpell method when we cast a spell that creates a building (Wall of Stone, Summoning Circle, Move Fortress, Spell of Return)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsCityValidTargetForSpell_CreateBuilding () throws Exception
	{
		// Spell being targeted
		final Spell spellDef = new Spell ();
		spellDef.setSpellBookSectionID (SpellBookSectionID.CITY_ENCHANTMENTS);
		spellDef.setBuildingID ("BL01");

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (1);
		cityData.setCityPopulation (1000);
		
		terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.findBuilding (buildings, new MapCoordinates3DEx (20, 10, 1), "BL01")).thenReturn (null);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setMemoryBuildingUtils (memoryBuildingUtils);
		
		// Can't see location
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isCityValidTargetForSpell
			(null, spellDef, 1, new MapCoordinates3DEx (20, 10, 1), terrain, fow, buildings, null, null));
	}
	
	/**
	 * Tests the isCityValidTargetForSpell method when we cast a spell that creates a building in an outpost
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsCityValidTargetForSpell_CreateBuilding_Outpost () throws Exception
	{
		// Spell being targeted
		final Spell spellDef = new Spell ();
		spellDef.setSpellBookSectionID (SpellBookSectionID.CITY_ENCHANTMENTS);
		spellDef.setBuildingID ("BL01");

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (1);
		cityData.setCityPopulation (999);
		
		terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Can't see location
		assertEquals (TargetSpellResult.CANT_CREATE_BUILDINGS_IN_OUTPOSTS, utils.isCityValidTargetForSpell
			(null, spellDef, 1, new MapCoordinates3DEx (20, 10, 1), terrain, fow, null, null, null));
	}
	
	/**
	 * Tests the isCityValidTargetForSpell method when we cast a spell that creates a building, but the city already has that building
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsCityValidTargetForSpell_CreateBuilding_AlreadyHas () throws Exception
	{
		// Spell being targeted
		final Spell spellDef = new Spell ();
		spellDef.setSpellBookSectionID (SpellBookSectionID.CITY_ENCHANTMENTS);
		spellDef.setBuildingID ("BL01");

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (1);
		cityData.setCityPopulation (1000);
		
		terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Buildings
		final List<MemoryBuilding> buildings = new ArrayList<MemoryBuilding> ();
		final MemoryBuildingUtils memoryBuildingUtils = mock (MemoryBuildingUtils.class);
		when (memoryBuildingUtils.findBuilding (buildings, new MapCoordinates3DEx (20, 10, 1), "BL01")).thenReturn (new MemoryBuilding ());
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setMemoryBuildingUtils (memoryBuildingUtils);
		
		// Can't see location
		assertEquals (TargetSpellResult.CITY_ALREADY_HAS_BUILDING, utils.isCityValidTargetForSpell
			(null, spellDef, 1, new MapCoordinates3DEx (20, 10, 1), terrain, fow, buildings, null, null));
	}
	
	/**
	 * Tests the isCityValidTargetForSpell method when the city has a Spell Ward that blocks this realm of magic
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsCityValidTargetForSpell_SpellWard () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CitySpellEffect effect = new CitySpellEffect ();
		effect.getProtectsAgainstSpellRealm ().add ("MB01");
		when (db.findCitySpellEffect ("CSE01", "isCityProtectedAgainstSpellRealm")).thenReturn (effect);
		
		// Spell being targeted
		final Spell spellDef = new Spell ();
		spellDef.setSpellBookSectionID (SpellBookSectionID.ATTACK_SPELLS);
		spellDef.setSpellRealm ("MB01");

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (2);
		
		terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Spell Ward
		final MemoryMaintainedSpell spellWard = new MemoryMaintainedSpell ();
		spellWard.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		spellWard.setCitySpellEffectID ("CSE01");
		spellWard.setCastingPlayerID (2);
		
		final List<MemoryMaintainedSpell> spells = Arrays.asList (spellWard);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Can't see location
		assertEquals (TargetSpellResult.PROTECTED_AGAINST_SPELL_REALM, utils.isCityValidTargetForSpell
			(spells, spellDef, 1, new MapCoordinates3DEx (20, 10, 1), terrain, fow, null, null, db));
	}
	
	/**
	 * Tests the isCityValidTargetForSpell method when try to use an attack spell (e.g. Earthquake)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsCityValidTargetForSpell_Attack () throws Exception
	{
		// Spell being targeted
		final Spell spellDef = new Spell ();
		spellDef.setSpellBookSectionID (SpellBookSectionID.ATTACK_SPELLS);

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (2);
		
		terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Can't see location
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isCityValidTargetForSpell
			(null, spellDef, 1, new MapCoordinates3DEx (20, 10, 1), terrain, fow, null, null, null));
	}
		
	/**
	 * Tests the isCityValidTargetForSpell method when try to cast a curse on an enemy city, but the curse has no effects defined
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsCityValidTargetForSpell_CityCurse_NoEffectsDefined () throws Exception
	{
		// Spell being targeted
		final Spell spellDef = new Spell ();
		spellDef.setSpellBookSectionID (SpellBookSectionID.CITY_CURSES);

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (2);
		
		terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Can't see location
		assertEquals (TargetSpellResult.NO_SPELL_EFFECT_IDS_DEFINED, utils.isCityValidTargetForSpell
			(null, spellDef, 1, new MapCoordinates3DEx (20, 10, 1), terrain, fow, null, null, null));
	}
	
	/**
	 * Tests the isCityValidTargetForSpell method when try to cast a curse on an enemy city, but the city already has that curse cast on it
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsCityValidTargetForSpell_CityCurse_AlreadyHasEffect () throws Exception
	{
		// Spell being targeted
		final Spell spellDef = new Spell ();
		spellDef.getSpellHasCityEffect ().add ("CSE01");
		spellDef.setSpellBookSectionID (SpellBookSectionID.CITY_CURSES);

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (2);
		
		terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// Already has effect
		final MemoryMaintainedSpell curse = new MemoryMaintainedSpell ();
		curse.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		curse.setCitySpellEffectID ("CSE01");
		curse.setCastingPlayerID (1);
		
		final List<MemoryMaintainedSpell> spells = Arrays.asList (curse);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Can't see location
		assertEquals (TargetSpellResult.ALREADY_HAS_ALL_POSSIBLE_SPELL_EFFECTS, utils.isCityValidTargetForSpell
			(spells, spellDef, 1, new MapCoordinates3DEx (20, 10, 1), terrain, fow, null, null, null));
	}
	
	/**
	 * Tests the isCityValidTargetForSpell method when try to cast a curse on an enemy city and its valid
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsCityValidTargetForSpell_CityCurse () throws Exception
	{
		// Spell being targeted
		final Spell spellDef = new Spell ();
		spellDef.getSpellHasCityEffect ().add ("CSE01");
		spellDef.setSpellBookSectionID (SpellBookSectionID.CITY_CURSES);

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (2);
		
		terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// No existing effect
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Can't see location
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isCityValidTargetForSpell
			(spells, spellDef, 1, new MapCoordinates3DEx (20, 10, 1), terrain, fow, null, null, null));
	}
	
	/**
	 * Tests the isCityValidTargetForSpell method when try to cast Evil Prescence on an enemy city and its valid
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsCityValidTargetForSpell_EvilPrescence () throws Exception
	{
		// Spell being targeted
		final Spell spellDef = new Spell ();
		spellDef.getSpellHasCityEffect ().add (CommonDatabaseConstants.CITY_SPELL_EFFECT_ID_EVIL_PRESENCE);
		spellDef.setSpellBookSectionID (SpellBookSectionID.CITY_CURSES);

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (2);
		
		terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// City owner
		final KnownWizardDetails cityOwner = new KnownWizardDetails ();
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		final List<KnownWizardDetails> wizards = new ArrayList<KnownWizardDetails> ();
		
		when (knownWizardUtils.findKnownWizardDetails (wizards, 2, "isCityValidTargetForSpell")).thenReturn (cityOwner);
		
		// City owner's picks
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		when (playerPickUtils.getQuantityOfPick (cityOwner.getPick (), CommonDatabaseConstants.PICK_ID_DEATH_BOOK)).thenReturn (0);
		
		// No existing effect
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKnownWizardUtils (knownWizardUtils);
		utils.setPlayerPickUtils (playerPickUtils);
		
		// Can't see location
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isCityValidTargetForSpell
			(spells, spellDef, 1, new MapCoordinates3DEx (20, 10, 1), terrain, fow, null, wizards, null));
	}
	
	/**
	 * Tests the isCityValidTargetForSpell method when try to cast Evil Prescence on an enemy city and its valid, but the city owner has death books
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsCityValidTargetForSpell_EvilPrescence_DeathWizard () throws Exception
	{
		// Spell being targeted
		final Spell spellDef = new Spell ();
		spellDef.getSpellHasCityEffect ().add (CommonDatabaseConstants.CITY_SPELL_EFFECT_ID_EVIL_PRESENCE);
		spellDef.setSpellBookSectionID (SpellBookSectionID.CITY_CURSES);

		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells terrain = GenerateTestData.createOverlandMap (sys);
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// City
		final OverlandMapCityData cityData = new OverlandMapCityData ();
		cityData.setCityOwnerID (2);
		
		terrain.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setCityData (cityData);
		
		// City owner
		final KnownWizardDetails cityOwner = new KnownWizardDetails ();
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		final List<KnownWizardDetails> wizards = new ArrayList<KnownWizardDetails> ();
		
		when (knownWizardUtils.findKnownWizardDetails (wizards, 2, "isCityValidTargetForSpell")).thenReturn (cityOwner);
		
		// City owner's picks
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		when (playerPickUtils.getQuantityOfPick (cityOwner.getPick (), CommonDatabaseConstants.PICK_ID_DEATH_BOOK)).thenReturn (1);
		
		// No existing effect
		final List<MemoryMaintainedSpell> spells = new ArrayList<MemoryMaintainedSpell> ();
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKnownWizardUtils (knownWizardUtils);
		utils.setPlayerPickUtils (playerPickUtils);
		
		// Can't see location
		assertEquals (TargetSpellResult.WIZARD_HAS_DEATH_BOOKS, utils.isCityValidTargetForSpell
			(spells, spellDef, 1, new MapCoordinates3DEx (20, 10, 1), terrain, fow, null, wizards, null));
	}
	
	/**
	 * Tests the testIsOverlandLocationValidTargetForSpell method on Earth Lore or Enchant Road, which are valid anywhere
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsOverlandLocationValidTargetForSpell_EarthLore_EnchantRoad () throws Exception
	{
		// Spell being targetted
		final Spell spell = new Spell ();
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.EARTH_LORE);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (map);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		
		// Any location is valid
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, null, null, null));
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.CORRUPTION);
		
		// Visible area
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		// Map
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (map);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		
		// Any location is valid
		assertEquals (TargetSpellResult.CANNOT_SEE_TARGET, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, null, null));
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
		
		final SpellValidTileTypeTarget validTileType = new SpellValidTileTypeTarget ();
		validTileType.setTileTypeID ("TT01");
		spell.getSpellValidTileTypeTarget ().add (validTileType);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.CORRUPTION);
		
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		
		// Any location is valid
		assertEquals (TargetSpellResult.INVALID_TILE_TYPE, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, null, null));
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
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.CORRUPTION);
		
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		
		// Any location is valid
		assertEquals (TargetSpellResult.ALREADY_HAS_ALL_POSSIBLE_SPELL_EFFECTS, utils.isOverlandLocationValidTargetForSpell
			(spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, null, null));
	}

	/**
	 * Tests the testIsOverlandLocationValidTargetForSpell method on wrong kind of tile
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsOverlandLocationValidTargetForSpell_Corrupt_InvalidTileType () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_OVERLAND_SPELLS);

		final SpellValidTileTypeTarget validTileType = new SpellValidTileTypeTarget ();
		validTileType.setTileTypeID ("TT01");
		spell.getSpellValidTileTypeTarget ().add (validTileType);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.CORRUPTION);
		
		// Visible area
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// Map
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT02");
		
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (map);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		
		// Any location is valid
		assertEquals (TargetSpellResult.INVALID_TILE_TYPE, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, null, db));
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
		
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_OVERLAND_SPELLS);
		
		final SpellValidTileTypeTarget validTileType = new SpellValidTileTypeTarget ();
		validTileType.setTileTypeID ("TT01");
		spell.getSpellValidTileTypeTarget ().add (validTileType);
				
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.CORRUPTION);
		
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		
		// Any location is valid
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, null, db));
	}
	
	/**
	 * Tests the testIsOverlandLocationValidTargetForSpell method trying to cast a spell on a city which is specifically warded against that realm of magic
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsOverlandLocationValidTargetForSpell_Corrupt_SpellWard () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CitySpellEffect effect = new CitySpellEffect ();
		effect.getProtectsAgainstSpellRealm ().add ("MB01");
		when (db.findCitySpellEffect ("CSE01", "isCityProtectedAgainstSpellRealm")).thenReturn (effect);
		
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_OVERLAND_SPELLS);
		spell.setSpellRealm ("MB01");
		
		final SpellValidTileTypeTarget validTileType = new SpellValidTileTypeTarget ();
		validTileType.setTileTypeID ("TT01");
		spell.getSpellValidTileTypeTarget ().add (validTileType);
				
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.CORRUPTION);
		
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
		
		// Spell Ward
		final MemoryMaintainedSpell spellWard = new MemoryMaintainedSpell ();
		spellWard.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		spellWard.setCitySpellEffectID ("CSE01");
		spellWard.setCastingPlayerID (1);
		
		mem.getMaintainedSpell ().add (spellWard);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		
		// Any location is valid
		assertEquals (TargetSpellResult.PROTECTED_AGAINST_SPELL_REALM, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, null, db));
	}
	
	/**
	 * Tests the testIsOverlandLocationValidTargetForSpell method on transmute (change map feature) when we have no terrain data
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsOverlandLocationValidTargetForSpell_Transmute_NoTerrainData () throws Exception
	{
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_OVERLAND_SPELLS);
		
		final SpellValidMapFeatureTarget validMapFeature = new SpellValidMapFeatureTarget ();
		validMapFeature.setMapFeatureID ("MF01");
		spell.getSpellValidMapFeatureTarget ().add (validMapFeature);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.CHANGE_MAP_FEATURE);
		
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		
		// Any location is valid
		assertEquals (TargetSpellResult.INVALID_MAP_FEATURE, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, null, null));
	}

	/**
	 * Tests the testIsOverlandLocationValidTargetForSpell method on transmute (change map feature) on the wrong kind of map feature
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsOverlandLocationValidTargetForSpell_Transmute_InvalidMapFeature () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_OVERLAND_SPELLS);

		final SpellValidMapFeatureTarget validMapFeature = new SpellValidMapFeatureTarget ();
		validMapFeature.setMapFeatureID ("MF01");
		spell.getSpellValidMapFeatureTarget ().add (validMapFeature);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.CHANGE_MAP_FEATURE);
		
		// Visible area
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// Map
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setMapFeatureID ("MF02");
		
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (map);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		
		// Any location is valid
		assertEquals (TargetSpellResult.INVALID_MAP_FEATURE, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, null, db));
	}
	
	/**
	 * Tests the testIsOverlandLocationValidTargetForSpell method on transmute (change map feature) on the right kind of map feature
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsOverlandLocationValidTargetForSpell_Transmute () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_OVERLAND_SPELLS);
		
		final SpellValidMapFeatureTarget validMapFeature = new SpellValidMapFeatureTarget ();
		validMapFeature.setMapFeatureID ("MF01");
		spell.getSpellValidMapFeatureTarget ().add (validMapFeature);
				
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.CHANGE_MAP_FEATURE);
		
		// Visible area
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// Map
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setMapFeatureID ("MF01");
		
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (map);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		
		// Any location is valid
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, null, db));
	}
	
	/**
	 * Tests the testIsOverlandLocationValidTargetForSpell method with disenchant area, but there's nothing to dispel
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsOverlandLocationValidTargetForSpell_Dispel_Nothing () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.DISPEL_SPELLS);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.DISPEL_UNIT_CITY_COMBAT_SPELLS);
		
		// Visible area
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);

		// Map
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (map);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		
		// Any location is valid
		assertEquals (TargetSpellResult.NOTHING_TO_DISPEL, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, null, db));
	}
	
	/**
	 * Tests the testIsOverlandLocationValidTargetForSpell method with disenchant area and there's a spell cast on a city
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsOverlandLocationValidTargetForSpell_Dispel_CitySpell () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.DISPEL_SPELLS);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.DISPEL_UNIT_CITY_COMBAT_SPELLS);
		
		// Visible area
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);

		// Map
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (map);
		
		// Spells and units
		final MemoryMaintainedSpell citySpell = new MemoryMaintainedSpell ();
		citySpell.setCityLocation (new MapCoordinates3DEx (20, 10, 1));
		citySpell.setCastingPlayerID (3);
		
		mem.getMaintainedSpell ().add (citySpell);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		
		// Any location is valid
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, null, db));
	}
		
	/**
	 * Tests the testIsOverlandLocationValidTargetForSpell method with disenchant area and there's a spell cast on a unit there
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsOverlandLocationValidTargetForSpell_Dispel_UnitSpell () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.DISPEL_SPELLS);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.DISPEL_UNIT_CITY_COMBAT_SPELLS);
		
		// Visible area
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);

		// Map
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (map);
		
		// Spells and units
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
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		
		// Any location is valid
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, null, db));
	}

	/**
	 * Tests the testIsOverlandLocationValidTargetForSpell method, targeting disenchant area on a warped node to try to turn it back to normal
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsOverlandLocationValidTargetForSpell_Dispel_WarpedNode () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final TileTypeEx node = new TileTypeEx ();
		node.setMagicRealmID ("MB01");
		when (db.findTileType ("TT01", "isOverlandLocationValidTargetForSpell")).thenReturn (node);
		
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.DISPEL_SPELLS);
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.DISPEL_UNIT_CITY_COMBAT_SPELLS);
		
		// Visible area
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);

		// Map
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setWarped (true);
		terrainData.setTileTypeID ("TT01");
		
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (map);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		
		// Any location is valid
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, null, db));
	}
	
	/**
	 * Tests the testIsOverlandLocationValidTargetForSpell method summoning a floating island
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsOverlandLocationValidTargetForSpell_FloatingIsland () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);
		spell.getSummonedUnit ().add ("UN001");
				
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.SUMMONING);
		
		// Visible area
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// Map
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (map);
		
		// Units in the target cell
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findFirstAliveEnemyAtLocation (mem.getUnit (), 20, 10, 1, 2)).thenReturn (null);
		when (unitUtils.countAliveEnemiesAtLocation (mem.getUnit (), 20, 10, 1, 0)).thenReturn (8);
		
		// Sample island
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final SampleUnitUtils sampleUnitUtils = mock (SampleUnitUtils.class);
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (sampleUnitUtils.createSampleUnit (spell.getSummonedUnit ().get (0), 2, null, players, mem, db)).thenReturn (xu);
		
		// Tile is passable
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		when (unitCalculations.calculateDoubleMovementToEnterTileType (xu, xu.listModifiedSkillIDs (), terrainData.getTileTypeID (), db)).thenReturn (1);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		utils.setUnitUtils (unitUtils);
		utils.setSampleUnitUtils (sampleUnitUtils);
		utils.setUnitCalculations (unitCalculations);
		
		// Any location is valid
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, players, db));
	}
	
	/**
	 * Tests the testIsOverlandLocationValidTargetForSpell method summoning a floating island but an enemy unit is here
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsOverlandLocationValidTargetForSpell_FloatingIsland_EnemyHere () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);
		spell.getSummonedUnit ().add ("UN001");
				
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.SUMMONING);
		
		// Visible area
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// Map
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (map);
		
		// Units in the target cell
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findFirstAliveEnemyAtLocation (mem.getUnit (), 20, 10, 1, 2)).thenReturn (new MemoryUnit ());
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		utils.setUnitUtils (unitUtils);
		
		// Any location is valid
		assertEquals (TargetSpellResult.ENEMIES_HERE, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, null, db));
	}
	
	/**
	 * Tests the testIsOverlandLocationValidTargetForSpell method summoning a floating island but we already have 9 units in the target cell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsOverlandLocationValidTargetForSpell_FloatingIsland_CellFull () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);
		spell.getSummonedUnit ().add ("UN001");
				
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.SUMMONING);
		
		// Visible area
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// Map
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (map);
		
		// Units in the target cell
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findFirstAliveEnemyAtLocation (mem.getUnit (), 20, 10, 1, 2)).thenReturn (null);
		when (unitUtils.countAliveEnemiesAtLocation (mem.getUnit (), 20, 10, 1, 0)).thenReturn (9);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		utils.setUnitUtils (unitUtils);
		
		// Any location is valid
		assertEquals (TargetSpellResult.CELL_FULL, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, null, db));
	}
	
	/**
	 * Tests the testIsOverlandLocationValidTargetForSpell method summoning a floating island but the terrain is impassable
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsOverlandLocationValidTargetForSpell_FloatingIsland_Impassable () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SUMMONING);
		spell.getSummonedUnit ().add ("UN001");
				
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.SUMMONING);
		
		// Visible area
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// Map
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (map);
		
		// Units in the target cell
		final UnitUtils unitUtils = mock (UnitUtils.class);
		when (unitUtils.findFirstAliveEnemyAtLocation (mem.getUnit (), 20, 10, 1, 2)).thenReturn (null);
		when (unitUtils.countAliveEnemiesAtLocation (mem.getUnit (), 20, 10, 1, 0)).thenReturn (8);
		
		// Sample island
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final SampleUnitUtils sampleUnitUtils = mock (SampleUnitUtils.class);
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (sampleUnitUtils.createSampleUnit (spell.getSummonedUnit ().get (0), 2, null, players, mem, db)).thenReturn (xu);
		
		// Tile is passable
		final UnitCalculations unitCalculations = mock (UnitCalculations.class);
		when (unitCalculations.calculateDoubleMovementToEnterTileType (xu, xu.listModifiedSkillIDs (), terrainData.getTileTypeID (), db)).thenReturn (null);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		utils.setUnitUtils (unitUtils);
		utils.setSampleUnitUtils (sampleUnitUtils);
		utils.setUnitCalculations (unitCalculations);
		
		// Any location is valid
		assertEquals (TargetSpellResult.TERRAIN_IMPASSABLE, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, players, db));
	}
	
	/**
	 * Tests the testIsOverlandLocationValidTargetForSpell method targeting Warp Node
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsOverlandLocationValidTargetForSpell_WarpNode () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_OVERLAND_SPELLS);
				
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.WARP_NODE);
		
		// Visible area
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// Map
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setNodeOwnerID (1);
		
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (map);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		
		// Any location is valid
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, null, db));
	}
	
	/**
	 * Tests the testIsOverlandLocationValidTargetForSpell method targeting Warp Node on a node that's already warped
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsOverlandLocationValidTargetForSpell_WarpNode_AlreadyWarped () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_OVERLAND_SPELLS);
				
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.WARP_NODE);
		
		// Visible area
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// Map
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setNodeOwnerID (1);
		terrainData.setWarped (true);
		
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (map);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		
		// Any location is valid
		assertEquals (TargetSpellResult.ALREADY_HAS_ALL_POSSIBLE_SPELL_EFFECTS, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, null, db));
	}
	
	/**
	 * Tests the testIsOverlandLocationValidTargetForSpell method targeting Warp Node on a node that isn't owned by anyone
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsOverlandLocationValidTargetForSpell_WarpNode_Unowned () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_OVERLAND_SPELLS);
				
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.WARP_NODE);
		
		// Visible area
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// Map
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (map);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		
		// Any location is valid
		assertEquals (TargetSpellResult.UNOWNED_NODE, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, null, db));
	}
	
	/**
	 * Tests the testIsOverlandLocationValidTargetForSpell method targeting Warp Node on a node that we control
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsOverlandLocationValidTargetForSpell_WarpNode_Ours () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// Spell being targetted
		final Spell spell = new Spell ();
		spell.setSpellBookSectionID (SpellBookSectionID.SPECIAL_OVERLAND_SPELLS);
				
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.WARP_NODE);
		
		// Visible area
		final CoordinateSystem sys = GenerateTestData.createOverlandMapCoordinateSystem ();
		final MapVolumeOfFogOfWarStates fow = GenerateTestData.createFogOfWarArea (sys);
		
		fow.getPlane ().get (1).getRow ().get (10).getCell ().set (20, FogOfWarStateID.CAN_SEE);
		
		// Map
		final MapVolumeOfMemoryGridCells map = GenerateTestData.createOverlandMap (sys);
		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setNodeOwnerID (2);
		
		map.getPlane ().get (1).getRow ().get (10).getCell ().get (20).setTerrainData (terrainData);
		
		final FogOfWarMemory mem = new FogOfWarMemory ();
		mem.setMap (map);
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		
		// Any location is valid
		assertEquals (TargetSpellResult.CURSING_OR_ATTACKING_OWN, utils.isOverlandLocationValidTargetForSpell (spell, 2, new MapCoordinates3DEx (20, 10, 1), mem, fow, null, db));
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
	 * Tests the isWizardValidTargetForSpell method trying to cast something nasty on ourselves
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsWizardValidTargetForSpell_Us () throws Exception
	{
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Call method
		assertEquals (TargetSpellResult.ATTACKING_OWN_WIZARD, utils.isWizardValidTargetForSpell (null, 1, null, 1, null));
	}
	
	/**
	 * Tests the isWizardValidTargetForSpell method trying to cast something on a wizard we haven't met
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsWizardValidTargetForSpell_NotMet () throws Exception
	{
		// Player memory
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge castingPriv = new MomPersistentPlayerPrivateKnowledge ();
		castingPriv.setFogOfWarMemory (mem);
		
		// Wizard being targeted
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), 2)).thenReturn (null);

		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Call method
		assertEquals (TargetSpellResult.WIZARD_NOT_MET, utils.isWizardValidTargetForSpell (null, 1, castingPriv, 2, null));
	}
	
	/**
	 * Tests the isWizardValidTargetForSpell method trying to cast something on a wizard who is banished
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsWizardValidTargetForSpell_Banished () throws Exception
	{
		// Player memory
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge castingPriv = new MomPersistentPlayerPrivateKnowledge ();
		castingPriv.setFogOfWarMemory (mem);
		
		// Wizard being targeted
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		
		final KnownWizardDetails targetWizard = new KnownWizardDetails ();
		targetWizard.setWizardState (WizardState.BANISHED);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), 2)).thenReturn (targetWizard);

		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKnownWizardUtils (knownWizardUtils);
		
		// Call method
		assertEquals (TargetSpellResult.WIZARD_BANISHED_OR_DEFEATED, utils.isWizardValidTargetForSpell (null, 1, castingPriv, 2, null));
	}
	
	/**
	 * Tests the isWizardValidTargetForSpell method trying to cast something on Raiders or Rampaging Monsters
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsWizardValidTargetForSpell_Raiders () throws Exception
	{
		// Player memory
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge castingPriv = new MomPersistentPlayerPrivateKnowledge ();
		castingPriv.setFogOfWarMemory (mem);
		
		// Wizard being targeted
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		
		final KnownWizardDetails targetWizard = new KnownWizardDetails ();
		targetWizard.setWizardState (WizardState.ACTIVE);
		targetWizard.setWizardID ("WZ01");
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), 2)).thenReturn (targetWizard);
		
		// Is it a real wizard?
		final PlayerKnowledgeUtils playerKnowledgeUtils = mock (PlayerKnowledgeUtils.class);
		when (playerKnowledgeUtils.isWizard ("WZ01")).thenReturn (false);

		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKnownWizardUtils (knownWizardUtils);
		utils.setPlayerKnowledgeUtils (playerKnowledgeUtils);
		
		// Call method
		assertEquals (TargetSpellResult.NOT_A_WIZARD, utils.isWizardValidTargetForSpell (null, 1, castingPriv, 2, null));
	}
	
	/**
	 * Tests the isWizardValidTargetForSpell method casting a normal enemy wizard spell at a valid target
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsWizardValidTargetForSpell_Valid () throws Exception
	{
		// Spell
		final Spell spell = new Spell ();
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ENEMY_WIZARD_SPELLS);
		
		// Player memory
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge castingPriv = new MomPersistentPlayerPrivateKnowledge ();
		castingPriv.setFogOfWarMemory (mem);
		
		// Wizard being targeted
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		
		final KnownWizardDetails targetWizard = new KnownWizardDetails ();
		targetWizard.setWizardState (WizardState.ACTIVE);
		targetWizard.setWizardID ("WZ01");
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), 2)).thenReturn (targetWizard);
		
		// Is it a real wizard?
		final PlayerKnowledgeUtils playerKnowledgeUtils = mock (PlayerKnowledgeUtils.class);
		when (playerKnowledgeUtils.isWizard ("WZ01")).thenReturn (true);

		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKnownWizardUtils (knownWizardUtils);
		utils.setPlayerKnowledgeUtils (playerKnowledgeUtils);
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		
		// Call method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isWizardValidTargetForSpell (spell, 1, castingPriv, 2, null));
	}
	
	/**
	 * Tests the isWizardValidTargetForSpell method casting Spell Blast on a wizard who isn't casting anything
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsWizardValidTargetForSpell_SpellBlast_NotCastingAnything () throws Exception
	{
		// Spell
		final Spell spell = new Spell ();
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.SPELL_BLAST);
		
		// Player memory
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge castingPriv = new MomPersistentPlayerPrivateKnowledge ();
		castingPriv.setFogOfWarMemory (mem);
		
		// Wizard being targeted
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		
		final KnownWizardDetails targetWizard = new KnownWizardDetails ();
		targetWizard.setWizardState (WizardState.ACTIVE);
		targetWizard.setWizardID ("WZ01");
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), 2)).thenReturn (targetWizard);
		
		// Is it a real wizard?
		final PlayerKnowledgeUtils playerKnowledgeUtils = mock (PlayerKnowledgeUtils.class);
		when (playerKnowledgeUtils.isWizard ("WZ01")).thenReturn (true);

		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKnownWizardUtils (knownWizardUtils);
		utils.setPlayerKnowledgeUtils (playerKnowledgeUtils);
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		
		// Call method
		assertEquals (TargetSpellResult.NO_SPELL_BEING_CAST, utils.isWizardValidTargetForSpell (spell, 1, castingPriv, 2, null));
	}
	
	/**
	 * Tests the isWizardValidTargetForSpell method casting Spell Blast when we don't have enough MP to blast the target spell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsWizardValidTargetForSpell_SpellBlast_InsufficientMana () throws Exception
	{
		// Spell
		final Spell spell = new Spell ();
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.SPELL_BLAST);
		
		// Player memory
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge castingPriv = new MomPersistentPlayerPrivateKnowledge ();
		castingPriv.setFogOfWarMemory (mem);
		
		// Wizard being targeted
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		
		final KnownWizardDetails targetWizard = new KnownWizardDetails ();
		targetWizard.setWizardState (WizardState.ACTIVE);
		targetWizard.setWizardID ("WZ01");
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), 2)).thenReturn (targetWizard);
		
		// Is it a real wizard?
		final PlayerKnowledgeUtils playerKnowledgeUtils = mock (PlayerKnowledgeUtils.class);
		when (playerKnowledgeUtils.isWizard ("WZ01")).thenReturn (true);
		
		// Spell they are casting
		final OverlandCastingInfo targetCastingInfo = new OverlandCastingInfo ();
		targetCastingInfo.setSpellID ("SP001");
		targetCastingInfo.setManaSpentOnCasting (100);
		
		// How much MP we have
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (resourceValueUtils.findAmountStoredForProductionType (castingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (99);

		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKnownWizardUtils (knownWizardUtils);
		utils.setPlayerKnowledgeUtils (playerKnowledgeUtils);
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		utils.setResourceValueUtils (resourceValueUtils);
		
		// Call method
		assertEquals (TargetSpellResult.INSUFFICIENT_MANA, utils.isWizardValidTargetForSpell (spell, 1, castingPriv, 2, targetCastingInfo));
	}
	
	/**
	 * Tests the isWizardValidTargetForSpell method casting Spell Blast on a valid target
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsWizardValidTargetForSpell_SpellBlast_Valid () throws Exception
	{
		// Spell
		final Spell spell = new Spell ();
		
		// Kind of spell
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.SPELL_BLAST);
		
		// Player memory
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge castingPriv = new MomPersistentPlayerPrivateKnowledge ();
		castingPriv.setFogOfWarMemory (mem);
		
		// Wizard being targeted
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		
		final KnownWizardDetails targetWizard = new KnownWizardDetails ();
		targetWizard.setWizardState (WizardState.ACTIVE);
		targetWizard.setWizardID ("WZ01");
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), 2)).thenReturn (targetWizard);
		
		// Is it a real wizard?
		final PlayerKnowledgeUtils playerKnowledgeUtils = mock (PlayerKnowledgeUtils.class);
		when (playerKnowledgeUtils.isWizard ("WZ01")).thenReturn (true);
		
		// Spell they are casting
		final OverlandCastingInfo targetCastingInfo = new OverlandCastingInfo ();
		targetCastingInfo.setSpellID ("SP001");
		targetCastingInfo.setManaSpentOnCasting (100);
		
		// How much MP we have
		final ResourceValueUtils resourceValueUtils = mock (ResourceValueUtils.class);
		when (resourceValueUtils.findAmountStoredForProductionType (castingPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA)).thenReturn (100);

		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKnownWizardUtils (knownWizardUtils);
		utils.setPlayerKnowledgeUtils (playerKnowledgeUtils);
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		utils.setResourceValueUtils (resourceValueUtils);
		
		// Call method
		assertEquals (TargetSpellResult.VALID_TARGET, utils.isWizardValidTargetForSpell (spell, 1, castingPriv, 2, targetCastingInfo));
	}
	
	/**
	 * Tests the isCombatLocationValidTargetForSpell method casting Earth to Mud at a valid combat tile
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsCombatLocationValidTargetForSpell_EarthToMud_Valid () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CombatTileType combatTileType = new CombatTileType ();
		combatTileType.setLand (true);
		when (db.findCombatTileType ("CTL01", "isCombatLocationValidTargetForSpell")).thenReturn (combatTileType);

		// Spell being targetted
		final Spell spell = new Spell ();
		
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.EARTH_TO_MUD);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles map = GenerateTestData.createCombatMap (sys);

		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.getCombatTileTypeForLayer (map.getRow ().get (5).getCell ().get (10), CombatMapLayerID.TERRAIN)).thenReturn ("CTL01");
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		utils.setCombatMapUtils (combatMapUtils);
		
		// Run method
		assertTrue (utils.isCombatLocationValidTargetForSpell (spell, new MapCoordinates2DEx (10, 5), map, db));
	}

	/**
	 * Tests the isCombatLocationValidTargetForSpell method casting Earth to Mud off the edge of the map
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsCombatLocationValidTargetForSpell_EarthToMud_OffMapEdge () throws Exception
	{
		// Map
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles map = GenerateTestData.createCombatMap (sys);
		map.getRow ().get (1).getCell ().get (2).setOffMapEdge (true);

		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		
		// Call method
		assertFalse (utils.isCombatLocationValidTargetForSpell (null, new MapCoordinates2DEx (2, 1), map, null));
	}

	/**
	 * Tests the isCombatLocationValidTargetForSpell method casting Earth to Mud at a water tile
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsCombatLocationValidTargetForSpell_EarthToMud_Water () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CombatTileType combatTileType = new CombatTileType ();
		combatTileType.setLand (false);
		when (db.findCombatTileType ("CTL01", "isCombatLocationValidTargetForSpell")).thenReturn (combatTileType);

		// Spell being targetted
		final Spell spell = new Spell ();
		
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.EARTH_TO_MUD);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles map = GenerateTestData.createCombatMap (sys);

		final CombatMapUtils combatMapUtils = mock (CombatMapUtils.class);
		when (combatMapUtils.getCombatTileTypeForLayer (map.getRow ().get (5).getCell ().get (10), CombatMapLayerID.TERRAIN)).thenReturn ("CTL01");
		
		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		utils.setCombatMapUtils (combatMapUtils);
		
		// Run method
		assertFalse (utils.isCombatLocationValidTargetForSpell (spell, new MapCoordinates2DEx (10, 5), map, db));
	}

	/**
	 * Tests the isCombatLocationValidTargetForSpell method on Disrupt
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testIsCombatLocationValidTargetForSpell_Disrupt () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);

		// Spell being targetted
		final Spell spell = new Spell ();
		spell.getSpellValidBorderTarget ().add ("CTB01");
		
		final KindOfSpellUtils kindOfSpellUtils = mock (KindOfSpellUtils.class);
		when (kindOfSpellUtils.determineKindOfSpell (spell, null)).thenReturn (KindOfSpell.ATTACK_WALLS);
		
		// Map
		final CoordinateSystem sys = GenerateTestData.createCombatMapCoordinateSystem ();
		final MapAreaOfCombatTiles map = GenerateTestData.createCombatMap (sys);

		// Set up object to test
		final MemoryMaintainedSpellUtilsImpl utils = new MemoryMaintainedSpellUtilsImpl ();
		utils.setKindOfSpellUtils (kindOfSpellUtils);
		
		// Now we need a specific border
		assertFalse (utils.isCombatLocationValidTargetForSpell (spell, new MapCoordinates2DEx (10, 5), map, db));
		
		// Put the wrong kind of border there
		map.getRow ().get (5).getCell ().get (10).getBorderID ().add ("CTB02");
		assertFalse (utils.isCombatLocationValidTargetForSpell (spell, new MapCoordinates2DEx (10, 5), map, db));
		
		// Now the spell can hit either
		spell.getSpellValidBorderTarget ().add ("CTB02");
		assertTrue (utils.isCombatLocationValidTargetForSpell (spell, new MapCoordinates2DEx (10, 5), map, db));
		
		// Border already destroyed
		map.getRow ().get (5).getCell ().get (10).setWrecked (true);
		assertFalse (utils.isCombatLocationValidTargetForSpell (spell, new MapCoordinates2DEx (10, 5), map, db));
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