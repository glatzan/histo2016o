package org.histo.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;

public class URLConnection {

	private final String USER_AGENT = "Mozilla/5.0";

	public String getRequest(String url) {

		StringBuffer response = new StringBuffer();

		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			// optional default is GET
			con.setRequestMethod("GET");

			// add request header
			con.setRequestProperty("User-Agent", USER_AGENT);

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}

		return response.toString();
	}
}
