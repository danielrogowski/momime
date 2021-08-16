package momime.server.process;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.calculations.HeroItemCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.ExperienceLevel;
import momime.common.database.Pick;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitEx;
import momime.common.database.UnitType;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.NewTurnMessageOffer;
import momime.common.messages.NewTurnMessageOfferHero;
import momime.common.messages.NewTurnMessageOfferItem;
import momime.common.messages.NewTurnMessageOfferUnits;
import momime.common.messages.NewTurnMessageTypeID;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.AddUnassignedHeroItemMessage;
import momime.common.messages.servertoclient.OfferAcceptedMessage;
import momime.common.utils.HeroItemUtils;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.UnitTypeUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.calculations.ServerUnitCalculations;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.messages.MomGeneralServerKnowledge;
import momime.server.utils.UnitAddLocation;
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
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;

	/** Hero item calculations */
	private HeroItemCalculations heroItemCalculations;
	
	/** Resource calculations */
	private ServerResourceCalculations serverResourceCalculations;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** Hero item utils */
	private HeroItemUtils heroItemUtils;
	
	/**
	 * Tests to see if the player has any heroes they can get, and if so rolls a chance that one offers to join them this turn (for a fee).
	 * 
	 * @param player Player to check for hero offer for
	 * @param players List of players
	 * @param trueMap True map details
	 * @param db Lookup lists built over the XML database
	 * @return The offer that was generate if there was one, otherwise null
     * @throws RecordNotFoundException If we can't find one of our picks in the database
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final NewTurnMessageOfferHero generateHeroOffer (final PlayerServerDetails player, final List<PlayerServerDetails> players,
		final FogOfWarMemory trueMap, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		NewTurnMessageOfferHero offer = null;
		
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
				offer = new NewTurnMessageOfferHero ();
				offer.setMsgType (NewTurnMessageTypeID.OFFER_HERO);
				offer.setCost (heroDef.getProductionCost () / (halfPrice ? 2 : 1));
				offer.setHero (hero);
				
				final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();
				trans.getNewTurnMessage ().add (offer);
			}
		}
		
		return offer;
	}
	
	/**
	 * Randomly checks if the player gets an offer to hire mercenary units.
	 * 
	 * @param player Player to check for units offer for
	 * @param players List of players
	 * @param trueMap True map details
	 * @param db Lookup lists built over the XML database
	 * @return The offer that was generate if there was one, otherwise null
     * @throws RecordNotFoundException If we can't find one of our picks in the database
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final NewTurnMessageOfferUnits generateUnitsOffer (final PlayerServerDetails player, final List<PlayerServerDetails> players,
		final FogOfWarMemory trueMap, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		NewTurnMessageOfferUnits offer = null;
		
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
					offer = new NewTurnMessageOfferUnits ();
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
		
		return offer;
	}

	/**
	 * Randomly checks if the player gets an offer to buy a hero item.
	 * 
	 * @param player Player to check for item offer for
	 * @param players List of players
	 * @param trueMap True map details
	 * @param db Lookup lists built over the XML database
	 * @param gsk General server knowledge
	 * @return The offer that was generate if there was one, otherwise null
     * @throws RecordNotFoundException If we can't find one of our picks in the database
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final NewTurnMessageOfferItem generateItemOffer (final PlayerServerDetails player, final List<PlayerServerDetails> players,
		final FogOfWarMemory trueMap, final CommonDatabase db, final MomGeneralServerKnowledge gsk)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		NewTurnMessageOfferItem offer = null;
		
		if (gsk.getAvailableHeroItem ().size () > 0)
		{
			// How much fame do we have?
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			final int fame = getResourceValueUtils ().calculateModifiedFame (priv.getResourceValue (), player, players, trueMap, db);
			
			// 2% chance +1 per each 25 fame
			int chance = 2 + (fame / 25);
			
			final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
			if (getPlayerPickUtils ().getQuantityOfPick (pub.getPick (), CommonDatabaseConstants.RETORT_ID_FAMOUS) > 0)
				chance = chance * 2;
			
			// Cap chance at 10% no matter what
			if (chance > 10)
				chance = 10;
			
			log.debug ("Player ID " + player.getPlayerDescription ().getPlayerID () + " has " + chance + "% chance of an item offer this turn");
			
			if (getRandomUtils ().nextInt (100) < chance)
			{
				// Randomly pick an item
				final NumberedHeroItem item = gsk.getAvailableHeroItem ().get (getRandomUtils ().nextInt (gsk.getAvailableHeroItem ().size ()));
				
				// How much does it cost?
				final boolean halfPrice = (getPlayerPickUtils ().getQuantityOfPick (pub.getPick (), CommonDatabaseConstants.RETORT_ID_CHARISMATIC) > 0);
				final int cost = getHeroItemCalculations ().calculateCraftingCost (item, db) / (halfPrice ? 2 : 1);
				
				// Now we can see if we can actually afford it
				final int gold = getResourceValueUtils ().findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
				if (gold >= cost)
				{
					log.debug ("Player ID " + player.getPlayerDescription ().getPlayerID () + " got offer for item URN " + item.getHeroItemURN () + " - " + item.getHeroItemName ());
					
					// Add NTM for it
					offer = new NewTurnMessageOfferItem ();
					offer.setMsgType (NewTurnMessageTypeID.OFFER_ITEM);
					offer.setCost (cost);
					offer.setHeroItem (item);
					
					final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();
					trans.getNewTurnMessage ().add (offer);
				}
			}
		}
		
		return offer;
	}
	
	/**
	 * Processes accepting an offer.  Assumes we've already validated that the offer is genuine (the client didn't make it up) and that they can afford it.
	 * 
	 * @param player Player who is accepting an offer
	 * @param offer Offer being accepted
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected data item can't be found
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If there is a validation problem
	 */
	@Override
	public final void acceptOffer (final PlayerServerDetails player, final NewTurnMessageOffer offer, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, PlayerNotFoundException, RecordNotFoundException, MomException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		// All ok - deduct money & send to client
		getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, -offer.getCost ());
		getServerResourceCalculations ().sendGlobalProductionValues (player, null, false);
		
		// Mark offer as accepted
		if (player.getPlayerDescription ().isHuman ())
		{
			final OfferAcceptedMessage msg = new OfferAcceptedMessage ();
			msg.setOfferURN (offer.getOfferURN ());
			player.getConnection ().sendMessageToClient (msg);
		}
		
		// Remove it so they can't accept it twice
		mom.getGeneralServerKnowledge ().getOffer ().remove (offer);
		
		// Find where units should appear
		final MemoryBuilding fortressLocation = getMemoryBuildingUtils ().findCityWithBuilding
			(player.getPlayerDescription ().getPlayerID (), CommonDatabaseConstants.BUILDING_FORTRESS,
				mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ());
		
		if (fortressLocation == null)
			throw new MomException ("Player " + player.getPlayerDescription ().getPlayerName () + " tried to accept an offer but they have no Wizard's Fortress");
		
		// Heroes
		if (offer instanceof NewTurnMessageOfferHero)
		{
			final NewTurnMessageOfferHero heroOffer = (NewTurnMessageOfferHero) offer;
			
			final MemoryUnit hero = getUnitUtils ().findUnitURN (heroOffer.getHero ().getUnitURN (),
				mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), "acceptOffer");
			
			if (hero.getStatus () != UnitStatusID.GENERATED)
				throw new MomException ("Player " + player.getPlayerDescription ().getPlayerName () + " tried to accept an offer for hero URN " +
					hero.getUnitURN () + " but their status is " + hero.getStatus ());
			
			final UnitAddLocation addLocation = getUnitServerUtils ().findNearestLocationWhereUnitCanBeAdded
				((MapCoordinates3DEx) fortressLocation.getCityLocation (), hero.getUnitID (),
					player.getPlayerDescription ().getPlayerID (), mom.getGeneralServerKnowledge ().getTrueMap (),
					mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());
			
			// It is possible that we try to hire a hero but he has nowhere to fit
			if (addLocation.getUnitLocation () != null)
			{
				getFogOfWarMidTurnChanges ().updateUnitStatusToAliveOnServerAndClients (hero, addLocation.getUnitLocation (), player,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getSessionDescription (), mom.getServerDB ());
				
				// Let it move this turn
				hero.setDoubleOverlandMovesLeft (2 * getUnitUtils ().expandUnitDetails (hero, null, null, null,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ()).getModifiedSkillValue
						(CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED));
				
				// Update amounts to show what the hero is consuming/generatnig
				getServerResourceCalculations ().recalculateGlobalProductionValues (player.getPlayerDescription ().getPlayerID (), false, mom);
			}
		}
		
		// Units
		else if (offer instanceof NewTurnMessageOfferUnits)
		{
			final NewTurnMessageOfferUnits unitsOffer = (NewTurnMessageOfferUnits) offer;
			
			// How much experience will the new unit(s) have?
			final Pick normalUnitRealm = mom.getServerDB ().findPick (CommonDatabaseConstants.UNIT_MAGIC_REALM_LIFEFORM_TYPE_ID_NORMAL, "acceptOffer");
			final UnitType normalUnit = mom.getServerDB ().findUnitType (normalUnitRealm.getUnitTypeID (), "acceptOffer");
			final ExperienceLevel expLevel = UnitTypeUtils.findExperienceLevel (normalUnit, unitsOffer.getLevelNumber ());
			
			// Keep looping until we run out of units to add, or run out of places to put them
			boolean keepGoing = true;
			int unitsAdded = 0;
			
			while (keepGoing)
			{
				final UnitAddLocation addLocation = getUnitServerUtils ().findNearestLocationWhereUnitCanBeAdded
					((MapCoordinates3DEx) fortressLocation.getCityLocation (), unitsOffer.getUnitID (),
						player.getPlayerDescription ().getPlayerID (), mom.getGeneralServerKnowledge ().getTrueMap (),
						mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());
				
				if (addLocation.getUnitLocation () == null)
					keepGoing = false;
				else
				{
					final MemoryUnit newUnit = getFogOfWarMidTurnChanges ().addUnitOnServerAndClients (mom.getGeneralServerKnowledge (),
						unitsOffer.getUnitID (), addLocation.getUnitLocation (), null, expLevel.getExperienceRequired (),
						null, player, UnitStatusID.ALIVE, mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());

					// Let it move this turn
					newUnit.setDoubleOverlandMovesLeft (2 * getUnitUtils ().expandUnitDetails (newUnit, null, null, null,
						mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ()).getModifiedSkillValue
							(CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED));
					
					unitsAdded++;
					if (unitsAdded >= unitsOffer.getUnitCount ())
						keepGoing = false;
				}
			}

			// Update amounts to show what the units are consuming/generatnig
			if (unitsAdded > 0)
				getServerResourceCalculations ().recalculateGlobalProductionValues (player.getPlayerDescription ().getPlayerID (), false, mom);
		}
		
		// Item
		else if (offer instanceof NewTurnMessageOfferItem)
		{
			final NewTurnMessageOfferItem itemOffer = (NewTurnMessageOfferItem) offer;
			
			final NumberedHeroItem item = getHeroItemUtils ().findHeroItemURN
				(itemOffer.getHeroItem ().getHeroItemURN (), mom.getGeneralServerKnowledge ().getAvailableHeroItem ());
			
			if (item == null)
				throw new RecordNotFoundException (NumberedHeroItem.class, itemOffer.getHeroItem ().getHeroItemURN (), "acceptOffer");
			
			mom.getGeneralServerKnowledge ().getAvailableHeroItem ().remove (item);
			priv.getUnassignedHeroItem ().add (item);
			
			// Add on client
			if (player.getPlayerDescription ().isHuman ())
			{
				final AddUnassignedHeroItemMessage msg = new AddUnassignedHeroItemMessage ();
				msg.setHeroItem (item);
				player.getConnection ().sendMessageToClient (msg);
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

	/**
	 * @return Hero item calculations
	 */
	public final HeroItemCalculations getHeroItemCalculations ()
	{
		return heroItemCalculations;
	}

	/**
	 * @param calc Hero item calculations
	 */
	public final void setHeroItemCalculations (final HeroItemCalculations calc)
	{
		heroItemCalculations = calc;
	}

	/**
	 * @return Resource calculations
	 */
	public final ServerResourceCalculations getServerResourceCalculations ()
	{
		return serverResourceCalculations;
	}

	/**
	 * @param calc Resource calculations
	 */
	public final void setServerResourceCalculations (final ServerResourceCalculations calc)
	{
		serverResourceCalculations = calc;
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
	 * @return Hero item utils
	 */
	public final HeroItemUtils getHeroItemUtils ()
	{
		return heroItemUtils;
	}

	/**
	 * @param util Hero item utils
	 */
	public final void setHeroItemUtils (final HeroItemUtils util)
	{
		heroItemUtils = util;
	}
}