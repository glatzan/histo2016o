package org.histo.action.dialog.settings.groups;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.histo.action.dialog.AbstractDialog;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.View;
import org.histo.config.enums.WorklistSearchOption;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.SettingsDAO;
import org.histo.dao.UserDAO;
import org.histo.model.user.HistoGroup;
import org.histo.model.user.HistoGroup.AuthRole;
import org.histo.model.user.HistoPermissions;
import org.histo.model.user.HistoSettings;
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
public class GroupEditDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ResourceBundle resourceBundle;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserDAO userDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TransactionTemplate transactionTemplate;

	private boolean newGroup;

	private HistoGroup group;

	private View[] allViews;

	private List<WorklistSearchOption> availableWorklistsToLoad;

	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private Map<String, PermissionHolder> permissions;

	public void initAndPrepareBean() {
		HistoGroup group = new HistoGroup(new HistoSettings());
		group.getSettings().setAvailableViews(new ArrayList<View>());
		group.setAuthRole(AuthRole.ROLE_NONEAUTH);
		group.setPermissions(new HashSet<HistoPermissions>());
		
		if (initBean(group, false))
			prepareDialog();
	}

	public void initAndPrepareBean(HistoGroup group) {
		if (initBean(group, true))
			prepareDialog();
	}

	public boolean initBean(HistoGroup group, boolean initialize) {

		if (initialize) {
			try {
				setGroup(userDAO.initializeGroup(group, true));
			} catch (CustomDatabaseInconsistentVersionException e) {
				logger.debug("Version conflict, updating entity");
				setGroup(userDAO.getHistoGroup(group.getId(), true));
			}
		} else {
			setGroup(group);
		}

		setNewGroup(group.getId() == 0 ? true : false);

		setAllViews(
				new View[] { View.GUEST, View.USERLIST, View.WORKLIST_TASKS , View.WORKLIST_PATIENT,  View.WORKLIST_DIAGNOSIS, View.WORKLIST_RECEIPTLOG});

		setAvailableWorklistsToLoad(new ArrayList<WorklistSearchOption>());
		getAvailableWorklistsToLoad().add(WorklistSearchOption.DIAGNOSIS_LIST);
		getAvailableWorklistsToLoad().add(WorklistSearchOption.STAINING_LIST);
		getAvailableWorklistsToLoad().add(WorklistSearchOption.NOTIFICATION_LIST);
		getAvailableWorklistsToLoad().add(WorklistSearchOption.EMTY);

		setPermissions(group.getPermissions());

		for (View view : group.getSettings().getAvailableViews()) {
			System.out.println(view);
		}
		
		super.initBean(task, Dialog.SETTINGS_GROUP_EDIT);
		return true;
	}

	public void onChangePermission(PermissionHolder holder) {
		permissions.forEach((k, v) -> {
			if (v.getPermission().getParent() == holder.getPermission()) {
				v.setValue(holder.isValue());
			}
			;
		});
	}

	public void setPermissions(Set<HistoPermissions> groupPermissions) {
		permissions = new HashMap<String, PermissionHolder>();

		Set<HistoPermissions> groupPermissionsCopy = new HashSet<HistoPermissions>(groupPermissions);
		HistoPermissions[] permissionArr = HistoPermissions.values();

		for (int i = 0; i < permissionArr.length; i++) {
			PermissionHolder permissionIsSet = new PermissionHolder(false, permissionArr[i]);
			for (HistoPermissions histoPermission : groupPermissionsCopy) {
				if (permissionArr[i] == histoPermission) {
					permissionIsSet.setValue(true);
					break;
				}
			}

			permissions.put(permissionArr[i].name(), permissionIsSet);
		}
	}

	public Map<String, PermissionHolder> getPermissions() {
		return permissions;
	}

	public void saveGroup() {

		if (getGroup().getSettings().getDefaultView() == null) {
			if (getGroup().getSettings().getAvailableViews() == null
					&& getGroup().getSettings().getAvailableViews().size() > 0)
				getGroup().getSettings().setDefaultView(getGroup().getSettings().getAvailableViewsAsArray()[0]);
		}

		// settings permissions
		getGroup().getPermissions().clear();

		// adding/ readding permissions
		permissions.forEach((p, v) -> {
			if (v.isValue()) {
				getGroup().getPermissions().add(v.getPermission());
			}
		});

		try {
			if (getGroup().getId() == 0) {
				userDAO.save(getGroup(), resourceBundle.get("log.settings.group.new", getGroup()));
			} else {
				userDAO.save(getGroup(), resourceBundle.get("log.settings.group.edit", getGroup()));
			}

		} catch (Exception e) {
			e.printStackTrace();
			onDatabaseVersionConflict();
		}

	}

	@Getter
	@Setter
	public class PermissionHolder {

		private boolean value;
		private HistoPermissions permission;

		public PermissionHolder(boolean value, HistoPermissions permission) {
			this.value = value;
			this.permission = permission;
		}
	}
}
