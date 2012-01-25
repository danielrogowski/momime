package momime.server.messages;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.server.ServerTestData;
import momime.server.database.JAXBContextCreator;
import momime.server.database.ServerDatabaseLookup;
import momime.server.database.v0_9_4.ServerDatabase;

import org.junit.Test;

/**
 * Tests the ClientMemoryGridCellUtils class
 */
public final class TestServerMemoryGridCellUtils
{
	/**
	 * Tests the isNodeLairTower method when both the tile type and map feature are both null
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If we find a map feature or tile type that isn't in the database
	 */
	@Test
	public final void testIsNodeLairTower_BothNull () throws JAXBException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.SERVER_XML_FILE);
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();

		assertFalse (ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}

	/**
	 * Tests the isNodeLairTower method when the tile type has no magic realm defined, and map feature is null
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If we find a map feature or tile type that isn't in the database
	 */
	@Test
	public final void testIsNodeLairTower_TileTypeNo () throws JAXBException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.SERVER_XML_FILE);
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");

		assertFalse (ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}

	/**
	 * Tests the isNodeLairTower method when the tile type does have a magic realm defined, and map feature is null
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If we find a map feature or tile type that isn't in the database
	 */
	@Test
	public final void testIsNodeLairTower_TileTypeYes () throws JAXBException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.SERVER_XML_FILE);
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT12");

		assertTrue (ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}

	/**
	 * Tests the isNodeLairTower method when the map feature has no magic realm defined, and tile type is null
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If we find a map feature or tile type that isn't in the database
	 */
	@Test
	public final void testIsNodeLairTower_MapFeatureNo () throws JAXBException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.SERVER_XML_FILE);
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setMapFeatureID ("MF01");

		assertFalse (ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}

	/**
	 * Tests the isNodeLairTower method when the map feature does have a magic realm defined, and tile type is null
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If we find a map feature or tile type that isn't in the database
	 */
	@Test
	public final void testIsNodeLairTower_MapFeatureYes () throws JAXBException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.SERVER_XML_FILE);
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setMapFeatureID ("MF12A");

		assertTrue (ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}

	/**
	 * Tests the isNodeLairTower method when the map feature does have a magic realm defined, and tile type doesn't
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If we find a map feature or tile type that isn't in the database
	 */
	@Test
	public final void testIsNodeLairTower_MapFeatureYes_WithTileType () throws JAXBException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.SERVER_XML_FILE);
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT01");
		terrainData.setMapFeatureID ("MF12A");

		assertTrue (ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}
}
