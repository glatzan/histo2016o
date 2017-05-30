package org.histo.dao;

import java.util.ArrayList;
import java.util.List;

import org.histo.config.enums.ContactRole;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.model.Contact;
import org.histo.model.Person;
import org.histo.model.Physician;
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
	private PatientDao patientDao;

	/**
	 * Gets an lists of contacts for the tasks and markes the allready selecte
	 * ones.
	 * 
	 * @param task
	 * @param rolesToDisplay
	 * @param showAddedContactsOnly
	 * @return
	 */
	public List<Contact> getContactList(Task task, ContactRole[] rolesToDisplay, boolean showAddedContactsOnly) {

		ArrayList<Contact> result = new ArrayList<Contact>();

		List<Contact> contacts = task.getContacts();

		if (!showAddedContactsOnly) {
			logger.debug("Show all contacts");
			// getting all contact options
			List<Physician> databaseContacts = physicianDAO.getPhysicians(rolesToDisplay, false);
			// shows all contacts but marks the already selected contacts with
			// the selected flag.

			loop: for (Physician physician : databaseContacts) {
				for (Contact contact : contacts) {
					if (contact.getPerson().getId() == physician.getPerson().getId()) {
						contact.setSelected(true);
						result.add(contact);
						continue loop;
					}
				}

				result.add(new Contact(physician.getPerson()));

			}
		} else {
			// show only selected contacts, mark them as selected
			for (Contact contact : contacts) {
				contact.setSelected(true);
			}
			result.addAll(contacts);
		}

		// setting temp index for selecting via datalist
		int i = 0;
		for (Contact contact : result) {
			contact.setTmpId(i++);
		}

		return result;
	}

	/**
	 * Evaluates the role of the contact, sets the notification method
	 * accordingly and adds the contact to the task. (or removes it)
	 * 
	 * @param task
	 * @param contact
	 * @throws CustomDatabaseInconsistentVersionException
	 */
	public void contactChangeRole(Task task, Contact contact) throws CustomDatabaseInconsistentVersionException {
		logger.trace("Called onContactChangeRole(Task task, Contact contact)");
		// role was set to none so deselect every marker

		if (contact.getRole() == ContactRole.NONE) {
			logger.debug("Removing contact");
			task.getContacts().remove(contact);
			contact.setUseEmail(false);
			contact.setUseFax(false);
			contact.setUsePhone(false);
			contact.setSelected(false);

			patientDao.savePatientAssociatedDataFailSave(task, "log.patient.task.update");

			patientDao.deletePatientAssociatedDataFailSave(contact, task, "log.patient.task.contact.remove",
					contact.toString());

			// remove id, if someone wants to readd the contact in the same
			// dialog session
			contact.setId(0);
		} else {
			logger.debug("Changing or adding contact");

			if (contact.isUseEmail() || contact.isUsePhone() || contact.isUseFax()) {
				// something was already select, do nothing
			} else if (contact.getRole() == ContactRole.SURGEON) {
				// surgeon use email per default
				contact.setUseEmail(!contact.getPerson().getEmail().isEmpty() ? true : false);
			} else if ((contact.getRole() == ContactRole.PRIVATE_PHYSICIAN
					|| contact.getRole() == ContactRole.FAMILY_PHYSICIAN) && contact.getPerson().getFax() != null
					&& !contact.getPerson().getFax().isEmpty()) {
				// private physician use fax per default
				contact.setUseFax(true);
			} else if (contact.getPerson().getEmail() != null && !contact.getPerson().getEmail().isEmpty()) {
				// other contacts use email per default
				contact.setUseEmail(!contact.getPerson().getEmail().isEmpty() ? true : false);
			}

			patientDao.savePatientAssociatedDataFailSave(contact, task, "log.patient.task.contact.add",
					contact.toString());

			// adds contact if not added jet
			if (!task.getContacts().contains(contact)) {
				task.getContacts().add(contact);
				patientDao.savePatientAssociatedDataFailSave(task, "log.patient.task.update");
			}

		}
	}

	/**
	 * Checks if the contact list contains a primary contact for the given role.
	 * If there are multiple contact marked as primary the only the first one
	 * will remain the primary one. If on contact is marked, the first contact
	 * possessing this role will be selected.
	 * 
	 * @param task
	 * @param role
	 * @throws CustomDatabaseInconsistentVersionException
	 */
	public void updateContactRolePrimary(Task task, ContactRole role, Contact primaryContact)
			throws CustomDatabaseInconsistentVersionException {
		logger.trace("updateContactRolePrimary(Task  " + task.getId() + ", ContactRole " + role.toString()
				+ ", Contact " + primaryContact + ")");
		// setting all contacts to non primary except the given one
		if (primaryContact != null) {
			for (Contact contactListItem : task.getContacts()) {

				if (contactListItem.getRole() == role) {

					// set selected contact to primary
					if (contactListItem.getId() == primaryContact.getId()) {
						if (primaryContact.isPrimaryContact() == false) {
							primaryContact.setPrimaryContact(true);

							patientDao.savePatientAssociatedDataFailSave(primaryContact, task,
									"log.patient.task.contact.primaryRole.set", primaryContact.toString());
						}
					} else {
						// all other to non primary
						if (contactListItem.isPrimaryContact()) {
							contactListItem.setPrimaryContact(false);
							patientDao.savePatientAssociatedDataFailSave(primaryContact, task,
									"log.patient.task.contact.primaryRole.removed", primaryContact.toString());
						}
					}
				}
			}
		} else {
			// setting oldest selected primary contact as primary (determined
			// via id)

			Contact oldest = null;

			for (Contact contactListItem : task.getContacts()) {
				if (contactListItem.getRole() == role) {
					if (oldest == null)
						oldest = contactListItem;

					if (contactListItem.isPrimaryContact()) {
						// setting the first selected primary contact (via id)
						if (oldest.getId() > contactListItem.getId()) {
							oldest = contactListItem;
						}
					}

				}
			}

			// if at least one contact with the given role was found, or it is
			// the oldest contact with this role which is primary, set it
			if (oldest != null) {
				updateContactRolePrimary(task, role, oldest);
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
