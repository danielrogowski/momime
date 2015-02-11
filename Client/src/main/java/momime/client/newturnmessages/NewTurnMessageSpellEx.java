package momime.client.newturnmessages;

import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.io.IOException;

import momime.client.MomClient;
import momime.client.language.database.LanguageDatabaseEx;
import momime.client.language.database.LanguageDatabaseHolder;
import momime.client.language.database.SpellLang;
import momime.client.ui.MomUIConstants;
import momime.client.ui.frames.SpellBookUI;
import momime.client.ui.panels.OverlandMapRightHandPanel;
import momime.client.ui.panels.OverlandMapRightHandPanelBottom;
import momime.client.ui.panels.OverlandMapRightHandPanelTop;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.common.messages.MemoryUnit;
import momime.common.messages.NewTurnMessageSpell;
import momime.common.messages.NewTurnMessageTypeID;
import momime.common.messages.OverlandMapCityData;
import momime.common.utils.UnitUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.map.coordinates.MapCoordinates3DEx;

/**
 * NTM about a spell, either one we've researched or need to pick a target for
 */
public final class NewTurnMessageSpellEx extends NewTurnMessageSpell
	implements NewTurnMessageExpiration, NewTurnMessageSimpleUI, NewTurnMessageClickable, NewTurnMessageMusic, NewTurnMessageMustBeAnswered
{
	/** Class logger */
	private final Log log = LogFactory.getLog (NewTurnMessageSpellEx.class);
	
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
	
	/** Did we cancel targetting the spell? */
	private boolean targettingCancelled;
	
	/** Chosen city target */
	private MapCoordinates3DEx targettedCity;
	
	/** Chosen unit target */
	private Integer targettedUnitURN;
	
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
		final SpellLang spellLang = getLanguage ().findSpell (getSpellID ());
		final String spellName = (spellLang != null) ? spellLang.getSpellName () : null;
		
		// Text varies according to the message type
		String text = null;
		try
		{
			switch (getMsgType ())
			{
				// Finished researching a spell, so need to pick another one
				case RESEARCHED_SPELL:
					final String languageEntryID;
					if (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched () == null)
						languageEntryID = "ResearchNotChosen";
					else
						languageEntryID = "ResearchChosen";
					
					final String newSpellID = (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched () == null) ? "" : getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched ();
					final SpellLang newSpell = (getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched () == null) ? null : getLanguage ().findSpell
						(getClient ().getOurPersistentPlayerPrivateKnowledge ().getSpellIDBeingResearched ());
					final String newSpellName = (newSpell != null) ? newSpell.getSpellName () : null;
					
					text = getLanguage ().findCategoryEntry ("NewTurnMessages", languageEntryID).replaceAll
						("OLD_SPELL_NAME", (spellName != null) ? spellName : getSpellID ()).replaceAll
						("NEW_SPELL_NAME", (newSpellName != null) ? newSpellName : newSpellID);
					break;
					
				// Cast a city/unit enchantment/curse, so need to pick a target for it
				case TARGET_SPELL:
					String targetLanguageEntryID = "TargetSpell";
					if (isTargettingCancelled ())
						targetLanguageEntryID = targetLanguageEntryID + "Cancelled";
					else if ((getTargettedCity () != null) || (getTargettedUnitURN () != null))
						targetLanguageEntryID = targetLanguageEntryID + "Chosen";
					
					if (getStatus () == NewTurnMessageStatus.BEFORE_OUR_TURN_BEGAN)
						targetLanguageEntryID = targetLanguageEntryID + "LastTurn";
					
					text = getLanguage ().findCategoryEntry ("NewTurnMessages", targetLanguageEntryID).replaceAll
						("SPELL_NAME", (spellName != null) ? spellName : getSpellID ());
										
					if (getTargettedCity () != null)
					{
						final OverlandMapCityData cityData = getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getMap ().getPlane ().get
							(getTargettedCity ().getZ ()).getRow ().get (getTargettedCity ().getY ()).getCell ().get (getTargettedCity ().getX ()).getCityData ();
						if (cityData != null)
							text = text.replaceAll ("TARGET", cityData.getCityName ());
					}
					else if (getTargettedUnitURN () != null)
					{
						final MemoryUnit unit = getUnitUtils ().findUnitURN (getTargettedUnitURN (), getClient ().getOurPersistentPlayerPrivateKnowledge ().getFogOfWarMemory ().getUnit ());
						if (unit != null)
							text = text.replaceAll ("TARGET", getUnitClientUtils ().getUnitName (unit, UnitNameType.A_UNIT_NAME));
					}
					
					break;
					
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
			answered = (isTargettingCancelled ()) || (getTargettedCity () != null) || (getTargettedUnitURN () != null);
		else
			answered = true;
		
		return answered;
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
	 * Clicking on population changes brings up the city screen
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
				if ((!isTargettingCancelled ()) && (getTargettedCity () == null) && (getTargettedUnitURN () == null))
				{
					getOverlandMapRightHandPanel ().setTargetSpell (this);
					getOverlandMapRightHandPanel ().setTop (OverlandMapRightHandPanelTop.TARGET_SPELL);
					getOverlandMapRightHandPanel ().setBottom (OverlandMapRightHandPanelBottom.CANCEL);
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
	public final LanguageDatabaseEx getLanguage ()
	{
		return languageHolder.getLanguage ();
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
	 * @return Did we cancel targetting the spell?
	 */
	public final boolean isTargettingCancelled ()
	{
		return targettingCancelled;
	}

	/**
	 * @param cancelled Did we cancel targetting the spell?
	 * @throws IOException If we can't find any of the resource images
	 */
	public final void setTargettingCancelled (final boolean cancelled) throws IOException
	{
		targettingCancelled = cancelled;
		getOverlandMapRightHandPanel ().updateProductionTypesStoppingUsFromEndingTurn ();
	}
	
	/**
	 * @return Chosen city target
	 */
	public final MapCoordinates3DEx getTargettedCity ()
	{
		return targettedCity;
	}
	
	/**
	 * @param city Chosen city target
	 * @throws IOException If we can't find any of the resource images
	 */
	public final void setTargettedCity (final MapCoordinates3DEx city) throws IOException
	{
		targettedCity = city;
		getOverlandMapRightHandPanel ().updateProductionTypesStoppingUsFromEndingTurn ();
	}
	
	/**
	 * @return Chosen unit target
	 */
	public final Integer getTargettedUnitURN ()
	{
		return targettedUnitURN;
	}

	/**
	 * @param unitURN Chosen unit target
	 * @throws IOException If we can't find any of the resource images
	 */
	public final void setTargettedUnitURN (final Integer unitURN) throws IOException
	{
		targettedUnitURN = unitURN;
		getOverlandMapRightHandPanel ().updateProductionTypesStoppingUsFromEndingTurn ();
	}
}