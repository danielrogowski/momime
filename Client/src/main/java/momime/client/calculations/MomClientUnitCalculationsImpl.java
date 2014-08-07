package momime.client.calculations;

import java.util.List;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.v0_9_5.UnitSkill;
import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_5.UnitHasSkill;
import momime.common.messages.v0_9_5.AvailableUnit;
import momime.common.messages.v0_9_5.MemoryUnit;
import momime.common.utils.UnitUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.PlayerNotFoundException;

/**
 * Client side only methods dealing with unit calculations
 */
public final class MomClientUnitCalculationsImpl implements MomClientUnitCalculations
{
	/** Class logger */
	private final Log log = LogFactory.getLog (MomClientUnitCalculationsImpl.class);
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;

	/** Unit utils */
	private UnitUtils unitUtils;

	/** Multiplayer client */
	private MomClient client;
	
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
	@Override
	public final UnitSkill findPreferredMovementSkillGraphics (final AvailableUnit unit)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering findPreferredMovementSkillGraphics: " + unit.getUnitID ());
		
		// Pre-merge in skills granted from spells (e.g. chaos channels flight) so that we only do it once time
		final List<UnitHasSkill> mergedSkills;
		if (unit instanceof MemoryUnit)
			mergedSkills = getUnitUtils ().mergeSpellEffectsIntoSkillList (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (), (MemoryUnit) unit);
		else
			mergedSkills = unit.getUnitHasSkill ();
		
		// Check all movement skills
		UnitSkill bestMatch = null;
		for (final UnitSkill thisSkill : getGraphicsDB ().getUnitSkill ())
			if (thisSkill.getMovementIconImagePreference () != null)
				if ((bestMatch == null) || (thisSkill.getMovementIconImagePreference () < bestMatch.getMovementIconImagePreference ()))
					if (getUnitUtils ().getModifiedSkillValue (unit, mergedSkills, thisSkill.getUnitSkillID (), getClient ().getPlayers (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
						getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getCombatAreaEffect (), getClient ().getClientDB ()) >= 0)
						bestMatch = thisSkill;
		
		if (bestMatch == null)
			throw new MomException ("Unit " + unit.getUnitID () + " has no skills which have movement graphics");
		
		log.trace ("Exiting findPreferredMovementSkillGraphics = " + bestMatch.getUnitSkillID ());
		return bestMatch;
	}

	/**
	 * @return Graphics database
	 */
	public final GraphicsDatabaseEx getGraphicsDB ()
	{
		return graphicsDB;
	}

	/**
	 * @param db Graphics database
	 */
	public final void setGraphicsDB (final GraphicsDatabaseEx db)
	{
		graphicsDB = db;
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
	 * @return Multiplayer client
	 */
	public final MomClient getClient ()
	{
		return client;
	}
	
	/**
	 * @param obj Multiplayer client
	 */
	public final void setClient (final MomClient obj)
	{
		client = obj;
	}
}