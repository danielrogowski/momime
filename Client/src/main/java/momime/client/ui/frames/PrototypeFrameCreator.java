package momime.client.ui.frames;

import momime.client.ui.dialogs.CombatEndedUI;
import momime.client.ui.dialogs.MessageBoxUI;
import momime.client.ui.dialogs.MiniCityViewUI;
import momime.client.ui.dialogs.OverlandEnchantmentsUI;
import momime.client.ui.dialogs.UnitRowDisplayUI;

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
}