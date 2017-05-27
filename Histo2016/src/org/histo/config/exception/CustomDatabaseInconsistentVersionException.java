package org.histo.config.exception;

public class CustomDatabaseInconsistentVersionException extends Exception {

	private static final long serialVersionUID = 3202722948468001962L;

	public CustomDatabaseInconsistentVersionException() {
		super("Inconsistent Version in Database");
	}
}
