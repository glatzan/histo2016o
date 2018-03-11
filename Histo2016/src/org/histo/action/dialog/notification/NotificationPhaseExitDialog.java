package org.histo.action.dialog.notification;

import org.histo.action.UserHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.GlobalEditViewHandler;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.patient.Task;
import org.histo.service.NotificationService;
import org.histo.service.SampleService;
import org.histo.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class NotificationPhaseExitDialog extends AbstractDialog {

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

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserHandlerAction userHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private NotificationService notificationService;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskService taskService;

	/**
	 * If true the task will be removed from the notification list
	 */
	private boolean removeFromNotificationList;

	/**
	 * If true the notification phase of the task will terminated
	 */
	private boolean endNotificationPhase;

	/**
	 * If true the task will be removed from worklist
	 */
	private boolean removeFromWorklist;

	/**
	 * True if the phase was successfully left
	 */
	private boolean exitSuccessful;

	/**
	 * Initializes the bean and shows the dialog
	 * 
	 * @param patient
	 */
	public void initAndPrepareBean(Task task) {
		initBean(task);
		prepareDialog();
	}

	/**
	 * Initializes all field of the object
	 * 
	 * @param task
	 */
	public void initBean(Task task) {
		try {
			taskDAO.initializeTask(task, false);
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.onVersionConflictTask(task, false);
		}

		removeFromNotificationList = true;
		removeFromWorklist = true;

		exitSuccessful = false;

		endNotificationPhase = task.getDiagnosisRevisions().stream()
				.allMatch(p -> p.getNotificationDate() != 0 && !p.isNotificationPending());

		super.initBean(task, Dialog.NOTIFICATION_PHASE_EXIT);
	}

	/**
	 * Removes task from notification list and ends the notification phae. if set
	 * the task will be archived.
	 */
	public void exitPhase() {
		try {

			if (endNotificationPhase && removeFromNotificationList) {
				notificationService.endNotificationPhase(getTask());
			} else {
				if (removeFromNotificationList)
					favouriteListDAO.removeTaskFromList(task, PredefinedFavouriteList.NotificationList);
			}

			if (removeFromWorklist) {
				// only remove from worklist if patient has one active task
				if (task.getPatient().getTasks().stream().filter(p -> !p.isFinalized()).count() > 1) {
					mainHandlerAction.sendGrowlMessagesAsResource("growl.error",
							"growl.error.worklist.remove.moreActive");
				} else {
					worklistViewHandlerAction.removeFromWorklist(task.getPatient());
					worklistViewHandlerAction.onDeselectPatient(true);
				}
			}

			setExitSuccessful(true);
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	public void hideDialog() {
		super.hideDialog(new Boolean(exitSuccessful));
	}
}
