package momime.server.process;

/**
 * Lists actions the server may need to take to update a players' memory when the area the player can see changes
 *
 * The Delphi version uses some of the values from FogOfWarState again for this, but the names are confusing, so used a separate more understandable enum for it here
 */
enum FogOfWarUpdateAction
{
	/** No update necessary */
	FOG_OF_WAR_ACTION_NONE,

	/** Need to forget players' knowledge of this location */
	FOG_OF_WAR_ACTION_FORGET,

	/** Need to update players' knowledge of this location */
	FOG_OF_WAR_ACTION_UPDATE,

	/** Could see location last turn and can still see it now, so our knowledge of it must already be accurate so no update required - however MomTrueMap.needToAddUnitOnClient () needs this */
	FOG_OF_WAR_ACTION_ALREADY_VISIBLE;
}
