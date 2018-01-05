package org.histo.util.notification;

import org.apache.commons.validator.routines.EmailValidator;
import org.histo.action.handler.GlobalSettings;
import org.histo.util.HistoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Executes mail notifications. Checks if valid mail and sends the mail.
 * 
 * @author andi
 *
 */
@Configurable
public class MailExecutor extends NotificationExecutor<MailContainer> {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GlobalSettings globalSettings;

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
	public boolean performSend(MailContainer container) {
		if (!globalSettings.getProgramSettings().isOffline())
			return globalSettings.getMailHandler().sendMail(container.getContactAddress(), container.getMail());

		return super.performSend(container);
	}

}
