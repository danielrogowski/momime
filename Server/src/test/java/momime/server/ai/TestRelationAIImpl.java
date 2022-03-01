package momime.server.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import momime.common.database.CommonDatabase;
import momime.common.database.Pick;
import momime.common.messages.PlayerPick;
import momime.common.utils.PlayerPickUtils;

/**
 * Tests the RelationAIImpl class 
 */
@ExtendWith(MockitoExtension.class)
public final class TestRelationAIImpl
{
	/**
	 * Tests the calculateAlignment method to get a positive score
	 */
	@Test
	public final void testCalculateAlignment_Positive ()
	{
		// Mock database
		final Pick positivePick = new Pick ();
		positivePick.setPickID ("MB01");
		positivePick.setPickAlignment (1);
		
		final Pick negativePick = new Pick ();
		negativePick.setPickID ("MB02");
		negativePick.setPickAlignment (-1);

		final Pick neutralPick = new Pick ();
		neutralPick.setPickID ("MB03");
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getPick ()).thenReturn (Arrays.asList (positivePick, negativePick, neutralPick));
		
		// List of picks
		final PlayerPick positivePickCount = new PlayerPick ();
		positivePickCount.setPickID ("MB01");
		positivePickCount.setOriginalQuantity (5);

		final PlayerPick negativePickCount = new PlayerPick ();
		negativePickCount.setPickID ("MB02");
		negativePickCount.setOriginalQuantity (3);
		
		final PlayerPick neutralPickCount = new PlayerPick ();
		neutralPickCount.setPickID ("MB03");
		neutralPickCount.setOriginalQuantity (9);
		
		final List<PlayerPick> picks = Arrays.asList (positivePickCount, negativePickCount, neutralPickCount);
		
		// Run method
		assertEquals (2, new RelationAIImpl ().calculateAlignment (picks, db));
	}

	/**
	 * Tests the calculateAlignment method to get a negative score
	 */
	@Test
	public final void testCalculateAlignment_Negative ()
	{
		// Mock database
		final Pick positivePick = new Pick ();
		positivePick.setPickID ("MB01");
		positivePick.setPickAlignment (1);
		
		final Pick negativePick = new Pick ();
		negativePick.setPickID ("MB02");
		negativePick.setPickAlignment (-1);

		final Pick neutralPick = new Pick ();
		neutralPick.setPickID ("MB03");
		
		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getPick ()).thenReturn (Arrays.asList (positivePick, negativePick, neutralPick));
		
		// List of picks
		final PlayerPick positivePickCount = new PlayerPick ();
		positivePickCount.setPickID ("MB01");
		positivePickCount.setOriginalQuantity (5);

		final PlayerPick negativePickCount = new PlayerPick ();
		negativePickCount.setPickID ("MB02");
		negativePickCount.setOriginalQuantity (7);
		
		final PlayerPick neutralPickCount = new PlayerPick ();
		neutralPickCount.setPickID ("MB03");
		neutralPickCount.setOriginalQuantity (1);
		
		final List<PlayerPick> picks = Arrays.asList (positivePickCount, negativePickCount, neutralPickCount);
		
		// Run method
		assertEquals (-2, new RelationAIImpl ().calculateAlignment (picks, db));
	}
	
	/**
	 * Tests the calculateBaseRelation method on Freya and Oberic's picks
	 */
	@Test
	public final void testCalculateBaseRelation_Freya_Oberic ()
	{
		// Mock database
		final Pick chaosBook = new Pick ();
		chaosBook.setPickID ("MB03");
		chaosBook.setPickAlignment (-1);
		chaosBook.getBookImageFile ().add ("C");
		
		final Pick natureBook = new Pick ();
		natureBook.setPickID ("MB04");
		natureBook.setPickAlignment (1);
		natureBook.getBookImageFile ().add ("N");

		final CommonDatabase db = mock (CommonDatabase.class);
		when (db.getPick ()).thenReturn (Arrays.asList (chaosBook, natureBook));
		
		// Freya
		final PlayerPick freyaNature = new PlayerPick ();
		freyaNature.setPickID ("MB04");
		freyaNature.setOriginalQuantity (10);

		final List<PlayerPick> freyaPicks = Arrays.asList (freyaNature);
		
		// Oberic
		final PlayerPick obericChaos = new PlayerPick ();
		obericChaos.setPickID ("MB03");
		obericChaos.setOriginalQuantity (5);
		
		final PlayerPick obericNature = new PlayerPick ();
		obericNature.setPickID ("MB04");
		obericNature.setOriginalQuantity (5);

		final List<PlayerPick> obericPicks = Arrays.asList (obericChaos, obericNature);
		
		// Number of picks
		final PlayerPickUtils playerPickUtils = mock (PlayerPickUtils.class);
		when (playerPickUtils.getOriginalQuantityOfPick (freyaPicks, "MB03")).thenReturn (0);
		when (playerPickUtils.getOriginalQuantityOfPick (freyaPicks, "MB04")).thenReturn (10);
		when (playerPickUtils.getOriginalQuantityOfPick (obericPicks, "MB03")).thenReturn (5);
		when (playerPickUtils.getOriginalQuantityOfPick (obericPicks, "MB04")).thenReturn (5);
		
		// Set up object to test
		final RelationAIImpl ai = new RelationAIImpl ();
		ai.setPlayerPickUtils (playerPickUtils);
		
		// Run method
		assertEquals (-15, ai.calculateBaseRelation (freyaPicks, obericPicks, db));
	}
}