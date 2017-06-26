package org.histo.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.histo.config.enums.WorklistSortOrder;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.util.TaskUtil;

import lombok.Getter;
import lombok.Setter;

public class Worklist {

	@Getter
	@Setter
	private List<Patient> items;

	/**
	 * Sortorder of worklist
	 */
	@Getter
	@Setter
	private WorklistSortOrder worklistSortOrder;

	/**
	 * True if sort should be ascending, if falseF sort will be descending
	 */
	@Getter
	@Setter
	private boolean sortAscending;

	/**
	 * If true, only tasks which are explicitly marked as active are shown.
	 */
	@Getter
	@Setter
	private boolean showActiveTasksExplicit;

	/**
	 * If true none active tasks will be shown
	 */
	@Getter
	@Setter
	private boolean showNoneActiveTasks;

	/**
	 * True if auto update of worklist shoul take place 
	 */
	@Getter
	@Setter
	private boolean autoUpdate;
	
	/**
	 * Name of the worklist
	 */
	@Getter
	@Setter
	private String name;

	public Worklist(String name, List<Patient> items) {
		this(name, items, true, WorklistSortOrder.TASK_ID, false);
	}

	public Worklist(String name, List<Patient> items, boolean showNoneActiveTasks,
			WorklistSortOrder worklistSortOrder, boolean autoUpdate) {
		this.name = name;
		this.items = items;

		this.showActiveTasksExplicit = false;
		this.showNoneActiveTasks = showNoneActiveTasks;
		this.worklistSortOrder = worklistSortOrder;
		this.autoUpdate = autoUpdate;
	}

	public void removePatient(Patient toRemovePatient) {
		for (Patient patient : items) {
			if (patient.getId() == toRemovePatient.getId()) {
				items.remove(patient);
				return;
			}

		}
	}

	public void addPatient(Patient patient) {
		if (containsPatient(patient))
			replacePatient(patient);
		else
			getItems().add(patient);
	}

	public void replacePatient(Patient patient) {
		for (Patient pListItem : getItems()) {
			if (pListItem.getId() == patient.getId()) {
				int index = getItems().indexOf(pListItem);
				getItems().remove(pListItem);
				getItems().add(index, patient);
				break;
			}
		}

	}

	public boolean containsPatient(Patient patient) {
		return getItems().stream().anyMatch(p -> p.getId() == patient.getId());
	}

	public void sortWordklist() {
		sortWordklist(getWorklistSortOrder(), isSortAscending());
	}

	/**
	 * Sorts a list with patients either by task id or name of the patient
	 * 
	 * @param patiens
	 * @param order
	 */
	public void sortWordklist(WorklistSortOrder order, boolean asc) {
		switch (order) {
		case TASK_ID:
			// Sorting
			Collections.sort(items, new Comparator<Patient>() {
				@Override
				public int compare(Patient patientOne, Patient patientTwo) {
					Task lastTaskOne = patientOne.hasActiveTasks(showActiveTasksExplicit)
							? patientOne.getActiveTasks(showActiveTasksExplicit).get(0) : null;
					Task lastTaskTwo = patientTwo.hasActiveTasks(showActiveTasksExplicit)
							? patientTwo.getActiveTasks(showActiveTasksExplicit).get(0) : null;

					if (lastTaskOne == null && lastTaskTwo == null)
						return 0;
					else if (lastTaskOne == null)
						return asc ? -1 : 1;
					else if (lastTaskTwo == null)
						return asc ? 1 : -1;
					else {
						int res = lastTaskOne.getTaskID().compareTo(lastTaskTwo.getTaskID());
						return asc ? res : res * -1;
					}
				}
			});
			break;
		case PIZ:
			// Sorting
			Collections.sort(items, new Comparator<Patient>() {
				@Override
				public int compare(Patient patientOne, Patient patientTwo) {
					if (patientOne.getPiz() == null && patientTwo.getPiz() == null)
						return 0;
					else if (patientOne.getPiz() == null)
						return asc ? -1 : 1;
					else if (patientTwo.getPiz() == null)
						return asc ? 1 : -1;
					else {
						int res = patientOne.getPiz().compareTo(patientTwo.getPiz());
						return asc ? res : res * -1;
					}
				}
			});
			break;
		case NAME:
			Collections.sort(items, new Comparator<Patient>() {
				@Override
				public int compare(Patient patientOne, Patient patientTwo) {
					if (patientOne.getPerson().getName() == null && patientTwo.getPerson().getName() == null)
						return 0;
					else if (patientOne.getPerson().getName() == null)
						return asc ? -1 : 1;
					else if (patientTwo.getPerson().getName() == null)
						return asc ? 1 : -1;
					else {
						int res = patientOne.getPerson().getName().compareTo(patientTwo.getPerson().getName());
						return asc ? res : res * -1;
					}
				}
			});
			break;
		case PRIORITY:
			Collections.sort(items, new Comparator<Patient>() {
				@Override
				public int compare(Patient patientOne, Patient patientTwo) {
					Task highestPriorityOne = patientOne.hasActiveTasks(showActiveTasksExplicit)
							? TaskUtil.getTaskByHighestPriority(patientOne.getActiveTasks(showActiveTasksExplicit))
							: null;
					Task highestPriorityTwo = patientTwo.hasActiveTasks(showActiveTasksExplicit)
							? TaskUtil.getTaskByHighestPriority(patientTwo.getActiveTasks(showActiveTasksExplicit))
							: null;

					if (highestPriorityOne == null && highestPriorityTwo == null)
						return 0;
					else if (highestPriorityOne == null)
						return asc ? -1 : 1;
					else if (highestPriorityTwo == null)
						return asc ? 1 : -1;
					else {
						int res = highestPriorityOne.getTaskPriority().compareTo(highestPriorityTwo.getTaskPriority());
						return asc ? res : res * -1;
					}
				}
			});
			break;
		}
	}

	public boolean isEmpty(){
		return getItems().isEmpty();
	}
}
