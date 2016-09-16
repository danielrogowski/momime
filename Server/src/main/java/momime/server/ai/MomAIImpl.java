package momime.server.ai;

import java.util.ArrayList;
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
import momime.common.database.RecordNotFoundException;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.TurnSystem;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.fogofwar.FogOfWarMidTurnChanges;

/**
 * Overall AI strategy + control
 */
public final class MomAIImpl implements MomAI
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (MomAIImpl.class);
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** AI decisions about cities */
	private CityAI cityAI;

	/** AI decisions about spells */
	private SpellAI spellAI;

	/** AI decisions about units */
	private UnitAI unitAI;
	
	/** Random number generator */
	private RandomUtils randomUtils;

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/**
	 *
	 * @param player AI player whose turn to take
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we can't find the race inhabiting the city, or various buildings
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws PlayerNotFoundException If we can't find the player who owns a unit
	 * @throws MomException If we find a consumption value that is not an exact multiple of 2, or we find a production value that is not an exact multiple of 2 that should be
	 */
	@Override
	public final void aiPlayerTurn (final PlayerServerDetails player, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		log.trace ("Entering aiPlayerTurn: Player ID " + player.getPlayerDescription ().getPlayerID () + ", turn " + mom.getGeneralPublicKnowledge ().getTurnNumber ());

		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		
		// First find out what the best units we can construct or summon are - this gives us
		// something to gauge existing units by, to know if they're now obsolete or not
		final List<AIConstructableUnit> constructableUnits = getUnitAI ().listAllUnitsWeCanConstruct (player, mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());
		if (constructableUnits.isEmpty ())
			log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " can't make any units at all");
		else
		{
			for (final AIConstructableUnit unit : constructableUnits)
				log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " could make unit " + unit);
			
			// Work out how much defence we want to aim to have at every defensive position.  Even 1 point short will be regarded as underdefended,
			// so this starts out that 1 defender is fine and will ramp up at turn 50,100,150,200 until we hit a max of wanting 5 defenders.
			// It'll also start to consider positions as underdefended as better units become available to us.
			final int highestAverageRating = constructableUnits.get (0).getAverageRating ();
			final int desiredDefenceRating = (highestAverageRating * Math.min (mom.getGeneralPublicKnowledge ().getTurnNumber (), 200)) / 50;
			log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + "'s strongest UAR = " + highestAverageRating + ", so DDR = " + desiredDefenceRating);
			
			// Estimate the total strength of all the units we have at every point on the map for attack and defense purposes,
			// as well as the strength of all enemy units for attack purposes.
			final AIUnitsAndRatings [] [] [] ourUnits = new AIUnitsAndRatings [mom.getSessionDescription ().getOverlandMapSize ().getDepth ()] [mom.getSessionDescription ().getOverlandMapSize ().getHeight ()] [mom.getSessionDescription ().getOverlandMapSize ().getWidth ()];
			final AIUnitsAndRatings [] [] [] enemyUnits = new AIUnitsAndRatings [mom.getSessionDescription ().getOverlandMapSize ().getDepth ()] [mom.getSessionDescription ().getOverlandMapSize ().getHeight ()] [mom.getSessionDescription ().getOverlandMapSize ().getWidth ()];
			getUnitAI ().calculateUnitRatingsAtEveryMapCell (ourUnits, enemyUnits, player.getPlayerDescription ().getPlayerID (), priv.getFogOfWarMemory (), mom.getPlayers (), mom.getServerDB ());
			
			// Go through every defensive position that we either own, or is unoccupied and we should capture, seeing if we have enough units there defending
			final List<AIUnitAndRatings> mobileUnits = new ArrayList<AIUnitAndRatings> ();
			final List<AIDefenceLocation> underdefendedLocations = getUnitAI ().evaluateCurrentDefence (ourUnits, enemyUnits, mobileUnits,
				player.getPlayerDescription ().getPlayerID (), priv.getFogOfWarMemory (), desiredDefenceRating, mom.getSessionDescription ().getOverlandMapSize (), mom.getServerDB ());

			for (final AIDefenceLocation location : underdefendedLocations)
				log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " is underdefended at " + location);
			
			for (final AIUnitAndRatings mu : mobileUnits)
			{
				log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " can move " + mu);
				
				// This is a bit of a hack but the alternative is that resetUnitOverlandMovement is done entirely server side and the movement rates are
				// sent to the client with the FOW routines which I'm reluctant to do.. it'd be technically more correct but a huge unnecessary flood of
				// messages over the network for something as simple as setting movement rates.
				mu.getUnit ().setDoubleOverlandMovesLeft (getUnitUtils ().findUnitURN (mu.getUnit ().getUnitURN (),
					mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), "aiPlayerTurn-M").getDoubleOverlandMovesLeft ());
			}
			
			// Try to find somewhere to move each mobile unit to.
			// In "one player at a time" games, we can see the results our each movement step, so here we only ever move 1 cell at a time.
			// In "simultaneous turns" games, we will move anywhere that we can reach in one turn.
			while (mobileUnits.size () > 0)
			{
				final AIUnitAndRatings thisUnit = mobileUnits.get (getRandomUtils ().nextInt (mobileUnits.size ()));
				if (thisUnit.getUnit ().getDoubleOverlandMovesLeft () <= 0)
				{
					// Not really sure how this could happen, but just to be safe
					mobileUnits.remove (thisUnit);
				}
				else
				{
					log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " checking where it can move " + thisUnit);
					
					final boolean moved = getUnitAI ().decideUnitMovement (thisUnit, underdefendedLocations, player, mom);
					if ((!moved) || (mom.getSessionDescription ().getTurnSystem () == TurnSystem.SIMULTANEOUS) || (thisUnit.getUnit ().getDoubleOverlandMovesLeft () <= 0))
						mobileUnits.remove (thisUnit);
				}
			}
		}

		// Decide what to build in all of this players' cities
		// Note we do this EVERY TURN - we don't wait for the previous building to complete - this allows the AI player to change their mind
		// e.g. if a city has minimal defence and has a university almost built and then a
		// group of halbardiers show up 2 squares away, you're going to want to stuff the university and rush buy the best unit you can afford
		for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
			for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
				for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
				{
					final OverlandMapCityData cityData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x).getCityData ();
					if ((cityData != null) && (cityData.getCityOwnerID () == player.getPlayerDescription ().getPlayerID ()))
					{
						final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (x, y, z);

						getCityAI ().decideWhatToBuild (cityLocation, cityData, priv.getFogOfWarMemory ().getMap (), priv.getFogOfWarMemory ().getBuilding (),
							mom.getSessionDescription (), mom.getServerDB ());
						
						getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
							mom.getPlayers (), cityLocation, mom.getSessionDescription ().getFogOfWarSetting ());
					}
				}

		// This relies on knowing what's being built in each city, so do it 2nd
		getCityAI ().setOptionalFarmersInAllCities (mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), player, mom.getServerDB (), mom.getSessionDescription ());

		// Do we need to choose a spell to research?
		if ((PlayerKnowledgeUtils.isWizard (pub.getWizardID ())) && (priv.getSpellIDBeingResearched () == null))
			getSpellAI ().decideWhatToResearch (player, mom.getServerDB ());

		log.trace ("Exiting aiPlayerTurn");
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

	/**
	 * @return AI decisions about cities
	 */
	public final CityAI getCityAI ()
	{
		return cityAI;
	}

	/**
	 * @param ai AI decisions about cities
	 */
	public final void setCityAI (final CityAI ai)
	{
		cityAI = ai;
	}

	/**
	 * @return AI decisions about spells
	 */
	public final SpellAI getSpellAI ()
	{
		return spellAI;
	}

	/**
	 * @param ai AI decisions about spells
	 */
	public final void setSpellAI (final SpellAI ai)
	{
		spellAI = ai;
	}

	/**
	 * @return AI decisions about units
	 */
	public final UnitAI getUnitAI ()
	{
		return unitAI;
	}

	/**
	 * @param ai AI decisions about units
	 */
	public final void setUnitAI (final UnitAI ai)
	{
		unitAI = ai;
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
}