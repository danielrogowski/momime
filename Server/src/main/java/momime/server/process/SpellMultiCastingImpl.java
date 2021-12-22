package momime.server.process;

import java.util.Arrays;

import jakarta.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.SquareMapDirection;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.calculations.CityCalculationsImpl;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.servertoclient.AttackCitySpellResult;
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
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final AttackCitySpellResult castCityAttackSpell (final Spell spell, final PlayerServerDetails castingPlayer, final String eventID,
		final Integer variableDamage, final MapCoordinates3DEx targetLocation, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, MomException, PlayerNotFoundException
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
			
			// Surrounding tiles
			final MapCoordinates3DEx coords = new MapCoordinates3DEx (targetLocation);
			for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
				if ((getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, direction.getDirectionID ())) &&
					getRandomUtils ().nextBoolean ())
					
					getSpellCasting ().corruptTile (coords, mom.getGeneralServerKnowledge ().getTrueMap (),
						mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());
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
}