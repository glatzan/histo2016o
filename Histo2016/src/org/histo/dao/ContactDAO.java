package org.histo.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.histo.action.handler.SettingsHandler;
import org.histo.config.enums.ContactRole;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.model.AssociatedContact;
import org.histo.model.AssociatedContactNotification;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.model.patient.Diagnosis;
import org.histo.model.patient.DiagnosisRevision;
import org.histo.model.patient.Task;
import org.histo.model.view.ContactPhysicanRole;
import org.histo.util.HistoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

@Component
@Transactional
@Scope("session")
public class ContactDAO extends AbstractDAO {

	@Autowired
	private PhysicianDAO physicianDAO;

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private SettingsHandler settingsHandler;

	public void updateNotificationsOnRoleChange(Task task, AssociatedContact associatedContact) {

		if (associatedContact.getNotifications() == null) {
			associatedContact.setNotifications(new ArrayList<AssociatedContactNotification>());
		}

		// do nothing if there is
		if (associatedContact.getNotifications().size() != 0) {
			return;
		}

		List<AssociatedContactNotification.NotificationTyp> types = settingsHandler.getDefaultNotificationSettings()
				.getDefaultNotificationForRole(associatedContact.getRole());

		for (AssociatedContactNotification.NotificationTyp notificationTyp : types) {
			addNotificationType(task, associatedContact, notificationTyp);
		}

		updateNotificationOnDiagnosisChange(task, associatedContact);
	}

	public void updateNotificationOnDiagnosisChange(Task task, AssociatedContact associatedContact) {
		Set<ContactRole> sendLetterTo = new HashSet<ContactRole>();

		for (DiagnosisRevision diagnosisRevision : task.getDiagnosisContainer().getDiagnosisRevisions()) {
			for (Diagnosis diagnosis : diagnosisRevision.getDiagnoses()) {
				if (diagnosis.getDiagnosisPrototype() != null)
					sendLetterTo.addAll(diagnosis.getDiagnosisPrototype().getDiagnosisReportAsLetter());
			}
		}

		for (ContactRole contactRole : sendLetterTo) {
			System.out.println(contactRole);
		}

		// checking if contact is within the send letter to roles
		loop: for (ContactRole contactRole : sendLetterTo) {
			if (associatedContact.getRole().equals(contactRole)) {

				for (AssociatedContactNotification notification : associatedContact.getNotifications()) {
					if (notification.getNotificationTyp().equals(AssociatedContactNotification.NotificationTyp.LETTER))
						break loop;
				}

				addNotificationType(task, associatedContact, AssociatedContactNotification.NotificationTyp.LETTER);
				return;
			}
		}

	}

	public void reOrderContactList(Task task, int indexRemove, int indexMove) {
		AssociatedContact remove = task.getContacts().remove(indexRemove);

		task.getContacts().add(indexMove, remove);

		genericDAO.savePatientData(task, "log.patient.task.contact.list.reoder");
	}

	public void removeAssociatedContact(Task task, AssociatedContact associatedContact) {
		task.getContacts().remove(associatedContact);
		genericDAO.deletePatientData(associatedContact, task, "log.patient.task.contact.remove",
				associatedContact.toString());
	}

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

	public AssociatedContact addAssociatedContact(Task task, Person person, ContactRole role) {
		return addAssociatedContact(task, new AssociatedContact(task, person, role));
	}

	public AssociatedContact addAssociatedContact(Task task, AssociatedContact associatedContact) {
		task.getContacts().add(associatedContact);
		genericDAO.savePatientData(associatedContact, task, "log.patient.task.contact.add",
				new Object[] { associatedContact.toString() }, task.getParent());

		return associatedContact;
	}

	public AssociatedContactNotification addNotificationType(Task task, AssociatedContact associatedContact,
			AssociatedContactNotification.NotificationTyp notificationTyp) {
		return addNotificationType(task, associatedContact, notificationTyp, true, false, false, null);

	}

	public AssociatedContactNotification addNotificationType(Task task, AssociatedContact associatedContact,
			AssociatedContactNotification.NotificationTyp notificationTyp, boolean active, boolean performed,
			boolean failed, Date dateOfAction) {
		AssociatedContactNotification newNotification = new AssociatedContactNotification();
		newNotification.setActive(active);
		newNotification.setPerformed(performed);
		newNotification.setDateOfAction(dateOfAction);
		newNotification.setFailed(failed);
		newNotification.setNotificationTyp(notificationTyp);
		newNotification.setContact(associatedContact);

		if (associatedContact.getNotifications() == null)
			associatedContact.setNotifications(new ArrayList<AssociatedContactNotification>());

		associatedContact.getNotifications().add(newNotification);

		genericDAO.savePatientData(associatedContact, task, "log.patient.task.contact.notification.added",
				notificationTyp.toString(), associatedContact.toString());

		return newNotification;
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
