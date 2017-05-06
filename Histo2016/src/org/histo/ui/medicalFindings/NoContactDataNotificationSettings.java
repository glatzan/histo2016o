package org.histo.ui.medicalFindings;

import java.util.List;

import org.histo.config.enums.ContactMethod;
import org.histo.config.enums.NotificationOption;
import org.histo.model.patient.Task;

public class NoContactDataNotificationSettings {

	/**
	 * List of physician notify via email
	 */
	private List<MedicalFindingsChooser> noContactDataList;
	
	/**
	 * Temporary Task
	 */
	private Task task;
	
	public NoContactDataNotificationSettings(Task task) {
		setTask(task);
		updateNoContactDataList();
	}

	/**
	 * Updates the phone list, if the contact page was used to edit contacts.
	 */
	public void updateNoContactDataList(){
		setNoContactDataList(MedicalFindingsChooser.getSublist(task.getContacts(), ContactMethod.NO_CONTACT_DATA));
	}
	
	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	public boolean isNoContactData() {
		return noContactDataList != null && noContactDataList.size() > 0;
	}

	public Task getTask() {
		return task;
	}

	public void setTask(Task task) {
		this.task = task;
	}

	public List<MedicalFindingsChooser> getNoContactDataList() {
		return noContactDataList;
	}

	public void setNoContactDataList(List<MedicalFindingsChooser> noContactDataList) {
		this.noContactDataList = noContactDataList;
	}
	
	
	/********************************************************
	 * Getter/Setter
	 ********************************************************/
}
