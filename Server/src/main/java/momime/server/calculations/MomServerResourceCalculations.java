package momime.server.calculations;

import java.util.List;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import momime.common.MomException;
import momime.common.calculations.CalculateCityProductionResult;
import momime.common.calculations.MomCityCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.SpellUpkeep;
import momime.common.database.v0_9_4.UnitUpkeep;
import momime.common.messages.PlayerPickUtils;
import momime.common.messages.ResourceValueUtils;
import momime.common.messages.UnitUtils;
import momime.common.messages.servertoclient.v0_9_4.UpdateGlobalEconomyMessage;
import momime.common.messages.v0_9_4.FogOfWarMemory;
import momime.common.messages.v0_9_4.MemoryMaintainedSpell;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.v0_9_4.MomSessionDescription;
import momime.common.messages.v0_9_4.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.v0_9_4.OverlandMapCityData;
import momime.common.messages.v0_9_4.OverlandMapCoordinates;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.server.database.ServerDatabaseLookup;
import momime.server.database.v0_9_4.Plane;
import momime.server.database.v0_9_4.Spell;
import momime.server.database.v0_9_4.Unit;
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
		final FogOfWarMemory trueMap, final MomSessionDescription sd, final ServerDatabaseLookup db, final Logger debugLogger)
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
		for (final Plane plane : db.getPlanes ())
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
		for (final Plane plane : db.getPlanes ())
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
		final FogOfWarMemory trueMap, final MomSessionDescription sd, final ServerDatabaseLookup db, final Logger debugLogger)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		debugLogger.exiting (MomServerResourceCalculations.class.getName (), "recalculateGlobalProductionValues",
			new String [] {new Integer (onlyOnePlayerID).toString (), new Boolean (duringStartPhase).toString ()});

		for (final PlayerServerDetails player : players)
			if ((onlyOnePlayerID == 0) || (player.getPlayerDescription ().getPlayerID () == onlyOnePlayerID))
			{
				// Calculate base amounts
				recalculateAmountsPerTurn (player, players, trueMap, sd, db, debugLogger);

				// If during the start phase, use these per turn production amounts as the amounts to add to the stored totals
				if (duringStartPhase)
				{
					throw new UnsupportedOperationException ("recalculateGlobalProductionValues with duringStartPhase = true not yet implemented");
				}
				else if (player.getPlayerDescription ().isHuman ())
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
