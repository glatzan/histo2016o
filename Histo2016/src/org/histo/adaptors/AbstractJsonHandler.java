package org.histo.adaptors;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.log4j.Logger;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AbstractJsonHandler {

	private static Logger logger = Logger.getLogger("org.histo");

	/**
	 * Loaded from json
	 */
	protected String baseUrl;
	
	/**
	 * Loaded from json
	 */
	protected String userAgent;
	
	/**
	 * Returns a json string grabbed from the given url.
	 * 
	 * @param url
	 * @return
	 */
	public String requestJsonData(String url) {

		StringBuffer response = new StringBuffer();

		try {

			logger.debug("Fetching patient json from clinic backend: " + url);

			URL obj = new URL(url);

			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			// optional default is GET
			con.setRequestMethod("GET");

			// add request header
			con.setRequestProperty("User-Agent", userAgent);

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}

		logger.debug("Fetch string from clinic: " + response.toString());
		return response.toString();
	}
}
