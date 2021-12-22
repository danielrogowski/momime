package momime.client.graphics.database;

import jakarta.xml.bind.annotation.XmlRegistry;

/**
 * Creates our custom extended GraphicsDatabase when it is unmarshalled with JAXB
 */
@XmlRegistry
public final class GraphicsDatabaseObjectFactory extends ObjectFactory
{
	/**
	 * @return Custom extended GraphicsDatabase 
	 */
	@Override
	public final GraphicsDatabase createGraphicsDatabase ()
	{
		return new GraphicsDatabaseExImpl ();
	}

	/**
	 * @return Custom extended Unit positions
	 */
	@Override
	public final CombatTileFigurePositions createCombatTileFigurePositions ()
	{
		return new CombatTileFigurePositionsGfx ();
	}
}