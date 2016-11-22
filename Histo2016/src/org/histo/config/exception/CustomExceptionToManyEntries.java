package org.histo.config.exception;

/**
 * Is thrown if to many search result are found in the clinic database
 * 
 * @author glatza
 *
*/
public class CustomExceptionToManyEntries extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1276141793052183990L;

	public CustomExceptionToManyEntries() {
		super("To many search results found");
	}
	
}
