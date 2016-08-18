package momime.server.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.calculations.HeroItemCalculations;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.PickAndQuantity;
import momime.common.database.PickExclusiveFrom;
import momime.common.database.PickPrerequisite;
import momime.common.database.ProductionTypeAndUndoubledValue;
import momime.common.database.RecordNotFoundException;
import momime.common.database.SummonedUnit;
import momime.common.database.UnitSkillComponent;
import momime.common.database.UnitSkillPositiveNegative;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.NumberedHeroItem;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.common.messages.UnitStatusID;
import momime.common.messages.servertoclient.AddUnassignedHeroItemMessage;
import momime.common.messages.servertoclient.FullSpellListMessage;
import momime.common.messages.servertoclient.ReplacePicksMessage;
import momime.common.messages.servertoclient.TreasureRewardMessage;
import momime.common.messages.servertoclient.TreasureRewardPrisoner;
import momime.common.utils.HeroItemUtils;
import momime.common.utils.PlayerPickUtils;
import momime.common.utils.ResourceValueUtils;
import momime.common.utils.SpellUtils;
import momime.common.utils.UnitSkillUtils;
import momime.server.calculations.ServerResourceCalculations;
import momime.server.calculations.ServerSpellCalculations;
import momime.server.database.MapFeatureSvr;
import momime.server.database.PickFreeSpellSvr;
import momime.server.database.PickSvr;
import momime.server.database.PickTypeSvr;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.SpellRankSvr;
import momime.server.database.SpellSvr;
import momime.server.database.TileTypeSvr;
import momime.server.database.UnitSvr;
import momime.server.database.v0_9_7.MapFeatureTreasureBookReward;
import momime.server.fogofwar.FogOfWarMidTurnChanges;
import momime.server.messages.v0_9_7.MomGeneralServerKnowledge;

/**
 * Methods dealing with choosing treasure to reward to a player for capturing a lair/node/tower
 */
public final class TreasureUtilsImpl implements TreasureUtils
{
	/** Class logger */
	private final Log log = LogFactory.getLog (TreasureUtilsImpl.class);
	
	/** Minimum award of gold/mana; it is assumed that this is the cheapest possible reward */
	private final static int MINIMUM_RESOURCE_REWARD = 50;
	
	/** Maximum award of gold/mana per iteration; NB. since gold/mana can be awarded multiple times, the actual amount awarded may be higher */
	private final static int MAXIMUM_RESOURCE_REWARD = 200;
	
	/** Cost to the treasure budget of being awarded a special/pick */
	private final static int SPECIAL_REWARD_COST = 2000;

	/** Cost to the treasure budget of being awarded a prisoner hero */
	private final static int PRISONER_REWARD_COST = 1000;
	
	/** Hero item utils */
	private HeroItemUtils heroItemUtils;
	
	/** Hero item calculations */
	private HeroItemCalculations heroItemCalculations;
	
	/** Spell utils */
	private SpellUtils spellUtils;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Unit skill utils */
	private UnitSkillUtils unitSkillUtils;
	
	/** Server-only unit utils */
	private UnitServerUtils unitServerUtils;
	
	/** Methods for updating true map + players' memory */
	private FogOfWarMidTurnChanges fogOfWarMidTurnChanges;
	
	/** Player pick utils */
	private PlayerPickUtils playerPickUtils;
	
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
	/** Server-only spell calculations */
	private ServerSpellCalculations serverSpellCalculations;

	/** Resource calculations */
	private ServerResourceCalculations serverResourceCalculations;
	
	/**
	 * The treasure reward process detailed in the strategy guide (Appendix C) and on the MoM Wiki is awkward to implement, and in
	 * many situations doesn't make much sense to me.  Especially that
	 * a) The design seems based on that each possible reward (e.g. items, spells, coins) is graded from a minimum to maximum reward,
	 *		so e.g. you qualify for coins as long as you have 50 treasure points left to claim.  But then the number of coins actually received
	 *		depends on how much more than 50 treasure points you have left to claim.  That sounds great, but then in many cases the actual rules don't adhere to that.
	 *		The worst example are hero items, where the number of points charged has no relation to which item you get.
	 *		I want to fix this, so the number of treasure points you get charged exactly matches the quality of the reward that you received.
	 * b) Following on from (a), the cap that hero items over 3,000 points aren't available seems unnecessary (though that seems just what the strategy guide
	 *		says and not what was actually in MoM 1.31).  It makes sense to me that if you capture a small lair, you get a small hero item;
	 *		if you capture a powerful node, you might get a powerful hero item, and get charged accordingly.
	 * c) Complications around spells.  What if the only spell you don't have is a very rare.  In theory, by the original treasure rules, you can
	 *		get that spell from a lair with only 200 treasure points, by rolling "uncommon spell" 4 times and adding them together.  So that means
	 *		spells have to be in the list as a possible reward.  But what if you don't get it 4 times?  Say you get 1 "uncommon spell" and
	 *		3 gold or mana rewards.  It certainly shouldn't grant a very rare spell just because you didn't have an uncommon spell that you could earn.
	 *		i.e. you have to fully determine all treasure rewards before you can see if the awarded spell rank can actually be claimed or not, and
	 *		if not then it just gets lost.  I want to have a model where things get awarded as we go along, and never get "put back" or later
	 *		determined that we can't claim them. 
	 * d) Limits which make no sense why they're there - limit of 3 hero items; limit of 1 spell; limit of 2 specials; that gaining a special
	 *		(pick) reward immediately loses all other rewards.  If you capture a node worth 19,000 treasure points, why shouldn't you actually get
	 *		19,000 worth of reward just because of artificial limits?
	 *
	 * So the rules written here work as follows:
	 * 1) Same process where we keep going as long as we have 50 pts or more of treasure to reward.
	 * 2) The chances of receiving each reward are the same, but any item that we cannot get doesn't get included in the list of possible rewards, so:
	 * 2a) Hero items have 5 chances of being picked, but will only be included if there is at least one unallocated hero item that is within our remaining treasure budget
	 *		that we meet the prerequisites for, e.g. if the item requires 4 life books to get.
	 * 2b) Spells have 3 chances of being picked, but will only be included if there is a spell rank (uncommon, common, rare, very rare) within
	 *		our remaining treasure budget AND where we have at least one spell of that rank that we could obtain (i.e. we don't know it yet, but meet minimum book
	 *		requirements of 1 for uncommon/common, 2 for rare, 3 for very rare).
	 * 2c) Gold (2 chances) and Mana (2 chances) are always in the list of possible rewards.
	 * 2d) Specials/picks (2 chances) are included as a possible reward when the remaining treasure budget is 3,000 or more. 
	 * 2e) A prisoner (1 chance) is in the list of possible rewards as long as our wizard has at least one hero available (i.e. one we haven't had in our
	 *		army before, whether or not they got subsequently killed; but heroes we wilfilly dismissed, or were dimissed because we couldn't feed/pay them may come back)
	 *		and we have at least 1,000 points remaining of treasure reward budget.
	 * 3) Roll random reward, and award/charge points for it as follows:
	 * 3a) If we get a hero item, randomly pick one that i) we meet any preqrequisites for, e.g. if the item requires 4 life books to get it; and ii) is within our
	 *		remaining treasure reward.  There is no cap that items over 2,000 cost cannot be obtained.  The amount deduced from our treasure budget is
	 *		the cost of the item we received.  There is no limit that only 3 items may be obtained.  (That 2,000 is TBC and might need adjusting).
	 * 3b) If a spell is awarded, choose a random obtaininable spell from that spell rank.  Points are charged as per that spell rank.
	 *		Ranks do not get combined together - you can't get a very rare spell cheap by rolling 2 uncommon spells and combining them.  There is no limit of only 1 spell reward.
	 * 3c) Random amount of gold or mana is awarded, in units of 10, from a minimum of 50 up to a maximum of 200, or the remaining treasure reward
	 *		budget, whichever is lower.  Whatever the amount of gold/mana is awarded, the same amount is deducted from the remaining treasure reward budget. 
	 * 3d) Each Special/pick reward chosen deducts 2,000 from the remaining treasure budget.  Which actual special/pick is awarded is only worked out at the very end;
	 *		this is necessary to award picks such as Warlord - we have to complete the whole treasure allocation process so we can see whether 2 picks got awarded and
	 *		so this (and similar) retorts are available as possible rewards.  So for now all we do is keep count of how many were awarded.
	 *		There is no cap of 2 special/pick rewards.  There is no rule that when you get a special/pick reward, all other treasure is discarded.
	 * 3e) Heroes are picked randomly and deduct 1,000 from our treasure reward budget.
	 * 4) Repeat back to step 1 until all treasure is allocated.
	 * 5) The rule that Towers of Wizardry always pick a spell as their first reward, and at a 100 point discount, still applies as long as we have a suitable spell we can obtain.
	 * 6) If any special/picks were rewarded:
	 * 6a) If we have any possible books we may obtain, subject to the possible books that can be awarded from what was captured (e.g. Fallen Temples
	 *		always award Life books, and Nodes always award books of their colour), then they have 75% chance of being awarded;
	 * 6b) If we have any possible retorts we may obtain, then they have 25% chance of being awarded (the only limitations being that: Myrran can never be obtained,
	 *		Divine Power requires 1 life book to obtain, and Infernal Power requires 1 death book to obtain);
	 * 6c) If we may obtain only one or the other, then it is automatic;
	 * 6d) If we may obtain neither, then one "artifact" is awarded, costing all remaining special/pick rewards.  There is no cap on the cost of the awarded item,
	 *		but we must still meet the prerequisites for it, e.g. if the item requires 4 life books to get.
	 * 6e) Books cost 1 special/pick reward if we get one; retorts cost 1 or 2 depending on which retort was awarded.
	 * 7) Repeat back to step 6 until all special/pick rewards are decided.
	 * 
	 * NB. This routine will grant whatever bonuses it awards, i.e. it will add books/retorts to the player's picks, it will mark spells as available,
	 * it will add hero items to their bank and gold/mana to their resources.  But it doesn't generate messages to inform the player of that.
	 * It makes things too complicated if we *don't* grant the bonuses here, because then on the next loop we'd have to be considering
	 * treasure already awarded.  e.g. we get a life book first, then another special - when consdiering whether we can get a death book or
	 * Divine Power, we'd have to consider both the picks the player already has, plus picks already awarded.  So just awarding them
	 * immediately simplies this.
	 * 
	 * The exception to this is that prisoners are added via the regular FOW routines and so the client does get notified of the unit update.
	 * 
	 * The other thing it will not do is, should the rewards include any additional spell books, it won't go through the process of marking
	 * which additional spells the wizard now has available to research, or available to win from further treasure rewards.  If a spell is rewarded that
	 * was previously "researchable now", it will also not pick which new spell becomes "researchable now" to get us back to 8 choices.
	 * Since special/pick rewards are allocated last, we don't have the complication to worry about whereby we might e.g. get a life book
	 * as a first pick, and then as a second pick get a life spell that is only available to us because of the book we just got.
	 *
	 * @param treasureValue Amount of treasure to award
	 * @param player Player who captured the lair/node/tower
	 * @param lairNodeTowerLocation The location of where the lair/node/tower was
	 * @param tileTypeID The tile type that the lair/node/tower was, before it was possibly altered/removed by capturing it
	 * @param mapFeatureID The map feature that the lair/node/tower was, before it was possibly altered/removed by capturing it (will be null for nodes/towers)
	 * @param players List of players in this session
	 * @param gsk Server knowledge structure
	 * @param sd Session description
	 * @param db Lookup lists built over the XML database
	 * @return Details of all rewards given (pre-built message ready to send back to client)
	 * @throws MomException If there is a problem with any of the calculations
	 * @throws RecordNotFoundException If we encounter a something that we can't find in the XML data
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 */
	@Override
	public final TreasureRewardMessage rollTreasureReward (final int treasureValue, final PlayerServerDetails player,
		final MapCoordinates3DEx lairNodeTowerLocation, final String tileTypeID, final String mapFeatureID,
		final List<PlayerServerDetails> players, final MomGeneralServerKnowledge gsk, final MomSessionDescription sd, final ServerDatabaseEx db)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		log.trace ("Entering rollTreasureReward: Value " + treasureValue + " for player " + player.getPlayerDescription ().getPlayerID ());
		
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		final SpellSvr summonHero = db.findSpell (CommonDatabaseConstants.SPELL_ID_SUMMON_HERO, "rollTreasureReward");

		// Initialize message
		final TreasureRewardMessage reward = new TreasureRewardMessage ();
		reward.setTileTypeID (tileTypeID);
		reward.setMapFeatureID (mapFeatureID);
		
		// Keep going until we run out of points to spend
		int specialRewardCount = 0;		// Which picks/retort(s) are awarded is decided at the end, after the main loop - for now just record a count
		int remainingTreasureValue = treasureValue;
		
		while (remainingTreasureValue >= MINIMUM_RESOURCE_REWARD)
		{
			// Get a list of all hero items we might possibly get as a reward.
			final List<NumberedHeroItem> availableHeroItems = new ArrayList<NumberedHeroItem> ();
			for (final NumberedHeroItem item : gsk.getAvailableHeroItem ())
				if ((getHeroItemCalculations ().haveRequiredBooksForItem (item, pub.getPick (), db)) &&
					(getHeroItemCalculations ().calculateCraftingCost (item, db) <= remainingTreasureValue))
					
					availableHeroItems.add (item);
			
			// Do we have any spells missing of each rank, within cost limits?
			final List<SpellRankSvr> availableSpellRanks = new ArrayList<SpellRankSvr> ();
			for (final SpellRankSvr spellRank : db.getSpellRanks ())
				if ((spellRank.getTreasureRewardCost () != null) && (remainingTreasureValue >= spellRank.getTreasureRewardCost ()))
				{
					// Maybe we have all spells of this rank already, or maybe we can't them because we don't have enough books in this realm.
					// To get spells as treasure rewards, need 1 pick for common/uncommon, 2 picks for rare, 3 picks for very rare.
					boolean found = false;
					final Iterator<SpellSvr> spells = db.getSpells ().iterator ();
					while ((!found) && (spells.hasNext ()))
					{
						final SpellSvr spell = spells.next ();
						if (spell.getSpellRank ().equals (spellRank.getSpellRankID ()))
						{
							final SpellResearchStatus researchStatus = getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), spell.getSpellID ());
							if ((researchStatus != null) && ((researchStatus.getStatus () == SpellResearchStatusID.NOT_IN_SPELL_BOOK) ||
								(researchStatus.getStatus () == SpellResearchStatusID.RESEARCHABLE) || (researchStatus.getStatus () == SpellResearchStatusID.RESEARCHABLE_NOW)))
								
								found = true;
						}
					}
					
					if (found)
						availableSpellRanks.add (spellRank);
				}

			// Get a list of non-dead heroes who aren't currently in service (heroes that we dismiss go back to "Generated")
			final List<MemoryUnit> availableHeroes = new ArrayList<MemoryUnit> ();
			if (remainingTreasureValue >= PRISONER_REWARD_COST)
				for (final SummonedUnit summoned : summonHero.getSummonedUnit ())
				{
					final UnitSvr possibleUnit = db.findUnit (summoned.getSummonedUnitID (), "rollTreasureReward");
					
					final MemoryUnit hero = getUnitServerUtils ().findUnitWithPlayerAndID
						(gsk.getTrueMap ().getUnit (), player.getPlayerDescription ().getPlayerID (), summoned.getSummonedUnitID ());
					
					boolean addToList = ((hero != null) && ((hero.getStatus () == UnitStatusID.GENERATED) || (hero.getStatus () == UnitStatusID.NOT_GENERATED)));

					// Check for units that require particular picks to summon
					final Iterator<PickAndQuantity> iter = possibleUnit.getUnitPickPrerequisite ().iterator ();
					while ((addToList) && (iter.hasNext ()))
					{
						final PickAndQuantity prereq = iter.next ();
						if (getPlayerPickUtils ().getQuantityOfPick (pub.getPick (), prereq.getPickID ()) < prereq.getQuantity ())
							addToList = false;
					}
					
					if (addToList)
						availableHeroes.add (hero);
				}
			
			// Make a list of all possible options, and include them the right number of times for the relative chances
			final List<TreasureRewardType> rewardTypes = new ArrayList<TreasureRewardType> ();
			if (availableHeroItems.size () > 0)
				addPossibleRewardType (rewardTypes, TreasureRewardType.HERO_ITEM);
			
			if (availableSpellRanks.size () > 0)
				addPossibleRewardType (rewardTypes, TreasureRewardType.SPELL);

			addPossibleRewardType (rewardTypes, TreasureRewardType.GOLD);
			addPossibleRewardType (rewardTypes, TreasureRewardType.MANA);
			
			if (remainingTreasureValue >= SPECIAL_REWARD_COST)
				addPossibleRewardType (rewardTypes, TreasureRewardType.SPECIAL);
			
			if (availableHeroes.size () > 0)
				addPossibleRewardType (rewardTypes, TreasureRewardType.PRISONER);
			
			// Debug the choices
			if (log.isDebugEnabled ())
			{
				final StringBuilder debug = new StringBuilder ("Treasure reward for player " + player.getPlayerDescription ().getPlayerID () + " at location " + lairNodeTowerLocation +
					" has following choices to spend remaining " + remainingTreasureValue + " budget: ");
				boolean first = true;
				for (final TreasureRewardType rewardType : rewardTypes)
				{
					if (!first)
						debug.append (", ");
					
					debug.append (rewardType);
					first = false;
				}
				log.debug (debug);
			}
			
			// Roll one of the choices
			final TreasureRewardType rewardType = rewardTypes.get (getRandomUtils ().nextInt (rewardTypes.size ()));
			
			// Grant the reward, and charge it to the treasure budget
			switch (rewardType)
			{
				// Choose the actual hero item granted
				case HERO_ITEM:
					final NumberedHeroItem item = availableHeroItems.get (getRandomUtils ().nextInt (availableHeroItems.size ()));
					final int craftingCost = getHeroItemCalculations ().calculateCraftingCost (item, db);
					log.debug ("Treasure reward for player " + player.getPlayerDescription ().getPlayerID () + " at location " + lairNodeTowerLocation + " rolled hero item URN " +
						item.getHeroItemURN () + " name " + item.getHeroItemName () + " with cost " + craftingCost);
					
					gsk.getAvailableHeroItem ().remove (item);
					priv.getUnassignedHeroItem ().add (item);
					reward.getHeroItem ().add (item);
					remainingTreasureValue = remainingTreasureValue - craftingCost;
					break;
					
				// Choose the actual spell granted
				case SPELL:
					final SpellRankSvr spellRank = availableSpellRanks.get (getRandomUtils ().nextInt (availableSpellRanks.size ()));
					final List<SpellResearchStatus> availableSpells = new ArrayList<SpellResearchStatus> ();
					for (final SpellSvr spell : db.getSpells ())
						if (spell.getSpellRank ().equals (spellRank.getSpellRankID ()))
						{
							final SpellResearchStatus researchStatus = getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), spell.getSpellID ());
							if ((researchStatus != null) && ((researchStatus.getStatus () == SpellResearchStatusID.NOT_IN_SPELL_BOOK) ||
								(researchStatus.getStatus () == SpellResearchStatusID.RESEARCHABLE) || (researchStatus.getStatus () == SpellResearchStatusID.RESEARCHABLE_NOW)))
								
								availableSpells.add (researchStatus);
						}
					
					final SpellResearchStatus spell = availableSpells.get (getRandomUtils ().nextInt (availableSpells.size ()));
					log.debug ("Treasure reward for player " + player.getPlayerDescription ().getPlayerID () + " at location " + lairNodeTowerLocation + " rolled " +
						spellRank.getSpellRankDescription () + " spell " + spell.getSpellID () + " with cost " + spellRank.getTreasureRewardCost ());

					spell.setStatus (SpellResearchStatusID.AVAILABLE);
					reward.getSpellID ().add (spell.getSpellID ());
					remainingTreasureValue = remainingTreasureValue - spellRank.getTreasureRewardCost ();					
					break;
					
				// Choose the amount of resource actually given
				case GOLD:
				case MANA:
					final String productionTypeID = (rewardType == TreasureRewardType.GOLD) ? CommonDatabaseConstants.PRODUCTION_TYPE_ID_GOLD : CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA;
					final int min = MINIMUM_RESOURCE_REWARD / 10;
					final int max = Math.min (MAXIMUM_RESOURCE_REWARD, remainingTreasureValue) / 10;
					final int amount = (min + getRandomUtils ().nextInt (max - min + 1)) * 10;
					log.debug ("Treasure reward for player " + player.getPlayerDescription ().getPlayerID () + " at location " + lairNodeTowerLocation +
						" rolled " + amount + " " + productionTypeID);

					getResourceValueUtils ().addToAmountStored (priv.getResourceValue (), productionTypeID, amount);
					remainingTreasureValue = remainingTreasureValue - amount;					
					
					boolean found = false;
					for (final ProductionTypeAndUndoubledValue resourceValue : reward.getResource ())
						if (resourceValue.getProductionTypeID ().equals (productionTypeID))
						{
							found = true;
							resourceValue.setUndoubledProductionValue (resourceValue.getUndoubledProductionValue () + amount);
						}
					
					if (!found)
					{
						final ProductionTypeAndUndoubledValue resourceValue = new ProductionTypeAndUndoubledValue ();
						resourceValue.setProductionTypeID (productionTypeID);
						resourceValue.setUndoubledProductionValue (amount);
						reward.getResource ().add (resourceValue);
					}
					break;
					
				// Just count up the number of specials/picks for now - we decide which ones at the end
				case SPECIAL:
					log.debug ("Treasure reward for player " + player.getPlayerDescription ().getPlayerID () + " at location " + lairNodeTowerLocation + " rolled a special");
					specialRewardCount++;
					remainingTreasureValue = remainingTreasureValue - SPECIAL_REWARD_COST;
					break;
					
				// Choose the actual hero gained
				case PRISONER:
					final MemoryUnit hero = availableHeroes.get (getRandomUtils ().nextInt (availableHeroes.size ()));
					remainingTreasureValue = remainingTreasureValue - PRISONER_REWARD_COST;

					// Rest of this is as per summon hero spell
					// Check if the lair/node/tower has space for the unit
					final UnitAddLocation addLocation = getUnitServerUtils ().findNearestLocationWhereUnitCanBeAdded
						(lairNodeTowerLocation, hero.getUnitID (), player.getPlayerDescription ().getPlayerID (), gsk.getTrueMap (), players, sd, db);
					log.debug ("Treasure reward for player " + player.getPlayerDescription ().getPlayerID () + " at location " + lairNodeTowerLocation + " rolled prisoner URN " +
						hero.getUnitURN () + " ID " + hero.getUnitID () + " name " + hero.getUnitName () + ", bump result " + addLocation.getBumpType ());
					
					final TreasureRewardPrisoner rewardPrisoner = new TreasureRewardPrisoner ();
					rewardPrisoner.setPrisonerUnitURN (hero.getUnitURN ());
					rewardPrisoner.setUnitAddBumpType (addLocation.getBumpType ());
					reward.getPrisoner ().add (rewardPrisoner);
					
					if (addLocation.getUnitLocation () != null)
					{
						if (hero.getStatus () == UnitStatusID.NOT_GENERATED)
							getUnitServerUtils ().generateHeroNameAndRandomSkills (hero, db);

						getFogOfWarMidTurnChanges ().updateUnitStatusToAliveOnServerAndClients (hero, addLocation.getUnitLocation (), player, players, gsk.getTrueMap (), sd, db);
					
						// Let it move this turn
						hero.setDoubleOverlandMovesLeft (2 * getUnitSkillUtils ().getModifiedSkillValue (hero, hero.getUnitHasSkill (),
							CommonDatabaseConstants.UNIT_SKILL_ID_MOVEMENT_SPEED, null, UnitSkillComponent.ALL, UnitSkillPositiveNegative.BOTH,
							null, null, players, gsk.getTrueMap (), db));
					}
					break;

				default:
					throw new MomException ("rollTreasureReward chose treasure of type " + rewardType + " but doesn't know how to grant it");
			}
		}
		
		// Now deal with special/pick rewards
		if (specialRewardCount > 0)
		{
			// Spell books are limited according to the type of lair/node/tower that we captured
			final List<String> availableSpellBookIDs = new ArrayList<String> ();
			
			if (mapFeatureID != null)
			{
				final MapFeatureSvr mapFeature = db.findMapFeature (mapFeatureID, "rollTreasureReward");
				for (final MapFeatureTreasureBookReward book : mapFeature.getMapFeatureTreasureBookReward ())
					availableSpellBookIDs.add (book.getPickID ());
			}
			
			// If we got none, then either there was no map feature there, or its not a lair type of feature, e.g. gold
			if (availableSpellBookIDs.size () == 0)
			{
				final TileTypeSvr tileType = db.findTileType (tileTypeID, "rollTreasureReward");
				if (tileType.getMagicRealmID () != null)
					availableSpellBookIDs.add (tileType.getMagicRealmID ());
			}
			
			// Debug book choices
			if (log.isDebugEnabled ())
			{
				final StringBuilder debug = new StringBuilder ("Treasure reward for player " + player.getPlayerDescription ().getPlayerID () + " at location " + lairNodeTowerLocation +
					" awarded " + specialRewardCount + " special(s); possible books from tile type " + tileTypeID + " map feature " +
					mapFeatureID + " are: ");
				boolean first = true;
				for (final String magicRealmID : availableSpellBookIDs)
				{
					if (!first)
						debug.append (", ");
					
					debug.append (magicRealmID);
					first = false;
				}
				log.debug (debug);
			}

			// Decide on books and retorts
			while (specialRewardCount > 0)
			{
				// Get a list of all books and all retorts that we could obtain
				final Map<String, List<PickSvr>> availablePickTypes = new HashMap<String, List<PickSvr>> (); 
				for (final PickSvr pick : db.getPicks ())
				{					
					final PickTypeSvr pickType = db.findPickType (pick.getPickType (), "rollTreasureReward");
					
					// Can't pick Myrran, or spend more points than we have
					if ((pick.getPickCost () != null) && (pick.getPickCost () <= 2) && (pick.getPickCost () <= specialRewardCount) &&
							
						// Book types must match what is defined for the lair
						((pickType.getMaximumQuantity () != null) || (availableSpellBookIDs.contains (pick.getPickID ()))))
					{
						// Can't get life books if have any death books, and vice versa
						boolean ok = true;
						for (final PickExclusiveFrom exclusive : pick.getPickExclusiveFrom ())
							if (getPlayerPickUtils ().getQuantityOfPick (pub.getPick (), exclusive.getPickExclusiveFromID ()) > 0)
								ok = false;
		
						// We don't normally care about pre-requisites, except for the special situation that we only
						// allow Divine Power with at least 1 life book, or Infernal Power with at least 1 death book
						if (ok)
							for (final PickPrerequisite prereq : pick.getPickPrerequisite ())
								if (prereq.getPrerequisiteID () != null)
								{
									final PickSvr prereqPick = db.findPick (prereq.getPrerequisiteID (), "rollTreasureReward");
									
									// Sneaky way to identify Divine Power and Infernal Power without hard coding them
									if ((prereqPick.getPickExclusiveFrom ().size () > 0) && (getPlayerPickUtils ().getQuantityOfPick (pub.getPick (), prereq.getPrerequisiteID ()) == 0))
										ok = false;
								}
						
						if (ok)
						{
							// Can't get retorts twice
							ok = (pickType.getMaximumQuantity () == null) ||
								(getPlayerPickUtils ().getQuantityOfPick (pub.getPick (), pick.getPickID ()) < pickType.getMaximumQuantity ());
						}
						
						if (ok)
						{
							// Add to available choices
							List<PickSvr> availablePicks = availablePickTypes.get (pick.getPickType ());
							if (availablePicks == null)
							{
								availablePicks = new ArrayList<PickSvr> ();
								availablePickTypes.put (pick.getPickType (), availablePicks);
							}
							availablePicks.add (pick);
						}
					}
				}

				// Debug special choices
				if (log.isDebugEnabled ())
				{
					final StringBuilder debug = new StringBuilder ("Treasure reward for player " + player.getPlayerDescription ().getPlayerID () + " at location " + lairNodeTowerLocation +
						" has " + specialRewardCount + " special(s) left to award; possible choices are:");
					availablePickTypes.forEach ((pickTypeID, availablePicks) ->
					{
						debug.append (" " + pickTypeID + " (");
						boolean first = true;
						for (final PickSvr pick : availablePicks)
						{
							if (!first)
								debug.append (", ");
							
							debug.append (pick.getPickDescription ());
							first = false;
						}
						debug.append (")");
					});
					
					log.debug (debug);
				}
				
				// If we got no possible picks whatsoever, award an unlimited value hero item instead
				if (availablePickTypes.size () == 0)
				{
					// We must meet pick requirements for the item, but cost is unlimited				
					specialRewardCount = 0;
		
					final List<NumberedHeroItem> availableHeroItems = new ArrayList<NumberedHeroItem> ();
					for (final NumberedHeroItem item : gsk.getAvailableHeroItem ())
						if (getHeroItemCalculations ().haveRequiredBooksForItem (item, pub.getPick (), db))
							availableHeroItems.add (item);
				
					log.debug ("Treasure reward for player " + player.getPlayerDescription ().getPlayerID () + " at location " + lairNodeTowerLocation +
						" awarding artifact instead out of possible " + availableHeroItems.size () + " items");
					
					if (availableHeroItems.size () > 0)
					{
						final NumberedHeroItem item = availableHeroItems.get (getRandomUtils ().nextInt (availableHeroItems.size ()));
						gsk.getAvailableHeroItem ().remove (item);
						priv.getUnassignedHeroItem ().add (item);
						reward.getHeroItem ().add (item);

						log.debug ("Treasure reward for player " + player.getPlayerDescription ().getPlayerID () + " at location " + lairNodeTowerLocation + " rolled artifact item URN " +
							item.getHeroItemURN () + " name " + item.getHeroItemName ());
					}
				}
				else
				{
					// Choose pick type to award - have to list the pick types by their relative chance (75% chance to get books, 25% to get retorts)
					final List<String> availablePickTypesList = new ArrayList<String> ();
					for (final String pickTypeID : availablePickTypes.keySet ())
					{
						final Integer relativeChance = db.findPickType (pickTypeID, "rollTreasureReward").getRelativeChance ();
						if (relativeChance != null)
							for (int n = 0; n < relativeChance; n++)
								availablePickTypesList.add (pickTypeID);
					}
					
					final String pickTypeID = availablePickTypesList.get (getRandomUtils ().nextInt (availablePickTypesList.size ()));
					
					// Choose pick to award
					final List<PickSvr> availablePicks = availablePickTypes.get (pickTypeID);
					final PickSvr pick = availablePicks.get (getRandomUtils ().nextInt (availablePicks.size ()));
					
					getPlayerPickUtils ().updatePickQuantity (pub.getPick (), pick.getPickID (), 1);
					specialRewardCount = specialRewardCount - pick.getPickCost ();

					log.debug ("Treasure reward for player " + player.getPlayerDescription ().getPlayerID () + " at location " + lairNodeTowerLocation + " awarded special " +
						pickTypeID + " - " + pick.getPickDescription () + " with cost " + pick.getPickCost ());
					
					// It may already be listed in rewards
					boolean found = false;
					for (final PickAndQuantity rewardPick : reward.getPick ())
						if (rewardPick.getPickID ().equals (pick.getPickID ()))
						{
							found = true;
							rewardPick.setQuantity (rewardPick.getQuantity () + 1);
						}
					
					if (!found)
					{
						final PickAndQuantity rewardPick = new PickAndQuantity ();
						rewardPick.setPickID (pick.getPickID ());
						rewardPick.setQuantity (1);
						reward.getPick ().add (rewardPick);
					}
					
					// If the pick grants any spells (Artificer) then learn them
					for (final PickFreeSpellSvr freeSpell : pick.getPickFreeSpells ())
						getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), freeSpell.getFreeSpellID ()).setStatus (SpellResearchStatusID.AVAILABLE);
				}
			}
		}
		
		log.trace ("Exiting rollTreasureReward: " + reward.getHeroItem ().size () + " hero items, " + reward.getSpellID ().size () + " spells, " +
			reward.getResource ().size () + " resources, " + reward.getPick ().size () + " picks, " + reward.getPrisoner ().size () + " prisoners"); 
		return reward;
	}
	
	/**
	 * Adds a type of reward to the list of possible rewards, adding it the the number of times for the relative chance of this reward 
	 * 
	 * @param list List of possible reward types to add to
	 * @param rewardType Reward type to add
	 */
	final void addPossibleRewardType (final List<TreasureRewardType> list, final TreasureRewardType rewardType)
	{
		for (int n = 0; n < rewardType.getRelativeChance (); n++)
			list.add (rewardType);
	}
	
	/**
	 * Sends the reward info to the client, including all separate messages to e.g. add hero items and so on.
	 * 
	 * @param reward Details of treasure reward to send
	 * @param player Player who earned the reward
	 * @param players List of players in this session
	 * @param db Lookup lists built over the XML database
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 */
	@Override
	public final void sendTreasureReward (final TreasureRewardMessage reward, final PlayerServerDetails player,
		final List<PlayerServerDetails> players, final ServerDatabaseEx db)
		throws JAXBException, XMLStreamException, RecordNotFoundException
	{
		log.trace ("Entering sendTreasureReward: Player " + player.getPlayerDescription ().getPlayerID ());

		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) player.getPersistentPlayerPublicKnowledge ();
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		
		// Send picks - everybody can see these
		if (reward.getPick ().size () > 0)
		{
			// Resend all of them, not just the new ones
			final ReplacePicksMessage msg = new ReplacePicksMessage ();
			msg.setPlayerID (player.getPlayerDescription ().getPlayerID ());
			msg.getPick ().addAll (pub.getPick ());
			
			getMultiplayerSessionServerUtils ().sendMessageToAllClients (players, msg);
		}
			
		// Did gaining another book add more spells available for us to research in future?
		getServerSpellCalculations ().randomizeResearchableSpells (priv.getSpellResearchStatus (), pub.getPick (), db);

		// Make sure we still have 8 spells available to research, in case one of the 8 already listed was now gained for free,
		// or maybe we had less than 8 unresearched spells in total but now gained some more
		getServerSpellCalculations ().randomizeSpellsResearchableNow (priv.getSpellResearchStatus (), db);
		
		// Send private info
		if (player.getPlayerDescription ().isHuman ())
		{
			// Send hero items
			for (final NumberedHeroItem item : reward.getHeroItem ())
			{
				final AddUnassignedHeroItemMessage msg = new AddUnassignedHeroItemMessage ();
				msg.setHeroItem (item);
				player.getConnection ().sendMessageToClient (msg);
			}
			
			// Send revised spell list - its a bit difficult to tell whether anything actually changed here; so for now, just send it always
			final FullSpellListMessage spellsMsg = new FullSpellListMessage ();
			spellsMsg.getSpellResearchStatus ().addAll (priv.getSpellResearchStatus ());
			player.getConnection ().sendMessageToClient (spellsMsg);

			// Send main message
			player.getConnection ().sendMessageToClient (reward);
		}
		
		// Resend resource values if we gained some gold/mana
		if (reward.getResource ().size () > 0)
			getServerResourceCalculations ().sendGlobalProductionValues (player, null);
		
		log.trace ("Exiting sendTreasureReward");
	}

	/**
	 * @return Hero item utils
	 */
	public final HeroItemUtils getHeroItemUtils ()
	{
		return heroItemUtils;
	}

	/**
	 * @param util Hero item utils
	 */
	public final void setHeroItemUtils (final HeroItemUtils util)
	{
		heroItemUtils = util;
	}

	/**
	 * @return Hero item calculations
	 */
	public final HeroItemCalculations getHeroItemCalculations ()
	{
		return heroItemCalculations;
	}

	/**
	 * @param calc Hero item calculations
	 */
	public final void setHeroItemCalculations (final HeroItemCalculations calc)
	{
		heroItemCalculations = calc;
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
	 * @return Resource value utils
	 */
	public final ResourceValueUtils getResourceValueUtils ()
	{
		return resourceValueUtils;
	}

	/**
	 * @param util Resource value utils
	 */
	public final void setResourceValueUtils (final ResourceValueUtils util)
	{
		resourceValueUtils = util;
	}

	/**
	 * @return Unit skill utils
	 */
	public final UnitSkillUtils getUnitSkillUtils ()
	{
		return unitSkillUtils;
	}

	/**
	 * @param utils Unit skill utils
	 */
	public final void setUnitSkillUtils (final UnitSkillUtils utils)
	{
		unitSkillUtils = utils;
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
}