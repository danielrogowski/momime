package momime.common.utils;

import java.util.Iterator;

import momime.common.database.v0_9_4.CombatMapLayerID;
import momime.common.messages.v0_9_4.MomCombatTile;
import momime.common.messages.v0_9_4.MomCombatTileLayer;

/**
 * Helper utils for dealing with combat maps
 */
public final class CombatMapUtilsImpl implements CombatMapUtils
{
	/**
	 * @param tile Tile to search
	 * @param layer Layer to look for
	 * @return combatTileTypeID at this layer, or null if this layer doesn't exist
	 */
	@Override
	public final String getCombatTileTypeForLayer (final MomCombatTile tile, final CombatMapLayerID layer)
	{
		// Search to see if layer already exists
		MomCombatTileLayer found = null;
		final Iterator<MomCombatTileLayer> layers = tile.getTileLayer ().iterator ();
		while ((found == null) && (layers.hasNext ()))
		{
			final MomCombatTileLayer thisLayer = layers.next ();
			if (thisLayer.getLayer () == layer)
				found = thisLayer;
		}
		
		// Did we find it?
		final String result;
		if (found != null)
			result = found.getCombatTileTypeID ();
		else
			result = null;
		
		return result;
	}
	
	/**
	 * Checks to see if the specified layer exists already for the specified tile; if it does then it updates it, if it doesn't then it adds it
	 * 
	 * @param tile Tile to update
	 * @param layer Layer to update
	 * @param combatTileTypeID New tile type to use for that layer
	 */
	@Override
	public final void setCombatTileTypeForLayer (final MomCombatTile tile, final CombatMapLayerID layer, final String combatTileTypeID)
	{
		// Search to see if layer already exists
		MomCombatTileLayer found = null;
		final Iterator<MomCombatTileLayer> layers = tile.getTileLayer ().iterator ();
		while ((found == null) && (layers.hasNext ()))
		{
			final MomCombatTileLayer thisLayer = layers.next ();
			if (thisLayer.getLayer () == layer)
				found = thisLayer;
		}
		
		// Did we find it?
		if (found != null)
			found.setCombatTileTypeID (combatTileTypeID);
		else
		{
			final MomCombatTileLayer newLayer = new MomCombatTileLayer ();
			newLayer.setLayer (layer);
			newLayer.setCombatTileTypeID (combatTileTypeID);
			
			tile.getTileLayer ().add (newLayer);
		}
	}
}
