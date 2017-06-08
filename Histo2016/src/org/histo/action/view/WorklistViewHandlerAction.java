package org.histo.action.view;

import java.util.ArrayList;
import java.util.HashMap;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.histo.action.CommonDataHandlerAction;
import org.histo.action.UserHandlerAction;
import org.histo.action.dialog.WorklistSearchDialogHandler;
import org.histo.config.enums.View;
import org.histo.config.enums.Worklist;
import org.histo.config.enums.WorklistSearchOption;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.PatientDao;
import org.histo.dao.TaskDAO;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

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
	private WorklistSearchDialogHandler worklistSearchDialogHandler;

	@Autowired
	@Lazy
	private ReceiptlogViewHandlerAction receiptlogViewHandlerAction;

	@Autowired
	@Lazy
	private DiagnosisViewHandlerAction diagnosisViewHandlerAction;

	/**
	 * View
	 */
	private View currentView;

	/**
	 * The key of the current active worklist.
	 */
	private String activeWorklistKey;

	/**
	 * Hashmap containing all worklists for the current user.
	 */
	private HashMap<String, ArrayList<Patient>> worklists;

	@PostConstruct
	public void initBean() {
		logger.debug("PostConstruct Init worklist");

		// init worklist
		worklists = new HashMap<String, ArrayList<Patient>>();

		worklists.put(Worklist.DEFAULT.getName(), new ArrayList<Patient>());

		setActiveWorklistKey(Worklist.DEFAULT.getName());

		// preparing worklistSearchDialog for creating a worklist
		worklistSearchDialogHandler.initBean();

		setCurrentView(View.WORKLIST_TASKS);
		
		WorklistSearchOption defaultWorklistToLoad = userHandlerAction.getCurrentUser().getDefaultWorklistToLoad();

		if (defaultWorklistToLoad != null) {
			worklistSearchDialogHandler.setSearchIndex(defaultWorklistToLoad);
			getWorklists().put(getActiveWorklistKey(), worklistSearchDialogHandler.createWorklist());
		} else {
			getWorklists().put(getActiveWorklistKey(), new ArrayList<Patient>());
		}

	}

	public void goToNavigation() {
		goToNavigation(getCurrentView());
	}

	public void goToNavigation(View view) {
		switch (view) {
		case WORKLIST_TASKS:
			setCurrentView(view);
			break;
		case WORKLIST_PATIENT:
			if (commonDataHandlerAction.getSelectedPatient() != null)
				setCurrentView(view);
			else
				setCurrentView(View.WORKLIST_TASKS);
			break;
		case WORKLIST_RECEIPTLOG:
		case WORKLIST_DIAGNOSIS:
			if (commonDataHandlerAction.getSelectedPatient() != null
					&& commonDataHandlerAction.getSelectedTask() != null)
				setCurrentView(view);
			else
				setCurrentView(View.WORKLIST_TASKS);
			break;
		default:
			setCurrentView(View.WORKLIST_TASKS);
		}

	}

	public String getCenterView() {
		if (getCurrentView() != null)
			return getCurrentView().getPath();
		else
			return View.WORKLIST_BLANK.getPath();
	}

	public void onSelectPatient(Patient patient) {
		if (patient == null) {
			logger.debug("Deselecting patient");
			commonDataHandlerAction.setSelectedPatient(null);
			goToNavigation(View.WORKLIST_TASKS);
			return;
		}

		try {
			patientDao.initializePatient(patient, true);
		} catch (CustomDatabaseInconsistentVersionException e) {
			// Reloading the Patient, should not be happening
			logger.debug("Version conflict, updating entity");
			patient = patientDao.getPatient(patient.getId(), true);
			replacePatientInCurrentWorklist(patient);
		}

		commonDataHandlerAction.setSelectedPatient(patient);
		commonDataHandlerAction.setSelectedTask(null);

		logger.debug("Select patient " + commonDataHandlerAction.getSelectedPatient().getPerson().getFullName());

		goToNavigation(View.WORKLIST_PATIENT);
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
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			replacePatientInCurrentWorklist(task.getParent());
		}

		replacePatientInCurrentWorklist(task.getPatient());

		commonDataHandlerAction.setSelectedPatient(task.getPatient());
		commonDataHandlerAction.setSelectedTask(task);

		// init all available materials
		receiptlogViewHandlerAction.prepareForTask(task);
		diagnosisViewHandlerAction.prepareForTask(task);

		if (getCurrentView() != View.WORKLIST_RECEIPTLOG || getCurrentView() != View.WORKLIST_DIAGNOSIS) {
			setCurrentView(userHandlerAction.getCurrentUser().getDefaultView());
		}
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
				logger.debug("Version conflict, updating entity");
				patient = patientDao.getPatient(patient.getId(), true);
				replacePatientInCurrentWorklist(patient);
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

	public void replacePatientTaskInCurrentWorklistAndSetSelected() {
		replacePatientTaskInCurrentWorklistAndSetSelected(commonDataHandlerAction.getSelectedTask().getId());
	}

	public void replacePatientTaskInCurrentWorklistAndSetSelected(long taskID) {
		Task task = taskDAO.getTaskAndPatientInitialized(taskID);
		replacePatientTaskInCurrentWorklistAndSetSelected(task);
	}

	public void replacePatientTaskInCurrentWorklistAndSetSelected(Task task) {
		replacePatientInCurrentWorklist(task.getPatient());

		commonDataHandlerAction.setSelectedPatient(task.getPatient());
		commonDataHandlerAction.setSelectedTask(task);
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
		for (Patient pListItem : getWorkList()) {
			if (pListItem.getId() == patient.getId()) {
				int index = getWorkList().indexOf(pListItem);
				getWorkList().remove(pListItem);
				getWorkList().add(index, patient);
				break;
			}
		}

	}

	// ************************ Getter/Setter ************************
	public View getCurrentView() {
		return currentView;
	}

	public void setCurrentView(View currentView) {
		this.currentView = currentView;
	}

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
}
