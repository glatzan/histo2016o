package org.histo.config.enums;

public enum Pages {

	LOGIN("/pages/login.xhtml"), 
	GUEST("/pages/guest.xhtml"),
	SCIENTIST("/pages/scientist.xhtml"),
	USERLIST("/pages/userList.xhtml"),
	WORKLIST_PATIENT("/pages/worklist_patient.xhtml"),
	WORKLIST_RECEITLOG("/pages/worklist_receiptlog.xhtml"),
	WORKLIST_DIAGNOSIS("/pages/worklist_diagnosis.xthml");

	private final String path;

	Pages(final String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}
}
