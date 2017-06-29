package org.histo.ui.medicalFindings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.histo.config.enums.ContactMethod;
import org.histo.config.enums.NotificationOption;
import org.histo.model.AssociatedContact;
import org.histo.util.printer.PrintTemplate;

public class MedicalFindingsChooser {

	private AssociatedContact associatedContact;

	private NotificationOption notificationAttachment;

	private PrintTemplate printTemplate;

	private AtomicBoolean performed;

	private AtomicBoolean error;

	public MedicalFindingsChooser() {
		performed = new AtomicBoolean(false);
		error = new AtomicBoolean(false);
	}

	public MedicalFindingsChooser(AssociatedContact associatedContact) {
		this();
		this.associatedContact = associatedContact;
		this.performed = new AtomicBoolean(false);
	}

	public static final List<MedicalFindingsChooser> getSublist(List<AssociatedContact> associatedContacts, ContactMethod contactMethod) {
		ArrayList<MedicalFindingsChooser> result = new ArrayList<MedicalFindingsChooser>(associatedContacts.size());

		for (AssociatedContact associatedContact : associatedContacts) {
			MedicalFindingsChooser chooser = new MedicalFindingsChooser(associatedContact);
			if (contactMethod == ContactMethod.ALL) {
				result.add(chooser);
				chooser.setNotificationAttachment(NotificationOption.NONE);
			} else if (contactMethod == ContactMethod.EMAIL && associatedContact.isUseEmail()) {
				result.add(chooser);
				chooser.setNotificationAttachment(NotificationOption.TEXT);
			} else if (contactMethod == ContactMethod.FAX && associatedContact.isUseFax()) {
				result.add(chooser);
				chooser.setNotificationAttachment(NotificationOption.FAX);
			} else if (contactMethod == ContactMethod.PHONE && associatedContact.isUsePhone()) {
				result.add(chooser);
				chooser.setNotificationAttachment(NotificationOption.NONE);
			}else if(contactMethod == ContactMethod.NO_CONTACT_DATA && !(associatedContact.isUsePhone() || associatedContact.isUseEmail() || associatedContact.isUseFax())){
				result.add(chooser);
			}
		}
		return result;
	}

	public AssociatedContact getContact() {
		return associatedContact;
	}

	public void setContact(AssociatedContact associatedContact) {
		this.associatedContact = associatedContact;
	}

	public NotificationOption getNotificationAttachment() {
		return notificationAttachment;
	}

	public void setNotificationAttachment(NotificationOption notificationAttachment) {
		this.notificationAttachment = notificationAttachment;
	}

	public boolean isPerformed() {
		return performed.get();
	}

	public void setPerformed(boolean performed) {
		this.performed.set(performed);
	}

	public boolean isError() {
		return error.get();
	}

	public void setError(boolean error) {
		this.error.set(error);
	}

	public PrintTemplate getPrintTemplate() {
		return printTemplate;
	}

	public void setPrintTemplate(PrintTemplate printTemplate) {
		this.printTemplate = printTemplate;
	}
}