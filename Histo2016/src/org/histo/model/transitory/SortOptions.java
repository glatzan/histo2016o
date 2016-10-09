package org.histo.model.transitory;

import org.histo.config.enums.WorklistSortOrder;

public class SortOptions {

	/**
	 * Order of the Worklist, either by id or by patient name
	 */
	private WorklistSortOrder worklistSortOrder = WorklistSortOrder.TASK_ID;

	/**
	 * Order of the Worlist, either if true ascending, or if false descending
	 */
	private boolean worklistSortOrderAcs = true;

	/**
	 * A Filter which searches for a given pattern in the current worklist (
	 */
	private String worklistFilter;

	/**
	 * If true all not active task are skipped while the user useses the up and
	 * down arrows to navigate
	 */
	private boolean skipNotActiveTasks = true;

	/**
	 * If true the  active and completed task will be displayed in the patient list
	 */
	private boolean showAllTasks = true;

	public SortOptions() {
	}

	public WorklistSortOrder getWorklistSortOrder() {
		return worklistSortOrder;
	}

	public boolean isWorklistSortOrderAcs() {
		return worklistSortOrderAcs;
	}

	public String getWorklistFilter() {
		return worklistFilter;
	}

	public boolean isSkipNotActiveTasks() {
		return skipNotActiveTasks;
	}

	public void setWorklistSortOrder(WorklistSortOrder worklistSortOrder) {
		this.worklistSortOrder = worklistSortOrder;
	}

	public void setWorklistSortOrderAcs(boolean worklistSortOrderAcs) {
		this.worklistSortOrderAcs = worklistSortOrderAcs;
	}

	public void setWorklistFilter(String worklistFilter) {
		this.worklistFilter = worklistFilter;
	}

	public void setSkipNotActiveTasks(boolean skipNotActiveTasks) {
		this.skipNotActiveTasks = skipNotActiveTasks;
	}

	public boolean isShowAllTasks() {
		return showAllTasks;
	}

	public void setShowAllTasks(boolean showAllTasks) {
		this.showAllTasks = showAllTasks;
	}

	
	
}
