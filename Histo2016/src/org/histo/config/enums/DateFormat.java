package org.histo.config.enums;

public enum DateFormat {

	GERMAN_DATE("dd.MM.yyyy"), GERMAN_TIME_DATE("HH:mm:ss dd.MM.yyyy"), GERMAN_DATE_TIME("dd.MM.yyyy HH:mm:ss "),TIME("HH:mm:ss");

	private final String dateFormat;

	DateFormat(final String format) {
		this.dateFormat = format;
	}

	public String getDateFormat() {
		return dateFormat;
	}
}
