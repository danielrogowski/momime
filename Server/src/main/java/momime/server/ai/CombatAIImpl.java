package momime.server.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.calculations.UnitCalculations;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.CombatMapSize;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.UnitStatusID;
import momime.common.messages.WizardState;
import momime.common.messages.servertoclient.MoveUnitInCombatReason;
import momime.common.movement.CombatMovementType;
import momime.common.movement.UnitMovement;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.process.CombatProcessing;
import momime.server.process.CombatStartAndEndImpl;

/**
 * AI for deciding what to do with units in combat
 * This is used for human players who put their units on 'auto' as well as actual AI players
 */
public final class CombatAIImpl implements CombatAI
{
	/** All types of attack the AI is interested in making in combat */
	private final static List<CombatMovementType> ALL_ATTACKS = Arrays.asList (CombatMovementType.MELEE_UNIT, CombatMovementType.MELEE_UNIT_AND_WALL,
		CombatMovementType.RANGED_UNIT, CombatMovementType.RANGED_UNIT_AND_WALL);
	
	/** Only ranged attacks */
	private final static List<CombatMovementType> RANGED_ATTACKS = Arrays.asList (CombatMovementType.RANGED_UNIT, CombatMovementType.RANGED_UNIT_AND_WALL);
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
	/** Combat processing */
	private CombatProcessing combatProcessing;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** AI decisions about spells */
	private SpellAI spellAI;
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** Methods dealing with unit movement */
	private UnitMovement unitMovement;
	
	/**
	 * @param combatLocation The location the combat is taking place at (may not necessarily be the location of the defending units, see where this is set in startCombat)
	 * @param currentPlayerID AI player whose turn we are taking
	 * @param players Players list
	 * @param mem True terrain, buildings, spells and so on as known only to the server
	 * @param db Lookup lists built over the XML database
	 * @return List of units this AI player needs to take actions for in combat
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	final List<MemoryUnit> listUnitsToMove (final MapCoordinates3DEx combatLocation, final int currentPlayerID,
		final List<PlayerServerDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException 
	{
		final List<MemoryUnit> unitsToMove = new ArrayList<MemoryUnit> ();
		for (final MemoryUnit tu : mem.getUnit ())
			if ((tu.getStatus () == UnitStatusID.ALIVE) &&
				(combatLocation.equals (tu.getCombatLocation ())) && (tu.getCombatPosition () != null) && (tu.getCombatHeading () != null) && (tu.getCombatSide () != null) &&
				(tu.getDoubleCombatMovesLeft () != null) && (tu.getDoubleCombatMovesLeft () > 0))
			{
				final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (tu, null, null, null, players, mem, db);
					
				if (xu.getControllingPlayerID () == currentPlayerID)
					unitsToMove.add (tu);
			}
		
		return unitsToMove;
	}
	
	/**
	 * 1 = Spellcasters with over 10 mana remaining (since they may cast spells beneficial to other units, e.g. Prayer)
	 * 2 = Anyone with a ranged attack & remaining ammo
	 * 3 = Anyone without the caster skill (i.e. melee units, or phys ranged units who have run out of ammo)
	 * 4 = Spellcasters with less than 10 mana remaining (if all mana used up, then we need to protect them, so move them last)
	 * 
	 * @param unit Unit to compare
	 * @return Combat AI sort value for the unit, i.e. units to move first in combat get a lower number
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	final int calculateUnitCombatAIOrder (final ExpandedUnitDetails unit) throws MomException
	{
		final int result;

		// Caster with MP remaining?
		if ((unit.getManaRemaining () >= 10) && (unit.canCastSpells ()))
			result = 1;
		
		// Ranged attack?
		else if (getUnitCalculations ().canMakeRangedAttack (unit))
			result = 2;
		
		// Caster skill?
		else if (!unit.canCastSpells ())
			result = 3;
		
		else
			result = 4;
		
		return result;
	}
	
	/**
	 * This makes the AI players kill e.g. enemy spellcasters first.
	 * 
	 * NB. The attacker input param isn't currently used, but passing it in here so in future this can be made more clever, for
	 * example units with First Strike should de-rate units with Negate First Strike so they try to avoid attacking them.
	 * 
	 * @param attacker Unit that we are attacking with
	 * @param defender Unit we are considering attacking
	 * @return Numeric rating for how good of a target this unit is to attack; higher value=attack first, lower value=attack last
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 */
	@SuppressWarnings ("unused")
	final int evaluateTarget (final ExpandedUnitDetails attacker, final ExpandedUnitDetails defender) throws MomException
	{
		final int result;

		// Go for units with sufficient mana to still cast spells first
		if (defender.getManaRemaining () >= 10)
			result = 3;

		// Then go for units with ranged attacks left
		else if (getUnitCalculations ().canMakeRangedAttack (defender))
			result = 2;
		
		// Then take out everyone else
		else
			result = 1;
		
		return result;
	}
	
	/**
	 * Selects the best target that a particular unit should try to attack
	 * 
	 * @param attacker Unit that we are attacking with
	 * @param combatLocation Where the combat is taking place 
	 * @param movementDirections Movement directions as calculated by calculateCombatMovementDistances
	 * @param doubleMovementDistances Movement distances as calculated by calculateCombatMovementDistances
	 * @param movementTypes Movement types as calculated by calculateCombatMovementDistances
	 * @param units Units list
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param combatMapCoordinateSystem Combat map coordinate system
	 * @param db Lookup lists built over the XML database
	 * @return Best unit to attack, or null if enemy is wiped out already or if they're all invisible
	 * @throws RecordNotFoundException If one of the expected items can't be found in the DB
	 * @throws MomException If we cannot find any appropriate experience level for this unit
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 */
	final MemoryUnit selectBestTarget (final ExpandedUnitDetails attacker, final MapCoordinates3DEx combatLocation,
		final int [] [] movementDirections, final int [] [] doubleMovementDistances, final CombatMovementType [] [] movementTypes,
		final List<MemoryUnit> units, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final CoordinateSystem combatMapCoordinateSystem,
		final CommonDatabase db)
		throws RecordNotFoundException, MomException, PlayerNotFoundException
	{
		// Check all enemy units in combat
		MemoryUnit bestUnit = null;
		Integer bestScore = null;
		
		// Note on owning vs controlling player ID - attacker.getControllingPlayerID () is the AI player whose turn it is, who is controlling the unit, so this is fine.
		// But they don't want to attack their own units who might just be temporarily confused, equally if an enemy unit is confused and currently under our
		// control, we still want to kill it - ideally we confusee units and make them kill each other!  So this is why it is not thisUnit.getControllingPlayerID ()
		for (final MemoryUnit thisUnit : units)
			if ((thisUnit.getOwningPlayerID () != attacker.getControllingPlayerID ()) && (thisUnit.getStatus () == UnitStatusID.ALIVE) && 
				(combatLocation.equals (thisUnit.getCombatLocation ())) && (thisUnit.getCombatPosition () != null) &&
				(thisUnit.getCombatHeading () != null) && (thisUnit.getCombatSide () != null) && (thisUnit != attacker.getMemoryUnit ()) &&
				(!db.getUnitsThatMoveThroughOtherUnits ().contains (thisUnit.getUnitID ())) &&
				
				// Check that if we select this enemy, we have a valid action to take against it - i.e. that we don't have a cunning human
				// player box in a weak unit by surrounding it by 8 others and then get in a tizzy trying to work out a path to the unit.
				
				// This also stops grounded units trying to attack flying units, as calculateCombatMovementDistances will already have
				// figured out that we're not allowed to attack it and set it to CANNOT_MOVE and MOVEMENT_DISTANCE_CANNOT_MOVE_HERE
				((ALL_ATTACKS.contains (movementTypes [thisUnit.getCombatPosition ().getY ()] [thisUnit.getCombatPosition ().getX ()])) ||
				(movementDirections [thisUnit.getCombatPosition ().getY ()] [thisUnit.getCombatPosition ().getX ()] > 0)))
			{
				// Make sure we can actually see it
				final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (thisUnit, null, null, null, players, mem, db);
				if (getUnitUtils ().canSeeUnitInCombat (xu, attacker.getOwningPlayerID (), players, mem, db, combatMapCoordinateSystem))
				{
					// Is this the first possible target we've found, or better than our current target.
					// EvaluateTarget just returns 1, 2 or 3 - bump that up a lot.
					int thisScore = evaluateTarget (attacker, xu) * 1000;
					
					// Subtract more the further away the unit is, so closer units get a higher score.
					// Can't use doubleMovementDistances for this as it gets set to the same high 999 value for all ranged attacks.
					if (RANGED_ATTACKS.contains (movementTypes [thisUnit.getCombatPosition ().getY ()] [thisUnit.getCombatPosition ().getX ()]))
						thisScore = thisScore - (int) (10 * getCoordinateSystemUtils ().determineReal2DDistanceBetween
							(combatMapCoordinateSystem, attacker.getCombatPosition (), (MapCoordinates2DEx) thisUnit.getCombatPosition ()));
					else
						thisScore = thisScore - doubleMovementDistances [thisUnit.getCombatPosition ().getY ()] [thisUnit.getCombatPosition ().getX ()];
					
					if ((bestScore == null) || (thisScore > bestScore))
					{
						bestUnit = thisUnit;
						bestScore = thisScore;
					}
				}
			}
		
		return bestUnit;
	}
	
	/**
	 * Handles the AI working out what it wants to do with a single unit, and taking that action.
	 * 
	 * @param tu Unit being moved
	 * @param combatLocation Where the combat is taking place 
	 * @param combatMap Combat map
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether we did something with the unit or not
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	final CombatAIMovementResult moveOneUnit (final ExpandedUnitDetails tu, final MapCoordinates3DEx combatLocation,
		final MapAreaOfCombatTiles combatMap, final MomSessionVariables mom)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		// Work out where this unit can move
		final CombatMapSize combatMapSize = mom.getSessionDescription ().getCombatMapSize ();
		
		final int [] [] doubleMovementDistances = new int [combatMapSize.getHeight ()] [combatMapSize.getWidth ()];
		final int [] [] movementDirections = new int [combatMapSize.getHeight ()] [combatMapSize.getWidth ()];
		final CombatMovementType [] [] movementTypes = new CombatMovementType [combatMapSize.getHeight ()] [combatMapSize.getWidth ()];
		
		getUnitMovement ().calculateCombatMovementDistances (doubleMovementDistances, movementDirections,
			movementTypes, tu, mom.getGeneralServerKnowledge ().getTrueMap (),
			combatMap, combatMapSize, mom.getPlayers (), mom.getServerDB ());
		
		// Work out where we want to move.  Firstly, can we even make any kind of attack?  Units like settlers have no reason to move at all
		// We can get away with calling canMakeMeleeAttack generically without using the actual combatActionID of each enemy, as even if we can
		// make an attack in theory, selectBestTarget will realise we have no actual valid targets if we're grounded and all enemy are flying.
		final MapCoordinates2DEx moveTo;
		if ((getUnitCalculations ().canMakeRangedAttack (tu)) || (getUnitCalculations ().canMakeMeleeAttack (null, tu, mom.getServerDB ())))
		{
			// Can we find anything to attack?  Maybe they're all invisible
			final MemoryUnit bestUnit = selectBestTarget (tu, combatLocation, movementDirections, doubleMovementDistances, movementTypes,
				mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (),
				mom.getSessionDescription ().getCombatMapSize (), mom.getServerDB ());
			
			if (bestUnit != null)
				moveTo = new MapCoordinates2DEx ((MapCoordinates2DEx) bestUnit.getCombatPosition ());

			// Head to defenders area - then if both sides are completely invisible, at least they'll find each other - as long as we can actually go there
			else				
				moveTo = new MapCoordinates2DEx (CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_FRONT_ROW_CENTRE_X,
					CombatStartAndEndImpl.COMBAT_SETUP_DEFENDER_FRONT_ROW_CENTRE_Y);
		}
		else
			moveTo = null;
		
		CombatAIMovementResult result = CombatAIMovementResult.NOTHING; 
		if (moveTo != null)
		{
			// If we can attack at range then shoot it - if not then start walking towards it, or if adjacent to it already then attack it
			boolean ok = true;
			if (ALL_ATTACKS.contains (movementTypes [moveTo.getY ()] [moveTo.getX ()]))
			{
				// Have set MoveToX, MoveToY already so nothing to do
			}
			else
				// Walk towards it - To find our destination, we need to start from the unit we're trying to attack
				// and work backwards until we find a space that we can reach this turn
				while ((ok) && (movementTypes [moveTo.getY ()] [moveTo.getX ()] == CombatMovementType.CANNOT_MOVE))
				{
					final int d = movementDirections [moveTo.getY ()] [moveTo.getX ()];
					if (d < 1)
						ok = false;	// No route to get there (or we're already standing there)
					
					else if (!getCoordinateSystemUtils ().move2DCoordinates (combatMapSize, moveTo,
						getCoordinateSystemUtils ().normalizeDirection (combatMapSize.getCoordinateSystemType (), d+4)))
						
						throw new MomException ("AI Combat routine traced a location off the edge of the combat map");
				}
			
			// Move there
			if (ok)
				result = getCombatProcessing ().okToMoveUnitInCombat (tu, moveTo, MoveUnitInCombatReason.MANUAL, movementDirections, movementTypes, mom) ?
					CombatAIMovementResult.ENDED_COMBAT : CombatAIMovementResult.MOVED_OR_ATTACKED;
		}
		
		return result;
	}
	
	/**
	 * AI plays out one round in combat, deciding which units to move in which order, and taking all actions.
	 * 
	 * This might be for node defenders, raiders, rampaging monsters, an AI controlled
	 * wizard, or a human controlled wizard who has put the combat on 'Auto'
	 * 
	 * @param combatLocation Where the combat is taking place 
	 * @param currentPlayer The player whose turn is being taken
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether we had at least one unit take some useful action or not
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If an expected item cannot be found in the db
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final CombatAIMovementResult aiCombatTurn (final MapCoordinates3DEx combatLocation, final PlayerServerDetails currentPlayer, final MomSessionVariables mom)
		throws RecordNotFoundException, MomException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		// If AI Wizard (not raiders, not banished, not human player on auto) then maybe cast a spell before we move units
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) currentPlayer.getPersistentPlayerPublicKnowledge ();
		CombatAIMovementResult result = CombatAIMovementResult.NOTHING;
		
		if ((PlayerKnowledgeUtils.isWizard (pub.getWizardID ())) && (!currentPlayer.getPlayerDescription ().isHuman ()) &&
			(pub.getWizardState () == WizardState.ACTIVE))
			
			result = getSpellAI ().decideWhatToCastCombat (currentPlayer, null, combatLocation, mom);
		
		if (result != CombatAIMovementResult.ENDED_COMBAT)
		{
			// Get the combat terrain
			final ServerGridCellEx serverGridCell = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(combatLocation.getZ ()).getRow ().get (combatLocation.getY ()).getCell ().get (combatLocation.getX ());
			
			final MapAreaOfCombatTiles combatMap = serverGridCell.getCombatMap ();
			
			// Let units move more than once, but only safe way to do this is to call listUnitsToMove each time as units may be killed, scrubbed from memory, etc
			int retryCount = 0;
			while ((retryCount < 10) && (mom.getPlayers ().size () > 0))
			{
				// Get a list of all unit we need to move
				final List<MemoryUnit> unitsToMove = listUnitsToMove (combatLocation, currentPlayer.getPlayerDescription ().getPlayerID (),
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
				if (unitsToMove.size () == 0)
					retryCount = 100;
				else
				{
					// Sort the list so that we use spellcasters first, then ranged attacks, and only move close combat guys last
					final List<ExpandedUnitDetailsAndCombatAIOrder> sortedUnitsToMove = new ArrayList<ExpandedUnitDetailsAndCombatAIOrder> ();
					for (final MemoryUnit tu : unitsToMove)
					{
						final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (tu, null, null, null,
							mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
						
						sortedUnitsToMove.add (new ExpandedUnitDetailsAndCombatAIOrder (xu, calculateUnitCombatAIOrder (xu)));
					}
					
					Collections.sort (sortedUnitsToMove);
					
					// Move each unit in turn
					final Iterator<ExpandedUnitDetailsAndCombatAIOrder> sortedUnitsToMoveIter = sortedUnitsToMove.iterator ();
					while ((result != CombatAIMovementResult.ENDED_COMBAT) && (sortedUnitsToMoveIter.hasNext ()))
					{
						final ExpandedUnitDetailsAndCombatAIOrder tu = sortedUnitsToMoveIter.next ();
						
						// A previous unit might have already fired the shot that wiped out the enemy and ended the
						// combat, in which case all units would have had their CombatX, CombatY values set to -1, -1
						if (tu.getUnit ().getCombatPosition () != null)
						{
							// First try to cast fixed spells (e.g. Giant Spiders' web)
							CombatAIMovementResult thisResult = getSpellAI ().decideWhetherToCastFixedSpellInCombat (currentPlayer, tu.getUnit (), combatLocation, mom);
							
							// Next try to cast spells imbued in hero items
							if (thisResult == CombatAIMovementResult.NOTHING)
								thisResult = getSpellAI ().decideWhetherToCastSpellImbuedInHeroItem (currentPlayer, tu.getUnit (), combatLocation, mom);
							
							// Next consider casting a spell if the unit is a spellcaster without a ranged attack (e.g. Angel)
							if ((thisResult == CombatAIMovementResult.NOTHING) &&
								(tu.getUnit ().getManaRemaining () > 0) && (!getUnitCalculations ().canMakeRangedAttack (tu.getUnit ())))
								
								thisResult = getSpellAI ().decideWhatToCastCombat (currentPlayer, tu.getUnit (), combatLocation, mom);
							
							if (thisResult == CombatAIMovementResult.NOTHING)
								thisResult = moveOneUnit (tu.getUnit (), combatLocation, combatMap, mom);
							
							if (thisResult != CombatAIMovementResult.NOTHING)
								result = thisResult;
						}
					}

					retryCount++;
				}
			}
		}
		
		return result;
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