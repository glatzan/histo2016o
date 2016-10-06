package org.histo.config.enums;

public enum Role {
    GUEST(1), USER(100), MTA(200), PHYSICIAN(300), MODERATOR(400), ADMIN(500);

    private final int value;

    Role(final int newValue) {
	value = newValue;
    }

    public int getRoleValue() {
	return value;
    }
}
