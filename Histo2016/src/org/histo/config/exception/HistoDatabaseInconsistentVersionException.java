package org.histo.config.exception;

import lombok.Getter;
import lombok.Setter;

public class HistoDatabaseInconsistentVersionException extends RuntimeException {

	private static final long serialVersionUID = 3202722948468001962L;

	@Getter
	@Setter
	private Object oldVersion;

	public HistoDatabaseInconsistentVersionException(Object oldVersion) {
		super("Inconsistent Version in Database");
		this.oldVersion = oldVersion;
	}
}
