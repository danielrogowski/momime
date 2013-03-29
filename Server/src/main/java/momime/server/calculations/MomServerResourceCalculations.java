package momime.server.calculations;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.calculations.CalculateCityProductionResult;
import momime.common.calculations.MomCityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.EnforceProductionID;
import momime.common.database.v0_9_4.SpellUpkeep;
import momime.common.database.v0_9_4.UnitUpkeep;
import momime.common.messages.MemoryBuildingUtils;
import momime.common.messages.PlayerPickUtils;
import momime.common.messages.ResourceValueUtils;
import momime.common.messages.SpellUtils;
import momime.common.messages.UnitUtils;
import momime.common.messages.servertoclient.v0_9_4.FullSpellListMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateGlobalEconomyMessage;
import momime.common.messages.servertoclient.v0_9_4.UpdateRemainingResearchCostMessage;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MemoryBuilding;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MomResourceValue;
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
import momime.server.database.ServerDatabaseEx;
import momime.server.database.v0_9_4.Building;
import momime.server.database.v0_9_4.Plane;
import momime.server.database.v0_9_4.ProductionType;
import momime.server.database.v0_9_4.Spell;
import momime.server.database.v0_9_4.Unit;
import momime.server.process.resourceconsumer.IMomResourceConsumer;
import momime.server.process.resourceconsumer.MomResourceConsumerBuilding;
import momime.server.process.resourceconsumer.MomResourceConsumerSpell;
import momime.server.process.resourceconsumer.MomResourceConsumerUnit;
import momime.server.utils.RandomUtils;
import momime.server.utils.UnitServerUtils;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Server side methods for dealing with calculating and updating the global economy
 * e.g. gold being produced, cities growing, buildings progressing construction, spells being researched and so on
 */
public final class MomServerResourceCalculations
{
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
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws RecordNotFoundException If we find a game element (unit, building or so on) that we can't find the definition for in the DB
	 * @throws PlayerNotFoundException If we can't find the player who owns a game element
	 * @throws MomException If there are any issues with data or calculation logic
	 */
	static final void recalculateAmountsPerTurn (final PlayerServerDetails player, final List<PlayerServerDetails> players,
		final FogOfWarMemory trueMap, final MomSessionDescription sd, final ServerDatabaseEx db, final Logger debugLogger)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		debugLogger.entering (MomServerResourceCalculations.class.getName (), "recalculateAmountsPerTurn", player.getPlayerDescription ().getPlayerID ());

		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		// Start from zero
		ResourceValueUtils.zeroAmountsPerTurn (priv.getResourceValue (), debugLogger);

		// Subtract the amount of gold, food and mana that units are eating up in upkeep from the amount of resources that we'll make this turn
		for (final MemoryUnit thisUnit : trueMap.getUnit ())
			if ((thisUnit.getOwningPlayerID () == player.getPlayerDescription ().getPlayerID ()) && (thisUnit.getStatus () == UnitStatusID.ALIVE) &&
				(!UnitServerUtils.doesUnitSpecialOrderResultInDeath (thisUnit.getSpecialOrder ())))
			{
				final Unit unitDetails = db.findUnit (thisUnit.getUnitID (), "recalculateAmountsPerTurn");
				for (final UnitUpkeep upkeep : unitDetails.getUnitUpkeep ())
					ResourceValueUtils.addToAmountPerTurn (priv.getResourceValue (), upkeep.getProductionTypeID (),
						-UnitUtils.getModifiedUpkeepValue (thisUnit, upkeep.getProductionTypeID (), players, db, debugLogger), debugLogger);
			}

		// Subtract the mana maintenance of all spells from the economy
		for (final MemoryMaintainedSpell thisSpell : trueMap.getMaintainedSpell ())
			if (thisSpell.getCastingPlayerID () == player.getPlayerDescription ().getPlayerID ())
			{
				final Spell spellDetails = db.findSpell (thisSpell.getSpellID (), "recalculateAmountsPerTurn");

				// Note we deal with Channeler retort halving spell maintenance below, so there is no
				// getModifiedUpkeepValue method for spells, we can just use the values right out of the database
				for (final SpellUpkeep upkeep : spellDetails.getSpellUpkeep ())
					ResourceValueUtils.addToAmountPerTurn (priv.getResourceValue (), upkeep.getProductionTypeID (), -upkeep.getUpkeepValue (), debugLogger);
			}

		// At this point, the only Mana recorded is consumption - so we can halve consumption if the wizard has Channeler
		// Round up, so 1 still = 1
		final int manaConsumption = ResourceValueUtils.findAmountPerTurnForProductionType (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, debugLogger);
		if ((manaConsumption < -1) && (PlayerPickUtils.getQuantityOfPick (pub.getPick (), CommonDatabaseConstants.VALUE_RETORT_ID_CHANNELER, debugLogger) > 0))
			ResourceValueUtils.addToAmountPerTurn (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MANA, (-manaConsumption) / 2, debugLogger);

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

						for (final CalculateCityProductionResult cityProduction : MomCityCalculations.calculateAllCityProductions
							(players, trueMap.getMap (), trueMap.getBuilding (), cityLocation, priv.getTaxRateID (), sd, true, db, debugLogger))

							ResourceValueUtils.addToAmountPerTurn (priv.getResourceValue (), cityProduction.getProductionTypeID (),
								cityProduction.getModifiedProductionAmount () - cityProduction.getConsumptionAmount (), debugLogger);
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
			ResourceValueUtils.addToAmountPerTurn (priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_MAGIC_POWER,
				nodeAuraMagicPower, debugLogger);

		// We never explicitly add Mana from Magic Power, this is calculated on the fly by ResourceValueUtils.calculateAmountPerTurnForProductionType

		debugLogger.exiting (MomServerResourceCalculations.class.getName (), "recalculateAmountsPerTurn");
	}

	/**
	 * Sends one player's global production values to them
	 *
	 * Note Delphi version could either send the values to one player or all players
	 * Java version operates only on one player because each player now has their own resource list
	 *
	 * @param player Player whose values to send
	 * @param castingSkillRemainingThisCombat Only specified when this is called as a result of a combat spell being cast, thereby reducing skill and mana
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	public static final void sendGlobalProductionValues (final PlayerServerDetails player, final Integer castingSkillRemainingThisCombat, final Logger debugLogger)
		throws JAXBException, XMLStreamException
	{
		debugLogger.entering (MomServerResourceCalculations.class.getName (), "sendGlobalProductionValues", player.getPlayerDescription ().getPlayerID ());

		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();

		final UpdateGlobalEconomyMessage msg = new UpdateGlobalEconomyMessage ();
		msg.getResourceValue ().addAll (priv.getResourceValue ());
		msg.setOverlandCastingSkillRemainingThisTurn (trans.getOverlandCastingSkillRemainingThisTurn ());

		if (castingSkillRemainingThisCombat != null)
			msg.setCastingSkillRemainingThisCombat (castingSkillRemainingThisCombat);

		player.getConnection ().sendMessageToClient (msg);

		debugLogger.exiting (MomServerResourceCalculations.class.getName (), "sendGlobalProductionValues", player.getPlayerDescription ().getPlayerID ());
	}

	/**
	 * Find everything that this player has that uses the specified type of consumption
	 *
	 * @param player Player whose productions we need to check
	 * @param players List of players in the session
	 * @param productionTypeID Production type to look for
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return List of all things this player has that consume this type of resource
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws RecordNotFoundException If the unitID, buildingID or spellID doesn't exist
	 * @throws MomException If we find a building consumption that isn't a multiple of 2
	 */
	static final List<IMomResourceConsumer> listConsumersOfProductionType (final PlayerServerDetails player, final List<PlayerServerDetails> players,
		final String productionTypeID, final FogOfWarMemory trueMap, final ServerDatabaseEx db, final Logger debugLogger)
		throws PlayerNotFoundException, RecordNotFoundException, MomException
	{
		debugLogger.entering (MomServerResourceCalculations.class.getName (), "findInsufficientProductionAndSellSomething",
			new String [] {player.getPlayerDescription ().getPlayerID ().toString (), productionTypeID});

		final List<IMomResourceConsumer> consumers = new ArrayList<IMomResourceConsumer> ();

		// Units
		for (final MemoryUnit thisUnit : trueMap.getUnit ())
			if ((thisUnit.getOwningPlayerID () == player.getPlayerDescription ().getPlayerID ()) && (thisUnit.getStatus () == UnitStatusID.ALIVE))
			{
				final int consumptionAmount = UnitUtils.getModifiedUpkeepValue (thisUnit, productionTypeID, players, db, debugLogger);
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
				final int consumptionAmount = MemoryBuildingUtils.findBuildingConsumption (building, productionTypeID, debugLogger);
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

		debugLogger.exiting (MomServerResourceCalculations.class.getName (), "findInsufficientProductionAndSellSomething");
		return consumers;
	}

	/**
	 * Searches through the production amounts to see if any which aren't allowed to go below zero are - if they are, we have to kill/sell something
	 *
	 * @param player Player whose productions we need to check
	 * @param players List of players in the session
	 * @param enforceType Type of production enforcement to check
	 * @param addOnStoredAmount True if OK for the per turn production amount to be negative as long as we have some in reserve (e.g. ok to make -5 gold per turn if we have 1000 gold already); False if per turn must be positive
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @return True if had to sell something, false if all production OK
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	private static final boolean findInsufficientProductionAndSellSomething (final PlayerServerDetails player, final List<PlayerServerDetails> players,
		final EnforceProductionID enforceType, final boolean addOnStoredAmount,
		final FogOfWarMemory trueMap, final MomSessionDescription sd, final ServerDatabaseEx db, final Logger debugLogger)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
	{
		debugLogger.entering (MomServerResourceCalculations.class.getName (), "findInsufficientProductionAndSellSomething",
			new String [] {player.getPlayerDescription ().getPlayerID ().toString (), enforceType.toString ()});

		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		// Search through different types of production looking for ones matching the required enforce type
		boolean found = false;
		final Iterator<ProductionType> productionTypeIter = db.getProductionType ().iterator ();
		while ((!found) && (productionTypeIter.hasNext ()))
		{
			final ProductionType productionType = productionTypeIter.next ();
			if (enforceType.equals (productionType.getEnforceProduction ()))
			{
				// Check how much of this type of production the player has
				int valueToCheck = ResourceValueUtils.findAmountPerTurnForProductionType (priv.getResourceValue (), productionType.getProductionTypeID (), debugLogger);

				if (addOnStoredAmount)
					valueToCheck = valueToCheck + ResourceValueUtils.findAmountStoredForProductionType (priv.getResourceValue (), productionType.getProductionTypeID (), debugLogger);

				if (valueToCheck < 0)
				{
					final List<IMomResourceConsumer> consumers = listConsumersOfProductionType (player, players, productionType.getProductionTypeID (), trueMap, db, debugLogger);

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
					chosenConsumer.kill (trueMap, players, sd, db, debugLogger);
				}
			}
		}

		debugLogger.exiting (MomServerResourceCalculations.class.getName (), "findInsufficientProductionAndSellSomething", found);
		return found;
	}

	/**
	 * Adds production generated this turn to the permanent values
	 * @param resourceList List of resources to accumulate
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws RecordNotFoundException If one of the production types in our resource list can't be found in the db
	 * @throws MomException If we encounter an unknown rounding direction, or a value that should be an exact multiple of 2 isn't
	 */
	static final void accumulateGlobalProductionValues (final List<MomResourceValue> resourceList, final ServerDatabaseEx db, final Logger debugLogger)
		throws RecordNotFoundException, MomException
	{
		debugLogger.entering (MomServerResourceCalculations.class.getName (), "accumulateGlobalProductionValues");

		for (final MomResourceValue thisProduction : resourceList)
			if (thisProduction.getAmountPerTurn () != 0)
			{
				final ProductionType productionType = db.findProductionType (thisProduction.getProductionTypeID (), "accumulateGlobalProductionValues");
				if (productionType.getAccumulatesInto () != null)
				{
					// See if need to halve it
					int amountToAdd = thisProduction.getAmountPerTurn ();
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
									throw new MomException ("accumulateGlobalProductionValues: Expect value for " + thisProduction.getProductionTypeID () + " being accumulated into " +
										productionType.getAccumulatesInto () + " to be exact multiple of 2 but was " + amountToAdd);
								break;

							default:
								throw new MomException ("accumulateGlobalProductionValues: Unknown rounding direction " + productionType.getAccumulationHalved ());
						}

					// Add it
					ResourceValueUtils.addToAmountStored (resourceList, productionType.getAccumulatesInto (), amountToAdd, debugLogger);
				}
			}

		debugLogger.exiting (MomServerResourceCalculations.class.getName (), "accumulateGlobalProductionValues");
	}

	/**
	 * Checks how much research we generate this turn and puts it towards the current spell
	 * @param player Player to progress research for
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 * @throws JAXBException If there is a problem converting a reply message into XML
	 * @throws XMLStreamException If there is a problem writing a reply message to the XML stream
	 */
	static final void progressResearch (final PlayerServerDetails player, final ServerDatabaseEx db, final Logger debugLogger)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		debugLogger.entering (MomServerResourceCalculations.class.getName (), "progressResearch");
		
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();
		
		if (priv.getSpellIDBeingResearched () != null)
		{
			final int researchAmount = ResourceValueUtils.findAmountPerTurnForProductionType
				(priv.getResourceValue (), CommonDatabaseConstants.VALUE_PRODUCTION_TYPE_ID_RESEARCH, debugLogger);
			
			debugLogger.finest ("Player generated " + researchAmount + " RPs this turn in spell research");
			
			// Put research points towards current spell
			if (researchAmount > 0)
			{
				final SpellResearchStatus spellBeingResarched = SpellUtils.findSpellResearchStatus
					(priv.getSpellResearchStatus (), priv.getSpellIDBeingResearched (), debugLogger);
				
				// Put some research towards it
				if (spellBeingResarched.getRemainingResearchCost () < researchAmount)
					spellBeingResarched.setRemainingResearchCost (0);
				else
					spellBeingResarched.setRemainingResearchCost (spellBeingResarched.getRemainingResearchCost () - researchAmount);
				
				// If finished, then grant spell and blank out research
				debugLogger.finest ("Player has " + spellBeingResarched.getRemainingResearchCost () + " RPs left before completing researching spell " + priv.getSpellIDBeingResearched ());
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
					MomServerSpellCalculations.randomizeSpellsResearchableNow (priv.getSpellResearchStatus (), db, debugLogger);
					
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
		
		debugLogger.exiting (MomServerResourceCalculations.class.getName (), "progressResearch");
	}
	
	/**
	 * Resets the casting skill the wizard(s) have left to spend this turn back to their full skill
	 * @param player Player who's overlandCastingSkillRemainingThisTurn to set
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 */
	static final void resetCastingSkillRemainingThisTurnToFull (final PlayerServerDetails player, final Logger debugLogger)
	{
		debugLogger.entering (MomServerResourceCalculations.class.getName (), "resetCastingSkillRemainingThisTurnToFull");

		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();
		
		trans.setOverlandCastingSkillRemainingThisTurn (ResourceValueUtils.calculateCastingSkillOfPlayer (priv.getResourceValue (), debugLogger));
		
		debugLogger.exiting (MomServerResourceCalculations.class.getName (), "resetCastingSkillRemainingThisTurnToFull");
	}
	
	/**
	 * Recalculates the amount of production of all types that we make each turn and sends the updated figures to the player(s)
	 *
	 * @param onlyOnePlayerID If zero will calculate values in cities for all players; if non-zero will calculate values only for the specified player
	 * @param duringStartPhase If true does additional work around enforcing that we are producing enough, and progresses city construction, spell research & casting and so on
	 * @param players List of all players in the session
	 * @param trueMap Server true knowledge of everything on the map
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws RecordNotFoundException If we find a game element (unit, building or so on) that we can't find the definition for in the DB
	 * @throws PlayerNotFoundException If we can't find the player who owns a game element
	 * @throws MomException If there are any issues with data or calculation logic
	 * @throws JAXBException If there is a problem converting a reply message into XML
	 * @throws XMLStreamException If there is a problem writing a reply message to the XML stream
	 */
	public static final void recalculateGlobalProductionValues (final int onlyOnePlayerID, final boolean duringStartPhase, final List<PlayerServerDetails> players,
		final FogOfWarMemory trueMap, final MomSessionDescription sd, final ServerDatabaseEx db, final Logger debugLogger)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		debugLogger.exiting (MomServerResourceCalculations.class.getName (), "recalculateGlobalProductionValues",
			new String [] {new Integer (onlyOnePlayerID).toString (), new Boolean (duringStartPhase).toString ()});

		for (final PlayerServerDetails player : players)
			if ((onlyOnePlayerID == 0) || (player.getPlayerDescription ().getPlayerID () == onlyOnePlayerID))
			{
				debugLogger.finest ("recalculateGlobalProductionValues processing player ID " + player.getPlayerDescription ().getPlayerID () +
					" (" + player.getPlayerDescription ().getPlayerName () + ")");
				
				final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

				// Calculate base amounts
				recalculateAmountsPerTurn (player, players, trueMap, sd, db, debugLogger);

				// If during the start phase, use these per turn production amounts as the amounts to add to the stored totals
				if (duringStartPhase)
				{
					// Search for a type of per-turn production that we're not producing enough of - currently this is only Rations
					// However (and to keep this consistent with how we handle insufficient stored Gold) there are too many interdependencies with what
					// may happen when we sell buildings, e.g. if we sell a Bank we don't only save its maintenance cost, the population then produces less gold
					// So the only safe way to do this is to recalculate ALL the productions, from scratch, every time we sell something!
					while (findInsufficientProductionAndSellSomething (player, players, EnforceProductionID.PER_TURN_AMOUNT_CANNOT_GO_BELOW_ZERO, false, trueMap, sd, db, debugLogger));

					// Now do the same for stored production
					while (findInsufficientProductionAndSellSomething (player, players, EnforceProductionID.STORED_AMOUNT_CANNOT_GO_BELOW_ZERO, true, trueMap, sd, db, debugLogger));

					// Per turn production amounts are now fine, so do the accumulation and effect calculations
					accumulateGlobalProductionValues (priv.getResourceValue (), db, debugLogger);
					progressResearch (player, db, debugLogger);
					resetCastingSkillRemainingThisTurnToFull (player, debugLogger);

					// Continue casting spells
					throw new UnsupportedOperationException ("recalculateGlobalProductionValues for turn > 1 not finished yet");
				}
				else if (player.getPlayerDescription ().isHuman ())

					// No need to send values during start phase, since the start phase calls recalculateGlobalProductionValues () for a second time with DuringStartPhase set to False
					sendGlobalProductionValues  (player, 0, debugLogger);
			}

		debugLogger.exiting (MomServerResourceCalculations.class.getName (), "recalculateGlobalProductionValues");
	}

	/**
	 * Prevent instantiation
	 */
	private MomServerResourceCalculations ()
	{
	}
}
