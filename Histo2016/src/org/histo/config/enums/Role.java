package org.histo.config.enums;

import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {
	NONE_AUTH(0, "ROLE_NONEAUTH"), GUEST(1, "ROLE_GUEST"), SCIENTIST(50, "ROLE_SCIENTIST"), USER(100, "ROLE_USER"), MTA(
			200,
			"ROLE_MTA"), PHYSICIAN(300, "ROLE_PHYSICIAN"), MODERATOR(400, "ROLE_MODERATOR"), ADMIN(500, "ROLE_ADMIN");

	private final int value;

	private final String token;

	Role(final int value, final String token) {
		this.value = value;
		this.token = token;
	}

	public int getRoleValue() {
		return value;
	}

	public String getToken() {
		return token;
	}

	@Override
	public String getAuthority() {
		return getToken();
	}

	public static final Role getRoleByToken(String token) {
		Role[] roles = Role.values();

		for (int i = 0; i < roles.length; i++) {
			if (roles[i].getToken().equals(token))
				return roles[i];
		}

		return NONE_AUTH;
	}

}
