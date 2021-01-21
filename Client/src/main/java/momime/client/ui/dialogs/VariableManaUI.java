package momime.client.ui.dialogs;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.WindowConstants;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.actions.LoggingAction;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.ui.MomUIConstants;
import momime.client.ui.frames.CombatUI;
import momime.client.ui.frames.SpellBookUI;
import momime.client.utils.TextUtils;
import momime.common.MomException;
import momime.common.database.AttackSpellCombatTargetID;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.DamageResolutionTypeID;
import momime.common.database.LanguageText;
import momime.common.database.RecordNotFoundException;
import momime.common.database.Spell;
import momime.common.database.SpellBookSectionID;
import momime.common.messages.MomPersistentPlayerPublicKnowledge;
import momime.common.messages.clienttoserver.RequestCastSpellMessage;
import momime.common.utils.SpellCastType;
import momime.common.utils.SpellUtils;

/**
 * Popup box used for spells like fire bolt where we can choose an additional amount of MP to pump into the spell to make it more powerful
 */
public final class VariableManaUI extends MomClientDialogUI
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (VariableManaUI.class);

	/** XML layout */
	private XmlLayoutContainerEx variableManaLayout;
	
	/** Large font */
	private Font mediumFont;

	/** Text utils */
	private TextUtils textUtils;
	
	/** Spell utils */
	private SpellUtils spellUtils;

	/** Multiplayer client */
	private MomClient client;

	/** Session utils */
	private MultiplayerSessionUtils multiplayerSessionUtils;

	/** Combat UI */
	private CombatUI combatUI;
	
	/** Spell book */
	private SpellBookUI spellBookUI;
	
	/** OK action */
	private Action okAction;

	/** Effect of the additional MP */
	private JLabel leftLabel;
	
	/** Actual MP cost */
	private JLabel rightLabel;
	
	/** The slider to choose the amount */
	private JSlider slider;
	
	/** Spell chosen from spell book that we want to cast, and need to select MP and damage for */
	private Spell spellBeingTargetted;
	
	/** Minimum allowed value for this spell - whether this value represents MP or damage depends on getMode () */
	private int sliderMinimum;
	
	/** Maximum allowed value for this spell - whether this value represents MP or damage depends on getMode () */
	private int sliderMaximum;
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/editString298x76.png");
		final BufferedImage buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/okButton41x15Normal.png");
		final BufferedImage buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/okButton41x15Pressed.png");
		final BufferedImage sliderImage = getUtils ().loadImage ("/momime.client.graphics/ui/spellBook/variableManaSlider.png");

		// Actions
		okAction = new LoggingAction ((ev) ->
		{
			variableDamageChosen ();
			getDialog ().dispose ();
		});
		
		// Initialize the frame
		final VariableManaUI ui = this;
		getDialog ().setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
		getDialog ().addWindowListener (new WindowAdapter ()
		{
			@Override
			public final void windowClosed (@SuppressWarnings ("unused") final WindowEvent ev)
			{
				getLanguageChangeMaster ().removeLanguageChangeListener (ui);
			}
		});
		
		// Initialize the content pane
		final JPanel contentPane = getUtils ().createPanelWithBackgroundImage (background);
		contentPane.setBorder (BorderFactory.createEmptyBorder (2, 19, 8, 19));
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getVariableManaLayout ()));
		
		leftLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (leftLabel, "frmVariableManaLeft");

		rightLabel = getUtils ().createLabel (MomUIConstants.GOLD, getMediumFont ());
		contentPane.add (rightLabel, "frmVariableManaRight");

		contentPane.add (getUtils ().createImageButton (okAction, MomUIConstants.LIGHT_BROWN, MomUIConstants.DARK_BROWN, getMediumFont (),
			buttonNormal, buttonPressed, buttonNormal), "frmVariableManaOK");
		
		// Slider
		slider = new JSlider ()
		{
			@Override
			protected final void paintComponent (final Graphics g)
			{
				// Only draw the piece according to the current value - if there's no maximum (we have 0 of the 'from' resource) then just draw nothing
				if (getMaximum () > 0)
				{
					final int width = (sliderImage.getWidth () * (getValue () - getMinimum ())) / (getMaximum () - getMinimum ());
				
					// Draw a piece of the brighter colour, according to the current slider position
					g.drawImage (sliderImage,
						0, 0, width, sliderImage.getHeight (),
						0, 0, width, sliderImage.getHeight (), null);
				}
			}
		};
		
		slider.setOpaque (false);
		slider.addChangeListener ((ev) -> sliderPositionChanged ());
		
		contentPane.add (slider, "frmVariableManaSlider");
		
		// Update the slider if form being displayed for the first time
		if (getSpellBeingTargetted () != null)
			setSpellBeingTargetted (getSpellBeingTargetted ()); 
		
		// Lock frame size
		getDialog ().setContentPane (contentPane);
		getDialog ().setResizable (false);
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		getDialog ().setTitle (getLanguageHolder ().findDescription (getLanguages ().getVariableMana ().getTitle ()));
		
		// No action text to set, because the button has OK on it as part of the image
		
		// Update slider labels
		sliderPositionChanged ();		
	}

	/**
	 * Updates the labels as we move the slider
	 */
	public final void sliderPositionChanged ()
	{
		if ((slider != null) && (getSpellBeingTargetted () != null))
			try
			{
				// The slider value is the resulting damage of the spell
				// How this label appears depends on what kind of damage the spell does - for regular damage spells like fire bolt we want this
				// to say e.g. "15 damage" but for Banish and Life Drain we want it to say e.g. "-4 resistance"
				final List<LanguageText> languageText;
				if (getSpellBeingTargetted ().getSpellBookSectionID () == SpellBookSectionID.DISPEL_SPELLS)
					languageText = getLanguages ().getVariableMana ().getDispel ();
				else
					languageText = ((getSpellBeingTargetted ().getAttackSpellDamageResolutionTypeID () == DamageResolutionTypeID.EACH_FIGURE_RESIST_OR_DIE) ||
						(getSpellBeingTargetted ().getAttackSpellDamageResolutionTypeID () == DamageResolutionTypeID.SINGLE_FIGURE_RESIST_OR_DIE) ||
						(getSpellBeingTargetted ().getAttackSpellDamageResolutionTypeID () == DamageResolutionTypeID.RESISTANCE_ROLLS) ||
						(getSpellBeingTargetted ().getAttackSpellDamageResolutionTypeID () == DamageResolutionTypeID.RESIST_OR_TAKE_DAMAGE) ||
						(getSpellBeingTargetted ().getAttackSpellDamageResolutionTypeID () == DamageResolutionTypeID.DISINTEGRATE)) ?
								
						getLanguages ().getVariableMana ().getResistance () : getLanguages ().getVariableMana ().getDamage ();
				
				leftLabel.setText (getLanguageHolder ().findDescription (languageText).replaceAll
					("VALUE", getTextUtils ().intToStrCommas (getVariableDamage ())));

				// Lookup the "MP" suffix
				final String manaSuffix = getLanguageHolder ().findDescription (getClient ().getClientDB ().findProductionType (CommonDatabaseConstants.PRODUCTION_TYPE_ID_MANA, "sliderPositionChanged").getProductionTypeSuffix ());

				// Work out the unmodified MP cost
				final int unmodifiedCost;
				switch (getMode ())
				{
					case CHOOSE_DAMAGE_CALC_MANA:
						unmodifiedCost = getSpellBeingTargetted ().getCombatCastingCost () +
							((slider.getValue () - getSpellBeingTargetted ().getCombatBaseDamage ()) * getSpellBeingTargetted ().getCombatManaPerAdditionalDamagePoint ());
						break;
					
					case CHOOSE_MANA_CALC_DAMAGE:
						unmodifiedCost = slider.getValue ();
						break;
						
					default:
						throw new MomException ("VariableManaUI.sliderPositionChanged doesn't know how to calc unmodifiedCost for mode " + getMode ());
				}
				
				// Work out the modified MP cost, reduced if we have a lot of spell books
				final PlayerPublicDetails ourPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getClient ().getOurPlayerID (), "sliderPositionChanged");
				final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) ourPlayer.getPersistentPlayerPublicKnowledge ();
	
				final int modifiedCost = getSpellUtils ().getReducedCastingCost (getSpellBeingTargetted (), unmodifiedCost,
					pub.getPick (), getClient ().getSessionDescription ().getSpellSetting (), getClient ().getClientDB ());
				
				// Show modified cost or not?
				if (modifiedCost == unmodifiedCost)
					rightLabel.setText (getTextUtils ().intToStrCommas (unmodifiedCost) + " " + manaSuffix);
				else
					rightLabel.setText (getTextUtils ().intToStrCommas (unmodifiedCost) + " " + manaSuffix + " (" +
						getTextUtils ().intToStrCommas (modifiedCost) + " " + manaSuffix + ")");
						
				// This shouldn't really be necessary, but without it, I see odd 1 pixel errors around the edges of the bar as it is dragged back and forth
				slider.repaint ();
			}
			catch (final Exception e)
			{
				log.error (e, e);
			}
	}
	
	/**
	 * @return Spell chosen from spell book that we want to cast, and need to select MP for
	 */
	public final Spell getSpellBeingTargetted ()
	{
		return spellBeingTargetted;
	}
	
	/**
	 * Sets up prompt slider and labels to target a spell
	 * @param spell Spell chosen from spell book that we want to cast, and need to select MP for
	 * @throws IOException If we can't find our player, or there's a problem calculating the reduced casting cost 
	 */
	public final void setSpellBeingTargetted (final Spell spell) throws IOException
	{
		final PlayerPublicDetails ourPlayer = getMultiplayerSessionUtils ().findPlayerWithID (getClient ().getPlayers (), getClient ().getOurPlayerID (), "setSpellBeingTargetted");
		final MomPersistentPlayerPublicKnowledge pub = (MomPersistentPlayerPublicKnowledge) ourPlayer.getPersistentPlayerPublicKnowledge ();
		
		// This has to work and be able to calculate the minimum+maximum damage even if the form has never been displayed
		// so that we can handle the situation where the first variable damage spell cast is one that we don't have enough skill/MP
		// to put any additional MP into
		spellBeingTargetted = spell;
		
		switch (getMode ())
		{
			case CHOOSE_DAMAGE_CALC_MANA:
				sliderMinimum = spell.getCombatBaseDamage ();
				sliderMaximum = spell.getCombatMaxDamage ();
				break;
				
			case CHOOSE_MANA_CALC_DAMAGE:
				sliderMinimum = spell.getCombatCastingCost ();
				sliderMaximum = spell.getCombatCastingCost () + (getSpellBeingTargetted ().getCombatMaxDamage () - getSpellBeingTargetted ().getCombatBaseDamage ()) /
					getSpellBeingTargetted ().getCombatAdditionalDamagePointsPerMana ();
				break;

			default:
				throw new MomException ("VariableManaUI.setSpellBeingTargetted doesn't know how to set initial minimum and maximum for mode " + getMode ());
		}
		
		// We may not have enough mana (or casting skill remaining in combat) to use the spell at full power,
		// so work out what the maximum we can actually afford is.
		// This is awkward to work out directly because we have to go from
		// base dmg > unmodified mana cost > reduced mana cost e.g. from having a lot of that colour spell book.
		// NB. We already know we can at least cast the spell for minimum damage, or the player wouldn't
		// have been able to click the spell in the spell book to get here.
		if (getCastType () == SpellCastType.COMBAT)
		{
			boolean done = false;
			while (!done)
			{
				final int unmodifiedCost;
				switch (getMode ())
				{
					case CHOOSE_DAMAGE_CALC_MANA:
						unmodifiedCost = getSpellBeingTargetted ().getCombatCastingCost () +
							((sliderMaximum - sliderMinimum) * getSpellBeingTargetted ().getCombatManaPerAdditionalDamagePoint ());
						break;
					
					case CHOOSE_MANA_CALC_DAMAGE:
						unmodifiedCost = sliderMaximum;
						break;
						
					default:
						throw new MomException ("VariableManaUI.setSpellBeingTargetted doesn't know how to calc unmodifiedCost for mode " + getMode ());
				}
					
				// Work out the modified MP cost, reduced if we have a lot of spell books; but first check whether its the wizard casting, or a unit
				final int modifiedCost;
				if ((getCombatUI ().getCastingSource () == null) || (getCombatUI ().getCastingSource ().getCastingUnit () == null))
					modifiedCost = getSpellUtils ().getReducedCastingCost (getSpellBeingTargetted (), unmodifiedCost,
						pub.getPick (), getClient ().getSessionDescription ().getSpellSetting (), getClient ().getClientDB ());
				else
					modifiedCost = unmodifiedCost;
				
				if (modifiedCost > getSpellBookUI ().getCombatMaxCastable ())
					sliderMaximum--;
				else
					done = true;
			}
		}
		
		// Update the UI, if the form has ever been displayed
		if (slider != null)
		{
			slider.setMinimum (sliderMinimum);
			slider.setValue (sliderMinimum);
			slider.setMaximum (sliderMaximum);
	
			// Update slider labels
			sliderPositionChanged ();
		}
	}
	
	/**
	 * @return Which mode the VariableManaUI is operating in
	 * @throws MomException If the variable settings on the spell definition are inconsistent
	 */
	private final VariableManaUIMode getMode () throws MomException
	{
		final VariableManaUIMode mode;
		if ((getSpellBeingTargetted ().getCombatManaPerAdditionalDamagePoint () == null) && (getSpellBeingTargetted ().getCombatAdditionalDamagePointsPerMana () == null))
			throw new MomException ("VariableManaUI can't pick a mode to use for spell " + getSpellBeingTargetted ().getSpellID () + " because both values are null");

		else if ((getSpellBeingTargetted ().getCombatManaPerAdditionalDamagePoint () != null) && (getSpellBeingTargetted ().getCombatAdditionalDamagePointsPerMana () != null))
			throw new MomException ("VariableManaUI can't pick a mode to use for spell " + getSpellBeingTargetted ().getSpellID () + " because both values are set");
		
		else if (getSpellBeingTargetted ().getCombatManaPerAdditionalDamagePoint () != null)
			mode = VariableManaUIMode.CHOOSE_DAMAGE_CALC_MANA;
		
		else
			mode = VariableManaUIMode.CHOOSE_MANA_CALC_DAMAGE;
		
		return mode;
	}
	
	/**
	 * Handles when we click the "OK" button to confirm the chosen variable amount of damage that we want
	 * @throws JAXBException If there is a problem converting the object into XML
	 * @throws XMLStreamException If there is a problem writing to the XML stream
	 * @throws RecordNotFoundException If we can't find the spell being targetted
	 * @throws MomException If the variable settings on the spell definition are inconsistent
	 */
	public final void variableDamageChosen () throws JAXBException, XMLStreamException, RecordNotFoundException, MomException
	{
		final Spell spell = getClient ().getClientDB ().findSpell (getSpellBeingTargetted ().getSpellID (), "variableDamageChosen");
		final SpellBookSectionID sectionID = spell.getSpellBookSectionID ();
		
		if ((sectionID == SpellBookSectionID.UNIT_ENCHANTMENTS) || (sectionID == SpellBookSectionID.UNIT_CURSES) ||
			(sectionID == SpellBookSectionID.SUMMONING) ||
			(((sectionID == SpellBookSectionID.ATTACK_SPELLS) || (sectionID == SpellBookSectionID.DISPEL_SPELLS)) &&
				(spell.getAttackSpellCombatTarget () == AttackSpellCombatTargetID.SINGLE_UNIT)))
			
			getCombatUI ().setSpellBeingTargetted (spell);
		else
		{
			// Tell server to cast it
			final RequestCastSpellMessage msg = new RequestCastSpellMessage ();
			msg.setSpellID (spell.getSpellID ());

			if (getCastType () == SpellCastType.COMBAT)
			{
				msg.setCombatLocation (getCombatUI ().getCombatLocation ());
				if ((getCombatUI ().getCastingSource () != null) && (getCombatUI ().getCastingSource ().getCastingUnit () != null))
					msg.setCombatCastingUnitURN (getCombatUI ().getCastingSource ().getCastingUnit ().getUnitURN ());
			}
			
			msg.setVariableDamage (getVariableDamage ());
			
			getClient ().getServerConnection ().sendMessageToServer (msg);
		}
	}
	
	/**
	 * @return Overland or combat casting
	 */
	private final SpellCastType getCastType ()
	{
		return getCombatUI ().isVisible () ? SpellCastType.COMBAT : SpellCastType.OVERLAND;
	}
	
	/**
	 * @return Whether we need to display the slider at all; false if we've only enough casting skill or MP to cast the spell at base cost
	 */
	public final boolean anySelectableRange ()
	{
		return (sliderMaximum > sliderMinimum);
	}
	
	/**
	 * @return Reads off the slider value
	 * @throws MomException If the variable settings on the spell definition are inconsistent
	 */
	public final int getVariableDamage () throws MomException
	{
		// The only time we may get here when the form hasn't been displayed is if the first variable MP spell we cast
		// we had insufficient MP to make any choice at all, and so the form was never set up properly or displayed.
		// So if there's no range at all, avoid reading the slider value which may be null.
		final int dmg;
		if (!anySelectableRange ())
			dmg = getSpellBeingTargetted ().getCombatBaseDamage ();
		else
			switch (getMode ())
			{
				case CHOOSE_DAMAGE_CALC_MANA:
					dmg = slider.getValue ();
					break;
					
				case CHOOSE_MANA_CALC_DAMAGE:
					dmg = getSpellBeingTargetted ().getCombatBaseDamage () +
						((slider.getValue () - getSpellBeingTargetted ().getCombatCastingCost ()) * getSpellBeingTargetted ().getCombatAdditionalDamagePointsPerMana ());
					break;

				default:
					throw new MomException ("VariableManaUI.getVariableDamage doesn't know how to calculate damage for mode " + getMode ());
			}
		
		return dmg;
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getVariableManaLayout ()
	{
		return variableManaLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setVariableManaLayout (final XmlLayoutContainerEx layout)
	{
		variableManaLayout = layout;
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
	 * @return Text utils
	 */
	public final TextUtils getTextUtils ()
	{
		return textUtils;
	}

	/**
	 * @param tu Text utils
	 */
	public final void setTextUtils (final TextUtils tu)
	{
		textUtils = tu;
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
	 * @return Combat UI
	 */
	public final CombatUI getCombatUI ()
	{
		return combatUI;
	}

	/**
	 * @param ui Combat UI
	 */
	public final void setCombatUI (final CombatUI ui)
	{
		combatUI = ui;
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
}