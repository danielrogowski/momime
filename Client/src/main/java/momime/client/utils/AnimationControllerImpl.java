package momime.client.utils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.Timer;

import momime.client.graphics.database.AnimationEx;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.common.MomException;
import momime.common.database.RecordNotFoundException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.NdgUIUtils;

/**
 * Common class that keeps track of which frame number animations are on, to save every screen that needs to
 * display animations repeating very similar code.  It also prevents ending up with old timers still running,
 * e.g. continually reclicking a different building on the change construction screen will reuse any existing
 * timers for the animations rather than keep creating new ones. 
 */
public final class AnimationControllerImpl implements AnimationController
{
	/** Class logger */
	private final Log log = LogFactory.getLog (AnimationControllerImpl.class);
	
	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;

	/** Helper methods and constants for creating and laying out Swing components */
	private NdgUIUtils utils;

	/** Lists the frame number that animations are on, keyed by the animationID */
	private Map<String, AnimationFrameCounter> animationFrames = new HashMap<String, AnimationFrameCounter> ();
	
	/**
	 * Gets the image for either a staticly named image or an animationID.  If an animationID, will use the frame speed
	 * defined in the graphics XML to animate the frames appropriately.
	 * 
	 * imageResourceName and animationID should be mutally exclusive; one should be filled in and the other left null.
	 * 
	 * @param imageResourceName Name of static image resource on classpath, e.g. /images/cards/Clubs/5C.png 
	 * @param animationID AnimationID from the graphics XML file
	 * @return Appropriate image to display
	 * @throws MomException If the imageResourceName and the animationID are both null; or both are non-null; or if we request an anim that we didn't preregister interest in 
	 * @throws IOException If there is a problem loading either the statically named image, or a particular frame from the animation
	 */
	@Override
	public final BufferedImage loadImageOrAnimationFrame (final String imageResourceName, final String animationID)
		throws MomException, IOException
	{
		if ((imageResourceName == null) && (animationID == null))
			throw new MomException ("loadImageOrAnimationFrame: imageResourceName and animationID were both null");

		if ((imageResourceName != null) && (animationID != null))
			throw new MomException ("loadImageOrAnimationFrame: imageResourceName \"" + imageResourceName + "\" and animationID \"" + animationID + "\" were both non-null");
		
		// Easy if its a static image
		final String imageName;
		if (imageResourceName != null)
			imageName = imageResourceName;
		else
		{
			// Anim must already exist in the frames map
			final AnimationFrameCounter counter = animationFrames.get (animationID);
			if (counter == null)
				throw new MomException ("Requested loadImageOrAnimationFrame on animationID \"" + animationID + "\" without making a prior call to registerRepaintTrigger");
			
			// Now can grab the correct frame
			imageName = counter.anim.getFrame ().get (counter.animationFrame).getFrameImageFile ();
		}

		// Now can load the image
		return getUtils ().loadImage (imageName);
	}

	/**
	 * Call this when a component starts to display an animation - the component should know how to draw
	 * itself using the animation (via the loadImageOrAnimationFrame method) - so calling this method
	 * causes the component to be informed each time it needs to redraw itself to display the next animation frame.
	 * 
	 * @param animationID The animation that will be displayed; will be ignored and method will do nothing if this is null
	 * @param component The component that needs to receive repaint events from the animation
	 * @throws RecordNotFoundException If the animationID can't be found
	 * @throws MomException If component is null
	 */
	@Override
	public final void registerRepaintTrigger (final String animationID, final JComponent component) throws RecordNotFoundException, MomException
	{
		log.trace ("Entering registerRepaintTrigger: " + animationID + ", " + component);
		
		if (component == null)
			throw new MomException ("registerRepaintTrigger called for animationID " + animationID + " but component was null");
		
		if (animationID != null)
		{
			// It may already be in the list, and we're just adding another trigger
			final AnimationFrameCounter counter = animationFrames.get (animationID);
			if (counter != null)
			{
				// Don't add triggers twice if register is called twice on the same anim+component (e.g. multiple calls to CityViewPanel.init)
				if (counter.repaintTriggers.contains (component))
					log.debug ("registerRepaintTrigger already found a timer for animation " + animationID + " and component was already listening to it, so nothing to do: " + component);
				else
				{
					log.debug ("registerRepaintTrigger already found a timer for animation " + animationID + " so adding new listener to it: " + component);
					counter.repaintTriggers.add (component);
				}
			}
			else
			{
				log.debug ("registerRepaintTrigger setting up new timer for animation " + animationID + " for listener: " + component);
				
				final AnimationFrameCounter newCounter = new AnimationFrameCounter ();
				newCounter.anim = getGraphicsDB ().findAnimation (animationID, "registerRepaintTrigger");
				newCounter.repaintTriggers.add (component);

				// Set off a timer to increment the frame
				newCounter.timer = new Timer ((int) (1000 / newCounter.anim.getAnimationSpeed ()), newCounter);
				animationFrames.put (animationID, newCounter);
				newCounter.timer.start ();
			}
		}

		log.trace ("Exiting registerRepaintTrigger");		
	}
	
	/**
	 * When an animation is no longer being displayed on a particular component, call this method so that the
	 * component ceases to have repaint events triggered by the animation.  Additionally if this was the *only*
	 * component receiving repaint events from this animation then the animation effectively becomes orphaned,
	 * its timer is stopped and it is removed from memory to clean things up properly.
	 * 
	 * The safest+simplest thing to do is to call this with a null animationID as frame are disposed of, to search
	 * for and close out all corresponding animations.  Note some frames (e.g. main menu, change language, new game screens)
	 * in the MoM Client are singletons and these are merely hidden and never disposed of, so animations for
	 * those run forever.  Some frames (e.g. most popup type windows) are prototypes which are disposed out of
	 * memory when they are closed, so animations for these must be properly cleaned up as the frame closes. 
	 * 
	 * @param animationID The animation that was being displayed; if this is left null, ALL animations that the specified component registered an interest in will be unregistered 
	 * @param component The component that was receiving repaint events from the animation
	 * @throws MomException If component is null
	 */
	@Override
	public final void unregisterRepaintTrigger (final String animationID, final JComponent component) throws MomException
	{
		log.trace ("Entering unregisterRepaintTrigger: " + animationID + ", " + component);

		if (component == null)
			throw new MomException ("unregisterRepaintTrigger called for animationID " + animationID + " but component was null");
		
		if (animationID != null)
		{
			// Remove from a single specified animation ID
			final AnimationFrameCounter counter = animationFrames.get (animationID);
			if (counter != null)
				unregisterRepaintTriggerInternal (counter, component, null);
		}
		else
		{
			// Remove from all animations that are triggering this component
			final Iterator<AnimationFrameCounter> iter = animationFrames.values ().iterator ();
			while (iter.hasNext ())
				unregisterRepaintTriggerInternal (iter.next (), component, iter);
		}

		log.trace ("Exiting unregisterRepaintTrigger");
	}
	
	/**
	 * Internal method called each time we find a counter to remove the specified component from
	 * 
	 * @param counter Frame counter which the component no longer wishes to receive repaint events for
	 * @param component The component that was receiving repaint events from the animation
	 * @param iter If specified, closed out anims are removed from the animationFrames map via iter.remove (); if null they're removed via the remove method on the map
	 */
	final void unregisterRepaintTriggerInternal (final AnimationFrameCounter counter, final JComponent component, final Iterator<AnimationFrameCounter> iter)
	{
		log.trace ("Entering unregisterRepaintTriggerInternal: " + counter.anim.getAnimationID () + ", " + component);
		
		// Remove the component from the trigger list
		if (counter.repaintTriggers.remove (component))
		{
			if (counter.repaintTriggers.size () > 0)
				log.debug ("unregisterRepaintTrigger removed listener from animation " + counter.anim.getAnimationID () + " but it still has " + counter.repaintTriggers.size () +
					" other listeners so keeping the timer running: " + component);
			else
			{
				log.debug ("unregisterRepaintTrigger removed listener from animation " + counter.anim.getAnimationID () + " and closing down the timer: " + component);
				
				// There's now no triggers left, so close everything down
				counter.timer.stop ();
				counter.timer.removeActionListener (counter);
				
				// If checking all animations, have to take care to use the remove method on the iterator, since
				// we've removing items from the map that's being iterated over
				if (iter != null)
					iter.remove ();
				else
					animationFrames.remove (counter.anim.getAnimationID ());
			}
		}

		log.trace ("Exiting unregisterRepaintTriggerInternal");
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
	 * Internal class holding the current frame and the list of components on which we need to trigger repaint events when the current frame changes
	 */
	private final class AnimationFrameCounter implements ActionListener
	{
		/** The animation being displayed, so we don't have to keep re-finding it every frame */
		public AnimationEx anim;
		
		/** Current frame number to display */
		public int animationFrame;
		
		/** The timer that is updating this frame number */
		private Timer timer;
		
		/** List of components on which we need to trigger repaint events when the current frame changes */
		public final List<JComponent> repaintTriggers = new ArrayList<JComponent> ();  

		/**
		 * Update the frame number and trigger all the repaint methods
		 */
		@Override
		public final void actionPerformed (final ActionEvent ev)
		{
			int newFrame = animationFrame + 1;
			if (newFrame >= anim.getFrame ().size ())
				newFrame = 0;
			
			animationFrame = newFrame;
			
			for (final JComponent trigger : repaintTriggers)
				trigger.repaint ();
		}
	}
}