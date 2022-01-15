package momime.server.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;

import momime.common.MomException;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.SwitchResearch;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.common.messages.UnitStatusID;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.SpellUtils;
import momime.common.utils.TargetSpellResult;
import momime.server.MomSessionVariables;

/**
 * Server side methods dealing with researching and casting spells
 */
public final class SpellServerUtilsImpl implements SpellServerUtils
{
	/** Spell utils */
	private SpellUtils spellUtils;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/**
	 * @param player Player who wants to switch research
	 * @param spellID Spell that we want to research
	 * @param switchResearch Switch research option from session description
	 * @param db Lookup lists built over the XML database
	 * @return null if choice is acceptable; message to send back to client if choice isn't acceptable
	 * @throws RecordNotFoundException If either the spell we want to research now, or the spell previously being researched, can't be found
	 */
	@Override
	public final String validateResearch (final PlayerServerDetails player, final String spellID,
		final SwitchResearch switchResearch, final CommonDatabase db) throws RecordNotFoundException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		// Find the spell that we want to research
		final SpellResearchStatus spellWeWantToResearch = getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), spellID);

		// Find the spell that was previously being researched
		final Spell spellPreviouslyBeingResearched;
		final SpellResearchStatus spellPreviouslyBeingResearchedStatus;

		if (priv.getSpellIDBeingResearched () == null)
		{
			spellPreviouslyBeingResearched = null;
			spellPreviouslyBeingResearchedStatus = null;
		}
		else
		{
			spellPreviouslyBeingResearched = db.findSpell (priv.getSpellIDBeingResearched (), "validateResearch");
			spellPreviouslyBeingResearchedStatus = getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), priv.getSpellIDBeingResearched ());
		}

		// If we can't research it then its obviously disallowed regardless of the status of the previous research
		final String msg;
		if (spellWeWantToResearch.getStatus () != SpellResearchStatusID.RESEARCHABLE_NOW)
			msg = "The spell you've requested is currently not available for you to research.";

		// Picking research when we've got no current research, or switching to what we're already researching is always fine
		else if ((priv.getSpellIDBeingResearched () == null) || (priv.getSpellIDBeingResearched ().equals (spellID)))
			msg = null;

		// Check game option
		else if (switchResearch == SwitchResearch.DISALLOWED)
			msg = "You can't start researching a different spell until you've finished your current research.";

		else if ((spellPreviouslyBeingResearchedStatus.getRemainingResearchCost () < spellPreviouslyBeingResearched.getResearchCost ()) && (switchResearch == SwitchResearch.ONLY_IF_NOT_STARTED))
			msg = "You can't start researching a different spell until you've finished your current research.";

		else
			msg = null;

		return msg;
	}

	/**
	 * Generates list of target units for Great Unsummoning and Death Wish
	 * 
	 * @param spell Spell being cast
	 * @param castingPlayer Player who is casting the spell
	 * @param isTargeting True if calling this method to allow the player to target something at the unit, which means they must be able to see it,
	 * 	False if resolving damage - for example a unit we can't see is not a valid target to select, but if its hit by an area attack like ice storm, then we do damage it
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return List of target units
	 * @throws RecordNotFoundException If the definition of the unit, a skill or spell or so on cannot be found in the db
	 * @throws PlayerNotFoundException If we cannot find the player who owns the unit
	 * @throws MomException If the calculation logic runs into a situation it doesn't know how to deal with
	 */
	@Override
	public final List<MemoryUnit> listGlobalAttackTargets (final Spell spell, final PlayerServerDetails castingPlayer,
		final boolean isTargeting, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) castingPlayer.getPersistentPlayerPrivateKnowledge ();
		final FogOfWarMemory mem = isTargeting ? priv.getFogOfWarMemory () : mom.getGeneralServerKnowledge ().getTrueMap ();
		
		final List<String> protectedByCitySpellEffectIDs = mom.getServerDB ().getCitySpellEffect ().stream ().filter
			(e -> e.getProtectsAgainstSpellRealm ().contains (spell.getSpellRealm ())).map
			(e -> e.getCitySpellEffectID ()).collect (Collectors.toList ());
		
		// Get a list of all locations which have those citySpellEffectIDs cast
		final List<MapCoordinates3DEx> excludedLocations = mem.getMaintainedSpell ().stream ().filter
			(s -> (s.getCityLocation () != null) && (protectedByCitySpellEffectIDs.contains (s.getCitySpellEffectID ()))).map
			(s -> (MapCoordinates3DEx) s.getCityLocation ()).collect (Collectors.toList ());
		
		// Find all units that will be hit
		// If AI just considering casting the spell, then restrict this to only units they can see
		// but if actually processing casting it, will hit invisible units or units they can't see / have never seen
		final boolean attackOwnUnits = (spell.isAttackSpellOwnUnits () != null) && (spell.isAttackSpellOwnUnits ());
		final List<MemoryUnit> targetUnits = new ArrayList<MemoryUnit> ();
		
		for (final MemoryUnit tu : mem.getUnit ())
			if ((tu.getStatus () == UnitStatusID.ALIVE) && (!excludedLocations.contains (tu.getUnitLocation ())) &&
				((attackOwnUnits) || (tu.getOwningPlayerID () != castingPlayer.getPlayerDescription ().getPlayerID ())))
			{
				final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (tu, null, null, null,
					mom.getPlayers (), mem, mom.getServerDB ());
				
				if (getMemoryMaintainedSpellUtils ().isUnitValidTargetForSpell (spell, SpellBookSectionID.ATTACK_SPELLS, null, null,
					castingPlayer.getPlayerDescription ().getPlayerID (), null, null, xu, isTargeting, mem,
						priv.getFogOfWar (), mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET)
					
					targetUnits.add (tu);
			}				
		
		return targetUnits;
	}

	/**
	 * @return Spell utils
	 */
	public final SpellUtils getSpellUtils ()
	{
		return spellUtils;
	}

	/**
	 * @param utils Spell utils
	 */
	public final void setSpellUtils (final SpellUtils utils)
	{
		spellUtils = utils;
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
	 * @return expandUnitDetails method
	 */
	public final ExpandUnitDetails getExpandUnitDetails ()
	{
		return expandUnitDetails;
	}

	/**
	 * @param e expandUnitDetails method
	 */
	public final void setExpandUnitDetails (final ExpandUnitDetails e)
	{
		expandUnitDetails = e;
	}
}