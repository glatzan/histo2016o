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
import org.primefaces.context.RequestContext;
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
		HashMap<String, Object> options = new HashMap<String, Object>();

		if (getDilaog().getWidth() != 0) {
			options.put("width", getDilaog().getWidth());
			options.put("contentWidth", getDilaog().getWidth());
		} else
			options.put("width", "auto");

		if (getDilaog().getHeight() != 0) {
			options.put("contentHeight", getDilaog().getHeight());
			options.put("height", getDilaog().getHeight());
		} else
			options.put("height", "auto");

		if (getDilaog().isUseOptions()) {
			options.put("resizable", getDilaog().isResizeable());
			options.put("draggable", getDilaog().isDraggable());
			options.put("modal", getDilaog().isModal());
		}

		options.put("closable", false);

		if (getDilaog().getHeader() != null)
			options.put("headerElement", "dialogForm:header");

		RequestContext.getCurrentInstance().openDialog(getDilaog().getPath(), options, null);

		logger.debug("Showing Dialog: " + getDilaog());
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
		RequestContext.getCurrentInstance().closeDialog(returnValue);
	}

	public void onDatabaseVersionConflict() {
		hideDialog();
		mainHandlerAction.addQueueGrowlMessage(resourceBundle.get("growl.version.error"),
				resourceBundle.get("growl.version.error.text"));

		throw new AbortProcessingException();
	}
}
