package org.histo.config.exception;

import lombok.Getter;
import lombok.Setter;

public class CustomDatabaseConstraintViolationException extends RuntimeException {

	private static final long serialVersionUID = 3202722948468001962L;

	@Getter
	@Setter
	private Object oldVersion;
	
	public CustomDatabaseConstraintViolationException(Object oldVersion) {
		super("ConstraintViolationException");
		this.oldVersion = oldVersion;
	}
}
