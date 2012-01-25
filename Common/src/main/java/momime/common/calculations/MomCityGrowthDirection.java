package momime.common.calculations;

/**
 * Different ways a city population can be changing
 */
public enum MomCityGrowthDirection
{
	/** City has space left - population is growing */
	GROWING,

	/** City is overcrowded - population is dying */
	DYING,

	/** City is exactly at its maximum population */
	MAXIMUM;
}
