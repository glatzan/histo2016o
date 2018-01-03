package org.histo.action.dialog.settings.users;

import java.util.Arrays;
import java.util.List;

import org.histo.action.dialog.AbstractDialog;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseConstraintViolationException;
import org.histo.dao.UserDAO;
import org.histo.model.user.HistoUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class EditUserDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ResourceBundle resourceBundle;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserDAO userDAO;

	private HistoUser user;

	private List<ContactRole> allRoles;

	public void initAndPrepareBean(HistoUser user) {
		if (initBean(user))
			prepareDialog();
	}

	public boolean initBean(HistoUser user) {
		setUser(user);
		setAllRoles(Arrays.asList(ContactRole.values()));

		super.initBean(task, Dialog.SETTINGS_USERS_EDIT);
		return true;
	}

	public void saveUser() {

	}

	public void prepareDeleteUser() {
		prepareDialog(Dialog.SETTINGS_USERS_DELETE);
	}

	public void deleteUser() {
		try {
			userDAO.deleteUser(user);
		} catch (CustomDatabaseConstraintViolationException e) {
			prepareDialog(Dialog.SETTINGS_USERS_DELETE_ERROR);
		}
	}
}
