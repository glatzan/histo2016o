package org.histo.action;

import java.util.List;

import org.apache.log4j.Logger;
import org.histo.dao.TaskDAO;
import org.histo.model.patient.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class TaskListHandlerAction {

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
	private int[] pages;

	/**
	 * Page of list
	 */
	private int page;

	/**
	 * Inits the taskList;
	 */
	public void initBean() {
		worklistHandlerAction.setSelectedPatient(null);

		if (taskPerPull == 0) {
			setTaskPerPull(20);
			setPage(1);
			
			int maxPages = taskDAO.countTotalTasks();
			double pagesCount = Math.ceil((float)maxPages/ taskPerPull);
			
			pages = new int[(int)pagesCount];
			
			for (int i = 0; i < (int)pagesCount; i++) {
				pages[i] = i+1;
			}
			
		}

		taskList = taskDAO.getTasks(getTaskPerPull(), getPage() - 1);
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

	public void onSelectTask(Task task) {
		logger.debug("Showing task " + task.getTaskID());
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	public List<Task> getTaskList() {
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

	public int[] getPages() {
		return pages;
	}

	public void setPages(int[] pages) {
		this.pages = pages;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
