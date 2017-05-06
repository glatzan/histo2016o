package org.histo.action;

import java.util.List;

import org.histo.config.enums.ContactRole;
import org.histo.model.Contact;
import org.histo.model.patient.Task;
import org.histo.ui.transformer.AssociatedRoleTransformer;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Object containing commonly used variables (mostly for dialogs)!
 * 
 * @author andi
 *
 */
@Component
@Scope(value = "session")
public class CommenDataHandlerAction {

	/**
	 * Currently selectedTask
	 */
	private Task selectedTask;

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
	 * List contain contacts to select from, used by contacts
	 */
	private List<Contact> contactList;

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	public List<ContactRole> getAssociatedRoles() {
		return associatedRoles;
	}

	public AssociatedRoleTransformer getAssociatedRolesTransformer() {
		return associatedRolesTransformer;
	}

	public void setAssociatedRoles(List<ContactRole> associatedRoles) {
		this.associatedRoles = associatedRoles;
	}

	public void setAssociatedRolesTransformer(AssociatedRoleTransformer associatedRolesTransformer) {
		this.associatedRolesTransformer = associatedRolesTransformer;
	}

	public Task getSelectedTask() {
		return selectedTask;
	}

	public void setSelectedTask(Task selectedTask) {
		this.selectedTask = selectedTask;
	}

	public List<Contact> getContactList() {
		return contactList;
	}

	public void setContactList(List<Contact> contactList) {
		this.contactList = contactList;
	}
	/********************************************************
	 * Getter/Setter
	 ********************************************************/
}
