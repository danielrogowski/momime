package momime.server.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.PlayerServerDetails;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItem;
import momime.common.database.HeroItemBonus;
import momime.common.database.HeroItemTypeAllowedBonus;
import momime.common.database.PickAndQuantity;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.UnitSetting;
import momime.common.database.UnitSkillAndValue;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.SpellCastType;
import momime.common.utils.SpellUtils;
import momime.server.knowledge.MomGeneralServerKnowledgeEx;

/**
 * Server only helper methods for dealing with hero items
 */
public final class HeroItemServerUtilsImpl implements HeroItemServerUtils
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (HeroItemServerUtilsImpl.class);

	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/** Spell utils */
	private SpellUtils spellUtils;
	
	/**
	 * @param item Hero item stats
	 * @param gsk General server knowledge
	 * @return Hero item with allocated URN
	 */
	@Override
	public final NumberedHeroItem createNumberedHeroItem (final HeroItem item, final MomGeneralServerKnowledgeEx gsk)
	{
		log.trace ("Entering createNumberedHeroItem: " + item.getHeroItemName ());
		
		final NumberedHeroItem numberedItem = new NumberedHeroItem ();
		numberedItem.setHeroItemURN (gsk.getNextFreeHeroItemURN ());
		gsk.setNextFreeHeroItemURN (gsk.getNextFreeHeroItemURN () + 1);

		numberedItem.setHeroItemName (item.getHeroItemName ());
		numberedItem.setHeroItemTypeID (item.getHeroItemTypeID ());
		numberedItem.setHeroItemImageNumber (item.getHeroItemImageNumber ());
		numberedItem.setSpellID (item.getSpellID ());
		numberedItem.setSpellChargeCount (item.getSpellChargeCount ());
		numberedItem.getHeroItemChosenBonus ().addAll (item.getHeroItemChosenBonus ());
		
		log.trace ("Exiting createNumberedHeroItem = " + numberedItem.getHeroItemURN ());
		return numberedItem;
	}
	
	/**
	 * @param player Player who wants to craft an item
	 * @param spell The spell being used to craft it (Enchant Item or Create Artifact)
	 * @param heroItem The details of the item they want to create
	 * @param unitSettings Unit settings from session description
	 * @param db Lookup lists built over the XML database
	 * @return Error message describing why they can't create the desired item, or null if all validation passes
	 * @throws RecordNotFoundException If player requested a bonus that doesn't exist
	 * @throws MomException If there any serious failures in logic
	 */
	@Override
	public final String validateHeroItem (final PlayerServerDetails player, final Spell spell, final HeroItem heroItem, final UnitSetting unitSettings, final CommonDatabase db)
		throws RecordNotFoundException, MomException
	{
		log.trace ("Entering validateHeroItem: Player ID " + player.getPlayerDescription ().getPlayerID ());

		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		
		// Either spell can be used to make any item, at any cost, so nothing to check there.
		// More what we need to check are the number of bonuses selected, the cost of selected bonuses,
		// any necessary number of books required for each bonus selected, and spell charges.
		String error = null;
		if ((unitSettings.getMaxHeroItemBonuses () != null) && (heroItem.getHeroItemChosenBonus ().size () > unitSettings.getMaxHeroItemBonuses ()))
			error = "Hero items may be imbued with a maximum of " + unitSettings.getMaxHeroItemBonuses () + " bonuses";
		else
		{
			// We can't have two bonuses that give a bonus to the same stat, e.g. we can't pick both +1 attack and +2 attack
			final List<String> bonusIDs = new ArrayList<String> ();
			final List<String> bonusSkillIDs = new ArrayList<String> ();
			
			for (final HeroItemTypeAllowedBonus bonus : heroItem.getHeroItemChosenBonus ())
			{
				final HeroItemBonus bonusDef = db.findHeroItemBonus (bonus.getHeroItemBonusID (), "validateHeroItem");
				
				if (bonusIDs.contains (bonus.getHeroItemBonusID ()))
					error = "Bonus " + bonusDef.getHeroItemBonusID () + " was chosen more than once";
				else
				{
					bonusIDs.add (bonus.getHeroItemBonusID ());
					
					if ((spell.getHeroItemBonusMaximumCraftingCost () > 0) && ((bonusDef.getBonusCraftingCost () == null) ||
						(bonusDef.getBonusCraftingCost () > spell.getHeroItemBonusMaximumCraftingCost ())))
						
						error = "Bonus " + bonusDef.getHeroItemBonusID () + " exceeds the maximum cost of " + spell.getHeroItemBonusMaximumCraftingCost () + " per bonus";
					else
						for (final PickAndQuantity prereq : bonusDef.getHeroItemBonusPrerequisite ())
							if (getPlayerPickUtils ().getQuantityOfPick (pub.getPick (), prereq.getPickID ()) < prereq.getQuantity ())
								error = "Bonus " + bonusDef.getHeroItemBonusID () + " requires at least " + prereq.getQuantity () + " picks in magic realm " + prereq.getPickID ();
					
					if ((error == null) && ((bonusDef.isAllowCombiningWithBonusesToSameStat () == null) || (!bonusDef.isAllowCombiningWithBonusesToSameStat ())))
						for (final UnitSkillAndValue bonusStat : bonusDef.getHeroItemBonusStat ())
							if (bonusSkillIDs.contains (bonusStat.getUnitSkillID ()))
								error = "More than one bonus was selected that gives a bonus to stat " + bonusStat.getUnitSkillID ();
							else
								bonusSkillIDs.add (bonusStat.getUnitSkillID ());
				}
			}
			
			if (error == null)
			{
				// Validate spell charges
				if (bonusIDs.contains (CommonDatabaseConstants.HERO_ITEM_BONUS_ID_SPELL_CHARGES))
				{
					// Valid chosen spell and number of charges
					if (heroItem.getSpellID () == null)
						error = "Hero items must specify a spell if the spell charges bonus was picked";
					else if (heroItem.getSpellChargeCount () == null)
						error = "Hero items must specify a number of spells if the spell charges bonus was picked";
					else if (heroItem.getSpellChargeCount () > unitSettings.getMaxHeroItemSpellCharges ())
						error = "Number of spell charges on the item was over the allowed maximum";
					
					if (error == null)
					{
						// Make sure the player actually knows the spell, and that its a combat spell
						final Spell spellDef = db.findSpell (heroItem.getSpellID (), "validateHeroItem");
						final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
						final SpellResearchStatus researchStatus = getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), heroItem.getSpellID ());
						
						if (researchStatus.getStatus () != SpellResearchStatusID.AVAILABLE)
							error = "You tried to imbue a spell that you haven't yet researched into a hero item";
						else if (!getSpellUtils ().spellCanBeCastIn (spellDef, SpellCastType.COMBAT))
							error = "You can only imbue combat spells into hero items";
					}
				}
				else
				{
					// Validate that there is no spell
					if (heroItem.getSpellID () != null)
						error = "Hero items shouldn't specify a spell if the spell charges bonus was not picked";
					else if (heroItem.getSpellChargeCount () != null)
						error = "Hero items shouldn't specify a number of spells if the spell charges bonus was not picked";
				}
			}
		}

		log.trace ("Entering validateHeroItem: " + error);
		return error;
	}

	/**
	 * @return Player pick utils
	 */
	public final PlayerPickUtils getPlayerPickUtils ()
	{
		return playerPickUtils;
	}

	/**
	 * @param utils Player pick utils
	 */
	public final void setPlayerPickUtils (final PlayerPickUtils utils)
	{
		playerPickUtils = utils;
	}

	/**
	 * @return Spell utils
	 */
	public final SpellUtils getSpellUtils ()
	{
		return spellUtils;
	}

	/**
	 * @param utils Spell utils
	 */
	public final void setSpellUtils (final SpellUtils utils)
	{
		spellUtils = utils;
	}
}