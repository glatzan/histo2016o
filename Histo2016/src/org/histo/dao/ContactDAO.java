package org.histo.dao;

import java.util.ArrayList;
import java.util.List;

import org.histo.config.enums.ContactRole;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.model.AssociatedContact;
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
	public List<AssociatedContact> getContactList(Task task, ContactRole[] rolesToDisplay, boolean showAddedContactsOnly) {

		ArrayList<AssociatedContact> result = new ArrayList<AssociatedContact>();

		List<AssociatedContact> associatedContacts = task.getContacts();

		if (!showAddedContactsOnly) {
			logger.debug("Show all contacts");
			// getting all associatedContact options
			List<Physician> databaseContacts = physicianDAO.getPhysicians(rolesToDisplay, false);
			// shows all contacts but marks the already selected contacts with
			// the selected flag.

			loop: for (Physician physician : databaseContacts) {
				for (AssociatedContact associatedContact : associatedContacts) {
					if (associatedContact.getPerson().getId() == physician.getPerson().getId()) {
						associatedContact.setSelected(true);
						result.add(associatedContact);
						continue loop;
					}
				}

				result.add(new AssociatedContact(task,physician.getPerson()));

			}
		} else {
			// show only selected contacts, mark them as selected
			for (AssociatedContact associatedContact : associatedContacts) {
				associatedContact.setSelected(true);
			}
			result.addAll(associatedContacts);
		}

		// setting temp index for selecting via datalist
		int i = 0;
		for (AssociatedContact associatedContact : result) {
			associatedContact.setTmpId(i++);
		}

		return result;
	}

	/**
	 * Evaluates the role of the associatedContact, sets the notification method
	 * accordingly and adds the associatedContact to the task. (or removes it)
	 * 
	 * @param task
	 * @param associatedContact
	 * @throws CustomDatabaseInconsistentVersionException
	 */
	public void contactChangeRole(Task task, AssociatedContact associatedContact) throws CustomDatabaseInconsistentVersionException {
		logger.trace("Called onContactChangeRole(Task task, AssociatedContact associatedContact)");
		// role was set to none so deselect every marker

		if (associatedContact.getRole() == ContactRole.NONE) {
			logger.debug("Removing associatedContact");
			task.getContacts().remove(associatedContact);
			associatedContact.setUseEmail(false);
			associatedContact.setUseFax(false);
			associatedContact.setUsePhone(false);
			associatedContact.setSelected(false);

			patientDao.savePatientAssociatedDataFailSave(task, "log.patient.task.update");

			patientDao.deletePatientAssociatedDataFailSave(associatedContact, task, "log.patient.task.contact.remove",
					associatedContact.toString());

			// remove id, if someone wants to readd the associatedContact in the same
			// dialog session
			associatedContact.setId(0);
		} else {
			logger.debug("Changing or adding associatedContact");

			if (associatedContact.isUseEmail() || associatedContact.isUsePhone() || associatedContact.isUseFax()) {
				// something was already select, do nothing
			} else if (associatedContact.getRole() == ContactRole.SURGEON) {
				// surgeon use email per default
				associatedContact.setUseEmail(!associatedContact.getPerson().getContact().getEmail().isEmpty() ? true : false);
			} else if ((associatedContact.getRole() == ContactRole.PRIVATE_PHYSICIAN
					|| associatedContact.getRole() == ContactRole.FAMILY_PHYSICIAN) && associatedContact.getPerson().getContact().getFax() != null
					&& !associatedContact.getPerson().getContact().getFax().isEmpty()) {
				// private physician use fax per default
				associatedContact.setUseFax(true);
			} else if (associatedContact.getPerson().getContact().getEmail() != null && !associatedContact.getPerson().getContact().getEmail().isEmpty()) {
				// other contacts use email per default
				associatedContact.setUseEmail(!associatedContact.getPerson().getContact().getEmail().isEmpty() ? true : false);
			}

			patientDao.savePatientAssociatedDataFailSave(associatedContact, task, "log.patient.task.contact.add",
					associatedContact.toString());

			// adds associatedContact if not added jet
			if (!task.getContacts().contains(associatedContact)) {
				task.getContacts().add(associatedContact);
				patientDao.savePatientAssociatedDataFailSave(task, "log.patient.task.update");
			}

		}
	}

	/**
	 * Checks if the associatedContact list contains a primary associatedContact for the given role.
	 * If there are multiple associatedContact marked as primary the only the first one
	 * will remain the primary one. If on associatedContact is marked, the first associatedContact
	 * possessing this role will be selected.
	 * 
	 * @param task
	 * @param role
	 * @throws CustomDatabaseInconsistentVersionException
	 */
	public void updateContactRolePrimary(Task task, ContactRole role, AssociatedContact primaryContact)
			throws CustomDatabaseInconsistentVersionException {
		logger.trace("updateContactRolePrimary(Task  " + task.getId() + ", ContactRole " + role.toString()
				+ ", AssociatedContact " + primaryContact + ")");
		// setting all contacts to non primary except the given one
		if (primaryContact != null) {
			for (AssociatedContact contactListItem : task.getContacts()) {

				if (contactListItem.getRole() == role) {

					// set selected associatedContact to primary
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
			// setting oldest selected primary associatedContact as primary (determined
			// via id)

			AssociatedContact oldest = null;

			for (AssociatedContact contactListItem : task.getContacts()) {
				if (contactListItem.getRole() == role) {
					if (oldest == null)
						oldest = contactListItem;

					if (contactListItem.isPrimaryContact()) {
						// setting the first selected primary associatedContact (via id)
						if (oldest.getId() > contactListItem.getId()) {
							oldest = contactListItem;
						}
					}

				}
			}

			// if at least one associatedContact with the given role was found, or it is
			// the oldest associatedContact with this role which is primary, set it
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
