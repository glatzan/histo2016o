package org.histo.config.enums;

import com.sun.prism.Material;

public enum SettingsTab {
	USER(0), STAINING(1), MATERIAL(2), DIAGNOSIS(3), PHYSICIAN(4), MISELLANEOUS(5), LOG(6), P_LIST(PHYSICIAN), P_EDIT(
			PHYSICIAN), P_ADD_EXTER(PHYSICIAN), P_ADD_LDPA(PHYSICIAN), M_LIST(MATERIAL), M_EDIT(MATERIAL), M_ADD_STAINING(MATERIAL);

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
