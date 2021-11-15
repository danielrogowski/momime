package momime.server.process;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.UnitStatusID;
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
	
	/**
	 * Handles an overland enchantment triggering its effect.
	 * 
	 * @param spell Overland enchantment that was triggered
	 * @param offendingPlayerID Player who caused the trigger, if a player action triggers this effect; null if it just triggers automatically every turn
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void triggerSpell (final MemoryMaintainedSpell spell, final Integer offendingPlayerID, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		final Spell spellDef = mom.getServerDB ().findSpell (spell.getSpellID (), "triggerSpell");
		
		final PlayerServerDetails castingPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID
			(mom.getPlayers (), spell.getCastingPlayerID (), "spellTriggered");
		
		// Get a list of all cities owned by the offending player, or maybe ALL players if null.
		// In any case we never include the cities owned by the casting player.
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
						
						if ((cityData.getCityOwnerID () != spell.getCastingPlayerID ()) &&
							((offendingPlayerID == null) || (cityData.getCityOwnerID () == offendingPlayerID)))
							enemyCityLocations.add (cityLocation);
					}
				}

		if (spellDef.getAttackSpellDamageResolutionTypeID () != null)
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
				getSpellCasting ().castOverlandAttackSpell (castingPlayer, spellDef, spell.getVariableDamage (), unitLocations, mom);
		}
		
		// Roll all buildings at once
		if ((spellDef.getDestroyBuildingChance () != null) && (enemyCityLocations.size () > 0))
			getSpellCasting ().rollChanceOfEachBuildingBeingDestroyed (spell.getSpellID (), spell.getCastingPlayerID (),
				spellDef.getDestroyBuildingChance (), enemyCityLocations, mom);
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
}