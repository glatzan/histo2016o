package org.histo.config.enums;

public enum Worklist {

	DEFAULT("default");
	
	private final String name;

	Worklist(final String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
