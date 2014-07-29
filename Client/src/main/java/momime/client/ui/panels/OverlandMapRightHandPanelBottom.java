package momime.client.ui.panels;

/**
 * Different items that can display in the bottom portion of the right hand panel of the overland map
 */
public enum OverlandMapRightHandPanelBottom
{
	/** Who the current player is (this is displayed when it isn't our turn) */
	PLAYER,
	
	/** Next turn button */
	NEXT_TURN_BUTTON,
	
	/** Single cancel button (used to cancel surveyor mode, or to abort targetting an overland spell) */
	CANCEL,
	
	/** Special order buttons (wait, done, patrol, build city, create outpost, purify, meld with node) */
	SPECIAL_ORDERS;
}