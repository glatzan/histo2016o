package org.histo.action.dialog.notification;

import java.util.List;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.ContactDAO;
import org.histo.dao.PatientDao;
import org.histo.dao.PhysicianDAO;
import org.histo.dao.PhysicianDAO.PhysicianSortOrder;
import org.histo.dao.TaskDAO;
import org.histo.model.AssociatedContact;
import org.histo.model.Physician;
import org.histo.model.patient.Task;
import org.histo.ui.selectors.PhysicianSelector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class ContactSelectDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskDAO taskDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ContactDAO contactDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PhysicianDAO physicianDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PatientDao patientDao;

	/**
	 * List of all ContactRole available for selecting physicians, used by contacts
	 * and settings
	 */
	private ContactRole[] selectAbleContactRoles;

	/**
	 * Array of roles for that physicians should be shown.
	 */
	private ContactRole[] showRoles;

	private ContactRole[] addableRoles;

	/**
	 * Role of the quick associatedContact select dialog, either SURGEON or
	 * PRIVATE_PHYSICIAN
	 */
	private ContactRole addAsRole;

	/**
	 * List contain contacts to select from, used by contacts
	 */
	private List<PhysicianSelector> contactList;

	/**
	 * For quickContact selection
	 */
	private PhysicianSelector selectedContact;

	/**
	 * If true the user can change the role, for the the physician is added
	 */
	private boolean manuallySelectRole = false;

	public void initAndPrepareBean(Task task, ContactRole contactRole) {
		if (initBean(task, new ContactRole[] { contactRole }, new ContactRole[] { contactRole },
				new ContactRole[] { contactRole }, contactRole))
			prepareDialog();
	}

	public void initAndPrepareBean(Task task, ContactRole[] selectAbleRoles, ContactRole[] showRoles,
			ContactRole addAsRole) {
		if (initBean(task, selectAbleRoles, showRoles, new ContactRole[] { addAsRole }, addAsRole))
			prepareDialog();
	}

	public void initAndPrepareBean(Task task, ContactRole[] selectAbleRoles, ContactRole[] showRoles,
			ContactRole[] addableRoles, ContactRole addAsRole) {
		if (initBean(task, selectAbleRoles, showRoles, addableRoles, addAsRole))
			prepareDialog();
	}

	public boolean initBean(Task task, ContactRole[] selectAbleRoles, ContactRole[] showRoles,
			ContactRole[] addableRoles, ContactRole addAsRole) {
		try {
			taskDAO.initializeTask(task, false);
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.onVersionConflictTask(task, false);
		}

		super.initBean(task, Dialog.QUICK_CONTACTS);

		setSelectAbleContactRoles(selectAbleRoles);

		setShowRoles(showRoles);

		setAddAsRole(addAsRole);

		setAddableRoles(addableRoles);

		setSelectedContact(null);

		setManuallySelectRole(false);

		updateContactList();

		return true;
	}

	/**
	 * updates the associatedContact list if selection of contacts was changed (more
	 * or other roles should be displayed)
	 */
	public void updateContactList() {
		List<Physician> databasePhysicians = physicianDAO.getPhysicians(getShowRoles(), false,
				PhysicianSortOrder.PRIORITY);
		setContactList(PhysicianSelector.factory(task, databasePhysicians));
	}

	public void addPhysicianAsRole() {
		if (getSelectedContact() != null) {

			AssociatedContact associatedContact = new AssociatedContact(getTask(),
					getSelectedContact().getPhysician().getPerson());
			addPhysicianAsRole(associatedContact, getAddAsRole());
		}
	}

	/**
	 * Sets the given associatedContact to the given role
	 * 
	 * @param associatedContact
	 * @param role
	 */
	public void addPhysicianAsRole(AssociatedContact associatedContact, ContactRole role) {
		try {
			associatedContact.setRole(role);

			// saving
			contactDAO.addAssociatedContact(task, associatedContact);

			// settings roles
			contactDAO.updateNotificationsOnRoleChange(task, associatedContact);

			// increment counter
			contactDAO.incrementContactPriorityCounter(associatedContact.getPerson());
		} catch (IllegalArgumentException e) {
			// todo error message
			logger.debug("Not adding, double contact");
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

}
