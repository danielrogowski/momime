package momime.server.process;

import java.io.IOException;
import java.util.Arrays;

import javax.xml.stream.XMLStreamException;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.SquareMapDirection;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.utils.random.RandomUtils;

import jakarta.xml.bind.JAXBException;
import momime.common.calculations.CityCalculationsImpl;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Spell;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.servertoclient.AttackCitySpellResult;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.TargetSpellResult;
import momime.server.MomSessionVariables;

/**
 * Handles casting spells that have more than one effect, and so need to call multiple methods in SpellCastingImpl
 */
public final class SpellMultiCastingImpl implements SpellMultiCasting
{
	/** Casting for each type of spell */
	private SpellCasting spellCasting;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/**
	 * Used for spells that can hit the units stationed in a city, destroy buildings and (sometimes) kill some of the population as well.  Earthquake + Call the Void.
	 * 
	 * @param spell Spell being cast
	 * @param castingPlayer Player who is casting it; can be null for city damage from events like Earthquake and Great Meteor
	 * @param eventID The event that caused an attack, if it wasn't initiated by a player
	 * @param variableDamage The damage chosen, for spells where variable mana can be channeled into casting them
	 * @param targetLocation The city being hit
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Counts of how many units, buildings and population were killed
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final AttackCitySpellResult castCityAttackSpell (final Spell spell, final PlayerServerDetails castingPlayer, final String eventID,
		final Integer variableDamage, final MapCoordinates3DEx targetLocation, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		final AttackCitySpellResult attackCitySpellResult = new AttackCitySpellResult ();
		
		// The unit deaths we just send.  The buildings being destroyed control the animation popup on the client.
		attackCitySpellResult.setUnitsKilled (getSpellCasting ().castOverlandAttackSpell (castingPlayer, eventID, spell, variableDamage, Arrays.asList (targetLocation), mom));
		
		// Now do the buildings
		attackCitySpellResult.setBuildingsDestroyed (getSpellCasting ().rollChanceOfEachBuildingBeingDestroyed
			(spell.getSpellID (), (castingPlayer == null) ? null : castingPlayer.getPlayerDescription ().getPlayerID (),
					spell.getDestroyBuildingChance (), Arrays.asList (targetLocation), mom));
		
		if (spell.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_CALL_THE_VOID))
		{
			// Population
			final OverlandMapCityData cityData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(targetLocation.getZ ()).getRow ().get (targetLocation.getY ()).getCell ().get (targetLocation.getX ()).getCityData ();
			if (cityData != null)
			{
				final int populationRolls = (cityData.getCityPopulation () / 1000) - 1;
				int populationDeaths = 0;
				for (int n = 0; n < populationRolls; n++)
					if (getRandomUtils ().nextBoolean ())
						populationDeaths++;
				
				if (populationDeaths > 0)
				{
					attackCitySpellResult.setPopulationKilled (populationDeaths);
					cityData.setCityPopulation (cityData.getCityPopulation () - (populationDeaths * 1000));
					
					mom.getWorldUpdates ().recalculateCity (targetLocation);
					mom.getWorldUpdates ().process (mom);
				}
			}
			
			// Surrounding tiles; don't target water tiles or already corrupted tiles
			final Spell singleTileSpell = mom.getServerDB ().findSpell (CommonDatabaseConstants.SPELL_ID_CORRUPTION, "castCityAttackSpell");
			
			final MapCoordinates3DEx coords = new MapCoordinates3DEx (targetLocation);
			for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
				if ((getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, direction.getDirectionID ())) &&
					(getMemoryMaintainedSpellUtils ().isOverlandLocationValidTargetForSpell (singleTileSpell,
						(castingPlayer == null) ? 0: castingPlayer.getPlayerDescription ().getPlayerID (), coords,
							mom.getGeneralServerKnowledge ().getTrueMap (), null, mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET) &&
					(getRandomUtils ().nextBoolean ()))
					
					getSpellCasting ().corruptTile (coords, mom);
		}
		
		return attackCitySpellResult;
	}

	/**
	 * @return Casting for each type of spell
	 */
	public final SpellCasting getSpellCasting ()
	{
		return spellCasting;
	}

	/**
	 * @param c Casting for each type of spell
	 */
	public final void setSpellCasting (final SpellCasting c)
	{
		spellCasting = c;
	}

	/**
	 * @return Random number generator
	 */
	public final RandomUtils getRandomUtils ()
	{
		return randomUtils;
	}

	/**
	 * @param utils Random number generator
	 */
	public final void setRandomUtils (final RandomUtils utils)
	{
		randomUtils = utils;
	}

	/**
	 * @return Coordinate system utils
	 */
	public final CoordinateSystemUtils getCoordinateSystemUtils ()
	{
		return coordinateSystemUtils;
	}

	/**
	 * @param utils Coordinate system utils
	 */
	public final void setCoordinateSystemUtils (final CoordinateSystemUtils utils)
	{
		coordinateSystemUtils = utils;
	}

	/**
	 * @return MemoryMaintainedSpell utils
	 */
	public final MemoryMaintainedSpellUtils getMemoryMaintainedSpellUtils ()
	{
		return memoryMaintainedSpellUtils;
	}

	/**
	 * @param utils MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils utils)
	{
		memoryMaintainedSpellUtils = utils;
	}
}