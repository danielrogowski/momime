package momime.editors.server.raceCannotBuild;

import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * Table model which displays data from a list of RaceImplicitCannotBuild objects
 */
public final class RaceCannotBuildTableModel extends AbstractTableModel
{
	/** The pre-built list we're displaying data from */
	private final List<RaceImplicitCannotBuild> cannotBuildList;

	/**
	 * Creates a table model to display data from a list of RaceImplicitCannotBuild objects
	 * @param aCannotBuildList The list of buildings this race cannot build, to display in the table
	 */
	public RaceCannotBuildTableModel (final List<RaceImplicitCannotBuild> aCannotBuildList)
	{
		super ();

		cannotBuildList = aCannotBuildList;
	}

	/**
	 * @return The number of fields for this entity
	 */
	@Override
	public int getColumnCount ()
	{
		return 2;
	}

	/**
	 * @return The number of buildings we cannot build
	 */
	@Override
	public final int getRowCount ()
	{
		return cannotBuildList.size ();
	}

	/**
     * @param rowIndex The row whose value is to be queried
     * @param columnIndex The column whose value is to be queried
	 * @return The text to display in the grid for this cell
	 */
	@Override
	public final Object getValueAt (final int rowIndex, final int columnIndex)
	{
		String value;

		if (columnIndex == 0)
		{
			// Name of the building we cannot build
			value = cannotBuildList.get (rowIndex).getBuildingName ();
		}
		else
		{
			// Name of the building(s) that are pre-requisited for it
			value = cannotBuildList.get (rowIndex).getReasons ();
		}

		return value;
	}

	/**
     * @param columnIndex	 The index of the column
	 * @return The title to display in the header above this column
	 */
	@Override
	public final String getColumnName (final int columnIndex)
	{
		String columnHeading;

		if (columnIndex == 0)
			columnHeading = "Race Cannot Build";
		else
			columnHeading = "Because they cannot build";

		return columnHeading;
	}
}