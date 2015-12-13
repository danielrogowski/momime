package momime.client.ui.renderer;

import momime.common.messages.MemoryUnit;

/**
 * Combat spells can either be cast from the wizard's own MP pool, the MP pool of some units or heroes (e.g. Archangels can cast life magic),
 * or the spell charges imbued into her items.  So potentially when we hit the spell button in combat, we could need the player to pick 1 from
 * 5 casting sources.  So this class models all the possible casting sources.
 */
public final class CastCombatSpellFrom
{
	/** The unit casting the spell, if casting from their own MP poor or a hero item; null if the wizard is casting the spell */
	private final MemoryUnit castingUnit; 
	
	/** Index into the list of fixed spells that this unit can cast, e.g. Giant Spiders casting Web; null for all other casting types */
	private final Integer fixedSpellNumber;
	
	/** Slot number of the item the hero is casting a spell charge from; null for all other casting types */ 
	private final Integer heroItemSlotNumber;

	/**
	 * @param aCastingUnit The unit casting the spell, if casting from their own MP poor or a hero item; null if the wizard is casting the spell
	 * @param aFixedSpellNumber Index into the list of fixed spells that this unit can cast, e.g. Giant Spiders casting Web; null for all other casting types
	 * @param aHeroItemSlotNumber Slot number of the item the hero is casting a spell charge from; null for all other casting types
	 */
	public CastCombatSpellFrom (final MemoryUnit aCastingUnit, final Integer aFixedSpellNumber, final Integer aHeroItemSlotNumber)
	{
		castingUnit = aCastingUnit;
		fixedSpellNumber = aFixedSpellNumber;
		heroItemSlotNumber = aHeroItemSlotNumber;
	}

	/**
	 * @return The unit casting the spell, if casting from their own MP poor or a hero item; null if the wizard is casting the spell
	 */
	public final MemoryUnit getCastingUnit () 
	{
		return castingUnit;
	}

	/**
	 * @return Index into the list of fixed spells that this unit can cast, e.g. Giant Spiders casting Web; null for all other casting types
	 */
	public final Integer getFixedSpellNumber ()
	{
		return fixedSpellNumber;
	}
	
	/**
	 * @return Slot number of the item the hero is casting a spell charge from; null if the wizard is casting the spell or the hero is casting from their MP pool and not an item
	 */ 
	public final Integer getHeroItemSlotNumber ()
	{
		return heroItemSlotNumber;
	}
}