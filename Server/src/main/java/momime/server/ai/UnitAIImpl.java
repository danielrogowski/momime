package momime.server.ai;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItemTypeAllowedBonus;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MemoryUnitHeroItemSlot;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.UnitDamage;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.UnitUtils;
import momime.server.database.HeroItemBonusSvr;
import momime.server.database.HeroItemSlotTypeSvr;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.UnitSkillSvr;
import momime.server.database.UnitSvr;
import momime.server.utils.UnitSkillDirectAccess;

/**
 * Methods for AI players evaluating the strength of units
 */
public final class UnitAIImpl
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (UnitAIImpl.class);

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Unit skill values direct access */
	private UnitSkillDirectAccess unitSkillDirectAccess;
	
	/**
	 * @param xu Unit to calculate value for
	 * @param db Lookup lists built over the XML database
	 * @return Value AI estimates for the quality, usefulness and effectiveness of this unit
	 * @throws MomException If we hit any problems reading unit skill values
	 * @throws RecordNotFoundException If the unit has a skill that we can't find in the database
	 */
	final int calculateUnitRating (final ExpandedUnitDetails xu, final ServerDatabaseEx db)
		throws MomException, RecordNotFoundException
	{		
		log.trace ("Entering calculateUnitRating: " + xu.getDebugIdentifier ());
		
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
			
			final UnitSkillSvr skillDef = db.findUnitSkill (unitSkillID, "calculateUnitRating");
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
		
		log.trace ("Exiting calculateUnitRating = " + total);
		return total;
	}
	
	/**
	 * @param unit Unit to calculate value for
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Value AI estimates for the current quality, usefulness and effectiveness of this unit
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public final int calculateUnitCurrentRating (final AvailableUnit unit, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering calculateUnitCurrentRating: " + unit.getUnitID () + " owner by player ID " + unit.getOwningPlayerID ());
		
		final int rating = calculateUnitRating (getUnitUtils ().expandUnitDetails (unit, null, null, null, players, mem, db), db);
		
		log.trace ("Exiting calculateUnitCurrentRating = " + rating);
		return rating;
	}
	
	/**
	 * @param bonus Hero item bonus to evaluate
	 * @return Value AI estimates for how good of a hero item bonus this is 
	 */
	final int calculateHeroItemBonusRating (final HeroItemBonusSvr bonus)
	{
		log.trace ("Entering calculateHeroItemBonusRating: " + bonus.getHeroItemBonusID ());

		final int rating;
		
		if (bonus.getHeroItemBonusStat ().isEmpty ())
			rating = 2;
		else
			rating = bonus.getHeroItemBonusStat ().stream ().mapToInt (s -> (s.getUnitSkillValue () == null) ? 2 : s.getUnitSkillValue ()).sum ();
		
		log.trace ("Exiting calculateHeroItemBonusRating = " + rating);
		return rating;
	}

	/**
	 * @param item Hero item to evaluate
	 * @param db Lookup lists built over the XML database
	 * @return Value AI estimates for how good of a hero item this is
	 * @throws RecordNotFoundException If the item has a bonus property that we can't find in the database 
	 */
	final int calculateHeroItemRating (final NumberedHeroItem item, final ServerDatabaseEx db) throws RecordNotFoundException
	{
		log.trace ("Entering calculateHeroItemRating: Item URN " + item.getHeroItemURN () + ", name " + item.getHeroItemName ());

		int rating = 0;
		for (final HeroItemTypeAllowedBonus bonus : item.getHeroItemChosenBonus ())
			rating = rating + calculateHeroItemBonusRating (db.findHeroItemBonus (bonus.getHeroItemBonusID (), "calculateUnitPotentialRating"));
		
		log.trace ("Exiting calculateHeroItemRating = " + rating);
		return rating;
	}

	/**
	 * @param unit Unit to calculate value for
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Value AI estimates for the quality, usefulness and effectiveness that this unit has the potential to become
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	public final int calculateUnitPotentialRating (final AvailableUnit unit, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		log.trace ("Entering calculateUnitCurrentRating: " + unit.getUnitID () + " owner by player ID " + unit.getOwningPlayerID ());
		
		// Since MoM is single threaded (with respect to each session) we can temporarily fiddle with the existing unit details, then put it back the way it was afterwards
		final int experience = getUnitSkillDirectAccess ().getDirectSkillValue (unit.getUnitHasSkill (), CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE);
		if (experience >= 0)
			getUnitSkillDirectAccess ().setDirectSkillValue (unit, CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE, Integer.MAX_VALUE);
		
		final List<UnitDamage> unitDamage;
		final List<NumberedHeroItem> heroItems;
		final MemoryUnit mu;
		if (unit instanceof MemoryUnit)
		{
			mu = (MemoryUnit) unit;
			final UnitSvr unitDef = db.findUnit (unit.getUnitID (), "calculateUnitPotentialRating");
			
			unitDamage = new ArrayList<UnitDamage> ();
			unitDamage.addAll (mu.getUnitDamage ());
			mu.getUnitDamage ().clear ();
			
			heroItems = new ArrayList<NumberedHeroItem> ();
			for (int slotNumber = 0; slotNumber < mu.getHeroItemSlot ().size (); slotNumber++)
			{
				final MemoryUnitHeroItemSlot slot = mu.getHeroItemSlot ().get (slotNumber);
				heroItems.add (slot.getHeroItem ());
				
				// Is the item in this slot good enough already?
				if ((slotNumber < unitDef.getHeroItemSlot ().size ()) && ((slot.getHeroItem () == null) || (calculateHeroItemRating (slot.getHeroItem (), db) < 6)))
				{
					final HeroItemSlotTypeSvr slotType = db.findHeroItemSlotType (unitDef.getHeroItemSlot ().get (slotNumber).getHeroItemSlotTypeID (), "calculateUnitPotentialRating");
					
					if (slotType.getBasicHeroItemForAiRatingItemTypeID () != null)
					{
						// It needs to be a numberedHeroItem, so take a copy of the important details
						final NumberedHeroItem numberedItem = new NumberedHeroItem ();
						numberedItem.setHeroItemTypeID (slotType.getBasicHeroItemForAiRatingItemTypeID ());
						numberedItem.getHeroItemChosenBonus ().addAll (slotType.getBasicHeroItemForAiRatingChosenBonus ());
						
						slot.setHeroItem (numberedItem);
					}
				}
			}
		}
		else
		{
			mu = null;
			unitDamage = null;
			heroItems = null;
		}

		// Now calculate its rating
		final int rating = calculateUnitRating (getUnitUtils ().expandUnitDetails (unit, null, null, null, players, mem, db), db);
		
		// Now put everything back the way it was
		if (experience >= 0)
			getUnitSkillDirectAccess ().setDirectSkillValue (unit, CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE, experience);
		
		if (mu != null)
		{
			mu.getUnitDamage ().addAll (unitDamage);

			for (int slotNumber = 0; slotNumber < mu.getHeroItemSlot ().size (); slotNumber++)
				mu.getHeroItemSlot ().get (slotNumber).setHeroItem (heroItems.get (slotNumber));
		}
		
		log.trace ("Exiting calculateUnitCurrentRating = " + rating);
		return rating;
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
	 * @return Unit skill values direct access
	 */
	public final UnitSkillDirectAccess getUnitSkillDirectAccess ()
	{
		return unitSkillDirectAccess;
	}

	/**
	 * @param direct Unit skill values direct access
	 */
	public final void setUnitSkillDirectAccess (final UnitSkillDirectAccess direct)
	{
		unitSkillDirectAccess = direct;
	}
}