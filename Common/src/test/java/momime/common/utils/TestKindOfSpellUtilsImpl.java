package momime.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import momime.common.database.AttackSpellTargetID;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.SpellValidMapFeatureTarget;
import momime.common.database.SpellValidTileTypeTarget;

/**
 * Tests the KindOfSpellUtilsImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestKindOfSpellUtilsImpl
{
	// ----- Spell book section = Summoning -----
	
	/**
	 * Tests the determineKindOfSpell method on raise dead/animate dead spells
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_RaiseDead () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		spellDef.setResurrectedHealthPercentage (50);
		
		// Run method
		assertEquals (KindOfSpell.RAISE_DEAD, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.SUMMONING));
	}

	/**
	 * Tests the determineKindOfSpell method on enchant item/create artifact spells
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_CreateArtifact () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		spellDef.setHeroItemBonusMaximumCraftingCost (200);
		
		// Run method
		assertEquals (KindOfSpell.CREATE_ARTIFACT, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.SUMMONING));
	}

	/**
	 * Tests the determineKindOfSpell method on normal summoning spell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_Summoning () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		
		// Run method
		assertEquals (KindOfSpell.SUMMONING, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.SUMMONING));
	}
	
	// ----- Spell book section = Overland Enchantments  -----

	/**
	 * Tests the determineKindOfSpell method on an overland enchantment spell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_OverlandEnchantment () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		
		// Run method
		assertEquals (KindOfSpell.OVERLAND_ENCHANTMENTS, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.OVERLAND_ENCHANTMENTS));
	}
	
	// ----- Spell book section = City Enchantments  -----

	/**
	 * Tests the determineKindOfSpell method on a city enchantment spell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_CityEnchantment () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		
		// Run method
		assertEquals (KindOfSpell.CITY_ENCHANTMENTS, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.CITY_ENCHANTMENTS));
	}
	
	// ----- Spell book section = Unit Enchantments  -----

	/**
	 * Tests the determineKindOfSpell method on a unit enchantment that acts like a summoning spell, by killing off the existing unit and summoning a new one (Lycanthropy)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_ChangeUnitID () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		spellDef.getSummonedUnit ().add ("UN001");
		
		// Run method
		assertEquals (KindOfSpell.CHANGE_UNIT_ID, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.UNIT_ENCHANTMENTS));
	}

	/**
	 * Tests the determineKindOfSpell method on a normal unit enchantment spell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_UnitEnchantment () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		
		// Run method
		assertEquals (KindOfSpell.UNIT_ENCHANTMENTS, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.UNIT_ENCHANTMENTS));
	}
	
	// ----- Spell book section = Combat Enchantments  -----

	/**
	 * Tests the determineKindOfSpell method on a combat enchantment spell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_CombatEnchantment () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		
		// Run method
		assertEquals (KindOfSpell.COMBAT_ENCHANTMENTS, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.COMBAT_ENCHANTMENTS));
	}
	
	// ----- Spell book section = City Curses  -----

	/**
	 * Tests the determineKindOfSpell method on a city curse spell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_CityCurse () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		
		// Run method
		assertEquals (KindOfSpell.CITY_CURSES, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.CITY_CURSES));
	}
	
	// ----- Spell book section = Unit Curses  -----

	/**
	 * Tests the determineKindOfSpell method on a unit curse spell
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_UnitCurse () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		
		// Run method
		assertEquals (KindOfSpell.UNIT_CURSES, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.UNIT_CURSES));
	}
	
	// ----- Spell book section = Attack Spells -----

	/**
	 * Tests the determineKindOfSpell method on attack spells that are directed at single units, and also hit the walls the unit is standing next to; or can be targeted at walls only (Cracks Call)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_AttackUnitsAndWalls () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		spellDef.getSpellValidBorderTarget ().add ("CTB01");
		
		// Run method
		assertEquals (KindOfSpell.ATTACK_UNITS_AND_WALLS, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.ATTACK_SPELLS));
	}

	/**
	 * Tests the determineKindOfSpell method on attack spells that are aimed at cities only, and hits all units and all buildings inside (Earthquake, Call the Void)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_AttackUnitsAndBuildings () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		spellDef.setAttackSpellOverlandTarget (AttackSpellTargetID.ALL_UNITS_AND_BUILDINGS);
		
		// Run method
		assertEquals (KindOfSpell.ATTACK_UNITS_AND_BUILDINGS, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.ATTACK_SPELLS));
	}

	/**
	 * Tests the determineKindOfSpell method on a normal attack spell that can only be aimed at units
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_AttackUnits () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		
		// Run method
		assertEquals (KindOfSpell.ATTACK_UNITS, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.ATTACK_SPELLS));
	}
	
	// ----- Spell book section = Special Unit Spells -----

	/**
	 * Tests the determineKindOfSpell method on Plane Shift (this is a very special case and hard coded by its spell ID)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_PlaneShift () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		spellDef.setSpellID (CommonDatabaseConstants.SPELL_ID_PLANE_SHIFT);
		
		// Run method
		assertEquals (KindOfSpell.PLANE_SHIFT, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.SPECIAL_UNIT_SPELLS));
	}

	/**
	 * Tests the determineKindOfSpell method on healing spells that can be cast in combat (Healing, Mass Healing)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_CombatHealing () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		spellDef.setSpellID ("SP001");
		spellDef.setCombatBaseDamage (5);
		
		// Run method
		assertEquals (KindOfSpell.HEALING, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.SPECIAL_UNIT_SPELLS));
	}

	/**
	 * Tests the determineKindOfSpell method on healing spells that can be cast overland (Nature's Cures)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_OverlandHealing () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		spellDef.setSpellID ("SP001");
		spellDef.setOverlandCastingCost (75);
		spellDef.setAttackSpellOverlandTarget (AttackSpellTargetID.ALL_UNITS);
		
		// Run method
		assertEquals (KindOfSpell.HEALING, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.SPECIAL_UNIT_SPELLS));
	}


	/**
	 * Tests the determineKindOfSpell method on a recall spell (Word of Recall, Recall Hero)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_Recall () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		spellDef.setSpellID ("SP001");
		
		// Run method
		assertEquals (KindOfSpell.RECALL, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.SPECIAL_UNIT_SPELLS));
	}
	
	// ----- Spell book section = Special Overland Spells -----

	/**
	 * Tests the determineKindOfSpell method on Enchant Road
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_EnchantRoad () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		spellDef.setTileTypeID ("TT99");		// What tile the road changes into
		spellDef.setSpellRadius (2);
		
		// Run method
		assertEquals (KindOfSpell.ENCHANT_ROAD, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.SPECIAL_OVERLAND_SPELLS));
	}

	/**
	 * Tests the determineKindOfSpell method on Earth Lore
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_EarthLore () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		spellDef.setSpellRadius (2);
		
		// Run method
		assertEquals (KindOfSpell.EARTH_LORE, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.SPECIAL_OVERLAND_SPELLS));
	}

	/**
	 * Tests the determineKindOfSpell method on a spell that changes the overland tile type (Change Terrain, Raise Volcano)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_ChangeTileType () throws Exception
	{
		// Spell definition
		final SpellValidTileTypeTarget target = new SpellValidTileTypeTarget ();
		target.setChangeToTileTypeID ("TT01");
		
		final Spell spellDef = new Spell ();
		spellDef.getSpellValidTileTypeTarget ().add (target);
		
		// Run method
		assertEquals (KindOfSpell.CHANGE_TILE_TYPE, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.SPECIAL_OVERLAND_SPELLS));
	}

	/**
	 * Tests the determineKindOfSpell method on a spell that changes the overland map feature (Transmute)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_ChangeMapFeature () throws Exception
	{
		// Spell definition
		final SpellValidMapFeatureTarget target = new SpellValidMapFeatureTarget ();
		target.setChangeToMapFeatureID ("MF01");
		
		final Spell spellDef = new Spell ();
		spellDef.getSpellValidMapFeatureTarget ().add (target);
		
		// Run method
		assertEquals (KindOfSpell.CHANGE_MAP_FEATURE, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.SPECIAL_OVERLAND_SPELLS));
	}

	/**
	 * Tests the determineKindOfSpell method on Warp Node
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_WarpNode () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		
		for (int n = 0; n < 3; n++)
			spellDef.getSpellValidTileTypeTarget ().add (new SpellValidTileTypeTarget ());
		
		// Run method
		assertEquals (KindOfSpell.WARP_NODE, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.SPECIAL_OVERLAND_SPELLS));
	}

	/**
	 * Tests the determineKindOfSpell method on Corruption
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_Corruption () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		
		for (int n = 0; n < 4; n++)		// Warp Node and Corruption are distinguished only by their number of valid target tile types, which is a bit of a cheat
			spellDef.getSpellValidTileTypeTarget ().add (new SpellValidTileTypeTarget ());
		
		// Run method
		assertEquals (KindOfSpell.CORRUPTION, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.SPECIAL_OVERLAND_SPELLS));
	}
	
	// ----- Spell book section = Special Combat Spells -----

	/**
	 * Tests the determineKindOfSpell method on spells that can attack walls only (Disrupt Wall)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_AttackWalls () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		spellDef.getSpellValidBorderTarget ().add ("CTB01");
		
		// Run method
		assertEquals (KindOfSpell.ATTACK_WALLS, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.SPECIAL_COMBAT_SPELLS));
	}

	/**
	 * Tests the determineKindOfSpell method on Earth to Mud
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_EarthToMud () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		
		// Run method
		assertEquals (KindOfSpell.EARTH_TO_MUD, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.SPECIAL_COMBAT_SPELLS));
	}

	// ----- Spell book section = Dispel Spells -----

	/**
	 * Tests the determineKindOfSpell method on Spell Binding
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_SpellBinding () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		
		// Run method
		assertEquals (KindOfSpell.SPELL_BINDING, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.DISPEL_SPELLS));
	}

	/**
	 * Tests the determineKindOfSpell method on spells that dispel overland enchantments (Disjunction, Disjunction True)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_Disjunction () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		spellDef.setOverlandMaxDamage (1000);
		
		// Run method
		assertEquals (KindOfSpell.DISPEL_OVERLAND_ENCHANTMENTS, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.DISPEL_SPELLS));
	}


	/**
	 * Tests the determineKindOfSpell method on spells that dispel spells cast on units and cities (Dispel Magic, Dispel True, Disenchant Area, Disenchant True)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_DispelUnitCityAndCombatSpells () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		spellDef.setOverlandMaxDamage (100);
		spellDef.setAttackSpellCombatTarget (AttackSpellTargetID.ALL_UNITS);		// ALL_UNITS = Disenchant Area, SINGLE_UNIT = Dispel Magic
		
		// Run method
		assertEquals (KindOfSpell.DISPEL_UNIT_CITY_COMBAT_SPELLS, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.DISPEL_SPELLS));
	}

	// ----- Spell book section = Special Spells -----

	/**
	 * Tests the determineKindOfSpell method on unique spells that are just coded by their Spell ID (Great Unsummoning, Death Wish, Spell of Mastery)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_SpecialSpells () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		
		// Run method
		assertEquals (KindOfSpell.SPECIAL_SPELLS, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.SPECIAL_SPELLS));
	}

	// ----- Spell book section = Enemy Wizard Spells -----

	/**
	 * Tests the determineKindOfSpell method on Spell Blast (this is a very special case and hard coded by its spell ID)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_SpellBlast () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		spellDef.setSpellID (CommonDatabaseConstants.SPELL_ID_SPELL_BLAST);
		
		// Run method
		assertEquals (KindOfSpell.SPELL_BLAST, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.ENEMY_WIZARD_SPELLS));
	}

	/**
	 * Tests the determineKindOfSpell method on other enemy wizard spells (Drain Power, Subversion, Cruel Unminding)
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testDetermineKindOfSpell_EnemyWizardSpells () throws Exception
	{
		// Spell definition
		final Spell spellDef = new Spell ();
		spellDef.setSpellID ("SP001");
		
		// Run method
		assertEquals (KindOfSpell.ENEMY_WIZARD_SPELLS, new KindOfSpellUtilsImpl ().determineKindOfSpell (spellDef, SpellBookSectionID.ENEMY_WIZARD_SPELLS));
	}
}