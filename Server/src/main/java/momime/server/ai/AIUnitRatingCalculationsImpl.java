package momime.server.ai;

import java.util.ArrayList;
import java.util.List;

import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItemSlotType;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Unit;
import momime.common.database.UnitSkill;
import momime.common.messages.AvailableUnit;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MemoryUnitHeroItemSlot;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.UnitDamage;
import momime.common.messages.WizardState;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.common.utils.ResourceValueUtils;
import momime.server.utils.UnitSkillDirectAccess;

/**
 * Underlying methods that the AI uses to calculate ratings about how good units are
 */
public final class AIUnitRatingCalculationsImpl implements AIUnitRatingCalculations
{
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** Unit skill values direct access */
	private UnitSkillDirectAccess unitSkillDirectAccess;
	
	/** Methods that the AI uses to calculate ratings about how good hero items are */
	private AIHeroItemRatingCalculations aiHeroItemRatingCalculations;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
	/** Methods for working with wizardIDs */
	private PlayerKnowledgeUtils playerKnowledgeUtils;
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/**
	 * @param xu Unit to calculate value for
	 * @param db Lookup lists built over the XML database
	 * @return Value AI estimates for the quality, usefulness and effectiveness of this unit
	 * @throws MomException If we hit any problems reading unit skill values
	 * @throws RecordNotFoundException If the unit has a skill that we can't find in the database
	 */
	final int calculateUnitRating (final ExpandedUnitDetails xu, final CommonDatabase db)
		throws MomException, RecordNotFoundException
	{		
		// Units with no attacks whatsoever (settlers) aren't even considered to be combat units
		int total = 0;
		if ((xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_MELEE_ATTACK)) ||
			(xu.hasModifiedSkill (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_RANGED_ATTACK)))
		{
			// Add 10% for each figure over 1 that the unit has
			double multipliers = ((double) xu.calculateHitPointsRemaining ()) / ((double) xu.getModifiedSkillValue (CommonDatabaseConstants.UNIT_ATTRIBUTE_ID_HIT_POINTS));
			multipliers = ((multipliers - 1d) / 10d) + 1d;
	
			// Go through all skills totalling up additive and multiplicative bonuses from skills
			for (final String unitSkillID : xu.listModifiedSkillIDs ())
			{
				Integer value = xu.getModifiedSkillValue (unitSkillID);
				if ((value == null) || (value == 0))
					value = 1;
				
				final UnitSkill skillDef = db.findUnitSkill (unitSkillID, "calculateUnitRating");
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
			
			// Apply multiplicative modifiers
			total = (int) (total * multipliers);
		}
		
		return total;
	}
	
	/**
	 * @param unit Unit to calculate value for
	 * @param xu Expanded unit details to calculate value for if already worked out, otherwise can pass null
	 * @param players Players list
	 * @param mem Known overland terrain, units, buildings and so on
	 * @param db Lookup lists built over the XML database
	 * @return Value AI estimates for the current quality, usefulness and effectiveness of this unit
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final int calculateUnitCurrentRating (final AvailableUnit unit, final ExpandedUnitDetails xu, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		final int rating = calculateUnitRating ((xu != null) ? xu : getExpandUnitDetails ().expandUnitDetails (unit, null, null, null, players, mem, db), db);
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
	@Override
	public final int calculateUnitPotentialRating (final AvailableUnit unit, final List<PlayerServerDetails> players, final FogOfWarMemory mem, final CommonDatabase db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
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
			final Unit unitDef = db.findUnit (unit.getUnitID (), "calculateUnitPotentialRating");
			
			unitDamage = new ArrayList<UnitDamage> ();
			unitDamage.addAll (mu.getUnitDamage ());
			mu.getUnitDamage ().clear ();
			
			heroItems = new ArrayList<NumberedHeroItem> ();
			for (int slotNumber = 0; slotNumber < mu.getHeroItemSlot ().size (); slotNumber++)
			{
				final MemoryUnitHeroItemSlot slot = mu.getHeroItemSlot ().get (slotNumber);
				heroItems.add (slot.getHeroItem ());
				
				// Is the item in this slot good enough already?
				if ((slotNumber < unitDef.getHeroItemSlot ().size ()) && ((slot.getHeroItem () == null) || (getAiHeroItemRatingCalculations ().calculateHeroItemRating (slot.getHeroItem (), db) < 6)))
				{
					final HeroItemSlotType slotType = db.findHeroItemSlotType (unitDef.getHeroItemSlot ().get (slotNumber), "calculateUnitPotentialRating");
					
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
		final int rating = calculateUnitRating (getExpandUnitDetails ().expandUnitDetails (unit, null, null, null, players, mem, db), db);
		
		// Now put everything back the way it was
		if (experience >= 0)
			getUnitSkillDirectAccess ().setDirectSkillValue (unit, CommonDatabaseConstants.UNIT_SKILL_ID_EXPERIENCE, experience);
		
		if (mu != null)
		{
			mu.getUnitDamage ().addAll (unitDamage);

			for (int slotNumber = 0; slotNumber < mu.getHeroItemSlot ().size (); slotNumber++)
				mu.getHeroItemSlot ().get (slotNumber).setHeroItem (heroItems.get (slotNumber));
		}
		
		return rating;
	}
	
	/**
	 * If a unit stack is controlled by a wizard, bump up the rating of the unit stack to account for spells the wizard may cast to change the outcome of the battle
	 * 
	 * @param ratings Unit stack being evaluated
	 * @param players Players list
	 * @param wizards True wizard details list
	 * @return Bonus to add to unit stack rating
	 * @throws PlayerNotFoundException If the player who owns the unit stack cannot be found
	 * @throws RecordNotFoundException If the wizard who owns the unit stack cannot be found
	 */
	@Override
	public final int ratingBonusFromPowerBase (final AIUnitsAndRatings ratings, final List<PlayerServerDetails> players, final List<KnownWizardDetails> wizards)
		throws PlayerNotFoundException, RecordNotFoundException
	{
		int bonus = 0;
		if (ratings.size () > 0)
		{
			final int playerID = ratings.get (0).getUnit ().getOwningPlayerID ();
			
			final KnownWizardDetails wizardDetails = getKnownWizardUtils ().findKnownWizardDetails (wizards, playerID, "ratingBonusFromPowerBase");
			
			if ((getPlayerKnowledgeUtils ().isWizard (wizardDetails.getWizardID ())) && (wizardDetails.getWizardState () == WizardState.ACTIVE))
			{
				final PlayerServerDetails player = getMultiplayerSessionServerUtils ().findPlayerWithID (players, playerID, "ratingBonusFromPowerBase");
				final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
				
				final int powerBase = getResourceValueUtils ().findAmountPerTurnForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER);
				bonus = powerBase * 15;
			}
		}
		return bonus;
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

	/**
	 * @return Methods that the AI uses to calculate ratings about how good hero items are
	 */
	public final AIHeroItemRatingCalculations getAiHeroItemRatingCalculations ()
	{
		return aiHeroItemRatingCalculations;
	}

	/**
	 * @param calc Methods that the AI uses to calculate ratings about how good hero items are
	 */
	public final void setAiHeroItemRatingCalculations (final AIHeroItemRatingCalculations calc)
	{
		aiHeroItemRatingCalculations = calc;
	}

	/**
	 * @return Resource value utils
	 */
	public final ResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param utils Resource value utils
	 */
	public final void setResourceValueUtils (final ResourceValueUtils utils)
	{
		resourceValueUtils = utils;
	}

	/**
	 * @return Server only helper methods for dealing with players in a session
	 */
	public final MultiplayerSessionServerUtils getMultiplayerSessionServerUtils ()
	{
		return multiplayerSessionServerUtils;
	}

	/**
	 * @param obj Server only helper methods for dealing with players in a session
	 */
	public final void setMultiplayerSessionServerUtils (final MultiplayerSessionServerUtils obj)
	{
		multiplayerSessionServerUtils = obj;
	}

	/**
	 * @return Methods for working with wizardIDs
	 */
	public final PlayerKnowledgeUtils getPlayerKnowledgeUtils ()
	{
		return playerKnowledgeUtils;
	}

	/**
	 * @param k Methods for working with wizardIDs
	 */
	public final void setPlayerKnowledgeUtils (final PlayerKnowledgeUtils k)
	{
		playerKnowledgeUtils = k;
	}

	/**
	 * @return Methods for finding KnownWizardDetails from the list
	 */
	public final KnownWizardUtils getKnownWizardUtils ()
	{
		return knownWizardUtils;
	}

	/**
	 * @param k Methods for finding KnownWizardDetails from the list
	 */
	public final void setKnownWizardUtils (final KnownWizardUtils k)
	{
		knownWizardUtils = k;
	}
}