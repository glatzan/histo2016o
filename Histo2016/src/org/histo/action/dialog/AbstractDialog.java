package org.histo.action.dialog;

import java.util.HashMap;

import javax.faces.event.AbortProcessingException;

import org.apache.log4j.Logger;
import org.histo.action.MainHandlerAction;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomNotUniqueReqest;
import org.histo.dao.GenericDAO;
import org.histo.model.patient.Task;
import org.histo.util.UniqueRequestID;
import org.primefaces.PrimeFaces;
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

	public void initAndPrepareBean(Dialog dialog) {
		initBean(null,dialog);
		prepareDialog();
	}
	
	public void initAndPrepareBean(Task task, Dialog dialog) {
		initBean(task, dialog);
		prepareDialog();
	}

	public boolean initBean(Task task, Dialog dialog) {
		return initBean(task, dialog, false);
	}

	public boolean initBean(Task task, Dialog dialog, boolean uniqueRequestEnabled) {
		setTask(task);
		setDilaog(dialog);
		getUniqueRequestID().setEnabled(uniqueRequestEnabled);
		if (uniqueRequestEnabled)
			getUniqueRequestID().nextUniqueRequestID();

		return true;
	}

	/**
	 * Method for displaying the associated dialog.
	 */

	public void prepareDialog() {
		prepareDialog(dilaog);
	}

	/**
	 * Method for displaying the associated dialog.
	 */
	public void prepareDialog(Dialog dialog) {
		HashMap<String, Object> options = new HashMap<String, Object>();

		if (dialog.getWidth() != 0) {
			options.put("width", dialog.getWidth());
			options.put("contentWidth", dialog.getWidth());
		} else
			options.put("width", "auto");

		if (dialog.getHeight() != 0) {
			options.put("contentHeight", dialog.getHeight());
			options.put("height", dialog.getHeight());
		} else
			options.put("height", "auto");

		if (dialog.isUseOptions()) {
			options.put("resizable", dialog.isResizeable());
			options.put("draggable", dialog.isDraggable());
			options.put("modal", dialog.isModal());
		}

		options.put("closable", false);

		if (dialog.getHeader() != null)
			options.put("headerElement", "dialogForm:header");

		PrimeFaces.current().dialog().openDynamic(dialog.getPath(), options, null);
 
		logger.debug("Showing Dialog: " + dialog);
	}

	/**
	 * Method for hiding the associated dialog.
	 * 
	 * @throws CustomNotUniqueReqest
	 */
	public void hideDialog() {
		hideDialog(null);
	}

	public void hideDialog(Object returnValue) {
		logger.debug("Hiding Dialog: " + getDilaog());
		PrimeFaces.current().dialog().closeDynamic(returnValue);
	}

	public void onDatabaseVersionConflict() {
		hideDialog();
		mainHandlerAction.sendGrowlMessagesAsResource("growl.error", "growl.error.version");

		throw new AbortProcessingException();
	}
}
