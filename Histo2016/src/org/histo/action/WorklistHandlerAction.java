package org.histo.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.histo.config.enums.DiagnosisStatusState;
import org.histo.config.enums.QuickSearchOptions;
import org.histo.config.enums.Role;
import org.histo.config.enums.StainingListAction;
import org.histo.config.enums.StainingStatus;
import org.histo.config.enums.View;
import org.histo.config.enums.Worklist;
import org.histo.config.enums.WorklistSearchOption;
import org.histo.config.enums.WorklistSortOrder;
import org.histo.dao.PatientDao;
import org.histo.dao.TaskDAO;
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

	private static final long serialVersionUID = 7122206530891485336L;

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private PatientDao patientDao;

	@Autowired
	private UserHandlerAction userHandlerAction;

	@Autowired
	@Lazy
	private SettingsHandlerAction settingsHandlerAction;

	@Autowired
	@Lazy
	private SlideHandlerAction slideHandlerAction;

	@Autowired
	@Lazy
	private DiagnosisHandlerAction diagnosisHandlerAction;

	@Autowired
	@Lazy
	private TaskHandlerAction taskHandlerAction;

	@Autowired
	@Lazy
	private PatientHandlerAction patientHandlerAction;

	@Autowired
	private MainHandlerAction mainHandlerAction;

	/*
	 * ************************** Patient ****************************
	 */

	/**
	 * Currently selected patient
	 */
	private Patient selectedPatient;

	/*
	 * ************************** Patient ****************************
	 */

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

	@PostConstruct
	public void init() {
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

	/**
	 * Action is performed on selecting a patient in the patient list on the
	 * left hand side. If the receiptlog or the diagnosis view should be used it
	 * is checked if a task is selected. If not the program will select the
	 * first (active oder not depending on skipNotActiveTasks) task.
	 * 
	 * @return
	 */
	@SuppressWarnings("incomplete-switch")
	public String onSelectPatient(Patient patient) {
		setSelectedPatient(patient);

		if (patient == null)
			return View.WORKLIST.getPath();

		logger.debug("Select patient " + patient.getPerson().getFullName());
		
		patientDao.initializePatientPdfData(patient);
		
		switch (mainHandlerAction.getCurrentView()) {
		case WORKLIST_PATIENT:
		case WORKLIST_RECEIPTLOG:
		case WORKLIST_DIAGNOSIS:
			Task task = patient.getSelectedTask();
			if (task == null && !patient.getTasks().isEmpty()) {
				task = TaskUtil.getLastTask(patient.getTasks(),
						!getSortOptions().isShowAllTasks() || getSortOptions().isSkipNotActiveTasks());
			}

			if (task != null)
				return onSelectTask(task);
			else
				return View.WORKLIST_PATIENT.getPath();

		}

		return View.WORKLIST.getPath();
	}

	/**
	 * Selects a task and sets the patient of this task as selectedPatient
	 * 
	 * @param task
	 */
	public void onSelectTaskAndPatient(Task task) {

		if (mainHandlerAction.getCurrentView() != View.WORKLIST_RECEIPTLOG
				|| mainHandlerAction.getCurrentView() != View.WORKLIST_DIAGNOSIS) {

			if (userHandlerAction.getCurrentUser().getRole().getRoleValue() >= Role.PHYSICIAN.getRoleValue()){
				// all roles > mta
				logger.debug("User is physician, show diagnoses screen");
				mainHandlerAction.setCurrentView(View.WORKLIST_DIAGNOSIS);
			}else if (userHandlerAction.getCurrentUser().getRole() == Role.MTA){
				// mta
				logger.debug("User is mta, show receiptlog screen");
				mainHandlerAction.setCurrentView(View.WORKLIST_RECEIPTLOG);
			}else{
				// normal users
				logger.debug("User is normal user, show simple list");
				mainHandlerAction.setCurrentView(View.USERLIST);
			}

		}

		System.out.println(task.getPatient());
		task.getPatient().setSelectedTask(task);

		onSelectPatient(task.getPatient());
	}

	/**
	 * Task - Select and init
	 */
	public String onSelectTask(Task task) {
		// set patient.selectedTask is performed by the gui
		// sets this task as active, so it will be show in the navigation column
		// whether there is an action to perform or not
		// task.setActive(true);

		task.getPatient().setSelectedTask(task);

		Role userRole = userHandlerAction.getCurrentUser().getRole();

		task.generateSlideGuiList();

		// Setzte action to none
		slideHandlerAction.setActionOnMany(StainingListAction.NONE);

		// init all available diagnoses
		settingsHandlerAction.updateAllDiagnosisPrototypes();

		// init all available materials
		taskHandlerAction.prepareTask(task);

		return View.WORKLIST.getPath();
	}

	/**
	 * Deselects a task an show the worklist patient view.
	 * 
	 * @param patient
	 * @return
	 */
	public String onDeselectTask(Patient patient) {
		patient.setSelectedTask(null);
		mainHandlerAction.setCurrentView(View.WORKLIST_PATIENT);
		return View.WORKLIST.getPath();
	}

	/**
	 * If the view Worklist is displayed this method will return the subviews.
	 * 
	 * @return
	 */
	public String getCenterView() {
		View currentView = mainHandlerAction.getCurrentView();

		if (getSelectedPatient() == null || currentView == View.WORKLIST_BLANK)
			return View.WORKLIST_BLANK.getPath();
		if (getSelectedPatient().getSelectedTask() == null || currentView == View.WORKLIST_PATIENT)
			return View.WORKLIST_PATIENT.getPath();
		else if (currentView == View.WORKLIST_DIAGNOSIS) {

			return View.WORKLIST_DIAGNOSIS.getPath();
		} else if (currentView == View.WORKLIST_RECEIPTLOG) {
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
		for (Patient patientInWorklis : getWorkList()) {

			if (patientInWorklis.getId() == patient.getId()) {
				if (asSelectedPatient)
					setSelectedPatient(patientInWorklis);
				return;
			}
		}

		getWorkList().add(patient);

		if (asSelectedPatient)
			setSelectedPatient(patient);
	}

	/**
	 * Removes a patient from the worklist.
	 * 
	 * @param patient
	 */
	public void removeFromWorklist(Patient patient) {
		getWorkList().remove(patient);
		if (getSelectedPatient() == patient)
			setSelectedPatient(null);
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
			WorklistSortUtil.orderListByTaskID(patiens, asc);
			break;
		case PIZ:
			WorklistSortUtil.orderListByPIZ(patiens, asc);
			break;
		case NAME:
			WorklistSortUtil.orderListByName(patiens, asc);
			break;
		case PRIORITY:
			WorklistSortUtil.orderListByPriority(patiens, asc);
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
			if (searchOptions.isStaining_new()) {
				result.addAll(patientDao.getPatientWithoutTasks(TimeUtil.setDayBeginning(cal).getTimeInMillis(),
						TimeUtil.setDayEnding(cal).getTimeInMillis()));
			}

			if (searchOptions.isStaining_staining() && searchOptions.isStaining_restaining()) {
				result.addAll(patientDao.getPatientByStainingsBetweenDates(0, System.currentTimeMillis(), false));
			} else {
				List<Patient> paints = patientDao.getPatientByStainingsBetweenDates(0, System.currentTimeMillis(),
						false);
				for (Patient patient : paints) {
					if (searchOptions.isStaining_staining()
							&& patient.getStainingStatus() == StainingStatus.STAINING_NEEDED) {
						result.add(patient);
					} else if (searchOptions.isStaining_restaining()
							&& patient.getStainingStatus() == StainingStatus.RE_STAINING_NEEDED) {
						result.add(patient);
					}
				}
			}

			break;
		case DIAGNOSIS_LIST:
			logger.debug("Diagnosis list selected");
			if (searchOptions.isStaining_diagnosis() && searchOptions.isStaining_rediagnosis()) {
				result.addAll(patientDao.getPatientByDiagnosBetweenDates(0, System.currentTimeMillis(), false));
			} else {
				List<Patient> paints = patientDao.getPatientByDiagnosBetweenDates(0, System.currentTimeMillis(), false);
				for (Patient patient : paints) {
					if (searchOptions.isStaining_diagnosis()
							&& patient.getDiagnosisStatus() == DiagnosisStatusState.DIAGNOSIS_NEEDED) {
						result.add(patient);
					} else if (searchOptions.isStaining_rediagnosis()
							&& patient.getDiagnosisStatus() == DiagnosisStatusState.RE_DIAGNOSIS_NEEDED) {
						result.add(patient);
					}
				}
			}
			break;
		case NOTIFICATION_LIST:
			logger.debug("Notification list selected");
			result.addAll(
					patientDao.getPatientByNotificationBetweenDates(TimeUtil.setDayBeginning(cal).getTimeInMillis(),
							TimeUtil.setDayEnding(cal).getTimeInMillis(), false));
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

		getWorklists().put(getActiveWorklistKey(), result);

		setSelectedPatient(null);
	}

	/**
	 * Selects the next task in List
	 */
	public void selectNextTask() {
		if (getWorkList() != null && !getWorkList().isEmpty()) {
			if (getSelectedPatient() != null) {

				boolean activeOnly = !getSortOptions().isShowAllTasks() || getSortOptions().isSkipNotActiveTasks();

				Task nextTask = TaskUtil.getNextTask(getSelectedPatient().getTasks(),
						getSelectedPatient().getSelectedTask(), activeOnly);
				if (nextTask != null) {
					onSelectTask(nextTask);
					return;
				}

				int indexOfPatient = getWorkList().indexOf(getSelectedPatient());
				if (getWorkList().size() - 1 > indexOfPatient) {
					getSelectedPatient().setSelectedTask(null);
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

			Task nextTask = TaskUtil.getPrevTask(getSelectedPatient().getTasks(),
					getSelectedPatient().getSelectedTask(), activeOnly);

			if (nextTask != null) {
				onSelectTask(nextTask);
				return;
			}

			if (getSelectedPatient() != null) {
				int indexOfPatient = getWorkList().indexOf(getSelectedPatient());
				if (indexOfPatient > 0) {
					getSelectedPatient().setSelectedTask(null);

					Patient prevPatient = getWorkList().get(indexOfPatient - 1);

					Task preFirstTask = TaskUtil.getFirstTask(prevPatient.getTasks(), activeOnly);
					if (preFirstTask != null)
						prevPatient.setSelectedTask(preFirstTask);

					onSelectPatient(getWorkList().get(indexOfPatient - 1));
				}
			} else {
				onSelectPatient(getWorkList().get(getWorkList().size() - 1));
			}
		}
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

	public Patient getSelectedPatient() {
		return selectedPatient;
	}

	public void setSelectedPatient(Patient selectedPatient) {
		this.selectedPatient = selectedPatient;
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

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
