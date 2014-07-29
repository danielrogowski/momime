package momime.client.ui.panels;

/**
 * Different items that can display in the top portion of the right hand panel of the overland map
 */
public enum OverlandMapRightHandPanelTop
{
	/** Display buttons to select/deselect units at the current location, and show their health; always displayed with special orders on the bottom */
	UNITS,
	
	/**
	 * Display the amountPerTurn for gold, rations and mana (the amountStored for gold and mana is displayed no matter what the 'top' is displaying.
	 * This is displayed when we have no units left to move, along with the 'next turn' button.  In the original MoM its difficult but possible (via going
	 * in/out of the city screen) to get it into a state where we have no 'unitMoveFrom' value but still have units left to move, in which case it displays
	 * the Economy on the top and the special orders buttons on the bottom with 'wait' being the only enabled one.
	 * 
	 *  So economy+next turn is typical for our turn; economy+current player is typical for during opponents turns;
	 *  economy+special orders is possible in the original in unusual circumstances.
	 */
	ECONOMY,
	
	/** Terrain surveyor; always displayed with cancel on the bottom */
	SURVEYOR,
	
	/** Targetting and overland spell; always displayed with cancel on the bottom */
	TARGET_SPELL;
}