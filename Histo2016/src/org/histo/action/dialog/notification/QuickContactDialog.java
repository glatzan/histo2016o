package org.histo.action.dialog.notification;

import java.util.Arrays;
import java.util.List;

import org.histo.action.WorklistHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.ContactDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.Contact;
import org.histo.model.Person;
import org.histo.model.patient.Diagnosis;
import org.histo.model.patient.Task;
import org.histo.ui.transformer.AssociatedRoleTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class QuickContactDialog extends AbstractDialog {

	/**
	 * List of all ContactRole available for selecting physicians, used by
	 * contacts and settings
	 */
	private List<ContactRole> associatedRoles;

	/**
	 * Transformer for associatedRoles
	 */
	private AssociatedRoleTransformer associatedRolesTransformer;

	/**
	 * Role of the quick contact select dialog, either SURGEON or
	 * PRIVATE_PHYSICIAN
	 */
	private ContactRole selectedContactRole;

	/**
	 * Array of roles for that physicians should be shown.
	 */
	private ContactRole[] showPhysicianRoles;

	/**
	 * List contain contacts to select from, used by contacts
	 */
	private List<Contact> contactList;

	/**
	 * For quickContact selection
	 */
	private Contact selectedContact;

	/**
	 * True if archived physicians should be display
	 */
	private boolean showArchivedPhysicians = false;

	@Autowired
	private TaskDAO taskDAO;

	@Autowired
	private WorklistHandlerAction worklistHandlerAction;

	@Autowired
	private ContactDAO contactDAO;

	public void initAndPrepareBean(Task task, ContactRole contactRole) {
		if (initBean(task, contactRole))
			prepareDialog();
	}

	public boolean initBean(Task task, ContactRole contactRole) {
		long start = System.currentTimeMillis();
		System.out.println("start: 0");
		try {
			taskDAO.initializeTask(task, false);
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("!! Version inconsistent with Database updating");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistHandlerAction.updatePatientInCurrentWorklist(task.getPatient());
		}

		super.initBean(task, Dialog.QUICK_CONTACTS);

		System.out.println("udate done: " + Long.toString(System.currentTimeMillis() - start));
		
		setAssociatedRoles(Arrays.asList(ContactRole.values()));
		setAssociatedRolesTransformer(new AssociatedRoleTransformer(getAssociatedRoles()));

		setShowPhysicianRoles(new ContactRole[] { contactRole });

		setContactList(contactDAO.getContactList(task, getShowPhysicianRoles(), false));

		System.out.println("contact done done: " + Long.toString(System.currentTimeMillis() - start));
		
		setSelectedContactRole(contactRole);

		return true;
	}

	public void selectContactAsRole() {
		selectContactAsRole(getSelectedContact(), getSelectedContactRole());
	}

	/**
	 * Sets the given contact to the given role
	 * 
	 * @param contact
	 * @param role
	 */
	public void selectContactAsRole(Contact contact, ContactRole role) {
		long start = System.currentTimeMillis();
		System.out.println("start: " + Long.toString(System.currentTimeMillis() - start));
		try {
			contact.setRole(role);

			contactDAO.contactChangeRole(getTask(), contact);

			if (role != ContactRole.NONE)
				contactDAO.updateContactRolePrimary(task, role, contact);

		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
		System.out.println("close: " + Long.toString(System.currentTimeMillis() - start));
	}

	/**
	 * Returns array of default roles for the physician
	 * 
	 * @param person
	 * @return
	 */
	public ContactRole[] getDefaultAssociatedRoleForPhysician(Person person) {
		return contactDAO.getDefaultAssociatedRoleForPhysician(person, null);
	}

	/**
	 * updates the contact list if selection of contacts was changed (more or
	 * other roles should be displayed)
	 */
	public void updateContactList() {
		setContactList(contactDAO.getContactList(task, getShowPhysicianRoles(), isShowArchivedPhysicians()));
	}

	// ************************ Getter/Setter ************************
	public List<ContactRole> getAssociatedRoles() {
		return associatedRoles;
	}

	public void setAssociatedRoles(List<ContactRole> associatedRoles) {
		this.associatedRoles = associatedRoles;
	}

	public AssociatedRoleTransformer getAssociatedRolesTransformer() {
		return associatedRolesTransformer;
	}

	public void setAssociatedRolesTransformer(AssociatedRoleTransformer associatedRolesTransformer) {
		this.associatedRolesTransformer = associatedRolesTransformer;
	}

	public ContactRole getSelectedContactRole() {
		return selectedContactRole;
	}

	public void setSelectedContactRole(ContactRole selectedContactRole) {
		this.selectedContactRole = selectedContactRole;
	}

	public ContactRole[] getShowPhysicianRoles() {
		return showPhysicianRoles;
	}

	public void setShowPhysicianRoles(ContactRole[] showPhysicianRoles) {
		this.showPhysicianRoles = showPhysicianRoles;
	}

	public List<Contact> getContactList() {
		System.out.println("hallo---");
		return contactList;
	}

	public void setContactList(List<Contact> contactList) {
		System.out.println("hallo---!!!!");
		this.contactList = contactList;
	}

	public boolean isShowArchivedPhysicians() {
		return showArchivedPhysicians;
	}

	public void setShowArchivedPhysicians(boolean showArchivedPhysicians) {
		this.showArchivedPhysicians = showArchivedPhysicians;
	}

	public Contact getSelectedContact() {
		System.out.println("hallo!!!...");
		return selectedContact;
	}

	public void setSelectedContact(Contact selectedContact) {
		System.out.println("hallo");
		this.selectedContact = selectedContact;
	}

}
