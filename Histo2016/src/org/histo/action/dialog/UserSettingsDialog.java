package org.histo.action.dialog;

import org.histo.action.UserHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.model.HistoUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class UserSettingsDialog extends AbstractDialog {

	@Autowired
	private UserHandlerAction userHandlerAction;

	private HistoUser user;

	public void initAndPrepareBean() {
		if (initBean())
			prepareDialog();
	}

	public boolean initBean() {
		super.initBean(null, Dialog.USER_SETTINGS);
		setUser(userHandlerAction.getCurrentUser());
		return true;
	}

	// ************************ Getter/Setter ************************
	public HistoUser getUser() {
		return user;
	}

	public void setUser(HistoUser user) {
		this.user = user;
	}
}
