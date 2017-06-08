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
	private CommonDataHandlerAction commonDataHandlerAction;

	@Autowired
	@Lazy
	private ReceiptlogViewHandlerAction receiptlogViewHandlerAction;

	@Autowired
	private TaskStatusHandler taskStatusHandler;

	@Autowired
	private WorklistSearchDialogHandler worklistSearchDialogHandler;

	/*
	 * ************************** Worklist ****************************
	 */



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

		setSortOptions(new SortOptions());

		setFilterWorklist(false);

	}





	




	/**
	 * If the view Worklist is displayed this method will return the subviews.
	 * 
	 * @return
	 */







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


	/*
	 * ************************** Worklist ****************************
	 */

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

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
