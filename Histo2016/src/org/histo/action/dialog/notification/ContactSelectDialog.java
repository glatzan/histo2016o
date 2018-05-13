package org.histo.action.dialog.notification;

import java.util.List;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.HistoDatabaseInconsistentVersionException;
import org.histo.dao.ContactDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.AssociatedContact;
import org.histo.model.Physician;
import org.histo.model.patient.Task;
import org.histo.service.dao.PatientDao;
import org.histo.service.dao.PhysicianDao;
import org.histo.service.dao.PhysicianDao.PhysicianSortOrder;
import org.histo.service.dao.impl.PatientDaoImpl;
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
	private PhysicianDao physicianDao;

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

	/**
	 * IF true the role to add an physician will be determined by the physicians
	 * roles matching the addableRoles. If there are several matching roles the
	 * first matching role will be used.
	 */
	private boolean dynamicRoleSelection = false;

	public void initAndPrepareBean(Task task, ContactRole... contactRole) {
		if (initBean(task, contactRole, contactRole, contactRole, contactRole.length == 1 ? contactRole[0] : null))
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
		} catch (HistoDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.replaceTaskInCurrentWorklist(task, false);
		}

		super.initBean(task, Dialog.QUICK_CONTACTS);

		setSelectAbleContactRoles(selectAbleRoles);

		setShowRoles(showRoles);

		setAddAsRole(addAsRole);

		// if no role to add, the dynamic role selection
		if (getAddAsRole() == null)
			setDynamicRoleSelection(true);

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
		List<Physician> databasePhysicians = physicianDao.list(getShowRoles(), true, PhysicianSortOrder.PRIORITY);
		setContactList(PhysicianSelector.factory(task, databasePhysicians));
	}

	public void addPhysicianAsRole() {
		if (getSelectedContact() != null) {

			AssociatedContact associatedContact = new AssociatedContact(getTask(),
					getSelectedContact().getPhysician().getPerson());
			addPhysicianAsRole(associatedContact, getRoleForAddingContact(getSelectedContact().getPhysician()));
		}
	}

	/**
	 * IF dynamicRoleSelection is disabled the addAsRole will be used for adding a
	 * physician. If true the addableRoles an the roles of the physician will be
	 * compared and the first match will be used;
	 * 
	 * @param physician
	 * @return
	 */
	private ContactRole getRoleForAddingContact(Physician physician) {
		if (dynamicRoleSelection) {
			for (ContactRole contactRole : addableRoles) {
				for (ContactRole physicianRole : physician.getAssociatedRoles()) {
					if (contactRole == physicianRole)
						return physicianRole;
				}
			}
		} else
			getAddAsRole();

		return ContactRole.OTHER_PHYSICIAN;
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
		} catch (HistoDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

}
