package org.histo.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.dao.GenericDAO;
import org.histo.dao.PhysicianDAO;
import org.histo.model.Contact;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.model.patient.Task;
import org.histo.model.transitory.PhysicianRoleOptions;
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

	/**
	 * List with all available contacts
	 */
	private List<Contact> allAvailableContact;

	/**
	 * containing options for the physician list
	 */
	private PhysicianRoleOptions physicianRoleOptions;

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
	 * Temporary task
	 */
	private Task temporaryTask;

	@PostConstruct
	public void prepareBean() {
		setPhysicianRoleOptions(new PhysicianRoleOptions());
	}

	/**
	 * Show the contact dialog using the display settings from the
	 * {@link PhysicianRoleOptions} object
	 * 
	 * @param task
	 */
	public void prepareContactsDialog(Task task) {
		prepareContactsDialog(task, true, true, true, true, true);
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
	public void prepareContactsDialog(Task task, boolean surgeon, boolean privatePhysician, boolean other,
			boolean familyPhysician, boolean showAddedContactsOnly) {

		getPhysicianRoleOptions().setAll(surgeon, privatePhysician, familyPhysician, other, showAddedContactsOnly);

		updateContactList(task);
		
		mainHandlerAction.showDialog(Dialog.CONTACTS);
	}

	/**
	 * Updates the contact list using bean values.
	 * 
	 * @param task
	 */
	public void updateContactList(Task task) {
		updateContactList(task, getPhysicianRoleOptions().isSurgeon(), getPhysicianRoleOptions().isPrivatePhysician(),
				getPhysicianRoleOptions().isOther(), getPhysicianRoleOptions().isFamilyPhysician(),
				getPhysicianRoleOptions().isShowAddedContactsOnly());
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
	public void updateContactList(Task task, boolean surgeon, boolean extern, boolean other, boolean familyPhsysician,
			boolean showAddedContactsOnly) {
		// refreshing the selected task
		genericDAO.refresh(task);

		setAllAvailableContact(new ArrayList<Contact>());

		List<Contact> contacts = task.getContacts();

		// getting all contact options
		List<Physician> databaseContacts = physicianDAO
				.getPhysicians(ContactRole.getRoles(surgeon, extern, other, familyPhsysician), false);

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
						getAllAvailableContact().add(contact);
						continue loop;
					}
				}

				getAllAvailableContact().add(new Contact(physician.getPerson()));

			}
		} else {
			// show only selected contacts, mark them as selected
			for (Contact contact : contacts) {
				contact.setSelected(true);
			}
			getAllAvailableContact().addAll(contacts);
		}

		// setting temp index for selecting via datalist
		int i = 0;
		for (Contact contact : getAllAvailableContact()) {
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
				contact.setUseEmail(true);
			} else if ((contact.getRole() == ContactRole.PRIVATE_PHYSICIAN
					|| contact.getRole() == ContactRole.FAMILY_PHYSICIAN) && contact.getPerson().getFax() != null
					&& !contact.getPerson().getFax().isEmpty()) {
				// private physician use fax per default
				contact.setUseFax(true);
			} else if (contact.getPerson().getEmail() != null && !contact.getPerson().getEmail().isEmpty()) {
				// other contacts use email per default
				contact.setUseEmail(true);
			}

			genericDAO.save(contact,
					resourceBundle.get("log.patient.task.contact.add", task.getTaskID(), contact.getPerson().getName()),
					task.getPatient());

			// adds contact if not added jet
			if (!task.getContacts().contains(contact)) {
				task.getContacts().add(contact);
				genericDAO.save(task, resourceBundle.get("log.patient.task.save", task.getTaskID()), task.getPatient());
			}

			System.out.println("saving");

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
		Contact first = null;
		boolean primary = false;

		for (Contact contactListItem : task.getContacts()) {
			if (contactListItem.getRole() == role) {
				if (first == null)
					first = contactListItem;

				if (contactListItem.isPrimaryContact()) {
					if (primary) {
						contactListItem.setPrimaryContact(false);
						genericDAO
								.save(contactListItem,
										resourceBundle.get("log.patient.task.contact.primaryRole.removed",
												task.getTaskID(), contactListItem.getPerson().getName()),
										task.getPatient());

					} else
						primary = true;
				}
			}
		}

		if (!primary && first != null) {
			first.setPrimaryContact(true);
			genericDAO.save(first, resourceBundle.get("log.patient.task.contact.primaryRole.set", task.getTaskID(),
					first.getPerson().getName()), task.getPatient());
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

	public String getDefaultRoleForPhysician(Person person) {
		Physician result = physicianDAO.getPhysicianByPerson(person);
		if (result == null)
			return ContactRole.NONE.toString();

		return result.getDefaultContactRole().toString();
	}

	/********************************************************
	 * Quick Contacts
	 ********************************************************/
	public void prepareQuickContactsDialog(Task task, ContactRole contactRole) {
		setTemporaryTask(task);
		setSelectedContact(null);

		if (contactRole == ContactRole.SURGEON)
			getPhysicianRoleOptions().setAll(true, false, false, false, false);
		else if (contactRole == ContactRole.PRIVATE_PHYSICIAN)
			getPhysicianRoleOptions().setAll(false, true, false, false, false);

		updateContactList(task, getPhysicianRoleOptions().isSurgeon(), getPhysicianRoleOptions().isPrivatePhysician(),
				getPhysicianRoleOptions().isOther(), getPhysicianRoleOptions().isFamilyPhysician(), false);

		setSelectedContactRole(contactRole);

		mainHandlerAction.showDialog(Dialog.QUICK_CONTACTS);
	}

	public void selectContactAsRole(Contact contact, ContactRole role) {
		contact.setRole(role);
		onContactChangeRole(getTemporaryTask(),contact);

		if (role != ContactRole.NONE)
			updateContactRoleListForPrimaryContact(getTemporaryTask(), contact);
	}

	public void hideQuickContactsDialog() {
		setTemporaryTask(null);
		setSelectedContactRole(null);
		setSelectedContact(null);
		mainHandlerAction.hideDialog(Dialog.QUICK_CONTACTS);
	}

	/**
	 * Sets all other contacts with the same role to none primary
	 * 
	 * @param task
	 * @param primaryContact
	 */
	public void updateContactRoleListForPrimaryContact(Task task, Contact primaryContact) {
		for (Contact contactListItem : task.getContacts()) {
			if (primaryContact.getRole() == contactListItem.getRole()) {
				contactListItem.setPrimaryContact(false);
				genericDAO.save(contactListItem, resourceBundle.get("log.patient.task.contact.primaryRole.removed",
						task.getTaskID(), contactListItem.getPerson().getName()), task.getPatient());
			}
		}

		if (!primaryContact.isPrimaryContact()) {
			primaryContact.setPrimaryContact(true);
			genericDAO.save(primaryContact, resourceBundle.get("log.patient.task.contact.primaryRole.set",
					task.getTaskID(), primaryContact.getPerson().getName()), task.getPatient());
		}
	}

	/********************************************************
	 * Quick Contacts
	 ********************************************************/
	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	public List<Contact> getAllAvailableContact() {
		return allAvailableContact;
	}

	public void setAllAvailableContact(List<Contact> allAvailableContact) {
		this.allAvailableContact = allAvailableContact;
	}

	public PhysicianRoleOptions getPhysicianRoleOptions() {
		return physicianRoleOptions;
	}

	public void setPhysicianRoleOptions(PhysicianRoleOptions physicianRoleOptions) {
		this.physicianRoleOptions = physicianRoleOptions;
	}

	public ContactRole getSelectedContactRole() {
		return selectedContactRole;
	}

	public void setSelectedContactRole(ContactRole selectedContactRole) {
		this.selectedContactRole = selectedContactRole;
	}

	public Task getTemporaryTask() {
		return temporaryTask;
	}

	public void setTemporaryTask(Task temporaryTask) {
		this.temporaryTask = temporaryTask;
	}

	public Contact getSelectedContact() {
		return selectedContact;
	}

	public void setSelectedContact(Contact selectedContact) {
		this.selectedContact = selectedContact;
	}
	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
