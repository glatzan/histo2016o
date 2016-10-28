package org.histo.config.enums;

import java.util.ArrayList;

public enum ContactRole {
	NONE, FAMILY_PHYSICIAN, PRIVATE_PHYSICIAN, SURGEON, OTHER;

	/**
	 * Returns an arry with the passed roles.
	 * 
	 * @param surgeon
	 * @param extern
	 * @param other
	 * @param familyPhsysician
	 * @return
	 */
	public static final ContactRole[] getRoles(boolean surgeon, boolean extern, boolean other,
			boolean familyPhsysician) {
		ArrayList<ContactRole> roles = new ArrayList<ContactRole>();
		if (surgeon)
			roles.add(SURGEON);
		if (extern)
			roles.add(PRIVATE_PHYSICIAN);
		if (other)
			roles.add(OTHER);
		if (familyPhsysician)
			roles.add(FAMILY_PHYSICIAN);

		return roles.toArray(new ContactRole[roles.size()]);
	}
}
