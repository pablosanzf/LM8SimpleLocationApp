package es.deusto.ivazquez.android.net;

/**
 * Interface to receive notifications about HTTP operations from {@link SimpleHttpClient}
 * @author ivazquez
 */
public interface SimpleHttpClientListener{
	/**
	 * @param result the contents for the HTTP operation, null if error
	 */
	public void resultAvailable(String result);
}

