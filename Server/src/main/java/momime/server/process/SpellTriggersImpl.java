package momime.server.process;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.SquareMapDirection;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.utils.random.RandomUtils;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.calculations.CityCalculationsImpl;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Spell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.UnitStatusID;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.SpellTargetingUtils;
import momime.common.utils.TargetSpellResult;
import momime.server.MomSessionVariables;

/**
 * Methods mainly dealing with when very rare overland enchantments are triggered by a certain effect.
 * For some this is tied to another wizard casting as spell of a certain magic realm overland (e.g. casting Chaos spells);
 * some others just activate every turn.
 */
public final class SpellTriggersImpl implements SpellTriggers
{
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
	/** Casting for each type of spell */
	private SpellCasting spellCasting;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Methods that determine whether something is a valid target for a spell */
	private SpellTargetingUtils spellTargetingUtils;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;
	
	/** Random utils */
	private RandomUtils randomUtils;
	
	/** Dispel magic processing */
	private SpellDispelling spellDispelling;
	
	/**
	 * Handles an overland enchantment triggering its effect.
	 * 
	 * @param spell Overland enchantment that was triggered
	 * @param offendingPlayer Player who caused the trigger, if a player action triggers this effect; null if it just triggers automatically every turn
	 * @param offendingSpell The spell that triggered this overland enchantement effect; null if it just triggers automatically every turn
	 * @param offendingUnmodifiedCastingCost Unmodified mana cost of the spell that triggered this effect, including any extra MP for variable damage 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether the spell was successfully cast or not; so false = was dispelled
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final boolean triggerSpell (final MemoryMaintainedSpell spell,
		final PlayerServerDetails offendingPlayer, final Spell offendingSpell, final Integer offendingUnmodifiedCastingCost, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		final Spell spellDef = mom.getServerDB ().findSpell (spell.getSpellID (), "triggerSpell");
		
		final PlayerServerDetails castingPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID
			(mom.getPlayers (), spell.getCastingPlayerID (), "spellTriggered");
		
		// Get a list of all cities owned by the offending player, or maybe ALL players if null.
		// In any case we never include the cities owned by the casting player.
		final List<MapCoordinates3DEx> ourCityLocations = new ArrayList<MapCoordinates3DEx> ();
		final List<MapCoordinates3DEx> enemyCityLocations = new ArrayList<MapCoordinates3DEx> ();
		final List<MapCoordinates3DEx> allCityLocations = new ArrayList<MapCoordinates3DEx> ();
		
		for (int plane = 0; plane < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); plane++)
			for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
				for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
				{
					final OverlandMapCityData cityData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get (plane).getRow ().get (y).getCell ().get (x).getCityData ();
					if (cityData != null)
					{
						final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (x, y, plane);
						allCityLocations.add (cityLocation);

						if (cityData.getCityOwnerID () == spell.getCastingPlayerID ())
							ourCityLocations.add (cityLocation);
						
						if ((cityData.getCityOwnerID () != spell.getCastingPlayerID ()) &&
							((offendingPlayer == null) || (cityData.getCityOwnerID () == offendingPlayer.getPlayerDescription ().getPlayerID ())) &&
							(!getMemoryMaintainedSpellUtils ().isCityProtectedAgainstSpellRealm (cityLocation, spellDef.getSpellRealm (), spell.getCastingPlayerID (),
								mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), mom.getServerDB ())))
							enemyCityLocations.add (cityLocation);
					}
				}

		// Great Wasting and Armageddon are similar in that they both cast a lower level spell at 3-6 tiles per turn, the only difference is which spell it is
		if ((spell.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_GREAT_WASTING)) ||
			(spell.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_ARMAGEDDON)))
		{
			final String singleTileSpellID = spell.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_GREAT_WASTING) ?
				CommonDatabaseConstants.SPELL_ID_CORRUPTION : CommonDatabaseConstants.SPELL_ID_RAISE_VOLCANO;
			final Spell singleTileSpell = mom.getServerDB ().findSpell (singleTileSpellID, "triggerSpell (C)");
			
			// Get a list of every map cell that's a valid target for corruption (this is so we don't target water tiles, or already corrupted tiles)
			// except that corruption would normally not be targetable on cells we can't see.
			final List<MapCoordinates3DEx> targetCells = new ArrayList<MapCoordinates3DEx> ();
			for (int plane = 0; plane < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); plane++)
				for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
					for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
					{
						final MapCoordinates3DEx coords = new MapCoordinates3DEx (x, y, plane);
						if (getSpellTargetingUtils ().isOverlandLocationValidTargetForSpell (singleTileSpell, spell.getCastingPlayerID (), coords,
							mom.getGeneralServerKnowledge ().getTrueMap (), null, mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET)
							
							targetCells.add (coords);
					}
			
			// Now remove any cells that are too close to the caster's cities
			for (final MapCoordinates3DEx cityLocation : ourCityLocations)
			{
				final MapCoordinates3DEx coords = new MapCoordinates3DEx (cityLocation);
				for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
					if (getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, direction.getDirectionID ()))
						targetCells.remove (coords);
			}
			
			// Roll cells to corrupt
			int count = 3 + getRandomUtils ().nextInt (4);
			while ((count > 0) && (targetCells.size () > 0))
			{
				final MapCoordinates3DEx targetLocation = targetCells.get (getRandomUtils ().nextInt (targetCells.size ()));
				targetCells.remove (targetLocation);
				count--;
			
				if (spell.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_GREAT_WASTING))
					getSpellCasting ().corruptTile (targetLocation, mom);
				else
					getSpellCasting ().changeTileType (singleTileSpell, targetLocation, spell.getCastingPlayerID (), mom);
			}
		}
		
		else if (spellDef.getAttackSpellDamageResolutionTypeID () != null)
		{
			// How we figure out affected units depends on the TriggerAffectsUnits value
			final List<MapCoordinates3DEx> unitLocations;
			switch (spellDef.getTriggerAffectsUnits ())
			{
				// Enemy units inside their cities only; units not in cities are unaffected
				case INSIDE_CITIES:
					unitLocations = enemyCityLocations;
					break;
					
				// All units outside cities including our own
				case OUTSIDE_CITIES:
					unitLocations = mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ().stream ().filter
						(u -> (u.getStatus () == UnitStatusID.ALIVE) && (!allCityLocations.contains (u.getUnitLocation ()))).map
							(u -> (MapCoordinates3DEx) u.getUnitLocation ()).distinct ().collect (Collectors.toList ());
					break;
					
				default:
					throw new MomException ("triggerSpell does not know how to handle triggerAffectsUnits value of " + spellDef.getTriggerAffectsUnits ());
			}
	
			// Roll all units at once
			if (unitLocations.size () > 0)
				getSpellCasting ().castOverlandAttackSpell (castingPlayer, null, spellDef, spell.getVariableDamage (), unitLocations, 0, mom);
		}
		
		// Roll all buildings at once
		if ((spellDef.getDestroyBuildingChance () != null) && (enemyCityLocations.size () > 0))
			getSpellCasting ().rollChanceOfEachBuildingBeingDestroyed (spell.getSpellID (), spell.getCastingPlayerID (),
				spellDef.getDestroyBuildingChance (), enemyCityLocations, mom);
		
		boolean passesCounteringAttempts = true;
		if ((spellDef.getTriggerDispelPower () != null) && (!offendingSpell.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_SPELL_OF_RETURN)))
			passesCounteringAttempts = getSpellDispelling ().processCountering (offendingPlayer, offendingSpell, offendingUnmodifiedCastingCost, null,
				offendingPlayer, castingPlayer, spellDef, castingPlayer.getPlayerDescription ().getPlayerID (), mom);
		return passesCounteringAttempts;
	}

	/**
	 * @return Server only helper methods for dealing with players in a session
	 */
	public final MultiplayerSessionServerUtils getMultiplayerSessionServerUtils ()
	{
		return multiplayerSessionServerUtils;
	}

	/**
	 * @param obj Server only helper methods for dealing with players in a session
	 */
	public final void setMultiplayerSessionServerUtils (final MultiplayerSessionServerUtils obj)
	{
		multiplayerSessionServerUtils = obj;
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

	/**
	 * @return Methods that determine whether something is a valid target for a spell
	 */
	public final SpellTargetingUtils getSpellTargetingUtils ()
	{
		return spellTargetingUtils;
	}

	/**
	 * @param s Methods that determine whether something is a valid target for a spell
	 */
	public final void setSpellTargetingUtils (final SpellTargetingUtils s)
	{
		spellTargetingUtils = s;
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
	 * @return Random utils
	 */
	public final RandomUtils getRandomUtils ()
	{
		return randomUtils;
	}

	/**
	 * @param utils Random utils
	 */
	public final void setRandomUtils (final RandomUtils utils)
	{
		randomUtils = utils;
	}

	/**
	 * @return Dispel magic processing
	 */
	public final SpellDispelling getSpellDispelling ()
	{
		return spellDispelling;
	}

	/**
	 * @param p Dispel magic processing
	 */
	public final void setSpellDispelling (final SpellDispelling p)
	{
		spellDispelling = p;
	}
}