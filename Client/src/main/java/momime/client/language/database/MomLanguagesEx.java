package momime.client.language.database;

import java.util.List;

import momime.client.languages.database.AlchemyScreen;
import momime.client.languages.database.ArmyListScreen;
import momime.client.languages.database.BuyingAndSellingBuildings;
import momime.client.languages.database.ChangeConstructionScreen;
import momime.client.languages.database.ChooseFlagColourScreen;
import momime.client.languages.database.ChooseInitialSpellsScreen;
import momime.client.languages.database.ChoosePortraitScreen;
import momime.client.languages.database.ChooseRaceScreen;
import momime.client.languages.database.ChooseWizardScreen;
import momime.client.languages.database.CitiesListScreen;
import momime.client.languages.database.CityGrowthRate;
import momime.client.languages.database.CityProduction;
import momime.client.languages.database.CityScreen;
import momime.client.languages.database.CombatDamage;
import momime.client.languages.database.CombatEndedScreen;
import momime.client.languages.database.CombatScreen;
import momime.client.languages.database.ConnectToServerScreen;
import momime.client.languages.database.CustomPicksScreen;
import momime.client.languages.database.DiplomacyScreen;
import momime.client.languages.database.DispelMagic;
import momime.client.languages.database.HelpScreen;
import momime.client.languages.database.HeroItemInfoScreen;
import momime.client.languages.database.HeroItemsScreen;
import momime.client.languages.database.HistoryScreen;
import momime.client.languages.database.JoinGameScreen;
import momime.client.languages.database.KnownServer;
import momime.client.languages.database.LoadGameScreen;
import momime.client.languages.database.MagicSlidersScreen;
import momime.client.languages.database.MainMenuScreen;
import momime.client.languages.database.MessageBoxScreen;
import momime.client.languages.database.Month;
import momime.client.languages.database.Multiplayer;
import momime.client.languages.database.NameCityScreen;
import momime.client.languages.database.NewGameScreen;
import momime.client.languages.database.NewTurnMessages;
import momime.client.languages.database.OptionsScreen;
import momime.client.languages.database.OutpostGrowthChance;
import momime.client.languages.database.OverlandMapScreen;
import momime.client.languages.database.RazeCityScreen;
import momime.client.languages.database.SelectAdvisorScreen;
import momime.client.languages.database.Shortcut;
import momime.client.languages.database.ShortcutKey;
import momime.client.languages.database.Simple;
import momime.client.languages.database.SpellBookScreen;
import momime.client.languages.database.SpellCasting;
import momime.client.languages.database.SpellOfMasteryEndScreen;
import momime.client.languages.database.SpellOfMasteryStartScreen;
import momime.client.languages.database.SpellQueueScreen;
import momime.client.languages.database.TaxRateScreen;
import momime.client.languages.database.TreasureScreen;
import momime.client.languages.database.TurnSystems;
import momime.client.languages.database.UnitInfoScreen;
import momime.client.languages.database.UnitName;
import momime.client.languages.database.UnitRowDisplayScreen;
import momime.client.languages.database.UnrestCalculation;
import momime.client.languages.database.VariableMana;
import momime.client.languages.database.WaitForPlayersToJoinScreen;
import momime.client.languages.database.WizardBanishedScreen;
import momime.client.languages.database.WizardWonScreen;
import momime.client.languages.database.WizardsScreen;
import momime.common.database.Language;
import momime.common.database.RecordNotFoundException;

/**
 * New singular language XML
 */
public interface MomLanguagesEx
{
	/**
	 * @return List of all supported languages
	 */
	public List<LanguageOptionEx> getLanguageOptions ();
	
	/**
	 * @param lang Language to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Language option object
	 * @throws RecordNotFoundException If the language doesn't exist
	 */
	public LanguageOptionEx findLanguageOption (final Language lang, final String caller) throws RecordNotFoundException;
	
	/**
	 * @return List of all known servers
	 */
	public List<KnownServer> getKnownServer ();
	
	/**
	 * @param knownServerID Known server to search for
	 * @return Known server object if exists, null if not found
	 */
	public KnownServer findKnownServer (final String knownServerID);
		
	/**
	 * This doesn't throw RecordNotFoundExceptions so that we don't have to define shortcut keys in unit tests 
	 * 
	 * @param shortcut Game shortcut that we're looking to see if there is a key defined for it
	 * @return Details of the keys that should activate this shortcut in different languages, or null if not found
	 */
	public ShortcutKey findShortcutKey (final Shortcut shortcut);
	
	/**
	 * @return Container for language strings
	 */
    public Simple getSimple ();
	
	/**
	 * @return Container for language strings
	 */
    public List<Month> getMonth ();
	
	/**
	 * @return Container for language strings
	 */
    public Multiplayer getMultiplayer ();
	
	/**
	 * @return Container for language strings
	 */
    public UnitName getUnitName ();
	
	/**
	 * @return Container for language strings
	 */
    public TurnSystems getTurnSystems ();
	
	/**
	 * @return Container for language strings
	 */
    public SpellCasting getSpellCasting ();
	
	/**
	 * @return Container for language strings
	 */
    public DispelMagic getDispelMagic ();
	
	/**
	 * @return Container for language strings
	 */
    public VariableMana getVariableMana ();
	
	/**
	 * @return Container for language strings
	 */
    public SpellTargetingEx getSpellTargeting ();
	
	/**
	 * @return Container for language strings
	 */
    public CombatDamage getCombatDamage ();
	
	/**
	 * @return Container for language strings
	 */
    public CityGrowthRate getCityGrowthRate ();

	/**
	 * @return Container for language strings
	 */
    public OutpostGrowthChance getOutpostGrowthChance ();
    
	/**
	 * @return Container for language strings
	 */
    public CityProduction getCityProduction ();
	
	/**
	 * @return Container for language strings
	 */
    public UnrestCalculation getUnrestCalculation ();
	
	/**
	 * @return Container for language strings
	 */
    public BuyingAndSellingBuildings getBuyingAndSellingBuildings ();
	
	/**
	 * @return Container for language strings
	 */
    public NewTurnMessages getNewTurnMessages ();
	
	/**
	 * @return Container for language strings
	 */
    public MainMenuScreen getMainMenuScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public MessageBoxScreen getMessageBoxScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public ConnectToServerScreen getConnectToServerScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public NewGameScreen getNewGameScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public JoinGameScreen getJoinGameScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public LoadGameScreen getLoadGameScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public ChooseWizardScreen getChooseWizardScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public ChoosePortraitScreen getChoosePortraitScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public ChooseFlagColourScreen getChooseFlagColourScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public CustomPicksScreen getCustomPicksScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public ChooseInitialSpellsScreen getChooseInitialSpellsScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public ChooseRaceScreen getChooseRaceScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public WaitForPlayersToJoinScreen getWaitForPlayersToJoinScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public OverlandMapScreen getOverlandMapScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public NameCityScreen getNameCityScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public CityScreen getCityScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public ChangeConstructionScreen getChangeConstructionScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public UnitInfoScreen getUnitInfoScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public OptionsScreen getOptionsScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public RazeCityScreen getRazeCityScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public CombatScreen getCombatScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public CombatEndedScreen getCombatEndedScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public MagicSlidersScreen getMagicSlidersScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public AlchemyScreen getAlchemyScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public SpellBookScreen getSpellBookScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public SelectAdvisorScreen getSelectAdvisorScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public TaxRateScreen getTaxRateScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public UnitRowDisplayScreen getUnitRowDisplayScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public SpellQueueScreen getSpellQueueScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public WizardsScreen getWizardsScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public ArmyListScreen getArmyListScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public CitiesListScreen getCitiesListScreen ();
	
	/**
	 * @return Container for language strings
	 */
  public HeroItemsScreen getHeroItemsScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public HeroItemInfoScreen getHeroItemInfoScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public TreasureScreen getTreasureScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public WizardBanishedScreen getWizardBanishedScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public WizardWonScreen getWizardWonScreen ();
	
	/**
	 * @return Container for language strings
	 */
    public SpellOfMasteryStartScreen getSpellOfMasteryStartScreen ();

	/**
	 * @return Container for language strings
	 */
    public SpellOfMasteryEndScreen getSpellOfMasteryEndScreen ();
    
	/**
	 * @return Container for language strings
	 */
    public HistoryScreen getHistoryScreen ();
	
	/**
	 * @return Container for language strings
	 */
	public DiplomacyScreen getDiplomacyScreen ();
    
	/**
	 * @return Container for language strings
	 */
    public HelpScreen getHelpScreen ();
}