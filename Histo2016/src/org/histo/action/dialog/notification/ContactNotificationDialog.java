package org.histo.action.dialog.notification;

import java.util.ArrayList;

import org.histo.action.dialog.AbstractDialog;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.ContactDAO;
import org.histo.dao.GenericDAO;
import org.histo.model.AssociatedContact;
import org.histo.model.AssociatedContactNotification;
import org.histo.model.patient.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.DefaultMenuModel;
import org.primefaces.model.menu.MenuModel;

@Configurable
@Getter
@Setter
public class ContactNotificationDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ContactDAO contactDAO;

	private AssociatedContact associatedContact;

	private MenuModel model;

	public void initAndPrepareBean(Task task, AssociatedContact associatedContact) {
		if (initBean(task, associatedContact))
			prepareDialog();
	}

	public boolean initBean(Task task, AssociatedContact associatedContact) {
		try {
			contactDAO.refresh(associatedContact);
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
		}

		super.initBean(task, Dialog.CONTACTS_NOTIFICATION);

		setAssociatedContact(associatedContact);

		generatedMenuModel();

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

			DefaultMenuItem item = new DefaultMenuItem("External");
			item.setIcon("ui-icon-home");
			item.setCommand("#{dialogHandlerAction.contactNotificationDialog.addNotificationAndUpdate('"
					+ typeArr[i].toString() + "')}");
			item.setValue(typeArr[i]);
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

}
