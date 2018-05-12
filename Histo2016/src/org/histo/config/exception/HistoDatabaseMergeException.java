package org.histo.config.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HistoDatabaseMergeException extends RuntimeException {

	private static final long serialVersionUID = -6690684165543685979L;
	
	private Object oldVersion;

	public HistoDatabaseMergeException(Object oldVersion) {
		super("Inconsistent Version in Database");
		this.oldVersion = oldVersion;
	}
}
