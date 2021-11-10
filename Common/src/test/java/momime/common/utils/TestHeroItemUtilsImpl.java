package momime.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import momime.common.messages.NumberedHeroItem;

/**
 * Tests the HeroItemUtilsImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestHeroItemUtilsImpl
{
	/**
	 * Tests the findHeroItemURN method on a heroItem that does exist
	 */
	@Test
	public final void testFindHeroItemURN_Exists ()
	{
		final List<NumberedHeroItem> heroItems = new ArrayList<NumberedHeroItem> ();
		for (int n = 1; n <= 3; n++)
		{
			final NumberedHeroItem heroItem = new NumberedHeroItem ();
			heroItem.setHeroItemURN (n);
			heroItems.add (heroItem);
		}

		final HeroItemUtilsImpl utils = new HeroItemUtilsImpl ();
		assertEquals (2, utils.findHeroItemURN (2, heroItems).getHeroItemURN ());
	}

	/**
	 * Tests the findHeroItemURN method on a heroItem that doesn't exist
	 */
	@Test
	public final void testFindHeroItemURN_NotExists ()
	{
		final List<NumberedHeroItem> heroItems = new ArrayList<NumberedHeroItem> ();
		for (int n = 1; n <= 3; n++)
		{
			final NumberedHeroItem heroItem = new NumberedHeroItem ();
			heroItem.setHeroItemURN (n);
			heroItems.add (heroItem);
		}

		final HeroItemUtilsImpl utils = new HeroItemUtilsImpl ();
		assertNull (utils.findHeroItemURN (4, heroItems));
	}
}