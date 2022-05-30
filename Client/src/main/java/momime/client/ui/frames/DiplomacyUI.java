package momime.client.ui.frames;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.multiplayer.sessionbase.PlayerType;
import com.ndg.random.RandomUtils;
import com.ndg.swing.GridBagConstraintsNoFill;
import com.ndg.swing.actions.LoggingAction;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutComponent;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import jakarta.xml.bind.JAXBException;
import momime.client.MomClient;
import momime.client.audio.AudioPlayer;
import momime.client.graphics.AnimationContainer;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.graphics.database.GraphicsDatabaseEx;
import momime.client.messages.process.MeetWizardMessageImpl;
import momime.client.ui.MomUIConstants;
import momime.client.ui.dialogs.OverlandEnchantmentsUI;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.client.utils.SpellClientUtils;
import momime.client.utils.WizardClientUtils;
import momime.common.MomException;
import momime.common.database.AnimationEx;
import momime.common.database.LanguageText;
import momime.common.database.LanguageTextVariant;
import momime.common.database.RelationScore;
import momime.common.database.WizardEx;
import momime.common.database.WizardPersonality;
import momime.common.database.WizardPortraitMood;
import momime.common.messages.DiplomacyAction;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.clienttoserver.RequestDiplomacyMessage;
import momime.common.utils.KnownWizardUtils;

/**
 * Diplomacy screen for talking to other players (both human and AI)
 */
public final class DiplomacyUI extends MomClientFrameUI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (DiplomacyUI.class);
	
	/** Wizard portrait states that are animated */
	private final static List<DiplomacyPortraitState> ANIMATED_STATES = Arrays.asList
		(DiplomacyPortraitState.APPEARING, DiplomacyPortraitState.DISAPPEARING, DiplomacyPortraitState.TALKING);

	/** Wizard portrait states that start by drawing the mirror */
	private final static List<DiplomacyPortraitState> MIRROR_STATES = Arrays.asList
		(DiplomacyPortraitState.APPEARING, DiplomacyPortraitState.DISAPPEARING, DiplomacyPortraitState.MIRROR);
	
	/** Milliseconds between animation frames (same as WizardWonUI which also shows wizard talking) */
	private final static int TIMER_TICKS_MS = 150;
	
	/** How many ticks to show for appearing/disappearing anim */
	private final static int APPEARING_TICKS = 10;
	
	/** Spaces left around components added to the text panel */
	private final static int NO_INSET = 0;
	
	/** XML layout */
	private XmlLayoutContainerEx diplomacyLayout;

	/** Graphics database */
	private GraphicsDatabaseEx graphicsDB;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Methods for finding KnownWizardDetails from the list */
	private KnownWizardUtils knownWizardUtils;
	
	/** Client-side spell utils */
	private SpellClientUtils spellClientUtils;

	/** Music player */
	private AudioPlayer musicPlayer;
	
	/** Medium font */
	private Font mediumFont;
	
	/** Small font */
	private Font smallFont;
	
	/** Random utils */
	private RandomUtils randomUtils;

	/** Wizard client utils */
	private WizardClientUtils wizardClientUtils;

	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;
	
	/** Which wizard we are talking to */
	private int talkingWizardID;

	/** Which wizard is the one controlling making proposals (and which one is waiting for the other side to make a proposal) */
	private int proposingWizardID;
	
	/** Which wizard we are talking to */
	private KnownWizardDetails talkingWizardDetails;
	
	/** Which player we are talking to */
	private PlayerPublicDetails talkingPlayer;

	/** Our relationship with the talking wizard */
	private RelationScore relationScore;
	
	/** Left green/red gargoyle eyes */
	private JLabel eyesLeftLabel;
	
	/** Right green/red gargoyle eyes */
	private JLabel eyesRightLabel;
	
	/** Contains image and animation names if using standard photo */
	private WizardEx standardPhotoDef;
	
	/** Decoded custom photo */
	private BufferedImage customPhoto;
	
	/** Which state to draw the portrait in */
	private DiplomacyPortraitState portraitState;
	
	/** Which text to display */
	private DiplomacyTextState textState;
	
	/** Current frame number to display */
	private int frameNumber;
	
	/** Since every frame requires some modification, caches the generated images; keyed by state-frame (frame where applicable) */
	private final Map<String, BufferedImage> wizardPortraits = new HashMap<String, BufferedImage> ();
	
	/** Overland enchantments mirror anim, but need this for the frame to clip the wizard portraits to round */
	private AnimationEx fadeAnim;
	
	/** Animation of wizard talking */
	private AnimationEx talkingAnim;
	
	/** Animation timer */
	private Timer timer;
	
	/** Content pane */
	private JPanel contentPane;
	
	/** The meet wizard message we're showing the animation for; either this or diplomacyAction must be set but not both */
	private MeetWizardMessageImpl meetWizardMessage;
	
	/** The last action we received; either this or meetWizardMessage must be set but not both */
	private DiplomacyAction diplomacyAction;
	
	/** Relation to use to decide the eye colour, facial expression and music */
	private String visibleRelationScoreID;
	
	/** Text or buttons in the lower half of the screen */
	private JPanel textPanel;
	
	/** Sizing of the text panel */
	private XmlLayoutComponent textPanelLayout;
	
	/** Components added to the text panel */
	private List<Component> textPanelComponents = new ArrayList<Component> ();
	
	/** Other wizard requested to talk to us and we accept */
	private Action acceptTalkToAction;
	
	/** Other wizard requested to talk to us and we reluctantly accept */
	private Action reluctantlyTalkToAction;
	
	/** Other wizard requested to talk to us and we refuse */
	private Action refuseTalkToAction;
	
	/** Propose a treaty to the wizard */
	private Action proposeTreatyAction;
	
	/** Break (or threaten to break) an existing treaty with the wizard */
	private Action breakTreatyAction;
	
	/** Offer the wizard free things to make them like us more */
	private Action offerTributeAction;
	
	/** Suggest spell exchange */
	private Action exchangeSpellsAction;
	
	/** End conversation nicely */
	private Action endConversationAction;
	
	/** Other wizard made some proposal and we accept */
	private Action acceptProposalAction;
	
	/** Other wizard made some proposal and we refuse */
	private Action refuseProposalAction;
	
	/** Propose a wizard pact */
	private Action proposeWizardPactAction;
	
	/** Propose an alliance */
	private Action proposeAllianceAction;
	
	/** Propose ending war */
	private Action proposePeaceTreatyAction;
	
	/** Propose declaring war on another wizard */
	private Action proposeDeclareWarOnAnotherWizardAction;
	
	/** Propose breaking alliance with another wizard */
	private Action proposeBreakAllianceWithAnotherWizardAction;
	
	/** Back to main choices */
	private Action backToMainChoicesAction;
	
	/** Tired of talking (other wizard ends conversation on us) */
	private Action tiredOfTalkingAction;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/diplomacy/background.png");
		final BufferedImage shiny = getUtils ().loadImage ("/momime.client.graphics/ui/mirror/shiny.png");

		// Need this just to cut the corner off the wizard portraits
		fadeAnim = getGraphicsDB ().findAnimation (OverlandEnchantmentsUI.MIRROR_ANIM, "DiplomacyUI");

		// Need this to know where to draw the wizard's photo
		final XmlLayoutComponent mirrorLocation = getDiplomacyLayout ().findComponent ("frmDiplomacyMirror");
		
		// Actions
		acceptTalkToAction = new LoggingAction ((ev) -> acceptTalkTo (DiplomacyAction.ACCEPT_TALKING));
		reluctantlyTalkToAction = new LoggingAction ((ev) -> acceptTalkTo (DiplomacyAction.ACCEPT_TALKING_IMPATIENT));
		
		refuseTalkToAction = new LoggingAction ((ev) ->
		{
			final RequestDiplomacyMessage msg = new RequestDiplomacyMessage ();
			msg.setTalkToPlayerID (getTalkingWizardID ());
			msg.setAction (DiplomacyAction.REJECT_TALKING);
			getClient ().getServerConnection ().sendMessageToServer (msg);

			setTextState (DiplomacyTextState.NONE);
			setPortraitState (DiplomacyPortraitState.DISAPPEARING);
			initializeText ();
			initializePortrait ();
		});
		
		proposeTreatyAction = new LoggingAction ((ev) ->
		{
			setTextState (DiplomacyTextState.PROPOSE_TREATY);
			initializeText ();
		});
		
		breakTreatyAction = new LoggingAction ((ev) -> {});
		offerTributeAction = new LoggingAction ((ev) -> {});
		exchangeSpellsAction = new LoggingAction ((ev) -> {});
		endConversationAction = new LoggingAction ((ev) ->
		{
			final RequestDiplomacyMessage msg = new RequestDiplomacyMessage ();
			msg.setTalkToPlayerID (getTalkingWizardID ());
			msg.setAction (DiplomacyAction.END_CONVERSATION);
			getClient ().getServerConnection ().sendMessageToServer (msg);

			setTextState (DiplomacyTextState.NONE);
			setPortraitState (DiplomacyPortraitState.DISAPPEARING);
			initializeText ();
			initializePortrait ();
		});

		acceptProposalAction = new LoggingAction ((ev) ->
		{
			final RequestDiplomacyMessage msg = new RequestDiplomacyMessage ();
			msg.setTalkToPlayerID (getTalkingWizardID ());
			
			switch (getTextState ())
			{
				case PROPOSE_WIZARD_PACT:
					msg.setAction (DiplomacyAction.ACCEPT_WIZARD_PACT);
					setTextState (DiplomacyTextState.ACCEPT_WIZARD_PACT);
					break;

				case PROPOSE_ALLIANCE:
					msg.setAction (DiplomacyAction.ACCEPT_ALLIANCE);
					setTextState (DiplomacyTextState.ACCEPT_ALLIANCE);
					break;
				
				default:
					throw new IOException ("Accepting proposal, but don't know what of proposal " + getTextState () + " is");
			}
			
			getClient ().getServerConnection ().sendMessageToServer (msg);
			initializeText ();
		});
		
		refuseProposalAction = new LoggingAction ((ev) ->
		{
			final RequestDiplomacyMessage msg = new RequestDiplomacyMessage ();
			msg.setTalkToPlayerID (getTalkingWizardID ());
			
			switch (getTextState ())
			{
				case PROPOSE_WIZARD_PACT:
					msg.setAction (DiplomacyAction.REJECT_WIZARD_PACT);
					break;

				case PROPOSE_ALLIANCE:
					msg.setAction (DiplomacyAction.REJECT_ALLIANCE);
					break;
				
				default:
					throw new IOException ("Refusing proposal, but don't know what of proposal " + getTextState () + " is");
			}
			
			getClient ().getServerConnection ().sendMessageToServer (msg);

			setTextState (DiplomacyTextState.WAITING_FOR_CHOICE);
			initializeText ();
		});
		
		proposeWizardPactAction = new LoggingAction ((ev) ->
		{
			final RequestDiplomacyMessage msg = new RequestDiplomacyMessage ();
			msg.setTalkToPlayerID (getTalkingWizardID ());
			msg.setAction (DiplomacyAction.PROPOSE_WIZARD_PACT);
			getClient ().getServerConnection ().sendMessageToServer (msg);
			
			setTextState (DiplomacyTextState.WAITING_FOR_RESPONSE);
			initializeText ();
		});
		
		proposeAllianceAction = new LoggingAction ((ev) ->
		{
			final RequestDiplomacyMessage msg = new RequestDiplomacyMessage ();
			msg.setTalkToPlayerID (getTalkingWizardID ());
			msg.setAction (DiplomacyAction.PROPOSE_ALLIANCE);
			getClient ().getServerConnection ().sendMessageToServer (msg);
			
			setTextState (DiplomacyTextState.WAITING_FOR_RESPONSE);
			initializeText ();
		});
		
		proposePeaceTreatyAction = new LoggingAction ((ev) -> {});
		proposeDeclareWarOnAnotherWizardAction = new LoggingAction ((ev) -> {});
		proposeBreakAllianceWithAnotherWizardAction = new LoggingAction ((ev) -> {});
		
		backToMainChoicesAction = new LoggingAction ((ev) ->
		{
			setTextState (DiplomacyTextState.MAIN_CHOICES);
			initializeText ();
		});
		
		tiredOfTalkingAction = new LoggingAction ((ev) ->
		{
			final RequestDiplomacyMessage msg = new RequestDiplomacyMessage ();
			msg.setTalkToPlayerID (getTalkingWizardID ());
			msg.setAction (DiplomacyAction.GROWN_IMPATIENT);
			getClient ().getServerConnection ().sendMessageToServer (msg);

			setTextState (DiplomacyTextState.NONE);
			setPortraitState (DiplomacyPortraitState.DISAPPEARING);
			initializeText ();
			initializePortrait ();
		});
		
		// Initialize the frame
		getFrame ().setDefaultCloseOperation (WindowConstants.HIDE_ON_CLOSE);
		
		// Initialize the content pane
		contentPane = new JPanel (new XmlLayoutManager (getDiplomacyLayout ()))
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				g.drawImage (background, 0, 0, background.getWidth () * 2, background.getHeight () * 2, null);
				
				if (MIRROR_STATES.contains (getPortraitState ()))
					g.drawImage (shiny, mirrorLocation.getLeft () + 12*2, mirrorLocation.getTop () + 12*2, shiny.getWidth () * 2, shiny.getHeight () * 2, null);
				
				try
				{
					final BufferedImage image = generateWizardPortrait ();
					if (image != null)
						g.drawImage (image, mirrorLocation.getLeft () + 11*2, mirrorLocation.getTop () + 11*2, null);
				}
				catch (final IOException e)
				{
					log.error (e, e);
				}
			}
		};
		
		// Clicks advance to the next state, close the window, or do nothing, depending on current state
		final MouseAdapter diplomacyMouseAdapter = new MouseAdapter ()
		{
			/**
			 * Clicks advance to the next state, close the window, or do nothing, depending on current state
			 */
			@Override
			public final void mouseClicked (@SuppressWarnings ("unused") final MouseEvent ev)
			{
				// If the diplomacy screen is only used for an initial meeting or other message which has no associated options for us to pick, then clicking just closes it
				// For any other use of the screen, assume there'll be some other option to close it with
				if ((!MIRROR_STATES.contains (getPortraitState ())) && (getMeetWizardMessage () != null))
				{
					setPortraitState (DiplomacyPortraitState.DISAPPEARING);
					initializePortrait ();
				}
				
				else if (getTextState () == DiplomacyTextState.GROWN_IMPATIENT)
				{
					setPortraitState (DiplomacyPortraitState.DISAPPEARING);
					initializePortrait ();
				}
				
				else if ((getTextState () == DiplomacyTextState.ACCEPT_TALK) ||
					(getTextState () == DiplomacyTextState.ACCEPT_WIZARD_PACT) ||
					(getTextState () == DiplomacyTextState.ACCEPT_ALLIANCE) ||
					(getTextState () == DiplomacyTextState.GENERIC_REFUSE))
				{
					if (getProposingWizardID () == getClient ().getOurPlayerID ())
						setTextState (DiplomacyTextState.MAIN_CHOICES);
					else
						setTextState (DiplomacyTextState.WAITING_FOR_CHOICE);
					
					initializeText ();
				}
					
				else if (getTextState () == DiplomacyTextState.REFUSED_TALK)
					try
					{
						setVisibleFalse ();
					}
					catch (final Exception e)
					{
						log.error (e, e);
					}
			}
		};
		
		// Text area
		final JPanel textPanelContainer = new JPanel (new BorderLayout ());
		textPanelContainer.setOpaque (false);
		contentPane.add (textPanelContainer, "frmDiplomacyText");

		textPanel = new JPanel (new GridBagLayout ());
		textPanel.setOpaque (false);
		textPanelContainer.add (textPanel, BorderLayout.NORTH);
		
		textPanelLayout = getDiplomacyLayout ().findComponent ("frmDiplomacyText");
		
		// Lock dialog size
		getFrame ().setContentPane (contentPane);
		getFrame ().setResizable (false);
		getFrame ().addMouseListener (diplomacyMouseAdapter);
		
		// Set up dialog with values that should've been set before making it visible
		updateRelationScore ();
		initializeText ();
		initializePortrait ();
	}
	
	/**
	 * Hides the frame, and takes care of any updates necessary as part of hiding it and returning to the overland map 
	 * @throws Exception If there is a problem
	 */
	private final void setVisibleFalse () throws Exception
	{
		setVisible (false);
		getOverlandMapRightHandPanel ().updateProductionTypesStoppingUsFromEndingTurn ();
		
		// Unblock the message that caused this
		if (getMeetWizardMessage () != null)
			getClient ().finishCustomDurationMessage (getMeetWizardMessage ());
		
		// Stop animation timer
		if ((timer != null) && (timer.isRunning ()))
		{
			timer.stop ();
			timer = null;
		}
		
		// Go back to the overland music
		if ((standardPhotoDef != null) && (standardPhotoDef.getDiplomacyPlayList () != null))
		{
			getMusicPlayer ().setShuffle (true);
			getMusicPlayer ().playPlayList (GraphicsDatabaseConstants.PLAY_LIST_OVERLAND_MUSIC, AnimationContainer.GRAPHICS_XML);
		}
	}
	
	/**
	 * Handles when we're asked to talk to a wizard and we accept
	 * 
	 * @param typeOfAccept Whether we accept politely or impatiently
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 */
	private final void acceptTalkTo (final DiplomacyAction typeOfAccept)
		throws JAXBException, XMLStreamException
	{
		if (talkingPlayer.getPlayerDescription ().getPlayerType () == PlayerType.HUMAN)
		{
			// Show popup menu to select mood to talk to the wizard with
			final JPopupMenu popup = new JPopupMenu ();
			
			getClient ().getClientDB ().getRelationScore ().forEach (rs ->
			{
				final String relationScoreName = getLanguageHolder ().findDescription (rs.getRelationScoreName ());
				final JMenuItem item = new JMenuItem (new LoggingAction (relationScoreName, (ev2) ->
				{
					final RequestDiplomacyMessage msg = new RequestDiplomacyMessage ();
					msg.setTalkToPlayerID (getTalkingWizardID ());
					msg.setAction (typeOfAccept);
					msg.setVisibleRelationScoreID (rs.getRelationScoreID ());
					getClient ().getServerConnection ().sendMessageToServer (msg);

					setTextState (DiplomacyTextState.WAITING_FOR_CHOICE);
					initializeText ();
				}));
				
				item.setFont (getSmallFont ());
				popup.add (item);								
			});
			
			popup.show (contentPane, contentPane.getWidth () / 2, (contentPane.getHeight () * 3) / 4);
		}
		else
		{
			// Don't need to ask for mood to talk to AI players with so just send it right away
			final RequestDiplomacyMessage msg = new RequestDiplomacyMessage ();
			msg.setTalkToPlayerID (getTalkingWizardID ());
			msg.setAction (typeOfAccept);
			getClient ().getServerConnection ().sendMessageToServer (msg);
			
			setTextState (DiplomacyTextState.WAITING_FOR_CHOICE);
			initializeText ();
		}
	}
	
	/**
	 * Must be called whenever a new conversation is starting, and talkingWizardID has just been set to a new value
	 * @throws IOException If there is a problem
	 */
	public final void initializeTalkingWizard () throws IOException
	{
		// Wizard portraits are stored exactly from the original LBXes (109x104) but stretched to compensate for original MoM using non-square 320x200 mode pixels (218x250).
		// Note the original MoM doesn't use the main wizard portraits (WIZARDS.LBX) here, it uses a frame of the talking animations (DIPLOMAC.LBX), which fades out
		// the edges of the pic so that it looks more like its in a mirror.  While it would have been easier to do that here, that would have meant no pic for overland
		// enchantments would be available for custom wizard portraits.  So this has to work just from the main wizard portrait.
		talkingWizardDetails = getKnownWizardUtils ().findKnownWizardDetails
			(getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getWizardDetails (), getTalkingWizardID (), "DiplomacyUI");
		
		talkingPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getTalkingWizardID (), "DiplomacyUI (T)");
		
		if (talkingWizardDetails.getStandardPhotoID () != null)
		{
			standardPhotoDef = getClient ().getClientDB ().findWizard (talkingWizardDetails.getStandardPhotoID (), "DiplomacyUI");
			talkingAnim = getClient ().getClientDB ().findAnimation (standardPhotoDef.getDiplomacyAnimation (), "DiplomacyUI");
			customPhoto = null;
		}
		
		else if (talkingWizardDetails.getCustomPhoto () != null)
		{
			customPhoto = ImageIO.read (new ByteArrayInputStream (talkingWizardDetails.getCustomPhoto ()));
			standardPhotoDef = null;
			talkingAnim = null;
		}
	}
	
	/**
	 * Updates the colour of the gargoyle eyes when the mood of the wizard we're talking to changes
	 * 
	 * @throws IOException If there is a problem
	 */
	public final void updateRelationScore () throws IOException
	{
		if (contentPane != null)
		{
			// Remove old eyes
			if (eyesLeftLabel != null)
				contentPane.remove (eyesLeftLabel);
	
			if (eyesRightLabel != null)
				contentPane.remove (eyesRightLabel);
			
			// Find updated relation
			if (getVisibleRelationScoreID () != null)
			{
				relationScore = getClient ().getClientDB ().findRelationScore (getVisibleRelationScoreID (), "DiplomacyUI");
				final Image eyesLeft = getUtils ().doubleSize (getUtils ().loadImage (relationScore.getEyesLeftImage ()));
				final Image eyesRight = getUtils ().doubleSize (getUtils ().loadImage (relationScore.getEyesRightImage ()));
				
				// Create new eyes
				eyesLeftLabel = getUtils ().createImage (eyesLeft);
				contentPane.add (eyesLeftLabel, "frmDiplomacyEyesLeft");
				
				eyesRightLabel = getUtils ().createImage (eyesRight);
				contentPane.add (eyesRightLabel, "frmDiplomacyEyesRight");
			}
			
			contentPane.validate ();
			contentPane.repaint ();
		}
	}
	
	/**
	 * Updates the text, buttons or other controls in the main portion of the screen according to whatever is set in textState
	 */
	public final void initializeText ()
	{
		if (contentPane != null)
		{
			List<LanguageTextVariant> variants = null;
			List<LanguageText> singular = null;
			final List<Component> componentsBelowText = new ArrayList<Component> ();
	
			try
			{
				switch (getTextState ())
				{
					case NONE:
						break;
						
					// Initial meeting message, based on personality of the talking wizard
					case INITIAL_CONTACT:
						final WizardPersonality personality;
						if (talkingWizardDetails.getWizardPersonalityID () != null)
							personality = getClient ().getClientDB ().findWizardPersonality (talkingWizardDetails.getWizardPersonalityID (), "initializeText");
						else
							// Human players have no personality set, in which case just use messages from the first one
							personality = getClient ().getClientDB ().getWizardPersonality ().get (0);
						
						if (!personality.getInitialMeetingPhrase ().isEmpty ())
							variants = personality.getInitialMeetingPhrase ();
						break;
						
					// We requested another wizard talk to us and waiting to see if they accept or refuse
					case WAITING_FOR_ACCEPT:
						singular = getLanguages ().getDiplomacyScreen ().getWaitingForAcceptTalkTo ();
						break;
						
					// Normal greeting, and 2 options to accept or reject talking to them
					case ACCEPT_OR_REFUSE_TALK:
						variants = getLanguages ().getDiplomacyScreen ().getNormalGreetingPhrase ();
		
						// Buttons to accept or refuse
						componentsBelowText.add (Box.createRigidArea (new Dimension (10, 10)));
						componentsBelowText.add (getUtils ().createTextOnlyButton (acceptTalkToAction, MomUIConstants.GOLD, getMediumFont ()));
						componentsBelowText.add (getUtils ().createTextOnlyButton (reluctantlyTalkToAction, MomUIConstants.GOLD, getMediumFont ()));
						componentsBelowText.add (getUtils ().createTextOnlyButton (refuseTalkToAction, MomUIConstants.GOLD, getMediumFont ()));
						break;
	
					// Normal or impatient greeting, based on their level of patience talking to us
					case ACCEPT_TALK:
						if ((getDiplomacyAction () == DiplomacyAction.ACCEPT_TALKING_IMPATIENT) && (!getLanguages ().getDiplomacyScreen ().getImpatientGreetingPhrase ().isEmpty ()))
							variants = getLanguages ().getDiplomacyScreen ().getImpatientGreetingPhrase ();
						else
							variants = getLanguages ().getDiplomacyScreen ().getNormalGreetingPhrase ();
						break;
						
					// We requested another wizard talk to us and they refused
					case REFUSED_TALK:
						variants = getLanguages ().getDiplomacyScreen ().getRefuseGreetingPhrase ();
						break;
	
					// Main list of choices when we are in control of the conversation, trading spells and making/breaking pacts to choose from and passing control of the conversation
					case MAIN_CHOICES:
						componentsBelowText.add (getUtils ().createTextOnlyButton (proposeTreatyAction, MomUIConstants.GOLD, getMediumFont ()));
						componentsBelowText.add (getUtils ().createTextOnlyButton (breakTreatyAction, MomUIConstants.GOLD, getMediumFont ()));
						componentsBelowText.add (getUtils ().createTextOnlyButton (offerTributeAction, MomUIConstants.GOLD, getMediumFont ()));
						componentsBelowText.add (getUtils ().createTextOnlyButton (exchangeSpellsAction, MomUIConstants.GOLD, getMediumFont ()));
						componentsBelowText.add (getUtils ().createTextOnlyButton (endConversationAction, MomUIConstants.GOLD, getMediumFont ()));
						break;
		
					// Other wizard has "main choices", so we're waiting to see what they choose
					case WAITING_FOR_CHOICE:
						singular = getLanguages ().getDiplomacyScreen ().getWaitingForProposal ();
						
						// Button to end conversation
						componentsBelowText.add (Box.createRigidArea (new Dimension (10, 10)));
						componentsBelowText.add (getUtils ().createTextOnlyButton (tiredOfTalkingAction, MomUIConstants.GOLD, getMediumFont ()));
						break;
						
					// We made a proposal to the other wizard and waiting for them to accept/reject it
					case WAITING_FOR_RESPONSE:
						singular = getLanguages ().getDiplomacyScreen ().getWaitingForAcceptanceOfProposal ();
						break;
							
					// Pick a kind of treaty to propose
					case PROPOSE_TREATY:
						componentsBelowText.add (getUtils ().createTextOnlyButton (proposeWizardPactAction, MomUIConstants.GOLD, getMediumFont ()));
						componentsBelowText.add (getUtils ().createTextOnlyButton (proposeAllianceAction, MomUIConstants.GOLD, getMediumFont ()));
						componentsBelowText.add (getUtils ().createTextOnlyButton (proposePeaceTreatyAction, MomUIConstants.GOLD, getMediumFont ()));
						componentsBelowText.add (getUtils ().createTextOnlyButton (proposeDeclareWarOnAnotherWizardAction, MomUIConstants.GOLD, getMediumFont ()));
						componentsBelowText.add (getUtils ().createTextOnlyButton (proposeBreakAllianceWithAnotherWizardAction, MomUIConstants.GOLD, getMediumFont ()));
						componentsBelowText.add (getUtils ().createTextOnlyButton (backToMainChoicesAction, MomUIConstants.GOLD, getMediumFont ()));
						break;

					// General "no" message in response to some proposal
					case GENERIC_REFUSE:
						variants = getLanguages ().getDiplomacyScreen ().getGenericRefusePhrase ();
						break;
						
					// Other wizard proposes a wizard pact with us
					case PROPOSE_WIZARD_PACT:
						variants = getLanguages ().getDiplomacyScreen ().getProposeWizardPactPhrase ();
						
						// Buttons to accept or refuse
						componentsBelowText.add (Box.createRigidArea (new Dimension (10, 10)));
						componentsBelowText.add (getUtils ().createTextOnlyButton (acceptProposalAction, MomUIConstants.GOLD, getMediumFont ()));
						componentsBelowText.add (getUtils ().createTextOnlyButton (refuseProposalAction, MomUIConstants.GOLD, getMediumFont ()));
						break;

					// Other wizard proposes an alliance with us
					case PROPOSE_ALLIANCE:
						variants = getLanguages ().getDiplomacyScreen ().getProposeAlliancePhrase ();
						
						// Buttons to accept or refuse
						componentsBelowText.add (Box.createRigidArea (new Dimension (10, 10)));
						componentsBelowText.add (getUtils ().createTextOnlyButton (acceptProposalAction, MomUIConstants.GOLD, getMediumFont ()));
						componentsBelowText.add (getUtils ().createTextOnlyButton (refuseProposalAction, MomUIConstants.GOLD, getMediumFont ()));
						break;
						
					// Accepted wizard pact
					case ACCEPT_WIZARD_PACT:
						variants = getLanguages ().getDiplomacyScreen ().getAcceptWizardPactPhrase ();
						break;
						
					// Accepted alliance
					case ACCEPT_ALLIANCE:
						variants = getLanguages ().getDiplomacyScreen ().getAcceptAlliancePhrase ();
						break;

					// Other wizard got fed up of us making proposals to them and ended the conversation
					case GROWN_IMPATIENT:
						variants = getLanguages ().getDiplomacyScreen ().getGrownImpatientPhrase ();
						break;
				}
		
				if ((variants != null) && (!variants.isEmpty ()))
				{
					final LanguageTextVariant variant = variants.get (getRandomUtils ().nextInt (variants.size ()));
					singular = variant.getTextVariant ();
				}
				
				if ((singular != null) && (!singular.isEmpty ()))
				{
					final PlayerPublicDetails ourWizard = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getClient ().getOurPlayerID (), "initializeText (O)");
					
					final String text = getLanguageHolder ().findDescription (singular).replaceAll
						("OUR_PLAYER_NAME", getWizardClientUtils ().getPlayerName (ourWizard)).replaceAll
						("TALKING_PLAYER_NAME", getWizardClientUtils ().getPlayerName (talkingPlayer));
						
					showText (text, componentsBelowText);
				}
				else
					showText (null, componentsBelowText);
			}
			catch (final Exception e)
			{
				log.error (e, e);
			}
		}
	}
	
	/**
	 * Initializes the currently set portrait state, setting off a timer if necessary
	 */
	public final void initializePortrait ()
	{
		if (contentPane != null)
		{
			// If there's an old timer running then stop it
			if ((timer != null) && (timer.isRunning ()))
			{
				timer.stop ();
				timer = null;
			}
			
			// Initialize animation
			frameNumber = 0;
			final int frameCount = getFrameCount ();
			if (frameCount > 0)
			{
				timer = new Timer (TIMER_TICKS_MS, (ev2) ->
				{
					frameNumber = frameNumber + 1;
					if (frameNumber >= frameCount)
					{
						if (timer != null)
						{
							timer.stop ();					
							timer = null;
						}
						frameNumber = 0;
						
						if (getPortraitState () == DiplomacyPortraitState.APPEARING)
						{
							// Start music
							if (standardPhotoDef != null)
							{
								final String playList;
								if ((relationScore.getMood () == WizardPortraitMood.MAD) && (standardPhotoDef.getMadPlayList () != null))
									playList = standardPhotoDef.getMadPlayList ();
								else
									playList = standardPhotoDef.getDiplomacyPlayList ();
								
								if (playList != null)
									try
									{
										getMusicPlayer ().setShuffle (false);
										getMusicPlayer ().playPlayList (playList, AnimationContainer.COMMON_XML);
									}
									catch (final Exception e)
									{
										log.error (e, e);
									}
							}
							
							setPortraitState (DiplomacyPortraitState.TALKING);
							initializePortrait ();
							
							// Figure out which text to show when the wizard first appears
							if (getMeetWizardMessage () != null)
							{
								setTextState (DiplomacyTextState.INITIAL_CONTACT);
								initializeText ();
							}
							
							else if (getDiplomacyAction () != null)
							{
								switch (getDiplomacyAction ())
								{
									case INITIATE_TALKING:
										setTextState (DiplomacyTextState.ACCEPT_OR_REFUSE_TALK);
										initializeText ();
										break;
										
									case ACCEPT_TALKING:
									case ACCEPT_TALKING_IMPATIENT:
										setTextState (DiplomacyTextState.ACCEPT_TALK);
										initializeText ();
										break;
										
									default:
										log.warn ("DiplomacyUI doesn't know what text to show after portrait appears for action " + getDiplomacyAction ());
								}
							}
						}
						else if (getPortraitState () == DiplomacyPortraitState.TALKING)
						{
							// This is really here for the unit test which will have no message set; normally advanced by clicking
							if ((getMeetWizardMessage () == null) && (getDiplomacyAction () == null))
							{
								setPortraitState (DiplomacyPortraitState.DISAPPEARING);
								initializePortrait ();
							}
							
							// For any other "done talking", just revert back to the normal/happy/mad face
							else
							{
								switch (relationScore.getMood ())
								{
									case HAPPY:
										setPortraitState (DiplomacyPortraitState.HAPPY);
										break;
									case NORMAL:
										setPortraitState (DiplomacyPortraitState.NORMAL);
										break;
									case MAD:
										setPortraitState (DiplomacyPortraitState.MAD);
										break;
								}
								initializePortrait ();
							}
						}
						else if (getPortraitState () == DiplomacyPortraitState.DISAPPEARING)
							try
							{
								setVisibleFalse ();
							}
							catch (final Exception e)
							{
								log.error (e, e);
							}
					}
					
					contentPane.repaint ();
				});
				
				timer.start ();
			}
		}
	}
	
	/**
	 * @return Generated wizard portrait for the current state and frame
	 * @throws IOException If there is a problem
	 */
	private final BufferedImage generateWizardPortrait () throws IOException
	{
		if (getPortraitState () == DiplomacyPortraitState.MIRROR)
			return null;
			
		// It might already be in the cache
		final String cacheKey;
		if ((customPhoto != null) && (!MIRROR_STATES.contains (getPortraitState ())))
			cacheKey = getTalkingWizardID () + "-" + DiplomacyPortraitState.NORMAL.toString ();
		else if (!ANIMATED_STATES.contains (getPortraitState ()))
			cacheKey = getTalkingWizardID () + "-" + getPortraitState ().toString ();
		else if (MIRROR_STATES.contains (getPortraitState ()))
		{
			final String relation;
			if ((relationScore != null) && (relationScore.getMood () == WizardPortraitMood.HAPPY) && (standardPhotoDef != null) && (standardPhotoDef.getHappyImageFile () != null))
				relation = DiplomacyPortraitState.HAPPY.toString ();
			else if ((relationScore != null) && (relationScore.getMood () == WizardPortraitMood.MAD) && (standardPhotoDef != null) && (standardPhotoDef.getMadImageFile () != null))
				relation = DiplomacyPortraitState.MAD.toString ();
			else
				relation = DiplomacyPortraitState.NORMAL.toString ();
		
			if (getPortraitState () == DiplomacyPortraitState.DISAPPEARING)
				cacheKey = getTalkingWizardID () + "-" + DiplomacyPortraitState.APPEARING.toString () + "-" + relation + "-" + (APPEARING_TICKS - 1 - getFrameNumber ());
			else
				cacheKey = getTalkingWizardID () + "-" + getPortraitState ().toString () + "-" + relation + "-" + getFrameNumber ();
		}
		else
			cacheKey = getTalkingWizardID () + "-" + getPortraitState ().toString () + "-" + getFrameNumber ();
		
		BufferedImage image = wizardPortraits.get (cacheKey);
		if (image == null)
		{
			// Generate new image
			final BufferedImage unscaledPortrait;
			if (customPhoto != null)
			{
				if (getPortraitState () == DiplomacyPortraitState.APPEARING)
				{
					final int alpha = (255 * getFrameNumber ()) / APPEARING_TICKS;
					unscaledPortrait = getUtils ().multiplyImageByColourAndAlpha (customPhoto, (alpha << 24) | 0xFFFFFF);
				}
				else
					unscaledPortrait = customPhoto;
			}
			
			else if (standardPhotoDef != null)
			{
				final String moodPortraitFile;
				if ((relationScore != null) && (relationScore.getMood () == WizardPortraitMood.HAPPY) && (standardPhotoDef.getHappyImageFile () != null))
					moodPortraitFile = standardPhotoDef.getHappyImageFile ();
				else if ((relationScore != null) && (relationScore.getMood () == WizardPortraitMood.MAD) && (standardPhotoDef.getMadImageFile () != null))
					moodPortraitFile = standardPhotoDef.getMadImageFile ();
				else
					moodPortraitFile = standardPhotoDef.getPortraitImageFile ();
				
				if ((getPortraitState () == DiplomacyPortraitState.HAPPY) && (standardPhotoDef.getHappyImageFile () != null))
					unscaledPortrait = getUtils ().loadImage (standardPhotoDef.getHappyImageFile ());
				
				else if ((getPortraitState () == DiplomacyPortraitState.MAD) && (standardPhotoDef.getMadImageFile () != null))
					unscaledPortrait = getUtils ().loadImage (standardPhotoDef.getMadImageFile ());
				
				else if ((getPortraitState () == DiplomacyPortraitState.TALKING) && (talkingAnim != null) && (frameNumber >= 0) && (frameNumber < talkingAnim.getFrame ().size ()))
					unscaledPortrait = getUtils ().loadImage (talkingAnim.getFrame ().get (frameNumber).getImageFile ());	
				
				else if (getPortraitState () == DiplomacyPortraitState.APPEARING)
				{
					final int alpha = (255 * getFrameNumber ()) / APPEARING_TICKS;
					unscaledPortrait = getUtils ().multiplyImageByColourAndAlpha (getUtils ().loadImage (moodPortraitFile),
						(alpha << 24) | 0xFFFFFF);
				}
				
				else
					unscaledPortrait = getUtils ().loadImage (moodPortraitFile);
			}
			else
				throw new MomException ("Player ID " + getTalkingWizardID () + " has neither a custom or standard photo");
			
			final Image unclippedPortrait = unscaledPortrait.getScaledInstance
				(GraphicsDatabaseConstants.WIZARD_PORTRAIT_SIZE.width, GraphicsDatabaseConstants.WIZARD_PORTRAIT_SIZE.height, Image.SCALE_SMOOTH);
			
			// Cut the square corners off the wizard's photo
			image = getSpellClientUtils ().mergeImages (unclippedPortrait, getUtils ().loadImage (fadeAnim.getFrame ().get
				(fadeAnim.getFrame ().size () - 1).getImageFile ()), 0, -5*2);
			
			// Put in cache
			wizardPortraits.put (cacheKey, image);
		}
		
		return image;
	}
	
	/**
	 * @return Number of frames in the animation that's currently being shown; or 0 if it isn't an animation
	 */
	private final int getFrameCount ()
	{
		final int count;
		switch (getPortraitState ())
		{
			case APPEARING:
			case DISAPPEARING:
				count = APPEARING_TICKS;
				break;
				
			case TALKING:
				count = (talkingAnim != null) ? talkingAnim.getFrame ().size () : 0;
				break;
			
			default:
				count = 0;
		}
		return count;
	}
	
	/**
	 * Updates the contents of the text panel with the specified components
	 * 
	 * @param components Components to place within the text panel
	 */
	private final void updateTextPanel (final List<Component> components)
	{
		// Remove old components
		textPanelComponents.forEach (c -> textPanel.remove (c));
		textPanelComponents.clear ();
		
		// Add new components
		int y = 0;
		for (final Component component : components)
		{
			textPanel.add (component, getUtils ().createConstraintsNoFill (0, y, 1, 1, NO_INSET, GridBagConstraintsNoFill.CENTRE));
			textPanelComponents.add (component);
			y++;
		}
		
		contentPane.validate ();
		contentPane.repaint ();
	}
	
	/**
	 * @param text Text to show in the text panel; can be left null
	 * @param componentsBelowText Any components to add below the text lines
	 */
	private final void showText (final String text, final List<Component> componentsBelowText)
	{
		final List<Component> components = new ArrayList<Component> ();
		if (text != null)
			components.addAll (getUtils ().wrapLabels (getMediumFont (), MomUIConstants.GOLD, text, textPanelLayout.getWidth ()));
		
		if (componentsBelowText != null)
			components.addAll (componentsBelowText);
		
		updateTextPanel (components);
	}

	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		getFrame ().setTitle (getLanguageHolder ().findDescription (getLanguages ().getDiplomacyScreen ().getTitle ()));

		try
		{
			final PlayerPublicDetails ourWizard = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getClient ().getOurPlayerID (), "languageChanged (O)");
			
			acceptTalkToAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getDiplomacyScreen ().getAcceptTalkTo ()).replaceAll
				("OUR_PLAYER_NAME", getWizardClientUtils ().getPlayerName (ourWizard)).replaceAll
				("TALKING_PLAYER_NAME", getWizardClientUtils ().getPlayerName (talkingPlayer)));
			
			reluctantlyTalkToAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getDiplomacyScreen ().getReluctantlyTalkTo ()).replaceAll
				("OUR_PLAYER_NAME", getWizardClientUtils ().getPlayerName (ourWizard)).replaceAll
				("TALKING_PLAYER_NAME", getWizardClientUtils ().getPlayerName (talkingPlayer)));
				
			refuseTalkToAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getDiplomacyScreen ().getRefuseTalkTo ()).replaceAll
				("OUR_PLAYER_NAME", getWizardClientUtils ().getPlayerName (ourWizard)).replaceAll
				("TALKING_PLAYER_NAME", getWizardClientUtils ().getPlayerName (talkingPlayer)));
			
			proposeTreatyAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getDiplomacyScreen ().getProposeTreaty ()).replaceAll
				("OUR_PLAYER_NAME", getWizardClientUtils ().getPlayerName (ourWizard)).replaceAll
				("TALKING_PLAYER_NAME", getWizardClientUtils ().getPlayerName (talkingPlayer)));

			breakTreatyAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getDiplomacyScreen ().getBreakTreaty ()).replaceAll
				("OUR_PLAYER_NAME", getWizardClientUtils ().getPlayerName (ourWizard)).replaceAll
				("TALKING_PLAYER_NAME", getWizardClientUtils ().getPlayerName (talkingPlayer)));

			offerTributeAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getDiplomacyScreen ().getOfferTribute ()).replaceAll
				("OUR_PLAYER_NAME", getWizardClientUtils ().getPlayerName (ourWizard)).replaceAll
				("TALKING_PLAYER_NAME", getWizardClientUtils ().getPlayerName (talkingPlayer)));

			exchangeSpellsAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getDiplomacyScreen ().getExchangeSpells ()).replaceAll
				("OUR_PLAYER_NAME", getWizardClientUtils ().getPlayerName (ourWizard)).replaceAll
				("TALKING_PLAYER_NAME", getWizardClientUtils ().getPlayerName (talkingPlayer)));

			endConversationAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getDiplomacyScreen ().getEndConversation ()).replaceAll
				("OUR_PLAYER_NAME", getWizardClientUtils ().getPlayerName (ourWizard)).replaceAll
				("TALKING_PLAYER_NAME", getWizardClientUtils ().getPlayerName (talkingPlayer)));
			
			acceptProposalAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getDiplomacyScreen ().getAcceptProposal ()).replaceAll
				("OUR_PLAYER_NAME", getWizardClientUtils ().getPlayerName (ourWizard)).replaceAll
				("TALKING_PLAYER_NAME", getWizardClientUtils ().getPlayerName (talkingPlayer)));
			
			refuseProposalAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getDiplomacyScreen ().getRefuseProposal ()).replaceAll
				("OUR_PLAYER_NAME", getWizardClientUtils ().getPlayerName (ourWizard)).replaceAll
				("TALKING_PLAYER_NAME", getWizardClientUtils ().getPlayerName (talkingPlayer)));
			
			proposeWizardPactAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getDiplomacyScreen ().getProposeWizardPact ()).replaceAll
				("OUR_PLAYER_NAME", getWizardClientUtils ().getPlayerName (ourWizard)).replaceAll
				("TALKING_PLAYER_NAME", getWizardClientUtils ().getPlayerName (talkingPlayer)));
			
			proposeAllianceAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getDiplomacyScreen ().getProposeAlliance ()).replaceAll
				("OUR_PLAYER_NAME", getWizardClientUtils ().getPlayerName (ourWizard)).replaceAll
				("TALKING_PLAYER_NAME", getWizardClientUtils ().getPlayerName (talkingPlayer)));
			
			proposePeaceTreatyAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getDiplomacyScreen ().getProposePeaceTreaty ()).replaceAll
				("OUR_PLAYER_NAME", getWizardClientUtils ().getPlayerName (ourWizard)).replaceAll
				("TALKING_PLAYER_NAME", getWizardClientUtils ().getPlayerName (talkingPlayer)));
			
			proposeDeclareWarOnAnotherWizardAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getDiplomacyScreen ().getProposeDeclareWarOnAnotherWizard ()).replaceAll
				("OUR_PLAYER_NAME", getWizardClientUtils ().getPlayerName (ourWizard)).replaceAll
				("TALKING_PLAYER_NAME", getWizardClientUtils ().getPlayerName (talkingPlayer)));
			
			proposeBreakAllianceWithAnotherWizardAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getDiplomacyScreen ().getProposeBreakAllianceWithAnotherWizard ()).replaceAll
				("OUR_PLAYER_NAME", getWizardClientUtils ().getPlayerName (ourWizard)).replaceAll
				("TALKING_PLAYER_NAME", getWizardClientUtils ().getPlayerName (talkingPlayer)));
			
			backToMainChoicesAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getDiplomacyScreen ().getBackToMainChoices ()).replaceAll
				("OUR_PLAYER_NAME", getWizardClientUtils ().getPlayerName (ourWizard)).replaceAll
				("TALKING_PLAYER_NAME", getWizardClientUtils ().getPlayerName (talkingPlayer)));
			
			tiredOfTalkingAction.putValue (Action.NAME, getLanguageHolder ().findDescription (getLanguages ().getDiplomacyScreen ().getTiredOfWaitingForProposal ()).replaceAll
				("OUR_PLAYER_NAME", getWizardClientUtils ().getPlayerName (ourWizard)).replaceAll
				("TALKING_PLAYER_NAME", getWizardClientUtils ().getPlayerName (talkingPlayer)));
		}
		catch (final IOException e)
		{
			log.error (e, e);
		}
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getDiplomacyLayout ()
	{
		return diplomacyLayout;
	}

	/**
	 * @param x XML layout
	 */
	public final void setDiplomacyLayout (final XmlLayoutContainerEx x)
	{
		diplomacyLayout = x;
	}

	/**
	 * @return Graphics database
	 */
	public final GraphicsDatabaseEx getGraphicsDB ()
	{
		return graphicsDB;
	}

	/**
	 * @param db Graphics database
	 */
	public final void setGraphicsDB (final GraphicsDatabaseEx db)
	{
		graphicsDB = db;
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
	 * @return Client-side spell utils
	 */
	public final SpellClientUtils getSpellClientUtils ()
	{
		return spellClientUtils;
	}

	/**
	 * @param utils Client-side spell utils
	 */
	public final void setSpellClientUtils (final SpellClientUtils utils)
	{
		spellClientUtils = utils;
	}

	/**
	 * @return Music player
	 */
	public final AudioPlayer getMusicPlayer ()
	{
		return musicPlayer;
	}

	/**
	 * @param player Music player
	 */
	public final void setMusicPlayer (final AudioPlayer player)
	{
		musicPlayer = player;
	}
	
	/**
	 * @return Medium font
	 */
	public final Font getMediumFont ()
	{
		return mediumFont;
	}
	
	/**
	 * @param font Medium font
	 */
	public final void setMediumFont (final Font font)
	{
		mediumFont = font;
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
	 * @return Random utils
	 */
	public final RandomUtils getRandomUtils ()
	{
		return randomUtils;
	}

	/**
	 * @param utils Random utils
	 */
	public final void setRandomUtils (final RandomUtils utils)
	{
		randomUtils = utils;
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
	 * @return Overland map right hand panel showing economy etc
	 */
	public final OverlandMapRightHandPanel getOverlandMapRightHandPanel ()
	{
		return overlandMapRightHandPanel;
	}

	/**
	 * @param panel Overland map right hand panel showing economy etc
	 */
	public final void setOverlandMapRightHandPanel (final OverlandMapRightHandPanel panel)
	{
		overlandMapRightHandPanel = panel;
	}
	
	/**
	 * @return Which wizard we are talking to
	 */
	public final int getTalkingWizardID ()
	{
		return talkingWizardID;
	}

	/**
	 * @param w Which wizard we are talking to
	 */
	public final void setTalkingWizardID (final int w)
	{
		talkingWizardID = w;
	}

	/**
	 * @return Which wizard is the one controlling making proposals (and which one is waiting for the other side to make a proposal)
	 */
	public final int getProposingWizardID ()
	{
		return proposingWizardID;
	}

	/**
	 * @param w Which wizard is the one controlling making proposals (and which one is waiting for the other side to make a proposal)
	 */
	public final void setProposingWizardID (final int w)
	{
		proposingWizardID = w;
	}
	
	/**
	 * @return Which state to draw the portrait in
	 */
	public final DiplomacyPortraitState getPortraitState ()
	{
		return portraitState;
	}

	/**
	 * @param s Which state to draw the portrait in
	 */
	public final void setPortraitState (final DiplomacyPortraitState s)
	{
		portraitState = s;
	}

	/**
	 * @return Which text to display
	 */
	public final DiplomacyTextState getTextState ()
	{
		return textState;
	}

	/**
	 * @param s Which text to display
	 */
	public final void setTextState (final DiplomacyTextState s)
	{
		textState = s;
	}
	
	/**
	 * @return Current frame number to display
	 */
	public final int getFrameNumber ()
	{
		return frameNumber;
	}
	
	/**
	 * @param f Current frame number to display
	 */
	public final void setFrameNumber (final int f)
	{
		frameNumber = f;
	}

	/**
	 * @return The meet wizard message we're showing the animation for; either this or diplomacyAction must be set but not both
	 */
	public final MeetWizardMessageImpl getMeetWizardMessage ()
	{
		return meetWizardMessage;
	}

	/**
	 * @param m The meet wizard message we're showing the animation for; either this or diplomacyAction must be set but not both
	 */
	public final void setMeetWizardMessage (final MeetWizardMessageImpl m)
	{
		meetWizardMessage = m;
	}

	/**
	 * @return The last action we received; either this or meetWizardMessage must be set but not both
	 */
	public final DiplomacyAction getDiplomacyAction ()
	{
		return diplomacyAction;
	}

	/**
	 * @param a The last action we received; either this or meetWizardMessage must be set but not both
	 */
	public final void setDiplomacyAction (final DiplomacyAction a)
	{
		diplomacyAction = a;
	}

	/**
	 * @return Relation to use to decide the eye colour, facial expression and music
	 */
	public final String getVisibleRelationScoreID ()
	{
		return visibleRelationScoreID;
	}

	/**
	 * @param r Relation to use to decide the eye colour, facial expression and music
	 */
	public final void setVisibleRelationScoreID (final String r)
	{
		visibleRelationScoreID = r;
	}
}