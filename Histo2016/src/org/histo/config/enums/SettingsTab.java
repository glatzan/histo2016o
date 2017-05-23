package org.histo.config.enums;

public enum SettingsTab {
	USER(0), STAINING(1), MATERIAL(2), DIAGNOSIS(3), PHYSICIAN(4), STATIC_LISTS(5), FAVOURITE_LIST(6), ADMIN(7), LOG(8), P_LIST(
			PHYSICIAN), P_EDIT(PHYSICIAN),P_EDIT_EXTERN(PHYSICIAN), P_ADD_EXTER(PHYSICIAN), P_ADD_LDPA(PHYSICIAN), M_LIST(
					MATERIAL), M_EDIT(MATERIAL), M_ADD_STAINING(
							MATERIAL), S_LIST(STATIC_LISTS), S_EDIT(STATIC_LISTS), U_LIST(USER), U_EDIT(USER), F_LIST(FAVOURITE_LIST), F_EDIT(FAVOURITE_LIST);

	private final SettingsTab parent;
	private final int tabNumber;

	SettingsTab(int tabNumber) {
		this.tabNumber = tabNumber;
		this.parent = null;
	}

	SettingsTab(SettingsTab parent) {
		this.parent = parent;
		this.tabNumber = parent.getTabNumber();
	}

	public SettingsTab getParent() {
		return parent;
	}

	public int getTabNumber() {
		return tabNumber;
	}
}
