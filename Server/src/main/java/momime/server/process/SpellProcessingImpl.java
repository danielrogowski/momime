package momime.server.process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.CoordinateSystemUtils;
import com.ndg.map.areas.operations.MapAreaOperations2D;
import com.ndg.map.coordinates.MapCoordinates2DEx;
import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.calculations.CityCalculations;
import momime.common.calculations.UnitCalculations;
import momime.common.database.AttackSpellCombatTargetID;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.HeroItem;
import momime.common.database.MapFeatureEx;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.database.SpellValidMapFeatureTarget;
import momime.common.database.SpellValidTileTypeTarget;
import momime.common.database.StoredDamageTypeID;
import momime.common.database.Unit;
import momime.common.database.UnitCanCast;
import momime.common.database.UnitCombatSideID;
import momime.common.database.UnitSkillAndValue;
import momime.common.database.UnitSpellEffect;
import momime.common.messages.MemoryBuilding;
import momime.common.messages.MemoryCombatAreaEffect;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomCombatTile;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.NewTurnMessageCreateArtifact;
import momime.common.messages.NewTurnMessageSpell;
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
import momime.common.messages.servertoclient.ShowSpellAnimationMessage;
import momime.common.messages.servertoclient.UpdateCombatMapMessage;
import momime.common.messages.servertoclient.UpdateWizardStateMessage;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.KindOfSpell;
import momime.common.utils.KindOfSpellUtils;
import momime.common.utils.MemoryBuildingUtils;
import momime.common.utils.MemoryCombatAreaEffectUtils;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.SpellUtils;
import momime.common.utils.TargetSpellResult;
import momime.common.utils.UnitUtils;
import momime.server.MomSessionVariables;
import momime.server.ai.SpellAI;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.calculations.ServerSpellCalculations;
import momime.server.database.ServerDatabaseValues;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.fogofwar.FogOfWarMidTurnMultiChanges;
import momime.server.fogofwar.FogOfWarProcessing;
import momime.server.knowledge.ServerGridCellEx;
import momime.server.mapgenerator.CombatMapArea;
import momime.server.mapgenerator.CombatMapGenerator;
import momime.server.utils.HeroItemServerUtils;
import momime.server.utils.OverlandMapServerUtils;
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
	
	/** Memory CAE utils */
	private MemoryCombatAreaEffectUtils memoryCombatAreaEffectUtils;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;

	/** Unit calculations */
	private UnitCalculations unitCalculations;
	
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
	
	/**
	 * Handles casting an overland spell, i.e. when we've finished channeling sufficient mana in to actually complete the casting
	 *
	 * @param player Player who is casting the spell
	 * @param spell Which spell is being cast
	 * @param variableDamage Chosen damage selected for the spell, for spells like fire bolt where a varying amount of mana can be channeled into the spell
	 * @param heroItem The item being created; null for spells other than Enchant Item or Create Artifact
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void castOverlandNow (final PlayerServerDetails player, final Spell spell, final Integer variableDamage, final HeroItem heroItem, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		// Modifying this by section is really only a safeguard to protect against casting spells which we don't have researched yet
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final MomTransientPlayerPrivateKnowledge trans = (MomTransientPlayerPrivateKnowledge) player.getTransientPlayerPrivateKnowledge ();
		final SpellResearchStatus researchStatus = getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), spell.getSpellID ());
		final SpellBookSectionID sectionID = getSpellUtils ().getModifiedSectionID (spell, researchStatus.getStatus (), true);

		// Overland enchantments
		if (sectionID == SpellBookSectionID.OVERLAND_ENCHANTMENTS)
		{
			// Check if the player already has this overland enchantment cast
			// If they do, they can't have it twice so nothing to do, they just lose the cast
			if (getMemoryMaintainedSpellUtils ().findMaintainedSpell (mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), player.getPlayerDescription ().getPlayerID (), spell.getSpellID (), null, null, null, null) == null)
			{
				// Add it on server and anyone who can see it (which, because its an overland enchantment, will be everyone)
				getFogOfWarMidTurnChanges ().addMaintainedSpellOnServerAndClients
					(mom.getGeneralServerKnowledge (), player.getPlayerDescription ().getPlayerID (), spell.getSpellID (),
						null, null, false, null, null, variableDamage, mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ());

				// Does this overland enchantment give a global combat area effect? (Not all do)
				if (spell.getSpellHasCombatEffect ().size () > 0)
				{
					// Pick one at random
					final String combatAreaEffectID = spell.getSpellHasCombatEffect ().get (getRandomUtils ().nextInt (spell.getSpellHasCombatEffect ().size ()));
					getFogOfWarMidTurnChanges ().addCombatAreaEffectOnServerAndClients (mom.getGeneralServerKnowledge (), combatAreaEffectID, spell.getSpellID (),
						player.getPlayerDescription ().getPlayerID (), spell.getOverlandCastingCost (), null, mom.getPlayers (), mom.getSessionDescription ());
				}
				
				// If it is Detect Magic, the player now learns what spells everyone is casting overland
				if (spell.getSpellID ().equals (CommonDatabaseConstants.SPELL_ID_DETECT_MAGIC))
					getSpellCasting ().sendOverlandCastingInfo (spell.getSpellID (), player, mom.getPlayers ());
			}
		}
		
		// Enchant item / Create artifact
		else if ((sectionID == SpellBookSectionID.SUMMONING) && (heroItem != null))
		{
			// Put new item in mom.getPlayers ()' bank on the server
			final NumberedHeroItem numberedHeroItem = getHeroItemServerUtils ().createNumberedHeroItem (heroItem, mom.getGeneralServerKnowledge ());
			priv.getUnassignedHeroItem ().add (numberedHeroItem);

			// Put new item in mom.getPlayers ()' bank on the client
			if (player.getPlayerDescription ().isHuman ())
			{
				final AddUnassignedHeroItemMessage addItemMsg = new AddUnassignedHeroItemMessage ();
				addItemMsg.setHeroItem (numberedHeroItem);
				player.getConnection ().sendMessageToClient (addItemMsg);
			
				// Show on new turn messages for the player who summoned it
				final NewTurnMessageCreateArtifact createArtifactSpell = new NewTurnMessageCreateArtifact ();
				createArtifactSpell.setMsgType (NewTurnMessageTypeID.CREATE_ARTIFACT);
				createArtifactSpell.setSpellID (spell.getSpellID ());
				createArtifactSpell.setHeroItemName (heroItem.getHeroItemName ());

				trans.getNewTurnMessage ().add (createArtifactSpell);
			}
		}

		// Summoning, except Floating Island where you need to pick a target
		else if ((sectionID == SpellBookSectionID.SUMMONING) && ((spell.isSummonAnywhere () == null) || (!spell.isSummonAnywhere ())))
		{
			// Find the location of the wizards' summoning circle 'building'
			final MemoryBuilding summoningCircleLocation = getMemoryBuildingUtils ().findCityWithBuilding (player.getPlayerDescription ().getPlayerID (),
				CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE, mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ());

			if (summoningCircleLocation != null)
				getSpellCasting ().castOverlandSummoningSpell (spell, player, (MapCoordinates3DEx) summoningCircleLocation.getCityLocation (), mom);
		}

		// Any kind of overland spell that needs the player to choose a target
		else if ((sectionID == SpellBookSectionID.CITY_ENCHANTMENTS) || (sectionID == SpellBookSectionID.UNIT_ENCHANTMENTS) ||
			(sectionID == SpellBookSectionID.CITY_CURSES) || (sectionID == SpellBookSectionID.UNIT_CURSES) || (sectionID == SpellBookSectionID.SPECIAL_UNIT_SPELLS) ||
			(sectionID == SpellBookSectionID.SPECIAL_OVERLAND_SPELLS) || (sectionID == SpellBookSectionID.DISPEL_SPELLS) ||
			(sectionID == SpellBookSectionID.SUMMONING))
		{
			// Add it on server - note we add it without a target chosen and without adding it on any
			// clients - clients don't know about spells until the target has been chosen, since they might hit cancel or have no appropriate target.
			final MemoryMaintainedSpell maintainedSpell = getFogOfWarMidTurnChanges ().addMaintainedSpellOnServerAndClients
				(mom.getGeneralServerKnowledge (), player.getPlayerDescription ().getPlayerID (), spell.getSpellID (),
				null, null, false, null, null, variableDamage, null, mom.getServerDB (), mom.getSessionDescription ());

			if (player.getPlayerDescription ().isHuman ())
			{
				// Tell client to pick a target for this spell
				final NewTurnMessageSpell targetSpell = new NewTurnMessageSpell ();
				targetSpell.setMsgType (NewTurnMessageTypeID.TARGET_SPELL);
				targetSpell.setSpellID (spell.getSpellID ());
				trans.getNewTurnMessage ().add (targetSpell);
			}
			else
				getSpellAI ().decideSpellTarget (player, spell, maintainedSpell, mom);
		}
		
		// Special spells (Spell of Mastery and a few other unique spells that you don't even pick a target for)
		else if (sectionID == SpellBookSectionID.SPECIAL_SPELLS)
		{
			final PlayAnimationMessage msg = new PlayAnimationMessage ();
			msg.setAnimationID (AnimationID.FINISHED_SPELL_OF_MASTERY);
			msg.setPlayerID (player.getPlayerDescription ().getPlayerID ());
			
			getMultiplayerSessionServerUtils ().sendMessageToAllClients (mom.getPlayers (), msg);
			
			// Defeat everyone except the winner
			for (final PlayerServerDetails defeatedPlayer : mom.getPlayers ())
				if (defeatedPlayer != player)
				{
					final MomPersistentPlayerPublicKnowledge defeatedPub = (MomPersistentPlayerPublicKnowledge) defeatedPlayer.getPersistentPlayerPublicKnowledge ();
					if ((PlayerKnowledgeUtils.isWizard (defeatedPub.getWizardID ())) && (defeatedPub.getWizardState () != WizardState.DEFEATED))
					{
						defeatedPub.setWizardState (WizardState.DEFEATED);
						if (defeatedPlayer.getPlayerDescription ().isHuman ())
							mom.updateHumanPlayerToAI (defeatedPlayer.getPlayerDescription ().getPlayerID ());
					}
				}
			
			// Let the remaining player win (this kicks them and ends the session)
			getPlayerMessageProcessing ().checkIfWonGame (mom);
		}

		else
			throw new MomException ("Completed casting an overland spell with a section ID that there is no code to deal with yet: " + sectionID);
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
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether the spell cast was an attack that resulted in the combat ending
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final boolean castCombatNow (final PlayerServerDetails castingPlayer, final ExpandedUnitDetails xuCombatCastingUnit, final Integer combatCastingFixedSpellNumber,
		final Integer combatCastingSlotNumber, final Spell spell, final int reducedCombatCastingCost, final int multipliedManaCost,
		final Integer variableDamage, final MapCoordinates3DEx combatLocation, final PlayerServerDetails defendingPlayer, final PlayerServerDetails attackingPlayer,
		final MemoryUnit targetUnit, final MapCoordinates2DEx targetLocation, final MomSessionVariables mom)
		throws MomException, JAXBException, XMLStreamException, PlayerNotFoundException, RecordNotFoundException
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
		final MomPersistentPlayerPublicKnowledge castingPlayerPub = (MomPersistentPlayerPublicKnowledge) castingPlayer.getPersistentPlayerPublicKnowledge ();
		
		final boolean passesCounteringAttempts;
		if (immuneToCounterMagic)
			passesCounteringAttempts = true;
		else
		{
			final int unmodifiedCombatCastingCost = getSpellUtils ().getUnmodifiedCombatCastingCost (spell, variableDamage, castingPlayerPub.getPick ());
			passesCounteringAttempts = getSpellDispelling ().processCountering
				(castingPlayer, spell, unmodifiedCombatCastingCost, combatLocation, defendingPlayer, attackingPlayer,
					mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());
		}
		
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
					addUnitSpell = getDamageProcessor ().makeResistanceRoll ((xuCombatCastingUnit == null) ? null : xuCombatCastingUnit.getMemoryUnit (),
						targetUnit, attackingPlayer, defendingPlayer, spell, variableDamage, castingPlayer, mom);
				
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
					
					getFogOfWarMidTurnChanges ().addMaintainedSpellOnServerAndClients (mom.getGeneralServerKnowledge (),
						castingPlayer.getPlayerDescription ().getPlayerID (), spell.getSpellID (), targetUnit.getUnitURN (), unitSpellEffect.getUnitSkillID (),
						true, null, null, useVariableDamage, mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ());
				}
				else
				{
					// Even though not adding a spell, still have to tell the client to show animation for the failed spell
					final ShowSpellAnimationMessage anim = new ShowSpellAnimationMessage ();
					anim.setSpellID (spell.getSpellID ());
					anim.setCastInCombat (true);
					anim.setCombatTargetUnitURN (targetUnit.getUnitURN ());

					if (attackingPlayer.getPlayerDescription ().isHuman ())
						attackingPlayer.getConnection ().sendMessageToClient (anim);

					if (defendingPlayer.getPlayerDescription ().isHuman ())
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
				getFogOfWarMidTurnChanges ().addMaintainedSpellOnServerAndClients (mom.getGeneralServerKnowledge (),
					castingPlayer.getPlayerDescription ().getPlayerID (), spell.getSpellID (), null, null,
					true, combatLocation, citySpellEffectID, variableDamage, mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ());
				
				// The new enchantment presumably requires the combat map to be regenerated so we can see it
				// (the only city enchantments/curses that can be cast in combat are Wall of Fire / Wall of Darkness)
				getCombatMapGenerator ().regenerateCombatTileBorders (gc.getCombatMap (), mom.getServerDB (), mom.getGeneralServerKnowledge ().getTrueMap (), combatLocation);
				
				// Send the updated map
				final UpdateCombatMapMessage msg = new UpdateCombatMapMessage ();
				msg.setCombatLocation (combatLocation);
				msg.setCombatTerrain (gc.getCombatMap ());
				
				if (attackingPlayer.getPlayerDescription ().isHuman ())
					attackingPlayer.getConnection ().sendMessageToClient (msg);
	
				if (defendingPlayer.getPlayerDescription ().isHuman ())
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
					gc.getCombatMap ().getRow ().get (targetLocation.getY ()).getCell ().get (targetLocation.getX ()).setWrecked (true);
				}
				else
				{
					// Make an area muddy, the size of the area is set in the radius field
					final CombatMapArea areaBridge = new CombatMapArea ();
					areaBridge.setArea (gc.getCombatMap ());
					areaBridge.setCoordinateSystem (mom.getSessionDescription ().getCombatMapSize ());
					
					getCombatMapOperations ().processCellsWithinRadius (areaBridge, targetLocation.getX (), targetLocation.getY (),
						spell.getSpellRadius (), (tile) ->
					{
						tile.setMud (true);
						return true;
					});
				}
				
				// Show animation for it
				final ShowSpellAnimationMessage anim = new ShowSpellAnimationMessage ();
				anim.setSpellID (spell.getSpellID ());
				anim.setCastInCombat (true);
				anim.setCombatTargetLocation (targetLocation);
	
				if (attackingPlayer.getPlayerDescription ().isHuman ())
					attackingPlayer.getConnection ().sendMessageToClient (anim);
	
				if (defendingPlayer.getPlayerDescription ().isHuman ())
					defendingPlayer.getConnection ().sendMessageToClient (anim);
				
				// Send the updated map
				final UpdateCombatMapMessage msg = new UpdateCombatMapMessage ();
				msg.setCombatLocation (combatLocation);
				msg.setCombatTerrain (gc.getCombatMap ());
				
				if (attackingPlayer.getPlayerDescription ().isHuman ())
					attackingPlayer.getConnection ().sendMessageToClient (msg);
	
				if (defendingPlayer.getPlayerDescription ().isHuman ())
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
					final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (targetUnit, null, null, null,
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
				getFogOfWarMidTurnChanges ().updateUnitStatusToAliveOnServerAndClients (targetUnit, summonLocation, castingPlayer,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getSessionDescription (), mom.getServerDB ());
	
				// Show the "summoning" animation for it
				final int combatHeading = (castingPlayer == attackingPlayer) ? 8 : 4;
				
				final MapCoordinates2DEx actualTargetLocation = getUnitServerUtils ().findFreeCombatPositionAvoidingInvisibleClosestTo
					(combatLocation, gc.getCombatMap (), targetLocation, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (),
						mom.getSessionDescription ().getCombatMapSize (), mom.getServerDB ());
	
				getCombatProcessing ().setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer,
					mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), targetUnit,
					combatLocation, combatLocation, actualTargetLocation, combatHeading, castingSide, spell.getSpellID (), mom.getServerDB ());
	
				// Allow it to be moved this combat turn
				targetUnit.setDoubleCombatMovesLeft (2 * getUnitUtils ().expandUnitDetails (targetUnit, null, null, null,
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ()).getMovementSpeed ());
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
					final MemoryUnit tu = getFogOfWarMidTurnChanges ().addUnitOnServerAndClients (mom.getGeneralServerKnowledge (),
						unitID, summonLocation, null, null, combatLocation, castingPlayer, UnitStatusID.ALIVE,
						mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());
					
					// What direction should the unit face?
					final int combatHeading = (castingPlayer == attackingPlayer) ? 8 : 4;
					
					// Set it immediately into combat
					final MapCoordinates2DEx actualTargetLocation = getUnitServerUtils ().findFreeCombatPositionAvoidingInvisibleClosestTo
						(combatLocation, gc.getCombatMap (), targetLocation, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (),
							mom.getSessionDescription ().getCombatMapSize (), mom.getServerDB ());
					
					getCombatProcessing ().setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer,
						mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), tu,
						combatLocation, combatLocation, actualTargetLocation, combatHeading, castingSide, spell.getSpellID (), mom.getServerDB ());
					
					// Allow it to be moved this combat turn
					tu.setDoubleCombatMovesLeft (2 * getUnitUtils ().expandUnitDetails (tu, null, null, null,
						mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ()).getMovementSpeed ());
					
					// Make sure we remove it after combat
					tu.setWasSummonedInCombat (true);
				}
			}
			
			// Attack, healing or dispelling spells
			else if ((spell.getSpellBookSectionID () == SpellBookSectionID.ATTACK_SPELLS) || (spell.getSpellBookSectionID () == SpellBookSectionID.SPECIAL_UNIT_SPELLS) ||
				(spell.getSpellBookSectionID () == SpellBookSectionID.DISPEL_SPELLS))
			{
				// Does the spell attack a specific unit or ALL enemy units? e.g. Flame Strike or Death Spell
				final List<MemoryUnit> targetUnits = new ArrayList<MemoryUnit> ();
				final List<MemoryMaintainedSpell> targetSpells = new ArrayList<MemoryMaintainedSpell> ();
				final List<MemoryCombatAreaEffect> targetCAEs = new ArrayList<MemoryCombatAreaEffect> ();
				if (spell.getAttackSpellCombatTarget () == AttackSpellCombatTargetID.SINGLE_UNIT)
					targetUnits.add (targetUnit);
				else
				{
					for (final MemoryUnit thisUnit : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
					{
						final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (thisUnit, null, null, spell.getSpellRealm (),
							mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
						
						if (getMemoryMaintainedSpellUtils ().isUnitValidTargetForSpell (spell, null, combatLocation, castingPlayer.getPlayerDescription ().getPlayerID (),
							xuCombatCastingUnit, variableDamage, xu, mom.getGeneralServerKnowledge ().getTrueMap (),
							mom.getServerDB ()) == TargetSpellResult.VALID_TARGET)
							
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
						if ((spell.getSpellValidBorderTarget ().size () > 0) && (getMemoryMaintainedSpellUtils ().isCombatLocationValidTargetForSpell
							(spell, (MapCoordinates2DEx) targetUnit.getCombatPosition (), gc.getCombatMap ())))
						{
							wreckTileChance = 1;
							wreckTilePosition = (MapCoordinates2DEx) targetUnit.getCombatPosition ();
						}
								
						combatEnded = getDamageProcessor ().resolveAttack ((xuCombatCastingUnit == null) ? null : xuCombatCastingUnit.getMemoryUnit (),
							targetUnits, attackingPlayer, defendingPlayer,
							wreckTileChance, wreckTilePosition, null, null, spell, variableDamage, castingPlayer, combatLocation, mom);
					}
					else if (kind == KindOfSpell.HEALING)
					{
						// Healing spells work by sending ApplyDamage - this is basically just updating the client as to the damage taken by a bunch of combat units,
						// and handles showing the animation for us, so its convenient to reuse it for this.  Effectively we're just applying negative damage...
						for (final MemoryUnit tu : targetUnits)
						{
							final int dmg = getUnitUtils ().getHealableDamageTaken (tu.getUnitDamage ());
							final int heal = Math.min (dmg, spell.getCombatBaseDamage ());
							if (heal > 0)
								getUnitServerUtils ().healDamage (tu.getUnitDamage (), heal, false);
						}
						
						getFogOfWarMidTurnChanges ().sendCombatDamageToClients (null, attackingPlayer, defendingPlayer,
							targetUnits, null, spell.getSpellID (), null, null, null, mom.getPlayers (),
							mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting ());
					}
					else if (kind == KindOfSpell.RECALL)
					{
						// Recall spells - first we need the location of the wizards' summoning circle 'building' to know where we're recalling them to
						final MemoryBuilding summoningCircleLocation = getMemoryBuildingUtils ().findCityWithBuilding (castingPlayer.getPlayerDescription ().getPlayerID (),
							CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE, mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
							mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ());
						
						if (summoningCircleLocation != null)
						{
							// Recall spells - first take the unit(s) out of combat
							for (final MemoryUnit tu : targetUnits)
								getCombatProcessing ().setUnitIntoOrTakeUnitOutOfCombat (attackingPlayer, defendingPlayer,
									mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), tu,
									combatLocation, null, null, null, castingSide, spell.getSpellID (), mom.getServerDB ());
							
							// Now teleport it back to our summoning circle
							getFogOfWarMidTurnMultiChanges ().moveUnitStackOneCellOnServerAndClients (targetUnits, castingPlayer, combatLocation,
								(MapCoordinates3DEx) summoningCircleLocation.getCityLocation (),
								mom.getPlayers (), mom.getGeneralServerKnowledge (), mom.getSessionDescription (), mom.getServerDB ());
							
							// If we recalled our last remaining unit(s) out of combat, then we lose
							if (getDamageProcessor ().countUnitsInCombat (combatLocation, castingSide, mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ()) == 0)
								winningPlayer = (castingPlayer == defendingPlayer) ? attackingPlayer : defendingPlayer;
						}
					}
					else
					{
						// Dispel magic or similar - before we start rolling, send animation for it
						final ShowSpellAnimationMessage anim = new ShowSpellAnimationMessage ();
						anim.setSpellID (spell.getSpellID ());
						anim.setCastInCombat (true);
						anim.setCombatTargetLocation (targetLocation);
						
						if ((spell.getAttackSpellCombatTarget () == AttackSpellCombatTargetID.SINGLE_UNIT) && (targetUnits.size () > 0))
							anim.setCombatTargetUnitURN (targetUnits.get (0).getUnitURN ());
	
						if (attackingPlayer.getPlayerDescription ().isHuman ())
							attackingPlayer.getConnection ().sendMessageToClient (anim);
	
						if (defendingPlayer.getPlayerDescription ().isHuman ())
							defendingPlayer.getConnection ().sendMessageToClient (anim);
						
						// Now get a list of all enchantments cast on all the units in the list by other wizards
						final List<Integer> targetUnitURNs = targetUnits.stream ().map (u -> u.getUnitURN ()).collect (Collectors.toList ());
						final List<MemoryMaintainedSpell> spellsToDispel = mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ().stream ().filter
							(s -> (s.getCastingPlayerID () != castingPlayer.getPlayerDescription ().getPlayerID ()) && (targetUnitURNs.contains (s.getUnitURN ()))).collect (Collectors.toList ());
						spellsToDispel.addAll (targetSpells);
						
						// Common method does the rest
						if (getSpellDispelling ().processDispelling (spell, variableDamage, castingPlayer, spellsToDispel, targetCAEs, mom))
							
							// Its possible we dispelled Lionheart on the last enemy unit thereby winning the combat, so check to be sure
							if (getDamageProcessor ().countUnitsInCombat (combatLocation,
								(castingSide == UnitCombatSideID.ATTACKER) ? UnitCombatSideID.DEFENDER : UnitCombatSideID.ATTACKER,
								mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ()) == 0)
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
				if (gc.getCombatDefenderCastingSkillRemaining () != null)
				{
					gc.setCombatDefenderCastingSkillRemaining (gc.getCombatDefenderCastingSkillRemaining () - reducedCombatCastingCost);
					sendSkillValue = gc.getCombatDefenderCastingSkillRemaining ();
				}
			}
			else if (castingPlayer == attackingPlayer)
			{
				if (gc.getCombatAttackerCastingSkillRemaining () != null)
				{
					gc.setCombatAttackerCastingSkillRemaining (gc.getCombatAttackerCastingSkillRemaining () - reducedCombatCastingCost);
					sendSkillValue = gc.getCombatAttackerCastingSkillRemaining ();
				}
			}
			else
				throw new MomException ("Trying to charge combat casting cost to kill but the caster appears to be neither attacker nor defender");
			
			// Send both values to client
			if (sendSkillValue != null)
				getServerResourceCalculations ().sendGlobalProductionValues (castingPlayer, sendSkillValue, true);
			
			// Only allow casting one spell each combat turn
			gc.setSpellCastThisCombatTurn (true);
		}
		else if (combatCastingFixedSpellNumber != null)
		{
			// Casting a fixed spell that's part of the unit definition
			xuCombatCastingUnit.setDoubleCombatMovesLeft (0);

			xuCombatCastingUnit.getMemoryUnit ().getFixedSpellsRemaining ().set (combatCastingFixedSpellNumber,
				xuCombatCastingUnit.getMemoryUnit ().getFixedSpellsRemaining ().get (combatCastingFixedSpellNumber) - 1);

			getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (xuCombatCastingUnit.getMemoryUnit (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
				mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting (), null);
		}
		else if (combatCastingSlotNumber != null)
		{
			// Casting a spell imbued into a hero item
			xuCombatCastingUnit.setDoubleCombatMovesLeft (0);

			xuCombatCastingUnit.getMemoryUnit ().getHeroItemSpellChargesRemaining ().set (combatCastingSlotNumber,
				xuCombatCastingUnit.getMemoryUnit ().getHeroItemSpellChargesRemaining ().get (combatCastingSlotNumber) - 1);

			getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (xuCombatCastingUnit.getMemoryUnit (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
				mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting (), null);
		}
		else
		{
			// Unit or hero casting - so charge them the mana cost and zero their movement
			xuCombatCastingUnit.setManaRemaining (xuCombatCastingUnit.getManaRemaining () - multipliedManaCost);
			xuCombatCastingUnit.setDoubleCombatMovesLeft (0);
			
			getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (xuCombatCastingUnit.getMemoryUnit (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
				mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting (), null);
		}
		
		// Did casting the spell result in winning/losing the combat?
		if (winningPlayer != null)
		{
			getCombatStartAndEnd ().combatEnded (combatLocation, attackingPlayer, defendingPlayer, winningPlayer, null, mom);
			combatEnded = true;
		}

		return combatEnded;
	}
	
	/**
	 * The method in the FOW class physically removed spells from the server and players' memory; this method
	 * deals with all the knock on effects of spells being switched off, which isn't really much since spells don't grant money or anything when sold
	 * so this is mostly here for consistency with the building and unit methods
	 *
	 * Does not recalc global production (which will now be reduced from not having to pay the maintenance of the cancelled spell),
	 * this has to be done by the calling routine
	 * 
	 * NB. Delphi method was called OkToSwitchOffMaintainedSpell
	 *
	 * @param spellURN Which spell it is
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether switching off the spell resulted in the death of the unit it was cast on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final boolean switchOffSpell (final int spellURN, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException
	{
		// First find the spell
		final MemoryMaintainedSpell trueSpell = getMemoryMaintainedSpellUtils ().findSpellURN
			(spellURN, mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), "switchOffSpell");
		
		// Any secondary effects we also need to switch off?
		final PlayerServerDetails player = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), trueSpell.getCastingPlayerID (), "switchOffSpell");
		final Spell spell = mom.getServerDB ().findSpell (trueSpell.getSpellID (), "switchOffSpell");
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		final SpellResearchStatus researchStatus = getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), trueSpell.getSpellID ());
		final SpellBookSectionID sectionID = getSpellUtils ().getModifiedSectionID (spell, researchStatus.getStatus (), true);

		// Overland enchantments
		if (sectionID == SpellBookSectionID.OVERLAND_ENCHANTMENTS)
		{
			// Check each combat area effect that this overland enchantment gives to see if we have any of them in effect - if so cancel them
			for (final String combatAreaEffectID: spell.getSpellHasCombatEffect ())
			{
				final MemoryCombatAreaEffect cae = getMemoryCombatAreaEffectUtils ().findCombatAreaEffect
					(mom.getGeneralServerKnowledge ().getTrueMap ().getCombatAreaEffect (), null, combatAreaEffectID, trueSpell.getCastingPlayerID ());
				
				if (cae != null)
					getFogOfWarMidTurnChanges ().removeCombatAreaEffectFromServerAndClients
						(mom.getGeneralServerKnowledge ().getTrueMap (), cae.getCombatAreaEffectURN (), mom.getPlayers (), mom.getSessionDescription ());
			}
		}

		// Remove spell itself
		return getFogOfWarMidTurnChanges ().switchOffMaintainedSpellOnServerAndClients (mom.getGeneralServerKnowledge ().getTrueMap (),
			trueSpell.getSpellURN (), mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ());
	}
	
	/**
	 * Overland spells are cast first (probably taking several turns) and a target is only chosen after casting is completed.
	 * So this actually processes the actions from the spell once its target is chosen.
	 * This assumes all necessary validation has been done to verify that the action is allowed.
	 * 
	 * @param spell Definition of spell being targetted
	 * @param maintainedSpell Spell being targetted in server's true memory - at the time this is called, this is the only copy of the spell that exists
	 * 	as we can only determine which clients can "see" it once a target location has been chosen.  Even the player who cast it doesn't have a
	 *		record of it, just a special entry on their new turn messages scroll telling them to pick a target for it.
	 * @param targetLocation If the spell is targetted at a city or a map location, then sets that location; null for spells targetted on other things
	 * @param targetUnit If the spell is targetted at a unit, then the true unit to aim at; null for spells targetted on other things
	 * @param targetSpell If the spell is targetted at another spell, then the true spell to aim at; null for spells targetted on other things
	 * @param citySpellEffectID If spell creates a city spell effect, then which one - currently chosen at random, but supposed to be player choosable for Spell Ward
	 * @param unitSkillID If spell creates a unit skill, then which one - chosen at random for Chaos Channels
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void targetOverlandSpell (final Spell spell, final MemoryMaintainedSpell maintainedSpell,
		final MapCoordinates3DEx targetLocation, final MemoryUnit targetUnit, final MemoryMaintainedSpell targetSpell,
		final String citySpellEffectID, final String unitSkillID, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException, MomException
	{
		final PlayerServerDetails castingPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), maintainedSpell.getCastingPlayerID (), "targetOverlandSpell");
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) castingPlayer.getPersistentPlayerPrivateKnowledge ();
		final KindOfSpell kind = getKindOfSpellUtils ().determineKindOfSpell (spell, null);
		
		if ((spell.getSpellBookSectionID () == SpellBookSectionID.SPECIAL_UNIT_SPELLS) || (spell.getSpellBookSectionID () == SpellBookSectionID.SPECIAL_OVERLAND_SPELLS) ||
			(spell.getSpellBookSectionID () == SpellBookSectionID.DISPEL_SPELLS) || (spell.getSpellBookSectionID () == SpellBookSectionID.SUMMONING))
		{
			// Transient spell that performs some immediate action, then the temporary untargetted spell on the server gets removed
			// So the spell never does get added to any clients
			// Set values on server - it'll be removed below, but we need to set these to make the visibility checks in sendTransientSpellToClients () work correctly
			
			if (targetUnit != null)
				maintainedSpell.setUnitURN (targetUnit.getUnitURN ());
			
			maintainedSpell.setCityLocation (targetLocation);
			maintainedSpell.setUnitSkillID (unitSkillID);
			maintainedSpell.setCitySpellEffectID (citySpellEffectID);
			
			// Just remove it - don't even bother to check if any clients can see it
			getMemoryMaintainedSpellUtils ().removeSpellURN (maintainedSpell.getSpellURN (), mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ());
			
			// Tell the client to stop asking about targetting the spell, and show an animation for it - need to send this to all players that can see it!
			getFogOfWarMidTurnChanges ().sendTransientSpellToClients (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
				mom.getGeneralServerKnowledge ().getTrueMap ().getUnit (), maintainedSpell, mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting ());

			if (kind == KindOfSpell.RECALL)
			{
				// Recall spells - first we need the location of the wizards' summoning circle 'building' to know where we're recalling them to
				final MemoryBuilding summoningCircleLocation = getMemoryBuildingUtils ().findCityWithBuilding (castingPlayer.getPlayerDescription ().getPlayerID (),
					CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE, mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
					mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ());
				
				if (summoningCircleLocation != null)
				{
					final List<MemoryUnit> targetUnits = new ArrayList<MemoryUnit> ();
					targetUnits.add (targetUnit);
					
					getFogOfWarMidTurnMultiChanges ().moveUnitStackOneCellOnServerAndClients (targetUnits, castingPlayer, (MapCoordinates3DEx) targetUnit.getUnitLocation (),
						(MapCoordinates3DEx) summoningCircleLocation.getCityLocation (),
						mom.getPlayers (), mom.getGeneralServerKnowledge (), mom.getSessionDescription (), mom.getServerDB ());
				}
			}
			
			else if (kind == KindOfSpell.HEALING)
			{
				// Nature's Cures - heal every unit at the location
				for (final MemoryUnit tu : mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ())
					if ((targetLocation.equals (tu.getUnitLocation ())) && (tu.getStatus () == UnitStatusID.ALIVE) &&
						(tu.getOwningPlayerID () == maintainedSpell.getCastingPlayerID ()))
					{
						final ExpandedUnitDetails thisTarget = getUnitUtils ().expandUnitDetails (tu, null, null, null,
							mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
						
						if (getMemoryMaintainedSpellUtils ().isUnitValidTargetForSpell (spell, null, null,
							maintainedSpell.getCastingPlayerID (), null, null, thisTarget, mom.getGeneralServerKnowledge ().getTrueMap (),
							mom.getServerDB ()) == TargetSpellResult.VALID_TARGET)
						{
							// Heal this unit
							final int dmg = getUnitUtils ().getHealableDamageTaken (tu.getUnitDamage ());
							getUnitServerUtils ().healDamage (tu.getUnitDamage (), dmg, false);
							
							getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (tu, mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
								mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting (), null);
						}
					}
			}
			
			else if (spell.getSpellBookSectionID () == SpellBookSectionID.DISPEL_SPELLS)
			{
				final List<MemoryMaintainedSpell> targetSpells;
				if (spell.getAttackSpellCombatTarget () == null)
				{
					// Disjunction is easy - just targetted at one single spell, and we've already got it
					targetSpells = Arrays.asList (targetSpell);
				}
				else
				{
					// Disenchant Area / True - get a list of units at the location (ours as well)
		    		final List<Integer> unitURNs = mom.getGeneralServerKnowledge ().getTrueMap ().getUnit ().stream ().filter
		    			(u -> targetLocation.equals (u.getUnitLocation ())).map (u -> u.getUnitURN ()).collect (Collectors.toList ());
		    		
		    		// Now look for any spells cast by somebody else either targetted directly on the location, or on a unit at the location
		    		targetSpells = mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ().stream ().filter
		    			(s -> (s.getCastingPlayerID () != castingPlayer.getPlayerDescription ().getPlayerID ()) &&
		    				((targetLocation.equals (s.getCityLocation ())) || (unitURNs.contains (s.getUnitURN ())))).collect (Collectors.toList ());
				}
				
	    		getSpellDispelling ().processDispelling (spell, maintainedSpell.getVariableDamage (), castingPlayer, targetSpells, null, mom);
			}

			// The only targetted overland summoning spell is Floating Island
			else if (spell.getSpellBookSectionID () == SpellBookSectionID.SUMMONING)
				getSpellCasting ().castOverlandSummoningSpell (spell, castingPlayer, targetLocation, mom);
			
			else if (kind == KindOfSpell.CORRUPTION)
			{
				// Corruption
				final OverlandMapTerrainData terrainData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
					(targetLocation.getZ ()).getRow ().get (targetLocation.getY ()).getCell ().get (targetLocation.getX ()).getTerrainData ();
				terrainData.setCorrupted (5);
				
				getFogOfWarMidTurnChanges ().updatePlayerMemoryOfTerrain (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
					mom.getPlayers (), targetLocation, mom.getSessionDescription ().getFogOfWarSetting ().getTerrainAndNodeAuras ());
				
				getCityProcessing ().recheckCurrentConstructionIsStillValid (targetLocation,
					mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());
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
				
				getFogOfWarProcessing ().updateAndSendFogOfWar (mom.getGeneralServerKnowledge ().getTrueMap (), castingPlayer, mom.getPlayers (),
					"earthLore", mom.getSessionDescription (), mom.getServerDB ());
			}
			
			else if (kind == KindOfSpell.CHANGE_TILE_TYPE)
			{
				// Change Terrain or Raise Volcano
				final OverlandMapTerrainData terrainData = mom.getGeneralServerKnowledge ().getTrueMap ().getMap ().getPlane ().get
					(targetLocation.getZ ()).getRow ().get (targetLocation.getY ()).getCell ().get (targetLocation.getX ()).getTerrainData ();
				
				final Iterator<SpellValidTileTypeTarget> iter = spell.getSpellValidTileTypeTarget ().iterator ();				
				boolean found = false;
				while ((!found) && (iter.hasNext ()))
				{
					final SpellValidTileTypeTarget thisTileType = iter.next ();
					if (thisTileType.getTileTypeID ().equals (terrainData.getTileTypeID ()))
					{
						if (thisTileType.getChangeToTileTypeID () == null)
							throw new MomException ("Spell " + spell.getSpellID () + " is a change terrain type spell but has no tile type defined to change from " + thisTileType.getTileTypeID ());
						
						terrainData.setTileTypeID (thisTileType.getChangeToTileTypeID ());
						
						if (thisTileType.getChangeToTileTypeID ().equals (CommonDatabaseConstants.TILE_TYPE_RAISE_VOLCANO))
							terrainData.setVolcanoOwnerID (castingPlayer.getPlayerDescription ().getPlayerID ());
						else
							terrainData.setVolcanoOwnerID (null);
						
						if ((thisTileType.isMineralDestroyed () != null) && (thisTileType.isMineralDestroyed ()) && (terrainData.getMapFeatureID () != null))
						{
							// Minerals are destroyed, but not lairs
							final MapFeatureEx mapFeature = mom.getServerDB ().findMapFeature (terrainData.getMapFeatureID (), "targetOverlandSpell");
							if (mapFeature.getMapFeatureMagicRealm ().size () == 0)
								terrainData.setMapFeatureID (null);
						}
						
						if (thisTileType.getBuildingsDestroyedChance () != null)
						{
							// Every building here has a chance of being destroyed
							final List<MemoryBuilding> destroyedBuildings = new ArrayList<MemoryBuilding> ();
							for (final MemoryBuilding thisBuilding : mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ())
								if ((thisBuilding.getCityLocation ().equals (targetLocation)) &&
									(!thisBuilding.getBuildingID ().equals (CommonDatabaseConstants.BUILDING_FORTRESS)) &&
									(!thisBuilding.getBuildingID ().equals (CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE)) &&
									(getRandomUtils ().nextInt (100) < thisTileType.getBuildingsDestroyedChance ()))
									
									destroyedBuildings.add (thisBuilding);
							
							if (destroyedBuildings.size () > 0)
								getCityProcessing ().destroyBuildings (mom.getGeneralServerKnowledge ().getTrueMap (),
									mom.getPlayers (), destroyedBuildings, spell.getSpellID (), castingPlayer.getPlayerDescription ().getPlayerID (),
									mom.getSessionDescription (), mom.getServerDB ());
						}
						
						found = true;
					}
				}
				
				if (found)
				{
					getFogOfWarMidTurnChanges ().updatePlayerMemoryOfTerrain (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
						mom.getPlayers (), targetLocation, mom.getSessionDescription ().getFogOfWarSetting ().getTerrainAndNodeAuras ());

					getCityProcessing ().recheckCurrentConstructionIsStillValid (targetLocation,
						mom.getGeneralServerKnowledge ().getTrueMap (), mom.getPlayers (), mom.getSessionDescription (), mom.getServerDB ());
				}
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
					getFogOfWarMidTurnChanges ().updatePlayerMemoryOfTerrain (mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
						mom.getPlayers (), targetLocation, mom.getSessionDescription ().getFogOfWarSetting ().getTerrainAndNodeAuras ());
			}
		}

		else if (spell.getBuildingID () == null)
		{
			// Enchantment or curse spell that generates some city or unit effect
			// Set values on server
			if (targetUnit != null)
				maintainedSpell.setUnitURN (targetUnit.getUnitURN ());
			
			maintainedSpell.setCityLocation (targetLocation);
			maintainedSpell.setUnitSkillID (unitSkillID);
			maintainedSpell.setCitySpellEffectID (citySpellEffectID);
			
			// Add spell on clients (they don't have a blank version of it before now)
			getFogOfWarMidTurnChanges ().addExistingTrueMaintainedSpellToClients (mom.getGeneralServerKnowledge (), maintainedSpell,
				mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ());
			
			// If its a unit enchantment, does it grant any secondary permanent effects? (Black Channels making units Undead)
			if (spell.getSpellBookSectionID () == SpellBookSectionID.UNIT_ENCHANTMENTS)
			{
				final ExpandedUnitDetails xu = getUnitUtils ().expandUnitDetails (targetUnit, null, null, spell.getSpellRealm (),
					mom.getPlayers (), mom.getGeneralServerKnowledge ().getTrueMap (), mom.getServerDB ());
				
				for (final UnitSpellEffect effect : spell.getUnitSpellEffect ())
					if ((effect.isPermanent () != null) && (effect.isPermanent ()) && (!xu.hasBasicSkill (effect.getUnitSkillID ())))
					{
						final UnitSkillAndValue permanentEffect = new UnitSkillAndValue ();
						permanentEffect.setUnitSkillID (effect.getUnitSkillID ());
						targetUnit.getUnitHasSkill ().add (permanentEffect);
						
						getFogOfWarMidTurnChanges ().updatePlayerMemoryOfUnit (targetUnit, mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
							mom.getPlayers (), mom.getServerDB (), mom.getSessionDescription ().getFogOfWarSetting (), null);
					}
			}
		}
		else
		{
			// Spell that creates a building instead of an effect, like "Wall of Stone" or "Move Fortress"
			// Is it a type of building where we only ever have one of them, and need to remove the existing one?
			String secondBuildingID = null;
			if ((spell.getBuildingID ().equals (CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE)) ||
				(spell.getBuildingID ().equals (CommonDatabaseConstants.BUILDING_FORTRESS)))
			{
				// Find & remove the main building for this spell
				final MemoryBuilding destroyBuildingLocation = getMemoryBuildingUtils ().findCityWithBuilding
					(castingPlayer.getPlayerDescription ().getPlayerID (), spell.getBuildingID (), mom.getGeneralServerKnowledge ().getTrueMap ().getMap (),
						mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ());
				
				if (destroyBuildingLocation != null)
					getFogOfWarMidTurnChanges ().destroyBuildingOnServerAndClients (mom.getGeneralServerKnowledge ().getTrueMap (),
						mom.getPlayers (), destroyBuildingLocation.getBuildingURN (), false, mom.getSessionDescription (), mom.getServerDB ());
					
				// Move summoning circle as well if its in the same place as the wizard's fortress
				if (spell.getBuildingID ().equals (CommonDatabaseConstants.BUILDING_FORTRESS))
				{
					final MemoryBuilding summoningCircleLocation = getMemoryBuildingUtils ().findCityWithBuilding
						(castingPlayer.getPlayerDescription ().getPlayerID (), CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE,
							mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ());
						
					if ((summoningCircleLocation != null) && (summoningCircleLocation.equals (destroyBuildingLocation)))
						getFogOfWarMidTurnChanges ().destroyBuildingOnServerAndClients (mom.getGeneralServerKnowledge ().getTrueMap (),
							mom.getPlayers (), summoningCircleLocation.getBuildingURN (),
							false, mom.getSessionDescription (), mom.getServerDB ());

					// Place a summoning circle as well if we just destroyed it OR if we never had one in the first place (Spell of Return)
					if ((summoningCircleLocation == null) ||
						((summoningCircleLocation != null) && (summoningCircleLocation.equals (destroyBuildingLocation))))
						
						secondBuildingID = CommonDatabaseConstants.BUILDING_SUMMONING_CIRCLE;
				}
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
				final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) castingPlayer.getPersistentPlayerPublicKnowledge ();
				pub.setWizardState (WizardState.ACTIVE);
				
				// Update wizardState on client, and this triggers showing the returning animation as well
				final UpdateWizardStateMessage msg = new UpdateWizardStateMessage ();
				msg.setBanishedPlayerID (castingPlayer.getPlayerDescription ().getPlayerID ());
				msg.setWizardState (WizardState.ACTIVE);
				msg.setRenderCityData (getCityCalculations ().buildRenderCityData (targetLocation,
					mom.getSessionDescription ().getOverlandMapSize (), mom.getGeneralServerKnowledge ().getTrueMap ()));
				getMultiplayerSessionServerUtils ().sendMessageToAllClients (mom.getPlayers (), msg);
			}

			// First create the building(s) on the server
			getFogOfWarMidTurnChanges ().addBuildingOnServerAndClients (mom.getGeneralServerKnowledge (),
				mom.getPlayers (), targetLocation, spell.getBuildingID (), secondBuildingID, spell.getSpellID (), castingPlayer.getPlayerDescription ().getPlayerID (),
				mom.getSessionDescription (), mom.getServerDB ());
			
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
	 * @param maintainedSpell Spell being targetted in server's true memory - at the time this is called, this is the only copy of the spell that exists,
	 * 	so its the only thing we need to clean up
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If we encounter any elements that cannot be found in the DB
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final void cancelTargetOverlandSpell (final MemoryMaintainedSpell maintainedSpell, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
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
		if ((stolenSpellIDs.size () > 0) && (giveTo.getPlayerDescription ().isHuman ()))
		{
			final FullSpellListMessage spellsMsg = new FullSpellListMessage ();
			spellsMsg.getSpellResearchStatus ().addAll (giveToPriv.getSpellResearchStatus ());
			giveTo.getConnection ().sendMessageToClient (spellsMsg);
		}
		
		return stolenSpellIDs;		
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
	 * @return Unit calculations
	 */
	public final UnitCalculations getUnitCalculations ()
	{
		return unitCalculations;
	}

	/**
	 * @param calc Unit calculations
	 */
	public final void setUnitCalculations (final UnitCalculations calc)
	{
		unitCalculations = calc;
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
}