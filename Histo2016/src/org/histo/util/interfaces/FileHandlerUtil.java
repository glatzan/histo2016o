package org.histo.util.interfaces;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.apache.commons.io.FileUtils;
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

	public static boolean saveContentOfFile(File fileName, byte[] content) {
		try {
			FileUtils.writeByteArrayToFile(fileName, content);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static URI getAbsolutePath(String path) {
		ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext();
		Resource resource = appContext.getResource(path);
		URI result = null;
		try {
			result = resource.getURI();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			appContext.close();
		}

		return result;
	}
}
