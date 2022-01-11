package momime.server.knowledge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.messages.MapAreaOfCombatTiles;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.PendingMovement;

/**
 * Stores details on the server about one combat that is taking place.
 *  
 * Used to be part of ServerGridCellEx but it became very difficult when two combats took place one after the other at the same map cell for the
 * methods clearing up after the combat to make sure they were clearing up the right combat and not a new one taking place at the same location.
 */
public final class CombatDetails
{
	/** Unique identifier for this combat */
	private final int combatURN;
	
	/** Where the combat is taking place (usually the location of the defenders) */
	private final MapCoordinates3DEx combatLocation;
	
	/** Terrain for this combat */
	private final MapAreaOfCombatTiles combatMap;

	/** The player who attacked to initiate the combat */
	private final int attackingPlayerID;
	
	/** Player who was attacked to initiate the combat */
	private final int defendingPlayerID;

	/** In simultaneous turns games, the PendingMovement the attacker made which caused the combat currently taking place at this location */
	private final PendingMovement attackerPendingMovement;

	/** In simultaneous turns games, the PendingMovement the defender made which caused the combat currently taking place at this location (border conflicts/counterattacks only) */
	private final PendingMovement defenderPendingMovement;	
	
	/** Number of attacker units when the combat started */
	private final int attackerUnitCount;
	
	/** Number of defender units when the combat started */
	private final int defenderUnitCount;
	
	/** Cost of most expensive attacker */
	private final int attackerMostExpensiveUnitCost;
	
	/** Cost of most expensive defender */
	private final int defenderMostExpensiveUnitCost;
	
	/** The number of turns the combat has been going */
	private int combatTurnCount;
	
	/** Who's turn it is in the combat currently taking place at this location */
	private int combatCurrentPlayerID;
	
	/** Whether combatCurrentPlayer has cast a spell yet during this combat turn (can only cast 1 spell per combat turn) */
	private boolean spellCastThisCombatTurn;
	
	/** How much casting skill the attacker has left available for the duration of this combat */
	private int attackerCastingSkillRemaining;
	
	/** How much casting skill the defender has left available for the duration of this combat */
	private int defenderCastingSkillRemaining;
	
	/** Fame the attacker lost in combat due to particular units dying */
	private int attackerSpecialFameLost;
	
	/** Fame the defender lost in combat due to particular units dying */
	private int defenderSpecialFameLost;
	
	/** Keeps track of how bad the chances are of the town being damaged by combat */
	private int collateralAccumulator;

	/** List of items held by any heroes that died in this combat, no matter which side they were on */
	private final List<NumberedHeroItem> itemsFromHeroesWhoDiedInCombat = new ArrayList<NumberedHeroItem> ();
	
	/** Map keyed by Unit URN indicating how many times each unit has been attacked this combat round */
	private final Map<Integer, Integer> numberOfTimedAttacked = new HashMap<Integer, Integer> (); 

	/** Map keyed by Unit URN indicating the last direction a unit moved in the current combat */
	private final Map<Integer, Integer> lastCombatMoveDirection = new HashMap<Integer, Integer> (); 
	
	/**
	 * @param aCombatURN Unique identifier for this combat
	 * @param aCombatLocation Where the combat is taking place (usually the location of the defenders)
	 * @param aCombatMap Terrain for this combat
	 * @param anAttackingPlayerID The player who attacked to initiate the combat
	 * @param aDefendingPlayerID Player who was attacked to initiate the combat
	 * @param anAttackerPendingMovement In simultaneous turns games, the PendingMovement the attacker made which caused the combat currently taking place at this location
	 * @param aDefenderPendingMovement In simultaneous turns games, the PendingMovement the defender made which caused the combat currently taking place at this location
	 * 	(border conflicts/counterattacks only)
	 * @param anAttackerUnitCount Number of attacker units when the combat started
	 * @param aDefenderUnitCount Number of defender units when the combat started
	 * @param anAttackerMostExpensiveUnitCost Cost of most expensive attacker
	 * @param aDefenderMostExpensiveUnitCost Cost of most expensive defender
	 */
	public CombatDetails (final int aCombatURN, final MapCoordinates3DEx aCombatLocation, final MapAreaOfCombatTiles aCombatMap,
		final int anAttackingPlayerID, final int aDefendingPlayerID, final PendingMovement anAttackerPendingMovement, final PendingMovement aDefenderPendingMovement,
		final int anAttackerUnitCount, final int aDefenderUnitCount, final int anAttackerMostExpensiveUnitCost, final int aDefenderMostExpensiveUnitCost)
	{
		combatURN = aCombatURN;
		combatLocation = aCombatLocation;
		combatMap = aCombatMap;
		attackingPlayerID = anAttackingPlayerID;
		defendingPlayerID = aDefendingPlayerID;
		attackerPendingMovement = anAttackerPendingMovement;
		defenderPendingMovement = aDefenderPendingMovement;
		attackerUnitCount = anAttackerUnitCount;
		defenderUnitCount = aDefenderUnitCount;
		attackerMostExpensiveUnitCost = anAttackerMostExpensiveUnitCost;
		defenderMostExpensiveUnitCost = aDefenderMostExpensiveUnitCost;
	}

	/**
	 * @return Unique identifier for this combat
	 */
	public final int getCombatURN ()
	{
		return combatURN;
	}
	
	/**
	 * @return Where the combat is taking place (usually the location of the defenders)
	 */
	public final MapCoordinates3DEx getCombatLocation ()
	{
		return combatLocation;
	}
	
	/**
	 * @return Terrain for this combat
	 */
	public final MapAreaOfCombatTiles getCombatMap ()
	{
		return combatMap;
	}

	/**
	 * @return The player who attacked to initiate the combat
	 */
	public final int getAttackingPlayerID ()
	{
		return attackingPlayerID;
	}
	
	/**
	 * @return Player who was attacked to initiate the combat
	 */
	public final int getDefendingPlayerID ()
	{
		return defendingPlayerID;
	}

	/**
	 * @return In simultaneous turns games, the PendingMovement the attacker made which caused the combat currently taking place at this location
	 */
	public final PendingMovement getAttackerPendingMovement ()
	{
		return attackerPendingMovement;
	}

	/**
	 * @return In simultaneous turns games, the PendingMovement the defender made which caused the combat currently taking place at this location (border conflicts/counterattacks only)
	 */
	public final PendingMovement getDefenderPendingMovement ()
	{
		return defenderPendingMovement;
	}
	
	/**
	 * @return Number of attacker units when the combat started
	 */
	public final int getAttackerUnitCount ()
	{
		return attackerUnitCount;
	}
	
	/**
	 * @return Number of defender units when the combat started
	 */
	public final int getDefenderUnitCount ()
	{
		return defenderUnitCount;
	}
	
	/**
	 * @return Cost of most expensive attacker
	 */
	public final int getAttackerMostExpensiveUnitCost ()
	{
		return attackerMostExpensiveUnitCost;
	}
	
	/**
	 * @return Cost of most expensive defender
	 */
	public final int getDefenderMostExpensiveUnitCost ()
	{
		return defenderMostExpensiveUnitCost;
	}

	/**
	 * @return The number of turns the combat has been going
	 */
	public final int getCombatTurnCount ()
	{
		return combatTurnCount;
	}

	/**
	 * @param c The number of turns the combat has been going
	 */
	public final void setCombatTurnCount (final int c)
	{
		combatTurnCount = c;
	}
	
	/**
	 * @return Who's turn it is in the combat currently taking place at this location
	 */
	public final int getCombatCurrentPlayerID ()
	{
		return combatCurrentPlayerID;
	}
	
	/**
	 * @param c Who's turn it is in the combat currently taking place at this location
	 */
	public final void setCombatCurrentPlayerID (final int c)
	{
		combatCurrentPlayerID = c;
	}

	/**
	 * @return Whether combatCurrentPlayer has cast a spell yet during this combat turn (can only cast 1 spell per combat turn)
	 */
	public final boolean isSpellCastThisCombatTurn ()
	{
		return spellCastThisCombatTurn;
	}

	/**
	 * @param c Whether combatCurrentPlayer has cast a spell yet during this combat turn (can only cast 1 spell per combat turn)
	 */
	public final void setSpellCastThisCombatTurn (final boolean c)
	{
		spellCastThisCombatTurn = c;
	}

	/**
	 * @return How much casting skill the attacker has left available for the duration of this combat
	 */
	public final int getAttackerCastingSkillRemaining ()
	{
		return attackerCastingSkillRemaining;
	}
	
	/**
	 * @param c How much casting skill the attacker has left available for the duration of this combat
	 */
	public final void setAttackerCastingSkillRemaining (final int c)
	{
		attackerCastingSkillRemaining = c;
	}
	
	/**
	 * @return How much casting skill the defender has left available for the duration of this combat
	 */
	public final int getDefenderCastingSkillRemaining ()
	{
		return defenderCastingSkillRemaining;
	}

	/**
	 * @param c How much casting skill the defender has left available for the duration of this combat
	 */
	public final void setDefenderCastingSkillRemaining (final int c)
	{
		defenderCastingSkillRemaining = c;
	}

	/**
	 * @return Fame the attacker lost in combat due to particular units dying
	 */
	public final int getAttackerSpecialFameLost ()
	{
		return attackerSpecialFameLost;
	}

	/**
	 * @param f Fame the attacker lost in combat due to particular units dying
	 */
	public final void setAttackerSpecialFameLost (final int f)
	{
		attackerSpecialFameLost = f;
	}
	
	/**
	 * @return Fame the defender lost in combat due to particular units dying
	 */
	public final int getDefenderSpecialFameLost ()
	{
		return defenderSpecialFameLost;
	}
	
	/**
	 * @param f Fame the defender lost in combat due to particular units dying
	 */
	public final void setDefenderSpecialFameLost (final int f)
	{
		defenderSpecialFameLost = f;
	}

	/**
	 * @return Keeps track of how bad the chances are of the town being damaged by combat
	 */
	public final int getCollateralAccumulator ()
	{
		return collateralAccumulator;
	}

	/**
	 * @param a Keeps track of how bad the chances are of the town being damaged by combat
	 */
	public final void setCollateralAccumulator (final int a)
	{
		collateralAccumulator = a;
	}
	
	/**
	 * @return List of items held by any heroes that died in this combat, no matter which side they were on
	 */
	public final List<NumberedHeroItem> getItemsFromHeroesWhoDiedInCombat ()
	{
		return itemsFromHeroesWhoDiedInCombat;
	}

	/**
	 * @return Map keyed by Unit URN indicating how many times each unit has been attacked this combat round
	 */ 
	public final Map<Integer, Integer> getNumberOfTimedAttacked ()
	{
		return numberOfTimedAttacked;
	}

	/**
	 * @return Map keyed by Unit URN indicating the last direction a unit moved in the current combat
	 */
	public final Map<Integer, Integer> getLastCombatMoveDirection ()
	{
		return lastCombatMoveDirection;
	}
}