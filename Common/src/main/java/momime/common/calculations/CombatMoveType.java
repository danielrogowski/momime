package momime.common.calculations;

/**
 * The different types of 'move' a combat move can result in
 */
public enum CombatMoveType
{
	/** Can't move here, reach here, its too far away, unpassable terrain, or so on */
	CANNOT_MOVE ("cannot"),
	
	/** Can walk here in one turn */
	MOVE (null),
	
	/** Can teleport to here */
	TELEPORT (null),
	
	/** Adjacent enemy unit we can hit */
	MELEE_UNIT ("melee"),
	
	/** Adjacent wall we can hit */
	MELEE_WALL ("melee"),

	/** Adjacent enemy unit and wall we can hit at the same time; this can only happen at the doorway or with a flyer that can hit over the wall */
	MELEE_UNIT_AND_WALL ("melee"),
	
	/** Enemy unit and we have a ranged attack we can fire at it */
	RANGED_UNIT ("ranged"),
	
	/** Wall and we have a ranged attack we can fire at it */
	RANGED_WALL ("ranged"),
	
	/** Enemy unit and wall we have a ranged attack we can fire at both at the same time */
	RANGED_UNIT_AND_WALL ("ranged");
	
	/** Name of the image that the client displays for this move type */
	private final String imageFilename;
	
	/**
	 * @param anImageFilename Name of the image that the client displays for this move type
	 */
	private CombatMoveType (final String anImageFilename)
	{
		imageFilename = (anImageFilename == null) ? null :
			"/momime.client.graphics/ui/combat/moveType-" + anImageFilename + ".png";
	}

	/**
	 * @return Name of the image that the client displays for this move type
	 */
	public final String getImageFilename ()
	{
		return imageFilename;
	}
}