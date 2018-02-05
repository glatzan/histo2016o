package org.histo.action.dialog.print;

import org.histo.action.dialog.AbstractDialog;
import org.histo.config.enums.Dialog;
import org.histo.model.patient.Task;
import org.histo.model.user.HistoUser;
import org.histo.ui.selectors.ContactSelector;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.Setter;

@Component
@Scope(value = "session")
@Setter
public class SelectPrinter extends AbstractDialog {
	
	private HistoUser user;
	
	public void initAndPrepareBean(HistoUser user) {
		initBean(user);
		prepareDialog();
	}

	public void initBean(HistoUser user) {
		this.user = user;

		super.initBean(task, Dialog.PRINT_SELECT_PRINTER, false);
	}
	
}
