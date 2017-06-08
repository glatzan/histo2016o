package org.histo.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.GenericDAO;
import org.histo.dao.PhysicianDAO;
import org.histo.model.Contact;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.model.patient.Task;
import org.histo.ui.transformer.AssociatedRoleTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class ContactHandlerAction implements Serializable {

	private static final long serialVersionUID = -3672859612072175725L;

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private PhysicianDAO physicianDAO;

	@Autowired
	private MainHandlerAction mainHandlerAction;

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	private CommonDataHandlerAction commonDataHandlerAction;
	
	/**
	 * True if archived physicians should be display
	 */
	private boolean showArchivedPhysicians = false;

	/**
	 * Array of roles for that physicians should be shown.
	 */
	private ContactRole[] showPhysicianRoles;

	/**
	 * For quickContact selection
	 */
	private Contact selectedContact;

	/**
	 * Role of the quick contact select dialog, either SURGEON or
	 * PRIVATE_PHYSICIAN
	 */
	private ContactRole selectedContactRole;

	/**
	 * Show the contact dialog using the display settings from the
	 * {@link PhysicianRoleOptions} object
	 * 
	 * @param task
	 */
	public void prepareContactsDialog(Task task) {
		prepareContactsDialog(task, null, true);
	}

	/**
	 * Gets a list with all available contact for a specific task. Filters all
	 * duplicated entries.
	 * 
	 * @param task
	 * @param surgeon
	 * @param extern
	 * @param other
	 * @param addedContact
	 */
	public void prepareContactsDialog(Task task, ContactRole[] roles, boolean showAddedContactsOnly) {

		commonDataHandlerAction.setSelectedTask(task);
		commonDataHandlerAction.setAssociatedRoles(Arrays.asList(ContactRole.values()));
		commonDataHandlerAction.setAssociatedRolesTransformer(new AssociatedRoleTransformer(commonDataHandlerAction.getAssociatedRoles()));

		setShowPhysicianRoles(new ContactRole[]{ContactRole.PRIVATE_PHYSICIAN, ContactRole.SURGEON, ContactRole.OTHER_PHYSICIAN});
		
		if (roles != null)
			setShowPhysicianRoles(roles);
		else if (getShowPhysicianRoles() == null)
			setShowPhysicianRoles(new ContactRole[] { ContactRole.PRIVATE_PHYSICIAN, ContactRole.SURGEON,
					ContactRole.OTHER_PHYSICIAN });

		updateContactList();

		mainHandlerAction.showDialog(Dialog.CONTACTS);
	}

	/**
	 * Updates the contact list using bean values.
	 * 
	 * @param task
	 */
	public void updateContactList() {
		updateContactList(commonDataHandlerAction.getSelectedTask(), getShowPhysicianRoles(), isShowArchivedPhysicians());
	}

	/**
	 * Refreshes the contact list.
	 * 
	 * @param task
	 * @param surgeon
	 * @param extern
	 * @param other
	 * @param showAddedContactsOnly
	 */
	public void updateContactList(Task task, ContactRole[] rolesToDisplay, boolean showAddedContactsOnly) {
		// refreshing the selected task
		try {
			genericDAO.refresh(task);
		} catch (CustomDatabaseInconsistentVersionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		commonDataHandlerAction.setContactList(new ArrayList<Contact>());
		
		List<Contact> contacts = task.getContacts();

		// getting all contact options
		List<Physician> databaseContacts = physicianDAO.getPhysicians(rolesToDisplay, false);

		if (!showAddedContactsOnly) {
			logger.debug("Show all contacts");
			// shows all contacts but marks the already selected contacts with
			// the selected flag.
			loop: for (Physician physician : databaseContacts) {
				for (Contact contact : contacts) {
					logger.debug("Physician id " + physician.getPerson().getId() + ", name: "
							+ physician.getPerson().getFullName() + ", contact person id "
							+ contact.getPerson().getId());
					if (contact.getPerson().getId() == physician.getPerson().getId()) {
						logger.debug("Found " + contact.getPerson().getFullName() + " in contacts, role "
								+ contact.getRole());
						contact.setSelected(true);
						commonDataHandlerAction.getContactList().add(contact);
						continue loop;
					}
				}

				commonDataHandlerAction.getContactList().add(new Contact(task,physician.getPerson()));

			}
		} else {
			// show only selected contacts, mark them as selected
			for (Contact contact : contacts) {
				contact.setSelected(true);
			}
			commonDataHandlerAction.getContactList().addAll(contacts);
		}

		// setting temp index for selecting via datalist
		int i = 0;
		for (Contact contact : commonDataHandlerAction.getContactList()) {
			contact.setTmpId(i++);
		}
	}

	/**
	 * Sobald im Kontaktdialog ein neuer Kontakt ausgewählt wird, wird je nach
	 * Art eine Benachrichtigung vorausgewählt.
	 * 
	 * @param contact
	 */
	public void onContactChangeRole(Task task, Contact contact) {
		logger.trace("Called onContactChangeRole(Task task, Contact contact)");
		// role was set to none so deselect every marker
		if (contact.getRole() == ContactRole.NONE) {
			logger.debug("Removing contact");
			task.getContacts().remove(contact);
			contact.setUseEmail(false);
			contact.setUseFax(false);
			contact.setUsePhone(false);
			contact.setSelected(false);

			genericDAO.save(task, resourceBundle.get("log.patient.task.save", task.getTaskID()), task.getPatient());

			genericDAO.delete(contact, resourceBundle.get("log.patient.task.contact.remove", task.getTaskID(),
					contact.getPerson().getName()), task.getPatient());

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

			genericDAO.save(contact,
					resourceBundle.get("log.patient.task.contact.add", task.getTaskID(), contact.getPerson().getName()),
					task.getPatient());

			// adds contact if not added jet
			if (!task.getContacts().contains(contact)) {
				task.getContacts().add(contact);
				genericDAO.save(task, resourceBundle.get("log.patient.task.save", task.getTaskID()), task.getPatient());
			}

		}

	}

	/**
	 * Updates the contact list an checks if there are primary contacts for the
	 * surgeon role and the private physician role. If no primary contact is
	 * found the first contact possessing this role will be selected.
	 * 
	 * @param task
	 */
	public void updateContactRolePrimary(Task task) {
		updateContactRolePrimary(task, ContactRole.PRIVATE_PHYSICIAN);
		updateContactRolePrimary(task, ContactRole.FAMILY_PHYSICIAN);
		updateContactRolePrimary(task, ContactRole.SURGEON);
	}

	public void updateContactRolePrimary(Task task, ContactRole role) {
		updateContactRolePrimary(task, role, null);
	}

	/**
	 * Checks if the contact list contains a primary contact for the given role.
	 * If there are multiple contact marked as primary the only the first one
	 * will remain the primary one. If on contact is marked, the first contact
	 * possessing this role will be selected.
	 * 
	 * @param task
	 * @param role
	 */
	public void updateContactRolePrimary(Task task, ContactRole role, Contact primaryContact) {
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
							genericDAO
									.save(primaryContact,
											resourceBundle.get("log.patient.task.contact.primaryRole.set",
													task.getTaskID(), primaryContact.getPerson().getName()),
											task.getPatient());
						}
					} else {
						// all other to non primary
						if (contactListItem.isPrimaryContact()) {
							contactListItem.setPrimaryContact(false);
							genericDAO.save(
									contactListItem, resourceBundle.get("log.patient.task.contact.primaryRole.removed",
											task.getTaskID(), contactListItem.getPerson().getName()),
									task.getPatient());
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
	 * Is fired on primaryContact change an checks if any other contact is the
	 * primary contact. If so the status of the other contact will be set to
	 * false.
	 * 
	 * @param task
	 * @param contact
	 */
	public void onContactChangePrimary(Task task, Contact contact) {
		for (Contact contactListItem : task.getContacts()) {
			if (contactListItem.getRole() == contact.getRole() && contactListItem.isPrimaryContact()) {
				// is the same contact return
				if (contactListItem.getId() == contact.getId())
					continue;
				else {
					// otherwise set to false
					contactListItem.setPrimaryContact(false);
					genericDAO.save(contactListItem, resourceBundle.get("log.patient.task.contact.primaryRole.removed",
							task.getTaskID(), contactListItem.getPerson().getName()), task.getPatient());
				}
			}
		}

		if (!contact.isPrimaryContact()) {
			contact.setPrimaryContact(true);
			genericDAO.save(contact, resourceBundle.get("log.patient.task.contact.primaryRole.set", task.getTaskID(),
					contact.getPerson().getName()), task.getPatient());
		}
	}

	public ContactRole[] getDefaultAssociatedRoleForPhysician(Person person) {
		return getDefaultAssociatedRoleForPhysician(person, null);
	}

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

	/********************************************************
	 * Quick Contacts
	 ********************************************************/

	/********************************************************
	 * Quick Contacts
	 ********************************************************/
	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	public ContactRole getSelectedContactRole() {
		return selectedContactRole;
	}

	public void setSelectedContactRole(ContactRole selectedContactRole) {
		this.selectedContactRole = selectedContactRole;
	}

	public Contact getSelectedContact() {
		return selectedContact;
	}

	public void setSelectedContact(Contact selectedContact) {
		this.selectedContact = selectedContact;
	}

	public boolean isShowArchivedPhysicians() {
		return showArchivedPhysicians;
	}

	public ContactRole[] getShowPhysicianRoles() {
		return showPhysicianRoles;
	}

	public void setShowArchivedPhysicians(boolean showArchivedPhysicians) {
		this.showArchivedPhysicians = showArchivedPhysicians;
	}

	public void setShowPhysicianRoles(ContactRole[] showPhysicianRoles) {
		this.showPhysicianRoles = showPhysicianRoles;
	}
	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
