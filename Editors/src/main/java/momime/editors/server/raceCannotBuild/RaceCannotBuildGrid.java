package momime.editors.server.raceCannotBuild;

import java.awt.GridBagConstraints;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JTable;

import momime.editors.server.ServerEditorDatabaseConstants;

import org.jdom2.Element;

import com.ndg.xml.JdomUtils;
import com.ndg.xmleditor.editor.XmlEditorException;
import com.ndg.xmleditor.grid.XmlEditorGrid;

/**
 * Adds a second grid that shows buildings we implicitly cannot build as well as those explicitly listed in the XML file
 * e.g. if we explicitly cannot build a Sages' Guild, then this implies we cannot build a University, Alchemists' Guild, War College, etc. etc.
 */
public final class RaceCannotBuildGrid extends XmlEditorGrid
{
	/**
	 * Adds a second grid that shows buildings we implicitly cannot build as well as those explicitly listed in the XML file
	 * @throws XmlEditorException If there a syntax problem parsing the XSD
	 * @throws IOException If there is a problem loading the button images
	 */
	@Override
	public final void init () throws XmlEditorException, IOException
	{
		super.init ();

		// Create the table model
		final RaceCannotBuildTableModel tableModel = new RaceCannotBuildTableModel (buildListOfBuildingsThatRaceCannotBuild ());

		// Create the table itself
		final JTable cannotBuildTable = new JTable (tableModel);
		cannotBuildTable.setAutoResizeMode (JTable.AUTO_RESIZE_OFF);		// So it actually pays attention to the preferred widths
		cannotBuildTable.getColumnModel ().getColumn (0).setPreferredWidth (150);
		cannotBuildTable.getColumnModel ().getColumn (1).setPreferredWidth (200);

		// Put the grid into a scrolling area
		final GridBagConstraints gridConstraints = getUtils ().createConstraintsBothFill (0, getParentsAndGridPanelY (), 1, 1, INSET);
		gridConstraints.weightx = 1;
		gridConstraints.weighty = getGridWeightY ();

		final JScrollPane scrollPane = new JScrollPane (cannotBuildTable);
		getParentsAndGridPanel ().add (scrollPane, gridConstraints);
		
		setParentsAndGridPanelY (getParentsAndGridPanelY () + 1);
	}

	/**
	 * @return GridBagLayout weighty value to assign to the main grid on the form - if adding more grid, may need to reassign this down to make the grids resize nicely
	 */
	@Override
	public final double getGridWeightY ()
	{
		// Size the two grids equally
		return 0.5;
	}
	
	/**
	 * Does the actual work resolving out the implicit building restrictions
	 * @return Complete list of all the buildings this race cannot build, including both restrictions explicitly listed in the XML file, and implicit restrictions
	 */
	@SuppressWarnings ("rawtypes")
	private List<RaceImplicitCannotBuild> buildListOfBuildingsThatRaceCannotBuild ()
	{
		// Create list
		final List<RaceImplicitCannotBuild> cannotBuildList = new ArrayList<RaceImplicitCannotBuild> ();

		// Start by listing those buildings explicitly listed in the XML file
		final Iterator explicitList = getParentRecord ().getChildren (ServerEditorDatabaseConstants.TAG_CHILD_ENTITY_RACE_CANNOT_BUILD).iterator ();
		while (explicitList.hasNext ())
		{
			final Element explicitRestriction = (Element) explicitList.next ();
			final String buildingId = explicitRestriction.getAttributeValue (ServerEditorDatabaseConstants.TAG_ATTRIBUTE_RACE_CANNOT_BUILD_BUILDING_ID);

			final Element buildingNode = JdomUtils.findDomChildNodeWithTextAttribute (getMdiEditor ().getXmlDocuments ().get (0).getXml (),
				ServerEditorDatabaseConstants.TAG_ENTITY_BUILDING, ServerEditorDatabaseConstants.TAG_ATTRIBUTE_BUILDING_ID, buildingId);
			final String buildingName = buildingNode.getChildText (ServerEditorDatabaseConstants.TAG_VALUE_BUILDING_NAME);

			cannotBuildList.add (new RaceImplicitCannotBuild (buildingId, buildingName, null));
		}

		// Go through all buildings looking for buildings which require buildings already in the list, and repeat this until we add no more
		final List allBuildingsList = getMdiEditor ().getXmlDocuments ().get (0).getXml ().getChildren (ServerEditorDatabaseConstants.TAG_ENTITY_BUILDING);
		boolean keepGoing = true;
		while (keepGoing)
		{
			// Set this to true if we find an implicit restriction, to cause another iteration
			keepGoing = false;

			// Check every building in the databaase
			final Iterator allBuildings = allBuildingsList.iterator ();
			while (allBuildings.hasNext ())
			{
				final Element buildingNode = (Element) allBuildings.next ();
				final String buildingId = buildingNode.getAttributeValue (ServerEditorDatabaseConstants.TAG_ATTRIBUTE_BUILDING_ID);

				// Ignore if already in the list
				boolean alreadyInList = false;
				for (final RaceImplicitCannotBuild thisCannotBuild : cannotBuildList)
					if (thisCannotBuild.getBuildingId ().equals (buildingId))
						alreadyInList = true;

				if (!alreadyInList)
				{
					// Add reasons why we cannot build this building to here, i.e. if this stays null, then we can build it
					String reasons = null;

					// Go through all the pre-requisites of this building to see if it has any pre-requisites already in the list
					final Iterator prerequisites = buildingNode.getChildren (ServerEditorDatabaseConstants.TAG_CHILD_ENTITY_BUILDING_PREREQUISITE).iterator ();
					while (prerequisites.hasNext ())
					{
						final Element thisPrerequisite = (Element) prerequisites.next ();
						final String prerequisiteBuildingId = thisPrerequisite.getAttributeValue (ServerEditorDatabaseConstants.TAG_ATTRIBUTE_BUILDING_PREREQUISITE_ID);

						// Is this pre-requisite building one of those we cannot build?
						boolean preventsUsFromConstructingThisBuilding = false;
						for (final RaceImplicitCannotBuild thisCannotBuild : cannotBuildList)
							if (thisCannotBuild.getBuildingId ().equals (prerequisiteBuildingId))
								preventsUsFromConstructingThisBuilding = true;

						if (preventsUsFromConstructingThisBuilding)
						{
							// Get the name of the building that is preventing us from constructing this building
							final Element prerequisiteBuildingNode = JdomUtils.findDomChildNodeWithTextAttribute (getMdiEditor ().getXmlDocuments ().get (0).getXml (),
								ServerEditorDatabaseConstants.TAG_ENTITY_BUILDING, ServerEditorDatabaseConstants.TAG_ATTRIBUTE_BUILDING_ID, prerequisiteBuildingId);
							final String prerequisiteBuildingName = prerequisiteBuildingNode.getChildText (ServerEditorDatabaseConstants.TAG_VALUE_BUILDING_NAME);

							if (reasons == null)
								reasons = prerequisiteBuildingName;
							else
								reasons = reasons + ", " + prerequisiteBuildingName;
						}
					}

					// Did we find any required buildings that we cannot build?
					if (reasons != null)
					{
						final String buildingName = buildingNode.getChildText (ServerEditorDatabaseConstants.TAG_VALUE_BUILDING_NAME);

						cannotBuildList.add (new RaceImplicitCannotBuild (buildingId, buildingName, reasons));
						keepGoing = true;
					}
				}
			}
		}

		// Return the list
		return cannotBuildList;
	}
}