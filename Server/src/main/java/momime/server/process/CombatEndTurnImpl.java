package momime.server.process;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.sessionbase.PlayerType;
import com.ndg.utils.random.RandomUtils;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.database.AttackSpellTargetID;
import momime.common.database.CombatAreaEffect;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageResolutionTypeID;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.messages.CombatMapSize;
import momime.common.messages.ConfusionEffect;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomCombatTile;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.DamageCalculationConfusionData;
import momime.common.messages.servertoclient.MoveUnitInCombatReason;
import momime.common.movement.CombatMovementType;
import momime.common.movement.UnitMovement;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.SpellCastType;
import momime.common.utils.TargetSpellResult;
import momime.server.MomSessionVariables;
import momime.server.calculations.AttackDamage;
import momime.server.calculations.DamageCalculator;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.knowledge.CombatDetails;
import momime.server.utils.UnitServerUtils;

/**
 * Deals with any processing at the end of one player's turn in combat (after none of their units have any moves left)
 */
public final class CombatEndTurnImpl implements CombatEndTurn
{
	/** Move types which represent moving (rather than being blocked, or initiating some kind of attack) */
	private final static List<CombatMovementType> MOVE_TYPES = Arrays.asList (CombatMovementType.MOVE, CombatMovementType.TELEPORT);
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
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
	
	/** Damage processor */
	private DamageProcessor damageProcessor;
	
	/** Methods dealing with unit movement */
	private UnitMovement unitMovement;
	
	/**
	 * Makes any rolls necessary at the start of either player's combat turn, i.e. immediately before the defender gets a turn.
	 * 
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param combatDetails Details about the combat taking place
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void combatBeforeEitherTurn (final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final CombatDetails combatDetails,
		final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		// Look for units with confusion cast on them
		// Map is from unit URN to casting player ID
		final Map<Integer, Integer> unitsWithConfusion = mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ().stream ().filter
			(s -> CommonDatabaseConstants.UNIT_SKILL_ID_CONFUSION.equals (s.getUnitSkillID ())).collect (Collectors.toMap
			(s -> s.getUnitURN (), s -> s.getCastingPlayerID ()));

		if (unitsWithConfusion.size () > 0)
			for (final MemoryUnit thisUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
				if ((combatDetails.getCombatLocation ().equals (thisUnit.getCombatLocation ())) && (thisUnit.getCombatPosition () != null) &&
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
							final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (thisUnit, null, null, null,
								mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
							
							if (!xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_WEB))
							{
								thisUnit.setDoubleCombatMovesLeft (2 * xu.getMovementSpeed ());
							
								// Pick a random direction
								final CombatMapSize combatMapSize = mom.getSessionDescription ().getCombatMapSize ();
								final int d = getRandomUtils ().nextInt (getCoordinateSystemUtils ().getMaxDirection (combatMapSize.getCoordinateSystemType ())) + 1;
								
								// Walk until run out of movement or hit something impassable
								boolean keepGoing = true;
								while (keepGoing)
								{
									final int [] [] movementDirections = new int [combatMapSize.getHeight ()] [combatMapSize.getWidth ()];
									final CombatMovementType [] [] movementTypes = new CombatMovementType [combatMapSize.getHeight ()] [combatMapSize.getWidth ()];
									final int [] [] doubleMovementDistances = new int [combatMapSize.getHeight ()] [combatMapSize.getWidth ()];
									
									getUnitMovement ().calculateCombatMovementDistances (doubleMovementDistances, movementDirections, movementTypes, xu,
										mom.getGeneralServerKnowledge ().getTrueMap (), combatDetails.getCombatMap (), combatMapSize, mom.getPlayers (), mom.getServerDB ());
									
									// Check the intended cell
									final MapCoordinates2DEx coords = new MapCoordinates2DEx ((MapCoordinates2DEx) thisUnit.getCombatPosition ());
									if (!getCoordinateSystemUtils ().move2DCoordinates (combatMapSize, coords, d))
										keepGoing = false;	// Ran off edge of map
									else
									{
										final CombatMovementType moveType = movementTypes [coords.getY ()] [coords.getX ()];
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
		
		// Accumulate damage to the city
		final List<String> cityTiles = mom.getServerDB ().getCombatTileType ().stream ().filter
			(t -> (t.isInsideCity () != null) && (t.isInsideCity ())).map (t -> t.getCombatTileTypeID ()).collect (Collectors.toList ());

		for (final MemoryUnit thisUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
			if ((combatDetails.getCombatLocation ().equals (thisUnit.getCombatLocation ())) && (thisUnit.getCombatPosition () != null) &&
				(thisUnit.getCombatSide () != null) && (thisUnit.getCombatHeading () != null) && (thisUnit.getStatus () == UnitStatusID.ALIVE) &&
				(thisUnit.getOwningPlayerID () == attackingPlayer.getPlayerDescription ().getPlayerID ()) &&
				(!mom.getServerDB ().getUnitsThatMoveThroughOtherUnits ().contains (thisUnit.getUnitID ())))		// Ignore vortexes, they have their own way of increasing dmg
			{
				// Are they in the 4x4 town area?  Get the specific tile where the unit is
				final MomCombatTile combatTile = combatDetails.getCombatMap ().getRow ().get (thisUnit.getCombatPosition ().getY ()).getCell ().get (thisUnit.getCombatPosition ().getX ());
				
				if (combatTile.getTileLayer ().stream ().anyMatch (l -> cityTiles.contains (l.getCombatTileTypeID ())))
					combatDetails.setCollateralAccumulator (combatDetails.getCollateralAccumulator () + 1);
			}
	}
	
	/**
	 * Deals with any processing at the start of one player's turn in combat, before their movement is initialized.
	 * 
	 * @param combatDetails Details about the combat taking place
	 * @param playerID Which player is about to have their combat turn
	 * @param attackingPlayer The player who attacked to initiate the combat - not necessarily the owner of the 'attacker' unit 
	 * @param defendingPlayer Player who was attacked to initiate the combat - not necessarily the owner of the 'defender' unit
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return List of units frozen in terror who will not get any movement allocation this turn; will return null (rather than an empty list) if the combat ends
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final List<Integer> startCombatTurn (final CombatDetails combatDetails, final int playerID,
		final PlayerServerDetails attackingPlayer, final PlayerServerDetails defendingPlayer, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		final CombatMapSize combatMapSize = mom.getSessionDescription ().getCombatMapSize ();

		// Do we have any vortexes?  If so then move their 3 random moves.  Then they get their 1 movement controlled by the player during their regular turn.
		final List<ExpandedUnitDetails> vortexes = new ArrayList<ExpandedUnitDetails> ();
		
		for (final MemoryUnit thisUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
			if ((combatDetails.getCombatLocation ().equals (thisUnit.getCombatLocation ())) && (thisUnit.getCombatPosition () != null) &&
				(thisUnit.getCombatSide () != null) && (thisUnit.getCombatHeading () != null) && (thisUnit.getStatus () == UnitStatusID.ALIVE) &&
				(mom.getServerDB ().getUnitsThatMoveThroughOtherUnits ().contains (thisUnit.getUnitID ())))
			{
				final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (thisUnit, null, null, null,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
				if ((xu.getControllingPlayerID () == playerID) && (!xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_SKILL_ID_WEB)))
					vortexes.add (xu);
			}
		
		// Don't process over the main unit list in case earlier vortexes kill off some units and modify the list
		boolean combatEnded = false;
		for (final ExpandedUnitDetails xu : vortexes)
			if (!combatEnded)
			{
				xu.setDoubleCombatMovesLeft (6);
				
				int vortexMoveNumber = 0;
				while ((!combatEnded) && (vortexMoveNumber < 3))
				{
					// Only ever deviates +/- 90 degres from the last direction the vortex moved (whether a random or player chosen move).
					// okToMoveUnitInCombat will record the direction chosen, so don't need to track it here.
					final int d;
					if (!combatDetails.getLastCombatMoveDirection ().containsKey (xu.getUnitURN ()))
						d = getRandomUtils ().nextInt (getCoordinateSystemUtils ().getMaxDirection (combatMapSize.getCoordinateSystemType ())) + 1;
					else
						d = getCoordinateSystemUtils ().normalizeDirection (combatMapSize.getCoordinateSystemType (),
							combatDetails.getLastCombatMoveDirection ().get (xu.getUnitURN ()) - 2 + getRandomUtils ().nextInt (5));
	
					// Nothing is impassable, but might bang into the edge of the map
					final int [] [] movementDirections = new int [combatMapSize.getHeight ()] [combatMapSize.getWidth ()];
					final CombatMovementType [] [] movementTypes = new CombatMovementType [combatMapSize.getHeight ()] [combatMapSize.getWidth ()];
					final int [] [] doubleMovementDistances = new int [combatMapSize.getHeight ()] [combatMapSize.getWidth ()];
					
					getUnitMovement ().calculateCombatMovementDistances (doubleMovementDistances, movementDirections, movementTypes, xu,
						mom.getGeneralServerKnowledge ().getTrueMap (), combatDetails.getCombatMap (), combatMapSize, mom.getPlayers (), mom.getServerDB ());
					
					// Check the intended cell
					final MapCoordinates2DEx coords = new MapCoordinates2DEx (xu.getCombatPosition ());
					if (getCoordinateSystemUtils ().move2DCoordinates (combatMapSize, coords, d))
					{
						final CombatMovementType moveType = movementTypes [coords.getY ()] [coords.getX ()];
						if (MOVE_TYPES.contains (moveType))
							combatEnded = getCombatProcessing ().okToMoveUnitInCombat (xu, coords, MoveUnitInCombatReason.MAGIC_VORTEX,
								movementDirections, movementTypes, mom);
					}
					
					vortexMoveNumber++;
				}
			}

		// Work out the other player in combat
		final PlayerServerDetails castingPlayer = (playerID == attackingPlayer.getPlayerDescription ().getPlayerID ()) ? defendingPlayer : attackingPlayer;
		final MomPersistentPlayerPrivateKnowledge castingPlayerPriv = (MomPersistentPlayerPrivateKnowledge) castingPlayer.getPersistentPlayerPrivateKnowledge ();
		
		// Does opposing player have any CAEs cast that do some kind of damage each turn? (wrack, call lightning, terror)
		Spell terrorSpell = null;
		if (!combatEnded)
		{
			final List<Spell> damagingCAEs = mom.getServerDB ().getSpell ().stream ().filter
				(s -> (s.getAttackSpellDamageResolutionTypeID () != null) && (s.getSpellHasCombatEffect ().size () == 1)).collect (Collectors.toList ());
			
			for (final Spell damagingCAE : damagingCAEs)
				if (!combatEnded)
				{
					final String combatAreaEffectID = damagingCAE.getSpellHasCombatEffect ().get (0);
					
					if (getMemoryCombatAreaEffectUtils ().findCombatAreaEffect (mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), combatDetails.getCombatLocation (),
						combatAreaEffectID, castingPlayer.getPlayerDescription ().getPlayerID ()) != null)
					{
						// Terror has special handling below
						if (damagingCAE.getAttackSpellDamageResolutionTypeID () == DamageResolutionTypeID.TERROR)
							terrorSpell = damagingCAE;
						else
						{
							final List<MemoryUnit> enemyUnits = new ArrayList<MemoryUnit> ();
							
							for (final MemoryUnit thisUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
								if ((combatDetails.getCombatLocation ().equals (thisUnit.getCombatLocation ())) && (thisUnit.getCombatPosition () != null) &&
									(thisUnit.getCombatSide () != null) && (thisUnit.getCombatHeading () != null) && (thisUnit.getStatus () == UnitStatusID.ALIVE) &&
									(thisUnit.getOwningPlayerID () == playerID))
									
									enemyUnits.add (thisUnit);
							
							if (enemyUnits.size () > 0)
							{
								// Does it hit all units or pick one randomgly?
								if (damagingCAE.getAttackSpellCombatTarget () == AttackSpellTargetID.SINGLE_UNIT)
								{
									// How many times does it attack?
									final CombatAreaEffect caeDef = mom.getServerDB ().findCombatAreaEffect (combatAreaEffectID, "startCombatTurn");
									final int attacksPerRoundMinimum = (caeDef.getAttacksPerRoundMinimum () == null) ? 1 : caeDef.getAttacksPerRoundMinimum ();
									final int attacksPerRoundMaximum = (caeDef.getAttacksPerRoundMaximum () == null) ? 1 : caeDef.getAttacksPerRoundMaximum ();
									final int targetCount = attacksPerRoundMinimum + getRandomUtils ().nextInt (attacksPerRoundMaximum - attacksPerRoundMinimum + 1);
									
									final List<MemoryUnit> randomTargets = new ArrayList<MemoryUnit> ();
									
									for (int n = 0; n < targetCount; n++)
										randomTargets.add (enemyUnits.get (getRandomUtils ().nextInt (enemyUnits.size ())));
									
									enemyUnits.clear ();
									enemyUnits.addAll (randomTargets);
								}
								
								// Now see which ones are valid targets (not immune to the damage)								
								final List<ResolveAttackTarget> targetUnits = new ArrayList<ResolveAttackTarget> ();
								for (final MemoryUnit thisUnit : enemyUnits)
								{
									final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (thisUnit, null, null, damagingCAE.getSpellRealm (),
										mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
									
									if ((xu.getControllingPlayerID () == playerID) && (getMemoryMaintainedSpellUtils ().isUnitValidTargetForSpell
										(damagingCAE, SpellBookSectionID.ATTACK_SPELLS, combatDetails.getCombatLocation (), combatDetails.getCombatMap (),
											castingPlayer.getPlayerDescription ().getPlayerID (),
											null, null, xu, false, mom.getGeneralServerKnowledge ().getTrueMap (), castingPlayerPriv.getFogOfWar (),
											mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET))
										
										targetUnits.add (new ResolveAttackTarget (thisUnit));
								}
	
								if (targetUnits.size () > 0)
									combatEnded = getDamageProcessor ().resolveAttack (null, targetUnits,
										attackingPlayer, defendingPlayer, null, null, null, null, null, damagingCAE, null, castingPlayer, combatDetails.getCombatLocation (), false, mom).isCombatEnded ();
							}
						}
					}
			}
		}

		final List<Integer> terrifiedUnitURNs;
		if (combatEnded)
			terrifiedUnitURNs = null;
		else
		{
			terrifiedUnitURNs = new ArrayList<Integer> ();
			if (terrorSpell != null)
			{
				final List<ExpandedUnitDetails> unitsToRoll = new ArrayList<ExpandedUnitDetails> ();
				final List<MemoryUnit> defenders = new ArrayList<MemoryUnit> ();
				
				for (final MemoryUnit thisUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
					if ((combatDetails.getCombatLocation ().equals (thisUnit.getCombatLocation ())) && (thisUnit.getCombatPosition () != null) &&
						(thisUnit.getCombatSide () != null) && (thisUnit.getCombatHeading () != null) && (thisUnit.getStatus () == UnitStatusID.ALIVE))
					{
						final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (thisUnit, null, null, terrorSpell.getSpellRealm (),
							mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
						
						if ((xu.getControllingPlayerID () == playerID) && (getMemoryMaintainedSpellUtils ().isUnitValidTargetForSpell
							(terrorSpell, SpellBookSectionID.ATTACK_SPELLS, combatDetails.getCombatLocation (), combatDetails.getCombatMap (),
								castingPlayer.getPlayerDescription ().getPlayerID (),
								null, null, xu, false, mom.getGeneralServerKnowledge ().getTrueMap (), castingPlayerPriv.getFogOfWar (),
								mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET))
							
							unitsToRoll.add (xu);
					}
				
				// Only bother to send the damage calculation header if there's at least one unit that has to make a roll
				if (unitsToRoll.size () > 0)
				{
					getDamageCalculator ().sendDamageHeader (null, defenders, false, attackingPlayer, defendingPlayer, null, null, terrorSpell, castingPlayer);
					final AttackDamage attackDamage = getDamageCalculator ().attackFromSpell
						(terrorSpell, null, castingPlayer, null, attackingPlayer, defendingPlayer, null, mom.getServerDB (), SpellCastType.COMBAT, false);
					
					for (final ExpandedUnitDetails xu : unitsToRoll)
						if (getDamageCalculator ().calculateResistanceRoll (xu, attackingPlayer, defendingPlayer, attackDamage, false))
							terrifiedUnitURNs.add (xu.getUnitURN ());
				}
			}
			
			// Does opposing player have mana leak cast on this combat?
			if (getMemoryCombatAreaEffectUtils ().findCombatAreaEffect (mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), combatDetails.getCombatLocation (),
				CommonDatabaseConstants.COMBAT_AREA_EFFECT_ID_MANA_LEAK, castingPlayer.getPlayerDescription ().getPlayerID ()) != null)
			{
				for (final MemoryUnit thisUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
					if ((combatDetails.getCombatLocation ().equals (thisUnit.getCombatLocation ())) && (thisUnit.getCombatPosition () != null) &&
						(thisUnit.getCombatSide () != null) && (thisUnit.getCombatHeading () != null) && (thisUnit.getStatus () == UnitStatusID.ALIVE))
					{
						final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (thisUnit, null, null, null,
							mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
						if (xu.getControllingPlayerID () == playerID)
						{
							// Units with their own MP pool
							if (thisUnit.getManaRemaining () > 0)
							{
								thisUnit.setManaRemaining (Math.max (0, thisUnit.getManaRemaining () - 5));
								getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (thisUnit, mom, null);
							}
							
							// Units with magical ranged attacks
							else if ((thisUnit.getAmmoRemaining () > 0) && (xu.getRangedAttackType ().getMagicRealmID () != null))
							{
								thisUnit.setAmmoRemaining (thisUnit.getAmmoRemaining () - 1);
								getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (thisUnit, mom, null);
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
					if (thisPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
					{
						Integer sendSkillValue = null;
						if (thisPlayer == defendingPlayer)
							sendSkillValue = combatDetails.getDefenderCastingSkillRemaining ();
						else if (thisPlayer == attackingPlayer)
							sendSkillValue = combatDetails.getAttackerCastingSkillRemaining ();
						
						getServerResourceCalculations ().sendGlobalProductionValues (thisPlayer, sendSkillValue, false);
					}
				}
			}
		}
		
		return terrifiedUnitURNs;
	}
	
	/**
	 * Deals with any processing at the end of one player's turn in combat (after none of their units have any moves left) 
	 * 
	 * @param combatDetails Details about the combat taking place
	 * @param playerID Which player just finished their combat turn
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If an expected data item cannot be found
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final void combatEndTurn (final CombatDetails combatDetails, final int playerID, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		// Note we don't check the unit can normally heal damage (is not undead) because regeneration works even on undead
		final List<MemoryUnit> healedUnits = new ArrayList<MemoryUnit> ();

		for (final MemoryUnit thisUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
			if ((thisUnit.getOwningPlayerID () == playerID) && (combatDetails.getCombatLocation ().equals (thisUnit.getCombatLocation ())) && (thisUnit.getCombatPosition () != null) &&
				(thisUnit.getCombatSide () != null) && (thisUnit.getCombatHeading () != null) && (thisUnit.getStatus () == UnitStatusID.ALIVE) &&
				(thisUnit.getUnitDamage ().size () > 0))
			{
				final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (thisUnit, null, null, null,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
				
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
		{
			final List<ResolveAttackTarget> unitWrappers = healedUnits.stream ().map (u -> new ResolveAttackTarget (u)).collect (Collectors.toList ());
			
			getFogOfWarMidTurnChanges ().sendDamageToClients (null, null, null, unitWrappers, null, null, null, null, false, mom);
		}
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

	/**
	 * @return Damage processor
	 */
	public final DamageProcessor getDamageProcessor ()
	{
		return damageProcessor;
	}

	/**
	 * @param proc Damage processor
	 */
	public final void setDamageProcessor (final DamageProcessor proc)
	{
		damageProcessor = proc;
	}

	/**
	 * @return Methods dealing with unit movement
	 */
	public final UnitMovement getUnitMovement ()
	{
		return unitMovement;
	}

	/**
	 * @param u Methods dealing with unit movement
	 */
	public final void setUnitMovement (final UnitMovement u)
	{
		unitMovement = u;
	}
}