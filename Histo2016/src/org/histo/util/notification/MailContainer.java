package org.histo.util.notification;

import org.histo.model.AssociatedContact;
import org.histo.template.mail.DiagnosisReportMail;

import lombok.Getter;
import lombok.Setter;

/**
 * Child of NotificationContainer, adds a field for the mail object
 * 
 * @author andi
 *
 */
@Getter
@Setter
public class MailContainer extends NotificationContainer {

	private DiagnosisReportMail mail;
	
	public MailContainer(AssociatedContact contact) {
		super(contact);
	}

}
