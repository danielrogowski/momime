package momime.server;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.transform.JDOMSource;

import com.ndg.multiplayer.server.session.MultiplayerSessionThread;
import com.ndg.multiplayer.server.session.PlayerServerDetails;
import com.ndg.multiplayer.sessionbase.JoinFailedReason;
import com.ndg.multiplayer.sessionbase.JoinSuccessfulReason;
import com.ndg.multiplayer.sessionbase.PersistentPlayerPrivateKnowledge;
import com.ndg.multiplayer.sessionbase.PlayerDescription;
import com.ndg.multiplayer.sessionbase.SessionDescription;
import com.ndg.multiplayer.sessionbase.TransientPlayerPrivateKnowledge;
import com.ndg.multiplayer.sessionbase.TransientPlayerPublicKnowledge;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import momime.common.database.CommonDatabase;
import momime.common.database.CommonDatabaseConstants;
import momime.common.database.CommonDatabaseImpl;
import momime.common.database.HeroItem;
import momime.common.database.Spell;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.FogOfWarStateID;
import momime.common.messages.MagicPowerDistribution;
import momime.common.messages.MapAreaOfFogOfWarStates;
import momime.common.messages.MapAreaOfMemoryGridCells;
import momime.common.messages.MapRowOfFogOfWarStates;
import momime.common.messages.MapRowOfMemoryGridCells;
import momime.common.messages.MapVolumeOfFogOfWarStates;
import momime.common.messages.MapVolumeOfMemoryGridCells;
import momime.common.messages.MemoryGridCell;
import momime.common.messages.MomGeneralPublicKnowledge;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomSessionDescription;
import momime.common.messages.MomTransientPlayerPrivateKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.messages.SpellResearchStatus;
import momime.common.messages.SpellResearchStatusID;
import momime.server.database.ServerDatabaseConverters;
import momime.server.database.ServerDatabaseConvertersImpl;
import momime.server.database.ServerDatabaseValues;
import momime.server.knowledge.CombatDetails;
import momime.server.mapgenerator.OverlandMapGenerator;
import momime.server.messages.MomGeneralServerKnowledge;
import momime.server.process.PlayerMessageProcessing;
import momime.server.utils.HeroItemServerUtils;
import momime.server.worldupdates.WorldUpdates;

/**
 * Thread that handles everything going on in one MoM session
 */
public final class MomSessionThread extends MultiplayerSessionThread implements MomSessionVariables
{
	/** Class logger */
	private final static Log log = LogFactory.getLog (MomSessionThread.class);

	/** Overland map generator for this session */
	private OverlandMapGenerator overlandMapGenerator;	
	
	/** Database converters */
	private ServerDatabaseConverters serverDatabaseConverters;
	
	/** Path to where all the server database XMLs are - from config file */
	private String pathToServerXmlDatabases;

	/** Path to where all the mod XMLs are - from config file */
	private String pathToModXmls;
	
	/** JAXB Unmarshaller for loading database XML files */
	private Unmarshaller commonDatabaseUnmarshaller;
	
	/** JDOM builder */
	private SAXBuilder saxBuilder;
	
	/** Methods for dealing with player msgs */
	private PlayerMessageProcessing playerMessageProcessing;
	
	/** Methods dealing with hero items */
	private HeroItemServerUtils heroItemServerUtils;
	
	/** Engine for updating server's true copy of the game world */
	private WorldUpdates worldUpdates;
	
	/** Combat details storage */
	private List<CombatDetails> combatDetails = new ArrayList<CombatDetails> ();
	
	/**
	 * Descendant server classes will want to override this to create a thread that knows how to process useful messages
	 * 
	 * @throws JAXBException If there is an error dealing with any XML files during creation
	 * @throws XMLStreamException If there is an error dealing with any XML files during creation
	 * @throws IOException If there is a problem generating the client database for this session
	 */
	@Override
	public final void initializeNewGame () throws JAXBException, XMLStreamException, IOException
	{
		loadDatabase ();
		
		// Generate the overland map
		log.info ("Generating overland map...");
		getOverlandMapGenerator ().generateOverlandTerrain (this);
		getOverlandMapGenerator ().generateInitialCombatAreaEffects (this);
		
		// Make all predefined hero items available - need to allocate a number for each
		for (final HeroItem item : getServerDB ().getHeroItem ())
			getGeneralServerKnowledge ().getAvailableHeroItem ().add (getHeroItemServerUtils ().createNumberedHeroItem (item, getGeneralServerKnowledge ()));

		log.info ("Session startup completed");
	}
	
	/**
	 * Must load server XML and regenerate client XML before adding players
	 *    
	 * @throws JAXBException If there is an error dealing with any XML files during creation
	 * @throws XMLStreamException If there is an error dealing with any XML files during creation
	 * @throws IOException Can be used for non-JAXB related errors
	 */
	@Override
	public final void preInitializeLoadedGame () throws JAXBException, XMLStreamException, IOException
	{
		loadDatabase ();
	}
	
	/**
	 * Loads the XML database, applying any selected mods, then performs all checks on it
	 *    
	 * @throws JAXBException If there is an error dealing with any XML files during creation
	 * @throws XMLStreamException If there is an error dealing with any XML files during creation
	 * @throws IOException If there is a problem generating the client database for this session
	 */
	private final void loadDatabase () throws JAXBException, XMLStreamException, IOException
	{
		// Load server XML
		log.info ("Loading server XML...");
		final File fullFilename = new File (getPathToServerXmlDatabases () + "/" + getSessionDescription ().getXmlDatabaseName () +
			ServerDatabaseConvertersImpl.SERVER_XML_FILE_EXTENSION);
		
		final CommonDatabaseImpl sdb;
		if (getSessionDescription ().getModName ().isEmpty ())
		{
			// No mods to apply, so unmarshal XML directly
			sdb = (CommonDatabaseImpl) getCommonDatabaseUnmarshaller ().unmarshal (fullFilename);
		}
		else
			try
			{
				// Load main XML
				final Document doc = getSaxBuilder ().build (fullFilename);
				
				// Apply each mod in turn
				for (final String modName : getSessionDescription ().getModName ())
				{
					final File fullModFilename = new File (getPathToModXmls () + "/" + modName + ServerDatabaseConvertersImpl.SERVER_XML_FILE_EXTENSION);
					final Document mod = getSaxBuilder ().build (fullModFilename);
					applyMod (doc.getRootElement (), mod.getRootElement ());
				}
				
				sdb = (CommonDatabaseImpl) getCommonDatabaseUnmarshaller ().unmarshal (new JDOMSource (doc));
			}
			catch (final JDOMException e)
			{
				throw new IOException ("Error applying mods", e);
			}

		// Create hash maps to look up all the values from the DB
		log.info ("Building maps and running checks over XML data...");
		sdb.buildMaps ();
		sdb.consistencyChecks ();
		getGeneralPublicKnowledge ().setMomDatabase (sdb);
	}
	
	/**
	 * Applys modifications from one XML node to another
	 * 
	 * @param target Main XML element
	 * @param source Mod XML element
	 * @throws IOException If there is a problem
	 */
	private final void applyMod (final Element target, final Element source) throws IOException
	{
		for (final Element sourceElement : source.getChildren ())
		{
			final Map<String, String> sourceAttributes = sourceElement.getAttributes ().stream ().collect (Collectors.toMap (a -> a.getName (), a -> a.getValue ()));
			
			// Force replacement of all content under this node, as if it was new
			final boolean replace = "replace".equals (sourceAttributes.get ("mod"));
			if (replace)
				sourceAttributes.remove ("mod");

			// Find matching element in target if present, otherwise create one
			final List<Element> targetElementsWithName = target.getChildren (sourceElement.getName ());
			final List<Element> possibleTargetElements = targetElementsWithName.stream ().filter
				(t -> t.getAttributes ().stream ().collect (Collectors.toMap (a -> a.getName (), a -> a.getValue ())).equals (sourceAttributes)).collect (Collectors.toList ());
			
			log.debug ("Applying mod element, found " + possibleTargetElements.size () + " possible target element(s) for " + sourceElement + " - " + sourceAttributes);
			
			if (possibleTargetElements.isEmpty ())
			{
				// Have to find the right place to insert it - after all the elements with the same name
				if (targetElementsWithName.isEmpty ())
					target.addContent (sourceElement.clone ());		// Add it at the end and hope for the best
				else
				{
					// Be smarter about the insertion point
					final Element lastTargetElementWithName =  targetElementsWithName.get (targetElementsWithName.size () - 1);
					final int index = target.indexOf (lastTargetElementWithName);
					
					target.addContent (index + 1, sourceElement.clone ());
				}
			}
			else if (possibleTargetElements.size () == 1)
			{
				final Element targetElement = possibleTargetElements.get (0);
				
				if ((sourceElement.getChildren ().isEmpty ()) && (targetElement.getChildren ().isEmpty ()))
				{
					log.debug ("Applying mod element, replacing with text " + sourceElement.getText ());
					targetElement.setText (sourceElement.getText ());
				}
				else
				{
					if (replace)
					{
						final List<Element> removeElements = new ArrayList<Element> (targetElement.getChildren ());
						removeElements.forEach (c -> c.detach ());
					}
					
					// Drill down into child elements
					applyMod (targetElement, sourceElement);
				}
			}
			else
				throw new IOException ("Multiple possible target elements for " + sourceElement + " - " + sourceAttributes);
		}
	}
	
	/**
	 * Kick off the first turn after loading a game.  
	 * 
	 * @throws JAXBException If there is an error dealing with any XML files during creation
	 * @throws XMLStreamException If there is an error dealing with any XML files during creation
	 * @throws IOException Can be used for non-JAXB related errors
	 */
	@Override
	public final void initializeLoadedGame () throws JAXBException, XMLStreamException, IOException
	{
		// If its a single player game, then start it immediately
		getPlayerMessageProcessing ().checkIfCanStartLoadedGame (this);
	}
	
	/**
	 * Implement custom rules for max players who can join a session, because we must leave enough space for AI players
	 * 
	 * @param playerID Player who wants to join this session
	 * @return Reason why they cannot join this session; or null if they can
	 */
	@Override
	public final JoinFailedReason canJoinSession (@SuppressWarnings ("unused") final int playerID)
	{
		// Max players = human opponents + AI opponents + 3, so from this work out the max number of human players
		final int maxHumanPlayers = getSessionDescription ().getMaxPlayers () - getSessionDescription ().getAiPlayerCount () - 2;
		
		final JoinFailedReason result;
		if (getPlayers ().size () < maxHumanPlayers)
			result = null;
		else
			result = JoinFailedReason.SESSION_FULL;

		return result;
	}
	
	/**
	 * Called after players are added to the session for any reason (when setting up a new game, joining, rejoining or loading)
	 *
	 * @param player The player who just joined the session
	 * @param reason The type of session the player joined into; ignored and can be null if connection is null OR sendMessages is false
	 * @throws JAXBException If there is a problem converting the reply into XML
	 * @throws XMLStreamException If there is a problem writing the reply to the XML stream
	 * @throws IOException If there is a problem sending any reply back to the client
	 */
	@SuppressWarnings ("unused")
	@Override
	public final void playerAdded (final PlayerServerDetails player, final JoinSuccessfulReason reason)
		throws JAXBException, XMLStreamException, IOException
	{
		// Only do this for *additional* players joining saved games - if we've loading a single player saved game, the player is added too early so these are handled above
		if (reason == JoinSuccessfulReason.REJOINED_SESSION)
			getPlayerMessageProcessing ().checkIfCanStartLoadedGame (this);
	}
	
	/**
	 * @return Session description, typecasted to MoM specific type
	 */
	@Override
	public final MomSessionDescription getSessionDescription ()
	{
		return (MomSessionDescription) super.getSessionDescription ();
	}

	/**
	 * @return Server general knowledge, typecasted to MoM specific type
	 */
	@Override
	public final MomGeneralServerKnowledge getGeneralServerKnowledge ()
	{
		return (MomGeneralServerKnowledge) super.getGeneralServerKnowledge ();
	}

	/**
	 * Spring seems to need this to be able to set the property - odd, since the MP demo works without this
	 * @param gsk Server knowledge structure, typecasted to MoM specific type
	 */
	public final void setGeneralServerKnowledge (final MomGeneralServerKnowledge gsk)
	{
		super.setGeneralServerKnowledge (gsk);
	}

	/**
	 * @return Server XML in use for this session
	 */
	@Override
	public final CommonDatabase getServerDB ()
	{
		return (CommonDatabase) getGeneralPublicKnowledge ().getMomDatabase ();
	}

	/**
	/**
	 * @return Public knowledge structure, typecasted to MoM specific type
	 */
	@Override
	public final MomGeneralPublicKnowledge getGeneralPublicKnowledge ()
	{
		return (MomGeneralPublicKnowledge) super.getGeneralPublicKnowledge ();
	}

	/**
	 * Spring seems to need this to be able to set the property - odd, since the MP demo works without this
	 * @param gpk Public knowledge structure, typecasted to MoM specific type
	 */
	public final void setGeneralPublicKnowledge (final MomGeneralPublicKnowledge gpk)
	{
		super.setGeneralPublicKnowledge (gpk);
	}

	/**
	 * @param sd Session description, in case any info here is needed to populate the player knowledge structure
	 * @param pd Player description, in case any info here is needed to populate the player knowledge structure
	 * @return Descendant of PersistentPlayerPrivateKnowledge, or can be left as null if not required
	 */
	@SuppressWarnings ("unused")
	@Override
	protected final PersistentPlayerPrivateKnowledge createPersistentPlayerPrivateKnowledge (final SessionDescription sd, final PlayerDescription pd)
	{
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();

		// Initialize all spell research statuses
		for (final Spell spell : getServerDB ().getSpell ())
		{
			final SpellResearchStatus status = new SpellResearchStatus ();
			status.setSpellID (spell.getSpellID ());

			// Regular spells must be leared
			if (spell.getSpellRealm () != null)
			{
				status.setStatus (SpellResearchStatusID.UNAVAILABLE);
				if (spell.getResearchCost () != null)
					status.setRemainingResearchCost (spell.getResearchCost ());
			}

			// Some arcane spells are free
			else if (spell.getResearchCost () == null)
				status.setStatus (SpellResearchStatusID.AVAILABLE);

			// But all arcane spells are at least researchable
			else
			{
				status.setStatus (SpellResearchStatusID.RESEARCHABLE);
				status.setRemainingResearchCost (spell.getResearchCost ());
			}

			priv.getSpellResearchStatus ().add (status);
		}

		// Set default tax rate
		priv.setTaxRateID (ServerDatabaseValues.TAX_RATE_DEFAULT);
		
		// Set default power distribution
		final MagicPowerDistribution dist = new MagicPowerDistribution ();
		dist.setManaRatio		(CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX / 3);
		dist.setResearchRatio	(CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX / 3);
		dist.setSkillRatio			(CommonDatabaseConstants.MAGIC_POWER_DISTRIBUTION_MAX / 3);
		priv.setMagicPowerDistribution (dist);

		// Create and initialize fog of war area
		final MapVolumeOfFogOfWarStates fogOfWar = new MapVolumeOfFogOfWarStates ();
		for (int plane = 0; plane < getServerDB ().getPlane ().size (); plane++)
		{
			final MapAreaOfFogOfWarStates fogOfWarPlane = new MapAreaOfFogOfWarStates ();
			for (int y = 0; y < getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
			{
				final MapRowOfFogOfWarStates fogOfWarRow = new MapRowOfFogOfWarStates ();
				for (int x = 0; x < getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
					fogOfWarRow.getCell ().add (FogOfWarStateID.NEVER_SEEN);

				fogOfWarPlane.getRow ().add (fogOfWarRow);
			}

			fogOfWar.getPlane ().add (fogOfWarPlane);
		}

		priv.setFogOfWar (fogOfWar);

		// Create and initialize fog of war memory
		// Note we create a MemoryGridCell at every location even if we've never seen that location,
		// but the terrain and city data elements will remain null until we actually see it.
		// This is just because it would make the code overly complex to have null checks everywhere this gets accessed.
		final MapVolumeOfMemoryGridCells fogOfWarMap = new MapVolumeOfMemoryGridCells ();
		for (int plane = 0; plane < getServerDB ().getPlane ().size (); plane++)
		{
			final MapAreaOfMemoryGridCells fogOfWarPlane = new MapAreaOfMemoryGridCells ();
			for (int y = 0; y < getSessionDescription ().getOverlandMapSize ().getHeight (); y++)
			{
				final MapRowOfMemoryGridCells fogOfWarRow = new MapRowOfMemoryGridCells ();
				for (int x = 0; x < getSessionDescription ().getOverlandMapSize ().getWidth (); x++)
					fogOfWarRow.getCell ().add (new MemoryGridCell ());

				fogOfWarPlane.getRow ().add (fogOfWarRow);
			}

			fogOfWarMap.getPlane ().add (fogOfWarPlane);
		}

		final FogOfWarMemory fogOfWarMemory = new FogOfWarMemory ();
		fogOfWarMemory.setMap (fogOfWarMap);
		priv.setFogOfWarMemory (fogOfWarMemory);

		return priv;
	}

	/**
	 * @param sd Session description, in case any info here is needed to populate the player knowledge structure
	 * @param pd Player description, in case any info here is needed to populate the player knowledge structure
	 * @return Descendant of TransientPlayerPublicKnowledge, or can be left as null if not required
	 */
	@SuppressWarnings ("unused")
	@Override
	public final TransientPlayerPublicKnowledge createTransientPlayerPublicKnowledge (final SessionDescription sd, final PlayerDescription pd)
	{
		return new MomTransientPlayerPublicKnowledge ();
	}

	/**
	 * @param sd Session description, in case any info here is needed to populate the player knowledge structure
	 * @param pd Player description, in case any info here is needed to populate the player knowledge structure
	 * @return Descendant of TransientPlayerPrivateKnowledge, or can be left as null if not required
	 */
	@SuppressWarnings ("unused")
	@Override
	public final TransientPlayerPrivateKnowledge createTransientPlayerPrivateKnowledge (final SessionDescription sd, final PlayerDescription pd)
	{
		return new MomTransientPlayerPrivateKnowledge ();
	}

	/**
	 * @return Engine for updating server's true copy of the game world
	 */
	@Override
	public final WorldUpdates getWorldUpdates ()
	{
		return worldUpdates;
	}
	
	/**
	 * @param wu Engine for updating server's true copy of the game world
	 */
	public final void setWorldUpdates (final WorldUpdates wu)
	{
		worldUpdates = wu;
	}

	/**
	 * @return Combat details storage
	 */
	@Override
	public final List<CombatDetails> getCombatDetails ()
	{
		return combatDetails;
	}
	
	/**
	 * @return Overland map generator for this session
	 */
	@Override
	public final OverlandMapGenerator getOverlandMapGenerator ()
	{
		return overlandMapGenerator;
	}

	/**
	 * @param mapGen Overland map generator for this session
	 */
	public final void setOverlandMapGenerator (final OverlandMapGenerator mapGen)
	{
		overlandMapGenerator = mapGen;
	}

	/**
	 * @return Database converters
	 */
	public final ServerDatabaseConverters getServerDatabaseConverters ()
	{
		return serverDatabaseConverters;
	}

	/**
	 * @param conv Database converters
	 */
	public final void setServerDatabaseConverters (final ServerDatabaseConverters conv)
	{
		serverDatabaseConverters = conv;
	}
	
	/**
	 * @return Path to where all the server database XMLs are - from config file
	 */
	public final String getPathToServerXmlDatabases ()
	{
		return pathToServerXmlDatabases;
	}

	/**
	 * @param path Path to where all the server database XMLs are - from config file
	 */
	public final void setPathToServerXmlDatabases (final String path)
	{
		pathToServerXmlDatabases = path;
	}

	/**
	 * @return Path to where all the mod XMLs are - from config file
	 */
	public final String getPathToModXmls ()
	{
		return pathToModXmls;
	}

	/**
	 * @param path Path to where all the mod XMLs are - from config file
	 */
	public final void setPathToModXmls (final String path)
	{
		pathToModXmls = path;
	}
	
	/**
	 * @return JAXB Unmarshaller for loading database XML files
	 */
	public final Unmarshaller getCommonDatabaseUnmarshaller ()
	{
		return commonDatabaseUnmarshaller;
	}

	/**
	 * @param unmarshaller JAXB Unmarshaller for loading database XML files
	 */
	public final void setCommonDatabaseUnmarshaller (final Unmarshaller unmarshaller)
	{
		commonDatabaseUnmarshaller = unmarshaller;
	}

	/**
	 * @return JDOM builder
	 */
	public final SAXBuilder getSaxBuilder ()
	{
		return saxBuilder;
	}

	/**
	 * @param s JDOM builder
	 */
	public final void setSaxBuilder (final SAXBuilder s)
	{
		saxBuilder = s;
	}
	
	/**
	 * @return Methods for dealing with player msgs
	 */
	public PlayerMessageProcessing getPlayerMessageProcessing ()
	{
		return playerMessageProcessing;
	}

	/**
	 * @param obj Methods for dealing with player msgs
	 */
	public final void setPlayerMessageProcessing (final PlayerMessageProcessing obj)
	{
		playerMessageProcessing = obj;
	}

	/**
	 * @return Methods dealing with hero items
	 */
	public final HeroItemServerUtils getHeroItemServerUtils ()
	{
		return heroItemServerUtils;
	}

	/**
	 * @param util Methods dealing with hero items
	 */
	public final void setHeroItemServerUtils (final HeroItemServerUtils util)
	{
		heroItemServerUtils = util;
	}
}