package org.histo.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;

import com.lowagie.text.pdf.PdfReader;

public class HistoUtil {

	/**
	 * Reads a file form the passed path.
	 * 
	 * @param path
	 * @return
	 */
	public static final String loadTextFile(String path) {

		ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext();

		Resource resource = appContext.getResource(path);

		StringBuffer result = new StringBuffer();

		try {
			InputStream is = resource.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));

			String line;
			while ((line = br.readLine()) != null) {
				result.append(line);
			}
			br.close();
			appContext.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return result.toString();
	}

	/**
	 * Task a HashMap with key value pairs and replaces all entries within a
	 * string.
	 * 
	 * @param text
	 * @param replace
	 * @return
	 */
	public static final String replaceWildcardsInString(String text, HashMap<String, String> replace) {
		for (Map.Entry<String, String> entry : replace.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			text = text.replace(key, value);
		}
		return text;
	}

	/**
	 * Adds chars at the beginning of a string.
	 * 
	 * @param value
	 * @param len
	 * @param fitChar
	 * @return
	 */
	public final static String fitString(int value, int len, char fitChar) {
		return fitString(String.valueOf(value), len, fitChar);
	}

	/**
	 * Adds chars at the beginning of a string.
	 * 
	 * @param value
	 * @param len
	 * @param fitChar
	 * @return
	 */
	public final static String fitString(String value, int len, char fitChar) {
		StringBuilder str = new StringBuilder(value);
		while (str.length() < len) {
			str.insert(0, fitChar);
		}
		return str.toString();
	}
}
