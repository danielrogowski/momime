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
	MELEE ("melee"),
	
	/** Enemy unit and we have a ranged attack we can fire at it */
	RANGED ("ranged");
	
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