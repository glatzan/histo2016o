package org.histo.action.dialog.task;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.GlobalEditViewHandler;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.PatientDao;
import org.histo.dao.TaskDAO;
import org.histo.model.patient.Task;
import org.histo.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class ArchiveTaskDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskDAO taskDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PatientDao patientDao;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GlobalEditViewHandler globalEditViewHandler;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskService taskService;

	/**
	 * If true the task will be removed from worklist
	 */
	private boolean removeFromWorklist;

	/**
	 * commentary for restoring
	 */
	private String commentary;

	/**
	 * True if task was archived
	 */
	private boolean archiveSuccessful;

	public void initAndPrepareBean(Task task) {
		if (initBean(task))
			prepareDialog();
	}

	public boolean initBean(Task task) {
		try {
			taskDAO.initializeTask(task, false);
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.replaceTaskInCurrentWorklist(task, false);
		}

		this.removeFromWorklist = true;

		super.initBean(task, Dialog.TASK_ARCHIVE);

		return true;
	}

	public void archiveTask() {
		try {

			taskService.archiveTask(getTask());

			if (removeFromWorklist) {
				// only remove from worklist if patient has one active task
				if (task.getPatient().getTasks().stream().filter(p -> !p.isFinalized()).count() > 1) {
					mainHandlerAction.sendGrowlMessagesAsResource("growl.error",
							"growl.error.worklist.remove.moreActive");
				} else {
					worklistViewHandlerAction.removePatientFromCurrentWorklist(task.getPatient());
					worklistViewHandlerAction.onDeselectPatient(true);
				}
			}

			mainHandlerAction.sendGrowlMessagesAsResource("growl.task.archived", "growl.task.archived.text");
			setArchiveSuccessful(true);
		} catch (Exception e) {
			onDatabaseVersionConflict();
		}
	}

	public void hideDialog() {
		super.hideDialog(new Boolean(archiveSuccessful));
	}
}
