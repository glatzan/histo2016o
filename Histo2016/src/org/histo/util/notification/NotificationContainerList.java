package org.histo.util.notification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.histo.dao.ContactDAO;
import org.histo.model.AssociatedContact;
import org.histo.model.AssociatedContactNotification;
import org.histo.model.AssociatedContactNotification.NotificationTyp;
import org.histo.model.patient.Task;
import org.histo.template.documents.TemplateDiagnosisReport;
import org.histo.util.StreamUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Class Containing data for notification
 * 
 * @author andi
 *
 */
@Configurable
@Getter
@Setter
public class NotificationContainerList {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ContactDAO contactDAO;

	/**
	 * true if the notification should be performed
	 */
	protected boolean use;

	/**
	 * All container
	 */
	protected List<NotificationContainer> container = new ArrayList<NotificationContainer>();

	/**
	 * Generic report to generate
	 */
	protected TemplateDiagnosisReport defaultReport;

	/**
	 * True if data should be send by the program
	 */
	protected boolean send;

	/**
	 * True if data should be printed
	 */
	protected boolean print;

	/**
	 * True if individual address
	 */
	protected boolean individualAddresses;

	/**
	 * Amount of prints
	 */
	protected int printCount;

	/**
	 * Type of notification
	 */
	protected NotificationTyp notificationTyp;

	public NotificationContainerList(NotificationTyp notificationTyp) {
		this.notificationTyp = notificationTyp;
	}

	/**
	 * Returns only selected containers
	 * 
	 * @return
	 */
	public List<NotificationContainer> getContainerToNotify() {
		return container.stream().filter(p -> p.isPerform()).collect(Collectors.toList());
	}

	/**
	 * Renews all active notifications if they failed before
	 * 
	 * @param task
	 * @param contactHolder
	 */
	public void renewNotifications(Task task) {
		for (Iterator<NotificationContainer> it = container.iterator(); it.hasNext();) {
			NotificationContainer container = it.next();
			if (container.isFaildPreviously() && container.isPerform()) {
				contactDAO.renewNotification(task, container.getContact(), container.getNotification());
			}
		}

		updateList(task.getContacts(), notificationTyp);
	}

	/**
	 * Method updates the contact list for the given contact type. It will not
	 * overwrite changes by the user.
	 * 
	 * @param contacts
	 * @param notificationTyp
	 */
	public void updateList(List<AssociatedContact> contacts, NotificationTyp notificationTyp) {

		// copie the list of current containers with notifications
		List<NotificationContainer> tmpContainers = new ArrayList<NotificationContainer>(container);

		for (AssociatedContact associatedContact : contacts) {

			// getting the notification from the contact selected by the notification type
			List<AssociatedContactNotification> notification = associatedContact
					.getNotificationTypAsList(notificationTyp, true);

			// if there is no notification of this type do nothing, there should be only 1
			// notification pending of the given type, if there are more ignore the rest
			if (notification.size() != 0) {
				try {
					// searching if the notification is already in the list, updating
					NotificationContainer tmpContainer = tmpContainers.stream()
							.filter(p -> p.getContact().equals(associatedContact))
							.collect(StreamUtils.singletonCollector());
					// updating notification type
					tmpContainer.setNotification(notification.get(0));
					tmpContainer.setFaildPreviously(notification.get(0).isFailed());
					tmpContainers.remove(tmpContainer);
				} catch (IllegalStateException e) {
					// not in list creating a new notification
					NotificationContainer holder = (notificationTyp == NotificationTyp.EMAIL
							? new MailContainer(associatedContact, notification.get(0))
							: new NotificationContainer(associatedContact, notification.get(0)));

					holder.initAddressForNotificationType();
					container.add(holder);
				}
			}
		}

		// container which are still in the tmpContainers array were removed, so delete
		// them from the current container list
		for (NotificationContainer contactContainer : tmpContainers)
			container.remove(contactContainer);

		// sorting
		Collections.sort(container, (NotificationContainer p1, NotificationContainer p2) -> {
			if (p1.isFaildPreviously() == p2.isFaildPreviously()) {
				return 0;
			} else if (p1.isFaildPreviously()) {
				return 1;
			} else {
				return -1;
			}
		});
	}
}
