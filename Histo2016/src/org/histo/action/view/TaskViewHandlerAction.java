package org.histo.action.view;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.histo.config.enums.ContactRole;
import org.histo.dao.ContactDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.AssociatedContact;
import org.histo.model.AssociatedContactNotification;
import org.histo.model.HistoUser;
import org.histo.model.patient.Task;
import org.histo.util.StreamUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Controller
@Scope("session")
@Getter
@Setter
public class TaskViewHandlerAction {
	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskDAO taskDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	@Lazy
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ContactDAO contactDAO;

	/**
	 * Lists of task to display
	 */
	private List<Task> taskList;

	/**
	 * Task to select from list
	 */
	private Task selectedTask;

	/**
	 * Task per Page
	 */
	private int taskPerPull;

	/**
	 * List of available pages
	 */
	private List<Integer> pages;

	/**
	 * Page of list
	 */
	private int page;

	/**
	 * Inits the taskList;
	 */
	public void initBean() {
		logger.debug("Init Task list");

		if (taskPerPull == 0) {
			setTaskPerPull(20);
			setPage(1);
		}

		int maxPages = taskDAO.countTotalTasks();
		int pagesCount = (int) Math.ceil((double) maxPages / taskPerPull);

		logger.debug("Count of pages " + pagesCount);

		setPages(new ArrayList<Integer>(pagesCount));

		for (int i = 0; i < pagesCount; i++) {
			getPages().add(i + 1);
		}
		
		
		taskDAO.getTasksMenuModel();

		onChangeSelectionCriteria();
	}

	public void onAddTask(Task task) {

		if (worklistViewHandlerAction.getWorklist().containsPatient(task.getPatient())) {
			logger.debug("Showning task " + task.getTaskID());
			worklistViewHandlerAction.onSelectTaskAndPatient(task);
		} else {
			logger.debug("Adding task " + task.getTaskID() + " to worklist");
			task.setActive(true);
			worklistViewHandlerAction.addPatientToWorkList(task.getPatient(), false);
		}

	}

	public void onChangeSelectionCriteria() {
		logger.debug("Reloading task lists");
		setTaskList(taskDAO.getTasks(getTaskPerPull(), getPage() - 1));
	}

	public void addUserToNotification(Task task, HistoUser histoUser) {
		AssociatedContact associatedContact = contactDAO.addAssociatedContact(task,
				histoUser.getPhysician().getPerson(), ContactRole.CLINIC_PHYSICIAN);

		contactDAO.addNotificationType(task, associatedContact, AssociatedContactNotification.NotificationTyp.EMAIL);
	}

	public void removeUserFromNotification(Task task, HistoUser histoUser) {
		if (task.getContacts() != null) {
			try {
				AssociatedContact associatedContact = task.getContacts().stream()
						.filter(p -> p.getPerson().equals(histoUser.getPhysician().getPerson()))
						.collect(StreamUtils.singletonCollector());
				
				contactDAO.removeAssociatedContact(task, associatedContact);
			} catch (IllegalStateException e) {
				logger.debug("No matching contact found!");
				// do nothing
			}
		}
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	public List<Task> getTaskList() {
		// TODO move to action not getter
//		if (taskList == null)
//			initBean();

		return taskList;
	}

}
