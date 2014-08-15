package momime.client.utils;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_5.AvailableUnit;

/**
 * Client side only helper methods for dealing with units
 */
public interface UnitClientUtils
{
	/**
	 * Note the generated unit names are obviously very dependant on the selected language, but the names themselves don't get notified
	 * to update themselves when the language changes.  It is the responsibility of whatever is calling this method to register itself to be
	 * notified of language updates, and cause this method to be re-evalulated when that happens.
	 * 
	 * @param unit Unit to generate the name of
	 * @param unitNameType Type of name to generate (see comments against that enum)
	 * @return Generated unit name
	 * @throws RecordNotFoundException If we can't find the unit definition in the server XML
	 */
	public String getUnitName (final AvailableUnit unit, final UnitNameType unitNameType) throws RecordNotFoundException;
}