package momime.client.calculations;

/**
 * The city production calculation is complicated and things can be added in varying orders, i.e. you might get production, consumption, then production again.
 * So this enum allocates breakdown elements into groups and orders the elements into appropriate section headings.
 */
enum CityProductionBreakdownHeading
{
	/** Straight production amount, e.g. from civilians or surrounding terrain */
	PRODUCTION,
	
	/** Max city size cap */
	PRODUCTION_CAP,
	
	/** Rounding value up or down */
	PRODUCTION_ROUNDING,
	
	/** Percentage bonus to production, e.g. from mountains or various buildings */
	PERCENTAGE_BONUS_TO_PRODUCTION,
	
	/** Gold trade bonus % cap */
	PERCENTAGE_CAP,
	
	/** Consumption, e.g. civilians eating food or buildings requiring gold maintainence */
	CONSUMPTION;
}
