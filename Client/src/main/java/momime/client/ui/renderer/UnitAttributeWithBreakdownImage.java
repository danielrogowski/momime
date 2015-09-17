package momime.client.ui.renderer;

import java.awt.image.BufferedImage;

/**
 * Unit attributes displayed on the unit info panel show a breakdown of how much of the value is coming from
 * the basic stat, experience, CAEs and so on.  Producing that bitmap is therefore quite invovled, and so we do
 * it once up front as the attribute is added into the list, rather than redrawing the whole bitmap every
 * time the list is redrawn.  So this means we need somewhere to store it.
 */
public final class UnitAttributeWithBreakdownImage
{
	/** Which unit skill (attribute) is being drawn */
	private final String unitSkillID;
	
	/** The pre-built image showing the breakdown of all the components of the unit attribute value */
	private final BufferedImage unitAttributeBreakdownImage;

	/**
	 * @param aUnitSkillID Which unit skill (attribute) is being drawn
	 * @param aUnitAttributeBreakdownImage The pre-built image showing the breakdown of all the components of the unit attribute value
	 */
	public UnitAttributeWithBreakdownImage (final String aUnitSkillID, final BufferedImage aUnitAttributeBreakdownImage)
	{
		unitSkillID = aUnitSkillID;
		unitAttributeBreakdownImage = aUnitAttributeBreakdownImage;
	}

	/**
	 * @return Which unit skill (attribute) is being drawn
	 */
	public final String getUnitSkillID ()
	{
		return unitSkillID;
	}
	
	/**
 	 * @return The pre-built image showing the breakdown of all the components of the unit attribute value
	 */
	public final BufferedImage getUnitAttributeBreakdownImage ()
	{
		return unitAttributeBreakdownImage;
	}
}