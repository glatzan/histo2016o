package org.histo.model.transitory;

/**
 * Container for manipulation the contact list in the settings and contact
 * dialog.
 * 
 * @author glatza
 *
 */
public class PhysicianRoleOptions {

	private boolean surgeon = true;
	private boolean privatePhysician = true;
	private boolean familyPhysician = true;
	private boolean other = true;
	private boolean archived = false;

	private boolean showAddedContactsOnly = false;

	public boolean isSurgeon() {
		return surgeon;
	}

	public void setSurgeon(boolean surgeon) {
		this.surgeon = surgeon;
	}

	public boolean isPrivatePhysician() {
		return privatePhysician;
	}

	public void setPrivatePhysician(boolean privatePhysician) {
		this.privatePhysician = privatePhysician;
	}

	public boolean isOther() {
		return other;
	}

	public void setOther(boolean other) {
		this.other = other;
	}

	public boolean isArchived() {
		return archived;
	}

	public void setArchived(boolean archived) {
		this.archived = archived;
	}

	public boolean isShowAddedContactsOnly() {
		return showAddedContactsOnly;
	}

	public void setShowAddedContactsOnly(boolean showAddedContactsOnly) {
		this.showAddedContactsOnly = showAddedContactsOnly;
	}

	public boolean isFamilyPhysician() {
		return familyPhysician;
	}

	public void setFamilyPhysician(boolean familyPhysician) {
		this.familyPhysician = familyPhysician;
	}

}
