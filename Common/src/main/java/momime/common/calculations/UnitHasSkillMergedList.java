package momime.common.calculations;

import java.util.ArrayList;

import momime.common.database.v0_9_5.UnitHasSkill;

/**
 * UnitUtils.mergeSpellEffectsIntoSkillList adds into the basic skills list defined against a unit any skills granted from spells, such as Lionheart
 * So that there's no ambiguity between whether List<UnitHasSkill> includes spell skills or not, any list that has had spell skills merged into it is declared as this
 */
public final class UnitHasSkillMergedList extends ArrayList<UnitHasSkill>
{
	/** Unique value for serialization */
	private static final long serialVersionUID = 4732654481578611076L;
}
