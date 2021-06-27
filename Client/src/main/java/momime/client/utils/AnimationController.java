package momime.client.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.swing.JComponent;

import momime.client.graphics.AnimationContainer;
import momime.common.MomException;
import momime.common.database.AnimationFrame;
import momime.common.database.RecordNotFoundException;
import momime.common.database.UnitCombatImage;

/**
 * Common class to handle simple animations defined in the graphics XML file, i.e. cycle around a set of bitmap
 * images at a particular frame rate.  Other kinds of animations, e.g. showing an object moving from one point to another,
 * or bitmap cycles that must have a specific start or duration, e.g. spell book page turning, must be hand written. 
 * 
 * There are 3 types of frames in the MoM IME Java client:
 * 
 * 1) Those with no animations at all.  These are just made up of totally standard Swing components, possibly
 *		with some custom paintComponent method(s), but no animated parts at all.  There may be repaints triggered,
 *		but by game values changing, rather than by a scheduled timer.  Simple example would be MessageBoxUI,
 *		more complex example would be MagicSlidersUI.
 *
 * 2) Those with simple/few animations.  These work via this AnimationController.  A paintComponent method
 *		will draw the animation onto the frame, using the loadImageOrAnimationFrame with registeredAnimation = true
 *		to ask the AnimationController which frame should be drawn.  If the animation (as defined in the Graphics XML)
 *		runs at for example 6 FPS, then the AnimationController will then (by virtue of a call to registerRepaintTrigger)
 *		fire repaint () events at the panel at 6 FPS.  This ensures that the frame is redrawn exactly when it needs to be - there
 *		is no point redrawing a screen 60 times a second if the animation only updates 6 times a second.
 *
 *		Animations in this mode *must* be pre-registered by a call to registerRepaintTrigger prior to the paintComponent
 *		method requesting which frame to draw by calling loadImageOrAnimationFrame.  They should also call
 *		unregisterRepaintTrigger when completed so that leftover timers don't keep firing needless repaint events.
 *		Simple example would be MainMenuUI (the animated "Master of Magic" fancy red text at the top).  More complex
 *		example would be CityViewPanel, where various buildings and spell effects are all drawn via timers which are
 *		potentially running at different frame rates (a sawmill might rotate at 6 FPS while the flag on a fighters' guild
 *		might flutter in the wind at 5 FPS).
 *
 * 3) Those with complex/many animations.  These are panels where using the above system would flood the panel
 *		with so many repaint events at differing intervals that it grinds to a halt.  In this case the paintComponent method
 *		still calls loadImageOrAnimationFrame to obtain which animation frame to draw, but with registeredAnimation = false.
 *		No call to registerRepaintTrigger is necessary, but this means the AnimationController doesn't fire any repaint () events
 *		at the panel, so forcing continual repaints must be handled by some other means.  Typically by making the panel inherit from
 *		JPanelWithConstantRepaints so it redraws itself as fast as it possibly can, with any luck at 60 FPS.  The primary example
 *		is CombatUI, where there are multiple kinds of units facing multiple directions all possibly displaying flying animations,
 *		in addition to spell animations, the animated red/blue outlines on selected tiles, and units moving and shooting.
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
	 * @param registeredAnimation Determines frame number: True=by Swing timer, must have previously called registerRepaintTrigger; False=by System.nanoTime ()
	 * @param container Whether the animation is defined in the graphics or common XML
	 * @return Appropriate image to display
	 * @throws MomException If the imageResourceName and the animationID are both null; or both are non-null; or if we request an anim that we didn't preregister interest in 
	 * @throws IOException If there is a problem loading either the statically named image, or a particular frame from the animation
	 */
	public BufferedImage loadImageOrAnimationFrame (final String imageResourceName, final String animationID, final boolean registeredAnimation,
		final AnimationContainer container) throws MomException, IOException;

	/**
	 * Gets the correct frame of a unit combat image, either from the static image or choosing the correct animation frame.
	 * 
	 * @param unitCombatImage Details of either the static image or link to the animation 
	 * @param registeredAnimation Determines frame number: True=by Swing timer, must have previously called registerRepaintTrigger; False=by System.nanoTime ()
	 * @param container Whether the animation is defined in the graphics or common XML
	 * @return Appropriate frame to display
	 * @throws MomException If the imageResourceName and the animationID are both null; or both are non-null; or if we request an anim that we didn't preregister interest in 
	 * @throws IOException If there is a problem loading either the statically named image, or a particular frame from the animation
	 */
	public AnimationFrame getUnitCombatImageFrame (final UnitCombatImage unitCombatImage,
		final boolean registeredAnimation, final AnimationContainer container)
		throws MomException, IOException;
	
	/**
	 * Call this when a component starts to display an animation - the component should know how to draw
	 * itself using the animation (via the loadImageOrAnimationFrame method) - so calling this method
	 * causes the component to be informed each time it needs to redraw itself to display the next animation frame.
	 * 
	 * @param animationID The animation that will be displayed; will be ignored and method will do nothing if this is null
	 * @param component The component that needs to receive repaint events from the animation
	 * @param container Whether the animation is defined in the graphics or common XML
	 * @throws RecordNotFoundException If the animationID can't be found
	 * @throws MomException If component is null
	 */
	public void registerRepaintTrigger (final String animationID, final JComponent component, final AnimationContainer container)
		throws RecordNotFoundException, MomException;
	
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
	public void unregisterRepaintTrigger (final String animationID, final JComponent component) throws MomException;
}