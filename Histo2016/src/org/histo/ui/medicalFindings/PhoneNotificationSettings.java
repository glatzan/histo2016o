package org.histo.ui.medicalFindings;

import java.util.List;

import org.histo.config.enums.ContactMethod;
import org.histo.config.enums.NotificationOption;
import org.histo.model.patient.Task;

public class PhoneNotificationSettings {
	/**
	 * True if fax should be send;
	 */
	private boolean usePhone;

	/**
	 * List of physician notify via email
	 */
	private List<MedicalFindingsChooser> notificationPhoneList;

	public PhoneNotificationSettings(Task task) {
		setNotificationPhoneList(MedicalFindingsChooser.getSublist(task.getContacts(), ContactMethod.PHONE));
		setUsePhone(!getNotificationPhoneList().isEmpty() ? true : false);

		for (MedicalFindingsChooser notificationChooser : getNotificationPhoneList()) {
			notificationChooser.setNotificationAttachment(NotificationOption.PHONE);
		}
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	public boolean isUsePhone() {
		return usePhone;
	}

	public List<MedicalFindingsChooser> getNotificationPhoneList() {
		return notificationPhoneList;
	}

	public void setUsePhone(boolean usePhone) {
		this.usePhone = usePhone;
	}

	public void setNotificationPhoneList(List<MedicalFindingsChooser> notificationPhoneList) {
		this.notificationPhoneList = notificationPhoneList;
	}
	/********************************************************
	 * Getter/Setter
	 ********************************************************/
}
