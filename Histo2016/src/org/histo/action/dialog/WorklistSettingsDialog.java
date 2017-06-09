package org.histo.action.dialog;

import org.histo.config.enums.Dialog;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
//TODO move to common dialog handler for dialogs without logic
public class WorklistSettingsDialog extends AbstractDialog {

	public void initAndPrepareBeanForSorting() {
		super.initBean(null, Dialog.WORKLIST_ORDER);
		prepareDialog();
	}
		
	public void initAndPrepareBeanForSettings() {
		super.initBean(null, Dialog.WORKLIST_SETTINGS);
		prepareDialog();
	}

}
