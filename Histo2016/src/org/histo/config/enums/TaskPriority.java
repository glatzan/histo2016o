package org.histo.config.enums;

public enum TaskPriority {
	LOW(0), MEDIUM(50), HIGHT(100);

	private final int priority;

	TaskPriority(final int priority) {
		this.priority = priority;
	}

	public int getPriority() {
		return priority;
	}
}
