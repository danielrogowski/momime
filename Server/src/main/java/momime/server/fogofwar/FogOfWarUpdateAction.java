package momime.server.fogofwar;

/**
 * Lists actions the server may need to take to update a players' memory when the area the player can see changes
 *
 * The Delphi version uses some of the values from FogOfWarState again for this, but the names are confusing, so used a separate more understandable enum for it here
 */
enum FogOfWarUpdateAction
{
	/** No update necessary because we can't see the location */
	FOG_OF_WAR_ACTION_NONE,

	/** Need to forget players' knowledge of this location */
	FOG_OF_WAR_ACTION_FORGET,

	/** Need to update players' knowledge of this location */
	FOG_OF_WAR_ACTION_UPDATE,

	/** No update necessary because we never lost sight of the location, therefore our information on it must already be up to date */
	FOG_OF_WAR_ACTION_NEVER_LOST_SIGHT_OF;
}
