package momime.server.ai;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.random.RandomUtils;

import momime.common.MomException;
import momime.common.database.RecordNotFoundException;
import momime.common.database.SpellBookSectionID;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.common.utils.MemoryMaintainedSpellUtils;
import momime.common.utils.SpellCastType;
import momime.common.utils.SpellUtils;
import momime.common.utils.TargetSpellResult;
import momime.server.MomSessionVariables;
import momime.server.database.ServerDatabaseEx;
import momime.server.database.SpellSvr;
import momime.server.process.SpellProcessing;
import momime.server.process.SpellQueueing;

/**
 * Methods for AI players making decisions about spells
 */
public final class SpellAIImpl implements SpellAI
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (SpellAIImpl.class);
	
	/** Spell utils */
	private SpellUtils spellUtils;

	/** MemoryMaintainedSpell utils */
	private MemoryMaintainedSpellUtils memoryMaintainedSpellUtils;
	
	/** AI spell calculations */
	private AISpellCalculations aiSpellCalculations;

	/** Spell queueing methods */
	private SpellQueueing spellQueueing;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/** AI decisions about cities */
	private CityAI cityAI;
	
	/** Spell processing methods */
	private SpellProcessing spellProcessing;
	
	/**
	 * Common routine between picking free spells at the start of the game and picking the next spell to research - it picks a spell from the supplied list
	 * @param spells List of possible spells to choose from
	 * @param aiPlayerID Player ID, for debug message
	 * @return ID of chosen spell to research
	 * @throws MomException If the list was empty
	 */
	final SpellSvr chooseSpellToResearchAI (final List<SpellSvr> spells, final int aiPlayerID)
		throws MomException
	{
		log.trace ("Entering chooseSpellToResearchAI: Player ID " + aiPlayerID);

		String debugLogMessage = null;

		// Check each spell in the list to find the the best research order, 1 being the best, 9 being the worst, and make a list of spells with this research order
		int bestResearchOrder = Integer.MAX_VALUE;
		final List<SpellSvr> spellsWithBestResearchOrder = new ArrayList<SpellSvr> ();

		for (final SpellSvr spell : spells)
		{
			if (spell.getAiResearchOrder () != null)
			{
				// List possible choices in debug message
				if (debugLogMessage == null)
					debugLogMessage = spell.getSpellName () + " (" + spell.getAiResearchOrder () + ")";
				else
					debugLogMessage = debugLogMessage + ", " + spell.getSpellName () + " (" + spell.getAiResearchOrder () + ")";

				if (spell.getAiResearchOrder () < bestResearchOrder)
				{
					bestResearchOrder = spell.getAiResearchOrder ();
					spellsWithBestResearchOrder.clear ();
				}

				if (spell.getAiResearchOrder () == bestResearchOrder)
					spellsWithBestResearchOrder.add (spell);
			}
		}

		// Check we found one (this error should only happen if the list was totally empty)
		if ((bestResearchOrder == Integer.MAX_VALUE) || (spellsWithBestResearchOrder.size () == 0))
			throw new MomException ("chooseSpellToResearchAI: No appropriate spells to pick from list of " + spells.size ());

		// Pick one at random
		final SpellSvr chosenSpell = spellsWithBestResearchOrder.get (getRandomUtils ().nextInt (spellsWithBestResearchOrder.size ()));

		log.trace ("Exiting chooseSpellToResearchAI = " + chosenSpell.getSpellID ());
		return chosenSpell;
	}

	/**
	 * @param player AI player who needs to choose what to research
	 * @param db Lookup lists built over the XML database
	 * @throws RecordNotFoundException If there is a spell in the list of research statuses that doesn't exist in the DB
	 * @throws MomException If there is an error in the logic
	 */
	@Override
	public final void decideWhatToResearch (final PlayerServerDetails player, final ServerDatabaseEx db)
		throws RecordNotFoundException, MomException
	{
		log.trace ("Entering decideWhatToResearch: Player ID " + player.getPlayerDescription ().getPlayerID ());

		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();

		final List<momime.common.database.Spell> researchableSpells = getSpellUtils ().getSpellsForStatus
			(priv.getSpellResearchStatus (), SpellResearchStatusID.RESEARCHABLE_NOW, db);

		if (!researchableSpells.isEmpty ())
		{
			final List<SpellSvr> researchableServerSpells = new ArrayList<SpellSvr> ();
			for (final momime.common.database.Spell spell : researchableSpells)
				researchableServerSpells.add ((SpellSvr) spell);

			final SpellSvr chosenSpell = chooseSpellToResearchAI (researchableServerSpells, player.getPlayerDescription ().getPlayerID ());
			priv.setSpellIDBeingResearched (chosenSpell.getSpellID ());
		}

		log.trace ("Exiting decideWhatToResearch = " + priv.getSpellIDBeingResearched ());
	}

	/**
	 * AI player at the start of the game chooses any spell of the specific magic realm & rank and researches it for free
	 * @param spells Pre-locked list of the player's spell
	 * @param magicRealmID Magic Realm (e.g. chaos) to pick a spell from
	 * @param spellRankID Spell rank (e.g. uncommon) to pick a spell of
	 * @param aiPlayerID Player ID, for debug message
	 * @param db Lookup lists built over the XML database
	 * @return Spell AI chose to learn for free
	 * @throws MomException If no eligible spells are available (e.g. player has them all researched already)
	 * @throws RecordNotFoundException If the spell chosen couldn't be found in the player's spell list
	 */
	@Override
	public final SpellResearchStatus chooseFreeSpellAI (final List<SpellResearchStatus> spells, final String magicRealmID, final String spellRankID,
		final int aiPlayerID, final ServerDatabaseEx db)
		throws MomException, RecordNotFoundException
	{
		log.trace ("Entering chooseFreeSpellAI: Player ID " + aiPlayerID + ", " + magicRealmID + ", " + spellRankID);

		// Get candidate spells
		final List<momime.common.database.Spell> commonSpellList = getSpellUtils ().getSpellsNotInBookForRealmAndRank (spells, magicRealmID, spellRankID, db);
		final List<SpellSvr> spellList = new ArrayList<SpellSvr> ();
		for (final momime.common.database.Spell thisSpell : commonSpellList)
			spellList.add ((SpellSvr) thisSpell);

		// Choose a spell
		final SpellSvr chosenSpell = chooseSpellToResearchAI (spellList, aiPlayerID);

		// Return spell research status; calling routine sets it to available
		final SpellResearchStatus chosenSpellStatus = getSpellUtils ().findSpellResearchStatus (spells, chosenSpell.getSpellID ());

		log.trace ("Exiting chooseFreeSpellAI: " + chosenSpellStatus.getSpellID ());
		return chosenSpellStatus;
	}
	
	/**
	 * If AI player is not currently casting any spells overland, then look through all of them and consider if we should cast any.
	 * 
	 * @param player AI player who needs to choose what to cast
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws PlayerNotFoundException If we can't find one of the players
	 * @throws RecordNotFoundException If we find the spell they're trying to cast, or other expected game elements
	 * @throws MomException If there are any issues with data or calculation logic
	 */
	@Override
	public final void decideWhatToCastOverland (final PlayerServerDetails player, final MomSessionVariables mom)
		throws MomException, RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		log.trace ("Entering decideWhatToCastOverland: Player ID " + player.getPlayerDescription ().getPlayerID ());

		// Exit if we're already in the middle of casting something
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		if (priv.getQueuedSpell ().size () == 0)
		{
			// Consider every possible spell we could cast overland and can afford maintainence of
			final List<SpellSvr> considerSpells = new ArrayList<SpellSvr> ();
			for (final SpellSvr spell : mom.getServerDB ().getSpells ())
				if ((getSpellUtils ().spellCanBeCastIn (spell, SpellCastType.OVERLAND)) &&
					(getSpellUtils ().findSpellResearchStatus (priv.getSpellResearchStatus (), spell.getSpellID ()).getStatus () == SpellResearchStatusID.AVAILABLE) &&
					(getAiSpellCalculations ().canAffordSpellMaintenance (player, spell)))
				{
					// For now, only consider overland enchantments - and we must not already have in the enchantment in effect
					if ((spell.getSpellBookSectionID () == SpellBookSectionID.OVERLAND_ENCHANTMENTS) &&
						(getMemoryMaintainedSpellUtils ().findMaintainedSpell (mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (),
							player.getPlayerDescription ().getPlayerID (), spell.getSpellID (), null, null, null, null) == null))
					{
						log.debug ("AI player ID " + player.getPlayerDescription ().getPlayerID () + " considering casting overland spell " + spell.getSpellID ());
						considerSpells.add (spell);
					}
				}
			
			// If we found any, then pick one randomly
			if (considerSpells.size () > 0)
			{
				final SpellSvr spell = considerSpells.get (getRandomUtils ().nextInt (considerSpells.size ()));

				log.debug ("AI player ID " + player.getPlayerDescription ().getPlayerID () + " casting overland spell " + spell.getSpellID ());
				getSpellQueueing ().requestCastSpell (player, null, null, null, spell.getSpellID (), null, null, null, null, null, mom);
			}
		}
		
		log.trace ("Exiting decideWhatToCastOverland");
	}
	
	/**
	 * Overland spells are cast first (probably taking several turns) and a target is only chosen after casting is completed.  So after the AI finishes
	 * casting an overland spell that requires a target, this method tries to pick a good target for the spell.  This won't even get called for
	 * types of spell that don't require targets (e.g. overland enchantments or summoning spells), see method castOverlandNow.
	 * 
	 * @param player AI player who needs to choose a spell target
	 * @param spell Definition for the spell to target
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
	public final void decideSpellTarget (final PlayerServerDetails player, final SpellSvr spell, final MemoryMaintainedSpell maintainedSpell, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		log.trace ("Entering decideSpellTarget: Player ID " + player.getPlayerDescription ().getPlayerID () + ", " + spell + ", Spell URN " + maintainedSpell.getSpellURN ());
		
		final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
		
		MapCoordinates3DEx targetLocation = null;
		MemoryUnit targetUnit = null;
		String citySpellEffectID = null;
		String unitSkillID = null;
		
		switch (spell.getSpellBookSectionID ())
		{
			// City enchantments - currently this is only here for Spell of Return but tried to make it generic enough to work for others
			case CITY_ENCHANTMENTS:
			case CITY_CURSES:
				int bestCityQuality = -1;
				for (int z = 0; z < mom.getSessionDescription ().getOverlandMapSize ().getDepth (); z++)
					for (int y = 0; y < mom.getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
						for (int x = 0; x < mom.getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
						{
							final MapCoordinates3DEx cityLocation = new MapCoordinates3DEx (x, y, z);
							
							// Routine checks everything, even down to whether there is even a city there or not, so just let it handle it
							if (getMemoryMaintainedSpellUtils ().isCityValidTargetForSpell (mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell (), spell,
								player.getPlayerDescription ().getPlayerID (), cityLocation, mom.getGeneralServerKnowledge ().getTrueMap ().getMap (), priv.getFogOfWar (),
								mom.getGeneralServerKnowledge ().getTrueMap ().getBuilding ()) == TargetSpellResult.VALID_TARGET)
							{
								final int thisCityQuality = getCityAI ().evaluateCityQuality (cityLocation, false, false, priv.getFogOfWarMemory ().getMap (), mom.getSessionDescription (), mom.getServerDB ());
								if ((targetLocation == null) || (thisCityQuality > bestCityQuality))
								{
									targetLocation = cityLocation;
									bestCityQuality = thisCityQuality;
								}
							}
						}
				break;
				
			default:
				throw new MomException ("AI decideSpellTarget does not know how to decide a target for spell " + spell.getSpellID () + " in section " + spell.getSpellBookSectionID ());
		}
		
		// If we got a location then target the spell; if we didn't then cancel the spell
		if ((targetLocation == null) && (targetUnit == null))
			getSpellProcessing ().cancelTargetOverlandSpell (maintainedSpell, mom);
		else
			getSpellProcessing ().targetOverlandSpell (spell, maintainedSpell, targetLocation, targetUnit, citySpellEffectID, unitSkillID, mom);
		
		log.trace ("Exiting decideSpellTarget");
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
	 * @param spellUtil MemoryMaintainedSpell utils
	 */
	public final void setMemoryMaintainedSpellUtils (final MemoryMaintainedSpellUtils spellUtil)
	{
		memoryMaintainedSpellUtils = spellUtil;
	}
	
	/**
	 * @return AI spell calculations
	 */
	public final AISpellCalculations getAiSpellCalculations ()
	{
		return aiSpellCalculations;
	}

	/**
	 * @param calc AI spell calculations
	 */
	public final void setAiSpellCalculations (final AISpellCalculations calc)
	{
		aiSpellCalculations = calc;
	}

	/**
	 * @return Spell queueing methods
	 */
	public final SpellQueueing getSpellQueueing ()
	{
		return spellQueueing;
	}

	/**
	 * @param obj Spell queueing methods
	 */
	public final void setSpellQueueing (final SpellQueueing obj)
	{
		spellQueueing = obj;
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
	 * @return AI decisions about cities
	 */
	public final CityAI getCityAI ()
	{
		return cityAI;
	}

	/**
	 * @param ai AI decisions about cities
	 */
	public final void setCityAI (final CityAI ai)
	{
		cityAI = ai;
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
}