package org.histo.action.dialog.settings.users;

import java.util.List;

import org.histo.action.UserHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.UserDAO;
import org.histo.model.Physician;
import org.histo.model.user.HistoGroup;
import org.histo.model.user.HistoUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class UserListDialog extends AbstractDialog {

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TransactionTemplate transactionTemplate;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserDAO userDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserHandlerAction userHandlerAction;

	private List<HistoUser> users;

	private boolean showArchived;

	private HistoUser selectedUser;

	private boolean selectMode;

	private boolean editMode;

	public void initAndPrepareBean() {
		if (initBean(false, false))
			prepareDialog();
	}

	public void initAndPrepareBean(boolean selectMode, boolean editMode) {
		if (initBean(selectMode, editMode))
			prepareDialog();
	}

	public boolean initBean(boolean selectMode, boolean editMode) {
		updateData();

		setShowArchived(false);
		setSelectMode(selectMode);
		setEditMode(editMode);

		setSelectedUser(null);

		super.initBean(task, Dialog.SETTINGS_USERS_LIST);
		return true;
	}

	public void updateData() {
		setUsers(userDAO.getUsers(showArchived));
	}

	public void selectUserAndHide() {
		super.hideDialog(getSelectedUser());
	}

	public void onChangeUserRole(HistoUser histoUser) {
		try {
			userHandlerAction.groupOfUserHasChanged(histoUser);
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	public void archive(HistoUser user, boolean archive) {
		try {
			user.setArchived(archive);
			userDAO.save(user,
					resourceBundle.get(archive ? "log.settings.user.archived" : "log.settings.user.dearchived", user));
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

}
