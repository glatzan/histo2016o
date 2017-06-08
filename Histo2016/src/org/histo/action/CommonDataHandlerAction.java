package org.histo.action;

import java.util.List;

import org.histo.config.enums.ContactRole;
import org.histo.config.enums.View;
import org.histo.model.Contact;
import org.histo.model.patient.Patient;
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
public class CommonDataHandlerAction {

	// ************************ Navigation ************************
	/**
	 * View options, dynamically generated depending on the users role
	 */
	private List<View> navigationPages;

	// ************************ Patient ************************
	/**
	 * Currently selectedTask
	 */
	private Patient selectedPatient;

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

	public Patient getSelectedPatient() {
		return selectedPatient;
	}

	public void setSelectedPatient(Patient selectedPatient) {
		this.selectedPatient = selectedPatient;
	}

	public List<View> getNavigationPages() {
		return navigationPages;
	}

	public void setNavigationPages(List<View> navigationPages) {
		this.navigationPages = navigationPages;
	}
	/********************************************************
	 * Getter/Setter
	 ********************************************************/
}
