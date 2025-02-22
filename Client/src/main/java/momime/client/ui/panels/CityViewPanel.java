package momime.client.ui.panels;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.utils.swing.NdgUIUtils;

import momime.client.MomClient;
import momime.client.graphics.AnimationContainer;
import momime.client.utils.AnimationController;
import momime.common.MomException;
import momime.common.database.CityViewElement;
import momime.common.messages.servertoclient.RenderCityData;

/**
 * Panel that draws all city view elemenets (backgrounds, buildings, spell effects and so on) for a particular city 
 */
public final class CityViewPanel extends JPanel
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (CityViewPanel.class);

	/** Multiplayer client */
	private MomClient client;
	
	/** The city being viewed, note this is optional and will be null when displaying Spell of Return animation */
	private MapCoordinates3DEx cityLocation;
	
	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;
	
	/** Details about the city to draw */
	private RenderCityData renderCityData;
	
	/** Animation controller */
	private AnimationController anim;
	
	/** Whether we've added the mouse listener */
	private boolean mouseListenerAdded;
	
	/** Where to send building clicks to */
	private List<BuildingListener> buildingListeners = new ArrayList<BuildingListener> ();
	
	/** Image to show building to be sold at the end of the turn in simultaneous turns games */
	private BufferedImage pendingSaleImage;
	
	/**
	 * Sets up the panel once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	public final void init () throws IOException
	{
		// Used for simultaneous turns games
		pendingSaleImage = getUtils ().loadImage ("/momime.client.graphics/cityView/spellEffects/SE145.png");
		
		// Fix the size of the panel to be the same as a typical background
		final BufferedImage exampleBackground = getUtils ().loadImage ("/momime.client.graphics/cityView/landscape/arcanus.png");
		
		final Dimension backgroundSize = new Dimension (exampleBackground.getWidth () * 2, exampleBackground.getHeight () * 2);
		
		setMinimumSize (backgroundSize);
		setMaximumSize (backgroundSize);
		setPreferredSize (backgroundSize);
		
		// Find all animations, and start timers for them all
		// Match the exact selection logic of the draw routine, so we find only the necessary animations
		String elementSetsDone = "";
		
		for (final CityViewElement element : getClient ().getClientDB ().getCityViewElement ())
			if (drawElement (element, elementSetsDone) == DrawCityViewElement.YES)
			{
				// Register it, if its an animation
				getAnim ().registerRepaintTrigger (element.getCityViewAnimation (), this, AnimationContainer.COMMON_XML);
				
				// List in sets
				if (element.getCityViewElementSetID () != null)
					elementSetsDone = elementSetsDone + element.getCityViewElementSetID ();
			}
		
		repaint ();
		
		// Sell buildings when they're clicked on
		// init can get called multiple times to refresh the image from the cityViewUI, so make sure we don't add the mouse listener multiple times
		if (!mouseListenerAdded)
		{
			mouseListenerAdded = true;
			addMouseListener (new MouseAdapter ()
			{
				@Override
				public final void mouseClicked (final MouseEvent ev)
				{
					// If we've got no listener(s) to send the clicks to, then don't even bother
					if ((buildingListeners.size () > 0) && (getCityLocation () != null))
						try
						{
							// Was the pending sale gold coin clicked on?
							boolean found = false;
							String buildingID = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
								(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ()).getBuildingIdSoldThisTurn ();
							if (buildingID != null)
							{
								final CityViewElement element = getClient ().getClientDB ().findCityViewElementBuilding (buildingID, "CityViewPanel-clickPendingSale");
								
								// Is the click within the gold coin?
								if ((ev.getPoint ().x >= element.getLocationX ()) && (ev.getPoint ().y >= element.getLocationY ()) &&
									(ev.getPoint ().x < element.getLocationX () + pendingSaleImage.getWidth ()) &&
									(ev.getPoint ().y < element.getLocationY () + pendingSaleImage.getHeight ()))
								{
									{
										// Now more detailed check - ignore clicks on transparent pixels
										// First shift the point relative to the building location
										final int x = ev.getPoint ().x - element.getLocationX ();
										final int y = ev.getPoint ().y - element.getLocationY ();
									
										final int alpha = new Color (pendingSaleImage.getRGB (x, y), true).getAlpha ();
										if (alpha > 0)
											found = true;		// buildingID gets set to null just below anyway
									}
								}
							}
							
							// Look for a building that was clicked on
							String elementSetsDoneClick = "";
							buildingID = null;
					
							final Iterator<CityViewElement> iter = getClient ().getClientDB ().getCityViewElement ().iterator ();
							while ((!found) && (iter.hasNext ()))
							{
								final CityViewElement element = iter.next ();
	
								if (drawElement (element, elementSetsDoneClick) == DrawCityViewElement.YES)
								{
									// Ignore anything that isn't a building - we don't care if the e.g. river or some spell effect gets clicked on
									if (element.getBuildingID () != null)
									{
										// How big is the image for this building?
										// Just let the anim routines grab the image for us - we've registered for the anim anyway, and this means
										// we'll correctly handle exactly the pixels that are transparent in the current frame
										final BufferedImage image = getAnim ().loadImageOrAnimationFrame (element.getCityViewImageFile (), element.getCityViewAnimation (),
											true, AnimationContainer.COMMON_XML);
							
										// Is the click within this building image?
										if ((ev.getPoint ().x >= element.getLocationX ()) && (ev.getPoint ().y >= element.getLocationY ()) &&
											(ev.getPoint ().x < element.getLocationX () + (image.getWidth () * element.getSizeMultiplier ())) &&
											(ev.getPoint ().y < element.getLocationY () + (image.getHeight () * element.getSizeMultiplier ())))
										{
											// Now more detailed check - ignore clicks on transparent pixels
											// First shift the point relative to the building location and account for the size multiplier
											final int x = (ev.getPoint ().x - element.getLocationX ()) / element.getSizeMultiplier ();
											final int y = (ev.getPoint ().y - element.getLocationY ()) / element.getSizeMultiplier ();
										
											final int alpha = new Color (image.getRGB (x, y), true).getAlpha ();
											if (alpha > 0)
											{
												found = true;
												buildingID = element.getBuildingID ();
											}
										}
									}
							
									// List in sets
									if (element.getCityViewElementSetID () != null)
										elementSetsDoneClick = elementSetsDoneClick + element.getCityViewElementSetID ();
								}
							}
							
							// So was a building clicked on?
							if (found)
								for (final BuildingListener listener : buildingListeners)
									listener.buildingClicked (buildingID);
						}
						catch (final Exception e)
						{
							log.error (e, e);
						}
				}
			});
		}
	}
	
	/**
	 * This is called by the windowClosed handler of CityViewUI to close down all animations when the panel closes
	 * @throws MomException If unregisterRepaintTrigger is passed a null component, but that should never happen here 
	 */
	public final void cityViewClosing () throws MomException
	{
		getAnim ().unregisterRepaintTrigger (null, this);
	}
	
	/**
	 * @param g Graphics context on which to paint the city
	 */
	@Override
	protected final void paintComponent (final Graphics g)
	{
		String elementSetsDone = "";
		
		for (final CityViewElement element : getClient ().getClientDB ().getCityViewElement ())
		{
			final DrawCityViewElement draw = drawElement (element, elementSetsDone);
			if (draw != DrawCityViewElement.NO)
			{
				// Draw it
				try
				{
					final BufferedImage image;
					if (draw == DrawCityViewElement.RUBBLE)
						image = getUtils ().loadImage ("/momime.client.graphics/cityView/buildings/rubble.png");
					else
						image = getAnim ().loadImageOrAnimationFrame (element.getCityViewImageFile (), element.getCityViewAnimation (),
							true, AnimationContainer.COMMON_XML);
					
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
		
		// Need to show a pending sale?
		if (getCityLocation () != null)
		{
			final String buildingID = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
				(getCityLocation ().getZ ()).getRow ().get (getCityLocation ().getY ()).getCell ().get (getCityLocation ().getX ()).getBuildingIdSoldThisTurn ();
			if (buildingID != null)
				try
				{
					final CityViewElement element = getClient ().getClientDB ().findCityViewElementBuilding (buildingID, "CityViewPanel-drawPendingSale");
					g.drawImage (pendingSaleImage, element.getLocationX (), element.getLocationY (), null);
				}
				catch (final Exception e)
				{
					log.error (e, e);
				}
		}
	}
	
	/**
	 * These rules are needed in a bunch of places so keep it out separately here 
	 * 
	 * @param element Element to be considered
	 * @param elementSetsDone List of element sets that we already drew an element for
	 * @return Whether this element should be drawn, depending on what buildings etc. are in this city
	 */
	final DrawCityViewElement drawElement (final CityViewElement element, final String elementSetsDone)
	{
		final DrawCityViewElement draw;

		// Some special "buildings" list trade goods have no coordinates and hence are never drawn in the city view
		if ((element.getLocationX () == null) || (element.getLocationY () == null))
			draw = DrawCityViewElement.NO;
		
		// Once we've drawn an item from an element set, we never draw anything else from the same set
		else if ((element.getCityViewElementSetID () != null) && (elementSetsDone.contains (element.getCityViewElementSetID ())))
			draw = DrawCityViewElement.NO;
			
		// Plane matches?
		else if (((element.getPlaneNumber () == null) || (element.getPlaneNumber () == getRenderCityData ().getPlaneNumber ())) &&
			
			// Terrain matches?
			((element.getTileTypeID () == null) || (getRenderCityData ().getAdjacentTileTypeID ().contains (element.getTileTypeID ()))) &&
			
			// Building matches?
			((element.getBuildingID () == null) || (getRenderCityData ().getBuildingID ().contains (element.getBuildingID ()))) &&
				
			// Spell matches?
			((element.getCitySpellEffectID () == null) || (getRenderCityData ().getCitySpellEffectID ().contains (element.getCitySpellEffectID ()))))
			
			draw = DrawCityViewElement.YES;
		
		// Rubbled building?
		else if ((element.getBuildingID () != null) && (getRenderCityData ().getRubbleBuildingID ().contains (element.getBuildingID ())))
			draw = DrawCityViewElement.RUBBLE;
		
		else
			draw = DrawCityViewElement.NO;
		
		return draw;
	}
	
	/**
	 * @param listener Listener to send building clicks to
	 */
	public final void addBuildingListener (final BuildingListener listener)
	{
		buildingListeners.add (listener);
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
	 * @return The city being viewed, note this is optional and will be null when displaying Spell of Return animation
	 */
	public final MapCoordinates3DEx getCityLocation ()
	{
		return cityLocation;
	}

	/**
	 * @param loc The city being viewed, note this is optional and will be null when displaying Spell of Return animation
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
	 * @return Details about the city to draw
	 */
	public final RenderCityData getRenderCityData ()
	{
		return renderCityData;
	}

	/**
	 * @param r Details about the city to draw
	 */
	public final void setRenderCityData (final RenderCityData r)
	{
		renderCityData = r;
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