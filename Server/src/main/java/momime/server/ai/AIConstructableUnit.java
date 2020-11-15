package momime.server.ai;

import com.ndg.map.coordinates.MapCoordinates3DEx;

import momime.common.database.Spell;
import momime.common.database.Unit;

/**
 * Holds details about a unit an AI player could build
 */
final class AIConstructableUnit implements Comparable<AIConstructableUnit>
{
	/** The type of unit we can construct */
	private final Unit unit;
	
	/** The city where the unit can be constructed, or null if its a summoning spell */
	private final MapCoordinates3DEx cityLocation;
	
	/** The spell that summons the unit, or null if its a unit we're constructing in a city */
	private final Spell spell;
	
	/** The average rating calculated for the unit if we construct it */
	private final int averageRating;
	
	/** Type of unit the AI classifies this unit to be */
	private final AIUnitType aiUnitType;
	
	/** Whether we could afford the maintenance cost if we added another one of these units to our army */
	private final boolean canAffordMaintenance;
	
	/**
	 * @param aUnit The type of unit we can construct
	 * @param aCityLocation The city where the unit can be constructed, or null if its a summoning spell
	 * @param aSpell The spell that summons the unit, or null if its a unit we're constructing in a city
	 * @param anAverageRating The average rating calculated for the unit if we construct it
	 * @param anAiUnitType Type of unit the AI classifies this unit to be
	 * @param aCanAffordMaintenance Whether we could afford the maintenance cost if we added another one of these units to our army
	 */
	AIConstructableUnit (final Unit aUnit, final MapCoordinates3DEx aCityLocation, final Spell aSpell, final int anAverageRating,
		final AIUnitType anAiUnitType, final boolean aCanAffordMaintenance)
	{
		unit = aUnit;
		cityLocation = aCityLocation;
		spell = aSpell;
		averageRating = anAverageRating;
		aiUnitType = anAiUnitType;
		canAffordMaintenance = aCanAffordMaintenance;
	}

	/**
	 * @return Value to sort units by 'average rating'
	 */
	@Override
	public final int compareTo (final AIConstructableUnit o)
	{
		return o.getAverageRating () - getAverageRating ();
	}
	
	/**
	 * @return String representation of values, for debug messages
	 */
	@Override
	public final String toString ()
	{
		return getUnit ().getUnitName () + " (" + getUnit ().getUnitID () + ") type " + getAiUnitType () + " " +
			((getCityLocation () != null) ? ("constructed at " + getCityLocation ()) : ("summoned by " + getSpell ().getSpellName ())) +
			" has average rating of " + getAverageRating () + ", can afford = " + isCanAffordMaintenance ();
	}
	
	/**
	 * @return The type of unit we can construct
	 */
	public final Unit getUnit ()
	{
		return unit;
	}
	
	/**
	 * @return The city where the unit can be constructed, or null if its a summoning spell
	 */
	public final MapCoordinates3DEx getCityLocation ()
	{
		return cityLocation;
	}
	
	/**
	 * @return The spell that summons the unit, or null if its a unit we're constructing in a city
	 */
	public final Spell getSpell ()
	{
		return spell;
	}
	
	/**
	 * @return The average rating calculated for the unit if we construct it
	 */
	public final int getAverageRating ()
	{
		return averageRating;
	}

	/**
	 * @return Type of unit the AI classifies this unit to be
	 */
	public final AIUnitType getAiUnitType ()
	{
		return aiUnitType;
	}
	
	/**
	 * @return Whether we could afford the maintenance cost if we added another one of these units to our army
	 */
	public final boolean isCanAffordMaintenance ()
	{
		return canAffordMaintenance;
	}
}