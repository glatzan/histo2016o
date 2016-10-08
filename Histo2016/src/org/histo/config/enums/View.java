package org.histo.config.enums;

public enum View {

	LOGIN("/pages/login.xhtml"), 
	GUEST("/pages/guest.xhtml"),
	SCIENTIST("/pages/scientist.xhtml"),
	USERLIST("/pages/userList.xhtml"),
	WORKLIST("/pages/worklist.xhtml"),
	WORKLIST_BLANK("/pages/worklist/blank.xhtml", WORKLIST),
	WORKLIST_PATIENT("/pages/worklist/patient.xhtml", WORKLIST),
	WORKLIST_RECEIPTLOG("/pages/worklist/receiptlog.xhtml", WORKLIST),
	WORKLIST_DIAGNOSIS("/pages/worklist/diagnosis.xhtml", WORKLIST);

	private final String path;

	private final View parentView;
	
	View(final String path) {
		this.path = path;
		this.parentView = null;
	}
	
	View(final String path, View parentView) {
		this.path = path;
		this.parentView = parentView;
	}

	public String getPath() {
		return path;
	}
	
	public View getParentView(){
		return parentView;
	}
}
