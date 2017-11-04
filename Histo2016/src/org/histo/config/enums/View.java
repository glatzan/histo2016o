package org.histo.config.enums;

/**
 * View 
 * @author andi
 *
 */
public enum View {

	LOGIN("/pages/login.xhtml"), 
	GUEST("/pages/guest.xhtml"),
	SCIENTIST("/pages/scientist.xhtml"),
	USERLIST("/pages/userList.xhtml"),
	WORKLIST("/pages/worklist.xhtml"),
	WORKLIST_BLANK("/pages/worklist/blank.xhtml", WORKLIST),
	WORKLIST_TASKS("/pages/worklist/taskList.xhtml", WORKLIST),
	WORKLIST_PATIENT("/pages/worklist/patient.xhtml", WORKLIST),
	WORKLIST_RECEIPTLOG("/pages/worklist/receiptlog.xhtml", WORKLIST),
	WORKLIST_DIAGNOSIS("/pages/worklist/diagnosis.xhtml", WORKLIST);

	private final String path;

	private final View rootView;
	
	View(final String path) {
		this.path = path;
		this.rootView = null;
	}
	
	View(final String path, View parentView) {
		this.path = path;
		this.rootView = parentView;
	}

	public String getPath() {
		return path;
	}

	public String getRootPath(){
		if(rootView != null)
			return rootView.getPath();
		return getPath();
	}
	
	public View getRootView(){
		return rootView;
	}
}
