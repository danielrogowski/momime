package momime.client.ui.panels;

import momime.common.messages.NumberedHeroItem;

/**
 * The lower half of the unit info panel displays both unit skills and hero items and slots, so need a single
 * object to model all possible options so they can all go in a list together 
 */
public final class UnitSkillOrHeroItemSlot
{
	/** Filled in only if the row represents a unit skill or spell effect */
	private String unitSkillID;
	
	/** Filled in only if the row represents a unit skill or spell effect that takes a strength value parameter */
	private Integer unitSkillValue;
	
	/** Filled in only if the row represents the ability to cast a particular spell */
	private String spellID;
	
	/** Filled in only if the row represents a hero item slot that has no item in it */
	private String heroItemSlotTypeID;
	
	/** Filled in only if the row represents a hero item slot that has an item in it */
	private NumberedHeroItem heroItem;
	
	/** Whether to show the icon and text darkened on the unit info panel */
	private boolean darkened;

	/**
	 * @return Filled in only if the row represents a unit skill or spell effect
	 */
	public final String getUnitSkillID ()
	{
		return unitSkillID;
	}

	/**
	 * @param skillID Filled in only if the row represents a unit skill or spell effect
	 */
	public final void setUnitSkillID (final String skillID)
	{
		unitSkillID = skillID;
	}
	
	/**
	 * @return Filled in only if the row represents a unit skill or spell effect that takes a strength value parameter
	 */
	public final  Integer getUnitSkillValue ()
	{
		return unitSkillValue;
	}
	
	/**
	 * @param value Filled in only if the row represents a unit skill or spell effect that takes a strength value parameter
	 */
	public final void setUnitSkillValue (final Integer value)
	{
		unitSkillValue = value;
	}
	
	/**
	 * @return Filled in only if the row represents the ability to cast a particular spell
	 */
	public final String getSpellID ()
	{
		return spellID;
	}

	/**
	 * @param aSpellID Filled in only if the row represents the ability to cast a particular spell
	 */
	public final void setSpellID (final String aSpellID)
	{
		spellID = aSpellID;
	}
	
	/**
	 * @return Filled in only if the row represents a hero item slot that has no item in it
	 */
	public final String getHeroItemSlotTypeID ()
	{
		return heroItemSlotTypeID;
	}

	/**
	 * @param slotTypeID Filled in only if the row represents a hero item slot that has no item in it
	 */
	public final void setHeroItemSlotTypeID (final String slotTypeID)
	{
		heroItemSlotTypeID = slotTypeID;
	}
	
	/**
	 * @return Filled in only if the row represents a hero item slot that has an item in it
	 */
	public final NumberedHeroItem getHeroItem ()
	{
		return heroItem;
	}
	
	/**
	 * @param item Filled in only if the row represents a hero item slot that has an item in it
	 */
	public final void setHeroItem (final NumberedHeroItem item)
	{
		heroItem = item;
	}

	/**
	 * @return Whether to show the icon and text darkened on the unit info panel
	 */
	public final boolean isDarkened ()
	{
		return darkened;
	}

	/**
	 * @param d Whether to show the icon and text darkened on the unit info panel
	 */
	public final void setDarkened (final boolean d)
	{
		darkened = d;
	}
}