package momime.server.fogofwar;

/**
 * KillUnitActionID has now been phased out of the XSDs and network messages, so all possible values are defined here.
 * 
 * There will eventually need to be an OVERLAND_DAMAGE setting which I think is needed already to correctly handle heroes dying in
 * unfortunate non-combat situations, like being on a boat when it sinks.  I think what this amounts to is that I need to rename
 * some of these into HEALABLE_COMBAT_DAMAGE, HEALABLE_OVERLAND_DAMAGE and have FREE renamed into PERMANENT_DAMAGE
 * which would then cover settlers and spirits being consumed, as well as units being killed by e.g. cracks call.
 */
public enum KillUnitActionID
{
	/** Remove unit entirely; there is no possible means to bring it back in future */
	FREE,
	
	/** Hero dismissed from service; they return to the pool of available heroes (at status GENERATED) and available to be resummoned later */
	HERO_DIMISSED_VOLUNTARILY,

	/** Units get routed and abanoned our cause because we failed to pay/feed them - effectivley the same as FREE since there is no way it can come back;
	 * Heroes abanoned our cause because we failed to pay/feed them - effectively the same as HERO_DIMISSED_VOLUNTARILY - they are available to summon again */
	LACK_OF_PRODUCTION,
	
	/** Unit killed mainly by healable damage in combat; this may set the unit to DEAD if it can be later raised/animated during combat, or heroes resurrected after combat, or
	 * may be removed entirely like FREE, depending on the type of unit, whether the player is involved in the combat and whether it is our unit or somebody else's.
	 * Note the unit is still dead - healable in this sense just means the type of damage the unit took to kill it - because it was mainly healable, the unit can be
	 * potentially brought back to life by certain spells.
	 * 
	 * NB. killUnitOnServerAndClients could determine whether the damage the unit took was mainly healable or mainly permanent itself and not rely on this being passed in,
	 * however the same isn't true for overland damage, where units can be killed by certain situations not relating to them taking actual damage, for example
	 * units drowning when the boat that was transporting them sinks, or having their Flight spell dispelled while over water (or over land, if a boat).
	 * So for consistency I kept combat and overland damage both specifying whether the damage was mainly healable or mainly permanent on the way into the kill routine. */
	HEALABLE_COMBAT_DAMAGE,
	
	/** As HEALABLE_COMBAT_DAMAGE, except that units can be removed immediately since there's no overland raise dead spell that works on units - only heroes need to be kept */ 
	HEALABLE_OVERLAND_DAMAGE;
}