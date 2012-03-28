package momime.common.messages;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;

import org.junit.Test;

/**
 * Tests the MemoryMaintainedSpellUtils class
 */
public final class TestMemoryMaintainedSpellUtils
{
	/** Dummy logger to use during unit tests */
	private final Logger debugLogger = Logger.getLogger ("MoMIMECommonUnitTests");

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

		assertEquals ("SP004", MemoryMaintainedSpellUtils.findMaintainedSpell (spells, null, "SP004", null, null, null, null, debugLogger).getSpellID ());
		assertNull (MemoryMaintainedSpellUtils.findMaintainedSpell (spells, null, "SP006", null, null, null, null, debugLogger));
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

		assertEquals (14, MemoryMaintainedSpellUtils.findMaintainedSpell (spells, 14, null, null, null, null, null, debugLogger).getCastingPlayerID ());
		assertNull (MemoryMaintainedSpellUtils.findMaintainedSpell (spells, 16, null, null, null, null, null, debugLogger));
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

		assertEquals (14, MemoryMaintainedSpellUtils.findMaintainedSpell (spells, 14, "SP001", null, null, null, null, debugLogger).getCastingPlayerID ());
		assertNull (MemoryMaintainedSpellUtils.findMaintainedSpell (spells, 16, "SP001", null, null, null, null, debugLogger));
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

		assertNull (MemoryMaintainedSpellUtils.findMaintainedSpell (spells, null, "SP004", null, null, null, null, debugLogger));
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
			spell.setCityLocation (new OverlandMapCoordinates ());		// 0, 0, 0 is good enough for this test

			spells.add (spell);
		}

		assertNull (MemoryMaintainedSpellUtils.findMaintainedSpell (spells, null, "SP004", null, null, null, null, debugLogger));
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

		assertEquals (44, MemoryMaintainedSpellUtils.findMaintainedSpell (spells, null, null, 44, null, null, null, debugLogger).getUnitURN ().intValue ());
		assertNull (MemoryMaintainedSpellUtils.findMaintainedSpell (spells, null, null, 46, null, null, null, debugLogger));
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

		assertEquals ("SS024", MemoryMaintainedSpellUtils.findMaintainedSpell (spells, null, "SP004", 44, "SS024", null, null, debugLogger).getUnitSkillID ());
		assertNull (MemoryMaintainedSpellUtils.findMaintainedSpell (spells, null, "SP004", 44, "SS025", null, null, debugLogger));
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

		assertEquals ("SS024", MemoryMaintainedSpellUtils.findMaintainedSpell (spells, null, null, 44, "SS024", null, null, debugLogger).getUnitSkillID ());
		assertNull (MemoryMaintainedSpellUtils.findMaintainedSpell (spells, null, null, 44, "SS025", null, null, debugLogger));
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

			final OverlandMapCoordinates cityLocation = new OverlandMapCoordinates ();
			cityLocation.setX (100 + n);
			cityLocation.setY (200 + n);
			cityLocation.setPlane (300 + n);
			spell.setCityLocation (cityLocation);

			spells.add (spell);
		}

		final OverlandMapCoordinates cityLocation = new OverlandMapCoordinates ();
		cityLocation.setX (104);
		cityLocation.setY (204);
		cityLocation.setPlane (304);

		assertEquals ("SP004", MemoryMaintainedSpellUtils.findMaintainedSpell (spells, null, null, null, null, cityLocation, null, debugLogger).getSpellID ());

		cityLocation.setPlane (305);
		assertNull (MemoryMaintainedSpellUtils.findMaintainedSpell (spells, null, null, null, null, cityLocation, null, debugLogger));
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

			final OverlandMapCoordinates cityLocation = new OverlandMapCoordinates ();
			cityLocation.setX (100 + n);
			cityLocation.setY (200 + n);
			cityLocation.setPlane (300 + n);
			spell.setCityLocation (cityLocation);

			spells.add (spell);
		}

		final OverlandMapCoordinates cityLocation = new OverlandMapCoordinates ();
		cityLocation.setX (104);
		cityLocation.setY (204);
		cityLocation.setPlane (304);

		assertEquals ("SP004", MemoryMaintainedSpellUtils.findMaintainedSpell (spells, null, "SP004", null, null, cityLocation, "CSE034", debugLogger).getSpellID ());
		assertNull (MemoryMaintainedSpellUtils.findMaintainedSpell (spells, null, "SP004", null, null, cityLocation, "CSE035", debugLogger));
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

			final OverlandMapCoordinates cityLocation = new OverlandMapCoordinates ();
			cityLocation.setX (100 + n);
			cityLocation.setY (200 + n);
			cityLocation.setPlane (300 + n);
			spell.setCityLocation (cityLocation);

			spells.add (spell);
		}

		final OverlandMapCoordinates cityLocation = new OverlandMapCoordinates ();
		cityLocation.setX (104);
		cityLocation.setY (204);
		cityLocation.setPlane (304);

		assertEquals ("SP004", MemoryMaintainedSpellUtils.findMaintainedSpell (spells, null, null, null, null, cityLocation, "CSE034", debugLogger).getSpellID ());
		assertNull (MemoryMaintainedSpellUtils.findMaintainedSpell (spells, null, null, null, null, cityLocation, "CSE035", debugLogger));
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

		MemoryMaintainedSpellUtils.switchOffMaintainedSpell (spells, 14, "SP004", null, null, null, null, debugLogger);
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
			final OverlandMapCoordinates spellLocation = new OverlandMapCoordinates ();
			spellLocation.setX (20 + n);
			spellLocation.setY (10 + n);
			spellLocation.setPlane (n);

			final MemoryMaintainedSpell spell = new MemoryMaintainedSpell ();
			spell.setSpellID ("SP00" + n);
			spell.setCastingPlayerID (10 + n);
			spell.setCitySpellEffectID ("CSE00" + n);
			spell.setCityLocation (spellLocation);

			spells.add (spell);
		}

		final OverlandMapCoordinates switchOffLocation = new OverlandMapCoordinates ();
		switchOffLocation.setX (24);
		switchOffLocation.setY (14);
		switchOffLocation.setPlane (4);

		MemoryMaintainedSpellUtils.switchOffMaintainedSpell (spells, 14, "SP004", null, null, switchOffLocation, "CSE004", debugLogger);
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

		MemoryMaintainedSpellUtils.switchOffMaintainedSpell (spells, 14, "SP004", 104, "US004", null, null, debugLogger);
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

		MemoryMaintainedSpellUtils.switchOffMaintainedSpell (spells, 15, "SP004", null, null, null, null, debugLogger);
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

		MemoryMaintainedSpellUtils.removeSpellsCastOnUnitStack (spells, unitURNs, debugLogger);

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
}
