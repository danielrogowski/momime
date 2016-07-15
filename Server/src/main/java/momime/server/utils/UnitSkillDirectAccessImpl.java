package momime.server.utils;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import momime.common.MomException;
import momime.common.database.UnitSkillAndValue;
import momime.common.messages.AvailableUnit;

/**
 * Nearly all places in MoM IME should access unit skill values via ExpandedUnitDetails, since this takes all modifications
 * from spells, hero items and so on into account.  However a small handful of places on the server still need direct
 * access to the raw skill list of a unit, so the methods here can be used for that.
 * 
 * This should only be used in exceptional cases where there is a really good reason for wanting to do this.
 */
public final class UnitSkillDirectAccessImpl implements UnitSkillDirectAccess
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (UnitSkillDirectAccessImpl.class);
	
	/**
	 * @param skills List of unit skills to check; this can either be the unmodified list read straight from unit.getUnitHasSkill () or UnitHasSkillMergedList
	 * @param unitSkillID Unique identifier for this skill
	 * @return Basic value of the specified skill (defined in the XML or heroes rolled randomly); whether skills granted from spells are included depends on whether we pass in a UnitHasSkillMergedList or not; -1 if we do not have the skill
	 */
	@Override
	public final int getDirectSkillValue (final List<UnitSkillAndValue> skills, final String unitSkillID)
	{
		int skillValue = -1;
		final Iterator<UnitSkillAndValue> iter = skills.iterator ();

		while ((skillValue < 0) && (iter.hasNext ()))
		{
			final UnitSkillAndValue thisSkill = iter.next ();
			if (thisSkill.getUnitSkillID ().equals (unitSkillID))
			{
				if (thisSkill.getUnitSkillValue () == null)
					skillValue = 0;
				else
					skillValue = thisSkill.getUnitSkillValue ();
			}
		}

		return skillValue;
	}

	/**
	 * @param unit Unit whose skills to modify (note we pass in the unit rather than the skills list to force using the live list and not a UnitHasSkillMergedList)
	 * @param unitSkillID Unique identifier for this skill
	 * @param skillValue New basic value of the specified skill
	 * @throws MomException If this unit didn't previously have the specified skill (this method only modifies existing skills, not adds new ones)
	 */
	@Override
	public final void setDirectSkillValue (final AvailableUnit unit, final String unitSkillID, final int skillValue)
		throws MomException
	{
		log.trace ("Entering setDirectSkillValue: " + unit.getUnitID () + ", " + unitSkillID + ", " + skillValue);

		boolean found = false;
		final Iterator<UnitSkillAndValue> iter = unit.getUnitHasSkill ().iterator ();

		while ((!found) && (iter.hasNext ()))
		{
			final UnitSkillAndValue thisSkill = iter.next ();
			if (thisSkill.getUnitSkillID ().equals (unitSkillID))
			{
				found = true;
				thisSkill.setUnitSkillValue (skillValue);
			}
		}

		if (!found)
			throw new MomException ("setDirectSkillValue: Unit " + unit.getUnitID () + " does not have skill " + unitSkillID + " and so cannot set its value to " + skillValue);

		log.trace ("Exiting setDirectSkillValue");
	}
}