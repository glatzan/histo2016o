package org.histo.action.dialog.notification;

import org.histo.action.UserHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.handler.TaskHandlerAction;
import org.histo.action.view.GlobalEditViewHandler;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.patient.Task;
import org.histo.model.user.HistoPermissions;
import org.histo.service.NotificationService;
import org.histo.service.SampleService;
import org.histo.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

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
	 * Can be set to true if the task should stay in diagnosis phase.
	 */
	private boolean stayInNotificationPhase;

	/**
	 * If true the task will be shifted into diagnosis phase
	 */
	private boolean archiveTask;

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

			if (userHandlerAction.currentUserHasPermission(HistoPermissions.TASK_EDIT_ARCHIVE))
				setArchiveTask(true);

		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.onVersionConflictTask(task, false);
		}

		super.initBean(task, Dialog.NOTIFICATION_PHASE_EXIT);
	}

	/**
	 * Removes task from notification list and ends the notification phae. if set
	 * the task will be archived.
	 */
	public void exitPhase() {
		try {
			notificationService.endNotificationPhase(getTask());

			if (stayInNotificationPhase && !archiveTask)
				favouriteListDAO.addTaskToList(task, PredefinedFavouriteList.StayInNotificationList);

			if (archiveTask)
				taskService.archiveTask(getTask());

		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}
}
