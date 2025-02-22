package momime.server.calculations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.sessionbase.PlayerType;
import com.ndg.utils.random.RandomUtils;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.calculations.CityProductionCalculations;
import momime.common.database.Building;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.EnforceProductionID;
import momime.common.database.Event;
import momime.common.database.Pick;
import momime.common.database.ProductionTypeAndUndoubledValue;
import momime.common.database.ProductionTypeEx;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellSetting;
import momime.common.internal.CityProductionBreakdown;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomResourceValue;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.NewTurnMessageSpell;
import momime.common.messages.NewTurnMessageTypeID;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.common.messages.UnitStatusID;
import momime.common.messages.WizardState;
import momime.common.messages.servertoclient.FullSpellListMessage;
import momime.common.messages.servertoclient.UpdateGlobalEconomyMessage;
import momime.common.messages.servertoclient.UpdateRemainingResearchCostMessage;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.SpellUtils;
import momime.server.MomSessionVariables;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.process.SpellQueueing;
import momime.server.process.resourceconsumer.MomResourceConsumer;
import momime.server.process.resourceconsumer.MomResourceConsumerBuilding;
import momime.server.process.resourceconsumer.MomResourceConsumerFactory;
import momime.server.process.resourceconsumer.MomResourceConsumerSpell;
import momime.server.process.resourceconsumer.MomResourceConsumerUnit;
import momime.server.utils.UnitServerUtils;

/**
 * Server side methods for dealing with calculating and updating the global economy e.g. gold being produced, cities growing, buildings progressing construction, spells being researched and so on
 */
public final class ServerResourceCalculationsImpl implements ServerResourceCalculations
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (ServerResourceCalculationsImpl.class);

	/** Spell queueing methods */
	private SpellQueueing spellQueueing;

	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;

	/** Spell utils */
	private SpellUtils spellUtils;

	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;

	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;

	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** City production calculations */
	private CityProductionCalculations cityProductionCalculations;

	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;

	/** Server-only spell calculations */
	private ServerSpellCalculations serverSpellCalculations;
	
	/** Factory for creating resource consumers */
	private MomResourceConsumerFactory momResourceConsumerFactory;

	/** Random number generator */
	private RandomUtils randomUtils;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Methods for working with wizardIDs */
	private PlayerKnowledgeUtils playerKnowledgeUtils;
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/**
	 * Recalculates all per turn production values
	 * Note: Delphi version could either calculate the values for one player or all players and was named RecalcProductionValues
	 * Java version operates only on one player because each player now has their own resource list; the loop is in the outer calling method recalculateGlobalProductionValues
	 * 
	 * @param player Player to recalculate production for
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we find a game element (unit, building or so on) that we can't find the definition for in the DB
	 * @throws PlayerNotFoundException If we can't find the player who owns a game element
	 * @throws MomException If there are any issues with data or calculation logic
	 */
	final void recalculateAmountsPerTurn (final PlayerServerDetails player, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final KnownWizardDetails knownWizard = getKnownWizardUtils ().findKnownWizardDetails
			(mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), player.getPlayerDescription ().getPlayerID (), "recalculateAmountsPerTurn");
		
		// If wizard is banished then they do not get several production types
		final List<String> zeroedProductionTypes = new ArrayList<String> ();
		final Event conjunctionEvent = (mom.getGeneralPublicKnowledge ().getConjunctionEventID () == null) ? null :
			mom.getServerDB ().findEvent (mom.getGeneralPublicKnowledge ().getConjunctionEventID (), "recalculateAmountsPerTurn");
		
		if ((getPlayerKnowledgeUtils ().isWizard (knownWizard.getWizardID ())) && (knownWizard.getWizardState () != WizardState.ACTIVE))
		{
			for (final ProductionTypeEx productionType : mom.getServerDB ().getProductionTypes ())
				if ((productionType.isZeroWhenBanished () != null) && (productionType.isZeroWhenBanished ()))
					zeroedProductionTypes.add (productionType.getProductionTypeID ());
		}
		
		// If there is a mana short then magic power is zeroed, but research from libraries and so on still work
		else if ((conjunctionEvent != null) && (conjunctionEvent.getEventMagicRealm () == null))
			zeroedProductionTypes.add (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER);
					
		// Start from zero
		getResourceValueUtils ().zeroAmountsPerTurn (priv.getResourceValue ());

		// If the wizard has Time Stop cast, then their own economy comes to a halt, generating no per turn values aside the upkeep of Time Stop itself
		final MemoryMaintainedSpell timeStop = getMemoryMaintainedSpellUtils ().findMaintainedSpell
			(mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), player.getPlayerDescription ().getPlayerID (),
				CommonDatabaseConstants.SPELL_ID_TIME_STOP, null, null, null, null);
		if (timeStop != null)
		{
			final Spell spellDetails = mom.getServerDB ().findSpell (timeStop.getSpellID (), "recalculateAmountsPerTurn");
			for (final ProductionTypeAndUndoubledValue upkeep : spellDetails.getSpellUpkeep ())
				getResourceValueUtils ().addToAmountPerTurn (priv.getResourceValue (), upkeep.getProductionTypeID (), -upkeep.getUndoubledProductionValue ());
		}
		else
		{
			// Subtract the amount of gold, food and mana that units are eating up in upkeep from the amount of resources that we'll make this turn
			for (final MemoryUnit thisUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
				if ((thisUnit.getOwningPlayerID () == player.getPlayerDescription ().getPlayerID ()) && (thisUnit.getStatus () == UnitStatusID.ALIVE) && (!getUnitServerUtils ().doesUnitSpecialOrderResultInDeath (thisUnit.getSpecialOrder ())))
				{
					final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (thisUnit, null, null, null,
						mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
					for (final String upkeepProductionTypeID : xu.listModifiedUpkeepProductionTypeIDs ())
						getResourceValueUtils ().addToAmountPerTurn (priv.getResourceValue (), upkeepProductionTypeID, -xu.getModifiedUpkeepValue (upkeepProductionTypeID));
				}
	
			// Subtract the mana maintenance of all spells from the economy
			for (final MemoryMaintainedSpell thisSpell : mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ())
				if (thisSpell.getCastingPlayerID () == player.getPlayerDescription ().getPlayerID ())
				{
					final Spell spellDetails = mom.getServerDB ().findSpell (thisSpell.getSpellID (), "recalculateAmountsPerTurn");
	
					// Note we deal with Channeler retort halving spell maintenance below, so there is no
					// getModifiedUpkeepValue method for spells, we can just use the values right out of the database
					for (final ProductionTypeAndUndoubledValue upkeep : spellDetails.getSpellUpkeep ())
						getResourceValueUtils ().addToAmountPerTurn (priv.getResourceValue (), upkeep.getProductionTypeID (), -upkeep.getUndoubledProductionValue ());
				}
		}
		
		// AI players get cheaper upkeep
		if ((player.getPlayerDescription ().getPlayerType () == PlayerType.AI) && (mom.getSessionDescription ().getDifficultyLevel ().getAiUpkeepMultiplier () != 100))
			for (final MomResourceValue resourceValue : priv.getResourceValue ())
				if (resourceValue.getAmountPerTurn () < 0)
					resourceValue.setAmountPerTurn ((resourceValue.getAmountPerTurn () * mom.getSessionDescription ().getDifficultyLevel ().getAiUpkeepMultiplier ()) / 100);
	
		// At this point, the only Mana recorded is consumption - so we can halve consumption if the wizard has Channeler
		// Round up, so 1 still = 1
		final int manaConsumption = getResourceValueUtils ().findAmountPerTurnForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
		if ((manaConsumption < -1) && (getPlayerPickUtils ().getQuantityOfPick (knownWizard.getPick (), CommonDatabaseConstants.RETORT_ID_CHANNELER) > 0))
			getResourceValueUtils ().addToAmountPerTurn (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, (-manaConsumption) / 2);
		
		if (timeStop == null)
		{
			// Gold upkeep of troops is reduced by wizard's fame
			if (getPlayerKnowledgeUtils ().isWizard (knownWizard.getWizardID ()))
			{
				final int goldConsumption = getResourceValueUtils ().findAmountPerTurnForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD);
				if (goldConsumption < 0)
				{
					int fame = getResourceValueUtils ().calculateModifiedFame (priv.getResourceValue (), knownWizard,
						mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
					if (fame > 0)
					{
						// Can't actually generate gold from fame, so limit it to only what will cancel out our consumption
						if (fame > -goldConsumption)
							fame = -goldConsumption;
						
						getResourceValueUtils ().addToAmountPerTurn (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD, fame);
					}
				}
			}
	
			// The gist of the ordering here is that, now we've dealt with mana consumption, we can now add on things that *might* generate mana
			// In practice this is mostly irrelevant since *nothing* actually generates mana directly - it only generates magic power that can be converted into mana
	
			// Calculates production and consumption from all cities on the map
			for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
				for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
					for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
					{
						final OverlandMapCityData cityData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
							(z).getRow ().get (y).getCell ().get (x).getCityData ();
						if ((cityData != null) && (cityData.getCityOwnerID () == player.getPlayerDescription ().getPlayerID ()))
						{
							// Calculate all productions from this city
							final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (x, y, z);
	
							for (final CityProductionBreakdown cityProduction : getCityProductionCalculations ().calculateAllCityProductions (mom.getPlayers (),
								mom.getGeneralServerKnowledge ().getTrueMap (), cityLocation, priv.getTaxRateID (), mom.getSessionDescription (),
								mom.getGeneralPublicKnowledge ().getConjunctionEventID (), true, false, mom.getServerDB ()).getProductionType ())
							{
								int cityProductionValue = 0;
								
								// We have to pay gold upkeep even when banished
								if ((cityProduction.getProductionTypeID ().equals (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER)) &&
									(zeroedProductionTypes.contains (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER)))
								{
									// Don't charge magic power consumption from Wizards' Guild when banished, since they don't generate magic power
									// plus they aren't getting the research points from the Wizards' Guild either
								}
								else
									cityProductionValue = -cityProduction.getConsumptionAmount ();
								
								// If banished, we don't get positive benefits like research from library
								if (!zeroedProductionTypes.contains (cityProduction.getProductionTypeID ()))
									cityProductionValue = cityProductionValue + cityProduction.getCappedProductionAmount () + cityProduction.getConvertToProductionAmount ();
								
								getResourceValueUtils ().addToAmountPerTurn (priv.getResourceValue (), cityProduction.getProductionTypeID (), cityProductionValue);
							}
						}
					}
	
			if (!zeroedProductionTypes.contains (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER))
			{
				// Find all the nodes we own where we have the corresponding mastery, e.g. find chaos nodes we own if we have chaos mastery
				final List<String> doubledNodeMagicRealmIDs = mom.getServerDB ().getPick ().stream ().filter
					(p -> (p.getNodeAndDispelBonus () != null) && (getPlayerPickUtils ().getQuantityOfPick (knownWizard.getPick (), p.getPickID ()) > 0)).map
					(p -> p.getNodeAndDispelBonus ()).collect (Collectors.toList ());
				
				final List<String> doubledNodeTileTypeIDs = mom.getServerDB ().getTileTypes ().stream ().filter
					(t -> doubledNodeMagicRealmIDs.contains (t.getMagicRealmID ())).map (t -> t.getTileTypeID ()).collect (Collectors.toList ());
				
				// Build up a map saying how much magic power (doubled) we get from each type of node.
				// So 2 normally, 4 if we have the corresponding mastery.
				final Map<String, Integer> nodeTileTypeIDs = mom.getServerDB ().getTileTypes ().stream ().filter (t -> t.getMagicRealmID () != null).collect
					(Collectors.toMap (t -> t.getTileTypeID (), t -> doubledNodeTileTypeIDs.contains (t.getTileTypeID ()) ? 4 : 2));
				
				// Double/halve it if there is a conjunction
				if ((conjunctionEvent != null) && (conjunctionEvent.getEventMagicRealm () != null))
				{
					final Pick magicRealm = mom.getServerDB ().findPick (conjunctionEvent.getEventMagicRealm (), "recalculateAmountsPerTurn");
					if (magicRealm.getPickExclusiveFrom ().isEmpty ())	// ignore good/bad moon
						mom.getServerDB ().getTileTypes ().stream ().filter (t -> t.getMagicRealmID () != null).forEach (t ->
						{
							int multiplier = nodeTileTypeIDs.get (t.getTileTypeID ());
							
							if (t.getMagicRealmID ().equals (conjunctionEvent.getEventMagicRealm ()))
								multiplier = multiplier * 2;
							else
								multiplier = multiplier / 2;
							
							nodeTileTypeIDs.put (t.getTileTypeID (), multiplier);
						});
				}
				
				// Counts up how many node aura squares and volcanoes this player has; node aura squares are doubled as per map above
				int doubledNodeAuraSquares = 0;
				int volcanoSquares = 0;
				int warpedNodes = 0;
				
				for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
					for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
						for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
						{
							final ServerGridCellEx tc = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
								(z).getRow ().get (y).getCell ().get (x);
							final OverlandMapTerrainData terrainData = tc.getTerrainData ();
							if (terrainData != null)
							{
								if ((terrainData.getNodeOwnerID () != null) && (player.getPlayerDescription ().getPlayerID ().equals (terrainData.getNodeOwnerID ())))
								{
									// Non-warped Node auras
									if ((terrainData.isWarped () == null) || (!terrainData.isWarped ()))
									{
										// Locate the actual node, if we're just on a tile covered by the aura
										final OverlandMapTerrainData nodeTerrainData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
											(tc.getAuraFromNode ().getZ ()).getRow ().get (tc.getAuraFromNode ().getY ()).getCell ().get (tc.getAuraFromNode ().getX ()).getTerrainData ();
										
										doubledNodeAuraSquares = doubledNodeAuraSquares + nodeTileTypeIDs.get (nodeTerrainData.getTileTypeID ());
									}
									
									// Warped nodes, but make sure its the actual node tile and not just an aura
									else if (nodeTileTypeIDs.containsKey (terrainData.getTileTypeID ()))
										warpedNodes++;
								}
	
								if ((terrainData.getVolcanoOwnerID () != null) && (player.getPlayerDescription ().getPlayerID ().equals (terrainData.getVolcanoOwnerID ())))
									volcanoSquares++;
							}
						}
				
				if (doubledNodeAuraSquares > 0)
				{
					// Node mastery gives 2x magic power
					if (getPlayerPickUtils ().getQuantityOfPick (knownWizard.getPick (), CommonDatabaseConstants.RETORT_NODE_MASTERY) > 0)
						doubledNodeAuraSquares = doubledNodeAuraSquares * 2;
			
					// How much magic power does each square generate?
					// Both number of aura tiles we counted up and the strength from the session description are doubled, so need to divide the overall result by 4
					final int nodeAuraMagicPower = (doubledNodeAuraSquares * mom.getSessionDescription ().getNodeStrength ().getDoubleNodeAuraMagicPower ()) / 4;
					getResourceValueUtils ().addToAmountPerTurn (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER, nodeAuraMagicPower);
				}
				
				if (volcanoSquares > 0)
					getResourceValueUtils ().addToAmountPerTurn (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER, volcanoSquares);
	
				if (warpedNodes > 0)
				{
					// Don't allow magic power to be negative
					final int currentMagicPower = getResourceValueUtils ().findAmountPerTurnForProductionType
						(priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER);
					
					final int magicPowerLoss = Math.min (warpedNodes * 5, currentMagicPower);
					getResourceValueUtils ().addToAmountPerTurn (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER, -magicPowerLoss);
				}
			}
		}
		
		// We never explicitly add Mana from Magic Power, this is calculated on the fly by getResourceValueUtils ().calculateAmountPerTurnForProductionType
	}

	/**
	 * Sends one player's global production values to them Note Delphi version could either send the values to one player or all players Java version operates only on one player because each player now has their own resource list
	 * 
	 * @param player Player whose values to send
	 * @param castingSkillRemainingThisCombat Only specified when this is called as a result of a combat spell being cast by the wizard, thereby reducing skill and mana
	 * @param spellCastThisCombatTurn True if castingSkillRemainingThisCombat is set because we cast a spell (it can also be set because of Mana Leak, so need false here)
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final void sendGlobalProductionValues (final PlayerServerDetails player, final Integer castingSkillRemainingThisCombat, final boolean spellCastThisCombatTurn)
		throws JAXBException, XMLStreamException
	{
		if (player.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();
	
			final UpdateGlobalEconomyMessage msg = new UpdateGlobalEconomyMessage ();
			msg.getResourceValue ().addAll (priv.getResourceValue ());
			msg.setOverlandCastingSkillRemainingThisTurn (trans.getOverlandCastingSkillRemainingThisTurn ());
			msg.setCastingSkillRemainingThisCombat (castingSkillRemainingThisCombat);
			
			if (spellCastThisCombatTurn)
				msg.setSpellCastThisCombatTurn (spellCastThisCombatTurn);
	
			player.getConnection ().sendMessageToClient (msg);
		}
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
	final List<MomResourceConsumer> listConsumersOfProductionType (final PlayerServerDetails player, final List<PlayerServerDetails> players,
		final String productionTypeID, final FogOfWarMemory trueMap, final CommonDatabase db) throws PlayerNotFoundException, RecordNotFoundException, MomException
	{
		final List<MomResourceConsumer> consumers = new ArrayList<MomResourceConsumer> ();

		// If we have Time Stop cast, that's the only consumption.  Not just the only spell, because summoned units use up mana too.
		final MemoryMaintainedSpell timeStop = getMemoryMaintainedSpellUtils ().findMaintainedSpell (trueMap.getMaintainedSpell (),
			player.getPlayerDescription ().getPlayerID (), CommonDatabaseConstants.SPELL_ID_TIME_STOP, null, null, null, null);
		
		if (timeStop == null)
		{
			// Units
			for (final MemoryUnit thisUnit : trueMap.getUnit ())
				if ((thisUnit.getOwningPlayerID () == player.getPlayerDescription ().getPlayerID ()) && (thisUnit.getStatus () == UnitStatusID.ALIVE))
				{
					final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (thisUnit, null, null, null, players, trueMap, db);
					final int consumptionAmount = xu.getModifiedUpkeepValue (productionTypeID);
					if (consumptionAmount > 0)
					{
						final MomResourceConsumerUnit consumer = getMomResourceConsumerFactory ().createUnitConsumer ();
						consumer.setPlayer (player);
						consumer.setProductionTypeID (productionTypeID);
						consumer.setConsumptionAmount (consumptionAmount);
						consumer.setUnit (thisUnit);
						consumers.add (consumer);
					}
				}
	
			// Buildings
			for (final MemoryBuilding thisBuilding : trueMap.getBuilding ())
			{
				final OverlandMapCityData cityData = trueMap.getMap ().getPlane ().get (thisBuilding.getCityLocation ().getZ ()).getRow ().get (thisBuilding.getCityLocation ().getY ()).getCell ().get (thisBuilding.getCityLocation ().getX ()).getCityData ();
	
				if ((cityData != null) && (cityData.getCityOwnerID () == player.getPlayerDescription ().getPlayerID ()))
				{
					final Building building = db.findBuilding (thisBuilding.getBuildingID (), "listConsumersOfProductionType");
					final int consumptionAmount = getMemoryBuildingUtils ().findBuildingConsumption (building, productionTypeID);
					if (consumptionAmount > 0)
					{
						final MomResourceConsumerBuilding consumer = getMomResourceConsumerFactory ().createBuildingConsumer ();
						consumer.setPlayer (player);
						consumer.setProductionTypeID (productionTypeID);
						consumer.setConsumptionAmount (consumptionAmount);
						consumer.setBuilding (thisBuilding);
						consumers.add (consumer);
					}
				}
			}
		}

		// Spells
		for (final MemoryMaintainedSpell thisSpell : trueMap.getMaintainedSpell ())
			if ((thisSpell.getCastingPlayerID () == player.getPlayerDescription ().getPlayerID ()) &&
				((timeStop == null) || (timeStop == thisSpell)))
			{
				final Spell spell = db.findSpell (thisSpell.getSpellID (), "listConsumersOfProductionType");

				boolean found = false;
				final Iterator<ProductionTypeAndUndoubledValue> upkeepIter = spell.getSpellUpkeep ().iterator ();
				while ((!found) && (upkeepIter.hasNext ()))
				{
					final ProductionTypeAndUndoubledValue upkeep = upkeepIter.next ();
					if (upkeep.getProductionTypeID ().equals (productionTypeID))
					{
						found = true;
						if (upkeep.getUndoubledProductionValue () > 0)
						{
							final MomResourceConsumerSpell consumer = getMomResourceConsumerFactory ().createSpellConsumer ();
							consumer.setPlayer (player);
							consumer.setProductionTypeID (productionTypeID);
							consumer.setConsumptionAmount (upkeep.getUndoubledProductionValue ());
							consumer.setSpell (thisSpell);
							consumers.add (consumer);
						}
					}
				}
			}

		return consumers;
	}

	/**
	 * Searches through the production amounts to see if any which aren't allowed to go below zero are - if they are, we have to kill/sell something
	 * 
	 * @param player Player whose productions we need to check
	 * @param wizardDetails Wizard whose productions we need to check
	 * @param enforceType Type of production enforcement to check
	 * @param addOnStoredAmount True if OK for the per turn production amount to be negative as long as we have some in reserve (e.g. ok to make -5 gold per turn if we have 1000 gold already); False if per turn must be positive
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return True if had to sell something, false if all production OK
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	private final boolean findInsufficientProductionAndSellSomething (final PlayerServerDetails player, final KnownWizardDetails wizardDetails, final EnforceProductionID enforceType,
		final boolean addOnStoredAmount, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		// Search through different types of production looking for ones matching the required enforce type
		boolean found = false;
		final Iterator<ProductionTypeEx> productionTypeIter = mom.getServerDB ().getProductionTypes ().iterator ();
		while ((!found) && (productionTypeIter.hasNext ()))
		{
			final ProductionTypeEx productionType = productionTypeIter.next ();
			if (enforceType.equals (productionType.getEnforceProduction ()))
			{
				// Check how much of this type of production the player has
				int valueToCheck = getResourceValueUtils ().calculateAmountPerTurnForProductionType
					(priv, wizardDetails.getPick (), productionType.getProductionTypeID (), mom.getSessionDescription ().getSpellSetting (), mom.getServerDB ());

				log.debug ("findInsufficientProductionAndSellSomething: PlayerID " + player.getPlayerDescription ().getPlayerID () + " is generating " + valueToCheck + " per turn of productionType " + productionType.getProductionTypeID () + " which has enforceType " + enforceType);

				if (addOnStoredAmount)
				{
					final int amountStored = getResourceValueUtils ().findAmountStoredForProductionType (priv.getResourceValue (), productionType.getProductionTypeID ());
					valueToCheck = valueToCheck + amountStored;
					log.debug ("findInsufficientProductionAndSellSomething: +amountStored " + amountStored + " = " + valueToCheck);
				}

				if (valueToCheck < 0)
				{
					final List<MomResourceConsumer> consumers = listConsumersOfProductionType (player, mom.getPlayers (), productionType.getProductionTypeID (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());

					log.debug ("findInsufficientProductionAndSellSomething: Found " + consumers.size () + " consumers of productionType " + productionType.getProductionTypeID ());
					if (consumers.size () > 0)
					{
						// Want random choice to be weighted, e.g. if something consumes 4 gold then it should be 4x more likely to be chosen than something that only consumes 1 gold
						int totalConsumption = 0;
						for (final MomResourceConsumer consumer : consumers)
							totalConsumption = totalConsumption + consumer.getConsumptionAmount ();
	
						int randomConsumption = getRandomUtils ().nextInt (totalConsumption);
						MomResourceConsumer chosenConsumer = null;
						final Iterator<MomResourceConsumer> consumerIter = consumers.iterator ();
						while ((chosenConsumer == null) && (consumerIter.hasNext ()))
						{
							final MomResourceConsumer thisConsumer = consumerIter.next ();
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
		}

		return found;
	}

	/**
	 * Adds production generated this turn to the permanent values
	 * 
	 * @param player Player that production amounts are being accumulated for
	 * @param wizardDetails Wizard that production amounts are being accumulated for
	 * @param spellSettings Spell combination settings, either from the server XML cache or the Session description
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If one of the production types in our resource list can't be found in the db
	 * @throws MomException If we encounter an unknown rounding direction, or a value that should be an exact multiple of 2 isn't
	 */
	final void accumulateGlobalProductionValues (final PlayerServerDetails player, final KnownWizardDetails wizardDetails, final SpellSetting spellSettings, final CommonDatabase db)
		throws RecordNotFoundException, MomException
	{
		// Note that we can't simply go down the list of production types in the resource list because of the way Magic Power splits into
		// Mana/Research/Skill Improvement - so its entirely possible that we're supposed to accumulate some Mana even though there is
		// no pre-existing entry for Mana in this player's resource list
		for (final ProductionTypeEx productionType : db.getProductionTypes ())
			if (productionType.getAccumulatesInto () != null)
			{
				final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

				final int amountPerTurn = getResourceValueUtils ().calculateAmountPerTurnForProductionType
					(priv, wizardDetails.getPick (), productionType.getProductionTypeID (), spellSettings, db);
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
									throw new MomException ("accumulateGlobalProductionValues: Expect value for " + productionType.getProductionTypeID () + " being accumulated into " + productionType.getAccumulatesInto () + " to be exact multiple of 2 but was " + amountToAdd);
								break;

							default:
								throw new MomException ("accumulateGlobalProductionValues: Unknown rounding direction " + productionType.getAccumulationHalved ());
						}

					// Add it
					getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), productionType.getAccumulatesInto (), amountToAdd);
				}
			}
	}

	/**
	 * Checks how much research we generate this turn and puts it towards the current spell
	 * 
	 * @param player Player to progress research for
	 * @param wizardDetails Wizard to progress research for
	 * @param players Player list
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws JAXBException If there is a problem converting a reply message into XML
	 * @throws XMLStreamException If there is a problem writing a reply message to the XML stream
	 * @throws MomException If we find an invalid casting reduction type
	 */
	final void progressResearch (final PlayerServerDetails player, final KnownWizardDetails wizardDetails, final List<PlayerServerDetails> players,
		final MomSessionDescription sd, final CommonDatabase db)
		throws RecordNotFoundException, JAXBException, XMLStreamException, MomException, PlayerNotFoundException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();

		if (priv.getSpellIDBeingResearched () != null)
		{
			int researchAmount = getResourceValueUtils ().calculateAmountPerTurnForProductionType
				(priv, wizardDetails.getPick (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_RESEARCH, sd.getSpellSetting (), db) +
				getResourceValueUtils ().calculateResearchFromUnits (player.getPlayerDescription ().getPlayerID (), players, priv.getFogOfWarMemory (), db);				

			log.debug ("Player ID " + player.getPlayerDescription ().getPlayerID () + " generated " + researchAmount + " RPs this turn in spell research");
			
			// AI players get a bonus based on the difficulty level
			if (player.getPlayerDescription ().getPlayerType () == PlayerType.AI)
			{
				researchAmount = (researchAmount * sd.getDifficultyLevel ().getAiSpellResearchMultiplier ()) / 100;
				log.debug ("AI Player ID " + player.getPlayerDescription ().getPlayerID () + " gets " + sd.getDifficultyLevel ().getAiSpellResearchMultiplier () + "% research multiplier, so bumped up to " +
					researchAmount + " RPs this turn in spell research");
			}

			// Put research points towards current spell
			if (researchAmount > 0)
			{
				final SpellResearchStatus spellBeingResarched = getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), priv.getSpellIDBeingResearched ());

				// Put some research towards it
				if (spellBeingResarched.getRemainingResearchCost () < researchAmount)
					spellBeingResarched.setRemainingResearchCost (0);
				else
					spellBeingResarched.setRemainingResearchCost (spellBeingResarched.getRemainingResearchCost () - researchAmount);

				// If finished, then grant spell and blank out research
				log.debug ("Player has " + spellBeingResarched.getRemainingResearchCost () + " RPs left before completing researching spell " + priv.getSpellIDBeingResearched ());
				if (spellBeingResarched.getRemainingResearchCost () == 0)
				{
					// Show on New Turn Messages on the client
					final NewTurnMessageSpell researchedSpell = new NewTurnMessageSpell ();
					researchedSpell.setMsgType (NewTurnMessageTypeID.RESEARCHED_SPELL);
					researchedSpell.setSpellID (priv.getSpellIDBeingResearched ());
					trans.getNewTurnMessage ().add (researchedSpell);

					// Update on server
					spellBeingResarched.setStatus (SpellResearchStatusID.AVAILABLE);
					priv.setSpellIDBeingResearched (null);

					// Pick another random spell to add to the 8 spells researchable now
					getServerSpellCalculations ().randomizeSpellsResearchableNow (priv.getSpellResearchStatus (), db);

					// And send this info to the client
					if (player.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
					{
						final FullSpellListMessage spellsMsg = new FullSpellListMessage ();
						spellsMsg.getSpellResearchStatus ().addAll (priv.getSpellResearchStatus ());
						player.getConnection ().sendMessageToClient (spellsMsg);
					}
				}
				else if (player.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
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
	}

	/**
	 * Resets the casting skill the wizard(s) have left to spend this turn back to their full skill
	 * 
	 * @param player Player who's overlandCastingSkillRemainingThisTurn to set
	 * @param wizardDetails Wizard who's overlandCastingSkillRemainingThisTurn to set
	 * @param players Players list
	 * @param db Lookup lists built over the XML database
     * @throws RecordNotFoundException If we can't find one of our picks in the database
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	final void resetCastingSkillRemainingThisTurnToFull (final PlayerServerDetails player, final KnownWizardDetails wizardDetails,
		final List<PlayerServerDetails> players, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();

		trans.setOverlandCastingSkillRemainingThisTurn (getResourceValueUtils ().calculateModifiedCastingSkill (priv.getResourceValue (),
			wizardDetails, players, priv.getFogOfWarMemory (), db, true));
	}

	/**
	 * Recalculates the amount of production of all types that we make each turn and sends the updated figures to the player(s)
	 * 
	 * @param onlyOnePlayerID If zero will calculate values in cities for all players; if non-zero will calculate values only for the specified player
	 * @param duringStartPhase If true does additional work around enforcing that we are producing enough, and progresses city construction, spell research & casting and so on
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting a reply message into XML
	 * @throws XMLStreamException If there is a problem writing a reply message to the XML stream
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void recalculateGlobalProductionValues (final int onlyOnePlayerID, final boolean duringStartPhase, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		// Make a list of all players to process, in case the session ends and wipes the list out part way through
		final List<PlayerServerDetails> playersToProcess = new ArrayList<PlayerServerDetails> ();
		
		for (final PlayerServerDetails player : mom.getPlayers ())
			if ((onlyOnePlayerID == 0) || (player.getPlayerDescription ().getPlayerID () == onlyOnePlayerID))
				playersToProcess.add (player);
		
		for (final PlayerServerDetails player : playersToProcess)
			if (mom.getPlayers ().size () > 0)
			{
				final KnownWizardDetails knownWizard = getKnownWizardUtils ().findKnownWizardDetails
					(mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), player.getPlayerDescription ().getPlayerID (), "recalculateGlobalProductionValues");
				
				// Don't calc production for the Rampaging Monsters player, or the routine spots that they have lots of units
				// that take a lot of mana to maintain, and no buildings generating any mana to support them, and kills them all off
				if (!CommonDatabaseConstants.WIZARD_ID_MONSTERS.equals (knownWizard.getWizardID ()))
				{
					log.debug ("recalculateGlobalProductionValues processing player ID " + player.getPlayerDescription ().getPlayerID () + " (" + player.getPlayerDescription ().getPlayerName () + ")");

					// Calculate base amounts
					recalculateAmountsPerTurn (player, mom);

					// If during the start phase, use these per turn production amounts as the amounts to add to the stored totals
					if (duringStartPhase)
					{
						// Search for a type of per-turn production that we're not producing enough of - currently this is only Rations
						// However (and to keep this consistent with how we handle insufficient stored Gold) there are too many interdependencies with what
						// may happen when we sell buildings, e.g. if we sell a Bank we don't only save its maintenance cost, the population then produces less gold
						// So the only safe way to do this is to recalculate ALL the productions, from scratch, every time we sell something!
						while (findInsufficientProductionAndSellSomething (player, knownWizard, EnforceProductionID.PER_TURN_AMOUNT_CANNOT_GO_BELOW_ZERO, false, mom))
							recalculateAmountsPerTurn (player, mom);

						// Now do the same for stored production
						while (findInsufficientProductionAndSellSomething (player, knownWizard, EnforceProductionID.STORED_AMOUNT_CANNOT_GO_BELOW_ZERO, true, mom))
							recalculateAmountsPerTurn (player, mom);

						// Per turn production amounts are now fine, so do the accumulation and effect calculations
						accumulateGlobalProductionValues (player, knownWizard, mom.getSessionDescription ().getSpellSetting (), mom.getServerDB ());
						
						// Research is zeroed out when Time Stop is in effect anyway, so no need to avoid calling this
						progressResearch (player, knownWizard, mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());
						
						resetCastingSkillRemainingThisTurnToFull (player, knownWizard, mom.getPlayers (), mom.getServerDB ());

						// Continue casting spells
						// If we actually completed casting one, then adjust calculated per turn production to take into account the extra mana being used
						// as long as it wasn't Spell of Mastery and the session is closed out already
						if (getSpellQueueing ().progressOverlandCasting (player, mom))
							if (mom.getPlayers ().size () > 0)
								recalculateAmountsPerTurn (player, mom);
					}
					else if (player.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)

						// No need to send values during start phase, since the start phase calls recalculateGlobalProductionValues () for a second time with DuringStartPhase set to False
						sendGlobalProductionValues (player, null, false);
				}
			}
	}

	/**
	 * @return Spell queueing methods
	 */
	public final SpellQueueing getSpellQueueing ()
	{
		return spellQueueing;
	}

	/**
	 * @param obj Spell queueing methods
	 */
	public final void setSpellQueueing (final SpellQueueing obj)
	{
		spellQueueing = obj;
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
	 * @return Spell utils
	 */
	public final SpellUtils getSpellUtils ()
	{
		return spellUtils;
	}

	/**
	 * @param utils Spell utils
	 */
	public final void setSpellUtils (final SpellUtils utils)
	{
		spellUtils = utils;
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
	 * @return expandUnitDetails method
	 */
	public final ExpandUnitDetails getExpandUnitDetails ()
	{
		return expandUnitDetails;
	}

	/**
	 * @param e expandUnitDetails method
	 */
	public final void setExpandUnitDetails (final ExpandUnitDetails e)
	{
		expandUnitDetails = e;
	}

	/**
	 * @return City production calculations
	 */
	public final CityProductionCalculations getCityProductionCalculations ()
	{
		return cityProductionCalculations;
	}

	/**
	 * @param calc City production calculations
	 */
	public final void setCityProductionCalculations (final CityProductionCalculations calc)
	{
		cityProductionCalculations = calc;
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
	 * @return Server-only spell calculations
	 */
	public final ServerSpellCalculations getServerSpellCalculations ()
	{
		return serverSpellCalculations;
	}

	/**
	 * @param calc Server-only spell calculations
	 */
	public final void setServerSpellCalculations (final ServerSpellCalculations calc)
	{
		serverSpellCalculations = calc;
	}

	/**
	 * @return Factory for creating resource consumers
	 */
	public final MomResourceConsumerFactory getMomResourceConsumerFactory ()
	{
		return momResourceConsumerFactory;
	}

	/**
	 * @param factory Factory for creating resource consumers
	 */
	public final void setMomResourceConsumerFactory (final MomResourceConsumerFactory factory)
	{
		momResourceConsumerFactory = factory;
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
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param s MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils s)
	{
		memoryMaintainedSpellUtils = s;
	}

	/**
	 * @return Methods for working with wizardIDs
	 */
	public final PlayerKnowledgeUtils getPlayerKnowledgeUtils ()
	{
		return playerKnowledgeUtils;
	}

	/**
	 * @param k Methods for working with wizardIDs
	 */
	public final void setPlayerKnowledgeUtils (final PlayerKnowledgeUtils k)
	{
		playerKnowledgeUtils = k;
	}

	/**
	 * @return Methods for finding KnownWizardDetails from the list
	 */
	public final KnownWizardUtils getKnownWizardUtils ()
	{
		return knownWizardUtils;
	}

	/**
	 * @param k Methods for finding KnownWizardDetails from the list
	 */
	public final void setKnownWizardUtils (final KnownWizardUtils k)
	{
		knownWizardUtils = k;
	}
}