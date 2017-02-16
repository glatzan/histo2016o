package org.histo.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactMethod;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.Notification;
import org.histo.dao.GenericDAO;
import org.histo.dao.PhysicianDAO;
import org.histo.dao.TaskDAO;
import org.histo.experimental.NotificationHandler;
import org.histo.model.Contact;
import org.histo.model.PDFContainer;
import org.histo.model.Physician;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.model.transitory.PhysicianRoleOptions;
import org.histo.ui.NotificationChooser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class ContactHandlerAction implements Serializable {

	private static final long serialVersionUID = -3672859612072175725L;

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
	public void showContacts(Task task) {
		updateContactList(task);
		mainHandlerAction.showDialog(Dialog.CONTACTS);
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
	public void showContacts(Task task, boolean surgeon, boolean privatePhysician, boolean other,
			boolean familyPhysician, boolean showAddedContactsOnly) {

		getPhysicianRoleOptions().setSurgeon(surgeon);
		getPhysicianRoleOptions().setPrivatePhysician(privatePhysician);
		getPhysicianRoleOptions().setOther(other);
		getPhysicianRoleOptions().setFamilyPhysician(familyPhysician);
		getPhysicianRoleOptions().setShowAddedContactsOnly(showAddedContactsOnly);

		showContacts(task);
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
			// shows all contacts but marks the already selected contacts with
			// the selected flag.
			loop: for (Physician physician : databaseContacts) {
				for (Contact contact : contacts) {
					if (contact.getPhysician().getId() == physician.getId()) {
						contact.setSelected(true);
						getAllAvailableContact().add(contact);
						continue loop;
					}
				}

				getAllAvailableContact().add(new Contact(physician));

			}
		} else {
			// show only selected contacts, mark them as selected
			for (Contact contact : contacts) {
				contact.setSelected(true);
			}
			getAllAvailableContact().addAll(contacts);
		}

	}

	/**
	 * Sobald im Kontaktdialog ein neuer Kontakt ausgewählt wird, wird je nach
	 * Art eine Benachrichtigung vorausgewählt.
	 * 
	 * @param contact
	 */
	public void onContactChangeRole(Contact contact, Task task) {
		// role was set to none so deselect every marker
		if (contact.getRole() == ContactRole.NONE) {
			task.getContacts().remove(contact);
			contact.setUseEmail(false);
			contact.setUseFax(false);
			contact.setUsePhone(false);
			genericDAO.delete(contact, resourceBundle.get("log.patient.task.contact.remove", task.getTaskID(),
					contact.getPhysician().getPerson().getName()), task.getPatient());
		} else {

			if (contact.isUseEmail() || contact.isUsePhone() || contact.isUseFax()) {
				// something was already select, do nothing
			} else if (contact.getRole() == ContactRole.SURGEON) {
				// surgeon use email per default
				contact.setUseEmail(true);
			} else if ((contact.getRole() == ContactRole.PRIVATE_PHYSICIAN
					|| contact.getRole() == ContactRole.FAMILY_PHYSICIAN) && contact.getPhysician().getPerson().getFax() != null
					&& !contact.getPhysician().getPerson().getFax().isEmpty()) {
				// private physician use fax per default
				contact.setUseFax(true);
			} else if (contact.getPhysician().getPerson().getEmail() != null && !contact.getPhysician().getPerson().getEmail().isEmpty()) {
				// other contacts use email per default
				contact.setUseEmail(true);
			}

			// adds contact if not added jet
			if (!task.getContacts().contains(contact)) {
				task.getContacts().add(contact);
			}

			genericDAO.save(contact, resourceBundle.get("log.patient.task.contact.add", task.getTaskID(),
					contact.getPhysician().getPerson().getName()), task.getPatient());

			System.out.println("saving");

		}

		updateContactRolePrimary(task);

		genericDAO.save(task, resourceBundle.get("log.patient.task.save", task.getTaskID()), task.getPatient());
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

	/**
	 * Checks if the contact list contains a primary contact for the given role.
	 * If there are multiple contact marked as primary the only the first one
	 * will remain the primary one. If on contact is marked, the first contact
	 * possessing this role will be selected.
	 * 
	 * @param task
	 * @param role
	 */
	public void updateContactRolePrimary(Task task, ContactRole role) {
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
												task.getTaskID(), contactListItem.getPhysician().getPerson().getName()),
										task.getPatient());

					} else
						primary = true;
				}
			}
		}

		if (!primary && first != null) {
			first.setPrimaryContact(true);
			genericDAO.save(first, resourceBundle.get("log.patient.task.contact.primaryRole.set", task.getTaskID(),
					first.getPhysician().getPerson().getName()), task.getPatient());
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
							task.getTaskID(), contactListItem.getPhysician().getPerson().getName()), task.getPatient());
				}
			}
		}

		if (!contact.isPrimaryContact()) {
			contact.setPrimaryContact(true);
			genericDAO.save(contact, resourceBundle.get("log.patient.task.contact.primaryRole.set", task.getTaskID(),
					contact.getPhysician().getPerson().getName()), task.getPatient());
		}
	}

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

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
