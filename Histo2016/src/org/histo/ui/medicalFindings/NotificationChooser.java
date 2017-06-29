package org.histo.ui.medicalFindings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.histo.config.enums.ContactMethod;
import org.histo.config.enums.NotificationOption;
import org.histo.model.AssociatedContact;
import org.histo.model.PDFContainer;

public class NotificationChooser {

	private AssociatedContact associatedContact;

	private NotificationOption notificationAttachment;

	private PDFContainer pdf;

	private AtomicBoolean performed;
	
	private AtomicBoolean error;
	
	public NotificationChooser(){
		performed = new AtomicBoolean(false);
		error = new AtomicBoolean(false);
	}

	public NotificationChooser(AssociatedContact associatedContact) {
		this();
		this.associatedContact = associatedContact;
		this.performed = new AtomicBoolean(false);
	}

	public static final List<NotificationChooser> getSublist(List<AssociatedContact> associatedContacts, ContactMethod contactMethod) {
		ArrayList<NotificationChooser> result = new ArrayList<NotificationChooser>(associatedContacts.size());

		for (AssociatedContact associatedContact : associatedContacts) {
			NotificationChooser chooser = new NotificationChooser(associatedContact);
			if (contactMethod == ContactMethod.ALL) {
				result.add(chooser);
				chooser.setNotificationAttachment(NotificationOption.NONE);
			} else if (contactMethod == ContactMethod.EMAIL && associatedContact.isUseEmail()) {
				result.add(chooser);
				chooser.setNotificationAttachment(NotificationOption.TEXT);
			} else if (contactMethod == ContactMethod.FAX && associatedContact.isUseFax()) {
				result.add(chooser);
				chooser.setNotificationAttachment(NotificationOption.PDF);
			} else if (contactMethod == ContactMethod.PHONE && associatedContact.isUsePhone()) {
				result.add(chooser);
				chooser.setNotificationAttachment(NotificationOption.NONE);
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