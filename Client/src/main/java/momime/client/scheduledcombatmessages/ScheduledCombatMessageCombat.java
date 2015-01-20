package momime.client.scheduledcombatmessages;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.io.IOException;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.v0_9_5.MapFeature;
import momime.client.language.database.v0_9_5.TileType;
import momime.client.messages.ClientMemoryGridCellUtils;
import momime.client.newturnmessages.NewTurnMessageClickable;
import momime.client.ui.MomUIConstants;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.utils.WizardClientUtils;
import momime.common.database.CommonDatabaseConstants;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.MomScheduledCombat;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.messages.clienttoserver.RequestStartScheduledCombatMessage;
import momime.common.utils.ScheduledCombatUtils;
import momime.common.utils.UnitUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerNotFoundException;
import com.ndg.multiplayer.session.PlayerPublicDetails;

/**
 * Message about one scheduled combat that we're involved in and still needs to be played.
 * These appear under various categories, which is figured out by the methods below.
 */
public final class ScheduledCombatMessageCombat implements ScheduledCombatMessageSimpleUI, NewTurnMessageClickable
{
	/** Class logger */
	private final Log log = LogFactory.getLog (ScheduledCombatMessageCombat.class);
	
	/** Multiplayer client */
	private MomClient client;

	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Scheduled combat utils */
	private ScheduledCombatUtils scheduledCombatUtils; 
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;

	/** Scheduled combat message processing */
	private ScheduledCombatMessageProcessing scheduledCombatMessageProcessing;

	/** Wizard client utils */
	private WizardClientUtils wizardClientUtils;
	
	/** Small font */
	private Font smallFont;
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** Details about the combat we're displaying a message for */
	private MomScheduledCombat combat;

	/**
	 * @return One of the SORT_ORDER_ constants, indicating the sort order/title category to group this message under
	 */
	@Override
	public final ScheduledCombatMessageSortOrder getSortOrder ()
	{
		ScheduledCombatMessageSortOrder result = null;
		try
		{
			if (getCombat ().isWalkInWithoutAFight ())
				result = ScheduledCombatMessageSortOrder.SORT_ORDER_WALK_IN_WITHOUT_A_FIGHT;
			else
			{
				// Need to know who else is involved
				final PlayerPublicDetails ohp = getScheduledCombatUtils ().determineOtherHumanPlayer (getCombat (), getClient ().getOurPlayerID (), getClient ().getPlayers ());
				final MomTransientPlayerPublicKnowledge ohpTrans = (ohp == null) ? null : (MomTransientPlayerPublicKnowledge) ohp.getTransientPlayerPublicKnowledge ();
				
				if (ohp == null)
					result = ScheduledCombatMessageSortOrder.SORT_ORDER_AI_OPPONENT;
				else if ((ohpTrans.getScheduledCombatUrnRequested () != null) && (ohpTrans.getScheduledCombatUrnRequested ().intValue () == getCombat ().getScheduledCombatURN ()))
					result = ScheduledCombatMessageSortOrder.SORT_ORDER_REQUESTING_US_TO_PLAY;
				else if ((ohpTrans.getScheduledCombatUrnRequested () != null) || (ohpTrans.isCurrentlyPlayingCombat ()))
					result = ScheduledCombatMessageSortOrder.SORT_ORDER_HUMAN_OPPONENT_BUSY;
				else
					result = ScheduledCombatMessageSortOrder.SORT_ORDER_HUMAN_OPPONENT_FREE;
			}			
		}
		catch (final PlayerNotFoundException e)
		{
			log.error (e, e);
		}
	
		return result;
	}
	
	/**
	 * Take appropriate action when a new turn message is clicked on
	 * @throws Exception If there was a problem
	 */
	@Override
	public final void clicked () throws Exception
	{
		log.trace ("Entering: clicked");
		
		// Make sure the other player isn't busy (the server double checks this in case)
		final ScheduledCombatMessageSortOrder sortOrder = getSortOrder ();
		String languageEntryID = null;
		if (sortOrder == ScheduledCombatMessageSortOrder.SORT_ORDER_HUMAN_OPPONENT_BUSY)
			languageEntryID = "ClickBusy";
		else
		{
			// If against another human player, freeze us until they respond
			if (sortOrder == ScheduledCombatMessageSortOrder.SORT_ORDER_HUMAN_OPPONENT_FREE)
				languageEntryID = "ClickWaiting";
			
			// Request the server to start this combat
			final RequestStartScheduledCombatMessage msg = new RequestStartScheduledCombatMessage ();
			msg.setScheduledCombatURN (getCombat ().getScheduledCombatURN ());
			getClient ().getServerConnection ().sendMessageToServer (msg);
		}
		
		// Message to display?
		if (languageEntryID != null)
		{
			final PlayerPublicDetails ohp = getScheduledCombatUtils ().determineOtherHumanPlayer (getCombat (), getClient ().getOurPlayerID (), getClient ().getPlayers ());
			final String ohpName = getWizardClientUtils ().getPlayerName (ohp);
			
			final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
			msg.setTitleLanguageCategoryID ("ScheduledCombats");
			msg.setTitleLanguageEntryID ("Title");
			msg.setText (getLanguage ().findCategoryEntry ("ScheduledCombats", languageEntryID).replaceAll ("PLAYER_NAME", ohpName));
			msg.setVisible (true);
		}

		log.trace ("Exiting : clicked");
	}
	
	/**
	 * @return Image to draw for this combat, or null to display only text
	 */
	@Override
	public final Image getImage ()
	{
		return null;
	}

	/**
	 * @return Font to display the text in
	 */
	@Override
	public final Font getFont ()
	{
		return getSmallFont ();
	}
	
	/**
	 * @return Colour to display the text in
	 */
	@Override
	public final Color getColour ()
	{
		return MomUIConstants.SILVER;
	}

	/**
	 * @return Text to display for this combat
	 */
	@Override
	public final String getText ()
	{
		String text = null;

		// How many attacking units?
		final int attackingUnitCount = getCombat ().getAttackingUnitURN ().size ();
		
		// How many defending units?
		final MapCoordinates3DEx combatLocation = new MapCoordinates3DEx (getCombat ().getDefendingLocation ().getX (), getCombat ().getDefendingLocation ().getY (),
			Math.max (getCombat ().getDefendingLocation ().getZ (), getCombat ().getAttackingFrom ().getZ ()));
		
		final int defendingUnitCount = getUnitUtils ().countAliveEnemiesAtLocation (getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit (),
			combatLocation.getX (), combatLocation.getY (), combatLocation.getZ (), getCombat ().getAttackingPlayerID ());
		
		// Get info about the combat location
		final MemoryGridCell mc = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
			(combatLocation.getZ ()).getRow ().get (combatLocation.getY ()).getCell ().get (combatLocation.getX ());

		try
		{
			final boolean isNodeLairTower = ClientMemoryGridCellUtils.isNodeLairTower (mc.getTerrainData (), getClient ().getClientDB ());
			final boolean isCity = ((mc.getCityData () != null) && (mc.getCityData ().getCityPopulation () != null) && (mc.getCityData ().getCityPopulation () > 0));
			final String cityName = isCity ? mc.getCityData ().getCityName () : "";
			
			// Need to get the description of whether its a node/lair/tower
			String nodeLairTowerName = "";
			if (isNodeLairTower)
			{
				// Tile types (nodes)
				if ((mc.getTerrainData ().getTileTypeID () != null) && (getClient ().getClientDB ().findTileType (mc.getTerrainData ().getTileTypeID (), "ScheduledCombatMessageCombat").getMagicRealmID () != null))
				{
					final TileType tileType = getLanguage ().findTileType (mc.getTerrainData ().getTileTypeID ());
					if ((tileType != null) && (tileType.getTileTypeShowAsFeature () != null))
						nodeLairTowerName = tileType.getTileTypeShowAsFeature ();
				}
				
				// Map features (lairs and towers)
				else if ((mc.getTerrainData ().getMapFeatureID () != null) && (getClient ().getClientDB ().findMapFeature (mc.getTerrainData ().getMapFeatureID (), "ScheduledCombatMessageCombat").isAnyMagicRealmsDefined ()))
				{
					final MapFeature mapFeature = getLanguage ().findMapFeature (mc.getTerrainData ().getMapFeatureID ());
					if ((mapFeature != null) && (mapFeature.getMapFeatureDescription () != null))
						nodeLairTowerName = mapFeature.getMapFeatureDescription (); 
				}
			}

			// Get the two players involved
			final PlayerPublicDetails attackingPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getCombat ().getAttackingPlayerID (), "ScheduledCombatMessageCombat");
			final PlayerPublicDetails defendingPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getCombat ().getDefendingPlayerID (), "ScheduledCombatMessageCombat");
			final MomPersistentPlayerPublicKnowledge defPub = (MomPersistentPlayerPublicKnowledge) defendingPlayer.getPersistentPlayerPublicKnowledge ();
			
			// Now we can figure out the right text to display
			final String languageEntryID;
			if (getCombat ().getAttackingPlayerID () == getClient ().getOurPlayerID ().intValue ())
			{
				// We're the attacker
				if (getCombat ().isWalkInWithoutAFight ())
					languageEntryID = "WalkInWithoutAFight";
				else if (isNodeLairTower)
				{
					if (CommonDatabaseConstants.WIZARD_ID_MONSTERS.equals (defPub.getWizardID ()))
						languageEntryID = "AttackingUnclearedNodeLairTower";
					else
						languageEntryID = "AttackingClearedNodeLairTower";
				}
				else if (isCity)
					languageEntryID = "AttackingCity";
				else
					languageEntryID = "AttackingOutside";
			}
			else
			{
				// We're the defender
				if (isNodeLairTower)
					languageEntryID = "DefendingClearedNodeLairTower";		// It must be cleared, or we couldn't be defending it
				else if (isCity)
					languageEntryID = "DefendingCity";
				else
					languageEntryID = "DefendingOutside";
			}

			// Do all the text replacements
			text = getLanguage ().findCategoryEntry ("ScheduledCombats", languageEntryID).replaceAll
				("ATTACKING_UNIT_COUNT", new Integer (attackingUnitCount).toString ()).replaceAll
				("DEFENDING_UNIT_COUNT", new Integer (defendingUnitCount).toString ()).replaceAll
				("CITY_NAME", cityName).replaceAll
				("DEFENDERS_NAME", getWizardClientUtils ().getPlayerName (defendingPlayer)).replaceAll
				("ATTACKERS_NAME", getWizardClientUtils ().getPlayerName (attackingPlayer)).replaceAll
				("NODE_LAIR_TOWER", nodeLairTowerName);
		}
		catch (final IOException e)
		{
			log.error (e, e);
		}
		
		return text;
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
	 * @return Multiplayer client
	 */
	public final MomClient getClient ()
	{
		return client;
	}
	
	/**
	 * @param obj Multiplayer client
	 */
	public final void setClient (final MomClient obj)
	{
		client = obj;
	}
	
	/**
	 * @return Scheduled combat utils
	 */
	public final ScheduledCombatUtils getScheduledCombatUtils ()
	{
		return scheduledCombatUtils;
	}

	/**
	 * @param utils Scheduled combat utils
	 */
	public final void setScheduledCombatUtils (final ScheduledCombatUtils utils)
	{
		scheduledCombatUtils = utils;
	}

	/**
	 * @return Session utils
	 */
	public final MultiplayerSessionUtils getMultiplayerSessionUtils ()
	{
		return multiplayerSessionUtils;
	}

	/**
	 * @param util Session utils
	 */
	public final void setMultiplayerSessionUtils (final MultiplayerSessionUtils util)
	{
		multiplayerSessionUtils = util;
	}

	/**
	 * @return Scheduled combat message processing
	 */
	public final ScheduledCombatMessageProcessing getScheduledCombatMessageProcessing ()
	{
		return scheduledCombatMessageProcessing;
	}

	/**
	 * @param proc Scheduled combat message processing
	 */
	public final void setScheduledCombatMessageProcessing (final ScheduledCombatMessageProcessing proc)
	{
		scheduledCombatMessageProcessing = proc;
	}

	/**
	 * @return Wizard client utils
	 */
	public final WizardClientUtils getWizardClientUtils ()
	{
		return wizardClientUtils;
	}

	/**
	 * @param util Wizard client utils
	 */
	public final void setWizardClientUtils (final WizardClientUtils util)
	{
		wizardClientUtils = util;
	}
	
	/**
	 * @return Small font
	 */
	public final Font getSmallFont ()
	{
		return smallFont;
	}

	/**
	 * @param font Small font
	 */
	public final void setSmallFont (final Font font)
	{
		smallFont = font;
	}

	/**
	 * @return Language database holder
	 */
	public final LanguageDatabaseHolder getLanguageHolder ()
	{
		return languageHolder;
	}
	
	/**
	 * @param holder Language database holder
	 */
	public final void setLanguageHolder (final LanguageDatabaseHolder holder)
	{
		languageHolder = holder;
	}

	/**
	 * Convenience shortcut for accessing the Language XML database
	 * @return Language database
	 */
	public final LanguageDatabaseEx getLanguage ()
	{
		return languageHolder.getLanguage ();
	}

	/**
	 * @return Prototype frame creator
	 */
	public final PrototypeFrameCreator getPrototypeFrameCreator ()
	{
		return prototypeFrameCreator;
	}

	/**
	 * @param obj Prototype frame creator
	 */
	public final void setPrototypeFrameCreator (final PrototypeFrameCreator obj)
	{
		prototypeFrameCreator = obj;
	}
	
	/**
	 * @return Details about the combat we're displaying a message for
	 */
	public final MomScheduledCombat getCombat ()
	{
		return combat;
	}

	/**
	 * @param details Details about the combat we're displaying a message for
	 */
	public final void setCombat (final MomScheduledCombat details)
	{
		combat = details;
	}
}