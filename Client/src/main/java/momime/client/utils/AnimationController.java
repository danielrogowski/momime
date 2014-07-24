package momime.client.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.JComponent;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;

/**
 * Common class that keeps track of which frame number animations are on, to save every screen that needs to
 * display animations repeating very similar code.  It also prevents ending up with old timers still running,
 * e.g. continually reclicking a different building on the change construction screen will reuse any existing
 * timers for the animations rather than keep creating new ones. 
 */
public interface AnimationController
{
	/**
	 * Gets the image for either a staticly named image or an animationID.  Which frame of the animation to display
	 * should have been previously set by calling registerRepaintTrigger to start the animation.
	 * 
	 * imageResourceName and animationID should be mutally exclusive; one should be filled in and the other left null.
	 * 
	 * @param imageResourceName Name of static image resource on classpath, e.g. /images/cards/Clubs/5C.png 
	 * @param animationID AnimationID from the graphics XML file
	 * @return Appropriate image to display
	 * @throws MomException If the imageResourceName and the animationID are both null; or both are non-null; or if we request an anim that we didn't preregister interest in 
	 * @throws IOException If there is a problem loading either the statically named image, or a particular frame from the animation
	 */
	public BufferedImage loadImageOrAnimationFrame (final String imageResourceName, final String animationID)
		throws MomException, IOException;

	/**
	 * Call this when a component starts to display an animation - the component should know how to draw
	 * itself using the animation (via the loadImageOrAnimationFrame method) - so calling this method
	 * causes the component to be informed each time it needs to redraw itself to display the next animation frame.
	 * 
	 * @param animationID The animation that will be displayed; will be ignored and method will do nothing if this is null
	 * @param component The component that needs to receive repaint events from the animation
	 * @throws RecordNotFoundException If the animationID can't be found
	 */
	public void registerRepaintTrigger (final String animationID, final JComponent component) throws RecordNotFoundException;
	
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
	 */
	public void unregisterRepaintTrigger (final String animationID, final JComponent component);
}