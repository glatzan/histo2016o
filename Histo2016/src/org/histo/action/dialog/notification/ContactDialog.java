package org.histo.action.dialog.notification;

import java.io.Serializable;

import org.histo.action.DialogHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.ContactDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.AssociatedContact;
import org.histo.model.patient.Task;
import org.primefaces.event.ReorderEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class ContactDialog extends AbstractDialog {

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
	private DialogHandlerAction dialogHandlerAction;

	private ContactHolder[] contacts;

	/**
	 * List of all ContactRole available for selecting physicians, used by
	 * contacts and settings
	 */
	private ContactRole[] selectAbleContactRoles;

	/**
	 * Array of roles for that physicians should be shown.
	 */
	private ContactRole[] showRoles;

	public void initAndPrepareBean(Task task) {
		if (initBean(task))
			prepareDialog();
	}

	public boolean initBean(Task task) {
		try {
			taskDAO.initializeTask(task, false);
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.onVersionConflictTask(task, false);
		}

		super.initBean(task, Dialog.CONTACTS);

		setSelectAbleContactRoles(ContactRole.values());

		setShowRoles(ContactRole.values());

		updateContactHolders();

		return true;
	}

	public void addContact() {
		dialogHandlerAction.getContactSelectDialog().initBean(task, ContactRole.values(), ContactRole.values(),
				ContactRole.values(), ContactRole.OTHER_PHYSICIAN);
		// user can manually change the role for that the physician is added
		dialogHandlerAction.getContactSelectDialog().setManuallySelectRole(true);
		dialogHandlerAction.getContactSelectDialog().prepareDialog();
	}

	public void updateContactHolders() {
		if (task.getContacts() != null) {

			setContacts(new ContactHolder[task.getContacts().size()]);

			int i = 0;
			for (AssociatedContact associatedContact : task.getContacts()) {
				getContacts()[i] = new ContactHolder(associatedContact,
						associatedContact.getNotifications() != null
								? !associatedContact.getNotifications().stream().anyMatch(p -> p.isPerformed())
								: true);
				i++;
			}
		}
	}

	public void removeContact(Task task, AssociatedContact associatedContact) {
		try {
			contactDAO.removeAssociatedContact(task, associatedContact);
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	/**
	 * Is fired if the list is reordered by the user via drag and drop
	 *
	 * @param event
	 */
	public void onReorderList(ReorderEvent event) {
		try {

			logger.debug(
					"List order changed, moved contact from " + event.getFromIndex() + " to " + event.getToIndex());

			contactDAO.reOrderContactList(task, event.getFromIndex(), event.getToIndex());

		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	@Getter
	@Setter
	@AllArgsConstructor
	public class ContactHolder implements Serializable{

		private static final long serialVersionUID = 5002313305080131549L;
		
		private AssociatedContact contact;
		private boolean deleteAble;
	}
}
