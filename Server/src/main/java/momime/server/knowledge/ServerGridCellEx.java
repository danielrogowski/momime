package momime.server.knowledge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import momime.common.messages.NumberedHeroItem;
import momime.common.messages.PendingMovement;
import momime.server.messages.ServerGridCell;

/**
 * Server-side only additional storage required at every map cell, that doesn't need to be persisted into save game files
 */
public final class ServerGridCellEx extends ServerGridCell
{
	/** Whether the lair here was generated as "weak" - this is needed when populating the lair with monsters */ 
	private Boolean lairWeak;
	
	/**
	 * Stores the random roll between 0..1 for where between min and max the various stats of a node/lair/tower are.
	 * e.g. for nodes, this keeps the size of the aura, the strength of the defending monsters, and the quality of treasure reward all at the same level.
	 * Same as "lairWeak", this is only used temporarily during map generation, so doesn't need to be persisted into saved game files. 
	 */
	private Double nodeLairTowerPowerProportion;

	/** The player who attacked to initiate the combat */
	private Integer attackingPlayerID;
	
	/** Player who was attacked to initiate the combat */
	private Integer defendingPlayerID;
	
	/** Who's turn it is in the combat currently taking place at this location */
	private Integer combatCurrentPlayerID;
	
	/** Whether combatCurrentPlayer has cast a spell yet during this combat turn (can only cast 1 spell per combat turn) */
	private Boolean spellCastThisCombatTurn;
	
	/** How much casting skill the defender has left available for the duration of this combat */
	private Integer combatDefenderCastingSkillRemaining;
	
	/** How much casting skill the attacker has left available for the duration of this combat */
	private Integer combatAttackerCastingSkillRemaining;
	
	/** In simultaneous turns games, the PendingMovement the attacker made which caused the combat currently taking place at this location */
	private PendingMovement combatAttackerPendingMovement;

	/** In simultaneous turns games, the PendingMovement the defender made which caused the combat currently taking place at this location (border conflicts/counterattacks only) */
	private PendingMovement combatDefenderPendingMovement;
	
	/** Number of defender units when the combat started */
	private Integer defenderUnitCount;
	
	/** Number of attacker units when the combat started */
	private Integer attackerUnitCount;
	
	/** Cost of most expensive defender */
	private Integer defenderMostExpensiveUnitCost;
	
	/** Cost of most expensive attacker */
	private Integer attackerMostExpensiveUnitCost;
	
	/** Fame the defender lost in combat due to particular units dying */
	private Integer defenderSpecialFameLost;
	
	/** Fame the attacker lost in combat due to particular units dying */
	private Integer attackerSpecialFameLost;

	/** List of items held by any heroes that died in this combat, no matter which side they were on */
	private final List<NumberedHeroItem> itemsFromHeroesWhoDiedInCombat = new ArrayList<NumberedHeroItem> ();
	
	/** Map keyed by Unit URN indicating how many times each unit has been attacked this combat round */
	private final Map<Integer, Integer> numberOfTimedAttacked = new HashMap<Integer, Integer> (); 

	/** Map keyed by Unit URN indicating the last direction a unit moved in the current combat */
	private final Map<Integer, Integer> lastCombatMoveDirection = new HashMap<Integer, Integer> (); 
	
	/**
	 * @return Whether the lair here was generated as "weak" - this is needed when populating the lair with monsters
	 */ 
	public final Boolean isLairWeak ()
	{
		return lairWeak;
	}

	/**
	 * @param weak Whether the lair here was generated as "weak" - this is needed when populating the lair with monsters
	 */ 
	public final void setLairWeak (final Boolean weak)
	{
		lairWeak = weak;
	}

	/**
	 * @return Random roll between 0..1 for where between min and max the various stats of a node/lair/tower are
	 */
	public final Double getNodeLairTowerPowerProportion ()
	{
		return nodeLairTowerPowerProportion;
	}

	/**
	 * @param prop Random roll between 0..1 for where between min and max the various stats of a node/lair/tower are
	 */
	public final void setNodeLairTowerPowerProportion (final Double prop)
	{
		nodeLairTowerPowerProportion = prop;
	}

	/**
	 * @return The player who attacked to initiate the combat
	 */
	public final Integer getAttackingPlayerID ()
	{
		return attackingPlayerID;
	}		

	/**
	 * @param id The player who attacked to initiate the combat
	 */
	public final void setAttackingPlayerID (final Integer id)
	{
		attackingPlayerID = id;
	}
	
	/**
	 * @return Player who was attacked to initiate the combat
	 */
	public final Integer getDefendingPlayerID ()
	{
		return defendingPlayerID;
	}
	
	/**
	 * @param id Player who was attacked to initiate the combat
	 */
	public final void setDefendingPlayerID (final Integer id)
	{
		defendingPlayerID = id;
	}
	
	/**
	 * @return Who's turn it is in the combat currently taking place at this location
	 */
	public final Integer getCombatCurrentPlayerID ()
	{
		return combatCurrentPlayerID;
	}

	/**
	 * @param id Who's turn it is in the combat currently taking place at this location
	 */
	public final void setCombatCurrentPlayerID (final Integer id)
	{
		combatCurrentPlayerID = id;
	}
	
	/**
	 * @return Whether combatCurrentPlayer has cast a spell yet during this combat turn (can only cast 1 spell per combat turn)
	 */
	public final Boolean isSpellCastThisCombatTurn ()
	{
		return spellCastThisCombatTurn;
	}

	/**
	 * @param cast Whether combatCurrentPlayer has cast a spell yet during this combat turn (can only cast 1 spell per combat turn)
	 */
	public final void setSpellCastThisCombatTurn (final Boolean cast)
	{
		spellCastThisCombatTurn = cast;
	}

	/**
	 * @return How much casting skill the defender has left available for the duration of this combat
	 */
	public final Integer getCombatDefenderCastingSkillRemaining ()
	{
		return combatDefenderCastingSkillRemaining;
	}

	/**
	 * @param skill How much casting skill the defender has left available for the duration of this combat
	 */
	public final void setCombatDefenderCastingSkillRemaining (final Integer skill)
	{
		combatDefenderCastingSkillRemaining = skill;
	}
	
	/**
	 * @return How much casting skill the attacker has left available for the duration of this combat
	 */
	public final Integer getCombatAttackerCastingSkillRemaining ()
	{
		return combatAttackerCastingSkillRemaining;
	}
	
	/**
	 * @param skill How much casting skill the attacker has left available for the duration of this combat
	 */
	public final void setCombatAttackerCastingSkillRemaining (final Integer skill)
	{
		combatAttackerCastingSkillRemaining = skill;
	}

	/**
	 * @return In simultaneous turns games, the PendingMovement the attacker made which caused the combat currently taking place at this location
	 */
	public final PendingMovement getCombatAttackerPendingMovement ()
	{
		return combatAttackerPendingMovement;
	}

	/**
	 * @param move In simultaneous turns games, the PendingMovement the attacker made which caused the combat currently taking place at this location
	 */
	public final void setCombatAttackerPendingMovement (final PendingMovement move)
	{
		combatAttackerPendingMovement = move;
	}

	/**
	 * @return In simultaneous turns games, the PendingMovement the defender made which caused the combat currently taking place at this location (border conflicts/counterattacks only)
	 */
	public final PendingMovement getCombatDefenderPendingMovement ()
	{
		return combatDefenderPendingMovement;
	}
	
	/**
	 * @param move In simultaneous turns games, the PendingMovement the defender made which caused the combat currently taking place at this location (border conflicts/counterattacks only)
	 */
	public final void setCombatDefenderPendingMovement (final PendingMovement move)
	{
		combatDefenderPendingMovement = move;
	}

	/**
	 * @return Number of defender units when the combat started
	 */
	public final Integer getDefenderUnitCount ()
	{
		return defenderUnitCount;
	}
	
	/**
	 * @param c Number of defender units when the combat started
	 */
	public final void setDefenderUnitCount (final Integer c)
	{
		defenderUnitCount = c;
	}
	
	/**
	 * @return Number of attacker units when the combat started\
	 */
	public final Integer getAttackerUnitCount ()
	{
		return attackerUnitCount;
	}

	/**
	 * @param c Number of attacker units when the combat started
	 */
	public final void setAttackerUnitCount (final Integer c)
	{
		attackerUnitCount = c;
	}

	/**
	 * @return Cost of most expensive defender
	 */
	public final Integer getDefenderMostExpensiveUnitCost ()
	{
		return defenderMostExpensiveUnitCost;
	}

	/**
	 * @param c Cost of most expensive defender
	 */
	public final void setDefenderMostExpensiveUnitCost (final Integer c)
	{
		defenderMostExpensiveUnitCost = c;
	}
	
	/**
	 * @return Most of most expensive attacker
	 */
	public final Integer getAttackerMostExpensiveUnitCost ()
	{
		return attackerMostExpensiveUnitCost;
	}
	
	/**
	 * @param c Cost of most expensive attacker
	 */
	public final void setAttackerMostExpensiveUnitCost (final Integer c)
	{
		attackerMostExpensiveUnitCost = c;
	}

	/**
	 * @return Fame the defender lost in combat due to particular units dying
	 */
	public final Integer getDefenderSpecialFameLost ()
	{
		return defenderSpecialFameLost;
	}

	/**
	 * @param f Fame the defender lost in combat due to particular units dying
	 */
	public final void setDefenderSpecialFameLost (final Integer f)
	{
		defenderSpecialFameLost = f;
	}
	
	/**
	 * @return Fame the attacker lost in combat due to particular units dying
	 */
	public final Integer getAttackerSpecialFameLost ()
	{
		return attackerSpecialFameLost;
	}
	
	/**
	 * @param f Fame the attacker lost in combat due to particular units dying
	 */
	public final void setAttackerSpecialFameLost (final Integer f)
	{
		attackerSpecialFameLost = f;
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