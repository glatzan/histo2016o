package org.histo.action.view;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;
import org.histo.action.DialogHandlerAction;
import org.histo.action.MainHandlerAction;
import org.histo.action.UserHandlerAction;
import org.histo.action.handler.TaskStatusHandler;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.View;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.PatientDao;
import org.histo.dao.TaskDAO;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.util.StreamUtils;
import org.histo.worklist.Worklist;
import org.histo.worklist.search.WorklistSearch;
import org.histo.worklist.search.WorklistSimpleSearch.SimpleSearchOption;
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
	@Lazy
	private GlobalEditViewHandler globalEditViewHandler;

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
	private TaskStatusHandler taskStatusHandler;

	@Autowired
	@Lazy
	private TaskViewHandlerAction taskViewHandlerAction;

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

		SimpleSearchOption defaultWorklistToLoad = userHandlerAction.getCurrentUser().getSettings()
				.getWorklistToLoad();

		if (defaultWorklistToLoad != null) {
			dialogHandlerAction.getWorklistSearchDialog().getSimpleSearchTab().getWorklistSearch()
					.setSearchIndex(defaultWorklistToLoad);
			dialogHandlerAction.getWorklistSearchDialog().getSimpleSearchTab().getWorklistSearch().updateSearchIndex();

			addWorklist(new Worklist("Default",
					dialogHandlerAction.getWorklistSearchDialog().getSimpleSearchTab().getWorklistSearch(),
					userHandlerAction.getCurrentUser().getSettings().isWorklistHideNoneActiveTasks(),
					userHandlerAction.getCurrentUser().getSettings().getWorklistSortOrder(),
					userHandlerAction.getCurrentUser().getSettings().isWorklistAutoUpdate()), true);
		} else {
			addWorklist(new Worklist("Default", new WorklistSearch()), true);
		}

		goToNavigation(View.WORKLIST_TASKS);
		globalEditViewHandler.setLastDefaultView(userHandlerAction.getCurrentUser().getSettings().getDefaultView());
	}

	public void goToNavigation(View view) {
		switch (view) {
		case WORKLIST_TASKS:
			taskViewHandlerAction.initBean();
			changeView(View.WORKLIST_TASKS);
			break;
		case WORKLIST_PATIENT:
			// show patient if selected
			if (globalEditViewHandler.getSelectedPatient() != null)
				changeView(View.WORKLIST_PATIENT);
			else {
				// get first patient in worklist, show him
				Patient first = worklist.getFirstPatient();
				if (first != null)
					onSelectPatient(first);
				else
					// change view to blank
					changeView(View.WORKLIST_BLANK);
			}
			break;
		case WORKLIST_RECEIPTLOG:
		case WORKLIST_DIAGNOSIS:
			// if task is select change view
			if (globalEditViewHandler.getSelectedPatient() != null && globalEditViewHandler.getSelectedTask() != null)
				changeView(view);
			else {

				globalEditViewHandler.setLastDefaultView(view);

				Task first = worklist.getFirstActiveTask();

				// select the task
				if (first != null) {
					onSelectTaskAndPatient(first);
				} else {
					// change view to blank
					changeView(View.WORKLIST_BLANK);
				}
			}
			break;
		default:
			changeView(View.WORKLIST_BLANK);
		}

	}

	public void changeView(View view) {
		globalEditViewHandler.setCurrentView(view);

		if (view != View.WORKLIST_BLANK)
			globalEditViewHandler.setSelectedView(view);

		if (view == View.WORKLIST_DIAGNOSIS || view == View.WORKLIST_RECEIPTLOG)
			globalEditViewHandler.setLastDefaultView(view);
	}

	public void onSelectPatient(Patient patient) {
		onSelectPatient(patient, true);
	}

	public void onSelectPatient(Patient patient, boolean reload) {
		long test = System.currentTimeMillis();
		logger.info("start - > 0");

		if (patient == null) {
			logger.debug("Deselecting patient");
			globalEditViewHandler.setSelectedPatient(null);
			changeView(View.WORKLIST_BLANK);
			return;
		}

		if (reload) {
			try {
				patientDao.initializePatient(patient, true);
				globalEditViewHandler.setSelectedPatient(patient);
			} catch (CustomDatabaseInconsistentVersionException e) {
				// Reloading the Patient, should not be happening
				logger.debug("Version conflict, updating entity");
				patientDao.refresh(patient);
				patientDao.initializePatient(patient, true);
				onVersionConflictPatient(patient, false);
			}
		}

		globalEditViewHandler.setSelectedTask(null);

		// replacing patient, generating task status
		getWorklist().addPatient(patient);

		logger.debug("Select patient " + globalEditViewHandler.getSelectedPatient().getPerson().getFullName());

		globalEditViewHandler.updateMenuModel(false);

		changeView(View.WORKLIST_PATIENT);
		logger.info("end -> " + (System.currentTimeMillis() - test));
	}

	public void onDeselectPatient() {
		globalEditViewHandler.setSelectedPatient(null);
		globalEditViewHandler.setSelectedTask(null);
		changeView(View.WORKLIST_BLANK);
	}

	public void onSelectTaskAndPatient(Task task) {
		onSelectTaskAndPatient(task, true);
	}

	public void onSelectTaskAndPatient(long taskID) {
		Task task = taskDAO.getTaskAndPatientInitialized(taskID);
		onSelectTaskAndPatient(task, false);
	}

	/**
	 * Selects a task and sets the patient of this task as selectedPatient
	 * 
	 * @param task
	 */
	public void onSelectTaskAndPatient(Task task, boolean reload) {
		long test = System.currentTimeMillis();
		logger.info("start - > 0");
		if (task == null) {
			logger.debug("Deselecting task");
			changeView(View.WORKLIST_BLANK);
			return;
		}

		logger.debug("Selecting task " + task.getPatient().getPerson().getFullName() + " " + task.getTaskID());

		if (reload) {
			try {
				taskDAO.initializeTaskAndPatient(task);
			} catch (CustomDatabaseInconsistentVersionException e) {
				// Reloading the Task, should not be happening
				logger.debug("Version conflict, updating entity");

				// getting new task, possibility of deletion
				task = taskDAO.getTaskAndPatientInitialized(task.getId());

				if (task != null)
					onVersionConflictPatient(task.getParent(), false);
				else {
					// task might be delete from an other user
					if (globalEditViewHandler.getSelectedPatient() != null) {
						onVersionConflictPatient(globalEditViewHandler.getSelectedPatient());

						mainHandlerAction.addQueueGrowlMessage(resourceBundle.get("growl.version.error"),
								resourceBundle.get("growl.version.error.text"));

						RequestContext.getCurrentInstance()
								.execute("clickButtonFromBean('#headerForm\\\\:updateAllContent')");
					}
				}
			}
		}

		globalEditViewHandler.setSelectedPatient(task.getPatient());
		globalEditViewHandler.setSelectedTask(task);

		// init all available materials
		receiptlogViewHandlerAction.prepareForTask(task);
		diagnosisViewHandlerAction.prepareForTask(task);

		// replacing patient, generating task status
		getWorklist().addPatient(task.getPatient());
		getWorklist().sortWordklist();

		// generating menu
		globalEditViewHandler.updateMenuModel(false);

		task.setActive(true);

		if (globalEditViewHandler.getCurrentView() != View.WORKLIST_RECEIPTLOG
				&& globalEditViewHandler.getCurrentView() != View.WORKLIST_DIAGNOSIS) {
			changeView(globalEditViewHandler.getLastDefaultView());
		}

		logger.info("end -> " + (System.currentTimeMillis() - test));
	}

	/**
	 * Deselects a task an show the worklist patient view.
	 * 
	 * @param patient
	 * @return
	 */
	public void onDeselectTask() {
		globalEditViewHandler.setSelectedTask(null);
		changeView(View.WORKLIST_PATIENT);
	}

	public void addWorklist(WorklistSearch worklistSearch, String name, boolean selected) {
		addWorklist(new Worklist(name, worklistSearch,
				userHandlerAction.getCurrentUser().getSettings().isWorklistHideNoneActiveTasks(),
				userHandlerAction.getCurrentUser().getSettings().getWorklistSortOrder(),
				userHandlerAction.getCurrentUser().getSettings().isWorklistAutoUpdate()), selected);
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

	public void clearWorklist(Worklist worklist) {
		worklist.clear();
		if (getWorklist() == worklist)
			onDeselectPatient();
	}

	public void addTaskToWorklist(Task task) {

		if (getWorklist().containsPatient(task.getPatient())) {
			logger.debug("Showning task " + task.getTaskID());
			// reloading task and patient from database
			onSelectTaskAndPatient(task.getId());
		} else {
			logger.debug("Adding task " + task.getTaskID() + " to worklist");
			addPatientToWorkList(task.getPatient(), false);
			task.setActive(true);
		}
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
			}

			getWorklist().addPatient(patient);

			getWorklist().sortWordklist();
		}

		if (asSelectedPatient)
			onSelectPatient(patient);

		getWorklist().generateTaskStatus(patient);
	}

	/**
	 * Removes a patient from the worklist.
	 * 
	 * @param patient
	 */
	public void removeFromWorklist(Patient patient) {
		if (globalEditViewHandler.getSelectedPatient() == patient) {
			onDeselectPatient();
		}

		getWorklist().removePatient(patient);
	}

	public void onVersionConflictTask() {
		if (globalEditViewHandler.getSelectedTask() != null)
			onVersionConflictTask(globalEditViewHandler.getSelectedTask(), true);
	}

	public void onVersionConflictTask(Task task) {
		onVersionConflictTask(task, true);
	}

	public void onVersionConflictTask(Task task, boolean reload) {
		if (reload)
			task = taskDAO.getTaskAndPatientInitialized(task.getId());

		// onVersionConflictPatient(task.getParent(), false);
		onSelectTaskAndPatient(task, false);
	}

	public void onVersionConflictPatient(Patient patient) {
		onVersionConflictPatient(patient, true);
	}

	public void onVersionConflictPatient(Patient patient, boolean reload) {
		if (reload)
			patient = patientDao.getPatient(patient.getId(), true);

		if (globalEditViewHandler.getSelectedPatient() != null
				&& globalEditViewHandler.getSelectedPatient().getId() == patient.getId())
			globalEditViewHandler.setSelectedPatient(patient);

		logger.debug("Replacing patient due to external changes!");
		getWorklist().addPatient(patient);

	}

	public void updateCurrentWorklist() {
		if (getWorklist().isAutoUpdate()) {
			logger.debug("Auto updating worklist");
			getWorklist().updateWorklist(globalEditViewHandler.getSelectedPatient());
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
			if (globalEditViewHandler.getSelectedPatient() != null) {

				int indexOfTask = globalEditViewHandler.getSelectedPatient()
						.getActiveTasks(getWorklist().isShowActiveTasksExplicit())
						.indexOf(globalEditViewHandler.getSelectedTask());

				// next task is within the same patient
				if (indexOfTask - 1 >= 0) {
					onSelectTaskAndPatient(globalEditViewHandler.getSelectedPatient()
							.getActiveTasks(getWorklist().isShowActiveTasksExplicit()).get(indexOfTask - 1));
					return;
				}

				int indexOfPatient = getWorklist().getItems().indexOf(globalEditViewHandler.getSelectedPatient());

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
			if (globalEditViewHandler.getSelectedPatient() != null) {

				int indexOfTask = globalEditViewHandler.getSelectedPatient()
						.getActiveTasks(getWorklist().isShowActiveTasksExplicit())
						.indexOf(globalEditViewHandler.getSelectedTask());

				// next task is within the same patient
				if (indexOfTask + 1 < globalEditViewHandler.getSelectedPatient()
						.getActiveTasks(getWorklist().isShowActiveTasksExplicit()).size()) {
					onSelectTaskAndPatient(globalEditViewHandler.getSelectedPatient()
							.getActiveTasks(getWorklist().isShowActiveTasksExplicit()).get(indexOfTask + 1));
					return;
				}

				int indexOfPatient = getWorklist().getItems().indexOf(globalEditViewHandler.getSelectedPatient());

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
