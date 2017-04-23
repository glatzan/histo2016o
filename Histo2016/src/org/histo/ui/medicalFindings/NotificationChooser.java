package org.histo.ui.medicalFindings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.naming.directory.AttributeModificationException;

import org.histo.config.enums.ContactMethod;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.NotificationOption;
import org.histo.model.Contact;
import org.histo.model.PDFContainer;

public class NotificationChooser {

	private Contact contact;

	private NotificationOption notificationAttachment;

	private PDFContainer pdf;

	private AtomicBoolean performed;
	
	private AtomicBoolean error;
	
	public NotificationChooser(){
		performed = new AtomicBoolean(false);
		error = new AtomicBoolean(false);
	}

	public NotificationChooser(Contact contact) {
		this();
		this.contact = contact;
		this.performed = new AtomicBoolean(false);
	}

	public static final List<NotificationChooser> getSublist(List<Contact> contacts, ContactMethod contactMethod) {
		ArrayList<NotificationChooser> result = new ArrayList<NotificationChooser>(contacts.size());

		for (Contact contact : contacts) {
			NotificationChooser chooser = new NotificationChooser(contact);
			if (contactMethod == ContactMethod.ALL) {
				result.add(chooser);
				chooser.setNotificationAttachment(NotificationOption.NONE);
			} else if (contactMethod == ContactMethod.EMAIL && contact.isUseEmail()) {
				result.add(chooser);
				chooser.setNotificationAttachment(NotificationOption.TEXT);
			} else if (contactMethod == ContactMethod.FAX && contact.isUseFax()) {
				result.add(chooser);
				chooser.setNotificationAttachment(NotificationOption.PDF);
			} else if (contactMethod == ContactMethod.PHONE && contact.isUsePhone()) {
				result.add(chooser);
				chooser.setNotificationAttachment(NotificationOption.NONE);
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

	public PDFContainer getPdf() {
		return pdf;
	}

	public void setPdf(PDFContainer pdf) {
		this.pdf = pdf;
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
}