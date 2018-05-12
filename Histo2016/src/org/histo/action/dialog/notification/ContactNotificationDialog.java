package org.histo.action.dialog.notification;

import java.util.HashMap;

import org.histo.action.dialog.AbstractDialog;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.HistoDatabaseInconsistentVersionException;
import org.histo.dao.ContactDAO;
import org.histo.model.AssociatedContact;
import org.histo.model.AssociatedContactNotification;
import org.histo.model.AssociatedContactNotification.NotificationTyp;
import org.histo.model.patient.Task;
import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.DefaultMenuModel;
import org.primefaces.model.menu.MenuModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class ContactNotificationDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ContactDAO contactDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ResourceBundle resourceBundle;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TransactionTemplate transactionTemplate;

	private AssociatedContact associatedContact;

	private MenuModel model;

	private ContactRole[] selectableRoles;

	HashMap<String, String> icons = new HashMap<String, String>() {
		{
			put("EMAIL", "fa-envelope");
			put("FAX", "fa-fax");
			put("PHONE", "fa-phone");
			put("LETTER", "fa-pencil-square-o");
		}
	};

	public void initAndPrepareBean(Task task, AssociatedContact associatedContact) {
		if (initBean(task, associatedContact))
			prepareDialog();
	}

	public boolean initBean(Task task, AssociatedContact associatedContact) {
		try {
			contactDAO.reattach(associatedContact);
		} catch (HistoDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
		}

		super.initBean(task, Dialog.CONTACTS_NOTIFICATION);

		setAssociatedContact(associatedContact);

		generatedMenuModel();

		setSelectableRoles(ContactRole.values());

		return true;
	}

	public void generatedMenuModel() {
		AssociatedContactNotification.NotificationTyp[] typeArr = { NotificationTyp.EMAIL, NotificationTyp.FAX,
				NotificationTyp.LETTER, NotificationTyp.PHONE };

		model = new DefaultMenuModel();

		for (int i = 0; i < typeArr.length; i++) {
			boolean disabled = false;

			if (getAssociatedContact().getNotifications() != null)
				for (AssociatedContactNotification associatedContactNotification : getAssociatedContact()
						.getNotifications()) {
					if (associatedContactNotification.getNotificationTyp().equals(typeArr[i])
							&& associatedContactNotification.isActive() && !associatedContactNotification.isFailed()) {
						disabled = true;
						break;
					}
				}

			DefaultMenuItem item = new DefaultMenuItem("");
			item.setIcon("fa " + icons.get(typeArr[i].toString()));
			item.setCommand("#{dialogHandlerAction.contactNotificationDialog.addNotificationAndUpdate('"
					+ typeArr[i].toString() + "')}");
			item.setValue(resourceBundle.get("enum.notificationType." + typeArr[i].toString()));
			item.setDisabled(disabled);
			item.setUpdate("@form");
			model.addElement(item);
		}
	}

	public void removeNotificationAndUpdate(AssociatedContactNotification associatedContactNotification) {
		removeNotification(associatedContactNotification);
		generatedMenuModel();
	}

	public void removeNotification(AssociatedContactNotification associatedContactNotification) {
		try {
			contactDAO.removeNotification(task, associatedContact, associatedContactNotification);
		} catch (HistoDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	public void addNotificationAndUpdate(AssociatedContactNotification.NotificationTyp notification) {
		addNotification(notification);
		generatedMenuModel();
	}

	public void addNotification(AssociatedContactNotification.NotificationTyp notification) {

		try {

			transactionTemplate.execute(new TransactionCallbackWithoutResult() {

				public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
					// setting all other notifications with the same type as
					// inactive
					contactDAO.setNotificationsAsActive(task, associatedContact, notification, false);
					contactDAO.addNotificationType(task, associatedContact, notification);
				}
			});

		} catch (Exception e) {
			onDatabaseVersionConflict();
		}
	}

	public void saveRoleChange() {
		try {
			genericDAO.savePatientData(getAssociatedContact(), getTask(), "log.patient.task.contact.roleChange",
					getAssociatedContact().toString(), getAssociatedContact().getRole().toString());
		} catch (HistoDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}
}
