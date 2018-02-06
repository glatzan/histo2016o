package org.histo.util.notification;

import org.apache.commons.validator.routines.EmailValidator;
import org.histo.util.HistoUtil;

/**
 * Executes mail notifications. Checks if valid mail and sends the mail.
 * 
 * @author andi
 *
 */

public class MailExecutor extends NotificationExecutor<MailContainer> {

	public MailExecutor(NotificationFeedback feedback) {
		super(feedback);
	}

	/**
	 * Checks if the given string is empty and if the string is an email.
	 */
	@Override
	public boolean isAddressApproved(String address) {
		if (!HistoUtil.isNotNullOrEmpty(address) || !EmailValidator.getInstance().isValid(address))
			return false;

		return true;
	}

	/**
	 * Sends a mail for the given notifcationContainer (Container has to be a
	 * MailContainer)
	 */
	@Override
	protected boolean performSend(MailContainer container) {
		if (!globalSettings.getProgramSettings().isOffline()) {
			feedback.setFeedback("dialog.notification.sendProcess.mail.send", container.getContactAddress());
			
			// setting pdf as attachment
			container.getMail().setAttachment(container.getPdf());
			
			return globalSettings.getMailHandler().sendMail(container.getContactAddress(), container.getMail());
		}

		return false;
	}

}
