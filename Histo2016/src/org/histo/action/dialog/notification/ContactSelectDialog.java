package org.histo.action.dialog.notification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.ContactDAO;
import org.histo.dao.PatientDao;
import org.histo.dao.PhysicianDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.AssociatedContact;
import org.histo.model.Physician;
import org.histo.model.patient.Task;
import org.histo.model.view.ContactPhysicanRole;
import org.histo.ui.ListChooser;
import org.histo.ui.transformer.AssociatedRoleTransformer;
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
	 * List of all ContactRole available for selecting physicians, used by
	 * contacts and settings
	 */
	private ContactRole[] selectAbleContactRoles;

	/**
	 * Array of roles for that physicians should be shown.
	 */
	private ContactRole[] showRoles;

	/**
	 * Role of the quick associatedContact select dialog, either SURGEON or
	 * PRIVATE_PHYSICIAN
	 */
	private ContactRole addAsRole;

	/**
	 * List contain contacts to select from, used by contacts
	 */
	private List<PhysicianContainer> contactList;

	/**
	 * For quickContact selection
	 */
	private PhysicianContainer selectedContact;

	private boolean manuallySelectRole = false;

	public void initAndPrepareBean(Task task, ContactRole contactRole) {
		if (initBean(task, new ContactRole[] { contactRole }, new ContactRole[] { contactRole }, contactRole))
			prepareDialog();
	}

	public void initAndPrepareBean(Task task, ContactRole[] selectAbleRoles, ContactRole[] showRoles,
			ContactRole addAsRole) {
		if (initBean(task, selectAbleRoles, showRoles, addAsRole))
			prepareDialog();
	}

	public boolean initBean(Task task, ContactRole[] selectAbleRoles, ContactRole[] showRoles, ContactRole addAsRole) {
		try {
			taskDAO.initializeTask(task, false);
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.replacePatientTaskInCurrentWorklistAndSetSelected(task);
		}

		super.initBean(task, Dialog.QUICK_CONTACTS);

		setSelectAbleContactRoles(selectAbleRoles);

		setShowRoles(showRoles);

		setContactList(getPhysicianContainers(task, getShowRoles()));

		setAddAsRole(addAsRole);

		return true;
	}

	public List<PhysicianContainer> getPhysicianContainers(Task task, ContactRole[] contactRoles) {
		List<Physician> databasePhysicians = physicianDAO.getPhysicians(contactRoles, false);

		AtomicInteger i = new AtomicInteger(0);

		List<PhysicianContainer> resultList = databasePhysicians.stream()
				.map(p -> new PhysicianContainer(p, i.getAndIncrement())).collect(Collectors.toList());

		loop: for (PhysicianContainer physicianContainer : resultList) {
			for (AssociatedContact associatedContact : task.getContacts()) {
				if (associatedContact.getPerson().equals(physicianContainer.getPhysician().getPerson())) {
					physicianContainer.addAssociatedRole(associatedContact.getRole());
					continue loop;
				}
			}
		}

		return resultList;
	}

	/**
	 * updates the associatedContact list if selection of contacts was changed
	 * (more or other roles should be displayed)
	 */
	public void updateContactList() {
		setContactList(getPhysicianContainers(task, getShowRoles()));
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

			if (task.getContacts().stream().anyMatch(p -> p.equals(associatedContact))) {
				// todo error message
				logger.debug("Not adding, double contact");
				return;
			}

			task.getContacts().add(associatedContact);

			patientDao.save(associatedContact, "log.patient.task.contact.add",
					new Object[] { associatedContact.toString() }, task.getParent());

			patientDao.save(task, "log.patient.task.update");

		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	@Getter
	@Setter
	public class PhysicianContainer {

		private int id;
		private Physician physician;
		private List<ContactRole> associatedRoles;

		public PhysicianContainer(Physician physician, int id) {
			this.physician = physician;
			this.id = id;
		}

		public void addAssociatedRole(ContactRole role) {
			if (associatedRoles == null)
				associatedRoles = new ArrayList<ContactRole>();

			associatedRoles.add(role);
		}

		public boolean hasRole(ContactRole[] contactRoles) {
			if (getAssociatedRoles() == null || getAssociatedRoles().size() == 0)
				return false;

			return getAssociatedRoles().stream().anyMatch(p -> {
				for (int i = 0; i < contactRoles.length; i++) {
					if (p == contactRoles[i])
						return true;
				}
				return false;

			});
		}
	}

}
