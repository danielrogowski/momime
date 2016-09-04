package momime.client.config;

import momime.client.config.v0_9_8.MomImeClientConfig;
import momime.client.config.v0_9_8.ObjectFactory;

/**
 * Creates our custom extended client config when it is unmarshalled with JAXB
 */
public final class ClientConfigFactory extends ObjectFactory
{
	/**
	 * @return Custom extended client config 
	 */
	@Override
	public final MomImeClientConfig createMomImeClientConfig ()
	{
		return new MomImeClientConfigEx ();
	}
}