package momime.server.process;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.UnitEx;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.NewTurnMessageSummonUnit;
import momime.common.messages.NewTurnMessageTypeID;
import momime.common.messages.UnitStatusID;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerUnitCalculations;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.utils.UnitAddLocation;
import momime.server.utils.UnitServerUtils;

/**
 * Methods for casting specific types of spells
 */
public final class SpellCastingImpl implements SpellCasting
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (SpellCastingImpl.class);
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;
	
	/** Server-only unit calculations */
	private ServerUnitCalculations serverUnitCalculations;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/**
	 * Processes casting a summoning spell overland, finding where there is space for the unit to go and adding it
	 * 
	 * @param spell Summoning spell
	 * @param player Player who is casting it
	 * @param summonLocation Location where the unit will appear (or try to)
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void castOverlandSummoningSpell (final Spell spell, final PlayerServerDetails player, final MapCoordinates3DEx summonLocation,
		final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();
		
		// List out all the Unit IDs that this spell can summon
		final List<UnitEx> possibleUnits = getServerUnitCalculations ().listUnitsSpellMightSummon (spell, player, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getServerDB ());

		// Pick one at random
		if (possibleUnits.size () > 0)
		{
			final UnitEx summonedUnit = possibleUnits.get (getRandomUtils ().nextInt (possibleUnits.size ()));

			log.debug ("Player " + player.getPlayerDescription ().getPlayerName () + " had " + possibleUnits.size () + " possible units to summon from spell " +
				spell.getSpellID () + ", randomly picked unit ID " + summonedUnit.getUnitID ());

			// Check if the summon location has space for the unit
			final UnitAddLocation addLocation = getUnitServerUtils ().findNearestLocationWhereUnitCanBeAdded
				(summonLocation, summonedUnit.getUnitID (), player.getPlayerDescription ().getPlayerID (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());

			final MemoryUnit newUnit;
			if (addLocation.getUnitLocation () == null)
				newUnit = null;
			else
			{
				// Add the unit
				if (summonedUnit.getUnitMagicRealm ().equals (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO))
				{
					// The unit object already exists for heroes
					newUnit = getUnitServerUtils ().findUnitWithPlayerAndID (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), player.getPlayerDescription ().getPlayerID (), summonedUnit.getUnitID ());

					if (newUnit.getStatus () == UnitStatusID.NOT_GENERATED)
						getUnitServerUtils ().generateHeroNameAndRandomSkills (newUnit, mom.getServerDB ());

					getFogOfWarMidTurnChanges ().updateUnitStatusToAliveOnServerAndClients (newUnit, addLocation.getUnitLocation (), player, mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getSessionDescription (), mom.getServerDB ());
				}
				else
					// For non-heroes, create a new unit
					newUnit = getFogOfWarMidTurnChanges ().addUnitOnServerAndClients (mom.getGeneralServerKnowledge (),
						summonedUnit.getUnitID (), addLocation.getUnitLocation (), null, null, null,
						player, UnitStatusID.ALIVE, mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());
				
				// Let it move this turn
				newUnit.setDoubleOverlandMovesLeft (2 * getUnitUtils ().expandUnitDetails (newUnit, null, null, null,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ()).getModifiedSkillValue
						(CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED));
			}

			// Show on new turn messages for the player who summoned it
			if (player.getPlayerDescription ().isHuman ())
			{
				final NewTurnMessageSummonUnit summoningSpell = new NewTurnMessageSummonUnit ();
				summoningSpell.setMsgType (NewTurnMessageTypeID.SUMMONED_UNIT);
				summoningSpell.setSpellID (spell.getSpellID ());
				summoningSpell.setUnitID (summonedUnit.getUnitID ());
				summoningSpell.setCityLocation (addLocation.getUnitLocation ());
				summoningSpell.setUnitAddBumpType (addLocation.getBumpType ());

				if (newUnit != null)
					summoningSpell.setUnitURN (newUnit.getUnitURN ());

				trans.getNewTurnMessage ().add (summoningSpell);
			}
		}
	}

	/**
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
	}
	
	/**
	 * @return Server-only unit utils
	 */
	public final UnitServerUtils getUnitServerUtils ()
	{
		return unitServerUtils;
	}

	/**
	 * @param utils Server-only unit utils
	 */
	public final void setUnitServerUtils (final UnitServerUtils utils)
	{
		unitServerUtils = utils;
	}
	
	/**
	 * @return Server-only unit calculations
	 */
	public final ServerUnitCalculations getServerUnitCalculations ()
	{
		return serverUnitCalculations;
	}

	/**
	 * @param calc Server-only unit calculations
	 */
	public final void setServerUnitCalculations (final ServerUnitCalculations calc)
	{
		serverUnitCalculations = calc;
	}

	/**
	 * @return Random number generator
	 */
	public final RandomUtils getRandomUtils ()
	{
		return randomUtils;
	}

	/**
	 * @param utils Random number generator
	 */
	public final void setRandomUtils (final RandomUtils utils)
	{
		randomUtils = utils;
	}

	/**
	 * @return Methods for updating true map + players' memory
	 */
	public final FogOfWarMidTurnChanges getFogOfWarMidTurnChanges ()
	{
		return fogOfWarMidTurnChanges;
	}

	/**
	 * @param obj Methods for updating true map + players' memory
	 */
	public final void setFogOfWarMidTurnChanges (final FogOfWarMidTurnChanges obj)
	{
		fogOfWarMidTurnChanges = obj;
	}
}