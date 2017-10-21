package org.histo.action.view;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;
import org.histo.action.CommonDataHandlerAction;
import org.histo.action.DialogHandlerAction;
import org.histo.action.MainHandlerAction;
import org.histo.action.UserHandlerAction;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.View;
import org.histo.config.enums.WorklistSearchOption;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.PatientDao;
import org.histo.dao.TaskDAO;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.util.StreamUtils;
import org.histo.worklist.Worklist;
import org.histo.worklist.search.WorklistSearch;
import org.primefaces.context.RequestContext;
import org.primefaces.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import lombok.Getter;
import lombok.Setter;

@Controller
@Scope("session")
public class WorklistViewHandlerAction {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private CommonDataHandlerAction commonDataHandlerAction;

	@Autowired
	@Lazy
	private PatientDao patientDao;

	@Autowired
	private TaskDAO taskDAO;

	@Autowired
	private UserHandlerAction userHandlerAction;

	@Autowired
	private DialogHandlerAction dialogHandlerAction;

	@Autowired
	@Lazy
	private ReceiptlogViewHandlerAction receiptlogViewHandlerAction;

	@Autowired
	@Lazy
	private DiagnosisViewHandlerAction diagnosisViewHandlerAction;

	@Autowired
	@Lazy
	private MainHandlerAction mainHandlerAction;

	@Autowired
	@Lazy
	private ResourceBundle resourceBundle;

	@Autowired
	@Lazy
	private TaskViewHandlerAction taskViewHandlerAction;
	/**
	 * View
	 */
	@Getter
	@Setter
	private View currentView;

	/**
	 * Saves the last task view diagnosis view or receiptlog view
	 */
	@Getter
	@Setter
	private View lastTaskView;

	/**
	 * Containing all worklists
	 */
	@Getter
	@Setter
	private List<Worklist> worklists;

	/**
	 * Current worklist
	 */
	@Getter
	private Worklist worklist;

	@PostConstruct
	public void initBean() {
		logger.debug("PostConstruct Init worklist");

		// init worklist
		worklists = new ArrayList<Worklist>();

		// preparing worklistSearchDialog for creating a worklist
		dialogHandlerAction.getWorklistSearchDialog().initBean();

		setCurrentView(View.WORKLIST_TASKS);

		WorklistSearchOption defaultWorklistToLoad = userHandlerAction.getCurrentUser().getWorklistToLoad();

		if (defaultWorklistToLoad != null) {
			dialogHandlerAction.getWorklistSearchDialog().getWorklistSearchBasic()
					.setSearchIndex(defaultWorklistToLoad);
			dialogHandlerAction.getWorklistSearchDialog().getWorklistSearchBasic().updateSearchIndex();

			addWorklist(new Worklist("Default", dialogHandlerAction.getWorklistSearchDialog().getWorklistSearchBasic(),
					userHandlerAction.getCurrentUser().isWorklistHideNoneActiveTasks(),
					userHandlerAction.getCurrentUser().getWorklistSortOrder(),
					userHandlerAction.getCurrentUser().isWorklistAutoUpdate()), true);
		} else {
			addWorklist(new Worklist("Default", new WorklistSearch()), true);
		}

		setLastTaskView(userHandlerAction.getCurrentUser().getDefaultView());

	}

	public void goToNavigation() {
		goToNavigation(getCurrentView());
	}

	public void goToNavigation(View view) {
		switch (view) {
		case WORKLIST_TASKS:
			setCurrentView(view);
			taskViewHandlerAction.initBean();
			break;
		case WORKLIST_PATIENT:
			if (commonDataHandlerAction.getSelectedPatient() != null)
				setCurrentView(view);
			else
				goToNavigation(View.WORKLIST_TASKS);
			break;
		case WORKLIST_RECEIPTLOG:
		case WORKLIST_DIAGNOSIS:
			setLastTaskView(view);
			if (commonDataHandlerAction.getSelectedPatient() != null
					&& commonDataHandlerAction.getSelectedTask() != null)
				setCurrentView(view);
			else
				goToNavigation(View.WORKLIST_TASKS);
			break;
		default:
			goToNavigation(View.WORKLIST_TASKS);
		}

	}

	public String getCenterView() {
		if (getCurrentView() != null)
			return getCurrentView().getPath();
		else
			return View.WORKLIST_BLANK.getPath();
	}

	public void onSelectPatient(Patient patient) {
		long test = System.currentTimeMillis();
		logger.info("start - > 0");
		if (patient == null) {
			logger.debug("Deselecting patient");
			commonDataHandlerAction.setSelectedPatient(null);
			goToNavigation(View.WORKLIST_TASKS);
			return;
		}

		try {
			patientDao.initializePatient(patient, true);
			commonDataHandlerAction.setSelectedPatient(patient);
		} catch (CustomDatabaseInconsistentVersionException e) {
			// Reloading the Patient, should not be happening
			logger.debug("Version conflict, updating entity");
			patientDao.refresh(patient);
			patientDao.initializePatient(patient, true);
			replacePatientInCurrentWorklist(patient);
		}

		commonDataHandlerAction.setSelectedTask(null);

		logger.debug("Select patient " + commonDataHandlerAction.getSelectedPatient().getPerson().getFullName());

		goToNavigation(View.WORKLIST_PATIENT);
		logger.info("end -> " + (System.currentTimeMillis()-test));
	}

	public void onDeselectPatient() {
		commonDataHandlerAction.setSelectedPatient(null);
		commonDataHandlerAction.setSelectedTask(null);
		goToNavigation(View.WORKLIST_TASKS);
	}

	/**
	 * Selects a task and sets the patient of this task as selectedPatient
	 * 
	 * @param task
	 */
	public void onSelectTaskAndPatient(Task task) {
		long test = System.currentTimeMillis();
		logger.info("start - > 0");
		if (task == null) {
			logger.debug("Deselecting task");
			goToNavigation(View.WORKLIST_TASKS);
			return;
		}

		logger.debug("Selecting task " + task.getPatient().getPerson().getFullName() + " " + task.getTaskID());

		try {
			taskDAO.initializeTaskAndPatient(task);
		} catch (CustomDatabaseInconsistentVersionException e) {
			// Reloading the Task, should not be happening
			logger.debug("Version conflict, updating entity");

			// getting new task, possibility of deletion
			task = taskDAO.getTaskAndPatientInitialized(task.getId());

			if (task != null)
				replacePatientInCurrentWorklist(task.getParent());
			else {
				// task might be delete from an other user
				if (commonDataHandlerAction.getSelectedPatient() != null) {
					replacePatientInCurrentWorklist(commonDataHandlerAction.getSelectedPatient().getId());

					mainHandlerAction.addQueueGrowlMessage(resourceBundle.get("growl.version.error"),
							resourceBundle.get("growl.version.error.text"));

					RequestContext.getCurrentInstance()
							.execute("clickButtonFromBean('#headerForm\\\\:updateAllContent')");
				}
			}
		}

		commonDataHandlerAction.setSelectedPatient(task.getPatient());
		commonDataHandlerAction.setSelectedTask(task);

		// init all available materials
		receiptlogViewHandlerAction.prepareForTask(task);
		diagnosisViewHandlerAction.prepareForTask(task);

		if (getCurrentView() != View.WORKLIST_RECEIPTLOG && getCurrentView() != View.WORKLIST_DIAGNOSIS) {
			setCurrentView(getLastTaskView());
		}
		
		logger.info("end -> " + (System.currentTimeMillis()-test));
	}

	/**
	 * Deselects a task an show the worklist patient view.
	 * 
	 * @param patient
	 * @return
	 */
	public void onDeselectTask() {
		commonDataHandlerAction.setSelectedTask(null);
		setCurrentView(View.WORKLIST_PATIENT);
	}

	public void addWorklist(WorklistSearch worklistSearch, String name, boolean selected) {
		addWorklist(
				new Worklist(name, worklistSearch, userHandlerAction.getCurrentUser().isWorklistHideNoneActiveTasks(),
						userHandlerAction.getCurrentUser().getWorklistSortOrder(),
						userHandlerAction.getCurrentUser().isWorklistAutoUpdate()),
				selected);
	}

	public void addWorklist(Worklist worklist, boolean selected) {
		// removing worklist if worklist with the same name is present
		try {
			Worklist cWorklist = getWorklists().stream().filter(p -> p.getName().equals(worklist.getName()))
					.collect(StreamUtils.singletonCollector());

			removeWorklist(cWorklist);
		} catch (IllegalStateException e) {
			// do nothing
		}

		getWorklists().add(worklist);

		if (selected) {
			setWorklist(worklist);
			// deselecting patient
			onDeselectPatient();

			worklist.updateWorklist();
		}
	}

	public void removeWorklist(Worklist worklist) {
		getWorklists().remove(worklist);
		if (getWorklist() == worklist)
			setWorklist(new Worklist("", new WorklistSearch()));
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
		if (!getWorklist().containsPatient(patient)) {
			try {
				patientDao.initilaizeTasksofPatient(patient);
			} catch (CustomDatabaseInconsistentVersionException e) {
				logger.debug("Version conflict, updating entity");
				patient = patientDao.getPatient(patient.getId(), true);
				replacePatientInCurrentWorklist(patient);
			}
			getWorklist().addPatient(patient);

			getWorklist().sortWordklist();
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
		if (commonDataHandlerAction.getSelectedPatient() == patient) {
			onDeselectPatient();
		}

		getWorklist().removePatient(patient);
	}

	public void replacePatientTaskInCurrentWorklistAndSetSelected() {
		replacePatientTaskInCurrentWorklistAndSetSelected(commonDataHandlerAction.getSelectedTask().getId());
	}

	public void replacePatientTaskInCurrentWorklistAndSetSelected(long taskID) {
		Task task = taskDAO.getTaskAndPatientInitialized(taskID);
		replacePatientTaskInCurrentWorklistAndSetSelected(task);
	}

	public void replacePatientTaskInCurrentWorklistAndSetSelected(Task task) {
		replacePatientInCurrentWorklist(task.getPatient());

		logger.debug("Setting as active task and patient");

		onSelectTaskAndPatient(task);
	}

	public void replacePatientInCurrentWorklist(long id) {
		Patient patient = patientDao.getPatient(id, true);
		replacePatientInCurrentWorklist(patient);
	}

	public void replacePatientInCurrentWorklist(Patient patient) {
		if (commonDataHandlerAction.getSelectedPatient() != null
				&& commonDataHandlerAction.getSelectedPatient().getId() == patient.getId())
			commonDataHandlerAction.setSelectedPatient(patient);

		logger.debug("Replacing patient due to external changes!");
		getWorklist().replacePatient(patient);

	}

	public void updateCurrentWorklist() {
		if (getWorklist().isAutoUpdate()) {
			logger.debug("Auto updating worklist");
			getWorklist().updateWorklist(commonDataHandlerAction.getSelectedPatient());
		}
	}

	// TODO move
	public static boolean isDialogContext() {
		return FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap()
				.containsKey(Constants.DIALOG_FRAMEWORK.CONVERSATION_PARAM);
	}

	/**
	 * Selects the next task in List
	 */
	public void selectNextTask() {
		if (!getWorklist().isEmpty()) {
			if (commonDataHandlerAction.getSelectedPatient() != null) {

				int indexOfTask = commonDataHandlerAction.getSelectedPatient()
						.getActiveTasks(getWorklist().isShowActiveTasksExplicit())
						.indexOf(commonDataHandlerAction.getSelectedTask());

				// next task is within the same patient
				if (indexOfTask - 1 >= 0) {
					onSelectTaskAndPatient(commonDataHandlerAction.getSelectedPatient()
							.getActiveTasks(getWorklist().isShowActiveTasksExplicit()).get(indexOfTask - 1));
					return;
				}

				int indexOfPatient = getWorklist().getItems().indexOf(commonDataHandlerAction.getSelectedPatient());

				if (indexOfPatient == -1)
					return;

				if (indexOfPatient - 1 >= 0) {
					Patient newPatient = getWorklist().getItems().get(indexOfPatient - 1);

					if (newPatient.hasActiveTasks(getWorklist().isShowActiveTasksExplicit())) {
						onSelectTaskAndPatient(newPatient.getActiveTasks(getWorklist().isShowActiveTasksExplicit())
								.get(newPatient.getActiveTasks(getWorklist().isShowActiveTasksExplicit()).size() - 1));
					} else {
						onSelectPatient(newPatient);
					}
				}
			} else {
				Patient newPatient = getWorklist().getItems().get(getWorklist().getItems().size() - 1);

				if (newPatient.hasActiveTasks(getWorklist().isShowActiveTasksExplicit())) {
					onSelectTaskAndPatient(newPatient.getActiveTasks(getWorklist().isShowActiveTasksExplicit())
							.get(newPatient.getActiveTasks(getWorklist().isShowActiveTasksExplicit()).size() - 1));
				} else {
					onSelectPatient(newPatient);
				}
			}
		}
	}

	public void selectPreviouseTask() {
		if (!getWorklist().isEmpty()) {
			if (commonDataHandlerAction.getSelectedPatient() != null) {

				int indexOfTask = commonDataHandlerAction.getSelectedPatient()
						.getActiveTasks(getWorklist().isShowActiveTasksExplicit())
						.indexOf(commonDataHandlerAction.getSelectedTask());

				// next task is within the same patient
				if (indexOfTask + 1 < commonDataHandlerAction.getSelectedPatient()
						.getActiveTasks(getWorklist().isShowActiveTasksExplicit()).size()) {
					onSelectTaskAndPatient(commonDataHandlerAction.getSelectedPatient()
							.getActiveTasks(getWorklist().isShowActiveTasksExplicit()).get(indexOfTask + 1));
					return;
				}

				int indexOfPatient = getWorklist().getItems().indexOf(commonDataHandlerAction.getSelectedPatient());

				if (indexOfPatient == -1)
					return;

				if (indexOfPatient + 1 < getWorklist().getItems().size()) {
					Patient newPatient = getWorklist().getItems().get(indexOfPatient + 1);

					if (newPatient.hasActiveTasks(getWorklist().isShowActiveTasksExplicit())) {
						onSelectTaskAndPatient(
								newPatient.getActiveTasks(getWorklist().isShowActiveTasksExplicit()).get(0));
					} else {
						onSelectPatient(newPatient);
					}
				}
			} else {
				Patient newPatient = getWorklist().getItems().get(0);

				if (newPatient.hasActiveTasks(getWorklist().isShowActiveTasksExplicit())) {
					onSelectTaskAndPatient(newPatient.getActiveTasks(getWorklist().isShowActiveTasksExplicit()).get(0));
				} else {
					onSelectPatient(newPatient);
				}
			}
		}
	}

	public void setWorklist(Worklist worklist) {
		worklist.sortWordklist();
		this.worklist = worklist;
	}

}
