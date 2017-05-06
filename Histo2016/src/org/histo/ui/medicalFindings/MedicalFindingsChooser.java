package org.histo.ui.medicalFindings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.histo.config.enums.ContactMethod;
import org.histo.config.enums.NotificationOption;
import org.histo.model.Contact;
import org.histo.model.transitory.json.printing.PrintTemplate;

public class MedicalFindingsChooser {

	private Contact contact;

	private NotificationOption notificationAttachment;

	private PrintTemplate printTemplate;

	private AtomicBoolean performed;

	private AtomicBoolean error;

	public MedicalFindingsChooser() {
		performed = new AtomicBoolean(false);
		error = new AtomicBoolean(false);
	}

	public MedicalFindingsChooser(Contact contact) {
		this();
		this.contact = contact;
		this.performed = new AtomicBoolean(false);
	}

	public static final List<MedicalFindingsChooser> getSublist(List<Contact> contacts, ContactMethod contactMethod) {
		ArrayList<MedicalFindingsChooser> result = new ArrayList<MedicalFindingsChooser>(contacts.size());

		for (Contact contact : contacts) {
			MedicalFindingsChooser chooser = new MedicalFindingsChooser(contact);
			if (contactMethod == ContactMethod.ALL) {
				result.add(chooser);
				chooser.setNotificationAttachment(NotificationOption.NONE);
			} else if (contactMethod == ContactMethod.EMAIL && contact.isUseEmail()) {
				result.add(chooser);
				chooser.setNotificationAttachment(NotificationOption.TEXT);
			} else if (contactMethod == ContactMethod.FAX && contact.isUseFax()) {
				result.add(chooser);
				chooser.setNotificationAttachment(NotificationOption.FAX);
			} else if (contactMethod == ContactMethod.PHONE && contact.isUsePhone()) {
				result.add(chooser);
				chooser.setNotificationAttachment(NotificationOption.NONE);
			}else if(contactMethod == ContactMethod.NO_CONTACT_DATA && !(contact.isUsePhone() || contact.isUseEmail() || contact.isUseFax())){
				result.add(chooser);
			}
		}
		return result;
	}

	public Contact getContact() {
		return contact;
	}

	public void setContact(Contact contact) {
		this.contact = contact;
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