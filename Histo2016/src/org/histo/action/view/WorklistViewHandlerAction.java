package org.histo.action.view;

import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;
import org.histo.action.DialogHandlerAction;
import org.histo.action.MainHandlerAction;
import org.histo.action.UserHandlerAction;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.View;
import org.histo.config.exception.HistoDatabaseInconsistentVersionException;
import org.histo.dao.TaskDAO;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.service.dao.PatientDao;
import org.histo.service.dao.impl.PatientDaoImpl;
import org.histo.util.StreamUtils;
import org.histo.worklist.Worklist;
import org.histo.worklist.search.WorklistSearch;
import org.histo.worklist.search.WorklistSimpleSearch.SimpleSearchOption;
import org.primefaces.PrimeFaces;
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
	private TaskViewHandlerAction taskViewHandlerAction;

	@Autowired
	@Lazy
	private ReportViewHandlerAction reportViewHandlerAction;

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

	/**
	 * Init method is called via login-handler
	 */
	public void initBean() {
		logger.debug("PostConstruct Init worklist");

		// init worklist
		worklists = new ArrayList<Worklist>();

		// preparing worklistSearchDialog for creating a worklist
		dialogHandlerAction.getWorklistSearchDialog().initBean();

		SimpleSearchOption defaultWorklistToLoad = userHandlerAction.getCurrentUser().getSettings().getWorklistToLoad();

		// if a default to load was provided
		if (defaultWorklistToLoad != null && defaultWorklistToLoad != SimpleSearchOption.EMPTY_LIST) {
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

		// setting start view
		goToNavigation(userHandlerAction.getCurrentUser().getSettings().getStartView());

		// setting default subview
		globalEditViewHandler.setLastDefaultView(userHandlerAction.getCurrentUser().getSettings().getDefaultView());
	}

	public void goToNavigation() {
		goToNavigation(globalEditViewHandler.getCurrentView());
	}

	public void goToNavigation(View view) {

		logger.debug("Navigation goto: " + view);

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
					goToSelectPatient(first);
				else
					// change view to blank
					changeView(View.WORKLIST_PATIENT, View.WORKLIST_NOTHING_SELECTED);
			}
			break;
		case WORKLIST_RECEIPTLOG:
		case WORKLIST_DIAGNOSIS:
		case WORKLIST_REPORT:
			// if task is select change view
			if (globalEditViewHandler.getSelectedPatient() != null && globalEditViewHandler.getSelectedTask() != null) {
				changeView(view);
				onSelectTaskAndPatient(globalEditViewHandler.getSelectedTask());
			} else if (globalEditViewHandler.getSelectedPatient() != null) {
				// no task selected but patient
				// getting active tasks
				List<Task> tasks = globalEditViewHandler.getSelectedPatient()
						.getActiveTasks(getWorklist().isShowActiveTasksExplicit());

				boolean found = false;

				// searching for the first not finalized task
				for (Task task : tasks) {
					if (!task.isFinalized()) {
						changeView(view);
						onSelectTaskAndPatient(task);
						found = true;
						break;
					}
				}

				// if all tasks are finalized selecting the first task
				if (!found) {
					// display first task, if all task should be shown and there is a task
					if (!getWorklist().isShowActiveTasksExplicit()
							&& !globalEditViewHandler.getSelectedPatient().getTasks().isEmpty()) {
						changeView(view);
						onSelectTaskAndPatient(globalEditViewHandler.getSelectedPatient().getTasks().get(0));
					} else {
						changeView(view, View.WORKLIST_NOTHING_SELECTED);
					}
				}

			} else {
				// nothing selected

				Task first = worklist.getFirstActiveTask();

				// select the task
				if (first != null) {
					changeView(view);
					onSelectTaskAndPatient(first);
				} else {
					// change view to blank
					changeView(view, View.WORKLIST_NOTHING_SELECTED);
				}
			}
			break;
		default:
			changeView(View.WORKLIST_BLANK);
		}

	}

	public void changeView(View view) {
		changeView(view, view);
	}

	public void changeView(View currentView, View displayView) {
		logger.debug("Changing view to " + currentView + " display view (" + displayView + ")");

		globalEditViewHandler.setCurrentView(currentView);

		globalEditViewHandler.setDisplayView(displayView);

		if (currentView.isLastSubviewAble()) {
			logger.debug("Setting last default view to " + currentView);
			globalEditViewHandler.setLastDefaultView(currentView);
		}
	}

	public void goToSelectPatient(long patientID) {
		Patient p = patientDao.find(patientID, false);
		goToSelectPatient(p, true);
	}

	public void goToSelectPatient(Patient patient) {
		goToSelectPatient(patient, true);
	}

	public void goToSelectPatient(Patient patient, boolean reload) {
		onSelectPatient(patient, reload);
		changeView(View.WORKLIST_PATIENT);
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
				patientDao.initialize(patient, true);
				globalEditViewHandler.setSelectedPatient(patient);
			} catch (HistoDatabaseInconsistentVersionException e) {
				// Reloading the Patient, should not be happening
				logger.debug("Version conflict, updating entity");
				patientDao.reattach(patient);
				patientDao.initialize(patient, true);
				replacePatientInCurrentWorklist(patient, false);
			}
		}

		globalEditViewHandler.setSelectedTask(null);

		// replacing patient, generating task status
		getWorklist().addPatient(patient);

		logger.debug("Select patient " + globalEditViewHandler.getSelectedPatient().getPerson().getFullName());

		globalEditViewHandler.updateDataOfTask(true, false, false, false);

		logger.info("end -> " + (System.currentTimeMillis() - test));
	}

	public void onDeselectPatient() {
		onDeselectPatient(true);
	}

	public void onDeselectPatient(boolean updateView) {
		globalEditViewHandler.setSelectedPatient(null);
		globalEditViewHandler.setSelectedTask(null);

		if (updateView)
			goToNavigation();
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
			} catch (HistoDatabaseInconsistentVersionException e) {
				// Reloading the Task, should not be happening
				logger.debug("Version conflict, updating entity");

				// getting new task, possibility of deletion
				task = taskDAO.getTaskAndPatientInitialized(task.getId());

				if (task != null)
					replacePatientInCurrentWorklist(task.getParent(), false);
				else {
					// task might be delete from an other user
					if (globalEditViewHandler.getSelectedPatient() != null) {
						replacePatientInCurrentWorklist(globalEditViewHandler.getSelectedPatient());

						mainHandlerAction.sendGrowlMessagesAsResource("growl.error", "growl.error.version");

						PrimeFaces.current()
								.executeScript("clickButtonFromBean('#globalCommandsForm\\\\:refreshContentBtn')");
					}
				}
			}
		}

		globalEditViewHandler.setSelectedPatient(task.getPatient());
		globalEditViewHandler.setSelectedTask(task);

		receiptlogViewHandlerAction.prepareForTask(task);
		diagnosisViewHandlerAction.prepareForTask(task);

		if (globalEditViewHandler.getCurrentView() == View.WORKLIST_REPORT
				|| (!globalEditViewHandler.getCurrentView().isLastSubviewAble()
						&& globalEditViewHandler.getLastDefaultView() == View.WORKLIST_REPORT))
			reportViewHandlerAction.prepareForTask(task);

		// replacing patient, generating task status
		getWorklist().addPatient(task.getPatient());
		getWorklist().sortWordklist();

		// generating task data, taskstatus is generated previously
		globalEditViewHandler.updateDataOfTask(true, false, false, true);

		// task.setActive(true);

		// change if is subview (diagnosis, receipt log or report view)
		if (!globalEditViewHandler.getCurrentView().isLastSubviewAble()) {
			logger.debug("Setting subview " + globalEditViewHandler.getLastDefaultView());
			changeView(globalEditViewHandler.getLastDefaultView());
		}

		logger.info("Request processed in -> " + (System.currentTimeMillis() - test));
	}

	/**
	 * Deselects a task an show the worklist patient view.
	 * 
	 * @param patient
	 * @return
	 */
	public void onDeselectTask() {
		globalEditViewHandler.setSelectedTask(null);
		goToNavigation(View.WORKLIST_PATIENT);
	}

	public void addWorklist(WorklistSearch worklistSearch, String name, boolean selected) {
		addWorklist(new Worklist(name, worklistSearch,
				userHandlerAction.getCurrentUser().getSettings().isWorklistHideNoneActiveTasks(),
				userHandlerAction.getCurrentUser().getSettings().getWorklistSortOrder(),
				userHandlerAction.getCurrentUser().getSettings().isWorklistAutoUpdate()), selected);
	}

	public void addWorklist(Worklist worklist, boolean selected) {
		addWorklist(worklist, selected, false);
	}

	public void addWorklist(Worklist worklist, boolean selected, boolean changeView) {
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
			onDeselectPatient(false);

			worklist.sortWordklist();

			worklist.updateWorklist();
		}

		if (changeView)
			goToNavigation();
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

		// selecting task if patient is in worklist, or if usersettings force it
		if (getWorklist().containsPatient(task.getPatient())
				|| userHandlerAction.getCurrentUser().getSettings().isAddTaskWithSingelClick()) {
			logger.debug("Showning task " + task.getTaskID());
			// reloading task and patient from database

			// only selecting task if patient is already selected
			if (globalEditViewHandler.getSelectedPatient() != null
					&& globalEditViewHandler.getSelectedPatient().getId() == task.getPatient().getId()
					|| userHandlerAction.getCurrentUser().getSettings().isAddTaskWithSingelClick())
				onSelectTaskAndPatient(task.getId());
			else
				addPatientToWorkList(task.getPatient(), true, false);
		} else {
			logger.debug("Adding task " + task.getTaskID() + " to worklist");
			addPatientToWorkList(task.getPatient(), true, false);
			task.setActive(true);
		}
	}

	/**
	 * Adds a patient to the worklist. If already added it is check if the patient
	 * should be selected. If so the patient will be selected. The patient isn't
	 * added twice.
	 * 
	 * @param patient
	 * @param asSelectedPatient
	 */
	public void addPatientToWorkList(Patient patient, boolean asSelectedPatient, boolean changeToPatientView) {

		// checks if patient is already in database
		if (!getWorklist().containsPatient(patient)) {
			try {
				patientDao.initialize(patient, true, false);
			} catch (HistoDatabaseInconsistentVersionException e) {
				logger.debug("Version conflict, updating entity");
				patient = patientDao.find(patient.getId(), true, false);
			}

			getWorklist().addPatient(patient);

			getWorklist().sortWordklist();
		}

		if (changeToPatientView)
			goToSelectPatient(patient, true);
		else
			onSelectPatient(patient, true);

		getWorklist().generateTaskStatus(patient);
	}

	/**
	 * Removes a patient from the worklist.
	 * 
	 * @param patient
	 */
	public void removePatientFromCurrentWorklist(Patient patient) {

		getWorklist().removePatient(patient);

		if (globalEditViewHandler.getSelectedPatient() != null
				&& globalEditViewHandler.getSelectedPatient().equals(patient)) {
			onDeselectPatient(true);
		}
	}

	public void replaceSelectedTask() {
		if (globalEditViewHandler.getSelectedTask() != null)
			replaceTaskInCurrentWorklist(globalEditViewHandler.getSelectedTask(), true);
	}

	public void replaceTaskInCurrentWorklist(Task task) {
		replaceTaskInCurrentWorklist(task, true);
	}

	public void replaceTaskInCurrentWorklist(Task task, boolean reload) {
		if (reload)
			task = taskDAO.getTaskAndPatientInitialized(task.getId());

		// onVersionConflictPatient(task.getParent(), false);
		onSelectTaskAndPatient(task, false);
	}

	public void replacePatientInCurrentWorklist(Patient patient) {
		replacePatientInCurrentWorklist(patient, true);
	}

	public void replacePatientInCurrentWorklist(Patient patient, boolean reload) {
		if (reload)
			patient = patientDao.find(patient.getId(), true);

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
						goToSelectPatient(newPatient);
					}
				}
			} else {
				Patient newPatient = getWorklist().getItems().get(getWorklist().getItems().size() - 1);

				if (newPatient.hasActiveTasks(getWorklist().isShowActiveTasksExplicit())) {
					onSelectTaskAndPatient(newPatient.getActiveTasks(getWorklist().isShowActiveTasksExplicit())
							.get(newPatient.getActiveTasks(getWorklist().isShowActiveTasksExplicit()).size() - 1));
				} else {
					goToSelectPatient(newPatient);
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
						goToSelectPatient(newPatient);
					}
				}
			} else {
				Patient newPatient = getWorklist().getItems().get(0);

				if (newPatient.hasActiveTasks(getWorklist().isShowActiveTasksExplicit())) {
					onSelectTaskAndPatient(newPatient.getActiveTasks(getWorklist().isShowActiveTasksExplicit()).get(0));
				} else {
					goToSelectPatient(newPatient);
				}
			}
		}
	}

	public void setWorklist(Worklist worklist) {
		worklist.sortWordklist();
		this.worklist = worklist;
	}

}
