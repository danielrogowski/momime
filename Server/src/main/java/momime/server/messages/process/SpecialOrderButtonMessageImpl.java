package momime.server.messages.process;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.calculations.MomCityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.TurnSystem;
import momime.common.messages.UnitSpecialOrder;
import momime.common.messages.UnitStatusID;
import momime.common.messages.clienttoserver.SpecialOrderButtonMessage;
import momime.common.messages.servertoclient.TextPopupMessage;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.MomServerResourceCalculations;
import momime.server.database.v0_9_5.MapFeature;
import momime.server.database.v0_9_5.TileType;
import momime.server.process.PlayerMessageProcessing;
import momime.server.utils.CityServerUtils;
import momime.server.utils.OverlandMapServerUtils;
import momime.server.utils.UnitServerUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.server.session.PostSessionClientToServerMessage;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Client sends this to server when a special order button is clicked with a particular unit stack
 */
public final class SpecialOrderButtonMessageImpl extends SpecialOrderButtonMessage implements PostSessionClientToServerMessage
{
	/** Class logger */
	private final Log log = LogFactory.getLog (SpecialOrderButtonMessageImpl.class);

	/** Server-only overland map utils */
	private OverlandMapServerUtils overlandMapServerUtils;

	/** City calculations */
	private MomCityCalculations cityCalculations;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;
	
	/** Server-only city utils */
	private CityServerUtils cityServerUtils;

	/** Resource calculations */
	private MomServerResourceCalculations serverResourceCalculations;
	
	/** Methods for dealing with player msgs */
	private PlayerMessageProcessing playerMessageProcessing;
	
	/**
	 * @param thread Thread for the session this message is for; from the thread, the processor can obtain the list of players, sd, gsk, gpl, etc
	 * @param sender Player who sent the message
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If various elements cannot be found in the DB
	 * @throws MomException If an AI player has enough books that they should get some free spells, but we can't find any suitable free spells to give them
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void process (final MultiplayerSessionThread thread, final PlayerServerDetails sender)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.trace ("Entering process: " + getMapLocation () + ", " + getSpecialOrder () + ", " + getUnitURN ().size ());

		final MomSessionVariables mom = (MomSessionVariables) thread;
		
		// What skill do we need
		final String necessarySkillID;
		switch (getSpecialOrder ())
		{
			case BUILD_CITY:
				necessarySkillID = CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_CREATE_OUTPOST;
				break;
				
			case MELD_WITH_NODE:
				necessarySkillID = CommonDatabaseConstants.VALUE_UNIT_SKILL_ID_MELD_WITH_NODE;
				break;
				
			default:
				throw new MomException (SpecialOrderButtonMessageImpl.class.getName () + " does not know skill ID corresponding to order of " + getSpecialOrder ());
		}
		
		// Get map cell
		final MemoryGridCell tc = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(getMapLocation ().getZ ()).getRow ().get (getMapLocation ().getY ()).getCell ().get (getMapLocation ().getX ());
		final TileType tileType = mom.getServerDB ().findTileType (tc.getTerrainData ().getTileTypeID (), "SpecialOrderButtonMessageImpl");
		final MapFeature mapFeature = (tc.getTerrainData ().getMapFeatureID () == null) ? null : mom.getServerDB ().findMapFeature
			(tc.getTerrainData ().getMapFeatureID (), "SpecialOrderButtonMessageImpl");
		
		// Process through all the units
		String error = null;
		if (getUnitURN ().size () == 0)
			error = "You must select at least one unit to give a special order to.";

		final List<MemoryUnit> unitsWithNecessarySkillID = new ArrayList<MemoryUnit> ();

		final Iterator<Integer> unitUrnIterator = getUnitURN ().iterator ();
		while ((error == null) && (unitUrnIterator.hasNext ()))
		{
			final Integer thisUnitURN = unitUrnIterator.next ();
			final MemoryUnit thisUnit = getUnitUtils ().findUnitURN (thisUnitURN, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());

			if (thisUnit == null)
				error = "Some of the units you are trying to give a special order to could not be found";
			else if (thisUnit.getOwningPlayerID () != sender.getPlayerDescription ().getPlayerID ())
				error = "Some of the units you are trying to give a special order to belong to another player";
			else if (thisUnit.getStatus () != UnitStatusID.ALIVE)
				error = "Some of the units you are trying to give a special order to are dead/dismissed";
			else if (!thisUnit.getUnitLocation ().equals (getMapLocation ()))
				error = "Some of the units you are trying to give a special order to are not at the right location";
			
			// Does it have the necessary skill?
			else if (getUnitUtils ().getModifiedSkillValue (thisUnit, thisUnit.getUnitHasSkill (), necessarySkillID, mom.getPlayers (),
				mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), mom.getServerDB ()) >= 0)
				
				unitsWithNecessarySkillID.add (thisUnit);
		}
		
		// Hopefully we found exactly 1 unit
		if (error == null)
		{
			if (unitsWithNecessarySkillID.size () == 0)
				error = "No unit in the unit stack has the necessary skill to perform the requested special order";
			else if (unitsWithNecessarySkillID.size () > 1)
			{
				switch (getSpecialOrder ())
				{
					case BUILD_CITY:
						error = "You must select only a single settler to build a city with";
						break;
						
					case MELD_WITH_NODE:
						error = "You must select only a single spirit to meld with a node";
						break;
						
					default:
						error = "You must select only a single unit with the relevant skill";
				}
			}
			else if ((getSpecialOrder () == UnitSpecialOrder.BUILD_CITY) && (!tileType.isCanBuildCity ()))
				error = "You can't build a city on this type of terrain";
			else if ((getSpecialOrder () == UnitSpecialOrder.BUILD_CITY) && (mapFeature != null) && (!mapFeature.isCanBuildCity ()))
				error = "You can't build a city on top of this type of map feature";
			else if ((getSpecialOrder () == UnitSpecialOrder.BUILD_CITY) && (getCityCalculations ().markWithinExistingCityRadius
				(mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
				getMapLocation ().getZ (), mom.getSessionDescription ().getMapSize ()).get (getMapLocation ().getX (), getMapLocation ().getY ())))
				error = "Cities cannot be built within " + mom.getSessionDescription ().getMapSize ().getCitySeparation () + " squares of another city";
			else if ((getSpecialOrder () == UnitSpecialOrder.MELD_WITH_NODE) && (tileType.getMagicRealmID () == null))
				error = "Can only use the meld with node skill with node map squares";
			else if ((getSpecialOrder () == UnitSpecialOrder.MELD_WITH_NODE) && (sender.getPlayerDescription ().getPlayerID ().equals
				(tc.getTerrainData ().getNodeOwnerID ())))
				error = "You already control this node so cannot meld with it again";
		}

		if (error != null)
		{
			// Return error
			log.warn (SpecialOrderButtonMessageImpl.class.getName () + ".process: " + sender.getPlayerDescription ().getPlayerName () + " got an error: " + error);

			final TextPopupMessage reply = new TextPopupMessage ();
			reply.setText (error);
			sender.getConnection ().sendMessageToClient (reply);
		}
		else
		{
			final MemoryUnit trueUnit = unitsWithNecessarySkillID.get (0);
			
			// In a simultaneous turns game, settlers are put on special orders and the city isn't built until movement resolution
			// But we still have to confirm to the client that their unit selection/build location was fine
			if (mom.getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS)
				getUnitServerUtils ().setAndSendSpecialOrder (trueUnit, getSpecialOrder (), sender);
			else
			{
				// In a one-player-at-a-time game, actions take place immediately
				switch (getSpecialOrder ())
				{
					case BUILD_CITY:
						getCityServerUtils ().buildCityFromSettler (mom.getGeneralServerKnowledge (), sender, trueUnit, mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());
						break;
						
					case MELD_WITH_NODE:
						// If successful, this will generate messages about the node capture
						getOverlandMapServerUtils ().attemptToMeldWithNode (trueUnit, mom.getGeneralServerKnowledge ().getTrueMap (),
							mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());
						
						getPlayerMessageProcessing ().sendNewTurnMessages (mom.getGeneralPublicKnowledge (), mom.getPlayers (), null);						
						break;
						
					default:
						throw new MomException (SpecialOrderButtonMessageImpl.class.getName () + " does not know how to handle order of " + getSpecialOrder ());
				}
			}
			
			// The settler was probably eating some rations and is now 'dead' so won't be eating those rations anymore
			// Or the spirit has now melded with the node and so a) is no longer consuming mana and b) we now get the big magic power boost from capturing the node
			getServerResourceCalculations ().recalculateGlobalProductionValues (sender.getPlayerDescription ().getPlayerID (), false, mom);
		}
		
		log.trace ("Exiting process");
	}

	/**
	 * @return Server-only overland map utils
	 */
	public final OverlandMapServerUtils getOverlandMapServerUtils ()
	{
		return overlandMapServerUtils;
	}
	
	/**
	 * @param utils Server-only overland map utils
	 */
	public final void setOverlandMapServerUtils (final OverlandMapServerUtils utils)
	{
		overlandMapServerUtils = utils;
	}

	/**
	 * @return City calculations
	 */
	public final MomCityCalculations getCityCalculations ()
	{
		return cityCalculations;
	}

	/**
	 * @param calc City calculations
	 */
	public final void setCityCalculations (final MomCityCalculations calc)
	{
		cityCalculations = calc;
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
	 * @return Server-only city utils
	 */
	public final CityServerUtils getCityServerUtils ()
	{
		return cityServerUtils;
	}

	/**
	 * @param utils Server-only city utils
	 */
	public final void setCityServerUtils (final CityServerUtils utils)
	{
		cityServerUtils = utils;
	}

	/**
	 * @return Resource calculations
	 */
	public final MomServerResourceCalculations getServerResourceCalculations ()
	{
		return serverResourceCalculations;
	}

	/**
	 * @param calc Resource calculations
	 */
	public final void setServerResourceCalculations (final MomServerResourceCalculations calc)
	{
		serverResourceCalculations = calc;
	}

	/**
	 * @return Methods for dealing with player msgs
	 */
	public PlayerMessageProcessing getPlayerMessageProcessing ()
	{
		return playerMessageProcessing;
	}

	/**
	 * @param obj Methods for dealing with player msgs
	 */
	public final void setPlayerMessageProcessing (final PlayerMessageProcessing obj)
	{
		playerMessageProcessing = obj;
	}
}