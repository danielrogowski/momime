package momime.client.ui.panels;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.Timer;

import momime.client.MomClient;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.graphics.database.v0_9_5.Animation;
import momime.client.graphics.database.v0_9_5.CityViewElement;
import momime.client.ui.MomUIUtils;
import momime.client.utils.OverlandMapClientUtils;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;

import com.ndg.map.coordinates.MapCoordinates3DEx;

/**
 * Panel that draws all city view elemenets (backgrounds, buildings, spell effects and so on) for a particular city 
 */
public final class CityViewPanel extends JPanel
{
	/** Unique value for serialization */
	private static final long serialVersionUID = 1797763921049648147L;

	/** Class logger */
	private final Logger log = Logger.getLogger (CityViewPanel.class.getName ());

	/** Multiplayer client */
	private MomClient client;
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** The city being viewed */
	private MapCoordinates3DEx cityLocation;
	
	/** Helper methods and constants for creating and laying out Swing components */
	private MomUIUtils utils;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Overland map client utils */
	private OverlandMapClientUtils overlandMapClientUtils;
	
	/** Lists the frame number that animations are on, keyed by the animationID */
	private Map<String, Integer> animationFrames = new HashMap<String, Integer> ();
	
	/**
	 * Sets up the panel once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	public final void init () throws IOException
	{
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
				// Is it an animation?
				try
				{
					if (element.getCityViewImageFile () != null)
					{
						// Nothing to do for static images
					}
					else if (element.getCityViewAnimation () != null)
					{
						final Animation anim = getGraphicsDB ().findAnimation (element.getCityViewAnimation (), "CityViewPanel.init");
						
						// Start at frame 0
						animationFrames.put (element.getCityViewAnimation (), 0);
						
						// Set off a timer to increment the frame
						new Timer ((int) (1000 / anim.getAnimationSpeed ()), new ActionListener ()
						{
							@Override
							public final void actionPerformed (final ActionEvent e)
							{
								int newFrame = animationFrames.get (element.getCityViewAnimation ()) + 1;
								if (newFrame >= anim.getFrame ().size ())
									newFrame = 0;
								
								animationFrames.put (element.getCityViewAnimation (), newFrame);
								
								repaint ();
							}
						}).start ();

					}
					else
						log.warning ("Wanted to prepare a city view element that has no image nor animation (" +
							element.getPlaneNumber () + ", " + element.getTileTypeID () + ", " + element.getBuildingID () + ", " + element.getCitySpellEffectID () + ")");
				}
				catch (final IOException e)
				{
					e.printStackTrace ();
				}
				
				// List in sets
				if (element.getCityViewElementSetID () != null)
					elementSetsDone = elementSetsDone + element.getCityViewElementSetID ();
			}
	}
	
	/**
	 * @param g Graphics context on which to paint the city
	 */
	@Override
	protected final void paintComponent (final Graphics g)
	{
		log.entering (CityViewPanel.class.getName (), "paintComponent");
		
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
					if (element.getCityViewImageFile () != null)
					{
						final BufferedImage image = getUtils ().loadImage (element.getCityViewImageFile ());
						g.drawImage (image, element.getLocationX (), element.getLocationY (),
							image.getWidth () * element.getSizeMultiplier (), image.getHeight () * element.getSizeMultiplier (),
							null);
					}
					else if (element.getCityViewAnimation () != null)
					{
						final Animation anim = getGraphicsDB ().findAnimation (element.getCityViewAnimation (), "CityViewPanel.paint");
						final Integer frame = animationFrames.get (element.getCityViewAnimation ());
					
						final BufferedImage image = getUtils ().loadImage (anim.getFrame ().get (frame).getFrameImageFile ());
						g.drawImage (image, element.getLocationX (), element.getLocationY (),
							image.getWidth () * element.getSizeMultiplier (), image.getHeight () * element.getSizeMultiplier (),
							null);
					}
					else
						log.warning ("Wanted to draw a city view element that has no image nor animation (" +
							element.getPlaneNumber () + ", " + element.getTileTypeID () + ", " + element.getBuildingID () + ", " + element.getCitySpellEffectID () + ")");	
				}
				catch (final IOException e)
				{
					e.printStackTrace ();
				}
				
				// List in sets
				if (element.getCityViewElementSetID () != null)
					elementSetsDone = elementSetsDone + element.getCityViewElementSetID ();
			}
		
		log.exiting (CityViewPanel.class.getName (), "paintComponent");
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
	public final MomUIUtils getUtils ()
	{
		return utils;
	}

	/**
	 * @param util Helper methods and constants for creating and laying out Swing components
	 */
	public final void setUtils (final MomUIUtils util)
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
}
