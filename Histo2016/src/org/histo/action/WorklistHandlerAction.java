package org.histo.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.histo.action.dialog.SettingsDialogHandler;
import org.histo.action.dialog.WorklistSearchDialogHandler;
import org.histo.action.handler.TaskStatusHandler;
import org.histo.action.view.DiagnosisViewHandlerAction;
import org.histo.action.view.ReceiptlogViewHandlerAction;
import org.histo.config.enums.Role;
import org.histo.config.enums.View;
import org.histo.config.enums.Worklist;
import org.histo.config.enums.WorklistSearchOption;
import org.histo.config.enums.WorklistSortOrder;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.PatientDao;
import org.histo.dao.TaskDAO;
import org.histo.dao.UtilDAO;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.model.transitory.SortOptions;
import org.histo.util.TaskUtil;
import org.histo.util.WorklistSortUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class WorklistHandlerAction implements Serializable {

	protected static final long serialVersionUID = 7122206530891485336L;

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	@Lazy
	private PatientDao patientDao;

	@Autowired
	@Lazy
	private UserHandlerAction userHandlerAction;

	@Autowired
	@Lazy
	private SettingsDialogHandler settingsDialogHandler;

	@Autowired
	@Lazy
	private MainHandlerAction mainHandlerAction;

	@Autowired
	@Lazy
	private DiagnosisViewHandlerAction diagnosisViewHandlerAction;

	@Autowired
	@Lazy
	private UtilDAO utilDAO;

	@Autowired
	@Lazy
	private TaskDAO taskDAO;

	@Autowired
	@Lazy
	private CommonDataHandlerAction commonDataHandlerAction;

	@Autowired
	@Lazy
	private ReceiptlogViewHandlerAction receiptlogViewHandlerAction;

	@Autowired
	private TaskStatusHandler taskStatusHandler;

	@Autowired
	private WorklistSearchDialogHandler worklistSearchDialogHandler;

	/********************************************************
	 * Navigation
	 ********************************************************/
	/**
	 * Subview is saved
	 */
	private View lastSubView;

	/********************************************************
	 * Navigation
	 ********************************************************/
	/*
	 * ************************** Worklist ****************************
	 */

	/**
	 * The key of the current active worklist.
	 */
	private String activeWorklistKey;

	/**
	 * Hashmap containing all worklists for the current user.
	 */
	private HashMap<String, ArrayList<Patient>> worklists;

	/**
	 * Options for sorting the worklist
	 */
	private SortOptions sortOptions;

	/**
	 * A Filter which searches for a given pattern in the current worklist (
	 */
	private String worklistFilter;

	/**
	 * If true and a value is insert into the worklist search field, the
	 * worklist will be filtered using the value in worklistFilter. Otherwise
	 * new data will be loaded.
	 */
	private boolean filterWorklist;
	/*
	 * ************************** Worklist ****************************
	 */

	@PostConstruct
	public void initBean() {
		logger.debug("PostConstruct Init worklist");

		// init worklist
		worklists = new HashMap<String, ArrayList<Patient>>();

		worklists.put(Worklist.DEFAULT.getName(), new ArrayList<Patient>());

		setActiveWorklistKey(Worklist.DEFAULT.getName());

		setSortOptions(new SortOptions());

		setFilterWorklist(false);

		// preparing worklistSearchDialog for creating a worklist
		worklistSearchDialogHandler.initBean();

		WorklistSearchOption defaultWorklistToLoad = userHandlerAction.getCurrentUser().getDefaultWorklistToLoad();

		if (defaultWorklistToLoad != null) {
			worklistSearchDialogHandler.setSearchIndex(defaultWorklistToLoad);
			getWorklists().put(getActiveWorklistKey(), worklistSearchDialogHandler.createWorklist());
		} else {
			getWorklists().put(getActiveWorklistKey(), new ArrayList<Patient>());
		}

	}

	public void replaceInvaliedPatientInCurrentWorklist(Patient patient) {
		commonDataHandlerAction.setSelectedPatient(patient);
		logger.debug("Replacing patient due to external changes!");
		for (Patient pListItem : getWorkList()) {
			if (pListItem.getId() == patient.getId()) {
				int index = getWorkList().indexOf(pListItem);
				getWorkList().remove(pListItem);
				getWorkList().add(index, patient);

				// // setting the selected task
				// if (pListItem.getSelectedTask() != null) {
				// Task newSelectedTask = null;
				//
				// for (Task task : patient.getTasks()) {
				// if (task.getId() == pListItem.getSelectedTask().getId()) {
				// newSelectedTask = task;
				// break;
				// }
				// }
				// patient.setSelectedTask(newSelectedTask);
				// }

				// setting active tasks
				// for (Task activeTask : pListItem.getActivTasks()) {
				// for (Task task : patient.getTasks()) {
				// if (task.getId() == activeTask.getId()) {
				// task.setActive(true);
				// break;
				// }
				// }
				// }
				break;
			}
		}

	}

	/**
	 * Action is performed on selecting a patient in the patient list on the
	 * left hand side. If the receiptlog or the diagnosis view should be used it
	 * is checked if a task is selected. If not the program will select the
	 * first (active oder not depending on skipNotActiveTasks) task.
	 * 
	 * @return
	 */
	public String onSelectPatient(Patient patient) {
		if (patient == null) {
			logger.debug("Deselecting patient");
			commonDataHandlerAction.setSelectedPatient(null);
			return mainHandlerAction.goToNavigation(View.WORKLIST);
		}

		try {
			patientDao.initializePatient(patient, true);
		} catch (CustomDatabaseInconsistentVersionException e) {
			// Reloading the Patient, should not be happening
			logger.debug("!! Version inconsistent with Database updating");
			patient = patientDao.getPatient(patient.getId(), true);
			updatePatientInCurrentWorklist(patient);
		}

		commonDataHandlerAction.setSelectedPatient(patient);
		commonDataHandlerAction.setSelectedTask(null);

		logger.debug("Select patient " + commonDataHandlerAction.getSelectedPatient().getPerson().getFullName());

		return mainHandlerAction.goToNavigation(View.WORKLIST_PATIENT);
	}

	public String onDeselectPatient() {
		commonDataHandlerAction.setSelectedPatient(null);
		commonDataHandlerAction.setSelectedTask(null);
		return mainHandlerAction.goToNavigation(View.WORKLIST_TASKS);
	}

	/**
	 * Selects a task and sets the patient of this task as selectedPatient
	 * 
	 * @param task
	 */
	public String onSelectTaskAndPatient(Task task) {
		if (task == null) {
			logger.debug("Deselecting task");
			return mainHandlerAction.goToNavigation(View.WORKLIST);
		}

		logger.debug("Selecting task " + task.getPatient().getPerson().getFullName() + " " + task.getTaskID());

		try {
			taskDAO.initializeTaskAndPatient(task);
		} catch (CustomDatabaseInconsistentVersionException e) {
			// Reloading the Task, should not be happening
			logger.debug("!! Version inconsistent with Database updating");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			updatePatientInCurrentWorklist(task.getParent());
		}

		updatePatientInCurrentWorklist(task.getPatient());

		commonDataHandlerAction.setSelectedPatient(task.getPatient());
		commonDataHandlerAction.setSelectedTask(task);

		// init all available materials
		receiptlogViewHandlerAction.prepareForTask(task);
		diagnosisViewHandlerAction.prepareForTask(task);

		if (getLastSubView() == null && userHandlerAction.getCurrentUser().getDefaultView() != null) {
			setLastSubView(userHandlerAction.getCurrentUser().getDefaultView());
			return userHandlerAction.getCurrentUser().getDefaultView().getPath();
		} else {
			return mainHandlerAction.goToNavigation(getLastSubView());
		}
	}

	/**
	 * Deselects a task an show the worklist patient view.
	 * 
	 * @param patient
	 * @return
	 */
	public String onDeselectTask() {
		commonDataHandlerAction.setSelectedTask(null);
		return mainHandlerAction.goToNavigation(View.WORKLIST_PATIENT);
	}

	public void updateSelectedTaskAndPatientInCurrentWorklistOnVersionConflict() {
		updateSelectedTaskAndPatientInCurrentWorklistOnVersionConflict(
				commonDataHandlerAction.getSelectedTask().getId());
	}

	public void updateSelectedTaskAndPatientInCurrentWorklistOnVersionConflict(long taskID) {
		Task task = taskDAO.getTaskAndPatientInitialized(taskID);
		updatePatientInCurrentWorklist(task.getPatient());

		commonDataHandlerAction.setSelectedPatient(task.getPatient());
		commonDataHandlerAction.setSelectedTask(task);
	}

	public void updatePatientInCurrentWorklist(long id) {
		Patient patient = patientDao.getPatient(id, true);
		updatePatientInCurrentWorklist(patient);
	}

	public void updatePatientInCurrentWorklist(Patient patient) {
		logger.debug("Replacing patient due to external changes!");
		for (Patient pListItem : getWorkList()) {
			if (pListItem.getId() == patient.getId()) {
				int index = getWorkList().indexOf(pListItem);
				getWorkList().remove(pListItem);
				getWorkList().add(index, patient);

				// for (Task activeTask : pListItem.getActivTasks()) {
				// for (Task task : patient.getTasks()) {
				// if (task.getId() == activeTask.getId()) {
				// task.setActive(true);
				// break;
				// }
				// }
				// }
				break;
			}
		}
	}

	/**
	 * If the view Worklist is displayed this method will return the subviews.
	 * 
	 * @return
	 */
	public String getCenterView() {
		View currentView = commonDataHandlerAction.getCurrentView();

		if (currentView == View.WORKLIST_BLANK)
			return View.WORKLIST_BLANK.getPath();

		if (commonDataHandlerAction.getSelectedPatient() == null || currentView == View.WORKLIST_TASKS) {
			return View.WORKLIST_TASKS.getPath();
		}

		if (commonDataHandlerAction.getSelectedTask() == null || currentView == View.WORKLIST_PATIENT) {
			return View.WORKLIST_PATIENT.getPath();
		} else if (currentView == View.WORKLIST_DIAGNOSIS) {
			setLastSubView(View.WORKLIST_DIAGNOSIS);
			return View.WORKLIST_DIAGNOSIS.getPath();
		} else if (currentView == View.WORKLIST_RECEIPTLOG) {
			setLastSubView(View.WORKLIST_RECEIPTLOG);
			return View.WORKLIST_RECEIPTLOG.getPath();
		} else
			return View.WORKLIST_BLANK.getPath();
	}

	/**
	 * Adds a patient to the worklist. If already added it is check if the
	 * patient should be selected. If so the patient will be selected. The
	 * patient isn't added twice.
	 * 
	 * @param patient
	 * @param asSelectedPatient
	 */
	public void addPatientToWorkList(Patient patient, boolean asSelectedPatient) {

		// checks if patient is already in database
		if (!getWorkList().contains(patient)) {
			try {
				patientDao.initilaizeTasksofPatient(patient);
			} catch (CustomDatabaseInconsistentVersionException e) {
				logger.debug("!! Version inconsistent with Database updating");
				patient = patientDao.getPatient(patient.getId(), true);
				updatePatientInCurrentWorklist(patient);
			}
			getWorkList().add(patient);
		}

		if (asSelectedPatient)
			onSelectPatient(patient);
	}

	/**
	 * Removes a patient from the worklist.
	 * 
	 * @param patient
	 */
	public void removeFromWorklist(Patient patient) {
		getWorkList().remove(patient);
		if (commonDataHandlerAction.getSelectedPatient() == patient) {
			onDeselectPatient();
		}
	}

	/**
	 * Sorts a list with patients either by task id or name of the patient
	 * 
	 * @param patiens
	 * @param order
	 */
	public void sortWordklist(List<Patient> patiens, WorklistSortOrder order, boolean asc) {
		switch (order) {
		case TASK_ID:
			orderListByTaskID(patiens, asc);
			break;
		case PIZ:
			WorklistSortUtil.orderListByPIZ(patiens, asc);
			break;
		case NAME:
			WorklistSortUtil.orderListByName(patiens, asc);
			break;
		case PRIORITY:
			orderListByPriority(patiens, asc);
			break;
		}
	}

	/**
	 * Selects the next task in List
	 */
	public void selectNextTask() {
		if (getWorkList() != null && !getWorkList().isEmpty()) {
			if (commonDataHandlerAction.getSelectedPatient() != null) {

				boolean activeOnly = !getSortOptions().isShowAllTasks() || getSortOptions().isSkipNotActiveTasks();

				Task nextTask = getNextTask(commonDataHandlerAction.getSelectedPatient().getTasks(),
						commonDataHandlerAction.getSelectedTask(), activeOnly);
				if (nextTask != null) {
					onSelectTaskAndPatient(nextTask);
					return;
				}

				int indexOfPatient = getWorkList().indexOf(commonDataHandlerAction.getSelectedPatient());
				if (getWorkList().size() - 1 > indexOfPatient) {
					commonDataHandlerAction.setSelectedTask(null);
					onSelectPatient(getWorkList().get(indexOfPatient + 1));
				}
			} else {
				onSelectPatient(getWorkList().get(0));
			}
		}
	}

	public void selectPreviouseTask() {
		if (getWorkList() != null && !getWorkList().isEmpty()) {

			boolean activeOnly = !getSortOptions().isShowAllTasks() || getSortOptions().isSkipNotActiveTasks();

			Task nextTask = getPrevTask(commonDataHandlerAction.getSelectedPatient().getTasks(),
					commonDataHandlerAction.getSelectedTask(), activeOnly);

			if (nextTask != null) {
				onSelectTaskAndPatient(nextTask);
				return;
			}

			if (commonDataHandlerAction.getSelectedPatient() != null) {
				int indexOfPatient = getWorkList().indexOf(commonDataHandlerAction.getSelectedPatient());
				if (indexOfPatient > 0) {
					commonDataHandlerAction.setSelectedTask(null);

					Patient prevPatient = getWorkList().get(indexOfPatient - 1);

					Task preFirstTask = getFirstTask(prevPatient.getTasks(), activeOnly);
					if (preFirstTask != null)
						commonDataHandlerAction.setSelectedTask(preFirstTask);

					onSelectPatient(getWorkList().get(indexOfPatient - 1));
				}
			} else {
				onSelectPatient(getWorkList().get(getWorkList().size() - 1));
			}
		}
	}

	/**
	 * Returns the task with the highest taskID. (Is always the first task
	 * because of the descending order)
	 * 
	 * @param tasks
	 * @return
	 */
	public Task getLastTask(List<Task> tasks, boolean active) {
		if (tasks == null || tasks.isEmpty())
			return null;

		// List is ordere desc by taskID per default so return first (and
		// latest) task in List
		if (tasks != null && !tasks.isEmpty()) {
			if (active == false)
				return tasks.get(0);

			for (Task task : tasks) {
				if (taskStatusHandler.isActiveOrActionPending(task))
					return task;
			}
		}
		return null;
	}

	public Task getFirstTask(List<Task> tasks, boolean active) {
		if (tasks == null || tasks.isEmpty())
			return null;

		// List is ordere desc by taskID per default so return first (and
		// latest) task in List
		if (tasks != null && !tasks.isEmpty()) {

			if (active == false)
				return tasks.get(tasks.size() - 1);

			for (int i = tasks.size() - 1; i >= 0; i--) {
				if (taskStatusHandler.isActiveOrActionPending(tasks.get(i)))
					return tasks.get(i);
				else
					continue;
			}
		}
		return null;
	}

	public Task getPrevTask(List<Task> tasks, Task currentTask, boolean activeOnle) {

		int index = tasks.indexOf(currentTask);
		if (index == -1 || index == 0)
			return null;

		for (int i = index - 1; i >= 0; i--) {
			if (activeOnle) {
				if (taskStatusHandler.isActiveOrActionPending(tasks.get(i)))
					return tasks.get(i);
			} else
				return tasks.get(i);
		}
		return null;
	}

	public Task getNextTask(List<Task> tasks, Task currentTask, boolean activeOnle) {

		int index = tasks.indexOf(currentTask);
		if (index == -1 || index == tasks.size() - 1)
			return null;

		for (int i = index + 1; i < tasks.size(); i++) {
			if (activeOnle) {
				if (taskStatusHandler.isActiveOrActionPending(tasks.get(i)))
					return tasks.get(i);
			} else
				return tasks.get(i);
		}
		return null;
	}

	public List<Patient> orderListByPriority(List<Patient> patiens, boolean asc) {

		// Sorting
		Collections.sort(patiens, new Comparator<Patient>() {
			@Override
			public int compare(Patient patientOne, Patient patientTwo) {
				Task highestPriorityOne = taskStatusHandler.hasActiveTasks(patientOne)
						? TaskUtil.getTaskByHighestPriority(taskStatusHandler.getActiveTasks(patientOne)) : null;
				Task highestPriorityTwo = taskStatusHandler.hasActiveTasks(patientTwo)
						? TaskUtil.getTaskByHighestPriority(taskStatusHandler.getActiveTasks(patientTwo)) : null;

				if (highestPriorityOne == null && highestPriorityTwo == null)
					return 0;
				else if (highestPriorityOne == null)
					return asc ? -1 : 1;
				else if (highestPriorityTwo == null)
					return asc ? 1 : -1;
				else {
					int res = highestPriorityOne.getTaskPriority().compareTo(highestPriorityTwo.getTaskPriority());
					return asc ? res : res * -1;
				}
			}
		});

		return patiens;
	}

	/**
	 * Sorts a List of patients by the task id. The tasknumber will be ascending
	 * or descending depending on the asc parameter.
	 * 
	 * @param patiens
	 * @return
	 */
	public List<Patient> orderListByTaskID(List<Patient> patiens, boolean asc) {

		// Sorting
		Collections.sort(patiens, new Comparator<Patient>() {
			@Override
			public int compare(Patient patientOne, Patient patientTwo) {
				Task lastTaskOne = taskStatusHandler.hasActiveTasks(patientOne)
						? taskStatusHandler.getActiveTasks(patientOne).get(0) : null;
				Task lastTaskTwo = taskStatusHandler.hasActiveTasks(patientTwo)
						? taskStatusHandler.getActiveTasks(patientTwo).get(0) : null;

				if (lastTaskOne == null && lastTaskTwo == null)
					return 0;
				else if (lastTaskOne == null)
					return asc ? -1 : 1;
				else if (lastTaskTwo == null)
					return asc ? 1 : -1;
				else {
					int res = lastTaskOne.getTaskID().compareTo(lastTaskTwo.getTaskID());
					return asc ? res : res * -1;
				}
			}
		});

		return patiens;
	}
	/*
	 * ************************** Worklist ****************************
	 */

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	public String getActiveWorklistKey() {
		return activeWorklistKey;
	}

	public void setActiveWorklistKey(String activeWorklistKey) {
		this.activeWorklistKey = activeWorklistKey;
	}

	public HashMap<String, ArrayList<Patient>> getWorklists() {
		return worklists;
	}

	public void setWorklists(HashMap<String, ArrayList<Patient>> worklists) {
		this.worklists = worklists;
	}

	public ArrayList<Patient> getWorkList() {
		return worklists.get(getActiveWorklistKey());
	}

	public SortOptions getSortOptions() {
		return sortOptions;
	}

	public void setSortOptions(SortOptions sortOptions) {
		this.sortOptions = sortOptions;
	}

	public String getWorklistFilter() {
		return worklistFilter;
	}

	public void setWorklistFilter(String worklistFilter) {
		this.worklistFilter = worklistFilter;
	}

	public boolean isFilterWorklist() {
		return filterWorklist;
	}

	public void setFilterWorklist(boolean filterWorklist) {
		this.filterWorklist = filterWorklist;
	}

	public View getLastSubView() {
		return lastSubView;
	}

	public void setLastSubView(View lastSubView) {
		this.lastSubView = lastSubView;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
