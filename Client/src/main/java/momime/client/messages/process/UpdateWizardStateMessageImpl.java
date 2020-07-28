package momime.client.messages.process;

import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.CustomDurationServerToClientMessage;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.dialogs.WizardBanishedUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.ui.frames.WizardsUI;
import momime.client.utils.WizardClientUtils;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.WizardState;
import momime.common.messages.servertoclient.UpdateWizardStateMessage;
import momime.common.utils.PlayerKnowledgeUtils;

/**
 * Server announces to everybody when a wizard gets banished, so the clients can show the animation for it
 */
public final class UpdateWizardStateMessageImpl extends UpdateWizardStateMessage implements CustomDurationServerToClientMessage 
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (UpdateWizardStateMessageImpl.class);

	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Wizards UI */
	private WizardsUI wizardsUI;
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Wizard client utils */
	private WizardClientUtils wizardClientUtils;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		log.trace ("Entering start");
		
		// Update player details
		final PlayerPublicDetails banishedWizard = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getBanishedPlayerID (), "WizardBanishedMessageImpl (A)");
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) banishedWizard.getPersistentPlayerPublicKnowledge ();
		pub.setWizardState (getWizardState ());
		
		// Show cracked gem on wizards screen
		getWizardsUI ().updateWizards ();

		// Look up other details
		final boolean isDefeated = (getWizardState () == WizardState.DEFEATED);
		final PlayerPublicDetails banishingWizard = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getBanishingPlayerID (), "WizardBanishedMessageImpl (B)");
		final MomPersistentPlayerPublicKnowledge banishingPub = (MomPersistentPlayerPublicKnowledge) banishingWizard.getPersistentPlayerPublicKnowledge ();
		
		// Show animation
		if (pub.getStandardPhotoID () != null)
		{
			final WizardBanishedUI wizardBanishedUI = getPrototypeFrameCreator ().createWizardBanished ();
			wizardBanishedUI.setBanishedWizard (banishedWizard);
			wizardBanishedUI.setBanishingWizard (banishingWizard);
			wizardBanishedUI.setDefeated (isDefeated);
			wizardBanishedUI.setUpdateWizardStateMessage (this);
			wizardBanishedUI.setVisible (true);
		}
		else
		{
			// Custom portrait, so cannot show animation, just a message box
			final String languageEntryID = (isDefeated ? "Defeated" : "Banished") + "By" + (PlayerKnowledgeUtils.isWizard (banishingPub.getWizardID ()) ? "Wizard" : "Raiders");
			
			final String title = getLanguage ().findCategoryEntry ("frmWizardBanished", languageEntryID).replaceAll
				("BANISHED_WIZARD", getWizardClientUtils ().getPlayerName (banishedWizard)).replaceAll
				("BANISHING_WIZARD", getWizardClientUtils ().getPlayerName (banishingWizard));
			
			final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
			msg.setTitle (title);
			msg.setText (title);
			msg.setVisible (true);
			
			getClient ().finishCustomDurationMessage (this);
		}
		
		log.trace ("Exiting start");
	}
	
	/**
	 * Nothing to do here when the message completes, because its all handled in either WizardBanishedUI or MiniCityViewUI
	 */
	@Override
	public final void finish ()
	{
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
	 * @return Wizards UI
	 */
	public final WizardsUI getWizardsUI ()
	{
		return wizardsUI;
	}

	/**
	 * @param ui Wizards UI
	 */
	public final void setWizardsUI (final WizardsUI ui)
	{
		wizardsUI = ui;
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
}