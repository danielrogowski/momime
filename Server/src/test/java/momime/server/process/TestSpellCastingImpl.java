package momime.server.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.multiplayer.sessionbase.PlayerType;
import com.ndg.utils.random.RandomUtils;

import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Spell;
import momime.common.database.UnitEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.NewTurnMessageSummonUnit;
import momime.common.messages.NewTurnMessageTypeID;
import momime.common.messages.UnitAddBumpTypeID;
import momime.common.messages.UnitStatusID;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.KnownWizardUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerUnitCalculations;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.messages.MomGeneralServerKnowledge;
import momime.server.utils.UnitAddLocation;
import momime.server.utils.UnitServerUtils;

/**
 * Tests the SpellCastingImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestSpellCastingImpl
{
	/**
	 * Tests the castOverlandSummoningSpell spell casting a creature summoning spell overland
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCastOverlandSummoningSpell_Creature () throws Exception
	{
		// Mock database
		final UnitEx unitDef = new UnitEx ();
		unitDef.setUnitID ("UN001");
		unitDef.setUnitMagicRealm ("LT01");
		
		final CommonDatabase db = mock (CommonDatabase.class);
		
		// General server knowledge
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setPlayerType (PlayerType.HUMAN);
		
		final MomTransientPlayerPrivateKnowledge trans3 = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, null, null, trans3);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player3);
		
		// Wizard
		final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), pd3.getPlayerID (), "castOverlandSummoningSpell")).thenReturn (wizardDetails);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Spell to cast
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		
		// It only summons 1 kind of unit
		final ServerUnitCalculations serverUnitCalculations = mock (ServerUnitCalculations.class);
		when (serverUnitCalculations.listUnitsSpellMightSummon (spell, wizardDetails, trueMap.getUnit (), db)).thenReturn (Arrays.asList (unitDef));

		// Will the unit fit in the city?
		final UnitAddLocation addLocation = new UnitAddLocation (new MapCoordinates3DEx (15, 25, 0), UnitAddBumpTypeID.CITY);
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		when (unitServerUtils.findNearestLocationWhereUnitCanBeAdded (new MapCoordinates3DEx (15, 25, 0), "UN001", 7, mom)).thenReturn (addLocation);
		
		// Mock creation of the unit
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);
		final MemoryUnit unit = new MemoryUnit ();
		when (midTurn.addUnitOnServerAndClients ("UN001", new MapCoordinates3DEx (15, 25, 0), null, null, null,
			player3, UnitStatusID.ALIVE, true, mom)).thenReturn (unit);
		
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (unit, null, null, null, players, trueMap, db)).thenReturn (xu);
		
		// Set up test object
		final RandomUtils randomUtils = mock (RandomUtils.class);

		final SpellCastingImpl casting = new SpellCastingImpl ();
		casting.setRandomUtils (randomUtils);
		casting.setServerUnitCalculations (serverUnitCalculations);
		casting.setUnitServerUtils (unitServerUtils);
		casting.setFogOfWarMidTurnChanges (midTurn);
		casting.setExpandUnitDetails (expand);
		casting.setKnownWizardUtils (knownWizardUtils);
		
		// Run test
		casting.castOverlandSummoningSpell (spell, player3, new MapCoordinates3DEx (15, 25, 0), true, mom);

		// Prove that unit got added
		verify (midTurn).addUnitOnServerAndClients ("UN001", new MapCoordinates3DEx (15, 25, 0), null, null, null,
			player3, UnitStatusID.ALIVE, true, mom);
		
		// Casting player gets the "You have summoned Hell Hounds!" new turn message popup
		assertEquals (1, trans3.getNewTurnMessage ().size ());
		assertEquals (NewTurnMessageTypeID.SUMMONED_UNIT, trans3.getNewTurnMessage ().get (0).getMsgType ());
		final NewTurnMessageSummonUnit ntm = (NewTurnMessageSummonUnit) trans3.getNewTurnMessage ().get (0);
		assertEquals ("SP001", ntm.getSpellID ());
		assertEquals ("UN001", ntm.getUnitID ());
		assertEquals (new MapCoordinates3DEx (15, 25, 0), ntm.getCityLocation ());
		assertEquals (UnitAddBumpTypeID.CITY, ntm.getUnitAddBumpType ());
		
		verifyNoMoreInteractions (midTurn);
	}
	
	/**
	 * Tests the castOverlandSummoningSpell spell casting a hero summoning spell overland
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testCastOverlandSummoningSpell_Hero () throws Exception
	{
		// Mock database; also lets say there's 9 possible heroes but 1 we've already summoned, and another we've already summoned and got them killed
		final List<UnitEx> possibleSummons = new ArrayList<UnitEx> ();
		
		final CommonDatabase db = mock (CommonDatabase.class);
		for (int n = 1; n <= 9; n++)
			if ((n != 3) && (n != 6))		// See alive + dead heroes below 
			{
				final UnitEx unitDef = new UnitEx ();
				unitDef.setUnitID ("UN00" + n);
				unitDef.setUnitMagicRealm (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO);
				possibleSummons.add (unitDef);
			}
		
		// General server knowledge
		final FogOfWarMemory trueMap = new FogOfWarMemory ();
		final MomGeneralServerKnowledge gsk = new MomGeneralServerKnowledge ();
		gsk.setTrueMap (trueMap);
		
		// Human player, who is also the one casting the spell
		final PlayerDescription pd3 = new PlayerDescription ();
		pd3.setPlayerID (7);
		pd3.setPlayerType (PlayerType.HUMAN);
		
		final MomTransientPlayerPrivateKnowledge trans3 = new MomTransientPlayerPrivateKnowledge ();
		
		final PlayerServerDetails player3 = new PlayerServerDetails (pd3, null, null, null, trans3);
		
		final List<PlayerServerDetails> players = new ArrayList<PlayerServerDetails> ();
		players.add (player3);
		
		// Wizard
		final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (trueMap.getWizardDetails (), pd3.getPlayerID (), "castOverlandSummoningSpell")).thenReturn (wizardDetails);
		
		// Session variables
		final MomSessionVariables mom = mock (MomSessionVariables.class);
		when (mom.getGeneralServerKnowledge ()).thenReturn (gsk);
		when (mom.getPlayers ()).thenReturn (players);
		when (mom.getServerDB ()).thenReturn (db);
		
		// Spell to cast
		final Spell spell = new Spell ();
		spell.setSpellID ("SP001");
		
		// It only summons 1 kind of unit
		final ServerUnitCalculations serverUnitCalculations = mock (ServerUnitCalculations.class);
		when (serverUnitCalculations.listUnitsSpellMightSummon (spell, wizardDetails, trueMap.getUnit (), db)).thenReturn (possibleSummons);

		// Will the unit fit in the city?
		final UnitAddLocation addLocation = new UnitAddLocation (new MapCoordinates3DEx (15, 25, 0), UnitAddBumpTypeID.CITY);
		final UnitServerUtils unitServerUtils = mock (UnitServerUtils.class);
		when (unitServerUtils.findNearestLocationWhereUnitCanBeAdded (new MapCoordinates3DEx (15, 25, 0), "UN008", 7, mom)).thenReturn (addLocation);
		
		// Heroes already exist in the units list, but lets say 1 we've already summoned, and another we've already summoned and got them killed
		MemoryUnit theHero = null;
		for (int n = 1; n <= 9; n++)
		{
			final MemoryUnit hero = new MemoryUnit ();
			if (n == 3)
				hero.setStatus (UnitStatusID.ALIVE);
			else if (n == 6)
				hero.setStatus (UnitStatusID.DEAD);
			else
				hero.setStatus (UnitStatusID.GENERATED);
			
			if (n == 8)
			{
				theHero = hero;		// The one we'll actually summon
				when (unitServerUtils.findUnitWithPlayerAndID (trueMap.getUnit (), 7, "UN00" + n)).thenReturn (hero);
			}
		}
		
		final ExpandUnitDetails expand = mock (ExpandUnitDetails.class);
		final ExpandedUnitDetails xu = mock (ExpandedUnitDetails.class);
		when (expand.expandUnitDetails (theHero, null, null, null, players, trueMap, db)).thenReturn (xu);
		
		// Fix random results
		final RandomUtils randomUtils = mock (RandomUtils.class);
		when (randomUtils.nextInt (7)).thenReturn (5);
		
		// Set up test object
		final FogOfWarMidTurnChanges midTurn = mock (FogOfWarMidTurnChanges.class);

		final SpellCastingImpl casting = new SpellCastingImpl ();
		casting.setRandomUtils (randomUtils);
		casting.setServerUnitCalculations (serverUnitCalculations);
		casting.setUnitServerUtils (unitServerUtils);
		casting.setFogOfWarMidTurnChanges (midTurn);
		casting.setExpandUnitDetails (expand);
		casting.setKnownWizardUtils (knownWizardUtils);
		
		// Run test
		casting.castOverlandSummoningSpell (spell, player3, new MapCoordinates3DEx (15, 25, 0), true, mom);

		// Prove that unit got updated, not added
		verify (midTurn).updateUnitStatusToAliveOnServerAndClients (theHero, new MapCoordinates3DEx (15, 25, 0), player3, true, mom);
		
		// Casting player gets the "You have summoned Hell Hounds!" new turn message popup
		assertEquals (1, trans3.getNewTurnMessage ().size ());
		assertEquals (NewTurnMessageTypeID.SUMMONED_UNIT, trans3.getNewTurnMessage ().get (0).getMsgType ());
		final NewTurnMessageSummonUnit ntm = (NewTurnMessageSummonUnit) trans3.getNewTurnMessage ().get (0);
		assertEquals ("SP001", ntm.getSpellID ());
		assertEquals ("UN008", ntm.getUnitID ());
		assertEquals (new MapCoordinates3DEx (15, 25, 0), ntm.getCityLocation ());
		assertEquals (UnitAddBumpTypeID.CITY, ntm.getUnitAddBumpType ());
		
		verifyNoMoreInteractions (midTurn);
	}
}