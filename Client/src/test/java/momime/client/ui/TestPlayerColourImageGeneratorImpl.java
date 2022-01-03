package momime.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ndg.multiplayer.session.MultiplayerSessionUtils;
import com.ndg.multiplayer.session.PlayerPublicDetails;
import com.ndg.swing.NdgUIUtils;

import momime.client.MomClient;
import momime.common.messages.FogOfWarMemory;
import momime.common.messages.KnownWizardDetails;
import momime.common.messages.MomPersistentPlayerPrivateKnowledge;
import momime.common.messages.MomTransientPlayerPublicKnowledge;
import momime.common.utils.KnownWizardUtils;

/**
 * Tests the PlayerColourImageGeneratorImpl class
 */
@ExtendWith(MockitoExtension.class)
public final class TestPlayerColourImageGeneratorImpl
{
	/**
	 * Tests the getModifiedImage method when we don't ask for any modifications to the base image
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetModifiedImage_unmodified () throws Exception
	{
		// Base image
		final BufferedImage src = new BufferedImage (2, 1, BufferedImage.TYPE_INT_ARGB);
		
		final NdgUIUtils utils = mock (NdgUIUtils.class);
		when (utils.loadImage ("test.png")).thenReturn (src);
		
		// Set up object to test
		final PlayerColourImageGeneratorImpl gen = new PlayerColourImageGeneratorImpl ();
		gen.setUtils (utils);
		
		// Call method
		final BufferedImage image = gen.getModifiedImage ("test.png", false, null, null, null, null, null);
		
		// Check results
		assertSame (src, image);
	}

	/**
	 * Tests the getModifiedImage method colouring an entire image, like a wizard gem
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetModifiedImage_colourEntireImage () throws Exception
	{
		// Player's colour
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final MomTransientPlayerPublicKnowledge trans = new MomTransientPlayerPublicKnowledge ();
		trans.setFlagColour ("C");
		
		final PlayerPublicDetails player = new PlayerPublicDetails (null, null, trans);
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, 1, "getModifiedImage")).thenReturn (player);
		
		// Wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (mem);
		
		final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
		wizardDetails.setWizardID ("WZ01");
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), 1, "getModifiedImage")).thenReturn (wizardDetails);

		// Client
		final MomClient client = mock (MomClient.class);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		
		// Base image
		final BufferedImage src = new BufferedImage (2, 1, BufferedImage.TYPE_INT_ARGB);
		
		final NdgUIUtils utils = mock (NdgUIUtils.class);
		when (utils.loadImage ("test.png")).thenReturn (src);
		
		// Image being coloured
		final BufferedImage dest = new BufferedImage (2, 1, BufferedImage.TYPE_INT_ARGB);
		when (utils.multiplyImageByColour (src, 12)).thenReturn (dest);
				
		// Set up object to test
		final PlayerColourImageGeneratorImpl gen = new PlayerColourImageGeneratorImpl ();
		gen.setUtils (utils);
		gen.setClient (client);
		gen.setMultiplayerSessionUtils (multiplayerSessionUtils);
		gen.setKnownWizardUtils (knownWizardUtils);
		
		// Call method
		final BufferedImage image = gen.getModifiedImage ("test.png", true, null, null, null, 1, null);
		
		// Check results
		assertSame (dest, image);
	}

	/**
	 * Tests the getModifiedImage method colouring an entire image then applying shading to it
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetModifiedImage_colourAndShadeEntireImage () throws Exception
	{
		// Player's colour
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final MomTransientPlayerPublicKnowledge trans = new MomTransientPlayerPublicKnowledge ();
		trans.setFlagColour ("C");
		
		final PlayerPublicDetails player = new PlayerPublicDetails (null, null, trans);
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, 1, "getModifiedImage")).thenReturn (player);
		
		// Wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (mem);

		final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
		wizardDetails.setWizardID ("WZ01");
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), 1, "getModifiedImage")).thenReturn (wizardDetails);
		
		// Client
		final MomClient client = mock (MomClient.class);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
				
		// Base image
		final BufferedImage src = new BufferedImage (2, 1, BufferedImage.TYPE_INT_ARGB);
		
		final NdgUIUtils utils = mock (NdgUIUtils.class);
		when (utils.loadImage ("test.png")).thenReturn (src);
		
		// Image being coloured
		final BufferedImage intermediate1 = new BufferedImage (2, 1, BufferedImage.TYPE_INT_ARGB);
		when (utils.multiplyImageByColour (src, 12)).thenReturn (intermediate1);

		final BufferedImage intermediate2 = new BufferedImage (2, 1, BufferedImage.TYPE_INT_ARGB);
		when (utils.multiplyImageByColour (intermediate1, 13)).thenReturn (intermediate2);

		final BufferedImage dest = new BufferedImage (2, 1, BufferedImage.TYPE_INT_ARGB);
		when (utils.multiplyImageByColour (intermediate2, 14)).thenReturn (dest);
		
		// Set up object to test
		final PlayerColourImageGeneratorImpl gen = new PlayerColourImageGeneratorImpl ();
		gen.setUtils (utils);
		gen.setClient (client);
		gen.setMultiplayerSessionUtils (multiplayerSessionUtils);
		gen.setKnownWizardUtils (knownWizardUtils);
		
		// Call method
		final BufferedImage image = gen.getModifiedImage ("test.png", true, null, null, null, 1, Arrays.asList ("D", "E"));
		
		// Check results
		assertSame (dest, image);
	}

	/**
	 * Tests the getModifiedImage method colouring the flag portion of an image
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetModifiedImage_colourFlag () throws Exception
	{
		// Player's colour
		final List<PlayerPublicDetails> players = new ArrayList<PlayerPublicDetails> ();
		
		final MomTransientPlayerPublicKnowledge trans = new MomTransientPlayerPublicKnowledge ();
		trans.setFlagColour ("C");
		
		final PlayerPublicDetails player = new PlayerPublicDetails (null, null, trans);
		
		final MultiplayerSessionUtils multiplayerSessionUtils = mock (MultiplayerSessionUtils.class);
		when (multiplayerSessionUtils.findPlayerWithID (players, 1, "getModifiedImage")).thenReturn (player);
		
		// Wizard
		final FogOfWarMemory mem = new FogOfWarMemory ();
		
		final MomPersistentPlayerPrivateKnowledge priv = new MomPersistentPlayerPrivateKnowledge ();
		priv.setFogOfWarMemory (mem);

		final KnownWizardDetails wizardDetails = new KnownWizardDetails ();
		wizardDetails.setWizardID ("WZ01");
		
		final KnownWizardUtils knownWizardUtils = mock (KnownWizardUtils.class);
		when (knownWizardUtils.findKnownWizardDetails (mem.getWizardDetails (), 1, "getModifiedImage")).thenReturn (wizardDetails);
		
		// Client
		final MomClient client = mock (MomClient.class);
		when (client.getPlayers ()).thenReturn (players);
		when (client.getOurPersistentPlayerPrivateKnowledge ()).thenReturn (priv);
		
		// Base image
		final BufferedImage src = new BufferedImage (2, 1, BufferedImage.TYPE_INT_ARGB);
		src.setRGB (0, 0, 0xFF000080);
		src.setRGB (1, 0, 0xFF000081);
		
		final NdgUIUtils utils = mock (NdgUIUtils.class);
		when (utils.loadImage ("test.png")).thenReturn (src);
		
		// Flag being coloured
		final BufferedImage baseFlag = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		when (utils.loadImage ("flag.png")).thenReturn (baseFlag);
		
		final BufferedImage colouredFlag = new BufferedImage (1, 1, BufferedImage.TYPE_INT_ARGB);
		colouredFlag.setRGB (0, 0, 0xFF000082);
		when (utils.multiplyImageByColour (baseFlag, 12)).thenReturn (colouredFlag);		
				
		// Set up object to test
		final PlayerColourImageGeneratorImpl gen = new PlayerColourImageGeneratorImpl ();
		gen.setUtils (utils);
		gen.setClient (client);
		gen.setMultiplayerSessionUtils (multiplayerSessionUtils);
		gen.setKnownWizardUtils (knownWizardUtils);
		
		// Call method
		final BufferedImage image = gen.getModifiedImage ("test.png", false, "flag.png", 1, 0, 1, null);
		
		// Check results
		assertEquals (0xFF000080, image.getRGB (0, 0));
		assertEquals (0xFF000082, image.getRGB (1, 0));
	}

	/**
	 * Tests the getModifiedImage method applying shading to a base image that doesn't need any parts in the player's colour
	 * @throws Exception If there is a problem
	 */
	@Test
	public final void testGetModifiedImage_shadingOnly () throws Exception
	{
		// Base image
		final BufferedImage src = new BufferedImage (2, 1, BufferedImage.TYPE_INT_ARGB);
		
		final NdgUIUtils utils = mock (NdgUIUtils.class);
		when (utils.loadImage ("test.png")).thenReturn (src);
		
		// Image being coloured
		final BufferedImage intermediate2 = new BufferedImage (2, 1, BufferedImage.TYPE_INT_ARGB);
		when (utils.multiplyImageByColour (src, 13)).thenReturn (intermediate2);

		final BufferedImage dest = new BufferedImage (2, 1, BufferedImage.TYPE_INT_ARGB);
		when (utils.multiplyImageByColour (intermediate2, 14)).thenReturn (dest);
		
		// Set up object to test
		final PlayerColourImageGeneratorImpl gen = new PlayerColourImageGeneratorImpl ();
		gen.setUtils (utils);
		
		// Call method
		final BufferedImage image = gen.getModifiedImage ("test.png", true, null, null, null, null, Arrays.asList ("D", "E"));
		
		// Check results
		assertSame (dest, image);
	}
}