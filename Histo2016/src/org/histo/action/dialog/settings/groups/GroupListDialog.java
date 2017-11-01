package org.histo.action.dialog.settings.groups;

import java.util.List;

import org.histo.action.dialog.AbstractDialog;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.UserDAO;
import org.histo.model.Physician;
import org.histo.model.user.HistoGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class GroupListDialog extends AbstractDialog {

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

	private List<HistoGroup> groups;

	private boolean showArchived;

	private HistoGroup selectedGroup;

	private boolean selectMode;

	private boolean editMode;

	public void initAndPrepareBean() {
		if (initBean(false, true))
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

		setSelectedGroup(null);

		super.initBean(task, Dialog.SETTINGS_GROUP_LIST);
		return true;
	}

	public void updateData() {
		setGroups(userDAO.getGroups(showArchived));
	}

	public void selectGroupAndHide() {
		super.hideDialog(getSelectedGroup());
	}

	public void archive(HistoGroup group, boolean archive) {
		try {
			group.setArchived(archive);
			userDAO.save(group, resourceBundle
					.get(archive ? "log.settings.group.archived" : "log.settings.group.dearchived", group));
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

}
