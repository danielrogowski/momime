package momime.server.process;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitEx;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.NewTurnMessageOfferHero;
import momime.common.messages.NewTurnMessageOfferUnits;
import momime.common.messages.NewTurnMessageTypeID;
import momime.common.messages.UnitStatusID;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.ResourceValueUtils;
import momime.server.calculations.ServerUnitCalculations;
import momime.server.utils.UnitServerUtils;

/**
 * Generates offers to hire heroes, mercenaries and buy hero items
 */
public final class OfferGeneratorImpl implements OfferGenerator
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (OfferGeneratorImpl.class);

	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Server-only unit calculations */
	private ServerUnitCalculations serverUnitCalculations;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/**
	 * Tests to see if the player has any heroes they can get, and if so rolls a chance that one offers to join them this turn (for a fee).
	 * 
	 * @param player Player to check for hero offer for
	 * @param players List of players
	 * @param trueMap True map details
	 * @param db Lookup lists built over the XML database
     * @throws RecordNotFoundException If we can't find one of our picks in the database
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final void generateHeroOffer (final PlayerServerDetails player, final List<PlayerServerDetails> players,
		final FogOfWarMemory trueMap, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		// How much fame and gold do we have?
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		int gold = getResourceValueUtils ().findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
		final int fame = getResourceValueUtils ().calculateModifiedFame (priv.getResourceValue (), player, players, trueMap, db);
		
		// Hiring fee is halved if Charismatic
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();

		final boolean halfPrice = (getPlayerPickUtils ().getQuantityOfPick (pub.getPick (), CommonDatabaseConstants.RETORT_ID_CHARISMATIC) > 0);
		if (halfPrice)
			gold = gold * 2;
		
		// Get a list of all heroes that aren't already dead and we have necessary prerequisite picks for		
		final List<UnitEx> heroes = getServerUnitCalculations ().listHeroesForHire (player, trueMap.getUnit (), db);
		
		// Cut down the list to only ones we have enough fame for and can afford
		final Iterator<UnitEx> iter = heroes.iterator ();
		while (iter.hasNext ())
		{
			final UnitEx hero = iter.next ();
			if ((fame < hero.getHiringFame ()) || (gold < hero.getProductionCost ()))
				iter.remove ();
		}
		
		// Work out chance
		if (heroes.size () > 0)
		{
			int heroCount = 0;
			for (final MemoryUnit mu : trueMap.getUnit ())
				if ((mu.getOwningPlayerID () == player.getPlayerDescription ().getPlayerID ()) && (mu.getStatus () == UnitStatusID.ALIVE) &&
					(db.findUnit (mu.getUnitID (), "generateHeroOffer").getUnitMagicRealm ().equals (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_HERO)))
					
					heroCount++;
			
			int chance = 3 + (fame / 25);
			int chanceOutOf = 50 * (heroCount + 2);
			
			if (getPlayerPickUtils ().getQuantityOfPick (pub.getPick (), CommonDatabaseConstants.RETORT_ID_FAMOUS) > 0)
				chance = chance * 2;
			
			// Cap chance at 10% no matter what
			if (chanceOutOf < chance * 10)
				chanceOutOf = chance * 10;
			
			log.debug ("Player ID " + player.getPlayerDescription ().getPlayerID () + " has " + chance + " / " + chanceOutOf + " chance of a hero offer this turn");
			
			if (getRandomUtils ().nextInt (chanceOutOf) < chance)
			{
				// Randomly pick the hero
				final UnitEx heroDef = heroes.get (getRandomUtils ().nextInt (heroes.size ()));

				// Find the actual hero unit
				final MemoryUnit hero = getUnitServerUtils ().findUnitWithPlayerAndID (trueMap.getUnit (), player.getPlayerDescription ().getPlayerID (), heroDef.getUnitID ());

				// Generate their skills if not already done so
				if (hero.getStatus () == UnitStatusID.NOT_GENERATED)
					getUnitServerUtils ().generateHeroNameAndRandomSkills (hero, db);
				
				log.debug ("Player ID " + player.getPlayerDescription ().getPlayerID () + " got offer from hero " + heroDef.getUnitID () + ", Unit URN " + hero.getUnitURN ());
				
				// Add NTM for it
				final NewTurnMessageOfferHero offer = new NewTurnMessageOfferHero ();
				offer.setMsgType (NewTurnMessageTypeID.OFFER_HERO);
				offer.setCost (heroDef.getProductionCost () / (halfPrice ? 2 : 1));
				offer.setHero (hero);
				
				final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();
				trans.getNewTurnMessage ().add (offer);
			}
		}
	}
	
	/**
	 * Randomly checks if the player gets an offer to hire mercenary units.
	 * 
	 * @param player Player to check for units offer for
	 * @param players List of players
	 * @param trueMap True map details
	 * @param db Lookup lists built over the XML database
     * @throws RecordNotFoundException If we can't find one of our picks in the database
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final void generateUnitsOffer (final PlayerServerDetails player, final List<PlayerServerDetails> players,
		final FogOfWarMemory trueMap, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		// Find which plane we are on
		final MemoryBuilding fortressLocation = getMemoryBuildingUtils ().findCityWithBuilding
			(player.getPlayerDescription ().getPlayerID (), CommonDatabaseConstants.BUILDING_FORTRESS, trueMap.getMap (), trueMap.getBuilding ());
		if (fortressLocation != null)
		{
			// How much fame do we have?
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			final int fame = getResourceValueUtils ().calculateModifiedFame (priv.getResourceValue (), player, players, trueMap, db);
			
			// 1% chance +1 per each 20 fame
			int chance = 1 + (fame / 20);
			
			final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
			if (getPlayerPickUtils ().getQuantityOfPick (pub.getPick (), CommonDatabaseConstants.RETORT_ID_FAMOUS) > 0)
				chance = chance * 2;
			
			// Cap chance at 10% no matter what
			if (chance > 10)
				chance = 10;
			
			log.debug ("Player ID " + player.getPlayerDescription ().getPlayerID () + " has " + chance + "% chance of a units offer this turn");
			
			if (getRandomUtils ().nextInt (100) < chance)
			{
				// Get a list of races native to this plane
				final List<String> raceIDs = db.getRaces ().stream ().filter
					(r -> r.getNativePlane () == fortressLocation.getCityLocation ().getZ ()).map (r -> r.getRaceID ()).collect (Collectors.toList ());
				
				// Get a list of all normal units that belong to these races
				final List<UnitEx> units = db.getUnits ().stream ().filter (u -> (raceIDs.contains (u.getUnitRaceID ())) && (u.getProductionCost () != null) && 
					(u.getUnitHasSkill ().stream ().noneMatch (s -> s.getUnitSkillID ().equals (CommonDatabaseConstants.UNIT_SKILL_ID_CREATE_OUTPOST)))).collect (Collectors.toList ());
						
				// Randomly pick the hero
				final UnitEx unitDef = units.get (getRandomUtils ().nextInt (units.size ()));
				
				// How many?
				final int unitCountRoll = getRandomUtils ().nextInt (100) + 1 + fame;
				final int unitCount;
				if (unitCountRoll <= 60)
					unitCount = 1;
				else if (unitCountRoll <= 90)
					unitCount = 2;
				else
					unitCount = 3;
				
				// How experienced?
				final int expLevelRoll = getRandomUtils ().nextInt (100) + 1 + fame;
				final int expLevel;
				if (expLevelRoll <= 60)
					expLevel = 1;
				else if (expLevelRoll <= 90)
					expLevel = 2;
				else
					expLevel = 3;
				
				// Now we can work out how much they would cost
				final boolean halfPrice = (getPlayerPickUtils ().getQuantityOfPick (pub.getPick (), CommonDatabaseConstants.RETORT_ID_CHARISMATIC) > 0);
				final int cost = (unitDef.getProductionCost () * unitCount * (expLevel + 3)) / 2 / (halfPrice ? 2 : 1);
				
				// Now we can see if we can actually afford them
				final int gold = getResourceValueUtils ().findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
				if (gold >= cost)
				{
					log.debug ("Player ID " + player.getPlayerDescription ().getPlayerID () + " got offer for " + unitCount + "x " + unitDef.getUnitID () + " at level " + expLevel);
					
					// Add NTM for it
					final NewTurnMessageOfferUnits offer = new NewTurnMessageOfferUnits ();
					offer.setMsgType (NewTurnMessageTypeID.OFFER_UNITS);
					offer.setCost (cost);
					offer.setUnitCount (unitCount);
					offer.setLevelNumber (expLevel);
					offer.setUnitID (unitDef.getUnitID ());
					
					final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();
					trans.getNewTurnMessage ().add (offer);
				}
			}
		}
	}

	/**
	 * @return Resource value utils
	 */
	public final ResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param utils Resource value utils
	 */
	public final void setResourceValueUtils (final ResourceValueUtils utils)
	{
		resourceValueUtils = utils;
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
	 * @return Player pick utils
	 */
	public final PlayerPickUtils getPlayerPickUtils ()
	{
		return playerPickUtils;
	}

	/**
	 * @param utils Player pick utils
	 */
	public final void setPlayerPickUtils (final PlayerPickUtils utils)
	{
		playerPickUtils = utils;
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
	 * @return Memory building utils
	 */
	public final MemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param utils Memory building utils
	 */
	public final void setMemoryBuildingUtils (final MemoryBuildingUtils utils)
	{
		memoryBuildingUtils = utils;
	}
}