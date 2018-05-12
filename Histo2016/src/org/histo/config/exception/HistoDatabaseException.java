package org.histo.config.exception;

import lombok.Getter;
import lombok.Setter;

/**
 * Global Database exeption
 * 
 * @author andi
 *
 */
@Getter
@Setter
public class HistoDatabaseException extends RuntimeException {

	private static final long serialVersionUID = 5906132531367825820L;

	private Object oldVersion;

	public HistoDatabaseException(Object oldVersion) {
		super("Database Exception");
		this.oldVersion = oldVersion;
	}
}
