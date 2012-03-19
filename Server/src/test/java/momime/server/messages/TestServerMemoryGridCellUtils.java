package momime.server.messages;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import momime.common.database.CommonDatabaseConstants;
import momime.common.database.RecordNotFoundException;
import momime.common.messages.v0_9_4.OverlandMapTerrainData;
import momime.server.ServerTestData;
import momime.server.database.JAXBContextCreator;
import momime.server.database.ServerDatabaseLookup;
import momime.server.database.ServerDatabaseValues;
import momime.server.database.v0_9_4.ServerDatabase;

import org.junit.Test;

/**
 * Tests the ClientMemoryGridCellUtils class
 */
public final class TestServerMemoryGridCellUtils
{
	/**
	 * Tests the isNodeLairTower method when both the whole terrain data is null
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If we find a map feature or tile type that isn't in the database
	 */
	@Test
	public final void testIsNodeLairTower_TerrainDataNull () throws IOException, JAXBException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		assertFalse (ServerMemoryGridCellUtils.isNodeLairTower (null, db));
	}

	/**
	 * Tests the isNodeLairTower method when both the terrain data exists, but has tile type and map feature both null
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If we find a map feature or tile type that isn't in the database
	 */
	@Test
	public final void testIsNodeLairTower_BothNull () throws IOException, JAXBException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();

		assertFalse (ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}

	/**
	 * Tests the isNodeLairTower method when the tile type has no magic realm defined, and map feature is null
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If we find a map feature or tile type that isn't in the database
	 */
	@Test
	public final void testIsNodeLairTower_TileTypeNo () throws IOException, JAXBException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_MOUNTAIN);

		assertFalse (ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}

	/**
	 * Tests the isNodeLairTower method when the tile type does have a magic realm defined, and map feature is null
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If we find a map feature or tile type that isn't in the database
	 */
	@Test
	public final void testIsNodeLairTower_TileTypeYes () throws IOException, JAXBException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID ("TT12");

		assertTrue (ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}

	/**
	 * Tests the isNodeLairTower method when the map feature has no magic realm defined, and tile type is null
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If we find a map feature or tile type that isn't in the database
	 */
	@Test
	public final void testIsNodeLairTower_MapFeatureNo () throws IOException, JAXBException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setMapFeatureID ("MF01");

		assertFalse (ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}

	/**
	 * Tests the isNodeLairTower method when the map feature does have a magic realm defined, and tile type is null
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If we find a map feature or tile type that isn't in the database
	 */
	@Test
	public final void testIsNodeLairTower_MapFeatureYes () throws IOException, JAXBException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setMapFeatureID ("MF12A");

		assertTrue (ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}

	/**
	 * Tests the isNodeLairTower method when the map feature does have a magic realm defined, and tile type doesn't
	 * @throws IOException If we are unable to locate the server XML file
	 * @throws JAXBException If there is a problem reading the XML file
	 * @throws RecordNotFoundException If we find a map feature or tile type that isn't in the database
	 */
	@Test
	public final void testIsNodeLairTower_MapFeatureYes_WithTileType () throws IOException, JAXBException, RecordNotFoundException
	{
		final JAXBContext serverDatabaseContext = JAXBContextCreator.createServerDatabaseContext ();
		final ServerDatabase serverDB = (ServerDatabase) serverDatabaseContext.createUnmarshaller ().unmarshal (ServerTestData.locateServerXmlFile ());
		final ServerDatabaseLookup db = new ServerDatabaseLookup (serverDB);

		final OverlandMapTerrainData terrainData = new OverlandMapTerrainData ();
		terrainData.setTileTypeID (ServerDatabaseValues.VALUE_TILE_TYPE_MOUNTAIN);
		terrainData.setMapFeatureID (CommonDatabaseConstants.VALUE_FEATURE_UNCLEARED_TOWER_OF_WIZARDRY);

		assertTrue (ServerMemoryGridCellUtils.isNodeLairTower (terrainData, db));
	}
}
