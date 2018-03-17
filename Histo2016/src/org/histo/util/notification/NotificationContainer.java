package org.histo.util.notification;

import org.histo.model.AssociatedContact;
import org.histo.model.AssociatedContactNotification;
import org.histo.model.Organization;
import org.histo.model.PDFContainer;

import lombok.Getter;
import lombok.Setter;

/**
 * Class for encapsulating notifications, used for performing the notification
 * task.
 * 
 * @author andi
 *
 */
@Getter
@Setter
public class NotificationContainer {

	/**
	 * If true the notification should be performed
	 */
	protected boolean perform;

	/**
	 * contact
	 */
	protected AssociatedContact contact;

	/**
	 * Notification
	 */
	protected AssociatedContactNotification notification;

	/**
	 * The individual pdf is saved here
	 */
	protected PDFContainer pdf;

	/**
	 * Address, copied form contact
	 */
	protected String contactAddress;

	/**
	 * True if the notification failed on a previous notification attempt
	 */
	protected boolean faildPreviously;

	/**
	 * True if e.g. the address is not correct
	 */
	protected boolean warning;

	/**
	 * Warning text
	 */
	protected String warningInfo;

	public NotificationContainer(AssociatedContact contact, AssociatedContactNotification notification) {
		this.contact = contact;
		this.notification = notification;
		this.faildPreviously = notification.isFailed();
		this.perform = true;
	}

	/**
	 * Copies the notification address if failed from notification (for user to
	 * correct it) or if first try from contact.
	 */
	public void initAddressForNotificationType() {
		if (!notification.isFailed() && !notification.isRenewed()) {
			switch (notification.getNotificationTyp()) {
			case EMAIL:
				setContactAddress(contact.getPerson().getContact().getEmail());
				break;
			case FAX:
				setContactAddress(contact.getPerson().getContact().getFax());
				break;
			case PHONE:
				setContactAddress(contact.getPerson().getContact().getPhone());
				break;
			case LETTER:
				Organization orga = getContact().getPerson().getOrganizsations().size() > 0
						? getContact().getPerson().getOrganizsations().get(0)
						: null;
				setContactAddress(AssociatedContact.generateAddress(getContact(), orga));
			default:
				break;
			}
		} else {
			setContactAddress(notification.getContactAddress());
		}
	}

	/**
	 * Clears the warning
	 */
	public void clearWarning() {
		setWarning(false, "");
	}

	/**
	 * Sets warning and the warning info
	 * 
	 * @param warning
	 * @param info
	 */
	public void setWarning(boolean warning, String info) {
		setWarning(warning);
		setWarningInfo(info);
	}
}
