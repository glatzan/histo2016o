package org.histo.action.dialog.task;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.TaskDAO;
import org.histo.model.patient.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class ChangeTaskIDDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskDAO taskDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private WorklistViewHandlerAction worklistViewHandlerAction;

	private String origID;

	public void initAndPrepareBean(Task task) {
		if (initBean(task))
			prepareDialog();
	}

	/**
	 * Initializes the bean and calles updatePhysicianLists at the end.
	 * 
	 * @param task
	 */
	public boolean initBean(Task task) {
		try {
			taskDAO.initializeTask(task, false);
			taskDAO.initializeCouncils(task);

			setOrigID(task.getTaskID());
			super.initBean(task, Dialog.TASK_CHANGE_ID);

			return true;
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.onVersionConflictTask(task, false);
			return false;
		}
	}

	public void changeTaskID() {
		try {

			// do nothing if the same
			if (origID != null && origID.equals(task.getTaskID()))
				return;

			logger.debug("Changing task id form " + origID + " to " + task.getTaskID());

			genericDAO.savePatientData(task, "log.patient.task.changeID", origID, task.getTaskID());

		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	public void validateTaskID(FacesContext context, UIComponent componentToValidate, Object value)
			throws ValidatorException {

		if (value.equals(origID)) {
			return;
		}
		if (value == null || value.toString().length() != 6) {
			throw new ValidatorException(
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "Auftragsnummer muss sechs Zahlen enthalten."));
		} else if (!value.toString().matches("[0-9]{6}")) {
			throw new ValidatorException(
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "Auftragsnummer darf nur Zahlen enthalten"));
		} else if (taskDAO.isTaskIDPresentInDatabase(value.toString())) {
			throw new ValidatorException(
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "", "Auftragsnummer bereits vorhanden"));
		}
	}
}
