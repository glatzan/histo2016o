package org.histo.action.dialog.notification;

import java.util.Arrays;
import java.util.List;


import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.ContactDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.AssociatedContact;
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
	 * Role of the quick associatedContact select dialog, either SURGEON or
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
	private List<AssociatedContact> contactList;

	/**
	 * For quickContact selection
	 */
	private AssociatedContact selectedContact;

	/**
	 * True if archived physicians should be display
	 */
	private boolean showArchivedPhysicians = false;

	@Autowired
	private TaskDAO taskDAO;

	@Autowired
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	private ContactDAO contactDAO;

	public void initAndPrepareBean(Task task, ContactRole contactRole) {
		if (initBean(task, contactRole))
			prepareDialog();
	}

	public boolean initBean(Task task, ContactRole contactRole) {
		try {
			taskDAO.initializeTask(task, false);
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.replacePatientTaskInCurrentWorklistAndSetSelected(task);
		}

		super.initBean(task, Dialog.QUICK_CONTACTS);

		setAssociatedRoles(Arrays.asList(ContactRole.values()));
		setAssociatedRolesTransformer(new AssociatedRoleTransformer(getAssociatedRoles()));

		setShowPhysicianRoles(new ContactRole[] { contactRole });

		setContactList(contactDAO.getContactList(task, getShowPhysicianRoles(), false));

		setSelectedContactRole(contactRole);

		return true;
	}

	public void selectContactAsRole() {
		selectContactAsRole(getSelectedContact(), getSelectedContactRole());
	}

	/**
	 * Sets the given associatedContact to the given role
	 * 
	 * @param associatedContact
	 * @param role
	 */
	public void selectContactAsRole(AssociatedContact associatedContact, ContactRole role) {
		try {
			associatedContact.setRole(role);

			contactDAO.contactChangeRole(getTask(), associatedContact);

			if (role != ContactRole.NONE)
				contactDAO.updateContactRolePrimary(task, role, associatedContact);

		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
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
	 * updates the associatedContact list if selection of contacts was changed (more or
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

	public List<AssociatedContact> getContactList() {
		return contactList;
	}

	public void setContactList(List<AssociatedContact> contactList) {
		this.contactList = contactList;
	}

	public boolean isShowArchivedPhysicians() {
		return showArchivedPhysicians;
	}

	public void setShowArchivedPhysicians(boolean showArchivedPhysicians) {
		this.showArchivedPhysicians = showArchivedPhysicians;
	}

	public AssociatedContact getSelectedContact() {
		return selectedContact;
	}

	public void setSelectedContact(AssociatedContact selectedContact) {
		System.out.println("hallo");
		this.selectedContact = selectedContact;
	}

}
