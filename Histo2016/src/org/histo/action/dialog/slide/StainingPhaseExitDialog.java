package org.histo.action.dialog.slide;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.GlobalEditViewHandler;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.patient.Task;
import org.histo.service.SampleService;
import org.histo.ui.task.TaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class StainingPhaseExitDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskDAO taskDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GlobalEditViewHandler globalEditViewHandler;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private SampleService sampleService;

	/**
	 * If true the task will be removed from worklist
	 */
	private boolean removeFromWorklist;

	/**
	 * If true the task will be removed from staining list
	 */
	private boolean removeFromStainingList;

	/**
	 * If true the staining phase of the task will be finished
	 */
	private boolean endStainingPhase;

	/**
	 * If true the task will be shifted to the diagnosis phase
	 */
	private boolean goToDiagnosisPhase;

	/**
	 * Initializes the bean and shows the dialog
	 * 
	 * @param patient
	 */
	public void initAndPrepareBean(Task task) {
		initBean(task, false);
		prepareDialog();
	}

	public void initAndPrepareBean(Task task, boolean autoCompleteStainings) {
		initBean(task, autoCompleteStainings);
		prepareDialog();
	}

	/**
	 * Initializes all field of the object
	 * 
	 * @param task
	 */
	public void initBean(Task task, boolean autoCompleteStainings) {
		try {
			taskDAO.initializeTask(task, false);

			if (task.isListedInFavouriteList(PredefinedFavouriteList.NotificationList))
				this.goToDiagnosisPhase = false;
			else
				this.goToDiagnosisPhase = true;

			// all slides will be marked as completed by endStainingphase methode
			if (autoCompleteStainings) {
				setRemoveFromStainingList(true);
				setRemoveFromWorklist(true);
				setEndStainingPhase(true);
			} else {
				boolean stainingCompleted = TaskStatus.checkIfStainingCompleted(task);

				setRemoveFromStainingList(stainingCompleted);
				setRemoveFromWorklist(stainingCompleted);
				setEndStainingPhase(stainingCompleted);
			}

			setGoToDiagnosisPhase(true);

		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.replaceTaskInCurrentWorklist(task, false);
		}

		super.initBean(task, Dialog.STAINING_PHASE_EXIT);
	}

	public void exitPhase() {
		try {

			if (endStainingPhase && removeFromStainingList) {
				// ending staining pahse
				sampleService.endStainingPhase(task, removeFromStainingList);

				mainHandlerAction.sendGrowlMessages(resourceBundle.get("growl.staining.endAll"), resourceBundle.get(
						goToDiagnosisPhase ? "growl.staining.endAll.text.true" : "growl.staining.endAll.text.false"));
			} else {
				if (removeFromStainingList)
					favouriteListDAO.removeTaskFromList(task, PredefinedFavouriteList.StainingList,
							PredefinedFavouriteList.ReStainingList);
			}

			if (goToDiagnosisPhase) {
				logger.debug("Adding Task to diagnosis list");
				favouriteListDAO.addTaskToList(task, PredefinedFavouriteList.DiagnosisList);
			}

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

		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}
}
