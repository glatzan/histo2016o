package org.histo.action.dialog;

import org.apache.log4j.Logger;
import org.histo.action.MainHandlerAction;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomNotUniqueReqest;
import org.histo.dao.GenericDAO;
import org.histo.model.patient.Task;
import org.histo.util.UniqueRequestID;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.Getter;
import lombok.Setter;

public abstract class AbstractDialog {

	protected static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	protected GenericDAO genericDAO;

	@Autowired
	protected MainHandlerAction mainHandlerAction;

	@Autowired
	protected ResourceBundle resourceBundle;

	@Getter
	@Setter
	protected Task task;

	@Getter
	@Setter
	protected Dialog dilaog;

	@Getter
	@Setter
	protected UniqueRequestID uniqueRequestID = new UniqueRequestID();

	public void initAndPrepareBean(Task task, Dialog dialog) {
		initBean(task, dialog);
		prepareDialog();
	}

	public void initBean(Task task, Dialog dialog) {
		initBean(task, dialog, false);
	}

	public void initBean(Task task, Dialog dialog, boolean uniqueRequestEnabled) {
		setTask(task);
		setDilaog(dialog);
		getUniqueRequestID().setEnabled(uniqueRequestEnabled);
		if (uniqueRequestEnabled)
			getUniqueRequestID().nextUniqueRequestID();
	}

	/**
	 * Method for displaying the associated dialog.
	 */
	public void prepareDialog() {
		mainHandlerAction.showDialog(dilaog);
	}

	/**
	 * Method for hiding the associated dialog.
	 * 
	 * @throws CustomNotUniqueReqest
	 */
	public void hideDialog() {
		mainHandlerAction.hideDialog(dilaog);
	}

	public void onDatabaseVersionConflict() {
		hideDialog();
		mainHandlerAction.addQueueGrowlMessage(resourceBundle.get("growl.version.error"),
				resourceBundle.get("growl.version.error.text"));
	}
}
