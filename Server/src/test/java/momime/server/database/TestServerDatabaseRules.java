package momime.server.database;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import momime.common.database.CommonDatabase;
import momime.common.database.DamageType;
import momime.common.database.DamageTypeImmunity;
import momime.common.database.NegatedBySkill;
import momime.common.database.RangedAttackTypeEx;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.UnitEx;
import momime.common.database.UnitSkill;
import momime.common.database.UnitSkillEx;
import momime.server.ServerTestData;

/**
 * Dumps rules out from the XML so its easier to check them against the MoM wiki
 */
public final class TestServerDatabaseRules extends ServerTestData
{
	/**
	 * Dumps out info about damage types
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void dumpDamageTypeInfo () throws Exception {
		final CommonDatabase db = loadServerDatabase ();
		
		for (final DamageType dt : db.getDamageType ())
		{
			System.out.println ("");
			
			final StringBuilder s = new StringBuilder (dt.getDamageTypeID ());
			
			if (dt.getEnhancedVersion () != null)
				s.append (" > " + dt.getEnhancedVersion ());
			
			s.append (" (" + dt.getStoredDamageTypeID () + ") - \"" +
				dt.getDamageTypeName ().get (0).getText () + "\" - " + dt.getDamageTypeDescription ());

			System.out.println (s.toString ());
			
			final StringBuilder s2 = new StringBuilder ();
			for (final DamageTypeImmunity imm : dt.getDamageTypeImmunity ())
			{
				if (s2.length () > 0)
					s2.append (", ");
				
				final UnitSkillEx unitSkill = db.findUnitSkill (imm.getUnitSkillID (), "dumpDamageTypeInfo");
				s2.append (imm.getUnitSkillID () + " " + unitSkill.getUnitSkillDescription ().get (0).getText ());
				
				if (imm.getBoostsDefenceTo () == null)
					s2.append (" (immune)");
				else
					s2.append (" (" + imm.getBoostsDefenceTo () + " def)");
			}

			if (s2.length () > 0)
				System.out.println ("  Blocked by: " + s2);
			
			// Find skills that do this type of damage
			final StringBuilder s3 = new StringBuilder ();
			for (final UnitSkillEx sk : db.getUnitSkills ())
				if (dt.getDamageTypeID ().equals (sk.getDamageTypeID ()))
				{
					if (s3.length () > 0)
						s3.append (", ");

					s3.append (sk.getUnitSkillID () + " " + sk.getUnitSkillDescription ().get (0).getText ());
				}
			
			if (s3.length () > 0)
				System.out.println ("  Dealt by skills: " + s3);
			
			// Find spells that do this type of damage
			final StringBuilder s4 = new StringBuilder ();
			for (final Spell sp : db.getSpell ())
				if (dt.getDamageTypeID ().equals (sp.getAttackSpellDamageTypeID ()))
				{
					if (s4.length () > 0)
						s4.append (", ");

					s4.append (sp.getSpellID () + " " + sp.getSpellName ().get (0).getText ());
				}
			
			if (s4.length () > 0)
				System.out.println ("  Dealt by spells: " + s4);
			
			// Find RATs that do this type of damage
			for (final RangedAttackTypeEx rat : db.getRangedAttackTypes ())
				if (dt.getDamageTypeID ().equals (rat.getDamageTypeID ()))
				{
					final StringBuilder s5 = new StringBuilder ();
					
					for (final UnitEx unitDef : db.getUnits ())
						if (rat.getRangedAttackTypeID ().equals (unitDef.getRangedAttackType ()))
						{
							if (s5.length () > 0)
								s5.append (", ");
							
							if (unitDef.getHeroName ().size () > 0)
								s5.append (unitDef.getHeroName ().get (0).getHeroNameLang ().get (0).getText ());
							else
								s5.append (unitDef.getUnitName ().get (0).getText ());
						}

					if (s5.length () > 0)
						System.out.println ("  Dealt by " + rat.getRangedAttackTypeID () + " " + rat.getRangedAttackTypeDescription ().get (0).getText () + " from " + s5);
				}
		}
		
		// Also output what immunities block unit skills, but really we only want to output which unit curses are blocked
		// otherwise we get a ton of pointless output about e.g. one version of magic immunity being blocked by another version of magic immunity
		final Set<String> curseSkillIDs = new HashSet<String> ();
		db.getSpell ().stream ().filter (s -> s.getSpellBookSectionID () == SpellBookSectionID.UNIT_CURSES).forEach
			(s -> s.getUnitSpellEffect ().forEach (e -> curseSkillIDs.add (e.getUnitSkillID ())));
		
		for (final UnitSkill unitSkill : db.getUnitSkills ())
			if (curseSkillIDs.contains (unitSkill.getUnitSkillID ()))
			{
				System.out.println ("");
				System.out.println (unitSkill.getUnitSkillID () + " \"" + unitSkill.getUnitSkillDescription ().get (0).getText () + "\"");

				final StringBuilder s2 = new StringBuilder ();
				for (final NegatedBySkill negatedBy : unitSkill.getNegatedBySkill ())
				{
					if (s2.length () > 0)
						s2.append (", ");
					
					final UnitSkillEx negatedBySkill = db.findUnitSkill (negatedBy.getNegatedBySkillID (), "dumpDamageTypeInfo");
					s2.append (negatedBy.getNegatedBySkillID () + " " + negatedBySkill.getUnitSkillDescription ().get (0).getText ());
				}

				if (s2.length () > 0)
					System.out.println ("  Blocked by: " + s2);
			}
	}
}