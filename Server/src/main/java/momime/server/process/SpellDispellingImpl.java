package momime.server.process;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.PlayerType;
import com.ndg.utils.random.RandomUtils;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.database.CombatAreaEffect;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.messages.DiplomacyWizardDetails;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.PlayerPick;
import momime.common.messages.servertoclient.CounterMagicResult;
import momime.common.messages.servertoclient.CounterMagicResultsMessage;
import momime.common.messages.servertoclient.DispelMagicResult;
import momime.common.messages.servertoclient.DispelMagicResultsMessage;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.PlayerPickUtils;
import momime.server.MomSessionVariables;
import momime.server.ai.RelationAI;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.KillUnitActionID;
import momime.server.knowledge.ServerGridCellEx;

/**
 * Handles dispelling spells making rolls to dispel other spells
 */
public final class SpellDispellingImpl implements SpellDispelling
{
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Memory CAE utils */
	private MemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils;
	
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/** Spell processing methods */
	private SpellProcessing spellProcessing;
	
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/** For calculating relation scores between two wizards */
	private RelationAI relationAI;
	
	/**
	 * Makes dispel rolls against a list of target spells and CAEs
	 * 
	 * @param spell Dispel spell being cast
	 * @param variableDamage Chosen damage selected for the spell - in this case its dispelling power
	 * @param castingPlayer Player who is casting the dispel spell
	 * @param targetSpells Target spells that we will make rolls to try to dispel
	 * @param targetCAEs Target CAEs that we will make rolls to try to dispel, can be left null
	 * @param targetWarpedNode Warped node that we are trying to return to normal
	 * @param targetVortexes Vortexes are odd in that the unit as a whole gets dispelled (killed) rather than a spell cast on the unit
	 * @param penaltyToVisibleRelation How mad each wizard will get whose spells we are targeting for dispelling
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether dispelling any spells resulted in the death of any units
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final boolean processDispelling (final Spell spell, final Integer variableDamage, final PlayerServerDetails castingPlayer,
		final List<MemoryMaintainedSpell> targetSpells, final List<MemoryCombatAreaEffect> targetCAEs,
		final MapCoordinates3DEx targetWarpedNode, final List<MemoryUnit> targetVortexes, final int penaltyToVisibleRelation, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		// Build up a map so we remember which results we have to send to which players.
		// Also used to just get a list of players we made dispel attempts against, so for now put AI players in the list too.
		final Map<Integer, List<DispelMagicResult>> resultsMap = new HashMap<Integer, List<DispelMagicResult>> ();
		resultsMap.put (castingPlayer.getPlayerDescription ().getPlayerID (), new ArrayList<DispelMagicResult> ());

		// Work out dispelling power - this is a bit of a cheat to just check combatBaseDamage first, we should know for certain if being cast in combat or overland.
		// However all dispel spells have the same combat and overland stats, so this is fine.
		final Integer dispellingPower;
		if (variableDamage != null)
			dispellingPower = variableDamage;
		else
		{
			final Integer baseDispellingPower;
			if (spell.getCombatBaseDamage () != null)
				baseDispellingPower = spell.getCombatBaseDamage ();
			else if (spell.getOverlandBaseDamage () != null)
				baseDispellingPower = spell.getOverlandBaseDamage ();
			else
				throw new MomException ("processDispelling trying to process spell ID " + spell.getSpellID () + " but no dispelling power is defined");
			
			// If caster has Runemaster and specified variableDamage then this was already included in the specified dispelling power.
			// But if they just passed in null (like the AI does) then we need to double the base damage.
			int multiplier = 1;
			final List<PlayerPick> picks = getKnownWizardUtils ().findKnownWizardDetails (mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (),
				castingPlayer.getPlayerDescription ().getPlayerID (), "processDispelling (C)").getPick ();
			if (getPlayerPickUtils ().getQuantityOfPick (picks, CommonDatabaseConstants.RETORT_NODE_RUNEMASTER) > 0)
				multiplier++;
			
			dispellingPower = baseDispellingPower * multiplier;
		}
		
		// Sort the list, so all Spell Locks come first
		if (targetSpells.size () > 1)
		{
			final List<String> spellLockSpellIDs = mom.getServerDB ().getSpell ().stream ().filter
				(s -> (s.isBlocksOtherDispels () != null) && (s.isBlocksOtherDispels ())).map (s -> s.getSpellID ()).collect (Collectors.toList ());
			
			targetSpells.sort ((s1, s2) ->
			{
				final int v;
				if (spellLockSpellIDs.contains (s1.getSpellID ()) == spellLockSpellIDs.contains (s2.getSpellID ()))
					v = s1.getSpellURN () - s2.getSpellURN ();
				
				else if (spellLockSpellIDs.contains (s1.getSpellID ()))
					v = -1;
				
				else
					v = 1;
				
				return v;
			});
		}
		
		// Now go through trying to dispel each spell
		// Skip over any spells targeted on units for which we've previously found a Spell Lock
		final List<Integer> spellLockedUnitURNs = new ArrayList<Integer> ();
		
		final Map<String, String> masteries = mom.getServerDB ().getPick ().stream ().filter (p -> p.getNodeAndDispelBonus () != null).collect (Collectors.toMap
			(p -> p.getNodeAndDispelBonus (), p -> p.getPickID ()));
		
		for (final MemoryMaintainedSpell spellToDispel : targetSpells)
			if ((spellToDispel.getUnitURN () == null) || (!spellLockedUnitURNs.contains (spellToDispel.getUnitURN ())))
			{
				// How much did this spell cost to cast?  That depends whether it was cast overland or in combat
				final Spell spellToDispelDef = mom.getServerDB ().findSpell (spellToDispel.getSpellID (), "processDispelling (D)");
				
				final DispelMagicResult result = new DispelMagicResult ();
				result.setOwningPlayerID (spellToDispel.getCastingPlayerID ());
				result.setSpellID (spellToDispel.getSpellID ());
				
				if (spellToDispel.isCastInCombat ())
					result.setCastingCost ((spellToDispelDef.getCombatDispelCost () != null) ? spellToDispelDef.getCombatDispelCost () : spellToDispelDef.getCombatCastingCost ());
				else
					result.setCastingCost ((spellToDispelDef.getOverlandDispelCost () != null) ? spellToDispelDef.getOverlandDispelCost () : spellToDispelDef.getOverlandCastingCost ());
				
				// Retorts that make spells more difficult to dispel
				int multiplier = 1;
				
				final List<PlayerPick> spellOwnerPicks = getKnownWizardUtils ().findKnownWizardDetails (mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (),
					spellToDispel.getCastingPlayerID (), "processDispelling (D1)").getPick ();
				
				if (getPlayerPickUtils ().getQuantityOfPick (spellOwnerPicks, CommonDatabaseConstants.RETORT_ID_ARCHMAGE) > 0)
					multiplier++;
				
				if ((masteries.containsKey (spellToDispelDef.getSpellRealm ())) &&
					(getPlayerPickUtils ().getQuantityOfPick (spellOwnerPicks, masteries.get (spellToDispelDef.getSpellRealm ())) > 0))
					
					multiplier++;
				
				if (multiplier > 1)
					result.setCastingCost (result.getCastingCost () * multiplier);
				
				result.setChance (dispellingPower.doubleValue () / (result.getCastingCost () + dispellingPower));
				result.setDispelled ((getRandomUtils ().nextInt (result.getCastingCost () + dispellingPower) < dispellingPower));
	
				// Add it to the messages first, because we might update who owns it
				resultsMap.get (castingPlayer.getPlayerDescription ().getPlayerID ()).add (result);
				
				List<DispelMagicResult> results = resultsMap.get (spellToDispel.getCastingPlayerID ());
				if (results == null)
				{
					results = new ArrayList<DispelMagicResult> ();
					resultsMap.put (spellToDispel.getCastingPlayerID (), results);
				}
				results.add (result);
				
				// If spell locked then keep a list of it
				if ((spellToDispel.getUnitURN () != null) && (spellToDispelDef.isBlocksOtherDispels () != null) && (spellToDispelDef.isBlocksOtherDispels ()))
					spellLockedUnitURNs.add (spellToDispel.getUnitURN ());
	
				// Process dispelling or capturing it
				if (result.isDispelled ())
				{
					// Do we take over the spell or just cancel it?
					// However, also check if we already have this overland enchantment - if we do, treat Spell Binding just like regular Disjunction
					if ((spell.getOverlandMaxDamage () == null) && (spell.getCombatMaxDamage () == null) &&
						(getMemoryMaintainedSpellUtils ().findMaintainedSpell (mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
							castingPlayer.getPlayerDescription ().getPlayerID (), spellToDispel.getSpellID (), null, null, null, null) == null)) 
					{
						// Check each combat area effect that this overland enchantment gives to see if we have any of them in effect - if so cancel them
						for (final String combatAreaEffectID: spellToDispelDef.getSpellHasCombatEffect ())
						{
							final MemoryCombatAreaEffect cae = getMemoryCombatAreaEffectUtils ().findCombatAreaEffect
								(mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), null, combatAreaEffectID, spellToDispel.getCastingPlayerID ());
							
							if (cae != null)
							{
								cae.setCastingPlayerID (castingPlayer.getPlayerDescription ().getPlayerID ());
								getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCombatAreaEffect (cae, mom.getPlayers (), mom.getSessionDescription ());
							}
						}
	
						// Now take over the spell itself
						spellToDispel.setCastingPlayerID (castingPlayer.getPlayerDescription ().getPlayerID ());
						
						getFogOfWarMidTurnChanges ().updatePlayerMemoryOfSpell (spellToDispel, mom);
					}
					
					// Regular dispel
					else
						mom.getWorldUpdates ().switchOffSpell (spellToDispel.getSpellURN (), false);
				}			
			}
		
		// Also go through trying to dispel each CAE
		if (targetCAEs != null)
			for (final MemoryCombatAreaEffect cae : targetCAEs)
			{
				// Find the spell that created it
				final Spell caeSpell = mom.getServerDB ().getSpell ().stream ().filter
					(s -> s.getSpellHasCombatEffect ().contains (cae.getCombatAreaEffectID ())).findAny ().orElse (null);
				if (caeSpell != null)
				{
					final DispelMagicResult result = new DispelMagicResult ();
					result.setOwningPlayerID (cae.getCastingPlayerID ());
					result.setCombatAreaEffectID (cae.getCombatAreaEffectID ());
					result.setCastingCost (cae.getCastingCost ());
					
					// Retorts that make spells more difficult to dispel
					int multiplier = 1;
					
					final List<PlayerPick> spellOwnerPicks = getKnownWizardUtils ().findKnownWizardDetails (mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (),
						cae.getCastingPlayerID (), "processDispelling (D2)").getPick ();
					
					if (getPlayerPickUtils ().getQuantityOfPick (spellOwnerPicks, CommonDatabaseConstants.RETORT_ID_ARCHMAGE) > 0)
						multiplier++;
					
					if ((masteries.containsKey (caeSpell.getSpellRealm ())) &&
						(getPlayerPickUtils ().getQuantityOfPick (spellOwnerPicks, masteries.get (caeSpell.getSpellRealm ())) > 0))
						
						multiplier++;
					
					if (multiplier > 1)
						result.setCastingCost (result.getCastingCost () * multiplier);
					
					result.setChance (dispellingPower.doubleValue () / (result.getCastingCost () + dispellingPower));
					result.setDispelled ((getRandomUtils ().nextInt (result.getCastingCost () + dispellingPower) < dispellingPower));
		
					if (result.isDispelled ())
						mom.getWorldUpdates ().removeCombatAreaEffect (cae.getCombatAreaEffectURN ());
		
					resultsMap.get (castingPlayer.getPlayerDescription ().getPlayerID ()).add (result);
					
					List<DispelMagicResult> results = resultsMap.get (cae.getCastingPlayerID ());
					if (results == null)
					{
						results = new ArrayList<DispelMagicResult> ();
						resultsMap.put (cae.getCastingPlayerID (), results);
					}
					results.add (result);
				}
			}
		
		// Also try to revert warped nodes back to normal
		if (targetWarpedNode != null)
		{
			// How much did this spell cost to cast?  That depends whether it was cast overland or in combat
			final Spell spellToDispelDef = mom.getServerDB ().findSpell (CommonDatabaseConstants.SPELL_ID_WARP_NODE, "processDispelling (W)");
			
			final DispelMagicResult result = new DispelMagicResult ();
			result.setSpellID (spellToDispelDef.getSpellID ());
			result.setCastingCost (spellToDispelDef.getOverlandCastingCost ());
			result.setChance (dispellingPower.doubleValue () / (result.getCastingCost () + dispellingPower));
			result.setDispelled ((getRandomUtils ().nextInt (result.getCastingCost () + dispellingPower) < dispellingPower));

			if (result.isDispelled ())
			{
				// Resolve the node warping out across the full area, updating the true map as well as players' memory of who can see each cell and informing the clients too
				for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
					for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
						for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
						{
							final ServerGridCellEx aura = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x);
							if (targetWarpedNode.equals (aura.getAuraFromNode ()))
							{
								// Update true map
								aura.getTerrainData ().setWarped (null);
								
								// Update players' memory and clients
								final MapCoordinates3DEx auraLocation = new MapCoordinates3DEx (x, y, z);
								
								getFogOfWarMidTurnChanges ().updatePlayerMemoryOfTerrain (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
									mom.getPlayers (), auraLocation, mom.getSessionDescription ().getFogOfWarSetting ().getTerrainAndNodeAuras ());
							}
						}
			}
			
			resultsMap.get (castingPlayer.getPlayerDescription ().getPlayerID ()).add (result);
			
			final OverlandMapTerrainData terrainData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(targetWarpedNode.getZ ()).getRow ().get (targetWarpedNode.getY ()).getCell ().get (targetWarpedNode.getX ()).getTerrainData ();
			
			if (!castingPlayer.getPlayerDescription ().getPlayerID ().equals (terrainData.getNodeOwnerID ()))
			{
				List<DispelMagicResult> results = resultsMap.get (terrainData.getNodeOwnerID ());
				if (results == null)
				{
					results = new ArrayList<DispelMagicResult> ();
					resultsMap.put (terrainData.getNodeOwnerID (), results);
				}
				results.add (result);
			}
		}
		
		// Also try to dispel vortexes
		if (targetVortexes != null)
			for (final MemoryUnit vortex : targetVortexes)
			{
				// Find the spell that summoned it
				final Spell vortexSpell = mom.getServerDB ().getSpell ().stream ().filter
					(s -> s.getSummonedUnit ().contains (vortex.getUnitID ())).findAny ().orElse (null);
				if (vortexSpell != null)
				{
					final DispelMagicResult result = new DispelMagicResult ();
					result.setOwningPlayerID (vortex.getOwningPlayerID ());
					result.setSpellID (vortexSpell.getSpellID ());
					result.setCastingCost (vortexSpell.getCombatCastingCost ());
					
					// Retorts that make spells more difficult to dispel
					int multiplier = 1;
					
					final List<PlayerPick> spellOwnerPicks = getKnownWizardUtils ().findKnownWizardDetails (mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (),
						vortex.getOwningPlayerID (), "processDispelling (V1)").getPick ();
					
					if (getPlayerPickUtils ().getQuantityOfPick (spellOwnerPicks, CommonDatabaseConstants.RETORT_ID_ARCHMAGE) > 0)
						multiplier++;
					
					if ((masteries.containsKey (vortexSpell.getSpellRealm ())) &&
						(getPlayerPickUtils ().getQuantityOfPick (spellOwnerPicks, masteries.get (vortexSpell.getSpellRealm ())) > 0))
						
						multiplier++;
					
					if (multiplier > 1)
						result.setCastingCost (result.getCastingCost () * multiplier);
					
					result.setChance (dispellingPower.doubleValue () / (result.getCastingCost () + dispellingPower));
					result.setDispelled ((getRandomUtils ().nextInt (result.getCastingCost () + dispellingPower) < dispellingPower));

					if (result.isDispelled ())
						mom.getWorldUpdates ().killUnit (vortex.getUnitURN (), KillUnitActionID.PERMANENT_DAMAGE);
					
					resultsMap.get (castingPlayer.getPlayerDescription ().getPlayerID ()).add (result);
					
					List<DispelMagicResult> results = resultsMap.get (vortex.getOwningPlayerID ());
					if (results == null)
					{
						results = new ArrayList<DispelMagicResult> ();
						resultsMap.put (vortex.getOwningPlayerID (), results);
					}
					results.add (result);
				}
			}
		
		// Send the results to each human player invovled
		final DispelMagicResultsMessage msg = new DispelMagicResultsMessage ();
		msg.setCastingPlayerID (castingPlayer.getPlayerDescription ().getPlayerID ());
		msg.setSpellID (spell.getSpellID ());
		
		for (final Entry<Integer, List<DispelMagicResult>> entry : resultsMap.entrySet ())
		{
			// Send dispel results to human players
			final PlayerServerDetails entryPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), entry.getKey (), "processDispelling (D3)");
			if (entryPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
			{
				msg.getDispelMagicResult ().clear ();
				msg.getDispelMagicResult ().addAll (entry.getValue ());
				
				entryPlayer.getConnection ().sendMessageToClient (msg);
			}
			
			// Anyone who had their spells attempted to be dispelled will be upset at the caster
			if ((penaltyToVisibleRelation > 0) && (!castingPlayer.getPlayerDescription ().getPlayerID ().equals (entry.getKey ())))
			{
				final MomPersistentPlayerPrivateKnowledge entryPriv = (MomPersistentPlayerPrivateKnowledge) entryPlayer.getPersistentPlayerPrivateKnowledge ();
				final KnownWizardDetails entryOpinionOfCaster = getKnownWizardUtils ().findKnownWizardDetails (entryPriv.getFogOfWarMemory ().getWizardDetails (), castingPlayer.getPlayerDescription ().getPlayerID ());
				if (entryOpinionOfCaster != null)
					getRelationAI ().penaltyToVisibleRelation ((DiplomacyWizardDetails) entryOpinionOfCaster, penaltyToVisibleRelation);
			}
		}
		
		return mom.getWorldUpdates ().process (mom);
	}
	
	/**
	 * Makes dispel rolls that try to block a spell from ever being cast in combat in the first place (nodes and Counter Magic)
	 * 
	 * @param castingPlayer Player who is trying to cast a spell
	 * @param spell The spell they are trying to cast
	 * @param unmodifiedCastingCost Unmodified mana cost of the spell they are trying to cast, including any extra MP for variable damage 
	 * @param combatLocation Location of the combat where this spell is being cast; null = being cast overland
	 * @param defendingPlayer Defending player in the combat
	 * @param attackingPlayer Attacking player in the combat
	 * @param triggerSpellDef Additional spell that's trying to counter the spell from being cast
	 * @param triggerSpellCasterPlayerID Player who cast the additional spell that's trying to counter the spell from being cast
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether the spell was successfully cast or not; so false = was dispelled
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final boolean processCountering (final PlayerServerDetails castingPlayer, final Spell spell, final int unmodifiedCastingCost,
		final MapCoordinates3DEx combatLocation, final PlayerServerDetails defendingPlayer, final PlayerServerDetails attackingPlayer,
		final Spell triggerSpellDef, final Integer triggerSpellCasterPlayerID, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		final List<CounterMagicResult> results = new ArrayList<CounterMagicResult> ();

		final Map<String, String> masteries = mom.getServerDB ().getPick ().stream ().filter (p -> p.getNodeAndDispelBonus () != null).collect (Collectors.toMap
			(p -> p.getNodeAndDispelBonus (), p -> p.getPickID ()));
		
		// Retorts that make spells more difficult to dispel
		int multiplier = 1;
		
		final List<PlayerPick> picks = getKnownWizardUtils ().findKnownWizardDetails (mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (),
			castingPlayer.getPlayerDescription ().getPlayerID (), "processCountering (C)").getPick ();

		if (getPlayerPickUtils ().getQuantityOfPick (picks, CommonDatabaseConstants.RETORT_ID_ARCHMAGE) > 0)
			multiplier++;

		if ((masteries.containsKey (spell.getSpellRealm ())) &&
			(getPlayerPickUtils ().getQuantityOfPick (picks, masteries.get (spell.getSpellRealm ())) > 0))
				
			multiplier++;
		
		final int castingCost = unmodifiedCastingCost * multiplier;
		
		// As soon as one CAE blocks it, we don't bother keep rolling any more
		boolean dispelled = false;
		
		// Need to copy the list as we might remove CAEs as we go
		final List<MemoryCombatAreaEffect> trueCAEs = new ArrayList<MemoryCombatAreaEffect> ();
		trueCAEs.addAll (mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect ());
		
		// Look for CAEs in this location that have a dispelling power value defined
		for (final MemoryCombatAreaEffect cae : trueCAEs)
			if ((!dispelled) && (combatLocation != null) && (combatLocation.equals (cae.getMapLocation ())) &&
				((cae.getCastingPlayerID () == null) || (!cae.getCastingPlayerID ().equals (castingPlayer.getPlayerDescription ().getPlayerID ()))))
			{
				final CombatAreaEffect caeDef = mom.getServerDB ().findCombatAreaEffect (cae.getCombatAreaEffectID (), "processCountering");
				if ((caeDef.getDispellingPower () != null) &&
						
					// Nature nodes don't counter Nature spells
					((caeDef.getCombatAreaEffectMagicRealm () == null) || (!caeDef.getCombatAreaEffectMagicRealm ().equals (spell.getSpellRealm ()))) &&
					
					// Node dispelling negated by Node Mastery
					((caeDef.getCombatAreaEffectNegatedByPick () == null) || (getPlayerPickUtils ().getQuantityOfPick (picks, caeDef.getCombatAreaEffectNegatedByPick ()) < 1)))
					
				{
					final CounterMagicResult result = new CounterMagicResult ();
					result.setOwningPlayerID (cae.getCastingPlayerID ());
					result.setCombatAreaEffectID (cae.getCombatAreaEffectID ());
					result.setDispellingPower ((caeDef.getDispellingPower () > 0) ? caeDef.getDispellingPower () : cae.getCastingCost ());
					result.setChance (Integer.valueOf (result.getDispellingPower ()).doubleValue () / (castingCost + result.getDispellingPower ()));
					result.setDispelled ((getRandomUtils ().nextInt (castingCost + result.getDispellingPower ()) < result.getDispellingPower ()));
					results.add (result);
					
					if (result.isDispelled ())
						dispelled = true;
					
					// Decrease remaining power of Counter Magic
					if (caeDef.getDispellingPower () < 0)
					{
						cae.setCastingCost (cae.getCastingCost () + caeDef.getDispellingPower ());
						if (cae.getCastingCost () > 0)
							getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCombatAreaEffect (cae, mom.getPlayers (), mom.getSessionDescription ());
						else
						{
							mom.getWorldUpdates ().removeCombatAreaEffect (cae.getCombatAreaEffectURN ());
							mom.getWorldUpdates ().process (mom);
						}
					}
				}
			}
		
		// Is there an additional spell (that isn't a CAE) that will try to counter it?
		// NB. wiki says that Runemaster doesn't affect countering, only dispelling
		if ((!dispelled) && (triggerSpellDef != null))
		{
			final CounterMagicResult result = new CounterMagicResult ();
			result.setOwningPlayerID (triggerSpellCasterPlayerID);
			result.setSpellID (triggerSpellDef.getSpellID ());
			result.setDispellingPower (triggerSpellDef.getTriggerDispelPower ());
			result.setChance (Integer.valueOf (result.getDispellingPower ()).doubleValue () / (castingCost + result.getDispellingPower ()));
			result.setDispelled ((getRandomUtils ().nextInt (castingCost + result.getDispellingPower ()) < result.getDispellingPower ()));
			results.add (result);
			
			if (result.isDispelled ())
				dispelled = true;
		}
		
		// Any results to send to any human players?
		if ((results.size () > 0) && ((attackingPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN) || (defendingPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)))
		{
			final CounterMagicResultsMessage msg = new CounterMagicResultsMessage ();
			msg.setCastingPlayerID (castingPlayer.getPlayerDescription ().getPlayerID ());
			msg.setSpellID (spell.getSpellID ());
			msg.getCounterMagicResult ().addAll (results);
			
			if (attackingPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
				attackingPlayer.getConnection ().sendMessageToClient (msg);

			if (defendingPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
				defendingPlayer.getConnection ().sendMessageToClient (msg);
		}
		
		return !dispelled;
	}
	
	/**
	 * @param player AI player who is casting Disenchant Area
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Best location for AI to target an overland Disenchant Area spell
	 * @throws RecordNotFoundException If an expected data item cannot be found
	 */
	@Override
	public final MapCoordinates3DEx chooseDisenchantAreaTarget (final PlayerServerDetails player, final MomSessionVariables mom)
		throws RecordNotFoundException
	{
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		
		MapCoordinates3DEx bestTarget = null;
		Integer bestCount = null;
		
		// First look for our cities or warped nodes
		for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
			for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
				for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
				{
					final MemoryGridCell mc = priv.getFogOfWarMemory ().getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x);
					final MapCoordinates3DEx coords = new MapCoordinates3DEx (x, y, z);

					int thisCount = 0;					
					if ((mc != null) && (mc.getCityData () != null) && (mc.getCityData ().getCityOwnerID () == player.getPlayerDescription ().getPlayerID ()))
						
						// How many enemy spells are here?
						thisCount = (int) priv.getFogOfWarMemory ().getMaintainedSpell ().stream ().filter
							(s -> (s.getCastingPlayerID () != player.getPlayerDescription ().getPlayerID ()) && (coords.equals (s.getCityLocation ()))).count ();

					if ((thisCount == 0) && (mc != null) && (mc.getTerrainData () != null) && (mc.getTerrainData ().getNodeOwnerID () != null) &&
						(mc.getTerrainData ().getNodeOwnerID () == player.getPlayerDescription ().getPlayerID ()) &&
						(mc.getTerrainData ().isWarped () != null) && (mc.getTerrainData ().isWarped ()))
					{
						// Has to be actual node, not just an aura tile
						if (mom.getServerDB ().findTileType (mc.getTerrainData ().getTileTypeID (), "chooseDisenchantAreaTarget").getMagicRealmID () != null)
							thisCount = 1;
					}
					
					if ((thisCount > 0) && ((bestCount == null) || (thisCount > bestCount)))
					{
						bestCount = thisCount;
						bestTarget = coords;
					}
				}
		
		// If we didn't find any, look for positive spells cast on enemy cities or unit stacks, where the ONLY spells cast at
		// that location belong to the city or unit owner, or us.  Otherwise if someone else has curses cast
		// on the city, we might accidentally help remove them.
		if (bestTarget == null)
		{
			for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
				for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
					for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
					{
						final MemoryGridCell mc = priv.getFogOfWarMemory ().getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x);
						final MapCoordinates3DEx coords = new MapCoordinates3DEx (x, y, z);

						int thisCount = 0;					
						if ((mc != null) && (mc.getCityData () != null) && (mc.getCityData ().getCityOwnerID () != player.getPlayerDescription ().getPlayerID ()))
						{
							if (priv.getFogOfWarMemory ().getMaintainedSpell ().stream ().anyMatch
								(s -> (s.getCastingPlayerID () != player.getPlayerDescription ().getPlayerID ()) && (s.getCastingPlayerID () != mc.getCityData ().getCityOwnerID ()) &&
									(coords.equals (s.getCityLocation ()))))
								
								thisCount = -1;
							else
								thisCount = (int) priv.getFogOfWarMemory ().getMaintainedSpell ().stream ().filter (s -> coords.equals (s.getCityLocation ())).count ();
						}
						
						if (thisCount >= 0)
						{
							// Look for enemy units at this location
							final List<MemoryUnit> enemyUnits = priv.getFogOfWarMemory ().getUnit ().stream ().filter
								(u -> (u.getOwningPlayerID () != player.getPlayerDescription ().getPlayerID ()) && (coords.equals (u.getUnitLocation ()))).collect (Collectors.toList ());
							for (final MemoryUnit enemyUnit : enemyUnits)
								if (priv.getFogOfWarMemory ().getMaintainedSpell ().stream ().anyMatch
									(s -> (s.getCastingPlayerID () != player.getPlayerDescription ().getPlayerID ()) && (s.getCastingPlayerID () != enemyUnit.getOwningPlayerID ()) &&
										(s.getUnitURN () != null) && (s.getUnitURN () == enemyUnit.getUnitURN ())))
								
									thisCount = -1;
							
								else if (thisCount >= 0)
									thisCount = thisCount + (int) priv.getFogOfWarMemory ().getMaintainedSpell ().stream ().filter
										(s -> (s.getUnitURN () != null) && (s.getUnitURN () == enemyUnit.getUnitURN ())).count ();
						}
						
						if ((thisCount > 0) && ((bestCount == null) || (thisCount > bestCount)))
						{
							bestCount = thisCount;
							bestTarget = coords;
						}
					}
		}
		
		return bestTarget;
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
	 * @return Memory CAE utils
	 */
	public final MemoryCombatAreaEffectUtils getMemoryCombatAreaEffectUtils ()
	{
		return memoryCombatAreaEffectUtils;
	}

	/**
	 * @param utils Memory CAE utils
	 */
	public final void setMemoryCombatAreaEffectUtils (final MemoryCombatAreaEffectUtils utils)
	{
		memoryCombatAreaEffectUtils = utils;
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

	/**
	 * @return Spell processing methods
	 */
	public final SpellProcessing getSpellProcessing ()
	{
		return spellProcessing;
	}

	/**
	 * @param obj Spell processing methods
	 */
	public final void setSpellProcessing (final SpellProcessing obj)
	{
		spellProcessing = obj;
	}

	/**
	 * @return Player pick utils
	 */
	public final PlayerPickUtils getPlayerPickUtils ()
	{
		return playerPickUtils;
	}

	/**
	 * @param utils Player pick utils
	 */
	public final void setPlayerPickUtils (final PlayerPickUtils utils)
	{
		playerPickUtils = utils;
	}

	/**
	 * @return Methods for finding KnownWizardDetails from the list
	 */
	public final KnownWizardUtils getKnownWizardUtils ()
	{
		return knownWizardUtils;
	}

	/**
	 * @param k Methods for finding KnownWizardDetails from the list
	 */
	public final void setKnownWizardUtils (final KnownWizardUtils k)
	{
		knownWizardUtils = k;
	}

	/**
	 * @return For calculating relation scores between two wizards
	 */
	public final RelationAI getRelationAI ()
	{
		return relationAI;
	}

	/**
	 * @param ai For calculating relation scores between two wizards
	 */
	public final void setRelationAI (final RelationAI ai)
	{
		relationAI = ai;
	}
}