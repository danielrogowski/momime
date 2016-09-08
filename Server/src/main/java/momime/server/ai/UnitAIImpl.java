package momime.server.ai;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.utils.ExpandedUnitDetails;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.UnitSkillSvr;

/**
 * Methods for AI players evaluating the strength of units
 */
public final class UnitAIImpl
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (UnitAIImpl.class);

	/**
	 * @param xu Unit to calculate value for
	 * @param db Lookup lists built over the XML database
	 * @return Value AI estimates for the quality, usefulness and effectiveness of this unit
	 * @throws MomException If we hit any problems reading unit skill values
	 * @throws RecordNotFoundException If the unit has a skill that we can't find in the database
	 */
	public final int calculateUnitCurrentRating (final ExpandedUnitDetails xu, final ServerDatabaseEx db)
		throws MomException, RecordNotFoundException
	{		
		log.trace ("Entering calculateUnitCurrentRating: " + xu.getDebugIdentifier ());
		
		// Add 10% for each figure over 1 that the unit has
		double multipliers = ((double) xu.calculateHitPointsRemaining ()) / ((double) xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS));
		multipliers = ((multipliers - 1d) / 10d) + 1d;

		// Go through all skills totalling up additive and multiplicative bonuses from skills
		int total = 0;
		for (final String unitSkillID : xu.listModifiedSkillIDs ())
		{
			Integer value = xu.getModifiedSkillValue (unitSkillID);
			if ((value == null) || (value == 0))
				value = 1;
			
			final UnitSkillSvr skillDef = db.findUnitSkill (unitSkillID, "calculateUnitCurrentRating");
			if (skillDef.getAiRatingMultiplicative () != null)
				multipliers = multipliers * skillDef.getAiRatingMultiplicative ();
			
			else if (skillDef.getAiRatingAdditive () != null)
			{
				if ((skillDef.getAiRatingDiminishesAfter () == null) || (value <= skillDef.getAiRatingDiminishesAfter ()))
					total = total + (value * skillDef.getAiRatingAdditive ());
				else
				{
					// Diminishing skill - add on the fixed part
					total = total + (skillDef.getAiRatingDiminishesAfter () * skillDef.getAiRatingAdditive ());
					int leftToAdd = Math.min (value - skillDef.getAiRatingDiminishesAfter (), skillDef.getAiRatingAdditive () - 1);
					for (int n = 1; n <= leftToAdd; n++)
						total = total + (skillDef.getAiRatingAdditive () - n);
				}
			}
		}

		// If the unit has no attacks whatsoever (settlers) then severaly hamper its rating since its clearly not a combat unit,
		// and this is supposed to be an estimate of units' capability in combat.
		// This is to stop Troll Settlers getting a massive rating because of their 40 HP
		if ((!xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK)) &&
			(!xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK)))
			
			multipliers = multipliers * 0.2;
		
		// Apply multiplicative modifiers
		total = (int) (total * multipliers);
		
		log.trace ("Exiting calculateUnitCurrentRating = " + total);
		return total;
	}
}