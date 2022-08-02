package momime.common.ai;

import com.ndg.map.CoordinateSystem;
import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.areas.storage.MapArea3D;
import com.ndg.map.areas.storage.MapArea3DArrayListImpl;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.OverlandMapCityData;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.PlayerKnowledgeUtils;

/**
 * Calculates zones / national borders from what a player knows about the overland map.
 * This is principally used for the AI on the server, but is in the common project so that the client can
 * have a tickbox on the options screen to display their own border - which is mostly for purposes
 * of testing that this works as intended.
 */
public final class ZoneAIImpl implements ZoneAI
{
	/** How far out from a city do borders extend - set to match the largest visible radius from a city, from an Oracle */
	private final static int BORDER_RADIUS = 4;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/** Methods for working with wizardIDs */
	private PlayerKnowledgeUtils playerKnowledgeUtils;
	
	/**
	 * @param fogOfWarMemory Known overland terrain, units, buildings and so on
	 * @param overlandMapCoordinateSystem Coordinate system for traversing overland map
	 * @return 3D area marked with the player ID we consider as owning each tile
	 * @throws RecordNotFoundException If we encounter a tile type that can't be found in the db
	 */
	@Override
	public final MapArea3D<Integer> calculateZones (final FogOfWarMemory fogOfWarMemory, final CoordinateSystem overlandMapCoordinateSystem)
		throws RecordNotFoundException
	{
		// Create areas
		final MapArea3D<Integer> bestDistance = new MapArea3DArrayListImpl<Integer> ();
		bestDistance.setCoordinateSystem (overlandMapCoordinateSystem);

		final MapArea3D<Integer> zones = new MapArea3DArrayListImpl<Integer> ();
		zones.setCoordinateSystem (overlandMapCoordinateSystem);

		// Process all cities
		for (int plane = 0; plane < overlandMapCoordinateSystem.getDepth (); plane++)
		{
			final int z = plane;
			for (int y = 0; y < overlandMapCoordinateSystem.getHeight (); y++)
				for (int x = 0; x < overlandMapCoordinateSystem.getWidth (); x++)
				{
					final OverlandMapCityData cityData = fogOfWarMemory.getMap ().getPlane ().get (plane).getRow ().get (y).getCell ().get (x).getCityData ();
					if (cityData != null)
					{
						// Make sure its a wizard who owns it (ignore raiders)
						final KnownWizardDetails cityOwner = getKnownWizardUtils ().findKnownWizardDetails (fogOfWarMemory.getWizardDetails (), cityData.getCityOwnerID (), "calculateZones");
						if (getPlayerKnowledgeUtils ().isWizard (cityOwner.getWizardID ()))
							getCoordinateSystemUtils ().processCoordinatesWithinRadius (overlandMapCoordinateSystem, x, y, BORDER_RADIUS, (xc, yc, r, d, n) ->
							{
								final Integer currentDistance = bestDistance.get (xc, yc, z);
								
								if ((currentDistance == null) || (r < currentDistance))
								{
									zones.set (xc, yc, z, cityData.getCityOwnerID ());
									bestDistance.set (xc, yc, z, r);
								}
								
								return true;
							});
					}
				}
		}
		
		return zones;
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
}