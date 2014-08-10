package momime.common.utils;

/**
 * Different things that can give a unit attribute values
 *
 * Defined so we can colour them separately on the Unit Info screen
 */
public enum MomUnitAttributeComponent
{
	/** Total of all the below (this is all the server ever uses) */
	ALL,

	/** Basic value defined in the server XML */
	BASIC,

	/** Bonus from normal unit having magical, mithril or adamantium weapons */
	WEAPON_GRADE,

	/** Bonus from normal unit or hero being experienced */
    EXPERIENCE,

    /** Bonus from the types of hero skills that simply add to an attribute, e.g. Might +5 simply adds +5 to melee attack strength */
    HERO_SKILLS,

    /** Bonus from combat area effects, including CAE-type spells, e.g. node auras or prayer */
    COMBAT_AREA_EFFECTS;
}
