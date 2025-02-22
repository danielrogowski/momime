package momime.server.process;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.SquareMapDirection;
import com.ndg.map.areas.operations.MapAreaOperations2D;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.sessionbase.PlayerType;
import com.ndg.utils.random.RandomUtils;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.calculations.CityCalculationsImpl;
import momime.common.calculations.SkillCalculations;
import momime.common.database.AttackSpellTargetID;
import momime.common.database.CitySpellEffect;
import momime.common.database.CitySpellEffectTileType;
import momime.common.database.CombatMapLayerID;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageType;
import momime.common.database.HeroItem;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.SpellValidMapFeatureTarget;
import momime.common.database.StoredDamageTypeID;
import momime.common.database.Unit;
import momime.common.database.UnitCanCast;
import momime.common.database.UnitCombatSideID;
import momime.common.database.UnitSkillAndValue;
import momime.common.database.UnitSpellEffect;
import momime.common.messages.DiplomacyWizardDetails;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomCombatTile;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.NewTurnMessageCreateArtifact;
import momime.common.messages.NewTurnMessageSpell;
import momime.common.messages.NewTurnMessageSpellBlast;
import momime.common.messages.NewTurnMessageTypeID;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.OverlandMapTerrainData;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.common.messages.UnitDamage;
import momime.common.messages.UnitStatusID;
import momime.common.messages.WizardState;
import momime.common.messages.servertoclient.AddUnassignedHeroItemMessage;
import momime.common.messages.servertoclient.AnimationID;
import momime.common.messages.servertoclient.FullSpellListMessage;
import momime.common.messages.servertoclient.PlayAnimationMessage;
import momime.common.messages.servertoclient.RemoveQueuedSpellMessage;
import momime.common.messages.servertoclient.ShowSpellAnimationMessage;
import momime.common.messages.servertoclient.UpdateCombatMapMessage;
import momime.common.messages.servertoclient.UpdateManaSpentOnCastingCurrentSpellMessage;
import momime.common.messages.servertoclient.UpdateWizardStateMessage;
import momime.common.movement.MovementUtils;
import momime.common.utils.CombatMapUtils;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.KindOfSpell;
import momime.common.utils.KindOfSpellUtils;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.SpellCastType;
import momime.common.utils.SpellTargetingUtils;
import momime.common.utils.SpellUtils;
import momime.common.utils.TargetSpellResult;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.ai.RelationAI;
import momime.server.ai.SpellAI;
import momime.server.calculations.AttackDamage;
import momime.server.calculations.DamageCalculator;
import momime.server.calculations.ServerCityCalculations;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.calculations.ServerSpellCalculations;
import momime.server.calculations.ServerUnitCalculations;
import momime.server.database.ServerDatabaseValues;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.FogOfWarMidTurnMultiChanges;
import momime.server.fogofwar.FogOfWarProcessing;
import momime.server.fogofwar.KillUnitActionID;
import momime.server.knowledge.CombatDetails;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.mapgenerator.CombatMapArea;
import momime.server.mapgenerator.CombatMapGenerator;
import momime.server.utils.CombatMapServerUtils;
import momime.server.utils.HeroItemServerUtils;
import momime.server.utils.KnownWizardServerUtils;
import momime.server.utils.OverlandMapServerUtils;
import momime.server.utils.SpellServerUtils;
import momime.server.utils.UnitAddLocation;
import momime.server.utils.UnitServerUtils;

/**
 * Methods for processing the effects of spells that have completed casting
 */
public final class SpellProcessingImpl implements SpellProcessing
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (SpellProcessingImpl.class);
	
	/** Spell utils */
	private SpellUtils spellUtils;
	
	/** Memory building utils */
	private MemoryBuildingUtils memoryBuildingUtils;
	
	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** Methods that determine whether something is a valid target for a spell */
	private SpellTargetingUtils spellTargetingUtils;
	
	/** Memory CAE utils */
	private MemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;

	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;

	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnMultiChanges fogOfWarMidTurnMultiChanges;
	
	/** Combat processing */
	private CombatProcessing combatProcessing;
	
	/** Resource calculations */
	private ServerResourceCalculations serverResourceCalculations;

	/** Server-only overland map utils */
	private OverlandMapServerUtils overlandMapServerUtils;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
	/** Damage processor */
	private DamageProcessor damageProcessor;
	
	/** Map generator */
	private CombatMapGenerator combatMapGenerator;
	
	/** Methods dealing with hero items */
	private HeroItemServerUtils heroItemServerUtils;

	/** Starting and ending combats */
	private CombatStartAndEnd combatStartAndEnd;
	
	/** Operations for processing combat maps */
	private MapAreaOperations2D<MomCombatTile> combatMapOperations;

	/** City calculations */
	private CityCalculations cityCalculations;
	
	/** Coordinate system utils */
	private CoordinateSystemUtils coordinateSystemUtils;

	/** Main FOW update routine */
	private FogOfWarProcessing fogOfWarProcessing;
	
	/** Methods for dealing with player msgs */
	private PlayerMessageProcessing playerMessageProcessing;
	
	/** AI decisions about spells */
	private SpellAI spellAI;
	
	/** Server-only spell calculations */
	private ServerSpellCalculations serverSpellCalculations;
	
	/** Dispel magic processing */
	private SpellDispelling spellDispelling;
	
	/** Casting for each type of spell */
	private SpellCasting spellCasting;
	
	/** Kind of spell utils */
	private KindOfSpellUtils kindOfSpellUtils;
	
	/** City processing methods */
	private CityProcessing cityProcessing;
	
	/** Server-only city calculations */
	private ServerCityCalculations serverCityCalculations;
	
	/** Damage calc */
	private DamageCalculator damageCalculator;
	
	/** Attack resolution processing */
	private AttackResolutionProcessing attackResolutionProcessing;
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** Methods mainly dealing with when very rare overland enchantments are triggered */
	private SpellTriggers spellTriggers;
	
	/** Skill calculations */
	private SkillCalculations skillCalculations;
	
	/** Server-only unit calculations */
	private ServerUnitCalculations serverUnitCalculations;
	
	/** Combat map utils */
	private CombatMapUtils combatMapUtils;
	
	/** Movement utils */
	private MovementUtils movementUtils;
	
	/** Casting spells that have more than one effect */
	private SpellMultiCasting spellMultiCasting;
	
	/** Methods for working with wizardIDs */
	private PlayerKnowledgeUtils playerKnowledgeUtils;
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/** Process for making sure one wizard has met another wizard */
	private KnownWizardServerUtils knownWizardServerUtils;
	
	/** Methods dealing with combat maps that are only needed on the server */
	private CombatMapServerUtils combatMapServerUtils;
	
	/** Server-side only spell utils */
	private SpellServerUtils spellServerUtils;
	
	/** For calculating relation scores between two wizards */
	private RelationAI relationAI;
	
	/**
	 * Handles casting an overland spell, i.e. when we've finished channeling sufficient mana in to actually complete the casting
	 *
	 * @param castingPlayer Player who is casting the spell
	 * @param spell Which spell is being cast
	 * @param variableDamage Chosen damage selected for the spell, for spells like fire bolt where a varying amount of mana can be channeled into the spell
	 * @param heroItem The item being created; null for spells other than Enchant Item or Create Artifact
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void castOverlandNow (final PlayerServerDetails castingPlayer, final Spell spell, final Integer variableDamage, final HeroItem heroItem, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		final KnownWizardDetails castingWizard = getKnownWizardUtils ().findKnownWizardDetails (mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (),
			castingPlayer.getPlayerDescription ().getPlayerID (), "castOverlandNow");

		// Does the magic realm of the cast spell trigger an affect from any overland enchantments?  e.g. casting Death/Chaos spells while Nature's Wrath in effect
		boolean passesCounteringAttempts = true;
		if (!spell.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_SPELL_OF_RETURN))
		{
			final int unmodifiedOverlandCastingCost = getSpellUtils ().getUnmodifiedOverlandCastingCost (spell, heroItem, variableDamage, castingWizard.getPick (), mom.getServerDB ());
			
			// Don't trigger the same spell multiple times, even if multiple enemy wizards have it cast
			// Copy list, as have seen a ConcurrentModificationException here
			final Set<String> triggeredSpells = new HashSet<String> (); 
			final List<MemoryMaintainedSpell> copyOfSpells = new ArrayList<MemoryMaintainedSpell> (mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ());
			
			for (final MemoryMaintainedSpell triggerSpell : copyOfSpells)
				if (!triggeredSpells.contains (triggerSpell.getSpellID ()))
				{
					final Spell triggerSpellDef = mom.getServerDB ().findSpell (triggerSpell.getSpellID (), "castOverlandNow");
					if ((triggerSpellDef.getSpellBookSectionID () == SpellBookSectionID.OVERLAND_ENCHANTMENTS) &&
							
						// Any spell with a dispel power defined can also be triggered with an empty spell realm list (for Suppress Magic)
						(((spell.getSpellRealm () != null) && (triggerSpellDef.getTriggeredBySpellRealm ().contains (spell.getSpellRealm ()))) ||
							((triggerSpellDef.getTriggeredBySpellRealm ().size () == 0) && (triggerSpellDef.getTriggerDispelPower () != null))) &&
						
						((triggerSpell.getCastingPlayerID () != castingPlayer.getPlayerDescription ().getPlayerID ()) ||
							((triggerSpellDef.isTriggeredBySelf () != null) && (triggerSpellDef.isTriggeredBySelf ()))))
					{
						triggeredSpells.add (triggerSpell.getSpellID ());
						
						if (!getSpellTriggers ().triggerSpell (triggerSpell, castingPlayer, spell, unmodifiedOverlandCastingCost, mom))
							passesCounteringAttempts = false;
					}
				}
		}
		
		if (passesCounteringAttempts)
		{
			// Modifying this by section is really only a safeguard to protect against casting spells which we don't have researched yet
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) castingPlayer.getPersistentPlayerPrivateKnowledge ();
			final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) castingPlayer.getTransientPlayerPrivateKnowledge ();
			final SpellResearchStatus researchStatus = getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), spell.getSpellID ());
			final SpellBookSectionID sectionID = getSpellUtils ().getModifiedSectionID (spell, researchStatus.getStatus (), true);
			final KindOfSpell kind = getKindOfSpellUtils ().determineKindOfSpell (spell, null);
	
			// Overland enchantments
			if (sectionID == SpellBookSectionID.OVERLAND_ENCHANTMENTS)
			{
				// Check if the player already has this overland enchantment cast
				// If they do, they can't have it twice so nothing to do, they just lose the cast
				if (getMemoryMaintainedSpellUtils ().findMaintainedSpell (mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), castingPlayer.getPlayerDescription ().getPlayerID (), spell.getSpellID (), null, null, null, null) == null)
				{
					getKnownWizardServerUtils ().meetWizard (castingPlayer.getPlayerDescription ().getPlayerID (), null, false, mom);
					
					// Add it on server and anyone who can see it (which, because its an overland enchantment, will be everyone)
					getFogOfWarMidTurnChanges ().addMaintainedSpellOnServerAndClients
						(castingPlayer.getPlayerDescription ().getPlayerID (), spell.getSpellID (), null, null, false, null, null, variableDamage, false, true, mom);
	
					// Does this overland enchantment give a global combat area effect? (Not all do)
					if (spell.getSpellHasCombatEffect ().size () > 0)
					{
						// Pick one at random
						final String combatAreaEffectID = spell.getSpellHasCombatEffect ().get (getRandomUtils ().nextInt (spell.getSpellHasCombatEffect ().size ()));
						getFogOfWarMidTurnChanges ().addCombatAreaEffectOnServerAndClients (mom.getGeneralServerKnowledge (), combatAreaEffectID, spell.getSpellID (),
							castingPlayer.getPlayerDescription ().getPlayerID (), spell.getOverlandCastingCost (), null, mom.getPlayers (), mom.getSessionDescription ());
					}
					
					// If it is Detect Magic, the player now learns what spells everyone is casting overland
					if (spell.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_DETECT_MAGIC))
						getSpellCasting ().sendOverlandCastingInfo (spell.getSpellID (), castingPlayer.getPlayerDescription ().getPlayerID (), mom);
				}
			}
			
			// Enchant item / Create artifact
			else if (kind == KindOfSpell.CREATE_ARTIFACT)
			{
				// Put new item in mom.getPlayers ()' bank on the server
				final NumberedHeroItem numberedHeroItem = getHeroItemServerUtils ().createNumberedHeroItem (heroItem, mom.getGeneralServerKnowledge ());
				priv.getUnassignedHeroItem ().add (numberedHeroItem);
	
				// Put new item in mom.getPlayers ()' bank on the client
				if (castingPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
				{
					final AddUnassignedHeroItemMessage addItemMsg = new AddUnassignedHeroItemMessage ();
					addItemMsg.setHeroItem (numberedHeroItem);
					castingPlayer.getConnection ().sendMessageToClient (addItemMsg);
				
					// Show on new turn messages for the player who summoned it
					final NewTurnMessageCreateArtifact createArtifactSpell = new NewTurnMessageCreateArtifact ();
					createArtifactSpell.setMsgType (NewTurnMessageTypeID.CREATE_ARTIFACT);
					createArtifactSpell.setSpellID (spell.getSpellID ());
					createArtifactSpell.setHeroItemName (heroItem.getHeroItemName ());
	
					trans.getNewTurnMessage ().add (createArtifactSpell);
				}
			}
	
			// Summoning, except Floating Island where you need to pick a target
			else if ((kind == KindOfSpell.SUMMONING) && ((spell.isSummonAnywhere () == null) || (!spell.isSummonAnywhere ())))
			{
				// Find the location of the wizards' summoning circle 'building'
				final MemoryBuilding summoningCircleLocation = getMemoryBuildingUtils ().findCityWithBuilding (castingPlayer.getPlayerDescription ().getPlayerID (),
					CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE, mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ());
	
				if (summoningCircleLocation != null)
					getSpellCasting ().castOverlandSummoningSpell (spell, castingPlayer, (MapCoordinates3DEx) summoningCircleLocation.getCityLocation (), true, mom);
			}
	
			// Any kind of overland spell that needs the player to choose a target
			else if ((sectionID == SpellBookSectionID.CITY_ENCHANTMENTS) || (sectionID == SpellBookSectionID.UNIT_ENCHANTMENTS) ||
				(sectionID == SpellBookSectionID.CITY_CURSES) || (sectionID == SpellBookSectionID.UNIT_CURSES) || (sectionID == SpellBookSectionID.SPECIAL_UNIT_SPELLS) ||
				(sectionID == SpellBookSectionID.SPECIAL_OVERLAND_SPELLS) || (sectionID == SpellBookSectionID.DISPEL_SPELLS) ||
				(sectionID == SpellBookSectionID.ATTACK_SPELLS) ||
				(sectionID == SpellBookSectionID.SUMMONING) || (sectionID == SpellBookSectionID.ENEMY_WIZARD_SPELLS) || (kind == KindOfSpell.RAISE_DEAD))
			{
				// Add it on server - note we add it without a target chosen and without adding it on any
				// clients - clients don't know about spells until the target has been chosen, since they might hit cancel or have no appropriate target.
				final MemoryMaintainedSpell maintainedSpell = getFogOfWarMidTurnChanges ().addMaintainedSpellOnServerAndClients
					(castingPlayer.getPlayerDescription ().getPlayerID (), spell.getSpellID (), null, null, false, null, null, variableDamage, false, false, mom);
	
				if (castingPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
				{
					// Tell client to pick a target for this spell
					final NewTurnMessageSpell targetSpell = new NewTurnMessageSpell ();
					targetSpell.setMsgType (NewTurnMessageTypeID.TARGET_SPELL);
					targetSpell.setSpellID (spell.getSpellID ());
					trans.getNewTurnMessage ().add (targetSpell);
				}
				else
					getSpellAI ().decideOverlandSpellTarget (castingPlayer, castingWizard, spell, maintainedSpell, mom);
			}
			
			// Special spells (Spell of Mastery and a few other unique spells that you don't even pick a target for)
			else if (sectionID == SpellBookSectionID.SPECIAL_SPELLS)
			{
				getKnownWizardServerUtils ().meetWizard (castingPlayer.getPlayerDescription ().getPlayerID (), null, false, mom);
				
				if (spell.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_SPELL_OF_MASTERY))
				{
					final PlayAnimationMessage msg = new PlayAnimationMessage ();
					msg.setAnimationID (AnimationID.FINISHED_SPELL_OF_MASTERY);
					msg.setPlayerID (castingPlayer.getPlayerDescription ().getPlayerID ());
					
					getMultiplayerSessionServerUtils ().sendMessageToAllClients (mom.getPlayers (), msg);
					
					// Defeat everyone except the winner
					for (final PlayerServerDetails defeatedPlayer : mom.getPlayers ())
						if (defeatedPlayer != castingPlayer)
						{
							final KnownWizardDetails defeatedWizard = getKnownWizardUtils ().findKnownWizardDetails
								(mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), defeatedPlayer.getPlayerDescription ().getPlayerID (), "castOverlandNow");
							
							if ((getPlayerKnowledgeUtils ().isWizard (defeatedWizard.getWizardID ())) && (defeatedWizard.getWizardState () != WizardState.DEFEATED))
							{
								getKnownWizardServerUtils ().updateWizardState (defeatedPlayer.getPlayerDescription ().getPlayerID (), WizardState.DEFEATED, mom);

								if (defeatedPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
									mom.updateHumanPlayerToAI (defeatedPlayer.getPlayerDescription ().getPlayerID ());
							}
						}
					
					// Let the remaining player win (this kicks them and ends the session)
					getPlayerMessageProcessing ().checkIfWonGame (mom);
				}
				else
				{
					// Global attack spell that hits every unit on both planes (subject to being valid targets)
					// First of all show anim for it - these show the mirror like overland enchantments
					final ShowSpellAnimationMessage anim = new ShowSpellAnimationMessage ();
					anim.setSpellID (spell.getSpellID ());
					anim.setCastingPlayerID (castingPlayer.getPlayerDescription ().getPlayerID ());
				
					getMultiplayerSessionServerUtils ().sendMessageToAllClients (mom.getPlayers (), anim);
					
					// Find all target locations
					final List<MemoryUnit> targetUnits = getSpellServerUtils ().listGlobalAttackTargets (spell, castingPlayer, false, mom);
					if (!targetUnits.isEmpty ())
					{
						final List<MapCoordinates3DEx> unitLocations = targetUnits.stream ().map (u -> (MapCoordinates3DEx) u.getUnitLocation ()).distinct ().collect (Collectors.toList ());
					
						// Roll all units at once
						getSpellCasting ().castOverlandAttackSpell (castingPlayer, null, spell, variableDamage, unitLocations, 40, mom);
					}
				}
			}
	
			else
				throw new MomException ("Completed casting an overland spell with a section ID that there is no code to deal with yet: " + sectionID);
		}
	}
	
	/**
	 * Handles casting a spell in combat, after all validation has passed.
	 * If its a spell where we need to choose a target (like Doom Bolt or Phantom Warriors), additional mana (like Counter Magic)
	 * or both (like Firebolt), then the client will already have done all this and supplied us with the chosen values.
	 * 
	 * @param castingPlayer Player who is casting the spell
	 * @param xuCombatCastingUnit Unit who is casting the spell; null means its the wizard casting, rather than a specific unit
	 * @param combatCastingFixedSpellNumber For casting fixed spells the unit knows (e.g. Giant Spiders casting web), indicates the spell number; for other types of casting this is null
	 * @param combatCastingSlotNumber For casting spells imbued into hero items, this is the number of the slot (0, 1 or 2); for other types of casting this is null
	 * @param spell Which spell they want to cast
	 * @param reducedCombatCastingCost Skill cost of the spell, reduced by any book or retort bonuses the player may have
	 * @param multipliedManaCost MP cost of the spell, reduced as above, then multiplied up according to the distance the combat is from the wizard's fortress
	 * @param variableDamage Chosen damage selected for the spell, for spells like fire bolt where a varying amount of mana can be channeled into the spell
	 * @param combatLocation Location of the combat where this spell is being cast; null = being cast overland
	 * @param defendingPlayer Defending player in the combat
	 * @param attackingPlayer Attacking player in the combat
	 * @param targetUnit Unit to target the spell on, if appropriate for spell book section, otherwise null
	 * @param targetLocation Location to target the spell at, if appropriate for spell book section, otherwise null
	 * @param skipAnimation Tell the client to skip showing any animation and sound effect associated with this spell
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether the spell cast was an attack that resulted in the combat ending
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final boolean castCombatNow (final PlayerServerDetails castingPlayer, final ExpandedUnitDetails xuCombatCastingUnit, final Integer combatCastingFixedSpellNumber,
		final Integer combatCastingSlotNumber, final Spell spell, final int reducedCombatCastingCost, final int multipliedManaCost,
		final Integer variableDamage, final MapCoordinates3DEx combatLocation, final PlayerServerDetails defendingPlayer, final PlayerServerDetails attackingPlayer,
		final MemoryUnit targetUnit, final MapCoordinates2DEx targetLocation, final boolean skipAnimation, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		// Which side is casting the spell
		final UnitCombatSideID castingSide;
		if (castingPlayer == attackingPlayer)
			castingSide = UnitCombatSideID.ATTACKER;
		else if (castingPlayer == defendingPlayer)
			castingSide = UnitCombatSideID.DEFENDER;
		else
			throw new MomException ("castCombatNow: Casting player is neither the attacker nor defender");

		// Keep track of if we, or if resolveAttack, called combatEnded
		boolean combatEnded = false;

		final ServerGridCellEx gc = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
			(combatLocation.getZ ()).getRow ().get (combatLocation.getY ()).getCell ().get (combatLocation.getX ());

		final CombatDetails combatDetails = getCombatMapServerUtils ().findCombatByLocation (mom.getCombatDetails (), combatLocation, "castCombatNow");
		
		// Set this if we need to call combatEnded at the end
		PlayerServerDetails winningPlayer = null;
		
		// Some natural abilities that are not really "spells" as such are immune to counter magic, e.g. Giant Spiders' web
		boolean immuneToCounterMagic = false;
		if (combatCastingFixedSpellNumber != null)
		{
			final Unit unitDef = mom.getServerDB ().findUnit (xuCombatCastingUnit.getUnitID (), "castCombatNow");
			final UnitCanCast unitCanCast = unitDef.getUnitCanCast ().get (combatCastingFixedSpellNumber);
			if ((unitCanCast.isImmuneToCounterMagic () != null) && (unitCanCast.isImmuneToCounterMagic ()))
				immuneToCounterMagic = true;
		}
		
		// See if node aura or Counter Magic blocks it
		final boolean passesCounteringAttempts;
		if (immuneToCounterMagic)
			passesCounteringAttempts = true;
		else
		{
			final KnownWizardDetails castingWizard = getKnownWizardUtils ().findKnownWizardDetails (mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (),
				castingPlayer.getPlayerDescription ().getPlayerID (), "castCombatNow");
			
			final int unmodifiedCombatCastingCost = getSpellUtils ().getUnmodifiedCombatCastingCost (spell, variableDamage, castingWizard.getPick ());
			passesCounteringAttempts = getSpellDispelling ().processCountering
				(castingPlayer, spell, unmodifiedCombatCastingCost, combatLocation, defendingPlayer, attackingPlayer, null, null, mom);
		}
		
		final MomPersistentPlayerPrivateKnowledge castingPlayerPriv = (MomPersistentPlayerPrivateKnowledge) castingPlayer.getPersistentPlayerPrivateKnowledge ();
		if (passesCounteringAttempts)
		{
			final KindOfSpell kind = getKindOfSpellUtils ().determineKindOfSpell (spell, null);
			
			// Combat enchantments
			if (spell.getSpellBookSectionID () == SpellBookSectionID.COMBAT_ENCHANTMENTS)
			{
				// What effects doesn't the combat already have
				final List<String> combatAreaEffectIDs = getMemoryCombatAreaEffectUtils ().listCombatEffectsNotYetCastAtLocation
					(mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (),
					spell, castingPlayer.getPlayerDescription ().getPlayerID (), combatLocation);
	
				if ((combatAreaEffectIDs == null) || (combatAreaEffectIDs.size () == 0))
					throw new MomException ("castCombatNow was called for casting spell " + spell.getSpellID () + " in combat " + combatLocation +
						" but combatAreaEffectIDs list came back empty");
				
				final String combatAreaEffectID = combatAreaEffectIDs.get (getRandomUtils ().nextInt (combatAreaEffectIDs.size ()));
				log.debug ("castCombatNow chose CAE " + combatAreaEffectID + " as effect for spell " + spell.getSpellID ());
					
				getFogOfWarMidTurnChanges ().addCombatAreaEffectOnServerAndClients (mom.getGeneralServerKnowledge (), combatAreaEffectID,
					spell.getSpellID (), castingPlayer.getPlayerDescription ().getPlayerID (),
					(variableDamage != null) ? variableDamage : spell.getCombatCastingCost (),		// For putting extra MP into Counter Magic
					combatLocation, mom.getPlayers (), mom.getSessionDescription ());
			}
			
			// Unit enchantments or curses
			else if ((spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_ENCHANTMENTS) ||
				(spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_CURSES))
			{
				final boolean addUnitSpell;
				if (spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_ENCHANTMENTS)
					addUnitSpell = true;
				else if (spell.getCombatBaseDamage () == null)		// No saving throw is allowed, e.g. Web
					addUnitSpell = true;
				else
					// Using skipAnimation for skipDamageHeader for now, until I find a place where they need to be set differently
					addUnitSpell = getDamageProcessor ().makeResistanceRoll ((xuCombatCastingUnit == null) ? null : xuCombatCastingUnit.getMemoryUnit (),
						targetUnit, attackingPlayer, defendingPlayer, spell, variableDamage, false, castingPlayer, SpellCastType.COMBAT, skipAnimation, mom);
				
				if (addUnitSpell)
				{
					// What effects doesn't the unit already have - can cast Warp Creature multiple times
					final List<UnitSpellEffect> unitSpellEffects = getMemoryMaintainedSpellUtils ().listUnitSpellEffectsNotYetCastOnUnit
						(mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
						spell, castingPlayer.getPlayerDescription ().getPlayerID (), targetUnit.getUnitURN ());
					
					if ((unitSpellEffects == null) || (unitSpellEffects.size () == 0))
						throw new MomException ("castCombatNow was called for casting spell " + spell.getSpellID () + " on unit URN " + targetUnit.getUnitURN () +
							" but unitSpellEffectIDs list came back empty");
					
					// Pick an actual effect at random
					final UnitSpellEffect unitSpellEffect = unitSpellEffects.get (getRandomUtils ().nextInt (unitSpellEffects.size ()));
					final Integer useVariableDamage = ((unitSpellEffect.isStoreSkillValueAsVariableDamage () != null) && (unitSpellEffect.isStoreSkillValueAsVariableDamage ()) &&
						(unitSpellEffect.getUnitSkillValue () != null) && (unitSpellEffect.getUnitSkillValue () > 0)) ? unitSpellEffect.getUnitSkillValue () : variableDamage;
					
					getFogOfWarMidTurnChanges ().addMaintainedSpellOnServerAndClients (castingPlayer.getPlayerDescription ().getPlayerID (), spell.getSpellID (),
						targetUnit.getUnitURN (), unitSpellEffect.getUnitSkillID (), true, null, null, useVariableDamage, skipAnimation, true, mom);
					
					// In some cases adding a new spell may kill a unit, potentially ending the combat (webbing a unit that's flying over water)
					final MomCombatTile tile = combatDetails.getCombatMap ().getRow ().get (targetUnit.getCombatPosition ().getY ()).getCell ().get (targetUnit.getCombatPosition ().getX ());
					
					final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (targetUnit, null, null, null,
						mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
					
					if (getMovementUtils ().calculateDoubleMovementToEnterCombatTile (xu, tile, mom.getServerDB ()) < 0)
					{
						// Killing units but recording they took 0 damage will result in them coming back as undead
						getUnitServerUtils ().addDamage (targetUnit.getUnitDamage (), StoredDamageTypeID.HEALABLE, xu.calculateHitPointsRemaining ());
		
						mom.getWorldUpdates ().killUnit (targetUnit.getUnitURN (), KillUnitActionID.HEALABLE_COMBAT_DAMAGE);
						mom.getWorldUpdates ().process (mom);
						
						if (getDamageProcessor ().countUnitsInCombat (combatLocation,
							targetUnit.getCombatSide (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getServerDB ()) == 0)
							
							winningPlayer = castingPlayer;
					}
				}
				else if (!skipAnimation)
				{
					// Even though not adding a spell, still have to tell the client to show animation for the failed spell
					final ShowSpellAnimationMessage anim = new ShowSpellAnimationMessage ();
					anim.setSpellID (spell.getSpellID ());
					anim.setCastInCombat (true);
					anim.setCombatTargetUnitURN (targetUnit.getUnitURN ());
					anim.setCastingPlayerID (castingPlayer.getPlayerDescription ().getPlayerID ());

					if (attackingPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
						attackingPlayer.getConnection ().sendMessageToClient (anim);

					if (defendingPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
						defendingPlayer.getConnection ().sendMessageToClient (anim);
				}
			}
			
			else if ((spell.getSpellBookSectionID () == SpellBookSectionID.CITY_ENCHANTMENTS) || (spell.getSpellBookSectionID () == SpellBookSectionID.CITY_CURSES))
			{
				// What effects doesn't the city already have
				final List<String> citySpellEffectIDs = getMemoryMaintainedSpellUtils ().listCitySpellEffectsNotYetCastAtLocation
					(mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
					spell, castingPlayer.getPlayerDescription ().getPlayerID (), combatLocation);
				
				if ((citySpellEffectIDs == null) || (citySpellEffectIDs.size () == 0))
					throw new MomException ("castCombatNow was called for casting spell " + spell.getSpellID () + " at location " + combatLocation +
						" but citySpellEffectIDs list came back empty");
				
				// Pick an actual effect at random
				final String citySpellEffectID = citySpellEffectIDs.get (getRandomUtils ().nextInt (citySpellEffectIDs.size ()));
				getFogOfWarMidTurnChanges ().addMaintainedSpellOnServerAndClients (castingPlayer.getPlayerDescription ().getPlayerID (), spell.getSpellID (), null, null,
					true, combatLocation, citySpellEffectID, variableDamage, false, true, mom);
				
				// The new enchantment presumably requires the combat map to be regenerated so we can see it
				// (the only city enchantments/curses that can be cast in combat are Wall of Fire / Wall of Darkness)
				getCombatMapGenerator ().regenerateCombatTileBorders (combatDetails.getCombatMap (),
					mom.getServerDB (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getSessionDescription ().getOverlandMapSize (), combatLocation);
				
				// Send the updated map
				final UpdateCombatMapMessage msg = new UpdateCombatMapMessage ();
				msg.setCombatLocation (combatLocation);
				msg.setCombatTerrain (combatDetails.getCombatMap ());
				
				if (attackingPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
					attackingPlayer.getConnection ().sendMessageToClient (msg);
	
				if (defendingPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
					defendingPlayer.getConnection ().sendMessageToClient (msg);
			}
			
			// Spells aimed at a location
			// Include here Cracks Call when it is aimed ONLY at a wall segment.  If its aimed at a unit that also happens to be standing
			// adjacent to a wall segment, that is resolved differently in the ATTACK_SPELLS section below.
			else if ((kind == KindOfSpell.EARTH_TO_MUD) || (kind == KindOfSpell.ATTACK_WALLS) ||
				((kind == KindOfSpell.ATTACK_UNITS_AND_WALLS) && (targetLocation != null) && (targetUnit == null)))
			{
				if ((kind == KindOfSpell.ATTACK_WALLS) || (kind == KindOfSpell.ATTACK_UNITS_AND_WALLS))
				{
					// Wreck one tile
					combatDetails.getCombatMap ().getRow ().get (targetLocation.getY ()).getCell ().get (targetLocation.getX ()).setWrecked (true);
				}
				else
				{
					// Make an area muddy, the size of the area is set in the radius field
					final CombatMapArea areaBridge = new CombatMapArea ();
					areaBridge.setArea (combatDetails.getCombatMap ());
					areaBridge.setCoordinateSystem (mom.getSessionDescription ().getCombatMapSize ());
					
					final Set<String> muddableTiles = mom.getServerDB ().getCombatTileType ().stream ().filter
						(tt -> (tt.isLand () != null) && (tt.isLand ())).map (tt -> tt.getCombatTileTypeID ()).collect (Collectors.toSet ());
					
					getCombatMapOperations ().processCellsWithinRadius (areaBridge, targetLocation.getX (), targetLocation.getY (),
						spell.getSpellRadius (), (tile) ->
					{
						if (muddableTiles.contains (getCombatMapUtils ().getCombatTileTypeForLayer (tile, CombatMapLayerID.TERRAIN)))
							tile.setMud (true);
						
						return true;
					});
				}
				
				// Show animation for it
				final ShowSpellAnimationMessage anim = new ShowSpellAnimationMessage ();
				anim.setSpellID (spell.getSpellID ());
				anim.setCastInCombat (true);
				anim.setCombatTargetLocation (targetLocation);
				anim.setCastingPlayerID (castingPlayer.getPlayerDescription ().getPlayerID ());
	
				if (attackingPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
					attackingPlayer.getConnection ().sendMessageToClient (anim);
	
				if (defendingPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
					defendingPlayer.getConnection ().sendMessageToClient (anim);
				
				// Send the updated map
				final UpdateCombatMapMessage msg = new UpdateCombatMapMessage ();
				msg.setCombatLocation (combatLocation);
				msg.setCombatTerrain (combatDetails.getCombatMap ());
				
				if (attackingPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
					attackingPlayer.getConnection ().sendMessageToClient (msg);
	
				if (defendingPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
					defendingPlayer.getConnection ().sendMessageToClient (msg);
			}
			
			// Raise dead
			else if (kind == KindOfSpell.RAISE_DEAD)
			{
				// Even though we're summoning the unit into a combat, the location of the unit might not be
				// the same location as the combat - if its the attacker summoning a unit, it needs to go in the
				// cell they're attacking from, not the actual defending/combat cell
				final MapCoordinates3DEx summonLocation = getOverlandMapServerUtils ().findMapLocationOfUnitsInCombat
					(combatLocation, castingSide, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());
				
				// Heal it
				targetUnit.getUnitDamage ().clear ();
				targetUnit.setOwningPlayerID (castingPlayer.getPlayerDescription ().getPlayerID ());
				
				if (spell.getResurrectedHealthPercentage () < 100)
				{
					final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (targetUnit, null, null, null,
						mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
					
					final int totalHP = xu.calculateHitPointsRemaining ();
					final int hp = (totalHP * spell.getResurrectedHealthPercentage ()) / 100;
					final int dmg = totalHP - hp;
					if (dmg > 0)
					{
						final UnitDamage dmgTaken = new UnitDamage ();
						dmgTaken.setDamageTaken (dmg);
						dmgTaken.setDamageType (StoredDamageTypeID.HEALABLE);
						targetUnit.getUnitDamage ().add (dmgTaken);
					}
				}
				
				// Does it become undead?
				if (spell.getResurrectingAddsSkillID () != null)
				{
					final UnitSkillAndValue undead = new UnitSkillAndValue ();
					undead.setUnitSkillID (spell.getResurrectingAddsSkillID ());
					targetUnit.getUnitHasSkill ().add (undead);
				}
				
				// Set it back to alive; this also sends the updates from above
				getFogOfWarMidTurnChanges ().updateUnitStatusToAliveOnServerAndClients (targetUnit, summonLocation, castingPlayer, true, false, mom);
	
				// Show the "summoning" animation for it
				final ExpandedUnitDetails xuTargetUnit = getExpandUnitDetails ().expandUnitDetails (targetUnit, null, null, null,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
						
				final int combatHeading = (castingPlayer == attackingPlayer) ? 8 : 4;
				
				final MapCoordinates2DEx actualTargetLocation = getUnitServerUtils ().findFreeCombatPositionAvoidingInvisibleClosestTo
					(xuTargetUnit, combatLocation, combatDetails.getCombatMap (), targetLocation, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (),
						mom.getSessionDescription ().getCombatMapSize (), mom.getServerDB ());
	
				getCombatProcessing ().setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer, targetUnit,
					combatLocation, combatLocation, actualTargetLocation, combatHeading, castingSide, spell.getSpellID (), mom);
	
				// Allow it to be moved this combat turn
				targetUnit.setDoubleCombatMovesLeft (2 * xuTargetUnit.getMovementSpeed ());
			}
			
			// Combat summons
			else if (kind == KindOfSpell.SUMMONING)
			{
				// Pick an actual unit at random
				if (spell.getSummonedUnit ().size () > 0)
				{
					final String unitID = spell.getSummonedUnit ().get (getRandomUtils ().nextInt (spell.getSummonedUnit ().size ()));
					log.debug ("castCombatNow chose Unit ID " + unitID + " as unit to summon from spell " + spell.getSpellID ());
					
					// Even though we're summoning the unit into a combat, the location of the unit might not be
					// the same location as the combat - if its the attacker summoning a unit, it needs to go in the
					// cell they're attacking from, not the actual defending/combat cell
					final MapCoordinates3DEx summonLocation = getOverlandMapServerUtils ().findMapLocationOfUnitsInCombat
						(combatLocation, castingSide, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ());
					
					// Now can add it
					final MemoryUnit tu = getFogOfWarMidTurnChanges ().addUnitOnServerAndClients (unitID, summonLocation, null, null, combatLocation,
						castingPlayer, UnitStatusID.ALIVE, true, false, mom);
					
					// What direction should the unit face?
					final int combatHeading = (castingPlayer == attackingPlayer) ? 8 : 4;
					
					// Set it immediately into combat
					final ExpandedUnitDetails xuSummonedUnit = getExpandUnitDetails ().expandUnitDetails (tu, null, null, null,
						mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
					
					final MapCoordinates2DEx actualTargetLocation = getUnitServerUtils ().findFreeCombatPositionAvoidingInvisibleClosestTo
						(xuSummonedUnit, combatLocation, combatDetails.getCombatMap (), targetLocation, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (),
							mom.getSessionDescription ().getCombatMapSize (), mom.getServerDB ());
					
					getCombatProcessing ().setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer, tu,
						combatLocation, combatLocation, actualTargetLocation, combatHeading, castingSide, spell.getSpellID (), mom);
					
					// Allow it to be moved this combat turn
					tu.setDoubleCombatMovesLeft (2 * xuSummonedUnit.getMovementSpeed ());
					
					// Make sure we remove it after combat
					tu.setWasSummonedInCombat (true);
				}
			}
			
			// Call Chaos is so bizarre it needs its own section, because we need to pick the effect each unit will get BEFORE we call isUnitValidTargetForSpell
			// because some units may be immune to disintegrate (high resistance) or healing (undead) or chaos channels (already is CC) for different reasons
			else if (spell.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_CALL_CHAOS))
			{
				final List<ResolveAttackTarget> targetUnits = new ArrayList<ResolveAttackTarget> ();
				
				for (final MemoryUnit thisUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
					if ((combatLocation.equals (thisUnit.getCombatLocation ())) && (thisUnit.getCombatPosition () != null) &&
						(thisUnit.getCombatHeading () != null) && (thisUnit.getCombatSide () != null) && (thisUnit.getStatus () == UnitStatusID.ALIVE) &&
						(thisUnit.getOwningPlayerID () != castingPlayer.getPlayerDescription ().getPlayerID ()))
					{
						final String randomSpellID = CommonDatabaseConstants.CALL_CHAOS_CHOICES.get
							(getRandomUtils ().nextInt (CommonDatabaseConstants.CALL_CHAOS_CHOICES.size ()));
						if (randomSpellID != null)
						{
							final Spell randomSpell = mom.getServerDB ().findSpell (randomSpellID, "castCombatNow (CC)");
							final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (thisUnit, null, null, randomSpell.getSpellRealm (),
								mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
							
							// For healing we have to fudge the playerID or its going to stop us from healing an enemy unit; chaos channels is also a "friendly" enchantment
							final int thisCastingPlayerID = ((randomSpellID.equals (CommonDatabaseConstants.SPELL_ID_HEALING)) ||
								(randomSpellID.equals (CommonDatabaseConstants.SPELL_ID_CHAOS_CHANNELS))) ? thisUnit.getOwningPlayerID () : castingPlayer.getPlayerDescription ().getPlayerID ();
							
							if (getSpellTargetingUtils ().isUnitValidTargetForSpell (randomSpell, null, combatLocation, combatDetails.getCombatMap (), thisCastingPlayerID,
								xuCombatCastingUnit, variableDamage, xu, false, mom.getGeneralServerKnowledge ().getTrueMap (), castingPlayerPriv.getFogOfWar (),
								mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET)
							{
								final ResolveAttackTarget thisTarget = new ResolveAttackTarget (thisUnit);
								thisTarget.setSpellOverride (randomSpell);
								targetUnits.add (thisTarget);
							}
						}
					}
				
				if (targetUnits.size () > 0)
					combatEnded = getDamageProcessor ().resolveAttack ((xuCombatCastingUnit == null) ? null : xuCombatCastingUnit.getMemoryUnit (),
						targetUnits, attackingPlayer, defendingPlayer,
						null, null, null, null, null, spell, variableDamage, castingPlayer, combatLocation, skipAnimation, mom).isCombatEnded ();
			}
			
			// Attack, healing or dispelling spells
			else if ((spell.getSpellBookSectionID () == SpellBookSectionID.ATTACK_SPELLS) || (spell.getSpellBookSectionID () == SpellBookSectionID.SPECIAL_UNIT_SPELLS) ||
				(spell.getSpellBookSectionID () == SpellBookSectionID.DISPEL_SPELLS))
			{
				// Does the spell attack a specific unit or ALL enemy units? e.g. Flame Strike or Death Spell
				final List<MemoryUnit> targetUnits = new ArrayList<MemoryUnit> ();
				final List<MemoryMaintainedSpell> targetSpells = new ArrayList<MemoryMaintainedSpell> ();
				final List<MemoryCombatAreaEffect> targetCAEs = new ArrayList<MemoryCombatAreaEffect> ();
				if (spell.getAttackSpellCombatTarget () == AttackSpellTargetID.SINGLE_UNIT)
					targetUnits.add (targetUnit);
				else
				{
					for (final MemoryUnit thisUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
						if ((combatLocation.equals (thisUnit.getCombatLocation ())) && (thisUnit.getCombatPosition () != null) &&
							(thisUnit.getCombatHeading () != null) && (thisUnit.getCombatSide () != null) && (thisUnit.getStatus () == UnitStatusID.ALIVE))
						{
							final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (thisUnit, null, null, spell.getSpellRealm (),
								mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
							
							if (getSpellTargetingUtils ().isUnitValidTargetForSpell (spell, null, combatLocation, combatDetails.getCombatMap (),
								castingPlayer.getPlayerDescription ().getPlayerID (),
								xuCombatCastingUnit, variableDamage, xu, false, mom.getGeneralServerKnowledge ().getTrueMap (), castingPlayerPriv.getFogOfWar (),
								mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET)
								
								targetUnits.add (thisUnit);
						}
					
					// Disenchant Area / True will target spells cast on the combat as well
					if (spell.getSpellBookSectionID () == SpellBookSectionID.DISPEL_SPELLS)
					{
						targetSpells.addAll (mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ().stream ().filter
							(s -> (s.getCastingPlayerID () != castingPlayer.getPlayerDescription ().getPlayerID ()) &&
								(combatLocation.equals (s.getCityLocation ()))).collect (Collectors.toList ()));
						
						// Also CAEs which have no underlying maintained spell, like Prayer (method gets all CAEs in combat, so just need to restrict to ones cast by the opponent)
						targetCAEs.addAll (getMemoryCombatAreaEffectUtils ().listCombatAreaEffectsFromLocalisedSpells
							(mom.getGeneralServerKnowledge ().getTrueMap (), combatLocation, mom.getServerDB ()).stream ().filter
								(cae -> !cae.getCastingPlayerID ().equals (castingPlayer.getPlayerDescription ().getPlayerID ())).collect (Collectors.toList ()));
					}
				}
				
				if ((targetUnits.size () > 0) || (targetSpells.size () > 0) || (targetCAEs.size () > 0))
				{
					if (spell.getSpellBookSectionID () == SpellBookSectionID.ATTACK_SPELLS)
					{
						// If its Cracks Call, is there a wall segment to attack in addition to the unit we're attacking?
						Integer wreckTileChance = null;
						MapCoordinates2DEx wreckTilePosition = null;
						if ((spell.getSpellValidBorderTarget ().size () > 0) && (getSpellTargetingUtils ().isCombatLocationValidTargetForSpell
							(spell, (MapCoordinates2DEx) targetUnit.getCombatPosition (), combatDetails.getCombatMap (), mom.getServerDB ())))
						{
							wreckTileChance = 1;
							wreckTilePosition = (MapCoordinates2DEx) targetUnit.getCombatPosition ();
						}
						
						final List<ResolveAttackTarget> targetUnitWrappers = targetUnits.stream ().map (t -> new ResolveAttackTarget (t)).collect (Collectors.toList ());
						
						combatEnded = getDamageProcessor ().resolveAttack ((xuCombatCastingUnit == null) ? null : xuCombatCastingUnit.getMemoryUnit (),
							targetUnitWrappers, attackingPlayer, defendingPlayer, null,
							wreckTileChance, wreckTilePosition, null, null, spell, variableDamage, castingPlayer, combatLocation, skipAnimation, mom).isCombatEnded ();
					}
					else if (kind == KindOfSpell.HEALING)
					{
					    healUnits(spell, defendingPlayer, attackingPlayer, skipAnimation, mom, targetUnits);
					}
					else if (kind == KindOfSpell.RECALL)
					{
						// Recall spells - first we need the location of the wizards' summoning circle 'building' to know where we're recalling them to
						final MemoryBuilding summoningCircleLocation = getMemoryBuildingUtils ().findCityWithBuilding (castingPlayer.getPlayerDescription ().getPlayerID (),
							CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE, mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
							mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ());
						
						if (summoningCircleLocation != null)
						{
							// Maybe summoning circle location is full (making assumption here that all recall spells target a single unit only)
							final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (targetUnit, null, null, null,
								mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());

							final UnitAddLocation recallLocation = getUnitServerUtils ().findNearestLocationWhereUnitCanBeMoved
								((MapCoordinates3DEx) summoningCircleLocation.getCityLocation (), xu, mom);
							if (recallLocation.getUnitLocation () != null)
							{							
								// Recall spells - first take the unit(s) out of combat
								for (final MemoryUnit tu : targetUnits)
								{
									getCombatProcessing ().setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer, tu,
										combatLocation, null, null, null, null, spell.getSpellID (), mom);

									// Its possible this could kill the unit, if it has extra HP from Lionheart cast in combat that we now lose
									getFogOfWarMidTurnMultiChanges ().switchOffSpellsCastInCombatOnUnit (tu.getUnitURN (), mom);
								}
								
								mom.getWorldUpdates ().process (mom);
								
								// If the unit is still alive, teleport it back to our summoning circle
								if ((getUnitUtils ().findUnitURN (targetUnit.getUnitURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ()) != null) &&
									(!recallLocation.getUnitLocation ().equals (targetUnit.getUnitLocation ())))
									
									getFogOfWarMidTurnMultiChanges ().moveUnitStackOneCellOnServerAndClients (targetUnits, castingPlayer, combatLocation,
										recallLocation.getUnitLocation (), mom);
								
								// If we recalled our last remaining unit(s) out of combat, then we lose
								if (getDamageProcessor ().countUnitsInCombat (combatLocation, castingSide,
									mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getServerDB ()) == 0)
									
									winningPlayer = (castingPlayer == defendingPlayer) ? attackingPlayer : defendingPlayer;
							}
						}
					}
					else
					{
						// Dispel magic or similar - before we start rolling, send animation for it
						final ShowSpellAnimationMessage anim = new ShowSpellAnimationMessage ();
						anim.setSpellID (spell.getSpellID ());
						anim.setCastInCombat (true);
						anim.setCombatTargetLocation (targetLocation);
						anim.setCastingPlayerID (castingPlayer.getPlayerDescription ().getPlayerID ());
						
						if ((spell.getAttackSpellCombatTarget () == AttackSpellTargetID.SINGLE_UNIT) && (targetUnits.size () > 0))
							anim.setCombatTargetUnitURN (targetUnits.get (0).getUnitURN ());
	
						if (attackingPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
							attackingPlayer.getConnection ().sendMessageToClient (anim);
	
						if (defendingPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
							defendingPlayer.getConnection ().sendMessageToClient (anim);
						
						// Now get a list of all enchantments cast on all the units in the list by other wizards
		    			final List<String> spellsImmuneToDispelling = mom.getServerDB ().getSpell ().stream ().filter
	        				(s -> (s.isImmuneToDispelling () != null) && (s.isImmuneToDispelling ())).map (s -> s.getSpellID ()).collect (Collectors.toList ());
						
						final List<Integer> targetUnitURNs = targetUnits.stream ().map (u -> u.getUnitURN ()).collect (Collectors.toList ());
						final List<MemoryMaintainedSpell> spellsToDispel = mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ().stream ().filter
							(s -> (s.getCastingPlayerID () != castingPlayer.getPlayerDescription ().getPlayerID ()) && (targetUnitURNs.contains (s.getUnitURN ())) &&
								(!spellsImmuneToDispelling.contains (s.getSpellID ()))).collect (Collectors.toList ());
						spellsToDispel.addAll (targetSpells);
						
						// Is the combat taking place at a warped node?
			    		final boolean targetWarpedNode = (gc.getTerrainData ().isWarped () != null) && (gc.getTerrainData ().isWarped ()) &&
			    			(mom.getServerDB ().findTileType (gc.getTerrainData ().getTileTypeID (), "castCombatNow").getMagicRealmID () != null);
			    		
			    		// Are any of the units targeted directly as well as targeting the spells cast on them?
			    		final List<MemoryUnit> targetVortexes = targetUnits.stream ().filter
			    			(u -> mom.getServerDB ().getUnitsThatMoveThroughOtherUnits ().contains (u.getUnitID ())).collect (Collectors.toList ());
						
						// Common method does the rest
						if (getSpellDispelling ().processDispelling (spell, variableDamage, castingPlayer, spellsToDispel, targetCAEs,
							targetWarpedNode ? combatLocation : null, targetVortexes, 0, mom))
							
							// Its possible we dispelled Lionheart on the last enemy unit thereby winning the combat, so check to be sure
							if (getDamageProcessor ().countUnitsInCombat (combatLocation,
								(castingSide == UnitCombatSideID.ATTACKER) ? UnitCombatSideID.DEFENDER : UnitCombatSideID.ATTACKER,
								mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), mom.getServerDB ()) == 0)
									winningPlayer = castingPlayer;
	
					}
				}
			}
			
			else
				throw new MomException ("Cast a combat spell with a section ID that there is no code to deal with yet: " + spell.getSpellBookSectionID ());
		}
			
		// Who is casting the spell?
		if (xuCombatCastingUnit == null)
		{
			// Wizard casting - so charge them the mana cost
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) castingPlayer.getPersistentPlayerPrivateKnowledge ();
			getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, -multipliedManaCost);
			
			// Charge skill
			Integer sendSkillValue = null;
			if (castingPlayer == defendingPlayer)
			{
				combatDetails.setDefenderCastingSkillRemaining (combatDetails.getDefenderCastingSkillRemaining () - reducedCombatCastingCost);
				sendSkillValue = combatDetails.getDefenderCastingSkillRemaining ();
			}
			else if (castingPlayer == attackingPlayer)
			{
				combatDetails.setAttackerCastingSkillRemaining (combatDetails.getAttackerCastingSkillRemaining () - reducedCombatCastingCost);
				sendSkillValue = combatDetails.getAttackerCastingSkillRemaining ();
			}
			else
				throw new MomException ("Trying to charge combat casting cost to kill but the caster appears to be neither attacker nor defender");
			
			// Send both values to client
			if (sendSkillValue != null)
				getServerResourceCalculations ().sendGlobalProductionValues (castingPlayer, sendSkillValue, true);
			
			// Only allow casting one spell each combat turn
			combatDetails.setSpellCastThisCombatTurn (true);
		}
		else if (combatCastingFixedSpellNumber != null)
		{
			// Casting a fixed spell that's part of the unit definition
			xuCombatCastingUnit.setDoubleCombatMovesLeft (0);

			xuCombatCastingUnit.getMemoryUnit ().getFixedSpellsRemaining ().set (combatCastingFixedSpellNumber,
				xuCombatCastingUnit.getMemoryUnit ().getFixedSpellsRemaining ().get (combatCastingFixedSpellNumber) - 1);

			getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (xuCombatCastingUnit.getMemoryUnit (), mom, null);
		}
		else if (combatCastingSlotNumber != null)
		{
			// Casting a spell imbued into a hero item
			xuCombatCastingUnit.setDoubleCombatMovesLeft (0);

			xuCombatCastingUnit.getMemoryUnit ().getHeroItemSpellChargesRemaining ().set (combatCastingSlotNumber,
				xuCombatCastingUnit.getMemoryUnit ().getHeroItemSpellChargesRemaining ().get (combatCastingSlotNumber) - 1);

			getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (xuCombatCastingUnit.getMemoryUnit (), mom, null);
		}
		else
		{
			// Unit or hero casting - so charge them the mana cost and zero their movement
			xuCombatCastingUnit.setManaRemaining (xuCombatCastingUnit.getManaRemaining () - multipliedManaCost);
			xuCombatCastingUnit.setDoubleCombatMovesLeft (0);
			
			getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (xuCombatCastingUnit.getMemoryUnit (), mom, null);
		}
		
		// Did casting the spell result in winning/losing the combat?
		if (winningPlayer != null)
		{
			getCombatStartAndEnd ().combatEnded (combatDetails, attackingPlayer, defendingPlayer, winningPlayer, null, mom, true);
			combatEnded = true;
		}

		return combatEnded;
	}

    private void healUnits(final Spell spell, final PlayerServerDetails defendingPlayer,
            final PlayerServerDetails attackingPlayer, final boolean skipAnimation, final MomSessionVariables mom,
            final List<MemoryUnit> targetUnits)
            throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException {
        // Healing spells work by sending ApplyDamage - this is basically just updating the client as to the damage taken by a bunch of combat units,
        // and handles showing the animation for us, so its convenient to reuse it for this.  Effectively we're just applying negative damage...
        final List<ResolveAttackTarget> unitWrappers = new ArrayList<ResolveAttackTarget> ();
        for (final MemoryUnit tu : targetUnits)
        {
        	final int dmg = getUnitUtils ().getTotalDamageTaken(tu.getUnitDamage ());
        	final int heal = Math.min (dmg, spell.getCombatBaseDamage ());
        	if (heal > 0)
        	{
        		getUnitServerUtils ().healDamage (tu.getUnitDamage (), heal, false);
        		unitWrappers.add (new ResolveAttackTarget (tu));
        	}
        }
        
        getFogOfWarMidTurnChanges ().sendDamageToClients (null, attackingPlayer, defendingPlayer,
        	unitWrappers, null, spell.getSpellID (), null, null, skipAnimation, mom);
    }
	
	/**
	 * Overland spells are cast first (probably taking several turns) and a target is only chosen after casting is completed.
	 * So this actually processes the actions from the spell once its target is chosen.
	 * This assumes all necessary validation has been done to verify that the action is allowed.
	 * 
	 * @param spell Definition of spell being targeted
	 * @param maintainedSpell Spell being targeted in server's true memory - at the time this is called, this is the only copy of the spell that exists
	 * 	as we can only determine which clients can "see" it once a target location has been chosen.  Even the player who cast it doesn't have a
	 *		record of it, just a special entry on their new turn messages scroll telling them to pick a target for it.
	 * @param targetPlayerID If the spell is targeted at a wizard, then which one
	 * @param targetLocation If the spell is targeted at a city or a map location, then sets that location; null for spells targeted on other things
	 * @param targetUnit If the spell is targeted at a unit, then the true unit to aim at; null for spells targeted on other things
	 * @param targetSpell If the spell is targeted at another spell, then the true spell to aim at; null for spells targeted on other things
	 * @param citySpellEffectID If spell creates a city spell effect, then which one - currently chosen at random, but supposed to be player choosable for Spell Ward
	 * @param unitSkillID If spell creates a unit skill, then which one - chosen at random for Chaos Channels
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void targetOverlandSpell (final Spell spell, final MemoryMaintainedSpell maintainedSpell, final Integer targetPlayerID,
		final MapCoordinates3DEx targetLocation, final MemoryUnit targetUnit, final MemoryMaintainedSpell targetSpell,
		final String citySpellEffectID, final String unitSkillID, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		final PlayerServerDetails castingPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), maintainedSpell.getCastingPlayerID (), "targetOverlandSpell");
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) castingPlayer.getPersistentPlayerPrivateKnowledge ();
		final KindOfSpell kind = getKindOfSpellUtils ().determineKindOfSpell (spell, null);
		
		if ((spell.getSpellBookSectionID () == SpellBookSectionID.SPECIAL_UNIT_SPELLS) || (spell.getSpellBookSectionID () == SpellBookSectionID.SPECIAL_OVERLAND_SPELLS) ||
			(spell.getSpellBookSectionID () == SpellBookSectionID.DISPEL_SPELLS) || (spell.getSpellBookSectionID () == SpellBookSectionID.SUMMONING) ||
			(spell.getSpellBookSectionID () == SpellBookSectionID.ENEMY_WIZARD_SPELLS) || (kind == KindOfSpell.CHANGE_UNIT_ID) ||
			(kind == KindOfSpell.ATTACK_UNITS))
		{
			// Transient spell that performs some immediate action, then the temporary untargeted spell on the server gets removed
			// So the spell never does get added to any clients
			// Set values on server - it'll be removed below, but we need to set these to make the visibility checks in sendTransientSpellToClients () work correctly
			
			if (targetUnit != null)
				maintainedSpell.setUnitURN (targetUnit.getUnitURN ());
			
			maintainedSpell.setCityLocation (targetLocation);
			maintainedSpell.setUnitSkillID (unitSkillID);
			maintainedSpell.setCitySpellEffectID (citySpellEffectID);
			maintainedSpell.setTargetPlayerID (targetPlayerID);
			
			// Just remove it - don't even bother to check if any clients can see it
			getMemoryMaintainedSpellUtils ().removeSpellURN (maintainedSpell.getSpellURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ());
			
			// If resurrecting a dead hero, we need to figure out where its going to appear and move its location and set it back to alive first
			// otherwise its still at the location where it died, which makes sendTransientSpellToClients have a hard time knowing who should
			// see the spell effect on the map

			if (kind == KindOfSpell.RAISE_DEAD)
			{
				final MemoryBuilding summoningCircleLocation = getMemoryBuildingUtils ().findCityWithBuilding (maintainedSpell.getCastingPlayerID (),
					CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE, mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ());

				if (summoningCircleLocation != null)
				{
					final UnitAddLocation resurrectLocation = getUnitServerUtils ().findNearestLocationWhereUnitCanBeAdded
						((MapCoordinates3DEx) summoningCircleLocation.getCityLocation (), targetUnit.getUnitID (), maintainedSpell.getCastingPlayerID (), mom);
					
					if (resurrectLocation.getUnitLocation () != null)
					{
						targetUnit.getUnitDamage ().clear ();
						
						getFogOfWarMidTurnChanges ().updateUnitStatusToAliveOnServerAndClients (targetUnit, resurrectLocation.getUnitLocation (),
							castingPlayer, true, true, mom);
					}
				}
			}
			
			// Conditions under which the targeted wizard needs to learn the identity of the wizard casting something at them
			if (targetPlayerID != null)		// For Spell Blast or other enemy wizard spells like Drain Power
				getKnownWizardServerUtils ().meetWizard (maintainedSpell.getCastingPlayerID (), targetPlayerID, false, mom);
			
			else if (targetUnit != null)
				getKnownWizardServerUtils ().meetWizard (maintainedSpell.getCastingPlayerID (), targetUnit.getOwningPlayerID (), false, mom);

			else if (targetSpell != null)
				getKnownWizardServerUtils ().meetWizard (maintainedSpell.getCastingPlayerID (), targetSpell.getCastingPlayerID (), false, mom);
			
			else if (targetLocation != null)
			{
				final OverlandMapCityData cityData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
					(targetLocation.getZ ()).getRow ().get (targetLocation.getY ()).getCell ().get (targetLocation.getX ()).getCityData ();
				if (cityData != null)
					getKnownWizardServerUtils ().meetWizard (maintainedSpell.getCastingPlayerID (), cityData.getCityOwnerID (), false, mom);
				
				for (final MemoryUnit tu : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
					if ((tu.getStatus () == UnitStatusID.ALIVE) && (targetLocation.equals (tu.getUnitLocation ())))
						getKnownWizardServerUtils ().meetWizard (maintainedSpell.getCastingPlayerID (), tu.getOwningPlayerID (), false, mom);
			}
			
			// Tell the client to stop asking about targeting the spell, and show an animation for it - need to send this to all players that can see it!
			getFogOfWarMidTurnChanges ().sendTransientSpellToClients (maintainedSpell, mom);

			if (kind == KindOfSpell.RECALL)
			{
				// Recall spells - first we need the location of the wizards' summoning circle 'building' to know where we're recalling them to
				final MemoryBuilding summoningCircleLocation = getMemoryBuildingUtils ().findCityWithBuilding (castingPlayer.getPlayerDescription ().getPlayerID (),
					CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE, mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
					mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ());
				
				if (summoningCircleLocation != null)
				{
					// Maybe summoning circle location is full
					final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (targetUnit, null, null, null,
						mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
					
					final UnitAddLocation recallLocation = getUnitServerUtils ().findNearestLocationWhereUnitCanBeMoved
						((MapCoordinates3DEx) summoningCircleLocation.getCityLocation (), xu, mom);
					if ((recallLocation.getUnitLocation () != null) && (!recallLocation.getUnitLocation ().equals (targetUnit.getUnitLocation ())))
					{
						final List<MemoryUnit> targetUnits = new ArrayList<MemoryUnit> ();
						targetUnits.add (targetUnit);
						
						final MapCoordinates3DEx oldLocation = new MapCoordinates3DEx ((MapCoordinates3DEx) targetUnit.getUnitLocation ());
						getFogOfWarMidTurnMultiChanges ().moveUnitStackOneCellOnServerAndClients (targetUnits, castingPlayer, (MapCoordinates3DEx) targetUnit.getUnitLocation (),
							recallLocation.getUnitLocation (), mom);
						
						// Recheck the cell the unit was moved from - maybe it was a transport
						mom.getWorldUpdates ().recheckTransportCapacity (oldLocation);
						mom.getWorldUpdates ().process (mom);
					}
				}
			}
			
			else if (kind == KindOfSpell.HEALING)
			{
			    final String spellId = maintainedSpell.getSpellID();
			    log.info("spellId = " + spellId);
			    if ("SP125".equals(spellId)) {
			        healUnits(spell, castingPlayer, castingPlayer, false, mom, List.of(targetUnit));
			    } else {
    				// Nature's Cures - heal every unit at the location
    				for (final MemoryUnit tu : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ()) {
    					if ((targetLocation.equals (tu.getUnitLocation ())) && (tu.getStatus () == UnitStatusID.ALIVE) &&
    						(tu.getOwningPlayerID () == maintainedSpell.getCastingPlayerID ()))
    					{
    						final ExpandedUnitDetails thisTarget = getExpandUnitDetails ().expandUnitDetails (tu, null, null, null,
    							mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
    						
    						if (getSpellTargetingUtils ().isUnitValidTargetForSpell (spell, null, null, null,
    							maintainedSpell.getCastingPlayerID (), null, null, thisTarget, false, mom.getGeneralServerKnowledge ().getTrueMap (),
    							priv.getFogOfWar (), mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET)
    						{
    							// Heal this unit
    							final int dmg = getUnitUtils ().getHealableDamageTaken (tu.getUnitDamage ());
    							getUnitServerUtils ().healDamage (tu.getUnitDamage (), dmg, false);
    							
    							getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (tu, mom, null);
    						}
    					}
    				}
			    }
			}
			
			else if (spell.getSpellBookSectionID () == SpellBookSectionID.DISPEL_SPELLS)
			{
				final List<MemoryMaintainedSpell> targetSpells;
				boolean targetWarpedNode = false;
				
				if ((kind == KindOfSpell.DISPEL_OVERLAND_ENCHANTMENTS) || (kind == KindOfSpell.SPELL_BINDING))
				{
					// Disjunction is easy - just targeted at one single spell, and we've already got it
					targetSpells = Arrays.asList (targetSpell);
				}
				else
				{
					// Disenchant Area / True - get a list of units at the location (ours as well)
		    		final List<Integer> unitURNs = mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ().stream ().filter
		    			(u -> targetLocation.equals (u.getUnitLocation ())).map (u -> u.getUnitURN ()).collect (Collectors.toList ());
		    		
		    		// Now look for any spells cast by somebody else either targeted directly on the location, or on a unit at the location
	    			final List<String> spellsImmuneToDispelling = mom.getServerDB ().getSpell ().stream ().filter
           				(s -> (s.isImmuneToDispelling () != null) && (s.isImmuneToDispelling ())).map (s -> s.getSpellID ()).collect (Collectors.toList ());

		    		targetSpells = mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ().stream ().filter
		    			(s -> (s.getCastingPlayerID () != castingPlayer.getPlayerDescription ().getPlayerID ()) && (!spellsImmuneToDispelling.contains (s.getSpellID ())) &&
		    				((targetLocation.equals (s.getCityLocation ())) || (unitURNs.contains (s.getUnitURN ())))).collect (Collectors.toList ());
		    		
					final OverlandMapTerrainData terrainData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
						(targetLocation.getZ ()).getRow ().get (targetLocation.getY ()).getCell ().get (targetLocation.getX ()).getTerrainData ();
		    		
		    		targetWarpedNode = (terrainData.isWarped () != null) && (terrainData.isWarped ()) &&
		    			(mom.getServerDB ().findTileType (terrainData.getTileTypeID (), "targetOverlandSpell").getMagicRealmID () != null);
				}
				
	    		getSpellDispelling ().processDispelling (spell, maintainedSpell.getVariableDamage (), castingPlayer, targetSpells, null,
	    			targetWarpedNode ? targetLocation : null, null, 25, mom);
			}

			// The only targeted overland summoning spell is Floating Island
			else if (kind == KindOfSpell.SUMMONING)
				getSpellCasting ().castOverlandSummoningSpell (spell, castingPlayer, targetLocation, true, mom);
			
			else if (kind == KindOfSpell.CORRUPTION)
			{
				getSpellCasting ().corruptTile (targetLocation, mom);
				getCityProcessing ().penaltyToVisibleRelationFromNearbyCityOwner (targetLocation, maintainedSpell.getCastingPlayerID (), mom);
			}
			
			else if (kind == KindOfSpell.ENCHANT_ROAD)
			{
				// Enchant road - first just get a list of all coordinates we need to process
				final List<MapCoordinates3DEx> roadCells = new ArrayList<MapCoordinates3DEx> ();
				getCoordinateSystemUtils ().processCoordinatesWithinRadius (mom.getSessionDescription ().getOverlandMapSize (),
					targetLocation.getX (), targetLocation.getY (), spell.getSpellRadius (), (x, y, r, d, n) ->
				{
					roadCells.add (new MapCoordinates3DEx (x, y, targetLocation.getZ ()));
					return true;
				});
				
				// Now process them, and check which ones are actually road
				for (final MapCoordinates3DEx roadCoords : roadCells)
				{
					final OverlandMapTerrainData terrainData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
						(roadCoords.getZ ()).getRow ().get (roadCoords.getY ()).getCell ().get (roadCoords.getX ()).getTerrainData ();
					if ((terrainData.getRoadTileTypeID () != null) && (!terrainData.getRoadTileTypeID ().equals (spell.getTileTypeID ())))
					{
						terrainData.setRoadTileTypeID (spell.getTileTypeID ());
						
						getFogOfWarMidTurnChanges ().updatePlayerMemoryOfTerrain (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
							mom.getPlayers (), roadCoords, mom.getSessionDescription ().getFogOfWarSetting ().getTerrainAndNodeAuras ());
					}
				}
			}
			
			else if (kind == KindOfSpell.EARTH_LORE)
			{
				// Earth lore
				getFogOfWarProcessing ().canSeeRadius (priv.getFogOfWar (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
					mom.getSessionDescription ().getOverlandMapSize (), targetLocation.getX (), targetLocation.getY (),
					targetLocation.getZ (), spell.getSpellRadius ());
				
				getFogOfWarProcessing ().updateAndSendFogOfWar (castingPlayer, "earthLore", mom);
			}
			
			else if (kind == KindOfSpell.CHANGE_TILE_TYPE)
			{
				getSpellCasting ().changeTileType (spell, targetLocation, castingPlayer.getPlayerDescription ().getPlayerID (), mom);
				getCityProcessing ().penaltyToVisibleRelationFromNearbyCityOwner (targetLocation, maintainedSpell.getCastingPlayerID (), mom);
			}
			
			else if (kind == KindOfSpell.CHANGE_MAP_FEATURE)
			{
				// Transmute
				final OverlandMapTerrainData terrainData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
					(targetLocation.getZ ()).getRow ().get (targetLocation.getY ()).getCell ().get (targetLocation.getX ()).getTerrainData ();
				
				final Iterator<SpellValidMapFeatureTarget> iter = spell.getSpellValidMapFeatureTarget ().iterator ();				
				boolean found = false;
				while ((!found) && (iter.hasNext ()))
				{
					final SpellValidMapFeatureTarget thisMapFeature = iter.next ();
					if (thisMapFeature.getMapFeatureID ().equals (terrainData.getMapFeatureID ()))
					{
						if (thisMapFeature.getChangeToMapFeatureID () == null)
							throw new MomException ("Spell " + spell.getSpellID () + " is a change map feature spell but has no map feature defined to change from " + thisMapFeature.getMapFeatureID ());
						
						terrainData.setMapFeatureID (thisMapFeature.getChangeToMapFeatureID ());
						found = true;
					}
				}
				
				if (found)
				{
					getFogOfWarMidTurnChanges ().updatePlayerMemoryOfTerrain (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
						mom.getPlayers (), targetLocation, mom.getSessionDescription ().getFogOfWarSetting ().getTerrainAndNodeAuras ());
					getCityProcessing ().penaltyToVisibleRelationFromNearbyCityOwner (targetLocation, maintainedSpell.getCastingPlayerID (), mom);
				}
			}
			
			else if (kind == KindOfSpell.SPELL_BLAST)
			{
				final PlayerServerDetails targetPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), targetPlayerID, "targetOverlandSpell (T)");
				final MomPersistentPlayerPrivateKnowledge targetPriv = (MomPersistentPlayerPrivateKnowledge) targetPlayer.getPersistentPlayerPrivateKnowledge ();
				final MomTransientPlayerPrivateKnowledge targetTrans = (MomTransientPlayerPrivateKnowledge) targetPlayer.getTransientPlayerPrivateKnowledge ();
				
				if (targetPriv.getQueuedSpell ().size () > 0)
				{
					getKnownWizardServerUtils ().meetWizard (maintainedSpell.getCastingPlayerID (), targetPlayerID, false, mom);
					
					final int blastingCost = targetPriv.getManaSpentOnCastingCurrentSpell ();

					// Remove on client of the player who got blasted
					if (targetPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
					{
						final NewTurnMessageSpellBlast ntm = new NewTurnMessageSpellBlast ();
						ntm.setMsgType (NewTurnMessageTypeID.SPELL_BLAST);
						ntm.setSpellID (targetPriv.getQueuedSpell ().get (0).getQueuedSpellID ());
						ntm.setBlastedBySpellID (spell.getSpellID ());
						ntm.setBlastedByPlayerID (maintainedSpell.getCastingPlayerID ());
						
						targetTrans.getNewTurnMessage ().add (ntm);
						getPlayerMessageProcessing ().sendNewTurnMessages (null, mom.getPlayers (), null);
						
						final RemoveQueuedSpellMessage removeSpellMessage = new RemoveQueuedSpellMessage ();
						removeSpellMessage.setQueuedSpellIndex (0);
						targetPlayer.getConnection ().sendMessageToClient (removeSpellMessage);
						
						targetPlayer.getConnection ().sendMessageToClient (new UpdateManaSpentOnCastingCurrentSpellMessage ());
					}
					
					// Remove on server
					targetPriv.getQueuedSpell ().remove (0);
					targetPriv.setManaSpentOnCastingCurrentSpell (0);
					
					// Charge additional MP
					getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, -blastingCost);
					
					// On the caster's client, show that the targeted wizard is now casting nothing (if they don't have Detect Magic in effect, method will check this and do nothing)
					getSpellCasting ().sendOverlandCastingInfo (CommonDatabaseConstants.SPELL_ID_DETECT_MAGIC, castingPlayer.getPlayerDescription ().getPlayerID (), mom);
					
					// Wizards get annoyed at being targeted personally by nasty spells
					if (spell.getNastyCondition () != null)
					{
						final KnownWizardDetails targetOpinionOfCaster = getKnownWizardUtils ().findKnownWizardDetails (targetPriv.getFogOfWarMemory ().getWizardDetails (), maintainedSpell.getCastingPlayerID ());
						if (targetOpinionOfCaster != null)
							getRelationAI ().penaltyToVisibleRelation ((DiplomacyWizardDetails) targetOpinionOfCaster, 40);
					}
				}
			}
			
			else if (kind == KindOfSpell.ENEMY_WIZARD_SPELLS)
			{
				final PlayerServerDetails targetPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), targetPlayerID, "targetOverlandSpell (T)");
				final MomPersistentPlayerPrivateKnowledge targetPriv = (MomPersistentPlayerPrivateKnowledge) targetPlayer.getPersistentPlayerPrivateKnowledge ();
				
				switch (spell.getSpellID ())
				{
					case CommonDatabaseConstants.SPELL_ID_DRAIN_POWER:
					{
						final int mana = getResourceValueUtils ().findAmountStoredForProductionType (targetPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA);
						final int roll = getRandomUtils ().nextInt (101) + 50;
						final int manaLost = Math.min (mana, roll);
						
						if (manaLost > 0)
						{
							getResourceValueUtils ().addToAmountStored (targetPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, -manaLost);
							getServerResourceCalculations ().sendGlobalProductionValues (targetPlayer, null, false);
						}
						break;
					}

					case CommonDatabaseConstants.SPELL_ID_CRUEL_UNMINDING:
					{
						final int oldSkill = getResourceValueUtils ().calculateBasicCastingSkill (targetPriv.getResourceValue ());
						if (oldSkill > 0)
						{
							int newSkill = oldSkill - (((getRandomUtils ().nextInt (10) + 1) * oldSkill) / 100);		// Takes off between 1% and 10%
							if (newSkill >= oldSkill)
								newSkill = oldSkill - 1;		// Force always losing at least 1 casting skill
							
							final int newSkillPoints = getSkillCalculations ().getSkillPointsRequiredForCastingSkill (newSkill);
							final int skillLost = getResourceValueUtils ().findAmountStoredForProductionType
								(targetPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT) - newSkillPoints;

							if (skillLost > 0)
							{
								getResourceValueUtils ().addToAmountStored (targetPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_SKILL_IMPROVEMENT, -skillLost);
								getServerResourceCalculations ().sendGlobalProductionValues (targetPlayer, null, false);
							}
						}
						break;
					}

					case CommonDatabaseConstants.SPELL_ID_SUBVERSION:
					{
						// Look for all wizards who know the target wizard
						for (final PlayerServerDetails subversionPlayer : mom.getPlayers ())
						{
							final KnownWizardDetails subversionWizard = getKnownWizardUtils ().findKnownWizardDetails (mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), subversionPlayer.getPlayerDescription ().getPlayerID (), "targetOverlandSpell (S)");
							if (getPlayerKnowledgeUtils ().isWizard (subversionWizard.getWizardID ()))
							{
								final MomPersistentPlayerPrivateKnowledge subversionPriv = (MomPersistentPlayerPrivateKnowledge) subversionPlayer.getPersistentPlayerPrivateKnowledge ();
								final KnownWizardDetails subvertedWizard = getKnownWizardUtils ().findKnownWizardDetails (subversionPriv.getFogOfWarMemory ().getWizardDetails (), targetPlayerID);
								if (subvertedWizard != null)
									getRelationAI ().penaltyToVisibleRelation ((DiplomacyWizardDetails) subvertedWizard, 25);
							}
						}
						break;
					}
					
					default:
						throw new MomException ("No code to handle casting enemy wizard spell " + spell.getSpellID ());
				}
				
				// Wizards get annoyed at being targeted personally by nasty spells
				if (spell.getNastyCondition () != null)
				{
					final KnownWizardDetails targetOpinionOfCaster = getKnownWizardUtils ().findKnownWizardDetails (targetPriv.getFogOfWarMemory ().getWizardDetails (), maintainedSpell.getCastingPlayerID ());
					if (targetOpinionOfCaster != null)
						getRelationAI ().penaltyToVisibleRelation ((DiplomacyWizardDetails) targetOpinionOfCaster, 40);
				}
			}
			
			else if (kind == KindOfSpell.PLANE_SHIFT)
			{
				final List<ExpandedUnitDetails> planeShiftUnits = new ArrayList<ExpandedUnitDetails> ();
				
				for (final MemoryUnit tu : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
					if ((targetLocation.equals (tu.getUnitLocation ())) && (tu.getStatus () == UnitStatusID.ALIVE) &&
						(tu.getOwningPlayerID () == maintainedSpell.getCastingPlayerID ()))
					{
						final ExpandedUnitDetails thisTarget = getExpandUnitDetails ().expandUnitDetails (tu, null, null, null,
							mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
						
						if (getSpellTargetingUtils ().isUnitValidTargetForSpell (spell, null, null, null,
							maintainedSpell.getCastingPlayerID (), null, null, thisTarget, false, mom.getGeneralServerKnowledge ().getTrueMap (),
							priv.getFogOfWar (), mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET)
							
							planeShiftUnits.add (thisTarget);
					}
				
				if (planeShiftUnits.size () > 0)
					getFogOfWarMidTurnMultiChanges ().planeShiftUnitStack (planeShiftUnits, mom);
			}
			
			else if (kind == KindOfSpell.CHANGE_UNIT_ID)
			{
				final MapCoordinates3DEx unitLocation = (MapCoordinates3DEx) targetUnit.getUnitLocation ();
				
				// Kill the old unit
				mom.getWorldUpdates ().killUnit (targetUnit.getUnitURN (), KillUnitActionID.PERMANENT_DAMAGE);
				mom.getWorldUpdates ().process (mom);
				
				// Create new unit
				getSpellCasting ().castOverlandSummoningSpell (spell, castingPlayer, unitLocation, false, mom);
			}
			
			else if (kind == KindOfSpell.ATTACK_UNITS)
			{
				// Are the units in a city or outside any city?  Bigger diplomatic penalty for attacking units in a city
				final OverlandMapCityData cityData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
					(targetLocation.getZ ()).getRow ().get (targetLocation.getY ()).getCell ().get (targetLocation.getX ()).getCityData ();

				getSpellCasting ().castOverlandAttackSpell (castingPlayer, null, spell, maintainedSpell.getVariableDamage (), Arrays.asList (targetLocation), (cityData == null) ? 10 : 30, mom);
			}
			
			else if (kind == KindOfSpell.WARP_NODE)
			{
				// Resolve the node warping out across the full area, updating the true map as well as players' memory of who can see each cell and informing the clients too
				for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
					for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
						for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
						{
							final ServerGridCellEx aura = (ServerGridCellEx) mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get (z).getRow ().get (y).getCell ().get (x);
							if (targetLocation.equals (aura.getAuraFromNode ()))
							{
								// Update true map
								aura.getTerrainData ().setWarped (true);
								
								// Update players' memory and clients
								final MapCoordinates3DEx auraLocation = new MapCoordinates3DEx (x, y, z);
								
								getFogOfWarMidTurnChanges ().updatePlayerMemoryOfTerrain (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
									mom.getPlayers (), auraLocation, mom.getSessionDescription ().getFogOfWarSetting ().getTerrainAndNodeAuras ());
							}
						}
			}			
		}
		
		else if (kind == KindOfSpell.ATTACK_UNITS_AND_BUILDINGS)
		{
			// Earthquake or Call the Void attacking both units and buildings
			final OverlandMapCityData cityData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(targetLocation.getZ ()).getRow ().get (targetLocation.getY ()).getCell ().get (targetLocation.getX ()).getCityData ();
			getKnownWizardServerUtils ().meetWizard (maintainedSpell.getCastingPlayerID (), cityData.getCityOwnerID (), false, mom);
			
			// If it has a separate animation then show it
			if (spell.getCombatCastAnimation () != null)
			{
				maintainedSpell.setCityLocation (targetLocation);
				getFogOfWarMidTurnChanges ().sendTransientSpellToClients (maintainedSpell, mom);
			}
			
			getSpellMultiCasting ().castCityAttackSpell (spell, castingPlayer, null, maintainedSpell.getVariableDamage (), targetLocation, mom);
			
			// Remove the maintained spell on the server (clients would never have gotten it to begin with)
			mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ().remove (maintainedSpell);
			
			// This method is overkill when we know we have the exact location of the city, but still better to reuse it
			getCityProcessing ().penaltyToVisibleRelationFromNearbyCityOwner (targetLocation, maintainedSpell.getCastingPlayerID (), mom);
		}

		else if (spell.getBuildingID () == null)
		{
			// Enchantment or curse spell that generates some city or unit effect
			if ((spell.getAttackSpellOverlandTarget () == AttackSpellTargetID.ALL_UNITS) && (spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_CURSES))
			{
				// Multiple targets (stasis)
				for (final MemoryUnit tu : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
					if ((targetLocation.equals (tu.getUnitLocation ())) && (tu.getStatus () == UnitStatusID.ALIVE) &&
						(tu.getOwningPlayerID () != maintainedSpell.getCastingPlayerID ()))
					{
						final ExpandedUnitDetails thisTarget = getExpandUnitDetails ().expandUnitDetails (tu, null, null, null,
							mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
						
						if (getSpellTargetingUtils ().isUnitValidTargetForSpell (spell, null, null, null,
							maintainedSpell.getCastingPlayerID (), null, null, thisTarget, false, mom.getGeneralServerKnowledge ().getTrueMap (),
							priv.getFogOfWar (), mom.getPlayers (), mom.getServerDB ()) == TargetSpellResult.VALID_TARGET)
						{
							// For the first unit, reuse the spell that already exists on the server; for subsequent units we need to create a new spell
							getKnownWizardServerUtils ().meetWizard (maintainedSpell.getCastingPlayerID (), tu.getOwningPlayerID (), false, mom);
							
							if (maintainedSpell.getUnitURN () == null)
							{
								maintainedSpell.setUnitURN (tu.getUnitURN ());
								maintainedSpell.setUnitSkillID (unitSkillID);
								getFogOfWarMidTurnChanges ().addExistingTrueMaintainedSpellToClients (maintainedSpell, false, mom);
							}
							else
								getFogOfWarMidTurnChanges ().addMaintainedSpellOnServerAndClients (maintainedSpell.getCastingPlayerID (), maintainedSpell.getSpellID (),
									tu.getUnitURN (), unitSkillID, false, null, null, null, true,	// Don't show 2nd anim
									true, mom);
						}
					}
			}
			else
			{
				// Single target, so reuse the spell that already exists on the server
				if (spell.getSpellBookSectionID () == SpellBookSectionID.CITY_CURSES)
				{
					final OverlandMapCityData cityData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
						(targetLocation.getZ ()).getRow ().get (targetLocation.getY ()).getCell ().get (targetLocation.getX ()).getCityData ();
					getKnownWizardServerUtils ().meetWizard (maintainedSpell.getCastingPlayerID (), cityData.getCityOwnerID (), false, mom);
				}
				
				// Set values on server
				if (targetUnit != null)
					maintainedSpell.setUnitURN (targetUnit.getUnitURN ());
				
				maintainedSpell.setCityLocation (targetLocation);
				maintainedSpell.setUnitSkillID (unitSkillID);
				maintainedSpell.setCitySpellEffectID (citySpellEffectID);
				
				// Add spell on clients (they don't have a blank version of it before now)
				getFogOfWarMidTurnChanges ().addExistingTrueMaintainedSpellToClients (maintainedSpell, false, mom);
				
				// If its a unit enchantment, does it grant any secondary permanent effects? (Black Channels making units Undead)
				if (spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_ENCHANTMENTS)
				{
					final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (targetUnit, null, null, spell.getSpellRealm (),
						mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
					
					for (final UnitSpellEffect effect : spell.getUnitSpellEffect ())
						if ((effect.isPermanent () != null) && (effect.isPermanent ()) && (!xu.hasBasicSkill (effect.getUnitSkillID ())))
						{
							final UnitSkillAndValue permanentEffect = new UnitSkillAndValue ();
							permanentEffect.setUnitSkillID (effect.getUnitSkillID ());
							targetUnit.getUnitHasSkill ().add (permanentEffect);
							
							getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (targetUnit, mom, null);
						}
				}
			
				else if (targetLocation != null)
				{
					// Consecration will remove all curses cast on the city
					if (citySpellEffectID != null)
					{
						final CitySpellEffect citySpellEffect = mom.getServerDB ().findCitySpellEffect (citySpellEffectID, "targetOverlandSpell");
						if (citySpellEffect.getProtectsAgainstSpellRealm ().size () > 0)
						{
							final List<MemoryMaintainedSpell> spellsToRemove = mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ().stream ().filter
								(s -> (targetLocation.equals (s.getCityLocation ())) && (maintainedSpell.getCastingPlayerID () != s.getCastingPlayerID ())).collect (Collectors.toList ());
							
							// Still need to check spell realms
							for (final MemoryMaintainedSpell spellToRemove : spellsToRemove)
							{
								final Spell spellToRemoveDef = mom.getServerDB ().findSpell (spellToRemove.getSpellID (), "targetOverlandSpell");
								if (citySpellEffect.getProtectsAgainstSpellRealm ().contains (spellToRemoveDef.getSpellRealm ()))
									mom.getWorldUpdates ().switchOffSpell (spellToRemove.getSpellURN (), false);
							}
							
							mom.getWorldUpdates ().process (mom);
						}
					}
					
					// If its a city enchantment or curse, better recalculate everything on the city
					final OverlandMapCityData cityData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
						(targetLocation.getZ ()).getRow ().get (targetLocation.getY ()).getCell ().get (targetLocation.getX ()).getCityData ();
					if (cityData != null)
					{
						final PlayerServerDetails cityOwner = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), cityData.getCityOwnerID (), "targetOverlandSpell (C)");
						final MomPersistentPlayerPrivateKnowledge cityOwnerPriv = (MomPersistentPlayerPrivateKnowledge) cityOwner.getPersistentPlayerPrivateKnowledge ();
						
						getServerCityCalculations ().calculateCitySizeIDAndMinimumFarmers (targetLocation, mom);
							
						// Although farmers will be the same, capturing player may have a different tax rate or different units stationed here so recalc rebels
						cityData.setNumberOfRebels (getCityCalculations ().calculateCityRebels (mom.getGeneralServerKnowledge ().getTrueMap (),
							targetLocation, cityOwnerPriv.getTaxRateID (), mom.getServerDB ()).getFinalTotal ());
						
						getServerCityCalculations ().ensureNotTooManyOptionalFarmers (cityData);
						
						getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
							mom.getPlayers (), targetLocation, mom.getSessionDescription ().getFogOfWarSetting ());
					}
				}
			}
		}
		else
		{
			// Spell that creates a building instead of an effect, like "Wall of Stone" or "Move Fortress"
			// Is it a type of building where we only ever have one of them, and need to remove the existing one?
			final List<String> buildingIDsToAdd = new ArrayList<String> ();
			buildingIDsToAdd.add (spell.getBuildingID ());
			
			if ((spell.getBuildingID ().equals (CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE)) ||
				(spell.getBuildingID ().equals (CommonDatabaseConstants.BUILDING_FORTRESS)))
			{
				final List<Integer> buildingURNsToDestroy = new ArrayList<Integer> ();
				
				// Find & remove the main building for this spell
				final MemoryBuilding mainDestroyedBuilding = getMemoryBuildingUtils ().findCityWithBuilding
					(castingPlayer.getPlayerDescription ().getPlayerID (), spell.getBuildingID (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
						mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ());
				
				if (mainDestroyedBuilding != null)
					buildingURNsToDestroy.add (mainDestroyedBuilding.getBuildingURN ());
				
				// Move summoning circle as well if its in the same place as the wizard's fortress
				if (spell.getBuildingID ().equals (CommonDatabaseConstants.BUILDING_FORTRESS))
				{
					final MemoryBuilding secondaryDestroyedBuilding = getMemoryBuildingUtils ().findCityWithBuilding
						(castingPlayer.getPlayerDescription ().getPlayerID (), CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE,
							mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ());
						
					if ((secondaryDestroyedBuilding != null) && (secondaryDestroyedBuilding.getCityLocation ().equals (mainDestroyedBuilding.getCityLocation ())))
						buildingURNsToDestroy.add (secondaryDestroyedBuilding.getBuildingURN ());
					
					// Place a summoning circle as well if we just destroyed it OR if we never had one in the first place (Spell of Return)
					if ((secondaryDestroyedBuilding == null) ||
						((secondaryDestroyedBuilding != null) && (secondaryDestroyedBuilding.getCityLocation ().equals (mainDestroyedBuilding.getCityLocation ()))))
						
						buildingIDsToAdd.add (CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE);
				}
				
				// Remove old buildings if we found any
				if (buildingURNsToDestroy.size () > 0)
					getFogOfWarMidTurnChanges ().destroyBuildingOnServerAndClients (buildingURNsToDestroy, false, null, null, null, mom);
			}

			// Is the building that the spell is adding the same as what was being constructed?  If so then reset construction.
			// (Casting Wall of Stone in a city that's building City Walls).
			final OverlandMapCityData cityData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
				(targetLocation.getZ ()).getRow ().get (targetLocation.getY ()).getCell ().get (targetLocation.getX ()).getCityData ();
						
			if ((cityData != null) && (spell.getBuildingID ().equals (cityData.getCurrentlyConstructingBuildingID ())))
			{
				cityData.setCurrentlyConstructingBuildingID (ServerDatabaseValues.CITY_CONSTRUCTION_DEFAULT);
				getFogOfWarMidTurnChanges ().updatePlayerMemoryOfCity (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
					mom.getPlayers (), targetLocation, mom.getSessionDescription ().getFogOfWarSetting ());
			}
			
			// If it is Spell of Return then update wizard state back to active.
			// Note we have to do this before actually adding the building, so the animation shows the fortress initially NOT there.
			if (spell.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_SPELL_OF_RETURN))
			{
				// Update on server
				getKnownWizardServerUtils ().updateWizardState (castingPlayer.getPlayerDescription ().getPlayerID (), WizardState.ACTIVE, mom);
				
				// Update wizardState on client, and this triggers showing the returning animation as well
				final UpdateWizardStateMessage msg = new UpdateWizardStateMessage ();
				msg.setBanishedPlayerID (castingPlayer.getPlayerDescription ().getPlayerID ());
				msg.setWizardState (WizardState.ACTIVE);
				msg.setRenderCityData (getCityCalculations ().buildRenderCityData (targetLocation,
					mom.getSessionDescription ().getOverlandMapSize (), mom.getGeneralServerKnowledge ().getTrueMap ()));
				getMultiplayerSessionServerUtils ().sendMessageToAllClients (mom.getPlayers (), msg);
			}

			// Finally actually create the building(s)
			getFogOfWarMidTurnChanges ().addBuildingOnServerAndClients (targetLocation, buildingIDsToAdd, spell.getSpellID (),
				castingPlayer.getPlayerDescription ().getPlayerID (), true, mom);
			
			// Remove the maintained spell on the server (clients would never have gotten it to begin with)
			mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ().remove (maintainedSpell);
		}
		
		// New spell will probably use up some mana maintenance
		getServerResourceCalculations ().recalculateGlobalProductionValues (castingPlayer.getPlayerDescription ().getPlayerID (), false, mom);
	}

	/**
	 * Overland spells are cast first (probably taking several turns) and a target is only chosen after casting is completed.
	 * But perhaps by the time we finish casting it, we no longer have a valid target or changed our minds, so this just cancels and loses the spell.
	 * 
	 * @param maintainedSpell Spell being targeted in server's true memory - at the time this is called, this is the only copy of the spell that exists,
	 * 	so its the only thing we need to clean up
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void cancelTargetOverlandSpell (final MemoryMaintainedSpell maintainedSpell, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		// Remove it
		mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ().remove (maintainedSpell);
		
		// Cancelled spell will probably use up some mana maintenance
		getServerResourceCalculations ().recalculateGlobalProductionValues (maintainedSpell.getCastingPlayerID (), false, mom);
	}
	
	/**
	 * When a wizard is banished or defeated, can steal up to 2 spells from them as long as we have enough books to allow it.
	 * 
	 * @param stealFrom Wizard who was banished, who spells are being stolen from
	 * @param giveTo Wizard who banished them, who spells are being given to
	 * @param spellsStolenFromFortress Maximum number of spells to steal
	 * @param db Lookup lists built over the XML database
	 * @return List of spells that were stolen
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Override
	public final List<String> stealSpells (final PlayerServerDetails stealFrom, final PlayerServerDetails giveTo, final int spellsStolenFromFortress, final CommonDatabase db)
		throws JAXBException, XMLStreamException, RecordNotFoundException
	{
		final MomPersistentPlayerPrivateKnowledge stealFromPriv = (MomPersistentPlayerPrivateKnowledge) stealFrom.getPersistentPlayerPrivateKnowledge ();
		final MomPersistentPlayerPrivateKnowledge giveToPriv = (MomPersistentPlayerPrivateKnowledge) giveTo.getPersistentPlayerPrivateKnowledge ();
		
		final List<String> knownSpellIDs = stealFromPriv.getSpellResearchStatus ().stream ().filter
			(rs -> rs.getStatus () == SpellResearchStatusID.AVAILABLE).map (rs -> rs.getSpellID ()).collect (Collectors.toList ());
		
		// Check every spell the stealer does not have, to see if they can gain it
		final List<SpellResearchStatus> possibleSpellsToSteal = new ArrayList<SpellResearchStatus> ();
		for (final SpellResearchStatus spell : giveToPriv.getSpellResearchStatus ())
			if ((spell.getStatus () != SpellResearchStatusID.AVAILABLE) && (spell.getStatus () != SpellResearchStatusID.UNAVAILABLE) && (knownSpellIDs.contains (spell.getSpellID ())))
				possibleSpellsToSteal.add (spell);

		final List<String> stolenSpellIDs = new ArrayList<String> ();
		boolean anySpellsWereResearchableNow = false;
		while ((stolenSpellIDs.size () < spellsStolenFromFortress) && (possibleSpellsToSteal.size () > 0))
		{
			// Randomly pick one
			final SpellResearchStatus spell = possibleSpellsToSteal.get (getRandomUtils ().nextInt (possibleSpellsToSteal.size ()));
			stolenSpellIDs.add (spell.getSpellID ());
			possibleSpellsToSteal.remove (spell);

			if (spell.getStatus () == SpellResearchStatusID.RESEARCHABLE_NOW)
				anySpellsWereResearchableNow = true;
			
			// If the spell happened to be the one we were researching, then blank research out (client does this based on FullSpellListMessage)
			if (spell.getSpellID ().equals (giveToPriv.getSpellIDBeingResearched ()))
				giveToPriv.setSpellIDBeingResearched (null);
			
			spell.setStatus (SpellResearchStatusID.AVAILABLE);
		}
		
		// If any spells were in the 8 that could be researched now, then need to pull in some more
		if (anySpellsWereResearchableNow)		
			getServerSpellCalculations ().randomizeSpellsResearchableNow (giveToPriv.getSpellResearchStatus (), db);
		
		// Any update to send to client?
		if ((stolenSpellIDs.size () > 0) && (giveTo.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN))
		{
			final FullSpellListMessage spellsMsg = new FullSpellListMessage ();
			spellsMsg.getSpellResearchStatus ().addAll (giveToPriv.getSpellResearchStatus ());
			giveTo.getConnection ().sendMessageToClient (spellsMsg);
		}
		
		return stolenSpellIDs;		
	}
	
	/**
	 * For Gaia's blessing.  Each turn it has a chance of turning deserts into grasslands, or volcanoes into hills.
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param onlyOnePlayerID If zero, will process all spells; if specified will process only spells cast by the specified player
	 * @throws RecordNotFoundException If we encounter a spell with an unknown city spell effect
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 */
	@Override
	public final void rollSpellTerrainEffectsEachTurn (final MomSessionVariables mom, final int onlyOnePlayerID)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		for (final MemoryMaintainedSpell spell : mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ())
			if ((spell.getCitySpellEffectID () != null) && (spell.getCityLocation () != null) &&
				((onlyOnePlayerID == 0) || (onlyOnePlayerID == spell.getCastingPlayerID ())))
			{
				final CitySpellEffect citySpellEffect = mom.getServerDB ().findCitySpellEffect (spell.getCitySpellEffectID (), "rollSpellTerrainEffectsEachTurn");
				
				final Map<String, CitySpellEffectTileType> tileTypesToRoll = citySpellEffect.getCitySpellEffectTileType ().stream ().filter
					(t -> (t.getChangeToTileTypeID () != null) && (t.getChangeToTileTypeChance () != null)).collect (Collectors.toMap (t -> t.getTileTypeID (), t -> t));
				
				// Now check the tiles around the city to see if any match the ones that might get updated
				if ((!tileTypesToRoll.isEmpty ()) || (citySpellEffect.getPurifyCorruptedTilesChance () != null))
				{
					final MapCoordinates3DEx coords = new MapCoordinates3DEx ((MapCoordinates3DEx) spell.getCityLocation ());
					for (final SquareMapDirection direction : CityCalculationsImpl.DIRECTIONS_TO_TRAVERSE_CITY_RADIUS)
						if (getCoordinateSystemUtils ().move3DCoordinates (mom.getSessionDescription ().getOverlandMapSize (), coords, direction.getDirectionID ()))
						{
							final OverlandMapTerrainData terrainData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
								(coords.getZ ()).getRow ().get (coords.getY ()).getCell ().get (coords.getX ()).getTerrainData ();
							
							boolean terrainUpdated = false;
							
							// Changing one tile type into another
							final CitySpellEffectTileType change = tileTypesToRoll.get (terrainData.getTileTypeID ());
							if ((change != null) && (getRandomUtils ().nextInt (100) < change.getChangeToTileTypeChance ()))
							{
								if (terrainData.getTileTypeID ().equals (CommonDatabaseConstants.TILE_TYPE_RAISE_VOLCANO))
									terrainData.setVolcanoOwnerID (null);
								
								terrainData.setTileTypeID (change.getChangeToTileTypeID ());
								
								terrainUpdated = true;
							}
							
							// Purifying corruption
							if ((terrainData.getCorrupted () != null) && (citySpellEffect.getPurifyCorruptedTilesChance () != null) &&
								(getRandomUtils ().nextInt (100) < citySpellEffect.getPurifyCorruptedTilesChance ()))
							{
								terrainData.setCorrupted (null);
								terrainUpdated = true;
							}
							
							if (terrainUpdated)
								getFogOfWarMidTurnChanges ().updatePlayerMemoryOfTerrain (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
									mom.getPlayers (), coords, mom.getSessionDescription ().getFogOfWarSetting ().getTerrainAndNodeAuras ());
						}
				}
			}
	}
	
	/**
	 * For Chaos Rift.  Each turn, units in the city get struck by lightning bolts.
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param onlyOnePlayerID If zero, will process CSEs belonging to everyone; if specified will process only CAEs owned by the specified player
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void citySpellEffectsAttackingUnits (final MomSessionVariables mom, final int onlyOnePlayerID)
		throws JAXBException, XMLStreamException, IOException
	{
		for (final MemoryMaintainedSpell spell : mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ())
			if ((spell.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_CHAOS_RIFT)) && (spell.getCityLocation () != null) &&
				((onlyOnePlayerID == 0) || (onlyOnePlayerID == spell.getCastingPlayerID ())))
			{
				// Get a list of units in the city, even if they're invisible or immune to the damage
				final List<MemoryUnit> unitsInCity = getUnitUtils ().listAliveEnemiesAtLocation (mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (),
					spell.getCityLocation ().getX (), spell.getCityLocation ().getY (), spell.getCityLocation ().getZ (), 0);
				if (unitsInCity.size () > 0)
				{
					final Spell spellDef = mom.getServerDB ().findSpell (spell.getSpellID (), "citySpellEffectsAttackingUnits");

					final List<ExpandedUnitDetails> expandedUnitsInCity = new ArrayList<ExpandedUnitDetails> ();
					for (final MemoryUnit mu : unitsInCity)
						expandedUnitsInCity.add (getExpandUnitDetails ().expandUnitDetails (mu, null, null, spellDef.getSpellRealm (), mom.getPlayers (),
							mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ()));
					
					// Roll damage; the XML specifies the damage type, attack strength and so on
					final DamageType damageType = mom.getServerDB ().findDamageType (spellDef.getAttackSpellDamageTypeID (), "citySpellEffectsAttackingUnits");
					final PlayerServerDetails castingPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), spell.getCastingPlayerID (), "citySpellEffectsAttackingUnits");
					
					AttackDamage attackDamage = null;
					for (int lightningBoltNo = 0; lightningBoltNo < 5; lightningBoltNo++)
					{
						final ExpandedUnitDetails xu = expandedUnitsInCity.get (getRandomUtils ().nextInt (expandedUnitsInCity.size ()));
						
						// Be careful that previous lightning bolts may have already killed it
						if ((xu.calculateAliveFigureCount () > 0) && (!xu.isUnitImmuneToDamageType (damageType)))
						{
							// Only bother to send the damage calculation header once we find a unit that actually has to make a roll.
							// Maybe even though there's 2 units in the city, one is immune and one isn't, they get lucky and all 5 bolts hit the immune one...
							// Assuming here that all struck units will belong to the same player, but that should be the case.
							if (attackDamage == null)
							{
								getDamageCalculator ().sendDamageHeader (null, unitsInCity, false,
									castingPlayer, (PlayerServerDetails) xu.getOwningPlayer (), null, null, spellDef, castingPlayer);
								
								attackDamage = getDamageCalculator ().attackFromSpell (spellDef, null, castingPlayer, null, castingPlayer,
									(PlayerServerDetails) xu.getOwningPlayer (), null, mom.getServerDB (), SpellCastType.OVERLAND, false);
							}
							
							// Its not enough to call armour piercing damage directly - must call this wrapper method so that it applies the damage to the unit as well
							getAttackResolutionProcessing ().processAttackResolutionStep (null, new AttackResolutionUnit (xu.getMemoryUnit ()),
								castingPlayer, (PlayerServerDetails) xu.getOwningPlayer (), null, Arrays.asList (new AttackResolutionStepContainer (attackDamage)), mom);
							
							// Above method applies damage on the server, but doesn't send it to the client or check if the unit is now dead, so need to do that here
							if (xu.calculateAliveFigureCount () > 0)
								getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (xu.getMemoryUnit (), mom, null);
							else
								mom.getWorldUpdates ().killUnit (xu.getUnitURN (), KillUnitActionID.HEALABLE_OVERLAND_DAMAGE);
						}
					}
				}
			}
		
		mom.getWorldUpdates ().process (mom);
	}

	/**
	 * For Chaos Rift.  Each turn, there is a chance of each building in the city being destroyed.
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param onlyOnePlayerID If zero, will process CSEs belonging to everyone; if specified will process only CAEs owned by the specified player
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void citySpellEffectsAttackingBuildings (final MomSessionVariables mom, final int onlyOnePlayerID)
		throws JAXBException, XMLStreamException, IOException
	{
		for (final MemoryMaintainedSpell spell : mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ())
			if ((spell.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_CHAOS_RIFT)) && (spell.getCityLocation () != null) &&
				((onlyOnePlayerID == 0) || (onlyOnePlayerID == spell.getCastingPlayerID ())))
				
				getSpellCasting ().rollChanceOfEachBuildingBeingDestroyed (spell.getSpellID (), spell.getCastingPlayerID (), 5,
					Arrays.asList ((MapCoordinates3DEx) spell.getCityLocation ()), mom);
	}
	
	/**
	 * For Pestilence.  Each turn, there is a chance of 1000 people dying.
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param onlyOnePlayerID If zero, will process CSEs belonging to everyone; if specified will process only CAEs owned by the specified player
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void citySpellEffectsAttackingPopulation (final MomSessionVariables mom, final int onlyOnePlayerID)
		throws JAXBException, XMLStreamException, IOException
	{
		// Get a list of all cities with Pestilence cast on them, even if its the not the right player's turn to trigger it now
		final List<MapCoordinates3DEx> pestilenceAll = mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ().stream ().filter
			(s -> (s.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_PESTILENCE)) && (s.getCityLocation () != null)).map
			(s -> (MapCoordinates3DEx) s.getCityLocation ()).collect (Collectors.toList ());

		// Get a list of all cities with Pestilence cast on them that will be triggered now
		final List<MapCoordinates3DEx> triggerNow;
		if (onlyOnePlayerID == 0)
		{
			triggerNow = new ArrayList<MapCoordinates3DEx> ();
			triggerNow.addAll (pestilenceAll);
		}
		else
			triggerNow = mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ().stream ().filter
				(s -> (s.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_PESTILENCE)) && (s.getCityLocation () != null) &&
					(onlyOnePlayerID == s.getCastingPlayerID ())).map
				(s -> (MapCoordinates3DEx) s.getCityLocation ()).collect (Collectors.toList ());
		
		// Add to that list any cities with the plague event, this triggers on the turn of the city owner.
		// But don't trigger it if the city is also suffering from pestilence, which would trigger on the spell owner's turn.
		for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
			for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
				for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
				{
					final OverlandMapCityData cityData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
						(z).getRow ().get (y).getCell ().get (x).getCityData ();
					if ((cityData != null) && (cityData.getCityPopulation () >= CommonDatabaseConstants.MIN_CITY_POPULATION) && (CommonDatabaseConstants.EVENT_ID_PLAGUE.equals (cityData.getPopulationEventID ())) &&
						(!pestilenceAll.contains (new MapCoordinates3DEx (x, y, z))) &&
							((onlyOnePlayerID == 0) || (onlyOnePlayerID == cityData.getCityOwnerID ())))
						
						triggerNow.add (new MapCoordinates3DEx (x, y, z));
				}
		
		if (!triggerNow.isEmpty ())
		{
			for (final MapCoordinates3DEx cityLocation : triggerNow)
			{
				final OverlandMapCityData cityData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
					(cityLocation.getZ ()).getRow ().get (cityLocation.getY ()).getCell ().get (cityLocation.getX ()).getCityData ();
				if ((cityData != null) && (cityData.getCityPopulation () >= 2000))
					if (cityData.getCityPopulation () / 1000 > getRandomUtils ().nextInt (10) + 1)
					{
						cityData.setCityPopulation (cityData.getCityPopulation () - 1000);
						mom.getWorldUpdates ().recalculateCity (cityLocation);
					}
			}
			
			mom.getWorldUpdates ().process (mom);
		}
	}
	
	/**
	 * For Stasis.  Each turn, units with stasis get a resistance roll for a chance to free themselves.
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param onlyOnePlayerID If zero, will process units belonging to everyone; if specified will process only units owned by the specified player
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void rollToRemoveOverlandCurses (final MomSessionVariables mom, final int onlyOnePlayerID)
		throws JAXBException, XMLStreamException, IOException
	{
		// Run down copy of spell list, since we'll be possibly removing some as we go along
		final List<MemoryMaintainedSpell> trueSpells = new ArrayList<MemoryMaintainedSpell> ();
		trueSpells.addAll (mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ());
		
		for (final MemoryMaintainedSpell spell : trueSpells)
			if ((spell.getUnitURN () != null) && (spell.getUnitSkillID () != null))
			{
				final Spell spellDef = mom.getServerDB ().findSpell (spell.getSpellID (), "rollToRemoveOverlandCurses");
				if (spellDef.getSpellBookSectionID () == SpellBookSectionID.UNIT_CURSES)
				{
					// Check if right player
					final MemoryUnit tu = getUnitUtils ().findUnitURN (spell.getUnitURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), "rollToRemoveOverlandCurses");
					if ((onlyOnePlayerID == 0) || (onlyOnePlayerID == tu.getOwningPlayerID ()))
					{
						// Stasis has two skill IDs, one for the first time and one for subsequent turns.
						// This is so even units with Magic Immunity get frozen for one turn.
						if (spell.getUnitSkillID ().equals (CommonDatabaseConstants.UNIT_SKILL_ID_STASIS_FIRST_TURN))
						{
							spell.setUnitSkillID (CommonDatabaseConstants.UNIT_SKILL_ID_STASIS_LATER_TURNS);
							getFogOfWarMidTurnChanges ().updatePlayerMemoryOfSpell (spell, mom);
						}
						else
						{
							final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails
								(tu, null, null, spellDef.getSpellRealm (), mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
							
							// Are we immune to it?  Possibly we already had the effect and then had Magic Immunity cast on us overland.
							// If we are immune to it, it won't show in the unit's modified list, because we already know they have the curse.
							boolean removeSpell = !xu.hasModifiedSkill (spell.getUnitSkillID ());
							if (!removeSpell)
							{
								// Make resistance roll
								final PlayerServerDetails castingPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID
									(mom.getPlayers (), spell.getCastingPlayerID (), "rollOverlandCursesEachTurn");
								
								// Not in combat, so attacking/defending player not really relevant, but really all this is used for is
								// which two players to send the damage calc to, so just send the unit and spell owner
								removeSpell = !getDamageProcessor ().makeResistanceRoll (null, tu, castingPlayer, (PlayerServerDetails) xu.getOwningPlayer (),
									spellDef, null, true, castingPlayer, SpellCastType.OVERLAND, false, mom); 
							}
							
							if (removeSpell)
								mom.getWorldUpdates ().switchOffSpell (spell.getSpellURN (), false);
						}
					}
				}
			}
		
		mom.getWorldUpdates ().process (mom);
	}
	
	/**
	 * Triggers any overland enchantments that activate every turn with no specific trigger
	 * 
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param onlyOnePlayerID If zero, will process units belonging to everyone; if specified will process only units owned by the specified player
	 * @throws JAXBException If there is a problem converting a message to send to a player into XML
	 * @throws XMLStreamException If there is a problem sending a message to a player
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void triggerOverlandEnchantments (final MomSessionVariables mom, final int onlyOnePlayerID)
		throws JAXBException, XMLStreamException, IOException
	{
		for (final MemoryMaintainedSpell spell : mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ())
			if ((onlyOnePlayerID == 0) || (onlyOnePlayerID == spell.getCastingPlayerID ()))
			{
				final Spell spellDef = mom.getServerDB ().findSpell (spell.getSpellID (), "triggerOverlandEnchantments");
				
				// It has to be an overland enchantment that specifies some kind of damage, but no specific way to trigger it -
				// then we just assume it is triggered every turn.  Great Wasting is an exception as it does no actual damage but still triggers like this.
				if ((spell.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_GREAT_WASTING)) ||
					(spell.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_ARMAGEDDON)) ||
					((spellDef.getSpellBookSectionID () == SpellBookSectionID.OVERLAND_ENCHANTMENTS) &&
						(spellDef.getAttackSpellDamageResolutionTypeID () != null) && (spellDef.getTriggeredBySpellRealm ().size () == 0)))
					
					getSpellTriggers ().triggerSpell (spell, null, null, null, mom);
			}
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
	 * @return Memory building utils
	 */
	public final MemoryBuildingUtils getMemoryBuildingUtils ()
	{
		return memoryBuildingUtils;
	}

	/**
	 * @param utils Memory building utils
	 */
	public final void setMemoryBuildingUtils (final MemoryBuildingUtils utils)
	{
		memoryBuildingUtils = utils;
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
	 * @return Resource value utils
	 */
	public final ResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param utils Resource value utils
	 */
	public final void setResourceValueUtils (final ResourceValueUtils utils)
	{
		resourceValueUtils = utils;
	}
	
	/**
	 * @return Unit utils
	 */
	public final UnitUtils getUnitUtils ()
	{
		return unitUtils;
	}

	/**
	 * @param utils Unit utils
	 */
	public final void setUnitUtils (final UnitUtils utils)
	{
		unitUtils = utils;
	}
	
	/**
	 * @return Server-only unit utils
	 */
	public final UnitServerUtils getUnitServerUtils ()
	{
		return unitServerUtils;
	}

	/**
	 * @param utils Server-only unit utils
	 */
	public final void setUnitServerUtils (final UnitServerUtils utils)
	{
		unitServerUtils = utils;
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
	 * @return Methods for updating true map + players' memory
	 */
	public final FogOfWarMidTurnMultiChanges getFogOfWarMidTurnMultiChanges ()
	{
		return fogOfWarMidTurnMultiChanges;
	}

	/**
	 * @param obj Methods for updating true map + players' memory
	 */
	public final void setFogOfWarMidTurnMultiChanges (final FogOfWarMidTurnMultiChanges obj)
	{
		fogOfWarMidTurnMultiChanges = obj;
	}
	
	/**
	 * @return Combat processing
	 */
	public final CombatProcessing getCombatProcessing ()
	{
		return combatProcessing;
	}

	/**
	 * @param proc Combat processing
	 */
	public final void setCombatProcessing (final CombatProcessing proc)
	{
		combatProcessing = proc;
	}

	/**
	 * @return Resource calculations
	 */
	public final ServerResourceCalculations getServerResourceCalculations ()
	{
		return serverResourceCalculations;
	}

	/**
	 * @param calc Resource calculations
	 */
	public final void setServerResourceCalculations (final ServerResourceCalculations calc)
	{
		serverResourceCalculations = calc;
	}

	/**
	 * @return Server-only overland map utils
	 */
	public final OverlandMapServerUtils getOverlandMapServerUtils ()
	{
		return overlandMapServerUtils;
	}
	
	/**
	 * @param utils Server-only overland map utils
	 */
	public final void setOverlandMapServerUtils (final OverlandMapServerUtils utils)
	{
		overlandMapServerUtils = utils;
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
	 * @return Damage processor
	 */
	public final DamageProcessor getDamageProcessor ()
	{
		return damageProcessor;
	}

	/**
	 * @param proc Damage processor
	 */
	public final void setDamageProcessor (final DamageProcessor proc)
	{
		damageProcessor = proc;
	}

	/**
	 * @return Map generator
	 */
	public final CombatMapGenerator getCombatMapGenerator ()
	{
		return combatMapGenerator;
	}

	/**
	 * @param gen Map generator
	 */
	public final void setCombatMapGenerator (final CombatMapGenerator gen)
	{
		combatMapGenerator = gen;
	}

	/**
	 * @return Methods dealing with hero items
	 */
	public final HeroItemServerUtils getHeroItemServerUtils ()
	{
		return heroItemServerUtils;
	}

	/**
	 * @param util Methods dealing with hero items
	 */
	public final void setHeroItemServerUtils (final HeroItemServerUtils util)
	{
		heroItemServerUtils = util;
	}

	/**
	 * @return Starting and ending combats
	 */
	public final CombatStartAndEnd getCombatStartAndEnd ()
	{
		return combatStartAndEnd;
	}

	/**
	 * @param cse Starting and ending combats
	 */
	public final void setCombatStartAndEnd (final CombatStartAndEnd cse)
	{
		combatStartAndEnd = cse;
	}

	/**
	 * @return Operations for processing combat maps
	 */
	public final MapAreaOperations2D<MomCombatTile> getCombatMapOperations ()
	{
		return combatMapOperations;
	}

	/**
	 * @param op Operations for processing combat maps
	 */
	public final void setCombatMapOperations (final MapAreaOperations2D<MomCombatTile> op)
	{
		combatMapOperations = op;
	}

	/**
	 * @return City calculations
	 */
	public final CityCalculations getCityCalculations ()
	{
		return cityCalculations;
	}

	/**
	 * @param calc City calculations
	 */
	public final void setCityCalculations (final CityCalculations calc)
	{
		cityCalculations = calc;
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
	 * @return Main FOW update routine
	 */
	public final FogOfWarProcessing getFogOfWarProcessing ()
	{
		return fogOfWarProcessing;
	}

	/**
	 * @param obj Main FOW update routine
	 */
	public final void setFogOfWarProcessing (final FogOfWarProcessing obj)
	{
		fogOfWarProcessing = obj;
	}

	/**
	 * @return Methods for dealing with player msgs
	 */
	public PlayerMessageProcessing getPlayerMessageProcessing ()
	{
		return playerMessageProcessing;
	}

	/**
	 * @param obj Methods for dealing with player msgs
	 */
	public final void setPlayerMessageProcessing (final PlayerMessageProcessing obj)
	{
		playerMessageProcessing = obj;
	}

	/**
	 * @return AI decisions about spells
	 */
	public final SpellAI getSpellAI ()
	{
		return spellAI;
	}

	/**
	 * @param ai AI decisions about spells
	 */
	public final void setSpellAI (final SpellAI ai)
	{
		spellAI = ai;
	}

	/**
	 * @return Server-only spell calculations
	 */
	public final ServerSpellCalculations getServerSpellCalculations ()
	{
		return serverSpellCalculations;
	}

	/**
	 * @param calc Server-only spell calculations
	 */
	public final void setServerSpellCalculations (final ServerSpellCalculations calc)
	{
		serverSpellCalculations = calc;
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
	 * @return Kind of spell utils
	 */
	public final KindOfSpellUtils getKindOfSpellUtils ()
	{
		return kindOfSpellUtils;
	}

	/**
	 * @param k Kind of spell utils
	 */
	public final void setKindOfSpellUtils (final KindOfSpellUtils k)
	{
		kindOfSpellUtils = k;
	}

	/**
	 * @return City processing methods
	 */
	public final CityProcessing getCityProcessing ()
	{
		return cityProcessing;
	}

	/**
	 * @param obj City processing methods
	 */
	public final void setCityProcessing (final CityProcessing obj)
	{
		cityProcessing = obj;
	}

	/**
	 * @return Server-only city calculations
	 */
	public final ServerCityCalculations getServerCityCalculations ()
	{
		return serverCityCalculations;
	}

	/**
	 * @param calc Server-only city calculations
	 */
	public final void setServerCityCalculations (final ServerCityCalculations calc)
	{
		serverCityCalculations = calc;
	}

	/**
	 * @return Damage calc
	 */
	public final DamageCalculator getDamageCalculator ()
	{
		return damageCalculator;
	}

	/**
	 * @param calc Damage calc
	 */
	public final void setDamageCalculator (final DamageCalculator calc)
	{
		damageCalculator = calc;
	}

	/**
	 * @return Attack resolution processing
	 */
	public final AttackResolutionProcessing getAttackResolutionProcessing ()
	{
		return attackResolutionProcessing;
	}

	/**
	 * @param proc Attack resolution processing
	 */
	public final void setAttackResolutionProcessing (final AttackResolutionProcessing proc)
	{
		attackResolutionProcessing= proc;
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

	/**
	 * @return Methods mainly dealing with when very rare overland enchantments are triggered
	 */
	public final SpellTriggers getSpellTriggers ()
	{
		return spellTriggers;
	}

	/**
	 * @param t Methods mainly dealing with when very rare overland enchantments are triggered
	 */
	public final void setSpellTriggers (final SpellTriggers t)
	{
		spellTriggers = t;
	}

	/**
	 * @return Skill calculations
	 */
	public final SkillCalculations getSkillCalculations ()
	{
		return skillCalculations;
	}

	/**
	 * @param calc Skill calculations
	 */
	public final void setSkillCalculations (final SkillCalculations calc)
	{
		skillCalculations = calc;
	}

	/**
	 * @return Server-only unit calculations
	 */
	public final ServerUnitCalculations getServerUnitCalculations ()
	{
		return serverUnitCalculations;
	}

	/**
	 * @param calc Server-only unit calculations
	 */
	public final void setServerUnitCalculations (final ServerUnitCalculations calc)
	{
		serverUnitCalculations = calc;
	}

	/**
	 * @return Combat map utils
	 */
	public final CombatMapUtils getCombatMapUtils ()
	{
		return combatMapUtils;
	}

	/**
	 * @param util Combat map utils
	 */
	public final void setCombatMapUtils (final CombatMapUtils util)
	{
		combatMapUtils = util;
	}

	/**
	 * @return Movement utils
	 */
	public final MovementUtils getMovementUtils ()
	{
		return movementUtils;
	}

	/**
	 * @param u Movement utils
	 */
	public final void setMovementUtils (final MovementUtils u)
	{
		movementUtils = u;
	}

	/**
	 * @return Casting spells that have more than one effect
	 */
	public final SpellMultiCasting getSpellMultiCasting ()
	{
		return spellMultiCasting;
	}

	/**
	 * @param c Casting spells that have more than one effect
	 */
	public final void setSpellMultiCasting (final SpellMultiCasting c)
	{
		spellMultiCasting = c;
	}

	/**
	 * @return Methods for working with wizardIDs
	 */
	public final PlayerKnowledgeUtils getPlayerKnowledgeUtils ()
	{
		return playerKnowledgeUtils;
	}

	/**
	 * @param k Methods for working with wizardIDs
	 */
	public final void setPlayerKnowledgeUtils (final PlayerKnowledgeUtils k)
	{
		playerKnowledgeUtils = k;
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
	 * @return Process for making sure one wizard has met another wizard
	 */
	public final KnownWizardServerUtils getKnownWizardServerUtils ()
	{
		return knownWizardServerUtils;
	}

	/**
	 * @param k Process for making sure one wizard has met another wizard
	 */
	public final void setKnownWizardServerUtils (final KnownWizardServerUtils k)
	{
		knownWizardServerUtils = k;
	}

	/**
	 * @return Methods dealing with combat maps that are only needed on the server
	 */
	public final CombatMapServerUtils getCombatMapServerUtils ()
	{
		return combatMapServerUtils;
	}

	/**
	 * @param u Methods dealing with combat maps that are only needed on the server
	 */
	public final void setCombatMapServerUtils (final CombatMapServerUtils u)
	{
		combatMapServerUtils = u;
	}

	/**
	 * @return Server-side only spell utils
	 */
	public final SpellServerUtils getSpellServerUtils ()
	{
		return spellServerUtils;
	}

	/**
	 * @param utils Server-side only spell utils
	 */
	public final void setSpellServerUtils (final SpellServerUtils utils)
	{
		spellServerUtils = utils;
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