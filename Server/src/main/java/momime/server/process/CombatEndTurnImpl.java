package momime.server.process;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.FogOfWarSetting;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.ConfusionEffect;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.DamageCalculationConfusionData;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.UnitUtils;
import momime.server.calculations.DamageCalculator;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.utils.UnitServerUtils;

/**
 * Deals with any processing at the end of one player's turn in combat (after none of their units have any moves left)
 */
public final class CombatEndTurnImpl implements CombatEndTurn
{
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/** Damage calc */
	private DamageCalculator damageCalculator;
	
	/**
	 * Makes any rolls necessary at the start of a combat turn.  Note what this means by "combat turn" is different than what combatEndTurn below means.
	 * Here we mean before EITHER player has taken their turn, i.e. immediately before the defender gets a turn.
	 * 
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param combatLocation The location the combat is taking place
	 * @param trueMap True terrain, buildings, spells and so on as known only to the server
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final void combatStartTurn (final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer,
		final MapCoordinates3DEx combatLocation, final FogOfWarMemory trueMap)
		throws JAXBException, XMLStreamException
	{
		// Look for units with confusion cast on them
		// Map is from unit URN to casting player ID
		final Map<Integer, Integer> unitsWithConfusion = trueMap.getMaintainedSpell ().stream ().filter
			(s -> CommonDatabaseConstants.UNIT_SKILL_ID_CONFUSION.equals (s.getUnitSkillID ())).collect (Collectors.toMap
			(s -> s.getUnitURN (), s -> s.getCastingPlayerID ()));

		if (unitsWithConfusion.size () > 0)
			for (final MemoryUnit thisUnit : trueMap.getUnit ())
				if ((combatLocation.equals (thisUnit.getCombatLocation ())) && (thisUnit.getCombatPosition () != null) &&
					(thisUnit.getCombatSide () != null) && (thisUnit.getCombatHeading () != null) && (thisUnit.getStatus () == UnitStatusID.ALIVE) &&
					(unitsWithConfusion.containsKey (thisUnit.getUnitURN ())))
				{
					// Make random roll
					final ConfusionEffect effect = ConfusionEffect.values () [getRandomUtils ().nextInt (ConfusionEffect.values ().length)];
					thisUnit.setConfusionEffect (effect);
					
					// Inform players involved; this doubles up as the damage calculation message
					final DamageCalculationConfusionData msg = new DamageCalculationConfusionData ();
					msg.setUnitURN (thisUnit.getUnitURN ());
					msg.setConfusionEffect (effect);
					msg.setCastingPlayerID (unitsWithConfusion.get (thisUnit.getUnitURN ()));
					
					getDamageCalculator ().sendDamageCalculationMessage (attackingPlayer, defendingPlayer, msg);
				}
	}
	
	/**
	 * Deals with any processing at the end of one player's turn in combat (after none of their units have any moves left) 
	 * 
	 * @param combatLocation The location the combat is taking place
	 * @param playerID Which player just finished their combat turn
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @param fogOfWarSettings Fog of War settings from session description
	 * @throws RecordNotFoundException If an expected data item cannot be found
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final void combatEndTurn (final MapCoordinates3DEx combatLocation, final int playerID, final List<PlayerServerDetails> players,
		final FogOfWarMemory mem, final CommonDatabase db, final FogOfWarSetting fogOfWarSettings)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		// Note we don't check the unit can normally heal damage (is not undead) because regeneration works even on undead
		final List<MemoryUnit> healedUnits = new ArrayList<MemoryUnit> ();

		for (final MemoryUnit thisUnit : mem.getUnit ())
			if ((thisUnit.getOwningPlayerID () == playerID) && (combatLocation.equals (thisUnit.getCombatLocation ())) && (thisUnit.getCombatPosition () != null) &&
				(thisUnit.getCombatSide () != null) && (thisUnit.getCombatHeading () != null) && (thisUnit.getStatus () == UnitStatusID.ALIVE) &&
				(thisUnit.getUnitDamage ().size () > 0))
			{
				final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (thisUnit, null, null, null, players, mem, db);
				
				boolean regeneration = false;
				for (final String regenerationSkillID : CommonDatabaseConstants.UNIT_SKILL_IDS_REGENERATION)
					if (xu.hasModifiedSkill (regenerationSkillID))
						regeneration = true;
				
				if (regeneration)
				{
					getUnitServerUtils ().healDamage (thisUnit.getUnitDamage (), 1, false);
					healedUnits.add (thisUnit);
				}
			}
		
		if (healedUnits.size () > 0)
			getFogOfWarMidTurnChanges ().sendCombatDamageToClients (null, playerID, healedUnits, null, null, null, null, null, players, mem.getMap (), db, fogOfWarSettings);
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
	 * @return Damage calc
	 */
	public final DamageCalculator getDamageCalculator ()
	{
		return damageCalculator;
	}

	/**
	 * @param calc Damage calc
	 */
	public final void setDamageCalculator (final DamageCalculator calc)
	{
		damageCalculator = calc;
	}
}