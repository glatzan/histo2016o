package org.histo.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;

import com.lowagie.text.pdf.PdfReader;

public class FileUtil {

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
			BufferedReader br = new BufferedReader(new InputStreamReader(is,"UTF-8"));

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

}
