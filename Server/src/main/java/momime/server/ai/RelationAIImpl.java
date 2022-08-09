package momime.server.ai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.SquareMapDirection;
import com.ndg.map.areas.storage.MapArea3D;
import com.ndg.map.areas.storage.MapArea3DArrayListImpl;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;

import momime.common.ai.ZoneAI;
import momime.common.calculations.CityCalculationsImpl;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Pick;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.DiplomacyWizardDetails;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.Pact;
import momime.common.messages.PactType;
import momime.common.messages.PlayerPick;
import momime.common.messages.UnitStatusID;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.common.utils.PlayerPickUtils;
import momime.server.MomSessionVariables;

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
	
	/** Zone AI */
	private ZoneAI zoneAI;
	
	/** Methods for working with wizardIDs */
	private PlayerKnowledgeUtils playerKnowledgeUtils;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
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
		
		else if (baseRelation > 90)
			baseRelation = 90;
		
		return baseRelation;
	}
	
	/**
	 * @param player AI player whose turn to take
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If a wizard who owns a city can't be found
	 */
	@Override
	public final void updateVisibleRelationDueToUnitsInOurBorder (final PlayerServerDetails player, final MomSessionVariables mom)
		throws RecordNotFoundException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		
		final MapArea3D<Integer> zones = getZoneAI ().calculateZones (priv.getFogOfWarMemory (), mom.getSessionDescription ().getOverlandMapSize ());
		
		// Penalty is doubled in areas around our cities
		final MapArea3D<Boolean> cityResourceArea = new MapArea3DArrayListImpl<Boolean> ();
		cityResourceArea.setCoordinateSystem (mom.getSessionDescription ().getOverlandMapSize ());
		
		for (int plane = 0; plane < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); plane++)
			for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
				for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
				{
					final OverlandMapCityData cityData = priv.getFogOfWarMemory ().getMap ().getPlane ().get (plane).getRow ().get (y).getCell ().get (x).getCityData ();
					if ((cityData != null) && (cityData.getCityOwnerID () == player.getPlayerDescription ().getPlayerID ()) && (cityData.getCityPopulation () > 0))
					{
						final MapCoordinates3DEx coords = new MapCoordinates3DEx (x, y, plane);
						for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
							if (getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, direction.getDirectionID ()))
								cityResourceArea.set (coords, true);
					}
				}
		
		// Count how many units belonging to each player are in our zone
		final Map<Integer, Integer> unitCounts = new HashMap<Integer, Integer> ();
		for (final MemoryUnit mu : priv.getFogOfWarMemory ().getUnit ())
			if ((mu.getStatus () == UnitStatusID.ALIVE) && (mu.getOwningPlayerID () != player.getPlayerDescription ().getPlayerID ()))
			{
				// Only interested in units who are located in land we consider ours
				final Integer zonePlayerID = zones.get ((MapCoordinates3DEx) mu.getUnitLocation ());
				if (player.getPlayerDescription ().getPlayerID ().equals (zonePlayerID))
				{
					final Boolean doubled = cityResourceArea.get ((MapCoordinates3DEx) mu.getUnitLocation ());
					final int penalty = ((doubled != null) && (doubled)) ? 2 : 1;
					
					final Integer count = unitCounts.get (mu.getOwningPlayerID ());
					unitCounts.put (mu.getOwningPlayerID (), (count == null) ? penalty : (count + penalty));
				}
			}
		
		// Update each wizard's visibleRelation
		for (final KnownWizardDetails w : priv.getFogOfWarMemory ().getWizardDetails ())
			if ((unitCounts.containsKey (w.getPlayerID ())) && (getPlayerKnowledgeUtils ().isWizard (w.getWizardID ())))
			{
				final DiplomacyWizardDetails wizardDetails = (DiplomacyWizardDetails) w;
				wizardDetails.setVisibleRelation (wizardDetails.getVisibleRelation () - unitCounts.get (wizardDetails.getPlayerID ()));
			}
	}
	
	/**
	 * Grants a small bonus each turn we maintain a wizard pact or alliance with another wizard
	 * 
	 * @param player AI player whose turn to take
	 * @throws RecordNotFoundException If we can't find our wizard record or one of the wizards we have a pact with
	 */
	@Override
	public final void updateVisibleRelationDueToPactsAndAlliances (final PlayerServerDetails player)
		throws RecordNotFoundException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final KnownWizardDetails ourWizardDetails = getKnownWizardUtils ().findKnownWizardDetails
			(priv.getFogOfWarMemory ().getWizardDetails (), player.getPlayerDescription ().getPlayerID (), "updateVisibleRelationDueToPactsAndAlliances");
		
		for (final Pact pact : ourWizardDetails.getPact ())
			if (pact.getPactType () != PactType.WAR)
			{
				final DiplomacyWizardDetails theirWizardDetails = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
					(priv.getFogOfWarMemory ().getWizardDetails (), pact.getPactWithPlayerID (), "updateVisibleRelationDueToPactsAndAlliances");

				final int bonus = (pact.getPactType () == PactType.ALLIANCE) ? 6 : 3;
				theirWizardDetails.setVisibleRelation (theirWizardDetails.getVisibleRelation () + bonus);
			}
	}

	/**
	 * @param player AI player whose turn to take
	 */
	@Override
	public final void updateVisibleRelationDueToAuraOfMajesty (final PlayerServerDetails player)
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		
		final Set<Integer> playerIDs = priv.getFogOfWarMemory ().getMaintainedSpell ().stream ().filter
			(s -> s.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_AURA_OF_MAJESTY)).map (s -> s.getCastingPlayerID ()).collect (Collectors.toSet ());
		
		for (final KnownWizardDetails w : priv.getFogOfWarMemory ().getWizardDetails ())
			if (playerIDs.contains (w.getPlayerID ()))
			{
				final DiplomacyWizardDetails wizardDetails = (DiplomacyWizardDetails) w;
				wizardDetails.setVisibleRelation (wizardDetails.getVisibleRelation () + 1);
			}
	}
	
	/**
	 * @param player AI player to move their visible relations a little bit back towards base relation (unless they've ever started casting SoM, in which case we just permanently hate them)
	 */
	@Override
	public final void slideTowardsBaseRelation (final PlayerServerDetails player)
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		for (final KnownWizardDetails w : priv.getFogOfWarMemory ().getWizardDetails ())
			if ((getPlayerKnowledgeUtils ().isWizard (w.getWizardID ())) && (!w.isEverStartedCastingSpellOfMastery ()))
			{
				final DiplomacyWizardDetails wizardDetails = (DiplomacyWizardDetails) w;
				int shift = Math.abs (wizardDetails.getVisibleRelation () - wizardDetails.getBaseRelation ()) / 10;	// 10%
				
				if (shift > 0)
				{
					if (wizardDetails.getBaseRelation () < wizardDetails.getVisibleRelation ())
						shift = -shift;
					
					wizardDetails.setVisibleRelation (wizardDetails.getVisibleRelation () + shift);
				}
			}
	}

	/**
	 * @param player AI player to verify all their visibleRelation values are within the capped range
	 */
	@Override
	public final void capVisibleRelations (final PlayerServerDetails player)
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		for (final KnownWizardDetails w : priv.getFogOfWarMemory ().getWizardDetails ())
			if (getPlayerKnowledgeUtils ().isWizard (w.getWizardID ()))
			{
				final DiplomacyWizardDetails wizardDetails = (DiplomacyWizardDetails) w;
				
				if (wizardDetails.getVisibleRelation () < CommonDatabaseConstants.MIN_RELATION_SCORE)
					wizardDetails.setVisibleRelation (CommonDatabaseConstants.MIN_RELATION_SCORE);

				else if (wizardDetails.getVisibleRelation () > CommonDatabaseConstants.MAX_RELATION_SCORE)
					wizardDetails.setVisibleRelation (CommonDatabaseConstants.MAX_RELATION_SCORE);
			}
	}
	
	/**
	 * @param wizardDetails Wizard to receive bonus
	 * @param bonus Amount of bonus
	 */
	@Override
	public final void bonusToVisibleRelation (final DiplomacyWizardDetails wizardDetails, final int bonus)
	{
		wizardDetails.setVisibleRelation (wizardDetails.getVisibleRelation () + bonus);
		
		if (wizardDetails.getVisibleRelation () > CommonDatabaseConstants.MAX_RELATION_SCORE)
			wizardDetails.setVisibleRelation (CommonDatabaseConstants.MAX_RELATION_SCORE);	}
	
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

	/**
	 * @return Zone AI
	 */
	public final ZoneAI getZoneAI ()
	{
		return zoneAI;
	}

	/**
	 * @param ai Zone AI
	 */
	public final void setZoneAI (final ZoneAI ai)
	{
		zoneAI = ai;
	}

	/**
	 * @return Methods for working with wizardIDs
	 */
	public final PlayerKnowledgeUtils getPlayerKnowledgeUtils ()
	{
		return playerKnowledgeUtils;
	}

	/**
	 * @param k Methods for working with wizardIDs
	 */
	public final void setPlayerKnowledgeUtils (final PlayerKnowledgeUtils k)
	{
		playerKnowledgeUtils = k;
	}

	/**
	 * @return Coordinate system utils
	 */
	public final CoordinateSystemUtils getCoordinateSystemUtils ()
	{
		return coordinateSystemUtils;
	}

	/**
	 * @param utils Coordinate system utils
	 */
	public final void setCoordinateSystemUtils (final CoordinateSystemUtils utils)
	{
		coordinateSystemUtils = utils;
	}

	/**
	 * @return Methods for finding KnownWizardDetails from the list
	 */
	public final KnownWizardUtils getKnownWizardUtils ()
	{
		return knownWizardUtils;
	}

	/**
	 * @param k Methods for finding KnownWizardDetails from the list
	 */
	public final void setKnownWizardUtils (final KnownWizardUtils k)
	{
		knownWizardUtils = k;
	}
}