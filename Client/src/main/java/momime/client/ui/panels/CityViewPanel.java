package momime.client.ui.panels;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.JPanel;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.v0_9_5.CityViewElement;
import momime.client.utils.AnimationController;
import momime.client.utils.OverlandMapClientUtils;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.swing.NdgUIUtils;

/**
 * Panel that draws all city view elemenets (backgrounds, buildings, spell effects and so on) for a particular city 
 */
public final class CityViewPanel extends JPanel
{
	/** Unique value for serialization */
	private static final long serialVersionUID = 1797763921049648147L;

	/** Class logger */
	private final Log log = LogFactory.getLog (CityViewPanel.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** The city being viewed */
	private MapCoordinates3DEx cityLocation;
	
	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Overland map client utils */
	private OverlandMapClientUtils overlandMapClientUtils;
	
	/** Animation controller */
	private AnimationController anim;
	
	/**
	 * Sets up the panel once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	public final void init () throws IOException
	{
		log.trace ("Entering init");

		// Fix the size of the panel to be the same as a typical background
		final BufferedImage exampleBackground = getUtils ().loadImage ("/momime.client.graphics/cityView/landscape/arcanus.png");
		
		final Dimension backgroundSize = new Dimension (exampleBackground.getWidth () * 2, exampleBackground.getHeight () * 2);
		
		setMinimumSize (backgroundSize);
		setMaximumSize (backgroundSize);
		setPreferredSize (backgroundSize);
		
		// Find all animations, and start timers for them all
		// Match the exact selection logic of the draw routine, so we find only the necessary animations
		String elementSetsDone = "";
		
		for (final CityViewElement element : getGraphicsDB ().getCityViewElement ())
			
			// Some special "buildings" list trade goods have no coordinates and hence are never drawn in the city view
			if ((element.getLocationX () != null) && (element.getLocationY () != null) &&
			
				// Once we've drawn an item from an element set, we never draw anything else from the same set 
				((element.getCityViewElementSetID () == null) || (!elementSetsDone.contains (element.getCityViewElementSetID ()))) &&
				
				// Plane matches?
				((element.getPlaneNumber () == null) || (element.getPlaneNumber () == getCityLocation ().getZ ())) &&
				
				// Terrain matches?
				((element.getTileTypeID () == null) || (getOverlandMapClientUtils ().findAdjacentTileType
					(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (), getCityLocation (),
					getClient ().getSessionDescription ().getMapSize (), element.getTileTypeID ()))) &&
				
				// Building matches?
				((element.getBuildingID () == null) || (getMemoryBuildingUtils ().findBuilding
					(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (), getCityLocation (), element.getBuildingID ()))) &&
					
				// Spell matches?
				((element.getCitySpellEffectID () == null) || (getMemoryMaintainedSpellUtils ().findMaintainedSpell
					(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
					null, null, null, null, getCityLocation (), element.getCitySpellEffectID ()) != null)))
				
			{
				// Register it, if its an animation
				getAnim ().registerRepaintTrigger (element.getCityViewAnimation (), this);
				
				// List in sets
				if (element.getCityViewElementSetID () != null)
					elementSetsDone = elementSetsDone + element.getCityViewElementSetID ();
			}
		
		log.trace ("Exiting init");
	}
	
	/**
	 * This is called by the windowClosed handler of CityViewUI to close down all animations when the panel closes
	 */
	public final void cityViewClosing ()
	{
		log.trace ("Entering cityViewClosing");
		
		getAnim ().unregisterRepaintTrigger (null, this);

		log.trace ("Exiting cityViewClosing");
	}
	
	/**
	 * @param g Graphics context on which to paint the city
	 */
	@Override
	protected final void paintComponent (final Graphics g)
	{
		String elementSetsDone = "";
		
		for (final CityViewElement element : getGraphicsDB ().getCityViewElement ())
			
			// Some special "buildings" list trade goods have no coordinates and hence are never drawn in the city view
			if ((element.getLocationX () != null) && (element.getLocationY () != null) &&
			
				// Once we've drawn an item from an element set, we never draw anything else from the same set 
				((element.getCityViewElementSetID () == null) || (!elementSetsDone.contains (element.getCityViewElementSetID ()))) &&
				
				// Plane matches?
				((element.getPlaneNumber () == null) || (element.getPlaneNumber () == getCityLocation ().getZ ())) &&
				
				// Terrain matches?
				((element.getTileTypeID () == null) || (getOverlandMapClientUtils ().findAdjacentTileType
					(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap (), getCityLocation (),
					getClient ().getSessionDescription ().getMapSize (), element.getTileTypeID ()))) &&
				
				// Building matches?
				((element.getBuildingID () == null) || (getMemoryBuildingUtils ().findBuilding
					(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getBuilding (), getCityLocation (), element.getBuildingID ()))) &&
					
				// Spell matches?
				((element.getCitySpellEffectID () == null) || (getMemoryMaintainedSpellUtils ().findMaintainedSpell
					(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMaintainedSpell (),
					null, null, null, null, getCityLocation (), element.getCitySpellEffectID ()) != null)))
				
			{
				// Draw it
				try
				{
					final BufferedImage image = getAnim ().loadImageOrAnimationFrame (element.getCityViewImageFile (), element.getCityViewAnimation ());
					g.drawImage (image, element.getLocationX (), element.getLocationY (),
						image.getWidth () * element.getSizeMultiplier (), image.getHeight () * element.getSizeMultiplier (),
						null);
				}
				catch (final IOException e)
				{
					log.error (e, e);
				}
				
				// List in sets
				if (element.getCityViewElementSetID () != null)
					elementSetsDone = elementSetsDone + element.getCityViewElementSetID ();
			}
	}
	
	/**
	 * @return Multiplayer client
	 */
	public final MomClient getClient ()
	{
		return client;
	}
	
	/**
	 * @param obj Multiplayer client
	 */
	public final void setClient (final MomClient obj)
	{
		client = obj;
	}

	/**
	 * @return Graphics database
	 */
	public final GraphicsDatabaseEx getGraphicsDB ()
	{
		return graphicsDB;
	}

	/**
	 * @param db Graphics database
	 */
	public final void setGraphicsDB (final GraphicsDatabaseEx db)
	{
		graphicsDB = db;
	}

	/**
	 * @return The city being viewed
	 */
	public final MapCoordinates3DEx getCityLocation ()
	{
		return cityLocation;
	}

	/**
	 * @param loc The city being viewed
	 */
	public final void setCityLocation (final MapCoordinates3DEx loc)
	{
		cityLocation = loc;
	}

	/**
	 * @return Helper methods and constants for creating and laying out Swing components
	 */
	public final NdgUIUtils getUtils ()
	{
		return utils;
	}

	/**
	 * @param util Helper methods and constants for creating and laying out Swing components
	 */
	public final void setUtils (final NdgUIUtils util)
	{
		utils = util;
	}

	/**
	 * @return Memory building utils
	 */
	public final MemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param mbu Memory building utils
	 */
	public final void setMemoryBuildingUtils (final MemoryBuildingUtils mbu)
	{
		memoryBuildingUtils = mbu;
	}

	/**
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param spellUtils MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils spellUtils)
	{
		memoryMaintainedSpellUtils = spellUtils;
	}

	/**
	 * @return Overland map client utils
	 */
	public final OverlandMapClientUtils getOverlandMapClientUtils ()
	{
		return overlandMapClientUtils;
	}

	/**
	 * @param mapUtils Overland map client utils
	 */
	public final void setOverlandMapClientUtils (final OverlandMapClientUtils mapUtils)
	{
		overlandMapClientUtils = mapUtils;
	}

	/**
	 * @return Animation controller
	 */
	public final AnimationController getAnim ()
	{
		return anim;
	}

	/**
	 * @param controller Animation controller
	 */
	public final void setAnim (final AnimationController controller)
	{
		anim = controller;
	}
}