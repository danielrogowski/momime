package momime.client.ui.dialogs;

/**
 * The VariableManaUI operates in one of two modes, as per values below
 */
enum VariableManaUIMode
{
	/** The slider value sets the damage dealt; based on that we calculate how much MP the spell will cost */
	CHOOSE_DAMAGE_CALC_MANA,
	
	/** The slider value sets the MP cost; based on that we calculate how much damage the spell will do*/
	CHOOSE_MANA_CALC_DAMAGE;
}