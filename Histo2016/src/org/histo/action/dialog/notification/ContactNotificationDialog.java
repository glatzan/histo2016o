package org.histo.action.dialog.notification;

import java.util.HashMap;

import org.histo.action.dialog.AbstractDialog;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.ContactDAO;
import org.histo.model.AssociatedContact;
import org.histo.model.AssociatedContactNotification;
import org.histo.model.patient.Task;
import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.DefaultMenuModel;
import org.primefaces.model.menu.MenuModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

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
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
		}

		super.initBean(task, Dialog.CONTACTS_NOTIFICATION);

		setAssociatedContact(associatedContact);

		generatedMenuModel();

		setSelectableRoles(ContactRole.values());

		return true;
	}

	public void generatedMenuModel() {
		AssociatedContactNotification.NotificationTyp[] typeArr = AssociatedContactNotification.NotificationTyp
				.values();

		model = new DefaultMenuModel();

		for (int i = 0; i < typeArr.length; i++) {
			boolean disabled = false;

			if (getAssociatedContact().getNotifications() != null)
				for (AssociatedContactNotification associatedContactNotification : getAssociatedContact()
						.getNotifications()) {
					if (associatedContactNotification.getNotificationTyp().equals(typeArr[i])
							&& associatedContactNotification.isActive()) {
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

	public void removeNotification(AssociatedContactNotification associatedContactNotification) {
		contactDAO.removeNotification(task, associatedContact, associatedContactNotification);
	}

	public void addNotificationAndUpdate(AssociatedContactNotification.NotificationTyp notification) {
		addNotification(notification);
		generatedMenuModel();
	}

	public void addNotification(AssociatedContactNotification.NotificationTyp notification) {
		contactDAO.addNotificationType(task, associatedContact, notification);
	}

	public void saveRoleChange() {
		genericDAO.savePatientData(getAssociatedContact(), getTask(),
				"log.patient.task.contact.roleChange", getAssociatedContact().toString(),
				getAssociatedContact().getRole().toString());
	}
}
