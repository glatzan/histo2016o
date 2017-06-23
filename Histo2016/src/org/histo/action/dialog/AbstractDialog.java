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

public abstract class AbstractDialog {

	protected static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	protected GenericDAO genericDAO;

	@Autowired
	protected MainHandlerAction mainHandlerAction;

	@Autowired
	protected ResourceBundle resourceBundle;

	protected Task task;

	protected Dialog dilaog;

	protected UniqueRequestID uniqueRequestID = new UniqueRequestID();

	public void initAndPrepareBean(Task task, Dialog dialog) {
		initBean(task, dialog);
		prepareDialog();
	}

	public void initBean(Task task, Dialog dialog) {
		setTask(task);
		setDilaog(dialog);
		uniqueRequestID.nextUniqueRequestID();
	}

	/**
	 * Method for displaying the associated dialog.
	 */
	public void prepareDialog() {
		mainHandlerAction.showDialog(dilaog);
	}

	/**
	 * Method for hiding the associated dialog.
	 * @throws CustomNotUniqueReqest 
	 */
	public void hideDialog(){
		uniqueRequestID.checkUniqueRequestID(true);uniqueRequestID.checkUniqueRequestID(true);
		mainHandlerAction.hideDialog(dilaog);
	}

	public void onDatabaseVersionConflict(){
		hideDialog();
		mainHandlerAction.sendGrowlMessages(resourceBundle.get("growl.version.error"),
				resourceBundle.get("growl.version.error.text"));
	}

	// ************************ Getter/Setter ************************
	public Task getTask() {
		return task;
	}

	public void setTask(Task task) {
		this.task = task;
	}

	public Dialog getDilaog() {
		return dilaog;
	}

	public void setDilaog(Dialog dilaog) {
		this.dilaog = dilaog;
	}

	public UniqueRequestID getUniqueRequestID() {
		return uniqueRequestID;
	}

	public void setUniqueRequestID(UniqueRequestID uniqueRequestID) {
		this.uniqueRequestID = uniqueRequestID;
	}

	// ************************ Getter/Setter ************************

}
