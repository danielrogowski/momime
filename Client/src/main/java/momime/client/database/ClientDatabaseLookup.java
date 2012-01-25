package momime.client.database;

import momime.client.database.v0_9_4.ClientDatabase;
import momime.client.database.v0_9_4.MapFeature;
import momime.client.database.v0_9_4.Wizard;
import momime.common.database.CommonDatabaseLookup;
import momime.common.database.RecordNotFoundException;

/**
 * Adds client-side specific extensions to the common database lookup class
 */
public final class ClientDatabaseLookup extends CommonDatabaseLookup
{
	/**
	 * @param db Client DB structure coverted from server XML
	 */
	public ClientDatabaseLookup (final ClientDatabase db)
	{
		super (db.getPlane (), db.getMapFeature (), db.getTileType (), db.getProductionType (),
			db.getPickType (), db.getPick (), db.getWizard (), db.getUnitType (), db.getUnitMagicRealm (),
			db.getUnit (), db.getUnitSkill (), db.getWeaponGrade (), db.getRace (), db.getTaxRate (),
			db.getBuilding (), db.getSpell (), db.getCombatAreaEffect ());
	}

	/**
	 * @param mapFeatureID Map feature ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Map feature object, or null if not found
	 * @throws RecordNotFoundException If the mapFeatureID doesn't exist
	 */
	@Override
	public final MapFeature findMapFeature (final String mapFeatureID, final String caller) throws RecordNotFoundException
	{
		return (MapFeature) super.findMapFeature (mapFeatureID, caller);
	}

	/**
	 * @param wizardID Wizard ID to search for
	 * @param caller Name of method calling this, for inclusion in debug message if there is a problem
	 * @return Wizard object
	 * @throws RecordNotFoundException If the wizardID doesn't exist
	 */
	@Override
	public final Wizard findWizard (final String wizardID, final String caller) throws RecordNotFoundException
	{
		return (Wizard) super.findWizard (wizardID, caller);
	}
}
