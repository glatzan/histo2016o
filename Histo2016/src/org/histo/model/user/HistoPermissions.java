package org.histo.model.user;

public enum HistoPermissions {
	SHOW_SETTINGS_DIALOG, 
	EDIT_USER_SETTINGS (SHOW_SETTINGS_DIALOG),
	EDIT_GROUP_SETTINGS (SHOW_SETTINGS_DIALOG), 
	EDIT_DIAGNOSES_SETTINGS (SHOW_SETTINGS_DIALOG),
	EDIT_MATERIAL_SETTINGS (SHOW_SETTINGS_DIALOG),
	EDIT_STAINING_SETTINGS (SHOW_SETTINGS_DIALOG),
	EDIT_ITEM_LISTS_SETTINGS (SHOW_SETTINGS_DIALOG),
	EDIT_FAVOURITE_LIST_SETTINGS (SHOW_SETTINGS_DIALOG),
	EDIT_PHYSICIANS_SETTINGS (SHOW_SETTINGS_DIALOG),
	EDIT_ORGANIZATIONS_SETTINGS (SHOW_SETTINGS_DIALOG),
	EDIT_TASK;

	private HistoPermissions parent;

	HistoPermissions() {
		this(null);
	}

	HistoPermissions(HistoPermissions parent) {
		this.parent = parent;
	}

	public HistoPermissions getParent() {
		return parent;
	}
	
	public int getLevel() {
		if(parent != null)
			return 1 + parent.getLevel();
		return 1;
	}

}