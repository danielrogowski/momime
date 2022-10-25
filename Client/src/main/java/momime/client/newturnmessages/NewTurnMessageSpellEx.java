package momime.client.newturnmessages;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;
import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.MomLanguagesEx;
import momime.client.ui.MomUIConstants;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.dialogs.UnitRowDisplayUI;
import momime.client.ui.frames.MagicSlidersUI;
import momime.client.ui.frames.PrototypeFrameCreator;
import momime.client.ui.frames.SpellBookUI;
import momime.client.ui.frames.WizardsUI;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.client.ui.panels.OverlandMapRightHandPanelBottom;
import momime.client.ui.panels.OverlandMapRightHandPanelTop;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.client.utils.WizardClientUtils;
import momime.common.database.LanguageText;
import momime.common.database.Spell;
import momime.common.messages.MemoryUnit;
import momime.common.messages.NewTurnMessageSpell;
import momime.common.messages.NewTurnMessageTypeID;
import momime.common.messages.OverlandMapCityData;
import momime.common.messages.clienttoserver.TargetSpellMessage;
import momime.common.utils.ExpandUnitDetails;
import momime.common.utils.ExpandedUnitDetails;
import momime.common.utils.KindOfSpell;
import momime.common.utils.KindOfSpellUtils;
import momime.common.utils.SpellTargetingUtils;
import momime.common.utils.TargetSpellResult;
import momime.common.utils.UnitUtils;

/**
 * NTM about a spell, either one we've researched or need to pick a target for
 */
public final class NewTurnMessageSpellEx extends NewTurnMessageSpell
	implements NewTurnMessageExpiration, NewTurnMessageSimpleUI, NewTurnMessageClickable, NewTurnMessageMusic, NewTurnMessageMustBeAnswered
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (NewTurnMessageSpellEx.class);
	
	/** Current status of this NTM */
	private NewTurnMessageStatus status;
	
	/** Small font */
	private Font smallFont;
	
	/** Language database holder */
	private LanguageDatabaseHolder languageHolder;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Spell book */
	private SpellBookUI spellBookUI;
	
	/** Overland map right hand panel showing economy etc */
	private OverlandMapRightHandPanel overlandMapRightHandPanel;
	
	/** Unit utils */
	private UnitUtils unitUtils;
	
	/** Client-side unit utils */
	private UnitClientUtils unitClientUtils;
	
	/** Did we cancel targeting the spell? */
	private boolean targetingCancelled;
	
	/** Chosen city target */
	private MapCoordinates3DEx targetedCity;
	
	/** Chosen unit target */
	private Integer targetedUnitURN;
	
	/** Chosen player target */
	private Integer targetedPlayerID;
	
	/** Magic sliders screen */
	private MagicSlidersUI magicSlidersUI;
	
	/** Kind of spell utils */
	private KindOfSpellUtils kindOfSpellUtils;
	
	/** Wizards UI */
	private WizardsUI wizardsUI;
	
	/** Prototype frame creator */
	private PrototypeFrameCreator prototypeFrameCreator;

	/** Methods that determine whether something is a valid target for a spell */
	private SpellTargetingUtils spellTargetingUtils;
	
	/** expandUnitDetails method */
	private ExpandUnitDetails expandUnitDetails;
	
	/** Wizard client utils */
	private WizardClientUtils wizardClientUtils;
	
	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;
	
	/**
	 * @return One of the SORT_ORDER_ constants, indicating the sort order/title category to group this message under
	 */
	@Override
	public final NewTurnMessageSortOrder getSortOrder ()
	{
		return NewTurnMessageSortOrder.SORT_ORDER_SPELLS;
	}

	/**
	 * @return Name of music file on the classpath to play when this NTM is displayed; null if this message has no music associated
	 */
	@Override
	public final String getMusicResourceName ()
	{
		// Only "spell researched" plays music
		final String music;
		if (getMsgType () == NewTurnMessageTypeID.RESEARCHED_SPELL)
			music = "/momime.client.music/MUSIC_040 - ResearchedASpell-DetectMagic-Awareness-GreatUnsummoning-CharmOfLife.mp3";
		else
			music = null;
		
		return music;
	}
	
	/**
	 * @return Image to draw for this NTM, or null to display only text
	 */
	@Override
	public final Image getImage ()
	{
		return null;
	}
	
	/**
	 * @return Text to display for this NTM
	 */
	@Override
	public final String getText ()
	{
		// Text varies according to the message type
		String text = null;
		try
		{
			final String spellName = getLanguageHolder ().findDescription (getClient ().getClientDB ().findSpell (getSpellID (), "NewTurnMessageSpellEx (O)").getSpellName ());
			
			switch (getMsgType ())
			{
				// Finished researching a spell, so need to pick another one
				case RESEARCHED_SPELL:
				{
					final List<LanguageText> languageText;
					if (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched () == null)
						languageText = getLanguages ().getNewTurnMessages ().getResearchNotChosen ();
					else
						languageText = getLanguages ().getNewTurnMessages ().getResearchChosen ();
					
					final String newSpellID = (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched () == null) ? "" : getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched ();
					final String newSpellName = (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched () == null) ? null : getLanguageHolder ().findDescription
						(getClient ().getClientDB ().findSpell (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched (), "NewTurnMessageSpellEx (N)").getSpellName ());
					
					text = getLanguageHolder ().findDescription (languageText).replaceAll
						("OLD_SPELL_NAME", (spellName != null) ? spellName : getSpellID ()).replaceAll
						("NEW_SPELL_NAME", (newSpellName != null) ? newSpellName : newSpellID);
					break;
				}
					
				// Cast a city/unit enchantment/curse, so need to pick a target for it
				case TARGET_SPELL:
				{
					final List<LanguageText> languageText;;
					String target = null;
					
					// Cancelled spell
					if (isTargetingCancelled ())
						languageText = (getStatus () == NewTurnMessageStatus.BEFORE_OUR_TURN_BEGAN) ?
							getLanguages ().getNewTurnMessages ().getTargetSpellCancelledLastTurn () : getLanguages ().getNewTurnMessages ().getTargetSpellCancelled ();
							
					// Still need to choose a target
					else if ((getTargetedCity () == null) && (getTargetedUnitURN () == null) && (getTargetedPlayerID () == null))
						languageText = getLanguages ().getNewTurnMessages ().getTargetSpell ();
						
					// Target chosen
					else
					{
						// Does the target have a name, or is it a nameless location?
						if ((getTargetedCity () != null) && (getTargetedCity ().getX () >= 0) && (getTargetedCity ().getY () >= 0) && (getTargetedCity ().getZ () >= 0))
						{
							final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
								(getTargetedCity ().getZ ()).getRow ().get (getTargetedCity ().getY ()).getCell ().get (getTargetedCity ().getX ()).getCityData ();
							if (cityData != null)
								target = cityData.getCityName ();
						}
						else if (getTargetedUnitURN () != null)
						{
							final MemoryUnit unit = getUnitUtils ().findUnitURN (getTargetedUnitURN (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ());
							if (unit != null)
								target = getUnitClientUtils ().getUnitName (unit, UnitNameType.A_UNIT_NAME);
						}
						else if (getTargetedPlayerID () != null)
						{
							final PlayerPublicDetails targetedPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getTargetedPlayerID ());
							if (targetedPlayer != null)
								target = getWizardClientUtils ().getPlayerName (targetedPlayer);
						}

						// Do we know name for the target?
						if (target == null)
							languageText = (getStatus () == NewTurnMessageStatus.BEFORE_OUR_TURN_BEGAN) ?
								getLanguages ().getNewTurnMessages ().getTargetSpellChosenLastTurnUnnamed () : getLanguages ().getNewTurnMessages ().getTargetSpellChosenUnnamed ();
						else
							languageText = (getStatus () == NewTurnMessageStatus.BEFORE_OUR_TURN_BEGAN) ?
								getLanguages ().getNewTurnMessages ().getTargetSpellChosenLastTurnNamed () : getLanguages ().getNewTurnMessages ().getTargetSpellChosenNamed ();
					}
					
					// Finally know which language entry to look up
					text = getLanguageHolder ().findDescription (languageText).replaceAll
						("SPELL_NAME", (spellName != null) ? spellName : getSpellID ());
					
					if (target != null)
						text = text.replaceAll ("TARGET", target);
					
					break;
				}
					
				default:
					text = null;
			}
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		return text;
	}
	
	/**
	 * @return Whether the user has acted on this message yet
	 */
	@Override
	public final boolean isAnswered ()
	{
		// Only applicable for "target spell"
		final boolean answered;
		if (getMsgType () == NewTurnMessageTypeID.TARGET_SPELL)
			answered = (isTargetingCancelled ()) || (getTargetedCity () != null) || (getTargetedUnitURN () != null) || (getTargetedPlayerID () != null);
		else
			answered = true;
		
		return answered;
	}
	
	/**
	 * @return Text to display when we can't end turn because this NTM hasn't been answered yet 
	 */
	@Override
	public final List<LanguageText> getCannotEndTurnText ()
	{
		return getLanguages ().getOverlandMapScreen ().getMapRightHandBar ().getCannotEndTurnNewTurnMessagesSpell ();
	}
	
	/**
	 * @return Image to display when we can't end turn because this NTM hasn't been answered yet
	 */
	@Override
	public final String getCannotEndTurnImageFilename ()
	{
		return "/momime.client.graphics/ui/overland/rightHandPanel/cannotEndTurnDueToNTMs-spell.png";
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
	 * Clicking on research messages brings up spell book so we can pick next spell to research.
	 * Clicking on targeting messages sets up overland map UI to target the spell.
	 * 
	 * @throws Exception If there is a problem
	 */
	@Override
	public final void clicked () throws Exception
	{
		switch (getMsgType ())
		{
			// Finished researching a spell, so open up spell book to pick another one
			case RESEARCHED_SPELL:
				getSpellBookUI ().setVisible (true);
				break;
		
			// Cast a city/unit enchantment/curse, so need to pick a target for it
			case TARGET_SPELL:
				if ((!isTargetingCancelled ()) && (getTargetedCity () == null) && (getTargetedUnitURN () == null) && (getTargetedPlayerID () == null))
				{
					getOverlandMapRightHandPanel ().setTargetSpell (this);
					getOverlandMapRightHandPanel ().setTop (OverlandMapRightHandPanelTop.TARGET_SPELL);
					getOverlandMapRightHandPanel ().setBottom (OverlandMapRightHandPanelBottom.CANCEL);
					
					// Anything special to do for this particular spell?
					final Spell spell = getClient ().getClientDB ().findSpell (getSpellID (), "NewTurnMessageSpellEx (C)");
					final KindOfSpell kind = getKindOfSpellUtils ().determineKindOfSpell (spell, null);
					switch (kind)
					{
						// Disjunction type spell that targets an overland enchantment rather than something on the map
						case DISPEL_OVERLAND_ENCHANTMENTS:
						case SPELL_BINDING:
						{
							getMagicSlidersUI ().setTargetingSpell (spell);
							getMagicSlidersUI ().setVisible (true);
							break;
						}
						
						// Target an enemy wizard, but we don't yet have enough info to display the UI, so first have to request it
						case SPELL_BLAST:
						{
							final TargetSpellMessage msg = new TargetSpellMessage ();
							msg.setSpellID (getSpellID ());
							getClient ().getServerConnection ().sendMessageToServer (msg);
							break;
						}
						
						// Spells targeted at an enemy wizard, where we don't need extra info like with spell blast
						case ENEMY_WIZARD_SPELLS:
						{
							getWizardsUI ().setTargetingSpell (spell);
							getWizardsUI ().setVisible (true);
							break;
						}
						
						// Resurrection needs to prompt with a list of heroes who have died
						case RAISE_DEAD:
						{
							final List<MemoryUnit> deadUnits = new ArrayList<MemoryUnit> ();
							for (final MemoryUnit thisUnit : getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ())
							{
								final ExpandedUnitDetails xu = getExpandUnitDetails ().expandUnitDetails (thisUnit, null, null, spell.getSpellRealm (),
									getClient ().getPlayers (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (), getClient ().getClientDB ());
								
								if (getSpellTargetingUtils ().isUnitValidTargetForSpell
									(spell, null, null, null, getClient ().getOurPlayerID (), null, null, xu, true,
										getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory (),
										getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWar (), getClient ().getPlayers (),
										getClient ().getClientDB ()) == TargetSpellResult.VALID_TARGET)
									
									deadUnits.add (thisUnit);
							};						
									
							if (deadUnits.size () == 0)
							{
								final String spellName = getLanguageHolder ().findDescription (getClient ().getClientDB ().findSpell (getSpellID (), "NewTurnMessageSpellEx (C)").getSpellName ());
								
								final MessageBoxUI msg = getPrototypeFrameCreator ().createMessageBox ();
								msg.setLanguageTitle (getLanguages ().getSpellBookScreen ().getCastSpellTitle ());
								msg.setText (getLanguageHolder ().findDescription (getLanguages ().getSpellBookScreen ().getNoDeadHeroesToBeResurrected ()).replaceAll
									("SPELL_NAME", spellName));

								msg.setVisible (true);
								
								// They still have to hit "cancel" on the overland map RHP to cancel targeting it fully
							}
							else
							{
								final UnitRowDisplayUI unitRowDisplay = getPrototypeFrameCreator ().createUnitRowDisplay ();
								unitRowDisplay.setUnits (deadUnits);
								unitRowDisplay.setTargetSpellID (spell.getSpellID ());
								unitRowDisplay.setVisible (true);
							}
							break;
						}

						// This is fine, majority of targeted spells are at cities, units or overland map locations so dealt with above
						default:
							break;
					}
				}
				break;
				
			default:
		}
	}
	
	/**
	 * @return Current status of this NTM
	 */
	@Override
	public final NewTurnMessageStatus getStatus ()
	{
		return status;
	}
	
	/**
	 * @param newStatus New status for this NTM
	 */
	@Override
	public final void setStatus (final NewTurnMessageStatus newStatus)
	{
		status = newStatus;
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
	public final MomLanguagesEx getLanguages ()
	{
		return languageHolder.getLanguages ();
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
	 * @return Spell book
	 */
	public final SpellBookUI getSpellBookUI ()
	{
		return spellBookUI;
	}

	/**
	 * @param ui Spell book
	 */
	public final void setSpellBookUI (final SpellBookUI ui)
	{
		spellBookUI = ui;
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
	 * @return Client-side unit utils
	 */
	public final UnitClientUtils getUnitClientUtils ()
	{
		return unitClientUtils;
	}

	/**
	 * @param util Client-side unit utils
	 */
	public final void setUnitClientUtils (final UnitClientUtils util)
	{
		unitClientUtils = util;
	}
	
	/**
	 * @return Did we cancel targeting the spell?
	 */
	public final boolean isTargetingCancelled ()
	{
		return targetingCancelled;
	}

	/**
	 * @param cancelled Did we cancel targeting the spell?
	 * @throws IOException If we can't find any of the resource images
	 */
	public final void setTargetingCancelled (final boolean cancelled) throws IOException
	{
		targetingCancelled = cancelled;
		getOverlandMapRightHandPanel ().updateProductionTypesStoppingUsFromEndingTurn ();
	}
	
	/**
	 * @return Chosen city target
	 */
	public final MapCoordinates3DEx getTargetedCity ()
	{
		return targetedCity;
	}
	
	/**
	 * @param city Chosen city target
	 * @throws IOException If we can't find any of the resource images
	 */
	public final void setTargetedCity (final MapCoordinates3DEx city) throws IOException
	{
		targetedCity = city;
		getOverlandMapRightHandPanel ().updateProductionTypesStoppingUsFromEndingTurn ();
	}
	
	/**
	 * @return Chosen unit target
	 */
	public final Integer getTargetedUnitURN ()
	{
		return targetedUnitURN;
	}

	/**
	 * @param unitURN Chosen unit target
	 * @throws IOException If we can't find any of the resource images
	 */
	public final void setTargetedUnitURN (final Integer unitURN) throws IOException
	{
		targetedUnitURN = unitURN;
		getOverlandMapRightHandPanel ().updateProductionTypesStoppingUsFromEndingTurn ();
	}

	/**
	 * @return Chosen player target
	 */
	public final Integer getTargetedPlayerID ()
	{
		return targetedPlayerID;
	}

	/**
	 * @param playerID Chosen player target
	 * @throws IOException If we can't find any of the resource images
	 */
	public final void setTargetedPlayerID (final Integer playerID) throws IOException
	{
		targetedPlayerID = playerID;
		getOverlandMapRightHandPanel ().updateProductionTypesStoppingUsFromEndingTurn ();
	}
	
	/**
	 * @return Magic sliders screen
	 */
	public final MagicSlidersUI getMagicSlidersUI ()
	{
		return magicSlidersUI;
	}

	/**
	 * @param ui Magic sliders screen
	 */
	public final void setMagicSlidersUI (final MagicSlidersUI ui)
	{
		magicSlidersUI = ui;
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
}