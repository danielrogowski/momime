package momime.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.database.CitySpellEffect;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;

/**
 * Tests the MemoryCombatAreaEffectUtils class
 */
@ExtendWith(MockitoExtension.class)
public final class TestMemoryCombatAreaEffectUtilsImpl
{
	/**
	 * Tests the findCombatAreaEffect method with a player specific, but a null map location (i.e. an overland enchantment spell)
	 */
	@Test
	public final void testFindCombatAreaEffect_PlayerButNullLocation ()
	{
		final List<MemoryCombatAreaEffect> CAEs = new ArrayList<MemoryCombatAreaEffect> ();

		for (int n = 1; n <= 3; n++)
		{
			final MemoryCombatAreaEffect newCAE = new MemoryCombatAreaEffect ();
			newCAE.setCombatAreaEffectURN (n);
			newCAE.setCastingPlayerID (n);
			newCAE.setCombatAreaEffectID ("CAE0" + n);
			CAEs.add (newCAE);
		}

		final MemoryCombatAreaEffectUtilsImpl utils = new MemoryCombatAreaEffectUtilsImpl ();
		assertEquals (2, utils.findCombatAreaEffect (CAEs, null, "CAE02", 2).getCombatAreaEffectURN ());
	}

	/**
	 * Tests the findCombatAreaEffect method with a specified player and map location (i.e. a localized combat spell, like prayer)
	 */
	@Test
	public final void testFindCombatAreaEffect_WithPlayerAndLocation ()
	{
		final List<MemoryCombatAreaEffect> CAEs = new ArrayList<MemoryCombatAreaEffect> ();

		for (int n = 1; n <= 3; n++)
		{
			final MemoryCombatAreaEffect newCAE = new MemoryCombatAreaEffect ();
			newCAE.setCombatAreaEffectURN (n);
			newCAE.setCastingPlayerID (n);
			newCAE.setCombatAreaEffectID ("CAE0" + n);

			newCAE.setMapLocation (new MapCoordinates3DEx (10 + n, 20 + n, 30 + n));
			CAEs.add (newCAE);
		}

		final MapCoordinates3DEx desiredLocation = new MapCoordinates3DEx (12, 22, 32);
		final MemoryCombatAreaEffectUtilsImpl utils = new MemoryCombatAreaEffectUtilsImpl ();
		assertEquals (2, utils.findCombatAreaEffect (CAEs, desiredLocation, "CAE02", 2).getCombatAreaEffectURN ());
	}

	/**
	 * Tests the findCombatAreaEffect method with no player and no map location (don't think this actually applies to anything in game)
	 */
	@Test
	public final void testFindCombatAreaEffect_NullPlayerAndLocation ()
	{
		final List<MemoryCombatAreaEffect> CAEs = new ArrayList<MemoryCombatAreaEffect> ();

		for (int n = 1; n <= 3; n++)
		{
			final MemoryCombatAreaEffect newCAE = new MemoryCombatAreaEffect ();
			newCAE.setCombatAreaEffectURN (n);
			newCAE.setCombatAreaEffectID ("CAE0" + n);
			CAEs.add (newCAE);
		}

		final MemoryCombatAreaEffectUtilsImpl utils = new MemoryCombatAreaEffectUtilsImpl ();
		assertEquals (2, utils.findCombatAreaEffect (CAEs, null, "CAE02", null).getCombatAreaEffectURN ());
	}

	/**
	 * Tests the findCombatAreaEffect method with a valid map location
	 */
	@Test
	public final void testFindCombatAreaEffect_LocationButNullPlayer ()
	{
		final List<MemoryCombatAreaEffect> CAEs = new ArrayList<MemoryCombatAreaEffect> ();

		for (int n = 1; n <= 3; n++)
		{
			final MemoryCombatAreaEffect newCAE = new MemoryCombatAreaEffect ();
			newCAE.setCombatAreaEffectURN (n);
			newCAE.setCombatAreaEffectID ("CAE0" + n);
			newCAE.setMapLocation (new MapCoordinates3DEx (10 + n, 20 + n, 30 + n));
			CAEs.add (newCAE);
		}

		final MapCoordinates3DEx desiredLocation = new MapCoordinates3DEx (12, 22, 32);
		final MemoryCombatAreaEffectUtilsImpl utils = new MemoryCombatAreaEffectUtilsImpl ();
		assertEquals (2, utils.findCombatAreaEffect (CAEs, desiredLocation, "CAE02", null).getCombatAreaEffectURN ());
	}

	/**
	 * Tests the findCombatAreaEffectURN method on a combatAreaEffect that does exist
	 * @throws RecordNotFoundException If combatAreaEffect with requested URN is not found
	 */
	@Test
	public final void testFindCombatAreaEffectURN_Exists () throws RecordNotFoundException
	{
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		for (int n = 1; n <= 3; n++)
		{
			final MemoryCombatAreaEffect combatAreaEffect = new MemoryCombatAreaEffect ();
			combatAreaEffect.setCombatAreaEffectURN (n);
			combatAreaEffects.add (combatAreaEffect);
		}

		final MemoryCombatAreaEffectUtilsImpl utils = new MemoryCombatAreaEffectUtilsImpl ();
		assertEquals (2, utils.findCombatAreaEffectURN (2, combatAreaEffects).getCombatAreaEffectURN ());
		assertEquals (2, utils.findCombatAreaEffectURN (2, combatAreaEffects, "testFindCombatAreaEffectURN_Exists").getCombatAreaEffectURN ());
	}

	/**
	 * Tests the findCombatAreaEffectURN method on a combatAreaEffect that doesn't exist
	 * @throws RecordNotFoundException If combatAreaEffect with requested URN is not found
	 */
	@Test
	public final void testFindCombatAreaEffectURN_NotExists () throws RecordNotFoundException
	{
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		for (int n = 1; n <= 3; n++)
		{
			final MemoryCombatAreaEffect combatAreaEffect = new MemoryCombatAreaEffect ();
			combatAreaEffect.setCombatAreaEffectURN (n);
			combatAreaEffects.add (combatAreaEffect);
		}

		final MemoryCombatAreaEffectUtilsImpl utils = new MemoryCombatAreaEffectUtilsImpl ();
		assertNull (utils.findCombatAreaEffectURN (4, combatAreaEffects));
				
		assertThrows (RecordNotFoundException.class, () ->
		{
			utils.findCombatAreaEffectURN (4, combatAreaEffects, "testFindCombatAreaEffectURN_NotExists");
		});
	}

	/**
	 * Tests the removeCombatAreaEffectURN method on a combatAreaEffect that does exist
	 * @throws RecordNotFoundException If combatAreaEffect with requested URN is not found
	 */
	@Test
	public final void testRemoveCombatAreaEffectURN_Exists () throws RecordNotFoundException
	{
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		for (int n = 1; n <= 3; n++)
		{
			final MemoryCombatAreaEffect combatAreaEffect = new MemoryCombatAreaEffect ();
			combatAreaEffect.setCombatAreaEffectURN (n);
			combatAreaEffects.add (combatAreaEffect);
		}

		final MemoryCombatAreaEffectUtilsImpl utils = new MemoryCombatAreaEffectUtilsImpl ();
		utils.removeCombatAreaEffectURN (2, combatAreaEffects);
		assertEquals (2, combatAreaEffects.size ());
		assertEquals (1, combatAreaEffects.get (0).getCombatAreaEffectURN ());
		assertEquals (3, combatAreaEffects.get (1).getCombatAreaEffectURN ());
	}

	/**
	 * Tests the removeCombatAreaEffectURN method on a combatAreaEffect that doesn't exist
	 * @throws RecordNotFoundException If combatAreaEffect with requested URN is not found
	 */
	@Test
	public final void testRemoveCombatAreaEffectURN_NotExists () throws RecordNotFoundException
	{
		final List<MemoryCombatAreaEffect> combatAreaEffects = new ArrayList<MemoryCombatAreaEffect> ();
		for (int n = 1; n <= 3; n++)
		{
			final MemoryCombatAreaEffect combatAreaEffect = new MemoryCombatAreaEffect ();
			combatAreaEffect.setCombatAreaEffectURN (n);
			combatAreaEffects.add (combatAreaEffect);
		}

		final MemoryCombatAreaEffectUtilsImpl utils = new MemoryCombatAreaEffectUtilsImpl ();
		
		assertThrows (RecordNotFoundException.class, () ->
		{
			utils.removeCombatAreaEffectURN (4, combatAreaEffects);
		});
	}

	/**
	 * Tests the listCombatEffectsNotYetCastAtLocation method
	 */
	@Test
	public final void testListCombatEffectsNotYetCastAtLocation ()
	{
		final MemoryCombatAreaEffectUtilsImpl utils = new MemoryCombatAreaEffectUtilsImpl ();
		final List<MemoryCombatAreaEffect> CAEs = new ArrayList<MemoryCombatAreaEffect> ();
		
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (20, 10, 1);
		
		// Spell has no citySpellEffectIDs defined
		assertNull (utils.listCombatEffectsNotYetCastAtLocation (CAEs, spell, 1, combatLocation));
		
		// Spell with exactly one citySpellEffectID, that isn't cast yet
		spell.getSpellHasCombatEffect ().add ("A");
		
		final List<String> listOne = utils.listCombatEffectsNotYetCastAtLocation (CAEs, spell, 1, combatLocation);
		assertEquals (1, listOne.size ());
		assertEquals ("A", listOne.get (0));

		// Spell with exactly one citySpellEffectID, that is already cast yet
		final MemoryCombatAreaEffect existingEffectA = new MemoryCombatAreaEffect ();
		existingEffectA.setCastingPlayerID (1);
		existingEffectA.setCombatAreaEffectID ("A");
		existingEffectA.setMapLocation (new MapCoordinates3DEx (20, 10, 1));
		CAEs.add (existingEffectA);
		
		final List<String> listZero = utils.listCombatEffectsNotYetCastAtLocation (CAEs, spell, 1, combatLocation);
		assertEquals (0, listZero.size ());
		
		// Add three more effects
		for (final String effectID : new String [] {"B", "C", "D"})
			spell.getSpellHasCombatEffect ().add (effectID);
		
		// One matches
		final MemoryCombatAreaEffect existingEffectB = new MemoryCombatAreaEffect ();
		existingEffectB.setCastingPlayerID (1);
		existingEffectB.setCombatAreaEffectID ("B");
		existingEffectB.setMapLocation (new MapCoordinates3DEx (20, 10, 1));
		CAEs.add (existingEffectB);
		
		// One for wrong player
		final MemoryCombatAreaEffect existingEffectC = new MemoryCombatAreaEffect ();
		existingEffectC.setCastingPlayerID (2);
		existingEffectC.setCombatAreaEffectID ("C");
		existingEffectC.setMapLocation (new MapCoordinates3DEx (20, 10, 1));
		CAEs.add (existingEffectC);
		
		// One in wrong location
		final MemoryCombatAreaEffect existingEffectD = new MemoryCombatAreaEffect ();
		existingEffectD.setCastingPlayerID (1);
		existingEffectD.setCombatAreaEffectID ("D");
		existingEffectD.setMapLocation (new MapCoordinates3DEx (20, 11, 1));
		CAEs.add (existingEffectD);
		
		// All later two effect should still be listed
		final List<String> listThree = utils.listCombatEffectsNotYetCastAtLocation (CAEs, spell, 1, combatLocation);
		assertEquals (2, listThree.size ());
		assertEquals ("C", listThree.get (0));
		assertEquals ("D", listThree.get (1));
	}
	
	/**
	 * Tests the listCombatAreaEffectsFromLocalisedSpells method
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testListCombatAreaEffectsFromLocalisedSpells () throws Exception
	{
		// Mock database
		final CommonDatabase db = mock (CommonDatabase.class);
		
		final CitySpellEffect citySpellEffect = new CitySpellEffect ();
		citySpellEffect.setCombatAreaEffectID ("CAE01");
		when (db.findCitySpellEffect ("SE001", "listCombatAreaEffectsFromLocalisedSpells")).thenReturn (citySpellEffect);
		
		// Spells
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MemoryMaintainedSpell heavenlyLight = new MemoryMaintainedSpell ();
		heavenlyLight.setCityLocation (new MapCoordinates3DEx (20, 11, 1));
		heavenlyLight.setCitySpellEffectID ("SE001");
		heavenlyLight.setCastingPlayerID (2);
		mem.getMaintainedSpell ().add (heavenlyLight);
		
		// CAEs
		final MemoryCombatAreaEffect heavenlyLightCAE = new MemoryCombatAreaEffect ();		// CAE is backed by a spell so will be excluded
		heavenlyLightCAE.setCombatAreaEffectID ("CAE01");
		heavenlyLightCAE.setMapLocation (new MapCoordinates3DEx (20, 11, 1));
		heavenlyLightCAE.setCastingPlayerID (2);
		mem.getCombatAreaEffect ().add (heavenlyLightCAE);

		final MemoryCombatAreaEffect selectedCAE = new MemoryCombatAreaEffect ();		// Same, other than it isn't backed by a spell, so is included
		selectedCAE.setCombatAreaEffectID ("CAE02");
		selectedCAE.setMapLocation (new MapCoordinates3DEx (20, 11, 1));
		selectedCAE.setCastingPlayerID (2);
		mem.getCombatAreaEffect ().add (selectedCAE);
		
		final MemoryCombatAreaEffect wrongLocation = new MemoryCombatAreaEffect ();
		wrongLocation.setCombatAreaEffectID ("CAE02");
		wrongLocation.setMapLocation (new MapCoordinates3DEx (20, 12, 1));
		wrongLocation.setCastingPlayerID (2);
		mem.getCombatAreaEffect ().add (wrongLocation);
		
		final MemoryCombatAreaEffect globalCAE = new MemoryCombatAreaEffect ();
		globalCAE.setCombatAreaEffectID ("CAE02");
		globalCAE.setCastingPlayerID (2);
		mem.getCombatAreaEffect ().add (globalCAE);
		
		final MemoryCombatAreaEffect unownedCAE = new MemoryCombatAreaEffect ();
		unownedCAE.setCombatAreaEffectID ("CAE02");
		unownedCAE.setMapLocation (new MapCoordinates3DEx (20, 11, 1));
		mem.getCombatAreaEffect ().add (unownedCAE);
		
		// Set up object to test
		final MemoryCombatAreaEffectUtilsImpl utils = new MemoryCombatAreaEffectUtilsImpl ();
		
		// Run method
		final List<MemoryCombatAreaEffect> list = utils.listCombatAreaEffectsFromLocalisedSpells (mem, new MapCoordinates3DEx (20, 11, 1), db);
		assertEquals (1, list.size ());
		assertSame (selectedCAE, list.get (0));
	}
}