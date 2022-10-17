package momime.client.messages.process;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ndg.multiplayer.base.client.BaseServerToClientMessage;

import jakarta.xml.bind.JAXBException;
import momime.client.audio.AudioPlayer;
import momime.client.config.MomImeClientConfig;
import momime.client.config.WindowPosition;
import momime.client.graphics.AnimationContainer;
import momime.client.graphics.database.GraphicsDatabaseConstants;
import momime.client.ui.frames.AlchemyUI;
import momime.client.ui.frames.CitiesListUI;
import momime.client.ui.frames.DamageCalculationsUI;
import momime.client.ui.frames.HeroItemsUI;
import momime.client.ui.frames.HistoryUI;
import momime.client.ui.frames.MagicSlidersUI;
import momime.client.ui.frames.MainMenuUI;
import momime.client.ui.frames.NewGameUI;
import momime.client.ui.frames.NewTurnMessagesUI;
import momime.client.ui.frames.OverlandMapUI;
import momime.client.ui.frames.QueuedSpellsUI;
import momime.client.ui.frames.SelectAdvisorUI;
import momime.client.ui.frames.SpellBookUI;
import momime.client.ui.frames.TaxRateUI;
import momime.client.ui.frames.WizardsUI;
import momime.common.messages.servertoclient.StartGameMessage;

/**
 * Message server broadcasts when all game setup is complete and its time for clients to actually switch to the map screen
 */
public final class StartGameMessageImpl extends StartGameMessage implements BaseServerToClientMessage
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (StartGameMessageImpl.class);

	/** New Game UI */
	private NewGameUI newGameUI;
	
	/** Overland map UI */
	private OverlandMapUI overlandMapUI;
	
	/** Tax rate UI */
	private TaxRateUI taxRateUI;
	
	/** Magic sliders screen */
	private MagicSlidersUI magicSlidersUI;

	/** Alchemy UI */
	private AlchemyUI alchemyUI;
	
	/** Spell book */
	private SpellBookUI spellBookUI;

	/** Queued spells UI */
	private QueuedSpellsUI queuedSpellsUI;
	
	/** Cities list */
	private CitiesListUI citiesListUI;

	/** New turn messages UI */
	private NewTurnMessagesUI newTurnMessagesUI;
	
	/** Advisors UI */
	private SelectAdvisorUI selectAdvisorUI;
	
	/** UI for displaying damage calculations */
	private DamageCalculationsUI damageCalculationsUI;

	/** Wizards UI */
	private WizardsUI wizardsUI;
	
	/** Hero items UI */
	private HeroItemsUI heroItemsUI;
	
	/** UI for screen showing power base history for each wizard */
	private HistoryUI historyUI;
	
	/** Main menu UI */
	private MainMenuUI mainMenuUI;
	
	/** Music player */
	private AudioPlayer musicPlayer;
	
	/** Client config, containing various overland map settings */
	private MomImeClientConfig clientConfig;
	
	/**
	 * @throws JAXBException Typically used if there is a problem sending a reply back to the server
	 * @throws XMLStreamException Typically used if there is a problem sending a reply back to the server
	 * @throws IOException Can be used for more general types of processing failure
	 */
	@Override
	public final void start () throws JAXBException, XMLStreamException, IOException
	{
		getNewGameUI ().setVisible (false);
		getMainMenuUI ().setVisible (false);
		getOverlandMapUI ().setVisible (true);
		
		// Display other windows marked visible in the config file
		for (final WindowPosition pos : getClientConfig ().getWindowPosition ())
			if ((pos.isVisible () != null) && (pos.isVisible ()))
				switch (pos.getWindowID ())
				{
					case TAX_RATE:
						getTaxRateUI ().setVisible (true);
						break;
						
					case MAGIC_SLIDERS:
						getMagicSlidersUI ().setVisible (true);
						break;
						
					case ALCHEMY:
						getAlchemyUI ().setVisible (true);
						break;
						
					case SPELL_BOOK:
						getSpellBookUI ().setVisible (true);
						break;
						
					case QUEUED_SPELLS:
						getQueuedSpellsUI ().setVisible (true);
						break;
						
					case CITIES:
						getCitiesListUI ().setVisible (true);
						break;
						
					case NEW_TURN_MESSAGES:
						getNewTurnMessagesUI ().setVisible (true);
						break;
						
					case ADVISORS:
						getSelectAdvisorUI ().setVisible (true);
						break;
						
					case DAMAGE_CALCULATIONS:
						getDamageCalculationsUI ().setVisible (true);
						break;
						
					case WIZARDS:
						getWizardsUI ().setVisible (true);
						break;
						
					case HERO_ITEMS:
						getHeroItemsUI ().setVisible (true);
						break;
						
					case HISTORY:
						getHistoryUI ().setVisible (true);
						break;
						
					// This is fine, windows with persist visibility = false are not listed here
					default:
				}
		
		// Switch to the overland map background music
		try
		{
			getMusicPlayer ().playPlayList (GraphicsDatabaseConstants.PLAY_LIST_OVERLAND_MUSIC, AnimationContainer.GRAPHICS_XML);
		}
		catch (final Exception e)
		{
			log.error (e, e);
		}
	}
	
	/**
	 * @return New Game UI
	 */
	public final NewGameUI getNewGameUI ()
	{
		return newGameUI;
	}

	/**
	 * @param ui New Game UI
	 */
	public final void setNewGameUI (final NewGameUI ui)
	{
		newGameUI = ui;
	}

	/**
	 * @return Overland map UI
	 */
	public final OverlandMapUI getOverlandMapUI ()
	{
		return overlandMapUI;
	}

	/**
	 * @param ui Overland map UI
	 */
	public final void setOverlandMapUI (final OverlandMapUI ui)
	{
		overlandMapUI = ui;
	}

	/**
	 * @return Tax rate UI
	 */
	public final TaxRateUI getTaxRateUI ()
	{
		return taxRateUI;
	}

	/**
	 * @param ui Tax rate UI
	 */
	public final void setTaxRateUI (final TaxRateUI ui)
	{
		taxRateUI = ui;
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
	 * @return Alchemy UI
	 */
	public final AlchemyUI getAlchemyUI ()
	{
		return alchemyUI;
	}

	/**
	 * @param ui Alchemy UI
	 */
	public final void setAlchemyUI (final AlchemyUI ui)
	{
		alchemyUI = ui;
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
	 * @return Queued spells UI
	 */
	public final QueuedSpellsUI getQueuedSpellsUI ()
	{
		return queuedSpellsUI;
	}

	/**
	 * @param ui Queued spells UI
	 */
	public final void setQueuedSpellsUI (final QueuedSpellsUI ui)
	{
		queuedSpellsUI = ui;
	}
	
	/**
	 * @return Cities list
	 */
	public final CitiesListUI getCitiesListUI ()
	{
		return citiesListUI;
	}

	/**
	 * @param ui Cities list
	 */
	public final void setCitiesListUI (final CitiesListUI ui)
	{
		citiesListUI = ui;
	}
	
	/**
	 * @return New turn messages UI
	 */
	public final NewTurnMessagesUI getNewTurnMessagesUI ()
	{
		return newTurnMessagesUI;
	}

	/**
	 * @param ui New turn messages UI
	 */
	public final void setNewTurnMessagesUI (final NewTurnMessagesUI ui)
	{
		newTurnMessagesUI = ui;
	}

	/**
	 * @return Advisors UI
	 */
	public final SelectAdvisorUI getSelectAdvisorUI ()
	{
		return selectAdvisorUI;
	}

	/**
	 * @param ui Advisors UI
	 */
	public final void setSelectAdvisorUI (final SelectAdvisorUI ui)
	{
		selectAdvisorUI = ui;
	}
	
	/**
	 * @return UI for displaying damage calculations
	 */
	public final DamageCalculationsUI getDamageCalculationsUI ()
	{
		return damageCalculationsUI;
	}

	/**
	 * @param ui UI for displaying damage calculations
	 */
	public final void setDamageCalculationsUI (final DamageCalculationsUI ui)
	{
		damageCalculationsUI = ui;
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
	 * @return Hero items UI
	 */
	public final HeroItemsUI getHeroItemsUI ()
	{
		return heroItemsUI;
	}

	/**
	 * @param ui Hero items UI
	 */
	public final void setHeroItemsUI (final HeroItemsUI ui)
	{
		heroItemsUI = ui;
	}

	/**
	 * @return UI for screen showing power base history for each wizard
	 */
	public final HistoryUI getHistoryUI ()
	{
		return historyUI;
	}

	/**
	 * @param h UI for screen showing power base history for each wizard
	 */
	public final void setHistoryUI (final HistoryUI h)
	{
		historyUI = h;
	}
	
	/**
	 * @return Main menu UI
	 */
	public final MainMenuUI getMainMenuUI ()
	{
		return mainMenuUI;
	}

	/**
	 * @param ui Main menu UI
	 */
	public final void setMainMenuUI (final MainMenuUI ui)
	{
		mainMenuUI = ui;
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
	 * @return Client config, containing various overland map settings
	 */	
	public final MomImeClientConfig getClientConfig ()
	{
		return clientConfig;
	}

	/**
	 * @param config Client config, containing various overland map settings
	 */
	public final void setClientConfig (final MomImeClientConfig config)
	{
		clientConfig = config;
	}
}