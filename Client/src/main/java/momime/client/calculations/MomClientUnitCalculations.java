package momime.client.calculations;

import momime.client.graphics.database.v0_9_5.UnitSkill;
import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_5.AvailableUnit;

import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Client side only methods dealing with unit calculations
 */
public interface MomClientUnitCalculations
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
	public UnitSkill findPreferredMovementSkillGraphics (final AvailableUnit unit)
		throws RecordNotFoundException, PlayerNotFoundException, MomException;
}