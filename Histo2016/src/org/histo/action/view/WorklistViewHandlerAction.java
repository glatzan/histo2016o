package org.histo.action.view;

import org.apache.log4j.Logger;
import org.histo.action.CommonDataHandlerAction;
import org.histo.action.UserHandlerAction;
import org.histo.config.enums.View;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.PatientDao;
import org.histo.dao.TaskDAO;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

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
	/**
	 * Subview is saved
	 */
	private View currentView;

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
			logger.debug("!! Version inconsistent with Database updating");
			patient = patientDao.getPatient(patient.getId(), true);
			updatePatientInCurrentWorklist(patient);
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

		if (getCurrentView() != View.WORKLIST_RECEIPTLOG || getCurrentView() != View.WORKLIST_DIAGNOSIS)) {
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

	// ************************ Getter/Setter ************************
	public View getCurrentView() {
		return currentView;
	}

	public void setCurrentView(View currentView) {
		this.currentView = currentView;
	}
}
