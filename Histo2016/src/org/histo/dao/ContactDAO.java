package org.histo.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.histo.action.handler.GlobalSettings;
import org.histo.config.enums.ContactRole;
import org.histo.model.AssociatedContact;
import org.histo.model.AssociatedContactNotification;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.model.patient.Diagnosis;
import org.histo.model.patient.DiagnosisRevision;
import org.histo.model.patient.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Scope("session")
public class ContactDAO extends AbstractDAO {

	@Autowired
	private PhysicianDAO physicianDAO;

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private GlobalSettings globalSettings;

	/**
	 * Loads the predifend notification methods for the specific roles and
	 * applies them on the contact.
	 * 
	 * @param task
	 * @param associatedContact
	 */
	public void updateNotificationsOnRoleChange(Task task, AssociatedContact associatedContact) {

		if (associatedContact.getNotifications() == null) {
			associatedContact.setNotifications(new ArrayList<AssociatedContactNotification>());
		}

		// do nothing if there is
		if (associatedContact.getNotifications().size() != 0) {
			return;
		}

		List<AssociatedContactNotification.NotificationTyp> types = globalSettings.getDefaultNotificationSettings()
				.getDefaultNotificationForRole(associatedContact.getRole());

		for (AssociatedContactNotification.NotificationTyp notificationTyp : types) {
			addNotificationType(task, associatedContact, notificationTyp);
		}

		updateNotificationForPhysicalDiagnosisReport(task, associatedContact);
	}

	/**
	 * Checks all diagnoses for physical diagnosis report sending, and checks if
	 * the contact is a affected. If so the contact will be marked.
	 * 
	 * @param task
	 * @param associatedContact
	 */
	public void updateNotificationForPhysicalDiagnosisReport(Task task, AssociatedContact associatedContact) {
		Set<ContactRole> sendLetterTo = new HashSet<ContactRole>();

		// checking if a already a report should be send physically, if so do
		// nothing and return
		if (associatedContact.getNotifications().stream()
				.anyMatch(p -> p.getNotificationTyp().equals(AssociatedContactNotification.NotificationTyp.LETTER)))
			return;

		// collecting roles for which a report should be physically send
		for (DiagnosisRevision diagnosisRevision : task.getDiagnosisContainer().getDiagnosisRevisions()) {
			for (Diagnosis diagnosis : diagnosisRevision.getDiagnoses()) {
				if (diagnosis.getDiagnosisPrototype() != null)
					sendLetterTo.addAll(diagnosis.getDiagnosisPrototype().getDiagnosisReportAsLetter());
			}
		}

		// checking if contact is within the send letter to roles
		for (ContactRole contactRole : sendLetterTo) {
			if (associatedContact.getRole().equals(contactRole)) {
				// adding notification and return;
				addNotificationType(task, associatedContact, AssociatedContactNotification.NotificationTyp.LETTER);
				return;
			}
		}

	}

	/**
	 * Updates all contacts an checks if a physical letter should be send to
	 * them (depending on the selected diagnosis)
	 * 
	 * @param task
	 * @param diagnosisRevision
	 */
	public void updateNotificationsForPhysicalDiagnosisReport(Task task) {
		for (AssociatedContact associatedContact : task.getContacts()) {
			updateNotificationForPhysicalDiagnosisReport(task, associatedContact);
		}
	}

	public void reOrderContactList(Task task, int indexRemove, int indexMove) {
		AssociatedContact remove = task.getContacts().remove(indexRemove);

		task.getContacts().add(indexMove, remove);

		genericDAO.savePatientData(task, "log.patient.task.contact.list.reoder");
	}

	/**
	 * removes a contact
	 * 
	 * @param task
	 * @param associatedContact
	 */
	public void removeAssociatedContact(Task task, AssociatedContact associatedContact) {
		task.getContacts().remove(associatedContact);
		genericDAO.deletePatientData(associatedContact, task, "log.patient.task.contact.remove",
				associatedContact.toString());
	}

	/**
	 * removes a notification
	 * 
	 * @param task
	 * @param associatedContact
	 * @param notification
	 */
	public void removeNotification(Task task, AssociatedContact associatedContact,
			AssociatedContactNotification notification) {

		if (associatedContact.getNotifications() != null) {
			associatedContact.getNotifications().remove(notification);

			// only remove from array, and deleting the entity only (no saving
			// of contact necessary because mapped within notification)
			genericDAO.deletePatientData(notification, task, "log.patient.task.contact.notification.removed",
					notification.getNotificationTyp().toString(), associatedContact.toString());
		}
	}

	/**
	 * Adds an associated contact
	 * 
	 * @param task
	 * @param associatedContact
	 * @return
	 */
	public AssociatedContact addAssociatedContact(Task task, Person person, ContactRole role) {
		return addAssociatedContact(task, new AssociatedContact(task, person, role));
	}

	/**
	 * Adds an associated contact
	 * 
	 * @param task
	 * @param associatedContact
	 * @return
	 */
	public AssociatedContact addAssociatedContact(Task task, AssociatedContact associatedContact) {
		if (task.getContacts().stream().anyMatch(p -> p.equals(associatedContact)))
			throw new IllegalArgumentException("Already in list");

		task.getContacts().add(associatedContact);
		associatedContact.setTask(task);
		genericDAO.savePatientData(associatedContact, task, "log.patient.task.contact.add",
				new Object[] { associatedContact.toString() }, task.getParent());

		return associatedContact;
	}

	/**
	 * Adds a new notification with the given type
	 * 
	 * @param task
	 * @param associatedContact
	 * @param notificationTyp
	 * @return
	 */
	public AssociatedContactNotification addNotificationType(Task task, AssociatedContact associatedContact,
			AssociatedContactNotification.NotificationTyp notificationTyp) {
		return addNotificationType(task, associatedContact, notificationTyp, true, false, false, null, null);

	}

	/**
	 * Adds a new notification with the given type
	 * 
	 * @param task
	 * @param associatedContact
	 * @param notificationTyp
	 * @param active
	 * @param performed
	 * @param failed
	 * @param dateOfAction
	 * @return
	 */
	public AssociatedContactNotification addNotificationType(Task task, AssociatedContact associatedContact,
			AssociatedContactNotification.NotificationTyp notificationTyp, boolean active, boolean performed,
			boolean failed, Date dateOfAction, String customAddress) {
		AssociatedContactNotification newNotification = new AssociatedContactNotification();
		newNotification.setActive(active);
		newNotification.setPerformed(performed);
		newNotification.setDateOfAction(dateOfAction);
		newNotification.setFailed(failed);
		newNotification.setNotificationTyp(notificationTyp);
		newNotification.setContact(associatedContact);
		newNotification.setContactAddress(customAddress);

		if (associatedContact.getNotifications() == null)
			associatedContact.setNotifications(new ArrayList<AssociatedContactNotification>());

		associatedContact.getNotifications().add(newNotification);

		genericDAO.savePatientData(associatedContact, task, "log.patient.task.contact.notification.added",
				notificationTyp.toString(), associatedContact.toString());

		return newNotification;
	}

	/**
	 * Sets the given notification as inactive an adds a new notification of the
	 * same type (active)
	 * 
	 * @param task
	 * @param associatedContact
	 * @param notification
	 */
	public void renewNotification(Task task, AssociatedContact associatedContact,
			AssociatedContactNotification notification) {

		notification.setActive(false);
		genericDAO.savePatientData(notification, task, "log.patient.task.contact.notification.inactive",
				notification.getNotificationTyp().toString(), associatedContact.toString());
		addNotificationType(task, associatedContact, notification.getNotificationTyp());
	}

	/**
	 * Sets all notifications with the given type to the given active status
	 * 
	 * @param task
	 * @param associatedContact
	 * @param notificationTyp
	 * @param active
	 */
	public void setNotificationsAsActive(Task task, AssociatedContact associatedContact,
			AssociatedContactNotification.NotificationTyp notificationTyp, boolean active) {
		for (AssociatedContactNotification notification : associatedContact.getNotifications()) {
			if (notification.getNotificationTyp().equals(notificationTyp) && notification.isActive()) {
				notification.setActive(active);
				genericDAO.savePatientData(notification, task, "log.patient.task.contact.notification.inactive",
						notificationTyp.toString(), associatedContact.toString());
			}
		}
	}

	/**
	 * Gets the Physician object for a person an returns the associated roles.
	 * 
	 * @param person
	 * @param showOnlyRolesIfAvailable
	 * @return
	 */
	public ContactRole[] getDefaultAssociatedRoleForPhysician(Person person, ContactRole[] showOnlyRolesIfAvailable) {
		Physician result = physicianDAO.getPhysicianByPerson(person);

		if (result == null)
			return new ContactRole[] { ContactRole.NONE };
		else if (showOnlyRolesIfAvailable == null)
			return result.getAssociatedRolesAsArray();
		else {
			ArrayList<ContactRole> resultArr = new ArrayList<ContactRole>();

			for (ContactRole contactRole : result.getAssociatedRolesAsArray()) {
				for (ContactRole showOnly : showOnlyRolesIfAvailable) {
					if (contactRole == showOnly) {
						resultArr.add(contactRole);
						break;
					}
				}
			}

			return resultArr.toArray(new ContactRole[resultArr.size()]);
		}
	}

}
