package org.histo.action.dialog.task;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.GlobalEditViewHandler;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.PatientDao;
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

	/**
	 * If true the archive dialog is shown, otherwise the restore dialog
	 */
	private boolean archive;

	public void initAndPrepareBean(Task task, boolean archive) {
		if (initBean(task, archive))
			prepareDialog();
	}

	public boolean initBean(Task task, boolean archive) {
		try {
			taskDAO.initializeTask(task, false);
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.onVersionConflictTask(task, false);
		}

		this.archive = archive;

		if (archive)
			super.initBean(task, Dialog.TASK_ARCHIVE);
		else
			super.initBean(task, Dialog.TASK_RESTORE);

		return true;
	}

	public void archiveTask() {
		try {

			// remove from all system lists
			favouriteListDAO.removeTaskFromList(getTask(), PredefinedFavouriteList.values());

			// finalizing task
			getTask().setFinalizationDate(System.currentTimeMillis());
			getTask().setFinalized(true);

			patientDao.save(getTask(), "log.patient.task.change.diagnosisPhase.archive", new Object[] { getTask() });

		} catch (Exception e) {
			onDatabaseVersionConflict();
		}

		globalEditViewHandler.updateDataOfTask(true, false, true, false);
	}

	public void restoreTask() {
		try {

			// finalizing task
			getTask().setFinalizationDate(0);
			getTask().setFinalized(false);

			patientDao.save(getTask(), "log.patient.task.change.diagnosisPhase.dearchive", new Object[] { getTask() });

		} catch (Exception e) {
			onDatabaseVersionConflict();
		}

		globalEditViewHandler.updateDataOfTask(true, false, true, false);
	}
}
