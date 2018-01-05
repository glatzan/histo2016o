package org.histo.util.notification;

import org.histo.model.AssociatedContact;
import org.histo.model.AssociatedContactNotification;
import org.histo.model.PDFContainer;
import org.histo.model.AssociatedContactNotification.NotificationTyp;

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
	 * Type
	 */
	protected NotificationTyp notificationTyp;

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

	public NotificationContainer(AssociatedContact contact) {
		this.contact = contact;
	}
}