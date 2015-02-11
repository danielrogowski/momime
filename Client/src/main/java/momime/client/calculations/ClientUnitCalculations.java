package momime.client.calculations;

import momime.client.graphics.database.UnitSkillGfx;
import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.AvailableUnit;

import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Client side only methods dealing with unit calculations
 */
public interface ClientUnitCalculations
{
	/**
	 * Chooses the preferred method of movement for this unit, i.e. the one with the lowest preference number (no. 1 is chosen first, then no. 2, etc.)
	 * 
	 * This ensures that e.g. Flying units (whether natural flight, spell-cast Flight or Chaos Channels Flight) all show the
	 * correct flight icon, and units with Swimming/Sailing show the wave icon
	 * 
	 * @param unit Unit to determine the movement graphics for
	 * @return Movement graphics node
	 * @throws RecordNotFoundException If the unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If this unit has no skills which have movement graphics, or we can't find its experience level
	 */
	public UnitSkillGfx findPreferredMovementSkillGraphics (final AvailableUnit unit)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;

	/**
	 * combatActionIDs are MELEE when attacking melee, RANGED when attacking ranged, and generated by
	 * this routine when units are not attacking.  It looks up the combatActionIDs depending on what movement
	 * skills the unit has in such a way that we avoid having to hard code combatActionIDs.
	 * 
	 * e.g. a regular unit of swordsmen shows the STAND image while not moving, but if we cast
	 * Flight on them then we need to show the FLY animation instead.
	 *
	 * In the animations as directly converted from the original MoM graphics, WALK and FLY look the same - they
	 * resolve to the same animation, named e.g. UN100_D4_WALKFLY.  However the intention in the long term is
	 * to separate these and show flying units significantly raised up off the ground, so you can actually see flying
	 * units coming down to ground level when they have web cast on them, or swordsmen high up in the
	 * air when they have flight cast on them.
	 * 
	 * @param unit Unit to determine the combat action ID for
	 * @param isMoving Whether the unit is standing still or moving
	 * @return Action ID for a unit standing still or moving
	 * @throws RecordNotFoundException If the unit, weapon grade, skill or so on can't be found in the XML database
	 * @throws PlayerNotFoundException If we can't find the player who owns the unit
	 * @throws MomException If this unit has no skills which have movement graphics, we can't find its experience level, or a movement skill doesn't specify an action ID
	 */
	public String determineCombatActionID (final AvailableUnit unit, final boolean isMoving)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;
}