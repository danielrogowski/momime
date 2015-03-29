package momime.client.calculations.damage;

import java.io.IOException;

/**
 * Interface that damage calculation lines must implement so they can generate the text they need to display
 */
public interface DamageCalculationText
{
	/**
	 * Perform some pre-processing action
	 * @throws IOException If there is a problem
	 */
	public void preProcess () throws IOException;

	/**
	 * @return Text to display for this breakdown line
	 * @throws IOException If there is a problem
	 */
	public String getText () throws IOException;
}