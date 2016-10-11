package org.histo.model.transitory;

public class PhysicianRoleOptions {
	
	private boolean surgeon = true;
	private boolean privatePhysician = true;
	private boolean other = true;
	private boolean archived = false;
	
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
}
