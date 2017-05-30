package org.histo.config.exception;

public class CustomNullPatientExcepetion extends Exception {
	
	private static final long serialVersionUID = -5075979942901332921L;

	public CustomNullPatientExcepetion() {
		super("Patient with null data returned");
	}
}
