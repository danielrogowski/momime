package momime.server.events;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.utils.random.RandomUtils;
import com.ndg.utils.random.WeightedChoicesImpl;

import jakarta.xml.bind.JAXBException;
import momime.common.MomException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.Event;
import momime.common.database.EventWizardTarget;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.messages.MemoryMaintainedSpell;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.utils.ResourceValueUtils;
import momime.server.MomSessionVariables;

/**
 * Deals with targeting for all random events
 */
public final class RandomEventTargetingImpl implements RandomEventTargeting
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (RandomEventTargetingImpl.class);
	
	/** Random wizard events */
	private RandomWizardEvents randomWizardEvents;
	
	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Random number generator */
	private RandomUtils randomUtils;
	
	/** Rolls random events */
	private RandomEvents randomEvents;
	
	/**
	 * @param event Event we want to find a target for
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @return Whether this event can find any valid target or not
	 * @throws RecordNotFoundException If we can't find an expected data item
	 */
	@Override
	public final boolean isAnyValidTargetForEvent (final Event event, final MomSessionVariables mom)
		throws RecordNotFoundException
	{
		boolean valid = false;
		
		// Is it an event that targets a wizard?
		if (event.getEventWizardTarget () != null)
		{
			final Iterator<PlayerServerDetails> iter = mom.getPlayers ().iterator ();
			while ((!valid) && (iter.hasNext ()))
			{
				final PlayerServerDetails player = iter.next ();
				if (getRandomWizardEvents ().isWizardValidTargetForEvent (event, player, mom))
					valid = true;
			}
		}
		
		// Disjunction has no duration, and is only valid if there are some overland enchantments
		else if (event.getMinimumDuration () == null)
		{
			final Iterator<MemoryMaintainedSpell> iter = mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ().iterator ();
			while ((!valid) && (iter.hasNext ()))
			{
				final MemoryMaintainedSpell spell = iter.next ();
				final Spell spellDef = mom.getServerDB ().findSpell (spell.getSpellID (), "isAnyValidTargetForEvent");
				if (spellDef.getSpellBookSectionID () == SpellBookSectionID.OVERLAND_ENCHANTMENTS)
					valid = true;
			}
		}
		
		// Everything else are Conjunction events - good/bad moon and red/green/blue nodes, and also mana short, which are valid if there isn't already a conjunction
		else
			valid = (mom.getGeneralPublicKnowledge ().getConjunctionEventID () == null);
		
		return valid;
	}

	/**
	 * @param event Event to trigger
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the message
	 * @throws XMLStreamException If there is a problem sending the message
	 * @throws IOException If there is another kind of problem
	 */
	@Override
	public final void triggerEvent (final Event event, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, IOException
	{
		// Is it an event that targets a wizard?  If so then now need to pick the wizard to target
		if (event.getEventWizardTarget () != null)
		{
			final Map<Integer, Integer> powerBase = new HashMap<Integer, Integer> ();
			for (final PlayerServerDetails player : mom.getPlayers ())
				if (getRandomWizardEvents ().isWizardValidTargetForEvent (event, player, mom))
				{
					final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
					final int thisPowerBase = getResourceValueUtils ().findAmountPerTurnForProductionType (priv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER);
					powerBase.put (player.getPlayerDescription ().getPlayerID (), thisPowerBase);
				}
			
			if (powerBase.isEmpty ())
				throw new MomException ("Event " + event.getEventID () + " was picked, but now finding no wizard is a valid target");
			
			final int maxPowerBase = powerBase.values ().stream ().mapToInt (v -> v).max ().orElse (0);

			// Prefer strong or weak wizards?
			final WeightedChoicesImpl<PlayerServerDetails> playerChoices = new WeightedChoicesImpl<PlayerServerDetails> ();
			playerChoices.setRandomUtils (getRandomUtils ());
			
			for (final PlayerServerDetails player : mom.getPlayers ())
				if (powerBase.containsKey (player.getPlayerDescription ().getPlayerID ()))
				{
					int weighting = powerBase.get (player.getPlayerDescription ().getPlayerID ());
					if (event.getEventWizardTarget () == EventWizardTarget.WEAK)
						weighting = 10 + maxPowerBase - weighting;
					else
						weighting = 10 + weighting;
					
					playerChoices.add (weighting, player);
				}
			
			final PlayerServerDetails targetWizard = playerChoices.nextWeightedValue ();
			log.debug ("Triggering event " + event.getEventID () + " " + event.getEventName ().get (0).getText () + " on wizard " + targetWizard.getPlayerDescription ().getPlayerName ());
			
			getRandomWizardEvents ().triggerWizardEvent (event, targetWizard, mom);
		}
		
		else
		{
			// Disjunction
			if (event.getMinimumDuration () == null)
			{
				getRandomEvents ().sendRandomEventMessage (event.getEventID (), null, null, null, null, null, null, false, false, mom.getPlayers (), null);
				
				for (final MemoryMaintainedSpell spell : mom.getGeneralServerKnowledge ().getTrueMap ().getMaintainedSpell ())
				{
					final Spell spellDef = mom.getServerDB ().findSpell (spell.getSpellID (), "triggerEvent");
					if (spellDef.getSpellBookSectionID () == SpellBookSectionID.OVERLAND_ENCHANTMENTS)
						mom.getWorldUpdates ().switchOffSpell (spell.getSpellURN ());
				}
				
				mom.getWorldUpdates ().process (mom);
			}
			
			// Everything else are Conjunction events - good/bad moon and red/green/blue nodes, and also mana short
			else
			{
				getRandomEvents ().sendRandomEventMessage (event.getEventID (), null, null, null, null, null, null, true, false, mom.getPlayers (), null);
				
				mom.getGeneralPublicKnowledge ().setConjunctionEventID (event.getEventID ());
				mom.getGeneralServerKnowledge ().setConjunctionStartedTurnNumber (mom.getGeneralPublicKnowledge ().getTurnNumber ());
			}
		}
	}
	
	/**
	 * @param event Event to switch off
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we can't find an expected data item
	 * @throws PlayerNotFoundException If we can't find the player who owns a game element
	 * @throws MomException If there is another kind of error
	 * @throws JAXBException If there is a problem sending the message
	 * @throws XMLStreamException If there is a problem sending the message
	 */
	@Override
	public final void cancelEvent (final Event event, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, MomException, JAXBException, XMLStreamException
	{
		getRandomEvents ().sendRandomEventMessage (event.getEventID (), null, null, null, null, null, null, true, true, mom.getPlayers (), null);
		
		mom.getGeneralPublicKnowledge ().setConjunctionEventID (null);
		mom.getGeneralServerKnowledge ().setConjunctionStartedTurnNumber (null);
	}
	
	/**
	 * @return Random wizard events
	 */
	public final RandomWizardEvents getRandomWizardEvents ()
	{
		return randomWizardEvents;
	}

	/**
	 * @param e Random wizard events
	 */
	public final void setRandomWizardEvents (final RandomWizardEvents e)
	{
		randomWizardEvents = e;
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
	 * @return Rolls random events
	 */
	public final RandomEvents getRandomEvents ()
	{
		return randomEvents;
	}

	/**
	 * @param e Rolls random events
	 */
	public final void setRandomEvents (final RandomEvents e)
	{
		randomEvents = e;
	}
}