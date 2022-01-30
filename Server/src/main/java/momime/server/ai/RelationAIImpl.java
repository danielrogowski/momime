package momime.server.ai;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import momime.common.database.CommonDatabase;
import momime.common.database.Pick;
import momime.common.messages.PlayerPick;
import momime.common.utils.PlayerPickUtils;

/**
 * For calculating relation scores between two wizards
 */
public final class RelationAIImpl implements RelationAI
{
	/** Positive base relation for each book we share in common */
	private final static int SHARED_BOOK = 3;
	
	/** Negative base relation for the magnitude of alignment difference */
	private final static int ALIGNMENT_DIFFERENCE = 5;
	
	/** Fixed base score added to all base relations */
	private final static int FIXED_BASE_SCORE = 20;
	
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/**
	 * @param picks Wizard's spell book picks
	 * @param db Lookup lists built over the XML database
	 * @return Wizard's alignment; positive for good, negative for evil
	 */
	final int calculateAlignment (final List<PlayerPick> picks, final CommonDatabase db)
	{
		final Map<String, Integer> alignmentPicks = db.getPick ().stream ().filter (p -> (p.getPickAlignment () != null) && (p.getPickAlignment () != 0)).collect
			(Collectors.toMap (p -> p.getPickID (), p -> p.getPickAlignment ()));
		
		int alignment = 0;
		for (final PlayerPick pick : picks)
		{
			final Integer thisAlignment = alignmentPicks.get (pick.getPickID ());
			if (thisAlignment != null)
				alignment = alignment + (thisAlignment * pick.getOriginalQuantity ());
		}
		
		return alignment;
	}
	
	/**
	 * @param firstPicks First wizard's picks
	 * @param secondPicks Second wizard's picks
	 * @param db Lookup lists built over the XML database
	 * @return Natural relation between the two wizards based on their spell books (wiki calls this startingRelation)
	 */
	@Override
	public final int calculateBaseRelation (final List<PlayerPick> firstPicks, final List<PlayerPick> secondPicks, final CommonDatabase db)
	{
		// Go through each kind of book, and see how many are common to both lists
		int commonBookCount = 0;
		
		for (final Pick pick : db.getPick ())
			if (!pick.getBookImageFile ().isEmpty ())
			{
				final int firstCount = getPlayerPickUtils ().getOriginalQuantityOfPick (firstPicks, pick.getPickID ());
				final int secondCount = getPlayerPickUtils ().getOriginalQuantityOfPick (secondPicks, pick.getPickID ());
				commonBookCount = commonBookCount + Math.min (firstCount, secondCount);
			}
		
		// Work out total
		int baseRelation = FIXED_BASE_SCORE + (commonBookCount * SHARED_BOOK) - (ALIGNMENT_DIFFERENCE * Math.abs
			(calculateAlignment (firstPicks, db) - calculateAlignment (secondPicks, db)));
		
		if (baseRelation < -90)
			baseRelation = -90;
		
		if (baseRelation > 90)
			baseRelation = 90;
		
		return baseRelation;
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
}