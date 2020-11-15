package momime.client.graphics.database;

import javax.xml.bind.annotation.XmlRegistry;

import momime.client.graphics.database.v0_9_9.CombatTileFigurePositions;
import momime.client.graphics.database.v0_9_9.CombatTileUnitRelativeScale;
import momime.client.graphics.database.v0_9_9.FigurePositionsForFigureCount;
import momime.client.graphics.database.v0_9_9.GraphicsDatabase;
import momime.client.graphics.database.v0_9_9.ObjectFactory;
import momime.client.graphics.database.v0_9_9.UnitSkillComponentImage;
import momime.client.graphics.database.v0_9_9.UnitSpecialOrderImage;

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
	 * @return Custom extended scale
	 */
	@Override
	public final CombatTileUnitRelativeScale createCombatTileUnitRelativeScale ()
	{
		return new CombatTileUnitRelativeScaleGfx ();
	}

	/**
	 * @return Custom extended Unit positions
	 */
	@Override
	public final CombatTileFigurePositions createCombatTileFigurePositions ()
	{
		return new CombatTileFigurePositionsGfx ();
	}
	
	/**
	 * @return Custom extended Figure positions for Figure count
	 */
	@Override
	public final FigurePositionsForFigureCount createFigurePositionsForFigureCount ()
	{
		return new FigurePositionsForFigureCountGfx ();
	}

	/**
	 * @return Custom extended Unit attribute component images
	 */
	@Override
	public final UnitSkillComponentImage createUnitSkillComponentImage ()
	{
		return new UnitSkillComponentImageGfx ();
	}

	/**
	 * @return Custom extended Unit special order images
	 */
	@Override
	public final UnitSpecialOrderImage createUnitSpecialOrderImage ()
	{
		return new UnitSpecialOrderImageGfx ();
	}
}