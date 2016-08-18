package momime.common.calculations;

import java.util.ArrayList;
import java.util.List;

import momime.common.utils.ExpandedUnitDetails;

/**
 * This class models a stack of units that are moving together on the overland map, in an attempt to replicate the rather odd way that MoM handles transports.
 * There is no explicit "Load" or "Unload" option, units are simply moved onto a transport (or the transport moved onto units), and then when the transport moves
 * it will automatically take with it any units it can, regardless of whether the player actually clicked the secondary units to move with them.
 * So there is no kind of "loadedInUnitURN" element in the data model - transporting is all worked out on the fly when a unit stack goes to move.
 * 
 * Also (from the MoM wiki, but verified this myself too) when units are being transported, the transports govern the movement speed of the entire stack, even
 * units not actually loaded in the transport.  e.g. a Trireme can only hold 2 units, is stacked with 2 High Men spearmen who can't swim and 2 Lizardmen spearmen who can.
 * Selecting only the Trireme will cause all 5 units to move together, with a movement of 2 sea tiles.  For the High Men the movement speed makes sense... the Lizardmen
 * must be hanging onto the outside of the transport or being pulled along in its wake? :)  Note all the units will accompany the transport in the same stack, whether
 * we selected them to or not.
 * 
 * To model this, we say movement has 2 modes, "transported" and "normal" movement, indicated by whether the getTransports () list below is empty or not.
 * 
 * If the transports list is not empty, only the movement speed of the transports are taken into account for where the stack can move to, and only the transports
 * are charged movement points for the move, even if similar to the above example not all units actually fit inside the transports.
 * 
 * If the transports list is empty, units all move under their own steam as normal.
 * 
 * The requirement for units to be allowed to accompany a transported stack without fitting inside is if all terrain is passable to them - i.e. Lizardmen or fliers.
 * 
 * Example 1:
 * Sea tile contains 2 Triremes, 3 High Men, 2 Lizardmen.  We click 1 Trireme to move.  Ends up with transports list=the Trireme, and 2 of the High Men and
 * the 2 Lizardmen get automatically added into the units list even if we didn't select them.
 * 
 * Example 2:
 * Land tile contains a flying Trireme, 2 High Men, 2 Lizardmen.  We click the Trireme to move.  This is still a transported movement, its a minor
 * detail that the Lizardmen can't fit inside and can't actually fly :)  (yes actually tested this in the original!)
 * 
 * Example 3:
 * Land tile contains a flying Trireme, 3 High Men, 2 Lizardmen.  We click the Trireme to move.  This is a regular movement since the High Men don't
 * all fit, and so the Trireme would end up in the units list by itself and move alone.  If we individually clicked the entire stack, they'd all end up in the units
 * list, even the Trireme, and move according to their own movement speeds.
 */
public final class UnitStack
{
	/** Transports holding the other units in the stack, if empty list then all units move individually */
	private final List<ExpandedUnitDetails> transports = new ArrayList<ExpandedUnitDetails> ();
	
	/** Normal units in the stack */
	private final List<ExpandedUnitDetails> units = new ArrayList<ExpandedUnitDetails> ();
	
	/**
	 * @return Transports holding the other units in the stack, if empty list then all units move individually
	 */
	public final List<ExpandedUnitDetails> getTransports ()
	{
		return transports;
	}
	
	/**
	 * @return Normal units in the stack
	 */
	public final List<ExpandedUnitDetails> getUnits ()
	{
		return units;
	}
}