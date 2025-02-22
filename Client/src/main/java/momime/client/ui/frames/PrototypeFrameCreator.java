package momime.client.ui.frames;

import momime.client.ui.dialogs.ArmyListUI;
import momime.client.ui.dialogs.CombatEndedUI;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.dialogs.MiniCityViewUI;
import momime.client.ui.dialogs.OverlandEnchantmentsUI;
import momime.client.ui.dialogs.RandomEventUI;
import momime.client.ui.dialogs.SpellOfMasteryEndUI;
import momime.client.ui.dialogs.SpellOfMasteryStartUI;
import momime.client.ui.dialogs.TreasureUI;
import momime.client.ui.dialogs.UnitRowDisplayUI;
import momime.client.ui.dialogs.WizardBanishedUI;
import momime.client.ui.dialogs.WizardWonUI;

/**
 * Allows spring to create instances of frames that we allow multiple copies of, like message boxes and the city screen, and perform all necessary injections
 */
public interface PrototypeFrameCreator
{
	/**
	 * @return New message box UI
	 */
	public MessageBoxUI createMessageBox ();

	/**
	 * @return New calculation box UI
	 */
	public CalculationBoxUI createCalculationBox ();
	
	/**
	 * @return New edit string UI
	 */
	public EditStringUI createEditString ();	
	
	/**
	 * @return New city view UI
	 */
	public CityViewUI createCityView ();
	
	/**
	 * @return New mini city view UI
	 */
	public MiniCityViewUI createMiniCityView ();
	
	/**
	 * @return New change construction UI
	 */
	public ChangeConstructionUI createChangeConstruction ();
	
	/**
	 * @return New unit info UI
	 */
	public UnitInfoUI createUnitInfo ();
	
	/**
	 * @return New overland enchantment popup
	 */
	public OverlandEnchantmentsUI createOverlandEnchantments ();
	
	/**
	 * @return New combat ended popup
	 */
	public CombatEndedUI createCombatEnded ();
	
	/**
	 * @return New unit row display
	 */
	public UnitRowDisplayUI createUnitRowDisplay ();
	
	/**
	 * @return New hero item info UI
	 */
	public HeroItemInfoUI createHeroItemInfo ();

	/**
	 * @return New treasure reward UI
	 */
	public TreasureUI createTreasureReward ();
	
	/**
	 * @return New wizard banished UI
	 */
	public WizardBanishedUI createWizardBanished ();

	/**
	 * @return New wizard won UI
	 */
	public WizardWonUI createWizardWon ();

	/**
	 * @return New Spell of Mastery Start UI
	 */
	public SpellOfMasteryStartUI createSpellOfMasteryStart ();
	
	/**
	 * @return New Spell of Mastery End UI
	 */
	public SpellOfMasteryEndUI createSpellOfMasteryEnd ();
	
	/**
	 * @return New army list UI
	 */
	public ArmyListUI createArmyList ();
	
	/**
	 * @return New hiring UI
	 */
	public HeroOrUnitsOfferUI createHeroOrUnitsOffer ();

	/**
	 * @return New hero item offer UI
	 */
	public HeroItemOfferUI createHeroItemOffer ();

	/**
	 * @return New random event UI
	 */
	public RandomEventUI createRandomEvent ();
}