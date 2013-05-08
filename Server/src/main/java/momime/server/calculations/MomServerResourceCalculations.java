package momime.server.calculations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.calculations.CalculateCityProductionResult;
import momime.common.calculations.IMomCityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.newgame.v0_9_4.SpellSettingData;
import momime.common.database.v0_9_4.EnforceProductionID;
import momime.common.database.v0_9_4.SpellUpkeep;
import momime.common.database.v0_9_4.UnitUpkeep;
import momime.common.messages.IMemoryBuildingUtils;
import momime.common.messages.IPlayerPickUtils;
import momime.common.messages.IResourceValueUtils;
import momime.common.messages.ISpellUtils;
import momime.common.messages.IUnitUtils;
import momime.common.messages.servertoclient.v0_9_4.FullSpellListMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateGlobalEconomyMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateRemainingResearchCostMessage;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.NewTurnMessageData;
import momime.common.messages.v0_9_4.NewTurnMessageTypeID;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.common.messages.v0_9_4.SpellResearchStatus;
import momime.common.messages.v0_9_4.SpellResearchStatusID;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.server.IMomSessionVariables;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_4.Building;
import momime.server.database.v0_9_4.Plane;
import momime.server.database.v0_9_4.ProductionType;
import momime.server.database.v0_9_4.Spell;
import momime.server.database.v0_9_4.Unit;
import momime.server.process.ISpellProcessing;
import momime.server.process.resourceconsumer.IMomResourceConsumer;
import momime.server.process.resourceconsumer.MomResourceConsumerBuilding;
import momime.server.process.resourceconsumer.MomResourceConsumerSpell;
import momime.server.process.resourceconsumer.MomResourceConsumerUnit;
import momime.server.utils.IUnitServerUtils;
import momime.server.utils.RandomUtils;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Server side methods for dealing with calculating and updating the global economy
 * e.g. gold being produced, cities growing, buildings progressing construction, spells being researched and so on
 */
public final class MomServerResourceCalculations implements IMomServerResourceCalculations
{
	/** Class logger*/
	private final Logger log = Logger.getLogger (MomServerResourceCalculations.class.getName ());
	
	/** Spell processing methods */
	private ISpellProcessing spellProcessing;
	
	/** Resource value utils */
	private IResourceValueUtils resourceValueUtils;

	/** Spell utils */
	private ISpellUtils spellUtils;
	
	/** Memory building utils */
	private IMemoryBuildingUtils memoryBuildingUtils;
	
	/** Player pick utils */
	private IPlayerPickUtils playerPickUtils;
	
	/** Unit utils */
	private IUnitUtils unitUtils;

	/** City calculations */
	private IMomCityCalculations cityCalculations;
	
	/** Server-only unit utils */
	private IUnitServerUtils unitServerUtils;
	
	/** Server-only spell calculations */
	private IMomServerSpellCalculations serverSpellCalculations;
	
	/**
	 * Recalculates all per turn production values
	 *
	 * Note Delphi version could either calculate the values for one player or all players and was named RecalcProductionValues
	 * Java version operates only on one player because each player now has their own resource list; the loop is in the outer calling method recalculateGlobalProductionValues
	 *
	 * @param player Player to recalculate production for
	 * @param players List of all players in the session
	 * @param trueMap Server true knowledge of everything on the map
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If we find a game element (unit, building or so on) that we can't find the definition for in the DB
	 * @throws PlayerNotFoundException If we can't find the player who owns a game element
	 * @throws MomException If there are any issues with data or calculation logic
	 */
	final void recalculateAmountsPerTurn (final PlayerServerDetails player, final List<PlayerServerDetails> players,
		final FogOfWarMemory trueMap, final MomSessionDescription sd, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.entering (MomServerResourceCalculations.class.getName (), "recalculateAmountsPerTurn", player.getPlayerDescription ().getPlayerID ());

		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		// Start from zero
		getResourceValueUtils ().zeroAmountsPerTurn (priv.getResourceValue ());

		// Subtract the amount of gold, food and mana that units are eating up in upkeep from the amount of resources that we'll make this turn
		for (final MemoryUnit thisUnit : trueMap.getUnit ())
			if ((thisUnit.getOwningPlayerID () == player.getPlayerDescription ().getPlayerID ()) && (thisUnit.getStatus () == UnitStatusID.ALIVE) &&
				(!getUnitServerUtils ().doesUnitSpecialOrderResultInDeath (thisUnit.getSpecialOrder ())))
			{
				final Unit unitDetails = db.findUnit (thisUnit.getUnitID (), "recalculateAmountsPerTurn");
				for (final UnitUpkeep upkeep : unitDetails.getUnitUpkeep ())
					getResourceValueUtils ().addToAmountPerTurn (priv.getResourceValue (), upkeep.getProductionTypeID (),
						-getUnitUtils ().getModifiedUpkeepValue (thisUnit, upkeep.getProductionTypeID (), players, db));
			}

		// Subtract the mana maintenance of all spells from the economy
		for (final MemoryMaintainedSpell thisSpell : trueMap.getMaintainedSpell ())
			if (thisSpell.getCastingPlayerID () == player.getPlayerDescription ().getPlayerID ())
			{
				final Spell spellDetails = db.findSpell (thisSpell.getSpellID (), "recalculateAmountsPerTurn");

				// Note we deal with Channeler retort halving spell maintenance below, so there is no
				// getModifiedUpkeepValue method for spells, we can just use the values right out of the database
				for (final SpellUpkeep upkeep : spellDetails.getSpellUpkeep ())
					getResourceValueUtils ().addToAmountPerTurn (priv.getResourceValue (), upkeep.getProductionTypeID (), -upkeep.getUpkeepValue ());
			}

		// At this point, the only Mana recorded is consumption - so we can halve consumption if the wizard has Channeler
		// Round up, so 1 still = 1
		final int manaConsumption = getResourceValueUtils ().findAmountPerTurnForProductionType (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA);
		if ((manaConsumption < -1) && (getPlayerPickUtils ().getQuantityOfPick (pub.getPick (), CommonDatabaseConstants.VALUE_RETORT_ID_CHANNELER) > 0))
			getResourceValueUtils ().addToAmountPerTurn (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, (-manaConsumption) / 2);

		// The gist of the ordering here is that, now we've dealt with mana consumption, we can now add on things that *might* generate mana
		// In practice this is mostly irrelevant since *nothing* actually generates mana directly - it only generates magic power that can be converted into mana

		// Calculates production and consumption from all cities on the map
		for (final Plane plane : db.getPlane ())
			for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
				for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
				{
					final OverlandMapCityData cityData = trueMap.getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x).getCityData ();
					if ((cityData != null) && (cityData.getCityOwnerID () != null) && (cityData.getCityPopulation () != null) &&
						(cityData.getCityOwnerID () == player.getPlayerDescription ().getPlayerID ()) && (cityData.getCityPopulation () > 0))
					{
						// Calculate all productions from this city
						final OverlandMapCoordinates cityLocation = new OverlandMapCoordinates ();
						cityLocation.setX (x);
						cityLocation.setY (y);
						cityLocation.setPlane (plane.getPlaneNumber ());

						for (final CalculateCityProductionResult cityProduction : getCityCalculations ().calculateAllCityProductions
							(players, trueMap.getMap (), trueMap.getBuilding (), cityLocation, priv.getTaxRateID (), sd, true, db))

							getResourceValueUtils ().addToAmountPerTurn (priv.getResourceValue (), cityProduction.getProductionTypeID (),
								cityProduction.getModifiedProductionAmount () - cityProduction.getConsumptionAmount ());
					}
				}

		// Counts up how many node aura squares each player gets
		int nodeAuraSquares = 0;
		for (final Plane plane : db.getPlane ())
			for (int x = 0; x < sd.getMapSize ().getWidth (); x++)
				for (int y = 0; y < sd.getMapSize ().getHeight (); y++)
				{
					final OverlandMapTerrainData terrainData = trueMap.getMap ().getPlane ().get (plane.getPlaneNumber ()).getRow ().get (y).getCell ().get (x).getTerrainData ();
					if ((terrainData != null) && (terrainData.getNodeOwnerID () != null) && (terrainData.getNodeOwnerID () == player.getPlayerDescription ().getPlayerID ()))
						nodeAuraSquares++;
				}

		// How much magic power does each square generate?
		final int nodeAuraMagicPower = (nodeAuraSquares * sd.getNodeStrength ().getDoubleNodeAuraMagicPower ()) / 2;
		if (nodeAuraMagicPower > 0)
			getResourceValueUtils ().addToAmountPerTurn (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAGIC_POWER,
				nodeAuraMagicPower);

		// We never explicitly add Mana from Magic Power, this is calculated on the fly by getResourceValueUtils ().calculateAmountPerTurnForProductionType

		log.exiting (MomServerResourceCalculations.class.getName (), "recalculateAmountsPerTurn");
	}

	/**
	 * Sends one player's global production values to them
	 *
	 * Note Delphi version could either send the values to one player or all players
	 * Java version operates only on one player because each player now has their own resource list
	 *
	 * @param player Player whose values to send
	 * @param castingSkillRemainingThisCombat Only specified when this is called as a result of a combat spell being cast, thereby reducing skill and mana
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final void sendGlobalProductionValues (final PlayerServerDetails player, final Integer castingSkillRemainingThisCombat)
		throws JAXBException, XMLStreamException
	{
		log.entering (MomServerResourceCalculations.class.getName (), "sendGlobalProductionValues", player.getPlayerDescription ().getPlayerID ());

		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();

		final UpdateGlobalEconomyMessage msg = new UpdateGlobalEconomyMessage ();
		msg.getResourceValue ().addAll (priv.getResourceValue ());
		msg.setOverlandCastingSkillRemainingThisTurn (trans.getOverlandCastingSkillRemainingThisTurn ());

		if (castingSkillRemainingThisCombat != null)
			msg.setCastingSkillRemainingThisCombat (castingSkillRemainingThisCombat);

		player.getConnection ().sendMessageToClient (msg);

		log.exiting (MomServerResourceCalculations.class.getName (), "sendGlobalProductionValues", player.getPlayerDescription ().getPlayerID ());
	}

	/**
	 * Find everything that this player has that uses the specified type of consumption
	 *
	 * @param player Player whose productions we need to check
	 * @param players List of players in the session
	 * @param productionTypeID Production type to look for
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param db Lookup lists built over the XML database
	 * @return List of all things this player has that consume this type of resource
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws RecordNotFoundException If the unitID, buildingID or spellID doesn't exist
	 * @throws MomException If we find a building consumption that isn't a multiple of 2
	 */
	final List<IMomResourceConsumer> listConsumersOfProductionType (final PlayerServerDetails player, final List<PlayerServerDetails> players,
		final String productionTypeID, final FogOfWarMemory trueMap, final ServerDatabaseEx db)
		throws PlayerNotFoundException, RecordNotFoundException, MomException
	{
		log.entering (MomServerResourceCalculations.class.getName (), "listConsumersOfProductionType",
			new String [] {player.getPlayerDescription ().getPlayerID ().toString (), productionTypeID});

		final List<IMomResourceConsumer> consumers = new ArrayList<IMomResourceConsumer> ();

		// Units
		for (final MemoryUnit thisUnit : trueMap.getUnit ())
			if ((thisUnit.getOwningPlayerID () == player.getPlayerDescription ().getPlayerID ()) && (thisUnit.getStatus () == UnitStatusID.ALIVE))
			{
				final int consumptionAmount = getUnitUtils ().getModifiedUpkeepValue (thisUnit, productionTypeID, players, db);
				if (consumptionAmount > 0)
					consumers.add (new MomResourceConsumerUnit (player, productionTypeID, consumptionAmount, thisUnit));
			}

		// Buildings
		for (final MemoryBuilding thisBuilding : trueMap.getBuilding ())
		{
			final OverlandMapCityData cityData = trueMap.getMap ().getPlane ().get (thisBuilding.getCityLocation ().getPlane ()).getRow ().get
				(thisBuilding.getCityLocation ().getY ()).getCell ().get (thisBuilding.getCityLocation ().getX ()).getCityData ();

			if ((cityData != null) && (cityData.getCityOwnerID () != null) && (cityData.getCityOwnerID () == player.getPlayerDescription ().getPlayerID ()))
			{
				final Building building = db.findBuilding (thisBuilding.getBuildingID (), "listConsumersOfProductionType");
				final int consumptionAmount = getMemoryBuildingUtils ().findBuildingConsumption (building, productionTypeID);
				if (consumptionAmount > 0)
					consumers.add (new MomResourceConsumerBuilding (player, productionTypeID, consumptionAmount, thisBuilding));
			}
		}

		// Spells
		for (final MemoryMaintainedSpell thisSpell : trueMap.getMaintainedSpell ())
			if (thisSpell.getCastingPlayerID () == player.getPlayerDescription ().getPlayerID ())
			{
				final Spell spell = db.findSpell (thisSpell.getSpellID (), "listConsumersOfProductionType");

				boolean found = false;
				final Iterator<SpellUpkeep> upkeepIter = spell.getSpellUpkeep ().iterator ();
				while ((!found) && (upkeepIter.hasNext ()))
				{
					final SpellUpkeep upkeep = upkeepIter.next ();
					if (upkeep.getProductionTypeID ().equals (productionTypeID))
					{
						found = true;
						if (upkeep.getUpkeepValue () > 0)
							consumers.add (new MomResourceConsumerSpell (player, productionTypeID, upkeep.getUpkeepValue (), thisSpell));
					}
				}
			}

		log.exiting (MomServerResourceCalculations.class.getName (), "listConsumersOfProductionType");
		return consumers;
	}

	/**
	 * Searches through the production amounts to see if any which aren't allowed to go below zero are - if they are, we have to kill/sell something
	 *
	 * @param player Player whose productions we need to check
	 * @param enforceType Type of production enforcement to check
	 * @param addOnStoredAmount True if OK for the per turn production amount to be negative as long as we have some in reserve (e.g. ok to make -5 gold per turn if we have 1000 gold already); False if per turn must be positive
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return True if had to sell something, false if all production OK
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	private final boolean findInsufficientProductionAndSellSomething (final PlayerServerDetails player,
		final EnforceProductionID enforceType, final boolean addOnStoredAmount,
		final IMomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		log.entering (MomServerResourceCalculations.class.getName (), "findInsufficientProductionAndSellSomething",
			new String [] {player.getPlayerDescription ().getPlayerID ().toString (), enforceType.toString ()});

		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		// Search through different types of production looking for ones matching the required enforce type
		boolean found = false;
		final Iterator<ProductionType> productionTypeIter = mom.getServerDB ().getProductionType ().iterator ();
		while ((!found) && (productionTypeIter.hasNext ()))
		{
			final ProductionType productionType = productionTypeIter.next ();
			if (enforceType.equals (productionType.getEnforceProduction ()))
			{
				// Check how much of this type of production the player has
				int valueToCheck = getResourceValueUtils ().findAmountPerTurnForProductionType (priv.getResourceValue (), productionType.getProductionTypeID ());

				if (addOnStoredAmount)
					valueToCheck = valueToCheck + getResourceValueUtils ().findAmountStoredForProductionType (priv.getResourceValue (), productionType.getProductionTypeID ());

				if (valueToCheck < 0)
				{
					final List<IMomResourceConsumer> consumers = listConsumersOfProductionType
						(player, mom.getPlayers (), productionType.getProductionTypeID (), mom.getGeneralServerKnowledge ().getTrueMap (),
							mom.getServerDB ());

					// Want random choice to be weighted, e.g. if something consumes 4 gold then it should be 4x more likely to be chosen than something that only consumes 1 gold
					int totalConsumption = 0;
					for (final IMomResourceConsumer consumer : consumers)
						totalConsumption = totalConsumption + consumer.getConsumptionAmount ();

					int randomConsumption = RandomUtils.getGenerator ().nextInt (totalConsumption);
					IMomResourceConsumer chosenConsumer = null;
					final Iterator<IMomResourceConsumer> consumerIter = consumers.iterator ();
					while ((chosenConsumer == null) && (consumerIter.hasNext ()))
					{
						final IMomResourceConsumer thisConsumer = consumerIter.next ();
						if (randomConsumption < thisConsumer.getConsumptionAmount ())
							chosenConsumer = thisConsumer;
						else
							randomConsumption = randomConsumption - thisConsumer.getConsumptionAmount ();
					}

					if (chosenConsumer == null)
						throw new MomException ("findInsufficientProductionAndSellSomething failed to pick random weighted consumer from total consumption " + totalConsumption);

					// Kill off the unit/building/spell and generate a new turn message about it
					found = true;
					chosenConsumer.kill (mom);
				}
			}
		}

		log.exiting (MomServerResourceCalculations.class.getName (), "findInsufficientProductionAndSellSomething", found);
		return found;
	}

	/**
	 * Adds production generated this turn to the permanent values
	 * @param player Player that production amounts are being accumulated for
	 * @param spellSettings Spell combination settings, either from the server XML cache or the Session description
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If one of the production types in our resource list can't be found in the db
	 * @throws MomException If we encounter an unknown rounding direction, or a value that should be an exact multiple of 2 isn't
	 */
	final void accumulateGlobalProductionValues (final PlayerServerDetails player, final SpellSettingData spellSettings, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException
	{
		log.entering (MomServerResourceCalculations.class.getName (), "accumulateGlobalProductionValues", player.getPlayerDescription ().getPlayerID ());

		// Note that we can't simply go down the list of production types in the resource list because of the way Magic Power splits into
		// Mana/Research/Skill Improvement - so its entirely possible that we're supposed to accumulate some Mana even though there is
		// no pre-existing entry for Mana in this player's resource list
		for (final ProductionType productionType : db.getProductionType ())
			if (productionType.getAccumulatesInto () != null)
			{
				final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
				final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
				
				final int amountPerTurn = getResourceValueUtils ().calculateAmountPerTurnForProductionType (priv, pub.getPick (), productionType.getProductionTypeID (), spellSettings, db);
				if (amountPerTurn != 0)
				{
					// See if need to halve it
					int amountToAdd = amountPerTurn;
					if (productionType.getAccumulationHalved () != null)
						switch (productionType.getAccumulationHalved ())
						{
							case ROUND_DOWN:
								amountToAdd = amountToAdd / 2;
								break;

							case ROUND_UP:
								amountToAdd = (amountToAdd + 1) / 2;
								break;

							case MUST_BE_EXACT_MULTIPLE:
								if (amountToAdd % 2 == 0)
									amountToAdd = amountToAdd / 2;
								else
									throw new MomException ("accumulateGlobalProductionValues: Expect value for " + productionType.getProductionTypeID () + " being accumulated into " +
										productionType.getAccumulatesInto () + " to be exact multiple of 2 but was " + amountToAdd);
								break;

							default:
								throw new MomException ("accumulateGlobalProductionValues: Unknown rounding direction " + productionType.getAccumulationHalved ());
						}

					// Add it
					getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), productionType.getAccumulatesInto (), amountToAdd);
				}
			}

		log.exiting (MomServerResourceCalculations.class.getName (), "accumulateGlobalProductionValues");
	}

	/**
	 * Checks how much research we generate this turn and puts it towards the current spell
	 * @param player Player to progress research for
	 * @param spellSettings Spell combination settings, either from the server XML cache or the Session description
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 * @throws JAXBException If there is a problem converting a reply message into XML
	 * @throws XMLStreamException If there is a problem writing a reply message to the XML stream
	 * @throws MomException If we find an invalid casting reduction type
	 */
	final void progressResearch (final PlayerServerDetails player, final SpellSettingData spellSettings, final ServerDatabaseEx db)
		throws RecordNotFoundException, JAXBException, XMLStreamException, MomException
	{
		log.entering (MomServerResourceCalculations.class.getName (), "progressResearch");
		
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();
		
		if (priv.getSpellIDBeingResearched () != null)
		{
			final int researchAmount = getResourceValueUtils ().calculateAmountPerTurnForProductionType
				(priv, pub.getPick (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, spellSettings, db);
			
			log.finest ("Player generated " + researchAmount + " RPs this turn in spell research");
			
			// Put research points towards current spell
			if (researchAmount > 0)
			{
				final SpellResearchStatus spellBeingResarched = getSpellUtils ().findSpellResearchStatus
					(priv.getSpellResearchStatus (), priv.getSpellIDBeingResearched ());
				
				// Put some research towards it
				if (spellBeingResarched.getRemainingResearchCost () < researchAmount)
					spellBeingResarched.setRemainingResearchCost (0);
				else
					spellBeingResarched.setRemainingResearchCost (spellBeingResarched.getRemainingResearchCost () - researchAmount);
				
				// If finished, then grant spell and blank out research
				log.finest ("Player has " + spellBeingResarched.getRemainingResearchCost () + " RPs left before completing researching spell " + priv.getSpellIDBeingResearched ());
				if (spellBeingResarched.getRemainingResearchCost () == 0)
				{
					// Show on New Turn Messages on the client
					final NewTurnMessageData researchedSpell = new NewTurnMessageData ();
					researchedSpell.setMsgType (NewTurnMessageTypeID.RESEARCHED_SPELL);
					researchedSpell.setSpellID (priv.getSpellIDBeingResearched ());
					trans.getNewTurnMessage ().add (researchedSpell);
					
					// Update on server
					spellBeingResarched.setStatus (SpellResearchStatusID.AVAILABLE);
					priv.setSpellIDBeingResearched (null);
					
					// Pick another random spell to add to the 8 spells researchable now
					getServerSpellCalculations ().randomizeSpellsResearchableNow (priv.getSpellResearchStatus (), db);
					
					// And send this info to the client
					if (player.getPlayerDescription ().isHuman ())
					{
						final FullSpellListMessage spellsMsg = new FullSpellListMessage ();
						spellsMsg.getSpellResearchStatus ().addAll (priv.getSpellResearchStatus ());
						player.getConnection ().sendMessageToClient (spellsMsg);
					}
				}
				else if (player.getPlayerDescription ().isHuman ())
				{
					// Tell the client
					// NB. we don't bother sending this if we've now finished researching the spell - there's no point, the client
					// can tell there's zero RP left by the fact that the spell is now mpssAvailable
					final UpdateRemainingResearchCostMessage remainingMsg = new UpdateRemainingResearchCostMessage ();
					remainingMsg.setSpellID (priv.getSpellIDBeingResearched ());
					remainingMsg.setRemainingResearchCost (spellBeingResarched.getRemainingResearchCost ());
					player.getConnection ().sendMessageToClient (remainingMsg);
				}
			}
		}
		
		log.exiting (MomServerResourceCalculations.class.getName (), "progressResearch");
	}
	
	/**
	 * Resets the casting skill the wizard(s) have left to spend this turn back to their full skill
	 * @param player Player who's overlandCastingSkillRemainingThisTurn to set
	 */
	final void resetCastingSkillRemainingThisTurnToFull (final PlayerServerDetails player)
	{
		log.entering (MomServerResourceCalculations.class.getName (), "resetCastingSkillRemainingThisTurnToFull");

		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();
		
		trans.setOverlandCastingSkillRemainingThisTurn (getResourceValueUtils ().calculateCastingSkillOfPlayer (priv.getResourceValue ()));
		
		log.exiting (MomServerResourceCalculations.class.getName (), "resetCastingSkillRemainingThisTurnToFull");
	}
	
	/**
	 * Recalculates the amount of production of all types that we make each turn and sends the updated figures to the player(s)
	 *
	 * @param onlyOnePlayerID If zero will calculate values in cities for all players; if non-zero will calculate values only for the specified player
	 * @param duringStartPhase If true does additional work around enforcing that we are producing enough, and progresses city construction, spell research & casting and so on
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we find a game element (unit, building or so on) that we can't find the definition for in the DB
	 * @throws PlayerNotFoundException If we can't find the player who owns a game element
	 * @throws MomException If there are any issues with data or calculation logic
	 * @throws JAXBException If there is a problem converting a reply message into XML
	 * @throws XMLStreamException If there is a problem writing a reply message to the XML stream
	 */
	@Override
	public final void recalculateGlobalProductionValues (final int onlyOnePlayerID, final boolean duringStartPhase,
		final IMomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		log.exiting (MomServerResourceCalculations.class.getName (), "recalculateGlobalProductionValues",
			new String [] {new Integer (onlyOnePlayerID).toString (), new Boolean (duringStartPhase).toString ()});

		for (final PlayerServerDetails player : mom.getPlayers ())
			if ((onlyOnePlayerID == 0) || (player.getPlayerDescription ().getPlayerID () == onlyOnePlayerID))
			{
				log.finest ("recalculateGlobalProductionValues processing player ID " + player.getPlayerDescription ().getPlayerID () +
					" (" + player.getPlayerDescription ().getPlayerName () + ")");
				
				// Calculate base amounts
				recalculateAmountsPerTurn (player, mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (),
					mom.getSessionDescription (), mom.getServerDB ());

				// If during the start phase, use these per turn production amounts as the amounts to add to the stored totals
				if (duringStartPhase)
				{
					// Search for a type of per-turn production that we're not producing enough of - currently this is only Rations
					// However (and to keep this consistent with how we handle insufficient stored Gold) there are too many interdependencies with what
					// may happen when we sell buildings, e.g. if we sell a Bank we don't only save its maintenance cost, the population then produces less gold
					// So the only safe way to do this is to recalculate ALL the productions, from scratch, every time we sell something!
					while (findInsufficientProductionAndSellSomething (player, EnforceProductionID.PER_TURN_AMOUNT_CANNOT_GO_BELOW_ZERO,
						false, mom));

					// Now do the same for stored production
					while (findInsufficientProductionAndSellSomething (player, EnforceProductionID.STORED_AMOUNT_CANNOT_GO_BELOW_ZERO,
						true, mom));

					// Per turn production amounts are now fine, so do the accumulation and effect calculations
					accumulateGlobalProductionValues (player, mom.getSessionDescription ().getSpellSetting (), mom.getServerDB ());
					progressResearch (player, mom.getSessionDescription ().getSpellSetting (), mom.getServerDB ());
					resetCastingSkillRemainingThisTurnToFull (player);

					// Continue casting spells
					// If we actually completed casting one, then adjust calculated per turn production to take into account the extra mana being used
					if (getSpellProcessing ().progressOverlandCasting (mom.getGeneralServerKnowledge (),
						player, mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ()))
						
						recalculateAmountsPerTurn (player, mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (),
							mom.getSessionDescription (), mom.getServerDB ());
				}
				else if (player.getPlayerDescription ().isHuman ())

					// No need to send values during start phase, since the start phase calls recalculateGlobalProductionValues () for a second time with DuringStartPhase set to False
					sendGlobalProductionValues  (player, 0);
			}

		log.exiting (MomServerResourceCalculations.class.getName (), "recalculateGlobalProductionValues");
	}

	/**
	 * @return Spell processing methods
	 */
	public final ISpellProcessing getSpellProcessing ()
	{
		return spellProcessing;
	}

	/**
	 * @param obj Spell processing methods
	 */
	public final void setSpellProcessing (final ISpellProcessing obj)
	{
		spellProcessing = obj;
	}

	/**
	 * @return Resource value utils
	 */
	public final IResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param utils Resource value utils
	 */
	public final void setResourceValueUtils (final IResourceValueUtils utils)
	{
		resourceValueUtils = utils;
	}

	/**
	 * @return Spell utils
	 */
	public final ISpellUtils getSpellUtils ()
	{
		return spellUtils;
	}

	/**
	 * @param utils Spell utils
	 */
	public final void setSpellUtils (final ISpellUtils utils)
	{
		spellUtils = utils;
	}

	/**
	 * @return Memory building utils
	 */
	public final IMemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param utils Memory building utils
	 */
	public final void setMemoryBuildingUtils (final IMemoryBuildingUtils utils)
	{
		memoryBuildingUtils = utils;
	}
	
	/**
	 * @return Player pick utils
	 */
	public final IPlayerPickUtils getPlayerPickUtils ()
	{
		return playerPickUtils;
	}

	/**
	 * @param utils Player pick utils
	 */
	public final void setPlayerPickUtils (final IPlayerPickUtils utils)
	{
		playerPickUtils = utils;
	}
	
	/**
	 * @return Unit utils
	 */
	public final IUnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final IUnitUtils utils)
	{
		unitUtils = utils;
	}

	/**
	 * @return City calculations
	 */
	public final IMomCityCalculations getCityCalculations ()
	{
		return cityCalculations;
	}

	/**
	 * @param calc City calculations
	 */
	public final void setCityCalculations (final IMomCityCalculations calc)
	{
		cityCalculations = calc;
	}
	
	/**
	 * @return Server-only unit utils
	 */
	public final IUnitServerUtils getUnitServerUtils ()
	{
		return unitServerUtils;
	}

	/**
	 * @param utils Server-only unit utils
	 */
	public final void setUnitServerUtils (final IUnitServerUtils utils)
	{
		unitServerUtils = utils;
	}
	
	/**
	 * @return Server-only spell calculations
	 */
	public final IMomServerSpellCalculations getServerSpellCalculations ()
	{
		return serverSpellCalculations;
	}

	/**
	 * @param calc Server-only spell calculations
	 */
	public final void setServerSpellCalculations (final IMomServerSpellCalculations calc)
	{
		serverSpellCalculations = calc;
	}
}
