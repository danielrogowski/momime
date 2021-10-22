package momime.server.process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.calculations.CombatMoveType;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.FogOfWarSetting;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.messages.CombatMapSize;
import momime.common.messages.ConfusionEffect;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.DamageCalculationConfusionData;
import momime.common.messages.servertoclient.MoveUnitInCombatReason;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.SpellCastType;
import momime.common.utils.TargetSpellResult;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.calculations.AttackDamage;
import momime.server.calculations.DamageCalculator;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.utils.UnitServerUtils;

/**
 * Deals with any processing at the end of one player's turn in combat (after none of their units have any moves left)
 */
public final class CombatEndTurnImpl implements CombatEndTurn
{
	/** Move types which represent moving (rather than being blocked, or initiating some kind of attack) */
	private final static List<CombatMoveType> MOVE_TYPES = Arrays.asList (CombatMoveType.MOVE, CombatMoveType.TELEPORT);
	
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
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** Combat processing */
	private CombatProcessing combatProcessing;
	
	/** Memory CAE utils */
	private MemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;

	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;

	/** Resource calculations */
	private ServerResourceCalculations serverResourceCalculations;
	
	/**
	 * Makes any rolls necessary at the start of either player's combat turn, i.e. immediately before the defender gets a turn.
	 * 
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param combatLocation The location the combat is taking place
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final void combatBeforeEitherTurn (final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final MapCoordinates3DEx combatLocation,
		final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		// Look for units with confusion cast on them
		// Map is from unit URN to casting player ID
		final Map<Integer, Integer> unitsWithConfusion = mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ().stream ().filter
			(s -> CommonDatabaseConstants.UNIT_SKILL_ID_CONFUSION.equals (s.getUnitSkillID ())).collect (Collectors.toMap
			(s -> s.getUnitURN (), s -> s.getCastingPlayerID ()));

		if (unitsWithConfusion.size () > 0)
			for (final MemoryUnit thisUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
				if ((combatLocation.equals (thisUnit.getCombatLocation ())) && (thisUnit.getCombatPosition () != null) &&
					(thisUnit.getCombatSide () != null) && (thisUnit.getCombatHeading () != null) && (thisUnit.getStatus () == UnitStatusID.ALIVE))
				{
					if (!unitsWithConfusion.containsKey (thisUnit.getUnitURN ()))
						thisUnit.setConfusionEffect (null);
					else
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
						
						// Move randomly
						if (effect == ConfusionEffect.MOVE_RANDOMLY)
						{
							// Find how much movement they have
							final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (thisUnit, null, null, null,
								mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
							
							if (!xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_WEB))
							{
								thisUnit.setDoubleCombatMovesLeft (2 * xu.getMovementSpeed ());
							
								// Pick a random direction
								final CombatMapSize combatMapSize = mom.getSessionDescription ().getCombatMapSize ();
								final int d = getRandomUtils ().nextInt (getCoordinateSystemUtils ().getMaxDirection (combatMapSize.getCoordinateSystemType ())) + 1;
								
								// Walk until run out of movement or hit something impassable
								final ServerGridCellEx tc = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
									(thisUnit.getCombatLocation ().getZ ()).getRow ().get (thisUnit.getCombatLocation ().getY ()).getCell ().get (thisUnit.getCombatLocation ().getX ());

								boolean keepGoing = true;
								while (keepGoing)
								{
									final int [] [] movementDirections = new int [combatMapSize.getHeight ()] [combatMapSize.getWidth ()];
									final CombatMoveType [] [] movementTypes = new CombatMoveType [combatMapSize.getHeight ()] [combatMapSize.getWidth ()];
									final int [] [] doubleMovementDistances = new int [combatMapSize.getHeight ()] [combatMapSize.getWidth ()];
									
									getUnitCalculations ().calculateCombatMovementDistances (doubleMovementDistances, movementDirections, movementTypes, xu,
										mom.getGeneralServerKnowledge ().getTrueMap (), tc.getCombatMap (), combatMapSize, mom.getPlayers (), mom.getServerDB ());
									
									// Check the intended cell
									final MapCoordinates2DEx coords = new MapCoordinates2DEx ((MapCoordinates2DEx) thisUnit.getCombatPosition ());
									if (!getCoordinateSystemUtils ().move2DCoordinates (combatMapSize, coords, d))
										keepGoing = false;	// Ran off edge of map
									else
									{
										final CombatMoveType moveType = movementTypes [coords.getY ()] [coords.getX ()];
										if (!MOVE_TYPES.contains (moveType))
											keepGoing = false;	// Hit something impassable, or would attack an enemy unit
										else
										{
											getCombatProcessing ().okToMoveUnitInCombat (xu, coords, MoveUnitInCombatReason.CONFUSION,
												movementDirections, movementTypes, mom);
											keepGoing = (thisUnit.getDoubleCombatMovesLeft () > 0);
										}
									}
								}
							}
						}
					}
				}
	}
	
	/**
	 * Deals with any processing at the start of one player's turn in combat, before their movement is initialized.
	 * 
	 * @param combatLocation The location the combat is taking place
	 * @param playerID Which player is about to have their combat turn
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return List of units frozen in terror who will not get any movement allocation this turn
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final List<Integer> startCombatTurn (final MapCoordinates3DEx combatLocation, final int playerID,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		// Work out the other player in combat
		final PlayerServerDetails castingPlayer = (playerID == attackingPlayer.getPlayerDescription ().getPlayerID ()) ? defendingPlayer : attackingPlayer;
		final MomPersistentPlayerPrivateKnowledge castingPlayerPriv = (MomPersistentPlayerPrivateKnowledge) castingPlayer.getPersistentPlayerPrivateKnowledge ();
		
		// Does opposing player have terror cast on this combat?
		final Spell terrorDef = mom.getServerDB ().findSpell (CommonDatabaseConstants.SPELL_ID_TERROR, "startCombatTurn");
		final String combatAreaEffectID = terrorDef.getSpellHasCombatEffect ().get (0);
		
		final List<ExpandedUnitDetails> unitsToRoll = new ArrayList<ExpandedUnitDetails> ();
		final List<MemoryUnit> defenders = new ArrayList<MemoryUnit> ();
		
		if (getMemoryCombatAreaEffectUtils ().findCombatAreaEffect (mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), combatLocation,
			combatAreaEffectID, castingPlayer.getPlayerDescription ().getPlayerID ()) != null)
			
			for (final MemoryUnit thisUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
				if ((combatLocation.equals (thisUnit.getCombatLocation ())) && (thisUnit.getCombatPosition () != null) &&
					(thisUnit.getCombatSide () != null) && (thisUnit.getCombatHeading () != null) && (thisUnit.getStatus () == UnitStatusID.ALIVE))
				{
					final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (thisUnit, null, null, terrorDef.getSpellRealm (),
						mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
					
					// attackSpellDamageResolutionTypeID = R on Terror spell def just to make the resistance check in isUnitValidTargetForSpell take effect
					if ((xu.getControllingPlayerID () == playerID) && (getMemoryMaintainedSpellUtils ().isUnitValidTargetForSpell
						(terrorDef, SpellBookSectionID.ATTACK_SPELLS, combatLocation, castingPlayer.getPlayerDescription ().getPlayerID (),
							null, null, xu, false, mom.getGeneralServerKnowledge ().getTrueMap (), castingPlayerPriv.getFogOfWar (),
							mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET))
						
						unitsToRoll.add (xu);
				}
		
		// Only bother to send the damage calculation header if there's at least one unit that has to make a roll
		final List<Integer> terrifiedUnitURNs = new ArrayList<Integer> ();
		if (unitsToRoll.size () > 0)
		{
			getDamageCalculator ().sendDamageHeader (null, defenders, attackingPlayer, defendingPlayer, null, terrorDef, castingPlayer);
			final AttackDamage attackDamage = getDamageCalculator ().attackFromSpell
				(terrorDef, null, castingPlayer, null, attackingPlayer, defendingPlayer, mom.getServerDB (), SpellCastType.COMBAT);
			
			for (final ExpandedUnitDetails xu : unitsToRoll)
				if (getDamageCalculator ().calculateResistanceRoll (xu, attackingPlayer, defendingPlayer, attackDamage, false))
					terrifiedUnitURNs.add (xu.getUnitURN ());
		}
		
		// Does opposing player have mana leak cast on this combat?
		if (getMemoryCombatAreaEffectUtils ().findCombatAreaEffect (mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), combatLocation,
			CommonDatabaseConstants.COMBAT_AREA_EFFECT_ID_MANA_LEAK, castingPlayer.getPlayerDescription ().getPlayerID ()) != null)
		{
			for (final MemoryUnit thisUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
				if ((combatLocation.equals (thisUnit.getCombatLocation ())) && (thisUnit.getCombatPosition () != null) &&
					(thisUnit.getCombatSide () != null) && (thisUnit.getCombatHeading () != null) && (thisUnit.getStatus () == UnitStatusID.ALIVE))
				{
					final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (thisUnit, null, null, terrorDef.getSpellRealm (),
						mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
					if (xu.getControllingPlayerID () == playerID)
					{
						// Units with their own MP pool
						if (thisUnit.getManaRemaining () > 0)
						{
							thisUnit.setManaRemaining (Math.max (0, thisUnit.getManaRemaining () - 5));
							getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (thisUnit, mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
								mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting (), null);
						}
						
						// Units with magical ranged attacks
						else if ((thisUnit.getAmmoRemaining () > 0) && (xu.getRangedAttackType ().getMagicRealmID () != null))
						{
							thisUnit.setAmmoRemaining (thisUnit.getAmmoRemaining () - 1);
							getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (thisUnit, mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
								mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting (), null);
						}
					}
				}
			
			// Enemy wizard
			final PlayerServerDetails thisPlayer = (playerID == attackingPlayer.getPlayerDescription ().getPlayerID ()) ? attackingPlayer : defendingPlayer;
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) thisPlayer.getPersistentPlayerPrivateKnowledge ();
			final int mana = getResourceValueUtils ().findAmountStoredForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
			if (mana > 0)
			{
				final int subtractMana = Math.min (5, mana);
				getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, -subtractMana);
				
				// We have to re-send the remaining casting skill with the message, since the client doesn't record it, and by sending
				// it the client can correctly work out if the reduced MP is below the remaining casting skill and means the player can now cast less
				if (thisPlayer.getPlayerDescription ().isHuman ())
				{
					final ServerGridCellEx gc = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
						(combatLocation.getZ ()).getRow ().get (combatLocation.getY ()).getCell ().get (combatLocation.getX ());

					Integer sendSkillValue = null;
					if (thisPlayer == defendingPlayer)
					{
						if (gc.getCombatDefenderCastingSkillRemaining () != null)
							sendSkillValue = gc.getCombatDefenderCastingSkillRemaining ();
					}
					else if (thisPlayer == attackingPlayer)
					{
						if (gc.getCombatAttackerCastingSkillRemaining () != null)
							sendSkillValue = gc.getCombatAttackerCastingSkillRemaining ();
					}
					
					getServerResourceCalculations ().sendGlobalProductionValues (thisPlayer, sendSkillValue, false);
				}
			}
		}
		
		return terrifiedUnitURNs;
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
	public final void combatEndTurn (final MapCoordinates3DEx combatLocation, final int playerID,
		final List<PlayerServerDetails> players, final FogOfWarMemory mem, final CommonDatabase db, final FogOfWarSetting fogOfWarSettings)
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
		
		// We are only regenerating - there is no animation for it - so just pass nulls for attackingPlayer + defendingPlayer
		if (healedUnits.size () > 0)
			getFogOfWarMidTurnChanges ().sendDamageToClients (null, null, null,
				healedUnits, null, null, null, null, null, players, mem.getMap (), db, fogOfWarSettings);
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

	/**
	 * @return Coordinate system utils
	 */
	public final CoordinateSystemUtils getCoordinateSystemUtils ()
	{
		return coordinateSystemUtils;
	}

	/**
	 * @param utils Coordinate system utils
	 */
	public final void setCoordinateSystemUtils (final CoordinateSystemUtils utils)
	{
		coordinateSystemUtils = utils;
	}

	/**
	 * @return Unit calculations
	 */
	public final UnitCalculations getUnitCalculations ()
	{
		return unitCalculations;
	}

	/**
	 * @param calc Unit calculations
	 */
	public final void setUnitCalculations (final UnitCalculations calc)
	{
		unitCalculations = calc;
	}

	/**
	 * @return Combat processing
	 */
	public final CombatProcessing getCombatProcessing ()
	{
		return combatProcessing;
	}

	/**
	 * @param proc Combat processing
	 */
	public final void setCombatProcessing (final CombatProcessing proc)
	{
		combatProcessing = proc;
	}

	/**
	 * @return Memory CAE utils
	 */
	public final MemoryCombatAreaEffectUtils getMemoryCombatAreaEffectUtils ()
	{
		return memoryCombatAreaEffectUtils;
	}

	/**
	 * @param utils Memory CAE utils
	 */
	public final void setMemoryCombatAreaEffectUtils (final MemoryCombatAreaEffectUtils utils)
	{
		memoryCombatAreaEffectUtils = utils;
	}

	/**
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param spellUtils MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils spellUtils)
	{
		memoryMaintainedSpellUtils = spellUtils;
	}

	/**
	 * @return Resource value utils
	 */
	public final ResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param util Resource value utils
	 */
	public final void setResourceValueUtils (final ResourceValueUtils util)
	{
		resourceValueUtils = util;
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
}