package momime.server.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import com.ndg.multiplayer.server.session.MultiplayerSessionServerUtils;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.sessionbase.PlayerType;

import jakarta.xml.bind.JAXBException;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.database.RelationScore;
import momime.common.messages.DiplomacyWizardDetails;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.Pact;
import momime.common.messages.PactType;
import momime.common.messages.PlayerPick;
import momime.common.messages.WizardState;
import momime.common.messages.servertoclient.AddPowerBaseHistoryMessage;
import momime.common.messages.servertoclient.MeetWizardMessage;
import momime.common.messages.servertoclient.PactMessage;
import momime.common.messages.servertoclient.PowerBaseHistoryPlayer;
import momime.common.messages.servertoclient.ReplacePicksMessage;
import momime.common.utils.KnownWizardUtils;
import momime.common.utils.PlayerKnowledgeUtils;
import momime.common.utils.ResourceValueUtils;
import momime.server.MomSessionVariables;
import momime.server.ai.RelationAI;
import momime.server.database.ServerDatabaseValues;

/**
 * Process for making sure one wizard has met another wizard
 */
public final class KnownWizardServerUtilsImpl implements KnownWizardServerUtils
{
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/** Methods for working with wizardIDs */
	private PlayerKnowledgeUtils playerKnowledgeUtils;

	/** Resource value utils */
	private ResourceValueUtils resourceValueUtils;
	
	/** Server only helper methods for dealing with players in a session */
	private MultiplayerSessionServerUtils multiplayerSessionServerUtils;
	
	/** For calculating relation scores between two wizards */
	private RelationAI relationAI;
	
	/**
	 * @param metWizardID The wizard who has become known
	 * @param meetingWizardID The wizard who now knows them; if null then everybody now knows them
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @param showAnimation Whether to show animation popup of wizard announcing themselves to you
	 * @throws RecordNotFoundException If we can't find the wizard we are meeting
	 * @throws PlayerNotFoundException If we can't find the player we are meeting
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final void meetWizard (final int metWizardID, final Integer meetingWizardID, final boolean showAnimation, final MomSessionVariables mom)
		throws RecordNotFoundException, PlayerNotFoundException, JAXBException, XMLStreamException
	{
		final KnownWizardDetails metWizard = getKnownWizardUtils ().findKnownWizardDetails (mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), metWizardID, "meetWizard");
		final PlayerServerDetails metPlayer = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), metWizardID, "meetWizard");
		final MomPersistentPlayerPrivateKnowledge metPlayerPriv = (MomPersistentPlayerPrivateKnowledge) metPlayer.getPersistentPlayerPrivateKnowledge ();
		
		// Go through each player who gets to meet them
		for (final PlayerServerDetails player : mom.getPlayers ())
			if ((meetingWizardID == null) || (meetingWizardID.equals (player.getPlayerDescription ().getPlayerID ())))
			{
				final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
				
				// Do they already know them?
				if (getKnownWizardUtils ().findKnownWizardDetails (priv.getFogOfWarMemory ().getWizardDetails (), metWizardID) == null)
				{
					// On the server, remember these wizard have now met; make a separate copy of the object
					final DiplomacyWizardDetails knownWizardDetails = new DiplomacyWizardDetails ();
					knownWizardDetails.setPlayerID (metWizardID);
					knownWizardDetails.setWizardID (metWizard.getWizardID ());
					knownWizardDetails.setStandardPhotoID (metWizard.getStandardPhotoID ());
					knownWizardDetails.setCustomPhoto (metWizard.getCustomPhoto ());
					knownWizardDetails.setCustomFlagColour (metWizard.getCustomFlagColour ());
					knownWizardDetails.setWizardState (metWizard.getWizardState ());
					knownWizardDetails.setWizardPersonalityID (metWizard.getWizardPersonalityID ());
					knownWizardDetails.setWizardObjectiveID (metWizard.getWizardObjectiveID ());
					knownWizardDetails.setMaximumGoldTribute (ServerDatabaseValues.INITIAL_MAXIMUM_GOLD_TRIBUTE);
					knownWizardDetails.getPowerBaseHistory ().addAll (metWizard.getPowerBaseHistory ());
					copyPickList (metWizard.getPick (), knownWizardDetails.getPick ());
					copyPacts (metWizard.getPact (), knownWizardDetails.getPact (), priv.getFogOfWarMemory ().getWizardDetails ().stream ().map (w -> w.getPlayerID ()).collect (Collectors.toSet ()));
					
					// Calculate their base opinion of us based on what spell books we both have
					final KnownWizardDetails ourWizardDetails = getKnownWizardUtils ().findKnownWizardDetails
						(mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), player.getPlayerDescription ().getPlayerID (), "meetWizard");
					knownWizardDetails.setBaseRelation (getRelationAI ().calculateBaseRelation (metWizard.getPick (), ourWizardDetails.getPick (), mom.getServerDB ()));
					knownWizardDetails.setVisibleRelation (knownWizardDetails.getBaseRelation ());

					priv.getFogOfWarMemory ().getWizardDetails ().add (knownWizardDetails);
					
					// Tell the player the wizard they chose was OK; in that way they get their copy of their own KnownWizardDetails record
					if (player.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
					{
						final MeetWizardMessage meet = new MeetWizardMessage ();
						meet.setKnownWizardDetails (knownWizardDetails);
						
						if (showAnimation)
						{
							meet.setShowAnimation (true);
							
							// Even though we're only just meeting this wizard, its possible they knew about us a long time ago,
							// for example maybe we cast an overland enchantment, so if they do know about us then use their
							// real visible relation score.  Otherwise use the base starting value calculated above
							// (which is technically OUR opinion of THEM so is not really what we want, but initially its the same value)
							final DiplomacyWizardDetails theirOpinionOfUs = (DiplomacyWizardDetails) getKnownWizardUtils ().findKnownWizardDetails
								(metPlayerPriv.getFogOfWarMemory ().getWizardDetails (), player.getPlayerDescription ().getPlayerID ());
							final int visibleRelation = (theirOpinionOfUs != null) ? theirOpinionOfUs.getVisibleRelation () : knownWizardDetails.getVisibleRelation ();
							final RelationScore relationScore = mom.getServerDB ().findRelationScoreForValue (visibleRelation, "meetWizard");
							meet.setVisibleRelationScoreID (relationScore.getRelationScoreID ());
						}
						
						player.getConnection ().sendMessageToClient (meet);
					}
					
					// The reverse versions of any pacts sent above also need to be added+sent
					for (final Pact srcPact : knownWizardDetails.getPact ())
					{
						final KnownWizardDetails pactWithPlayer = getKnownWizardUtils ().findKnownWizardDetails (priv.getFogOfWarMemory ().getWizardDetails (), srcPact.getPactWithPlayerID (), "meetWizard (P)");
						
						final Pact destPact = new Pact ();
						destPact.setPactWithPlayerID (metWizardID);
						destPact.setPactType (srcPact.getPactType ());
						pactWithPlayer.getPact ().add (destPact);
						
						if (player.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
						{
							final PactMessage msg = new PactMessage ();
							msg.setUpdatePlayerID (srcPact.getPactWithPlayerID ());
							msg.setPactPlayerID (metWizardID);
							msg.setPactType (srcPact.getPactType ());
							player.getConnection ().sendMessageToClient (msg);
						}
					}
				}
			}
	}

	/**
	 * @param src List of picks to copy from
	 * @param dest List of picks to copy to
	 */
	@Override
	public final void copyPickList (final List<PlayerPick> src, final List<PlayerPick> dest)
	{
		dest.clear ();
		for (final PlayerPick srcPick : src)
		{
			final PlayerPick destPick = new PlayerPick ();
			destPick.setPickID (srcPick.getPickID ());
			destPick.setQuantity (srcPick.getQuantity ());
			destPick.setOriginalQuantity (srcPick.getOriginalQuantity ());
			dest.add (destPick);
		}					
	}

	/**
	 * @param src List of pacts to copy from
	 * @param dest List of pacts to copy to
	 * @param playerIDs Restrict copy to containing only these playerIDs
	 */
	final void copyPacts (final List<Pact> src, final List<Pact> dest, final Set<Integer> playerIDs)
	{
		dest.clear ();
		for (final Pact srcPact : src)
			if (playerIDs.contains (srcPact.getPactWithPlayerID ()))
			{
				final Pact destPact = new Pact ();
				destPact.setPactWithPlayerID (srcPact.getPactWithPlayerID ());
				destPact.setPactType (srcPact.getPactType ());
				dest.add (destPact);
			}
	}
	
	/**
	 * Updates all copies of a wizard state on the server.  Does not notify clients of the change.
	 * 
	 * @param playerID Player whose state changed
	 * @param newState New state
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we can't find the player in the server's true wizard details
	 */
	@Override
	public final void updateWizardState (final int playerID, final WizardState newState, final MomSessionVariables mom)
		throws RecordNotFoundException
	{
		// True memory
		getKnownWizardUtils ().findKnownWizardDetails (mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), playerID, "updateWizardState").setWizardState (newState);
		
		// Each player who knows them
		for (final PlayerServerDetails player : mom.getPlayers ())
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			final KnownWizardDetails wizardDetails = getKnownWizardUtils ().findKnownWizardDetails (priv.getFogOfWarMemory ().getWizardDetails (), playerID);
			if (wizardDetails != null)
				wizardDetails.setWizardState (newState);
		}
	}
	
	/**
	 * Picks have been updated in server's true memory.  Now they need copying to the player memory of each player who knows that wizard, and sending to the clients.
	 * 
	 * @param playerID Player whose picks changed.
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If we can't find the player in the server's true wizard details
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	@Override
	public final void copyAndSendUpdatedPicks (final int playerID, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException,XMLStreamException
	{
		// True memory
		final KnownWizardDetails trueWizard = getKnownWizardUtils ().findKnownWizardDetails (mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), playerID, "copyAndSendUpdatedPicks");
		
		// Build message - resend all of them, not just the new ones
		final ReplacePicksMessage msg = new ReplacePicksMessage ();
		msg.setPlayerID (playerID);
		msg.getPick ().addAll (trueWizard.getPick ());
		
		// Each player who knows them
		for (final PlayerServerDetails player : mom.getPlayers ())
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			final KnownWizardDetails wizardDetails = getKnownWizardUtils ().findKnownWizardDetails (priv.getFogOfWarMemory ().getWizardDetails (), playerID);
			if (wizardDetails != null)
			{
				copyPickList (trueWizard.getPick (), wizardDetails.getPick ());
				if (player.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
					player.getConnection ().sendMessageToClient (msg);
			}
		}
	}

	/**
	 * During the start phase, when resources are recalculated, this stores the power base of each wizard, which is public info to every player via the Historian screen.
	 * 
	 * @param onlyOnePlayerID If zero, will record power base for all players; if specified will record power base only for the specified player
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 * @throws RecordNotFoundException If one of the wizard isn't found in the list
	 * @throws PlayerNotFoundException If we can't find one of the players to send the messages out to
	 */
	@Override
	public final void storePowerBaseHistory (final int onlyOnePlayerID, final MomSessionVariables mom)
		throws JAXBException, XMLStreamException, RecordNotFoundException, PlayerNotFoundException
	{
		// Each player knows different wizards, so build up a different message for each player
		final Map<Integer, AddPowerBaseHistoryMessage> msgs = new HashMap<Integer, AddPowerBaseHistoryMessage> ();

		for (final PlayerServerDetails historyPlayer : mom.getPlayers ())
			if ((onlyOnePlayerID == 0) || (historyPlayer.getPlayerDescription ().getPlayerID () == onlyOnePlayerID))
			{
				final KnownWizardDetails trueWizard = getKnownWizardUtils ().findKnownWizardDetails
					(mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), historyPlayer.getPlayerDescription ().getPlayerID (), "storePowerBaseHistory"); 
				
				// Ignore raiders and rampaging monsters
				if (getPlayerKnowledgeUtils ().isWizard (trueWizard.getWizardID ()))
				{
					final MomPersistentPlayerPrivateKnowledge historyPriv = (MomPersistentPlayerPrivateKnowledge) historyPlayer.getPersistentPlayerPrivateKnowledge ();					
					final int powerBase = getResourceValueUtils ().findAmountPerTurnForProductionType (historyPriv.getResourceValue (), CommonDatabaseConstants.PRODUCTION_TYPE_ID_MAGIC_POWER);
					
					// Its possible some tries were missed if the player missed turns due to Time Stop
					final int zeroCount = mom.getGeneralPublicKnowledge ().getTurnNumber () - trueWizard.getPowerBaseHistory ().size () - 1;
					
					// Store in true wizard details
					for (int n = 0; n < zeroCount; n++)
						trueWizard.getPowerBaseHistory ().add (0);

					trueWizard.getPowerBaseHistory ().add (powerBase);
			
					// Build message					
					final PowerBaseHistoryPlayer item = new PowerBaseHistoryPlayer ();
					item.setPlayerID (historyPlayer.getPlayerDescription ().getPlayerID ());
					item.setPowerBase (powerBase);
					item.setZeroCount (zeroCount);
					
					// Each player who knows them
					for (final PlayerServerDetails player : mom.getPlayers ())
					{
						final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
						final KnownWizardDetails wizardDetails = getKnownWizardUtils ().findKnownWizardDetails (priv.getFogOfWarMemory ().getWizardDetails (), historyPlayer.getPlayerDescription ().getPlayerID ());
						if (wizardDetails != null)
						{
							for (int n = 0; n < zeroCount; n++)
								wizardDetails.getPowerBaseHistory ().add (0);

							wizardDetails.getPowerBaseHistory ().add (powerBase);
							
							if (player.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
							{
								AddPowerBaseHistoryMessage msg = msgs.get (player.getPlayerDescription ().getPlayerID ());
								if (msg == null)
								{
									msg = new AddPowerBaseHistoryMessage ();
									msgs.put (player.getPlayerDescription ().getPlayerID (), msg);
								}
								msg.getPlayer ().add (item);
							}
						}
					}
				}
			}
		
		// Send out all generated messages
		for (final Entry<Integer, AddPowerBaseHistoryMessage> msg : msgs.entrySet ())
		{
			final PlayerServerDetails player = getMultiplayerSessionServerUtils ().findPlayerWithID (mom.getPlayers (), msg.getKey (), "storePowerBaseHistory");
			player.getConnection ().sendMessageToClient (msg.getValue ());
		}
	}	
	
	/**
	 * This only updates the pact of the specified player; since pacts are two-way, the caller
	 * must therefore always call this method twice, switching the player params around.
	 * 
	 * @param updatePlayerID Player whose pact list is being updated
	 * @param pactPlayerID Who they have the pact with
	 * @param pactType New type of pact; null is fine and just means previous pact is now cancelled
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 * @throws JAXBException If there is a problem sending the reply to the client
	 * @throws XMLStreamException If there is a problem sending the reply to the client
	 */
	@Override
	public final void updatePact (final int updatePlayerID, final int pactPlayerID, final PactType pactType, final MomSessionVariables mom)
		throws RecordNotFoundException, JAXBException, XMLStreamException
	{
		// Update true wizard details on server
		final KnownWizardDetails trueUpdateWizard = getKnownWizardUtils ().findKnownWizardDetails (mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), updatePlayerID, "updatePact");
		getKnownWizardUtils ().updatePactWith (trueUpdateWizard.getPact (), pactPlayerID, pactType);

		// Build message
		final PactMessage msg = new PactMessage ();
		msg.setUpdatePlayerID (updatePlayerID);
		msg.setPactPlayerID (pactPlayerID);
		msg.setPactType (pactType);
		
		// Each player who knows BOTH wizards involved in the pact
		for (final PlayerServerDetails player : mom.getPlayers ())
		{
			final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
			final KnownWizardDetails wizardDetails = getKnownWizardUtils ().findKnownWizardDetails (priv.getFogOfWarMemory ().getWizardDetails (), updatePlayerID);
			if ((wizardDetails != null) && (getKnownWizardUtils ().findKnownWizardDetails (priv.getFogOfWarMemory ().getWizardDetails (), pactPlayerID) != null))
			{
				getKnownWizardUtils ().updatePactWith (wizardDetails.getPact (), pactPlayerID, pactType);
				if (player.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
					player.getConnection ().sendMessageToClient (msg);
			}
		}
	}
	
	/**
	 * Sets flag everywhere in server side memory
	 * 
	 * @param castingPlayerID Player who started casting Spell of Mastery
	 * @param mom Allows accessing server knowledge structures, player list and so on
	 * @throws RecordNotFoundException If the wizard to update isn't found in the list
	 */
	@Override
	public final void setEverStartedCastingSpellOfMastery (final int castingPlayerID, final MomSessionVariables mom)
		throws RecordNotFoundException
	{
		// Update true wizard details on server
		final KnownWizardDetails trueCastingWizard = getKnownWizardUtils ().findKnownWizardDetails (mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), castingPlayerID, "setEverStartedCastingSpellOfMastery");
		trueCastingWizard.setEverStartedCastingSpellOfMastery (true);

		// Each player who knows them
		for (final PlayerServerDetails player : mom.getPlayers ())
		{
			final KnownWizardDetails wizardDetails = getKnownWizardUtils ().findKnownWizardDetails (mom.getGeneralServerKnowledge ().getTrueMap ().getWizardDetails (), player.getPlayerDescription ().getPlayerID (), "setEverStartedCastingSpellOfMastery");
			if (getPlayerKnowledgeUtils ().isWizard (wizardDetails.getWizardID ()))
			{
				final MomPersistentPlayerPrivateKnowledge priv = (MomPersistentPlayerPrivateKnowledge) player.getPersistentPlayerPrivateKnowledge ();
				final KnownWizardDetails opinionOfCaster = getKnownWizardUtils ().findKnownWizardDetails (priv.getFogOfWarMemory ().getWizardDetails (), castingPlayerID);
				if (opinionOfCaster != null)
				{
					opinionOfCaster.setEverStartedCastingSpellOfMastery (true);
					if (player.getPlayerDescription ().getPlayerID () != castingPlayerID)
						getRelationAI ().penaltyToVisibleRelation ((DiplomacyWizardDetails) opinionOfCaster, 200);
				}
			}
		}
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