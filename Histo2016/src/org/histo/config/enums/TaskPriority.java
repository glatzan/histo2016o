package org.histo.config.enums;

public enum TaskPriority {
	NONE(0), HIGH(50), TIME(100);

	private final int priority;

	TaskPriority(final int priority) {
		this.priority = priority;
	}

	public int getPriority() {
		return priority;
	}
}
