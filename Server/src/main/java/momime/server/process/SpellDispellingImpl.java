package momime.server.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.servertoclient.DispelMagicResult;
import momime.common.messages.servertoclient.DispelMagicResultsMessage;
import momime.server.MomSessionVariables;
import momime.server.fogofwar.FogOfWarMidTurnChanges;

/**
 * Handles dispelling spells making rolls to dispel other spells
 */
public final class SpellDispellingImpl implements SpellDispelling
{
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/**
	 * Makes dispel rolls against a list of target spells and CAEs
	 * 
	 * @param spell Dispel spell being cast
	 * @param variableDamage Chosen damage selected for the spell - in this case its dispelling power
	 * @param castingPlayer Player who is casting the dispel spell
	 * @param targetSpells Target spells that we will make rolls to try to dispel
	 * @param targetCAEs Target CAEs that we will make rolls to try to dispel, can be left null
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether dispelling any spells resulted in the death of any units
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final boolean processDispelling (final Spell spell, final Integer variableDamage, final PlayerServerDetails castingPlayer,
		final List<MemoryMaintainedSpell> targetSpells, final List<MemoryCombatAreaEffect> targetCAEs, final MomSessionVariables mom)
		throws MomException, JAXBException, XMLStreamException, PlayerNotFoundException, RecordNotFoundException
	{
		// Build up a map so we remember which results we have to send to which players
		final Map<Integer, List<DispelMagicResult>> resultsMap = new HashMap<Integer, List<DispelMagicResult>> ();
		if (castingPlayer.getPlayerDescription ().isHuman ())
			resultsMap.put (castingPlayer.getPlayerDescription ().getPlayerID (), new ArrayList<DispelMagicResult> ());
		
		// Now go through trying to dispel each spell
		boolean anyKilled = false;
		final Integer dispellingPower = (variableDamage != null) ? variableDamage : spell.getCombatBaseDamage ();
		for (final MemoryMaintainedSpell spellToDispel : targetSpells)
		{
			// How much did this spell cost to cast?  That depends whether it was cast overland or in combat
			final Spell spellToDispelDef = mom.getServerDB ().findSpell (spellToDispel.getSpellID (), "castCombatNow (D)");
			
			final DispelMagicResult result = new DispelMagicResult ();
			result.setOwningPlayerID (spellToDispel.getCastingPlayerID ());
			result.setSpellID (spellToDispel.getSpellID ());
			result.setCastingCost (spellToDispel.isCastInCombat () ? spellToDispelDef.getCombatCastingCost () : spellToDispelDef.getOverlandCastingCost ());
			result.setChance (dispellingPower.doubleValue () / (result.getCastingCost () + dispellingPower));
			result.setDispelled ((getRandomUtils ().nextInt (result.getCastingCost () + dispellingPower) < dispellingPower));
			
			if (result.isDispelled ())
				if (getFogOfWarMidTurnChanges ().switchOffMaintainedSpellOnServerAndClients (mom.getGeneralServerKnowledge ().getTrueMap (),
					spellToDispel.getSpellURN (), mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ()))
						anyKilled = true;
			
			if (castingPlayer.getPlayerDescription ().isHuman ())
				resultsMap.get (castingPlayer.getPlayerDescription ().getPlayerID ()).add (result);
			
			final PlayerServerDetails spellOwner = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), spellToDispel.getCastingPlayerID (), "castCombatNow (D1)");
			if (spellOwner.getPlayerDescription ().isHuman ())
			{
				List<DispelMagicResult> results = resultsMap.get (spellToDispel.getCastingPlayerID ());
				if (results == null)
				{
					results = new ArrayList<DispelMagicResult> ();
					resultsMap.put (spellToDispel.getCastingPlayerID (), results);
				}
				results.add (result);
			}
		}
		
		// Also go through trying to dispel each CAE
		if (targetCAEs != null)
			for (final MemoryCombatAreaEffect cae : targetCAEs)
			{
				final DispelMagicResult result = new DispelMagicResult ();
				result.setOwningPlayerID (cae.getCastingPlayerID ());
				result.setCombatAreaEffectID (cae.getCombatAreaEffectID ());
				result.setCastingCost (cae.getCastingCost ());
				result.setChance (dispellingPower.doubleValue () / (result.getCastingCost () + dispellingPower));
				result.setDispelled ((getRandomUtils ().nextInt (result.getCastingCost () + dispellingPower) < dispellingPower));
	
				if (result.isDispelled ())
					getFogOfWarMidTurnChanges ().removeCombatAreaEffectFromServerAndClients (mom.getGeneralServerKnowledge ().getTrueMap (),
						cae.getCombatAreaEffectURN (), mom.getPlayers (), mom.getSessionDescription ());
	
				if (castingPlayer.getPlayerDescription ().isHuman ())
					resultsMap.get (castingPlayer.getPlayerDescription ().getPlayerID ()).add (result);
				
				final PlayerServerDetails spellOwner = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), cae.getCastingPlayerID (), "castCombatNow (D2)");
				if (spellOwner.getPlayerDescription ().isHuman ())
				{
					List<DispelMagicResult> results = resultsMap.get (cae.getCastingPlayerID ());
					if (results == null)
					{
						results = new ArrayList<DispelMagicResult> ();
						resultsMap.put (cae.getCastingPlayerID (), results);
					}
					results.add (result);
				}
			}
		
		// Send the results to each human player invovled
		if (resultsMap.size () > 0)
		{
			final DispelMagicResultsMessage msg = new DispelMagicResultsMessage ();
			msg.setCastingPlayerID (castingPlayer.getPlayerDescription ().getPlayerID ());
			msg.setSpellID (spell.getSpellID ());
			
			for (final Entry<Integer, List<DispelMagicResult>> entry : resultsMap.entrySet ())
			{
				msg.getDispelMagicResult ().clear ();
				msg.getDispelMagicResult ().addAll (entry.getValue ());
				
				final PlayerServerDetails entryPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), entry.getKey (), "castCombatNow (D3)");
				entryPlayer.getConnection ().sendMessageToClient (msg);
			}
		}
		
		return anyKilled;
	}

	/**
	 * @return Methods for updating true map + players' memory
	 */
	public final FogOfWarMidTurnChanges getFogOfWarMidTurnChanges ()
	{
		return fogOfWarMidTurnChanges;
	}

	/**
	 * @param obj Methods for updating true map + players' memory
	 */
	public final void setFogOfWarMidTurnChanges (final FogOfWarMidTurnChanges obj)
	{
		fogOfWarMidTurnChanges = obj;
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
}