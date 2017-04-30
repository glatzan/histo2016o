package org.histo.util.interfaces;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;

public interface FileHandlerUtil extends HasLogger {

	/**
	 * Reads the content of a template and returns the content as string.
	 * 
	 * @param file
	 * @return
	 */
	public static String getContentOfFile(String file) {

		logger.debug("Getting content of file one of " + file);
		ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext();
		Resource resource = appContext.getResource(file);
		String toPrint = null;
		try {
			toPrint = IOUtils.toString(resource.getInputStream(), "UTF-8");
			logger.debug("File found, size " + toPrint.length());
		} catch (IOException e) {
			logger.error(e);
		} finally {
			appContext.close();
		}

		return toPrint;
	}
}
