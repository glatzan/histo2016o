package org.histo.ui;

import org.histo.model.HistoUser;

public class UserAccRoleHolder {

	private HistoUser histoUser;

	private String roleName;

	public UserAccRoleHolder(HistoUser histoUser) {
		setUserAcc(histoUser);
		setRoleName(histoUser.getRole().getName());
	}

	public HistoUser getUserAcc() {
		return histoUser;
	}

	public void setUserAcc(HistoUser histoUser) {
		this.histoUser = histoUser;
	}

	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

}
