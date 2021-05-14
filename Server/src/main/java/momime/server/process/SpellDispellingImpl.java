package momime.server.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.database.CombatAreaEffect;
import momime.common.database.CommonDatabase;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.servertoclient.CounterMagicResult;
import momime.common.messages.servertoclient.CounterMagicResultsMessage;
import momime.common.messages.servertoclient.DispelMagicResult;
import momime.common.messages.servertoclient.DispelMagicResultsMessage;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.server.MomSessionVariables;
import momime.server.fogofwar.FogOfWarMidTurnChanges;

/**
 * Handles dispelling spells making rolls to dispel other spells
 */
public final class SpellDispellingImpl implements SpellDispelling
{
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
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

		// Work out dispelling power - this is a bit of a cheat to just check combatBaseDamage first, we should know for certain if being cast in combat or overland.
		// However all dispel spells have the same combat and overland stats, so this is fine.
		final Integer dispellingPower;
		if (variableDamage != null)
			dispellingPower = variableDamage;
		else if (spell.getCombatBaseDamage () != null)
			dispellingPower = spell.getCombatBaseDamage ();
		else if (spell.getOverlandBaseDamage () != null)
			dispellingPower = spell.getOverlandBaseDamage ();
		else
			throw new MomException ("processDispelling trying to process spell ID " + spell.getSpellID () + " but no dispelling power is defined");
		
		// Now go through trying to dispel each spell
		boolean anyKilled = false;
		for (final MemoryMaintainedSpell spellToDispel : targetSpells)
		{
			// How much did this spell cost to cast?  That depends whether it was cast overland or in combat
			final Spell spellToDispelDef = mom.getServerDB ().findSpell (spellToDispel.getSpellID (), "processDispelling (D)");
			
			final DispelMagicResult result = new DispelMagicResult ();
			result.setOwningPlayerID (spellToDispel.getCastingPlayerID ());
			result.setSpellID (spellToDispel.getSpellID ());
			result.setCastingCost (spellToDispel.isCastInCombat () ? spellToDispelDef.getCombatCastingCost () : spellToDispelDef.getOverlandCastingCost ());
			result.setChance (dispellingPower.doubleValue () / (result.getCastingCost () + dispellingPower));
			result.setDispelled ((getRandomUtils ().nextInt (result.getCastingCost () + dispellingPower) < dispellingPower));

			// Add it to the messages first, because we might update who owns it
			if (castingPlayer.getPlayerDescription ().isHuman ())
				resultsMap.get (castingPlayer.getPlayerDescription ().getPlayerID ()).add (result);
			
			final PlayerServerDetails spellOwner = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), spellToDispel.getCastingPlayerID (), "processDispelling (D1)");
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

			// Process dispelling or capturing it
			if (result.isDispelled ())
			{
				// Do we take over the spell or just cancel it?
				// However, also check if we already have this overland enchantment - if we do, treat Spell Binding just like regular Disjunction
				if ((spell.getOverlandMaxDamage () == null) && (spell.getCombatMaxDamage () == null) &&
					(getMemoryMaintainedSpellUtils ().findMaintainedSpell (mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
						castingPlayer.getPlayerDescription ().getPlayerID (), spellToDispel.getSpellID (), null, null, null, null) == null)) 
				{
					spellToDispel.setCastingPlayerID (castingPlayer.getPlayerDescription ().getPlayerID ());
					
					getFogOfWarMidTurnChanges ().updatePlayerMemoryOfSpell (spellToDispel, mom.getGeneralServerKnowledge (),
						mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ());
				}
				
				// Regular dispel
				else if (getFogOfWarMidTurnChanges ().switchOffMaintainedSpellOnServerAndClients (mom.getGeneralServerKnowledge ().getTrueMap (),
					spellToDispel.getSpellURN (), mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ()))
						anyKilled = true;
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
				
				final PlayerServerDetails spellOwner = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), cae.getCastingPlayerID (), "processDispelling (D2)");
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
				
				final PlayerServerDetails entryPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), entry.getKey (), "processDispelling (D3)");
				entryPlayer.getConnection ().sendMessageToClient (msg);
			}
		}
		
		return anyKilled;
	}
	
	/**
	 * Makes dispel rolls that try to block a spell from ever being cast in combat in the first place (nodes and Counter Magic)
	 * 
	 * @param castingPlayer Player who is trying to cast a spell
	 * @param spell The spell they are trying to cast
	 * @param unmodifiedCombatCastingCost Unmodified mana cost of the spell they are trying to cast, including any extra MP for variable damage 
	 * @param combatLocation Location of the combat where this spell is being cast; null = being cast overland
	 * @param defendingPlayer Defending player in the combat
	 * @param attackingPlayer Attacking player in the combat
	 * @param trueMap True server knowledge of buildings and terrain
	 * @param players List of players in the session
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @return Whether the spell was successfully cast or not; so false = was dispelled
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final boolean processCountering (final PlayerServerDetails castingPlayer, final Spell spell, final int unmodifiedCombatCastingCost,
		final MapCoordinates3DEx combatLocation, final PlayerServerDetails defendingPlayer, final PlayerServerDetails attackingPlayer,
		final FogOfWarMemory trueMap, final List<PlayerServerDetails> players, final MomSessionDescription sd, final CommonDatabase db)
		throws RecordNotFoundException, JAXBException, XMLStreamException, MomException, PlayerNotFoundException
	{
		final List<CounterMagicResult> results = new ArrayList<CounterMagicResult> ();
		
		// As soon as one CAE blocks it, we don't bother keep rolling any more
		boolean dispelled = false;
		
		// Need to copy the list as we might remove CAEs as we go
		final List<MemoryCombatAreaEffect> trueCAEs = new ArrayList<MemoryCombatAreaEffect> ();
		trueCAEs.addAll (trueMap.getCombatAreaEffect ());
		
		// Look for CAEs in this location that have a dispelling power value defined
		for (final MemoryCombatAreaEffect cae : trueCAEs)
			if ((!dispelled) && (combatLocation.equals (cae.getMapLocation ())) &&
				((cae.getCastingPlayerID () == null) || (!cae.getCastingPlayerID ().equals (castingPlayer.getPlayerDescription ().getPlayerID ()))))
			{
				final CombatAreaEffect caeDef = db.findCombatAreaEffect (cae.getCombatAreaEffectID (), "processCountering");
				if ((caeDef.getDispellingPower () != null) &&
					((caeDef.getCombatAreaEffectMagicRealm () == null) || (!caeDef.getCombatAreaEffectMagicRealm ().equals (spell.getSpellRealm ()))))
				{
					final CounterMagicResult result = new CounterMagicResult ();
					result.setOwningPlayerID (cae.getCastingPlayerID ());
					result.setCombatAreaEffectID (cae.getCombatAreaEffectID ());
					result.setDispellingPower ((caeDef.getDispellingPower () > 0) ? caeDef.getDispellingPower () : cae.getCastingCost ());
					result.setChance (Integer.valueOf (result.getDispellingPower ()).doubleValue () / (unmodifiedCombatCastingCost + result.getDispellingPower ()));
					result.setDispelled ((getRandomUtils ().nextInt (unmodifiedCombatCastingCost + result.getDispellingPower ()) < result.getDispellingPower ()));
					results.add (result);
					
					if (result.isDispelled ())
						dispelled = true;
					
					// Decrease remaining power of Counter Magic
					if (caeDef.getDispellingPower () < 0)
					{
						cae.setCastingCost (cae.getCastingCost () + caeDef.getDispellingPower ());
						if (cae.getCastingCost () > 0)
							getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCombatAreaEffect (cae, players, sd);
						else
							getFogOfWarMidTurnChanges ().removeCombatAreaEffectFromServerAndClients (trueMap, cae.getCombatAreaEffectURN (), players, sd);
					}
				}
			}
		
		// Any results to send to any human players?
		if ((results.size () > 0) && ((attackingPlayer.getPlayerDescription ().isHuman ()) || (defendingPlayer.getPlayerDescription ().isHuman ())))
		{
			final CounterMagicResultsMessage msg = new CounterMagicResultsMessage ();
			msg.setCastingPlayerID (castingPlayer.getPlayerDescription ().getPlayerID ());
			msg.setSpellID (spell.getSpellID ());
			msg.getCounterMagicResult ().addAll (results);
			
			if (attackingPlayer.getPlayerDescription ().isHuman ())
				attackingPlayer.getConnection ().sendMessageToClient (msg);

			if (defendingPlayer.getPlayerDescription ().isHuman ())
				defendingPlayer.getConnection ().sendMessageToClient (msg);
		}
		
		return !dispelled;
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