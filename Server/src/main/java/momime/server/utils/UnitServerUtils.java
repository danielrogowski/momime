package momime.server.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.database.v0_9_4.UnitHasSkill;
import momime.common.messages.UnitUtils;
import momime.common.messages.v0_9_4.MemoryUnit;
import momime.common.messages.v0_9_4.UnitSpecialOrder;
import momime.common.messages.v0_9_4.UnitStatusID;
import momime.server.database.ServerDatabaseLookup;
import momime.server.database.v0_9_4.Unit;
import momime.server.database.v0_9_4.UnitSkill;

/**
 * Server side only helper methods for dealing with units
 */
public final class UnitServerUtils
{
	/**
	 * Chooses a name for this hero (out of 5 possibilities) and rolls their random skills
	 * @param unit The hero to generate name and skills for
	 * @param db Lookup lists built over the XML database
	 * @param debugLogger Logger to write to debug text file when the debug log is enabled
	 * @throws MomException If we find a hero who has no possible names defined, or who needs a random skill and we can't find a suitable one
	 * @throws RecordNotFoundException If we can't find the definition for the unit
	 */
	public static final void generateHeroNameAndRandomSkills (final MemoryUnit unit, final ServerDatabaseLookup db, final Logger debugLogger)
		throws MomException, RecordNotFoundException
	{
		debugLogger.entering (UnitServerUtils.class.getName (), "generateHeroNameAndRandomSkills", unit.getUnitID ());

		final Unit unitDefinition = db.findUnit (unit.getUnitID (), "generateHeroNameAndRandomSkills");

		// Pick a name at random
		if (unitDefinition.getHeroName ().size () == 0)
			throw new MomException ("No hero names found for unit ID \"" + unit.getUnitID () + "\"");

		unit.setHeroNameID (unitDefinition.getHeroName ().get (RandomUtils.getGenerator ().nextInt (unitDefinition.getHeroName ().size ())).getHeroNameID ());

		// Any random skills to add?
		if ((unitDefinition.getHeroRandomPickCount () != null) && (unitDefinition.getHeroRandomPickCount () > 0))
		{
			debugLogger.finest ("Hero " + unit.getUnitID () + "' belonging to player ID " + unit.getOwningPlayerID () + " skills before rolling extras: " + UnitUtils.describeBasicSkillValuesInDebugString (unit));

			// Run once for each random skill we get
			for (int pickNo = 0; pickNo < unitDefinition.getHeroRandomPickCount (); pickNo++)
			{
				// Get a list of all valid choices
				final List<String> skillChoices = new ArrayList<String> ();
				for (final UnitSkill thisSkill : db.getUnitSkills ())
				{
					// We can spot hero skills since they'll have at least one of these values filled in
					if (((thisSkill.getHeroSkillTypeID () != null) && (!thisSkill.getHeroSkillTypeID ().equals (""))) ||
						((thisSkill.isOnlyIfHaveAlready () != null) && (thisSkill.isOnlyIfHaveAlready ())) ||
						((thisSkill.getMaxOccurrences () != null) && (thisSkill.getMaxOccurrences () > 0)))
					{
						// Its a hero skill - do we have it already?
						final int currentSkillValue = UnitUtils.getBasicSkillValue (unit.getUnitHasSkill (), thisSkill.getUnitSkillID ());

						// Is it applicable?
						// If the unit has no hero random pick type specified, then it means it can use any hero skills
						// If the skill has no hero random pick type specified, then it means it can be used by any units
						if (((unitDefinition.getHeroRandomPickType () == null) || (thisSkill.getHeroSkillTypeID () == null) || (unitDefinition.getHeroRandomPickType ().equals (thisSkill.getHeroSkillTypeID ()))) &&
							((thisSkill.isOnlyIfHaveAlready () == null) || (!thisSkill.isOnlyIfHaveAlready ()) || (currentSkillValue > 0)) &&
							((thisSkill.getMaxOccurrences () == null) || (currentSkillValue < thisSkill.getMaxOccurrences ())))

								skillChoices.add (thisSkill.getUnitSkillID ());
					}
				}

				// Pick one
				if (skillChoices.size () == 0)
					throw new MomException ("generateHeroNameAndRandomSkills: No valid hero skill choices for unit " + unit.getUnitID ());

				final String skillID = skillChoices.get (RandomUtils.getGenerator ().nextInt (skillChoices.size ()));
				final int currentSkillValue = UnitUtils.getBasicSkillValue (unit.getUnitHasSkill (), skillID);
				if (currentSkillValue >= 0)
					UnitUtils.setBasicSkillValue (unit, skillID, currentSkillValue + 1, debugLogger);
				else
				{
					final UnitHasSkill newSkill = new UnitHasSkill ();
					newSkill.setUnitSkillID (skillID);
					newSkill.setUnitSkillValue (1);
					unit.getUnitHasSkill ().add (newSkill);
				}

				debugLogger.finest ("Hero " + unit.getUnitID () + "' belonging to player ID " + unit.getOwningPlayerID () + " skills after rolling extras: " + UnitUtils.describeBasicSkillValuesInDebugString (unit));
			}
		}

		// Update status
		unit.setStatus (UnitStatusID.GENERATED);

		debugLogger.exiting (UnitServerUtils.class.getName (), "generateHeroNameAndRandomSkills");
	}

	/**
	 * @param order Special order that a unit has
	 * @return True if this special order results in the unit dying/being removed from play
	 */
	public static final boolean doesUnitSpecialOrderResultInDeath (final UnitSpecialOrder order)
	{
		return (order == UnitSpecialOrder.BUILD_CITY) || (order == UnitSpecialOrder.MELD_WITH_NODE) || (order == UnitSpecialOrder.DISMISS);
	}

	/**
	 * Prevent instantiation
	 */
	private UnitServerUtils ()
	{
	}
}
