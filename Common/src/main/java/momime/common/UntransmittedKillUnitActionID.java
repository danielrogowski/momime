package momime.common;

/**
 * KillUnitActionID has now been phased out of the XSDs and network messages, so all possible values are defined here.
 */
public enum UntransmittedKillUnitActionID
{
	/** Remove unit entirely; there is no possible means to bring it back in future */
	FREE,
	
	/** Unit routed and abanoned our cause because we failed to pay/feed them - effectivley the same as FREE since there is no way it can come back */
	UNIT_LACK_OF_PRODUCTION,
	
	/** Hero dismissed from service; they return to the pool of available heroes (at status GENERATED) and available to be resummoned later */
	HERO_DIMISSED_VOLUNTARILY,

	/** Hero abanoned our cause because we failed to pay/feed them - effectively the same as HERO_DIMISSED_VOLUNTARILY - they are available to summon again */
	HERO_LACK_OF_PRODUCTION,
	
	/** We lost sight of an enemy unit on the overland map - again effectively the same as FREE */
	VISIBLE_AREA_CHANGED,

	/**Unit killed by combat damage; this may set the unit to DEAD if it can be later raised/animated during combat, or heroes resurrected after combat, or
	 * may be removed entirely like FREE, depending on the type of unit, whether the player is involved in the combat and whether it is our unit or somebody else's */
	COMBAT_DAMAGE;
}