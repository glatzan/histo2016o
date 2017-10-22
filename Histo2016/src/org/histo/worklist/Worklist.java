package org.histo.worklist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.histo.config.enums.WorklistSortOrder;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.PatientDao;
import org.histo.dao.PatientMenuDAO;
import org.histo.model.immutable.patientmenu.PatientMenuModel;
import org.histo.model.immutable.patientmenu.TaskMenuModel;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.util.TaskUtil;
import org.histo.worklist.search.WorklistSearch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.Getter;
import lombok.Setter;

@Configurable
public class Worklist {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private PatientMenuDAO patientMenuDAO;

	@Getter
	@Setter
	private List<PatientMenuModel> items;

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

	@Setter
	@Getter
	private WorklistSearch worklistSearch;

	/**
	 * Update interval if enabled in sec
	 */
	@Setter
	@Getter
	private int udpateInterval = 10;

	public Worklist(String name, WorklistSearch worklistSearch) {
		this(name, worklistSearch, true, WorklistSortOrder.TASK_ID, false);
	}

	public Worklist(String name, WorklistSearch worklistSearch, boolean showNoneActiveTasks,
			WorklistSortOrder worklistSortOrder, boolean autoUpdate) {
		this.name = name;
		this.worklistSearch = worklistSearch;

		this.showActiveTasksExplicit = false;
		this.showNoneActiveTasks = showNoneActiveTasks;
		this.worklistSortOrder = worklistSortOrder;
		this.autoUpdate = autoUpdate;

		this.items = new ArrayList<PatientMenuModel>();
	}

	public void removePatient(PatientMenuModel toRemovePatient) {
		for (PatientMenuModel patient : items) {
			if (patient.equals(toRemovePatient)) {
				items.remove(patient);
				return;
			}
		}
	}

	public void addPatient(PatientMenuModel patient) {
		if (containsPatient(patient.getId()))
			replacePatient(patient);
		else
			getItems().add(patient);
	}

	public boolean replacePatient(PatientMenuModel patient) {
		for (PatientMenuModel pListItem : getItems()) {
			if (pListItem.equals(patient)) {
				int index = getItems().indexOf(pListItem);
				updateTaksActiveStatus(pListItem, patient);

				getItems().remove(pListItem);
				getItems().add(index, patient);
				return true;
			}
		}
		return false;
	}

	public void updateTaksActiveStatus(PatientMenuModel old, PatientMenuModel newPat) {
		for (TaskMenuModel newTask : newPat.getTasks()) {
			for (TaskMenuModel oldTask : old.getTasks()) {
				if (newTask.equals(oldTask)) {
					newTask.setActive(oldTask.isActive());
				}
				break;
			}
		}
	}

	public boolean containsPatient(long id) {
		return getItems().stream().anyMatch(p -> p.getId() == id);
	}

	public int indexOf(long id) {
		for (int i = 0; i < getItems().size(); i++) {
			if (getItems().get(i).getId() == id)
				return i;
		}
		return -1;
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
			Collections.sort(items, new Comparator<PatientMenuModel>() {
				@Override
				public int compare(PatientMenuModel patientOne, PatientMenuModel patientTwo) {
					TaskMenuModel lastTaskOne = patientOne.hasActiveTasks(showActiveTasksExplicit)
							? patientOne.getActiveTasks(showActiveTasksExplicit).get(0)
							: null;
					TaskMenuModel lastTaskTwo = patientTwo.hasActiveTasks(showActiveTasksExplicit)
							? patientTwo.getActiveTasks(showActiveTasksExplicit).get(0)
							: null;

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
			Collections.sort(items, new Comparator<PatientMenuModel>() {
				@Override
				public int compare(PatientMenuModel patientOne, PatientMenuModel patientTwo) {
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
			Collections.sort(items, new Comparator<PatientMenuModel>() {
				@Override
				public int compare(PatientMenuModel patientOne, PatientMenuModel patientTwo) {
					if (patientOne.getLastName() == null && patientTwo.getLastName() == null)
						return 0;
					else if (patientOne.getLastName() == null)
						return asc ? -1 : 1;
					else if (patientTwo.getLastName() == null)
						return asc ? 1 : -1;
					else {
						int res = patientOne.getLastName().compareTo(patientTwo.getLastName());
						return asc ? res : res * -1;
					}
				}
			});
			break;
		case PRIORITY:
			Collections.sort(items, new Comparator<PatientMenuModel>() {
				@Override
				public int compare(PatientMenuModel patientOne, PatientMenuModel patientTwo) {
					TaskMenuModel highestPriorityOne = patientOne.hasActiveTasks(showActiveTasksExplicit)
							? TaskUtil.getTaskMenuModelByHighestPriority(
									patientOne.getActiveTasks(showActiveTasksExplicit))
							: null;
					TaskMenuModel highestPriorityTwo = patientTwo.hasActiveTasks(showActiveTasksExplicit)
							? TaskUtil.getTaskMenuModelByHighestPriority(
									patientTwo.getActiveTasks(showActiveTasksExplicit))
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

	public boolean isEmpty() {
		return getItems().isEmpty();
	}

	public void updateWorklist() {
		updateWorklist(new PatientMenuModel());

	}

	public void updateWorklist(PatientMenuModel activePatient) {

		try {
			// executing worklistsearch
			List<PatientMenuModel> update = getWorklistSearch().getWorklist();

			for (PatientMenuModel patient : update) {
				// Skipping if patient is active patient
				if (!patient.equals(activePatient)) {
					logger.trace("Updatin or adding: " + patient.toString());
					// patientDao.initilaizeTasksofPatient(patient);
					addPatient(patient);
				} else
					logger.trace("Skippting " + activePatient.toString() + " (is selected patient)");
			}

			// fining patients which were not updated
			List<Long> manuallyUdatePizes = new ArrayList<Long>();

			loop: for (PatientMenuModel inList : getItems()) {
				for (PatientMenuModel patient : update) {
					if (inList.equals(patient))
						continue loop;
				}

				// Skipping if patient is active patient
				if (!inList.equals(activePatient))
					manuallyUdatePizes.add(inList.getId());
			}

			if (!manuallyUdatePizes.isEmpty()) {
				// updating patients in worklist which were not found by generic
				// search
				List<PatientMenuModel> histoMatchList = patientMenuDAO
						.getPatientByTaskInFavouriteList(manuallyUdatePizes);

				for (PatientMenuModel patient : histoMatchList) {
					logger.trace("Upadtin Patient not in search query: " + patient.toString());
					addPatient(patient);
				}
			}

		} catch (CustomDatabaseInconsistentVersionException e) {
			// TODO handle
			e.printStackTrace();
		}
	}

}
