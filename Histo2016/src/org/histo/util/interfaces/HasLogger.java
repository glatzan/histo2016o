package org.histo.util.interfaces;

import org.apache.log4j.Logger;

/**
 * Logger Interfaces for histo
 * @author andi
 *
 */
public interface HasLogger {
	
	public static Logger logger = Logger.getLogger("org.histo");

	public static Logger getLogger() {
		return logger;
	}
}
