package org.histo.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.histo.action.dialog.SettingsDialogHandler;
import org.histo.action.handler.TaskStatusHandler;
import org.histo.action.view.DiagnosisViewHandlerAction;
import org.histo.action.view.ReceiptlogViewHandlerAction;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.enums.QuickSearchOptions;
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
import org.histo.model.transitory.SearchOptions;
import org.histo.model.transitory.SortOptions;
import org.histo.ui.PatientList;
import org.histo.util.TaskUtil;
import org.histo.util.TimeUtil;
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
	private PatientHandlerAction patientHandlerAction;

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
	 * Search Options
	 */
	private SearchOptions searchOptions;

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

	public void initBean() {
		logger.debug("Init worklist");

		// init worklist
		worklists = new HashMap<String, ArrayList<Patient>>();

		worklists.put(Worklist.DEFAULT.getName(), new ArrayList<Patient>());

		setActiveWorklistKey(Worklist.DEFAULT.getName());

		setSearchOptions(new SearchOptions());

		setSortOptions(new SortOptions());

		setFilterWorklist(false);

		// getting default worklist depending on role
		Role userRole = userHandlerAction.getCurrentUser().getRole();

		switch (userRole) {
		case MTA:
			getSearchOptions().setSearchIndex(WorklistSearchOption.STAINING_LIST);
			createWorklist();
			break;
		case USER:
			break;
		case PHYSICIAN:
		case MODERATOR:
		case ADMIN:
			getSearchOptions().setSearchIndex(WorklistSearchOption.DIAGNOSIS_LIST);
			createWorklist();
		default:
			break;
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

		if (getLastSubView() == null) {
			if (userHandlerAction.getCurrentUser().getRole().getRoleValue() >= Role.PHYSICIAN.getRoleValue()) {
				// all roles > mta
				logger.debug("User is physician, show diagnoses screen");
				setLastSubView(View.WORKLIST_DIAGNOSIS);
				return mainHandlerAction.goToNavigation(View.WORKLIST_DIAGNOSIS);
			} else if (userHandlerAction.getCurrentUser().getRole() == Role.MTA) {
				// mta
				logger.debug("User is mta, show receiptlog screen");
				setLastSubView(View.WORKLIST_RECEIPTLOG);
				return mainHandlerAction.goToNavigation(View.WORKLIST_RECEIPTLOG);
			} else {
				// normal users
				logger.debug("User is normal user, show simple list");
				setLastSubView(View.WORKLIST_TASKS);
				return mainHandlerAction.goToNavigation(View.WORKLIST_TASKS);
			}
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
	public String onDeselectTask(Patient patient) {
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
		View currentView = mainHandlerAction.getCurrentView();

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
				;
			}
			getWorkList().add(patient);
		}

		if (asSelectedPatient)
			commonDataHandlerAction.setSelectedPatient(patient);
	}

	/**
	 * Removes a patient from the worklist.
	 * 
	 * @param patient
	 */
	public void removeFromWorklist(Patient patient) {
		getWorkList().remove(patient);
		if (commonDataHandlerAction.getSelectedPatient() == patient)
			commonDataHandlerAction.setSelectedPatient(null);
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
	 * Searches the database for the given searchOptions and overwrites the
	 * content of the current worklist with the found content.
	 */
	public void createWorklist() {
		createWorklist(getSearchOptions());
	}

	/**
	 * Searches the database for the given searchOptions and overwrites the
	 * content of the current worklist with the found content.
	 */
	public void createWorklist(SearchOptions searchOptions) {

		logger.debug("Searching current worklist");

		ArrayList<Patient> result = new ArrayList<Patient>();

		Calendar cal = Calendar.getInstance();
		Date currentDate = new Date(System.currentTimeMillis());
		cal.setTime(currentDate);

		switch (searchOptions.getSearchIndex()) {
		case STAINING_LIST:
			logger.debug("Staining list selected");

			// getting new stainigs
			if (searchOptions.isStaining_new()) {
				result.addAll(patientDao.getPatientWithoutTasks(TimeUtil.setDayBeginning(cal).getTimeInMillis(),
						TimeUtil.setDayEnding(cal).getTimeInMillis()));
			}

			ArrayList<Long> search = new ArrayList<Long>();

			if (searchOptions.isStaining_staining())
				search.add((long) PredefinedFavouriteList.StainingList.getId());

			if (searchOptions.isStaining_restaining())
				search.add((long) PredefinedFavouriteList.ReStainingList.getId());

			// TODO add check options
			search.add((long) PredefinedFavouriteList.StayInStainingList.getId());

			result.addAll(patientDao.getPatientByTaskList(search));

			break;
		case DIAGNOSIS_LIST:
			logger.debug("Diagnosis list selected");
			// getting diagnoses an re_diagnoses
			if (searchOptions.isStaining_diagnosis() && searchOptions.isStaining_rediagnosis()) {
				result.addAll(patientDao.getPatientByDiagnosis(true));
			} else {
				List<Patient> paints = patientDao.getPatientByDiagnosis(true);
				for (Patient patient : paints) {

					if (searchOptions.isStaining_diagnosis() && patient.isDiagnosisNeeded()) {
						result.add(patient);
					} else if (searchOptions.isStaining_rediagnosis() && patient.isReDiagnosisNeeded()) {
						result.add(patient);
					}
				}
			}
			break;
		case NOTIFICATION_LIST:
			logger.debug("Notification list selected");
			result.addAll(patientDao.getPatientByNotification(true));
			break;
		case TODAY:
			logger.debug("Today selected");
			result.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setDayBeginning(cal).getTimeInMillis(),
					TimeUtil.setDayEnding(cal).getTimeInMillis(), searchOptions.getFilterIndex()));
			break;
		case YESTERDAY:
			logger.debug("Yesterdy selected");
			cal.add(Calendar.DAY_OF_MONTH, -1);
			result.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setDayBeginning(cal).getTimeInMillis(),
					TimeUtil.setDayEnding(cal).getTimeInMillis(), searchOptions.getFilterIndex()));
			break;
		case CURRENTWEEK:
			logger.debug("Current week selected");
			result.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setWeekBeginning(cal).getTimeInMillis(),
					TimeUtil.setWeekEnding(cal).getTimeInMillis(), searchOptions.getFilterIndex()));
			break;
		case LASTWEEK:
			logger.debug("Last week selected");
			cal.add(Calendar.WEEK_OF_YEAR, -1);
			result.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setWeekBeginning(cal).getTimeInMillis(),
					TimeUtil.setWeekEnding(cal).getTimeInMillis(), searchOptions.getFilterIndex()));
			break;
		case LASTMONTH:
			logger.debug("Last month selected");
			cal.add(Calendar.MONDAY, -1);
			result.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setMonthBeginning(cal).getTimeInMillis(),
					TimeUtil.setMonthEnding(cal).getTimeInMillis(), searchOptions.getFilterIndex()));
			break;
		case DAY:
			logger.debug("Day selected");
			cal.setTime(searchOptions.getDay());
			result.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setDayBeginning(cal).getTimeInMillis(),
					TimeUtil.setDayEnding(cal).getTimeInMillis(), searchOptions.getFilterIndex()));
			break;
		case MONTH:
			logger.debug("Month selected");
			cal.set(Calendar.MONTH, searchOptions.getSearchMonth().getNumber());
			cal.set(Calendar.YEAR, searchOptions.getYear());
			result.addAll(patientDao.getWorklistDynamicallyByType(TimeUtil.setMonthBeginning(cal).getTimeInMillis(),
					TimeUtil.setMonthEnding(cal).getTimeInMillis(), searchOptions.getFilterIndex()));
			break;
		case TIME:
			logger.debug("Time selected");
			cal.setTime(searchOptions.getSearchFrom());
			long fromTime = TimeUtil.setDayBeginning(cal).getTimeInMillis();
			cal.setTime(searchOptions.getSearchTo());
			long toTime = TimeUtil.setDayEnding(cal).getTimeInMillis();
			result.addAll(patientDao.getWorklistDynamicallyByType(fromTime, toTime, searchOptions.getFilterIndex()));
			break;
		default:
			break;
		}

		for (Patient patient : result) {
			try {
				patientDao.initilaizeTasksofPatient(patient);
			} catch (CustomDatabaseInconsistentVersionException e) {
				e.printStackTrace();
			}
		}
		getWorklists().put(getActiveWorklistKey(), result);

		commonDataHandlerAction.setSelectedPatient(null);
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
						? TaskUtil.getTaskByHighestPriority(taskStatusHandler.getActivTasks(patientOne)) : null;
				Task highestPriorityTwo = taskStatusHandler.hasActiveTasks(patientTwo)
						? TaskUtil.getTaskByHighestPriority(taskStatusHandler.getActivTasks(patientTwo)) : null;

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
				Task lastTaskOne = taskStatusHandler.hasActiveTasks(patientOne) ? taskStatusHandler.getActivTasks(patientOne).get(0) : null;
				Task lastTaskTwo = taskStatusHandler.hasActiveTasks(patientTwo) ? taskStatusHandler.getActivTasks(patientTwo).get(0) : null;

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

	public void quickSearch() {
		quickSearch(getWorklistFilter(), false);
	}

	public void quickSearch(String searchString, boolean isFilter) {
		logger.debug("Search for " + searchString);

		String[] resultArr = new String[2];
		QuickSearchOptions search = QuickSearchOptions.getQuickSearchOption(searchString, resultArr);

		List<PatientList> result = null;

		if (isFilter) {

		} else {
			logger.debug("Search in database");
			switch (search) {
			case NAME_AND_SURNAME:
				logger.debug("Searching for name (" + resultArr[0] + ") and suranme (" + resultArr[1] + ")");

				// overwrites the toManyPatiensInClinicDatabse flag, so perform
				// method before search

				result = patientHandlerAction.searchForPatientList("", resultArr[0], resultArr[1], null);

				if (result.size() == 1) {
					patientHandlerAction.addNewInternalPatient(result.get(0).getPatient());
					addPatientToWorkList(result.get(0).getPatient(), true);
					logger.debug("Found patient " + result.get(0).getPatient() + " and adding to current worklist");
				} else {
					logger.debug("To many results found in clinic database, open addPatient dialog (" + resultArr[0]
							+ "," + resultArr[1] + ")");
					boolean toMany = false;
					if (patientHandlerAction.isToManyMatchesInClinicDatabase())
						toMany = true;

					patientHandlerAction.initAddPatientDialog();

					patientHandlerAction.setToManyMatchesInClinicDatabase(toMany);
					patientHandlerAction.setActivePatientDialogIndex(0);
					patientHandlerAction.setSearchForPatientName(resultArr[0]);
					patientHandlerAction.setSearchForPatientSurname(resultArr[1]);

					patientHandlerAction.setSearchForPatientList(result);

					patientHandlerAction.showAddPatientDialog();

				}
				break;
			case NAME:
				logger.debug("Searching for name, open addPatient dialog");

				result = patientHandlerAction.searchForPatientList("", resultArr[0], null, null);

				boolean toMany = false;
				if (patientHandlerAction.isToManyMatchesInClinicDatabase())
					toMany = true;

				patientHandlerAction.initAddPatientDialog();

				patientHandlerAction.setToManyMatchesInClinicDatabase(toMany);
				patientHandlerAction.setActivePatientDialogIndex(0);
				patientHandlerAction.setSearchForPatientName(resultArr[0]);
				patientHandlerAction.setSearchForPatientList(result);
				patientHandlerAction.showAddPatientDialog();

				break;
			case PIZ:

				logger.debug("Search for piz: " + searchString);

				result = patientHandlerAction.searchForPatientList(String.valueOf(resultArr[0]), null, null, null);

				if (result.size() == 1) {
					patientHandlerAction.addNewInternalPatient(result.get(0).getPatient());
					addPatientToWorkList(result.get(0).getPatient(), true);
					logger.debug("Found patient " + result.get(0).getPatient() + " and adding to current worklist");
				} else
					logger.debug("Found non patient");

				break;
			case SLIDE_ID:
				logger.debug("Search for SlideID: " + searchString);

				Patient searchResultSlide = patientDao.getPatientBySlidID(resultArr[0]);

				if (searchResultSlide != null) {
					logger.debug("Slide found");
					addPatientToWorkList(searchResultSlide.getPatient(), true);
				} else
					logger.debug("No slide with the given id found");

				break;
			case TASK_ID:
				logger.debug("Search for TaskID: " + searchString);

				Patient searchResultTask = patientDao.getPatientByTaskID(resultArr[0]);

				if (searchResultTask != null) {
					logger.debug("Task found");
					addPatientToWorkList(searchResultTask.getPatient(), true);
				} else
					logger.debug("No task with the given id found");

				break;
			default:
				break;
			}
		}
	}

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

	public SearchOptions getSearchOptions() {
		return searchOptions;
	}

	public void setSearchOptions(SearchOptions searchOptions) {
		this.searchOptions = searchOptions;
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
