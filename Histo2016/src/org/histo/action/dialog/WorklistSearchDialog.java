package org.histo.action.dialog;

import org.histo.config.enums.Dialog;
import org.histo.model.patient.Task;

public class WorklistSearchDialog extends AbstractDialog {

	public void initBean(Task task) {
		super.initBean(task, Dialog.WORKLIST_SEARCH);
		// setting associatedBioBank
	}
}
