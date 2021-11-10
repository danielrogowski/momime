package momime.server.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import momime.common.database.HeroItemBonus;
import momime.common.database.HeroItemBonusStat;

/**
 * Tests the AIHeroItemRatingCalculationsImpl class
 */
public final class TestAIHeroItemRatingCalculationsImpl
{
	/**
	 * Tests the calculateHeroItemBonusRating method
	 */
	@Test
	public final void testCalculateHeroItemBonusRating ()
	{
		// Set up object to test
		final AIHeroItemRatingCalculationsImpl ai = new AIHeroItemRatingCalculationsImpl ();

		// No stat entries
		final HeroItemBonus bonus = new HeroItemBonus ();
		assertEquals (2, ai.calculateHeroItemBonusRating (bonus));
		
		// A +1 stat, a valueless stat, and a +5 stat
		final HeroItemBonusStat statOne = new HeroItemBonusStat ();
		statOne.setUnitSkillValue (1);
		
		final HeroItemBonusStat statTwo = new HeroItemBonusStat ();

		final HeroItemBonusStat statThree = new HeroItemBonusStat ();
		statThree.setUnitSkillValue (5);
		
		bonus.getHeroItemBonusStat ().add (statOne);
		bonus.getHeroItemBonusStat ().add (statTwo);
		bonus.getHeroItemBonusStat ().add (statThree);
	
		assertEquals (1+2+5, ai.calculateHeroItemBonusRating (bonus));
	}
}