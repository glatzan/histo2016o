package org.histo.config.exception;

import lombok.Getter;
import lombok.Setter;

public class HistoDatabaseConstraintViolationException extends RuntimeException {

	private static final long serialVersionUID = 3202722948468001962L;

	@Getter
	@Setter
	private Object oldVersion;
	
	public HistoDatabaseConstraintViolationException(Object oldVersion) {
		super("ConstraintViolationException");
		this.oldVersion = oldVersion;
	}
}
