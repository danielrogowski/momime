package momime.client.ui.dialogs;

import java.awt.Color;
import java.awt.Font;
import java.awt.Polygon;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.swing.actions.LoggingAction;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutContainerEx;
import com.ndg.swing.layoutmanagers.xmllayout.XmlLayoutManager;

import momime.client.MomClient;
import momime.client.ui.MomUIConstants;
import momime.client.ui.frames.CombatUI;
import momime.client.ui.frames.SelectAdvisorUI;
import momime.client.ui.renderer.CastCombatSpellFrom;
import momime.client.utils.UnitClientUtils;
import momime.client.utils.UnitNameType;
import momime.common.database.Unit;
import momime.common.messages.NumberedHeroItem;

/**
 * Small popup that asks whether we want to cast combat spells from our wizard, a unit/hero, or the charges imbued in a hero item
 */
public final class CastCombatSpellFromUI extends MomClientDialogUI
{
	/** Class logger */
	private static final Log log = LogFactory.getLog (CastCombatSpellFromUI.class);

	/** XML layout */
	private XmlLayoutContainerEx selectAdvisorLayout;
	
	/** Small font */
	private Font smallFont;
	
	/** Multiplayer client */
	private MomClient client;
	
	/** Client-side unit utils */
	private UnitClientUtils unitClientUtils;
	
	/** Combat UI */
	private CombatUI combatUI;
	
	/** Title */
	private JLabel title;
	
	/** Content pane */
	private JPanel contentPane;
	
	/** Image of how buttons look normally */
	private BufferedImage buttonNormal;
	
	/** Image of how buttons look when pressed */
	private BufferedImage buttonPressed;
	
	/** List of casting sources to create buttons for */
	private List<CastCombatSpellFrom> castingSources;
	
	/** Map of casting source to actions */
	private Map<CastCombatSpellFrom, Action> castingSourceActions = new HashMap<CastCombatSpellFrom, Action> ();
	
	/** List of buttons for each casting source */
	private List<JButton> castingSourceButtons = new ArrayList<JButton> ();
	
	/**
	 * Sets up the frame once all values have been injected
	 * @throws IOException If a resource cannot be found
	 */
	@Override
	protected final void init () throws IOException
	{
		log.trace ("Entering init");
		
		// Load images
		final BufferedImage background = getUtils ().loadImage ("/momime.client.graphics/ui/backgrounds/selectAdvisor.png");
		buttonNormal = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button234x14Normal.png");
		buttonPressed = getUtils ().loadImage ("/momime.client.graphics/ui/buttons/button234x14Pressed.png");
		
		// Initialize the content pane
		contentPane = getUtils ().createPanelWithBackgroundImage (background);
		contentPane.setBackground (Color.BLACK);
		
		// Set up layout
		contentPane.setLayout (new XmlLayoutManager (getSelectAdvisorLayout ()));
		
		title = getUtils ().createLabel (MomUIConstants.GOLD, getSmallFont ());
		contentPane.add (title, "frmSelectAdvisorTitle");
		
		// Lock frame size
		getDialog ().setContentPane (contentPane);
		getDialog ().setResizable (false);
		getDialog ().setUndecorated (true);
		setCloseOnClick (true);
	
		getDialog ().setShape (new Polygon
			(new int [] {0, SelectAdvisorUI.BORDER_WIDTH, SelectAdvisorUI.BORDER_WIDTH, background.getWidth () - SelectAdvisorUI.BORDER_WIDTH, background.getWidth () - SelectAdvisorUI.BORDER_WIDTH, background.getWidth (), background.getWidth (), background.getWidth () - 2, background.getWidth () - 2, background.getWidth (), background.getWidth (), background.getWidth () - SelectAdvisorUI.BORDER_WIDTH, background.getWidth () - SelectAdvisorUI.BORDER_WIDTH, SelectAdvisorUI.BORDER_WIDTH, SelectAdvisorUI.BORDER_WIDTH, 0, 0, 2, 2, 0}, 
			new int [] {0, 0, 2, 2, 0, 0, SelectAdvisorUI.TOP_HEIGHT, SelectAdvisorUI.TOP_HEIGHT, background.getHeight () - SelectAdvisorUI.BOTTOM_HEIGHT, background.getHeight () - SelectAdvisorUI.BOTTOM_HEIGHT, background.getHeight (), background.getHeight (), background.getHeight () - 2, background.getHeight () - 2, background.getHeight (), background.getHeight (), background.getHeight () - SelectAdvisorUI.BOTTOM_HEIGHT, background.getHeight () - SelectAdvisorUI.BOTTOM_HEIGHT, SelectAdvisorUI.TOP_HEIGHT, SelectAdvisorUI.TOP_HEIGHT},
			20));		

		setCastingSources (castingSources);
		log.trace ("Exiting init");
	}
	
	/**
	 * @param aCastingSources List of casting sources to create buttons for  
	 */
	public final void setCastingSources (final List<CastCombatSpellFrom> aCastingSources)
	{
		log.trace ("Entering updateCastingSources");
		
		// Typically this gets called prior to showing the form
		castingSources = aCastingSources;
		if (contentPane != null)
		{
			// Clear out any old buttons
			for (final JButton button : castingSourceButtons)
				contentPane.remove (button);
			
			castingSourceButtons.clear ();
			castingSourceActions.clear ();
			
			// Create new actions and buttons
			int index = 0;
			if (castingSources != null)
				for (final CastCombatSpellFrom castingSource : castingSources)
				{
					// Create new action
					final Action castingSourceAction = new LoggingAction ((ev) ->
					{
						getDialog ().setVisible (false);
						getCombatUI ().setCastingSource (castingSource, true);
					});				
					castingSourceActions.put (castingSource, castingSourceAction);
					
					// Create new button
					index++;
					final JButton castingSourceButton = getUtils ().createImageButton (castingSourceAction, MomUIConstants.DULL_GOLD, Color.BLACK, getSmallFont (), buttonNormal, buttonPressed, buttonNormal);
					contentPane.add (castingSourceButton, "frmSelectAdvisor" + index);
					castingSourceButtons.add (castingSourceButton);
				}
				
			// Set the action labels
			languageChanged ();
		}
		
		log.trace ("Exiting updateCastingSources");
	}
	
	/**
	 * Update all labels and such from the chosen language 
	 */
	@Override
	public final void languageChanged ()
	{
		log.trace ("Entering languageChanged");

		title.setText (getLanguageHolder ().findDescription (getLanguages ().getSpellCasting ().getWhoWillCastTitle ()));
		
		try
		{
			for (final Entry<CastCombatSpellFrom, Action> castingSourceAction : castingSourceActions.entrySet ())
			{
				// Work out text for this line
				final String text;

				// Wizard casting
				if (castingSourceAction.getKey ().getCastingUnit () == null)
					text = getClient ().getOurPlayerName ();
				
				// Casting unit fixed spell, e.g. Giant Spiders casting Web
				else if (castingSourceAction.getKey ().getFixedSpellNumber () != null)
				{
					final Unit unitDef = getClient ().getClientDB ().findUnit (castingSourceAction.getKey ().getCastingUnit ().getUnitID (), "CastCombatSpellFromUI");
					final String spellID = unitDef.getUnitCanCast ().get (castingSourceAction.getKey ().getFixedSpellNumber ()).getUnitSpellID ();

					final String spellName = getLanguageHolder ().findDescription (getClient ().getClientDB ().findSpell (spellID, "CastCombatSpellFromUI").getSpellName ());

					text = getUnitClientUtils ().getUnitName (castingSourceAction.getKey ().getCastingUnit (), UnitNameType.SIMPLE_UNIT_NAME) + " - " +
						castingSourceAction.getKey ().getCastingUnit ().getFixedSpellsRemaining ().get
							(castingSourceAction.getKey ().getFixedSpellNumber ()) + "x " + spellName;
				}
				
				// Casting spell imbued in a hero item
				else if (castingSourceAction.getKey ().getHeroItemSlotNumber () != null)
				{
					final NumberedHeroItem item = castingSourceAction.getKey ().getCastingUnit ().getHeroItemSlot ().get
						(castingSourceAction.getKey ().getHeroItemSlotNumber ()).getHeroItem ();

					final String spellName = getLanguageHolder ().findDescription (getClient ().getClientDB ().findSpell (item.getSpellID (), "CastCombatSpellFromUI").getSpellName ());
					
					text = item.getHeroItemName () + " - " + castingSourceAction.getKey ().getCastingUnit ().getHeroItemSpellChargesRemaining ().get
						(castingSourceAction.getKey ().getHeroItemSlotNumber ()) + "x " + spellName;
				}
				else
					// Hero or unit casting from their own MP pool
					text = getUnitClientUtils ().getUnitName (castingSourceAction.getKey ().getCastingUnit (), UnitNameType.SIMPLE_UNIT_NAME);
				
				castingSourceAction.getValue ().putValue (Action.NAME, text);
			}
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
		
		log.trace ("Exiting languageChanged");
	}
	
	/**
	 * @return XML layout
	 */
	public final XmlLayoutContainerEx getSelectAdvisorLayout ()
	{
		return selectAdvisorLayout;
	}

	/**
	 * @param layout XML layout
	 */
	public final void setSelectAdvisorLayout (final XmlLayoutContainerEx layout)
	{
		selectAdvisorLayout = layout;
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
}