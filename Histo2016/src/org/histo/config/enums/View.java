package org.histo.config.enums;

import lombok.Getter;

/**
 * View 
 * @author andi
 *
 */
@Getter
public enum View {

	LOGIN("/login.xhtml"), 
	GUEST("/pages/guest.xhtml"),
	SCIENTIST("/pages/scientist.xhtml"),
	WORKLIST("/pages/worklist.xhtml"),
	WORKLIST_BLANK("/pages/worklist/blank.xhtml", WORKLIST),
	WORKLIST_DATA_ERROR("/pages/worklist/dataError.xhtml", WORKLIST),
	WORKLIST_NOTHING_SELECTED("/pages/worklist/notSelected.xhtml", WORKLIST),
	WORKLIST_TASKS("/pages/worklist/taskList.xhtml", WORKLIST),
	WORKLIST_PATIENT("/pages/worklist/patient.xhtml", WORKLIST),
	WORKLIST_RECEIPTLOG("/pages/worklist/receiptlog.xhtml", WORKLIST, true),
	WORKLIST_DIAGNOSIS("/pages/worklist/diagnosis.xhtml", WORKLIST, true),
	WORKLIST_REPORT("/pages/worklist/report.xhtml", WORKLIST, true);

	private final String path;

	private final View rootView;
	
	/**
	 * True if persistent view
	 */
	private boolean lastSubviewAble;
	
	View(final String path) {
		this.path = path;
		this.rootView = null;
		this.lastSubviewAble = false;
	}
	
	View(final String path, View parentView) {
		this.path = path;
		this.rootView = parentView;
		this.lastSubviewAble = false;
	}
	
	View(final String path, View parentView, boolean lastSubviewAble) {
		this.path = path;
		this.rootView = parentView;
		this.lastSubviewAble = lastSubviewAble;
	}

	public String getRootPath(){
		if(rootView != null)
			return rootView.getPath();
		return getPath();
	}
}
