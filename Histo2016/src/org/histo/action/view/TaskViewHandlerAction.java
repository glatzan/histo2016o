package org.histo.action.view;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.histo.action.WorklistHandlerAction;
import org.histo.dao.TaskDAO;
import org.histo.model.patient.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

public class TaskViewHandlerAction {
	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private TaskDAO taskDAO;

	@Autowired
	@Lazy
	private WorklistHandlerAction worklistHandlerAction;

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

		onChangeSelectionCriteria();
	}

	public void onAddTask(Task task) {

		if (worklistHandlerAction.getWorkList().contains(task.getPatient())) {
			logger.debug("Showning task " + task.getTaskID());
			worklistHandlerAction.onSelectTaskAndPatient(task);
		} else {
			logger.debug("Adding task " + task.getTaskID() + " to worklist");
			task.setActive(true);
			worklistHandlerAction.addPatientToWorkList(task.getPatient(), false);
		}

	}

	public void onChangeSelectionCriteria() {
		setTaskList(taskDAO.getTasks(getTaskPerPull(), getPage() - 1));
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	public List<Task> getTaskList() {
		if (taskList == null)
			initBean();

		return taskList;
	}

	public void setTaskList(List<Task> taskList) {
		this.taskList = taskList;
	}

	public int getTaskPerPull() {
		return taskPerPull;
	}

	public int getPage() {
		return page;
	}

	public void setTaskPerPull(int taskPerPull) {
		this.taskPerPull = taskPerPull;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public Task getSelectedTask() {
		return selectedTask;
	}

	public void setSelectedTask(Task selectedTask) {
		this.selectedTask = selectedTask;
	}

	public List<Integer> getPages() {
		return pages;
	}

	public void setPages(List<Integer> pages) {
		this.pages = pages;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
