package org.histo.action.dialog.settings.favouriteLists;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.histo.action.UserHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.View;
import org.histo.config.enums.WorklistSearchOption;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.SettingsDAO;
import org.histo.dao.UserDAO;
import org.histo.model.favouriteList.FavouriteList;
import org.histo.model.favouriteList.FavouriteListItem;
import org.histo.model.favouriteList.FavouritePermissionsGroup;
import org.histo.model.favouriteList.FavouritePermissionsUser;
import org.histo.model.transitory.PredefinedRoleSettings;
import org.histo.model.user.HistoGroup;
import org.histo.model.user.HistoSettings;
import org.histo.model.user.HistoUser;
import org.primefaces.event.SelectEvent;
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
public class FavouriteListEditDialog extends AbstractDialog {

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

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserHandlerAction userHandlerAction;

	private boolean newFavouriteList;

	private FavouriteList favouriteList;

	public void initAndPrepareBean() {
		FavouriteList favouriteList = new FavouriteList();
		favouriteList.setDefaultList(false);
		favouriteList.setUsers(new ArrayList<FavouritePermissionsUser>());
		favouriteList.setGroups(new ArrayList<FavouritePermissionsGroup>());
		favouriteList.setItems(new ArrayList<FavouriteListItem>());
		favouriteList.setOwner(userHandlerAction.getCurrentUser());

		if (initBean(favouriteList, false))
			prepareDialog();
	}

	public void initAndPrepareBean(FavouriteList favouriteList) {
		if (initBean(favouriteList, true))
			prepareDialog();
	}

	public boolean initBean(FavouriteList favouriteList, boolean initialize) {

		if (initialize) {
			// try {
			// setGroup(userDAO.initializeGroup(group, true));
			// } catch (CustomDatabaseInconsistentVersionException e) {
			// logger.debug("Version conflict, updating entity");
			// setGroup(userDAO.getHistoGroup(group.getId(), true));
			// }
		} else {
			setFavouriteList(favouriteList);
		}

		setNewFavouriteList(favouriteList.getId() == 0);

		super.initBean(task, Dialog.SETTINGS_FAVOURITE_LIST_EDIT);
		return true;
	}

	public void onSelectGlobalView() {
		if (favouriteList.isGlobalView()) {
			favouriteList.getUsers().stream().forEach(p -> p.setReadable(true));
			favouriteList.getGroups().stream().forEach(p -> p.setReadable(true));
		}
	}

	public void onReturnSelectOwner(SelectEvent event) {
		if (event.getObject() != null && event.getObject() instanceof HistoUser) {
			favouriteList.setOwner((HistoUser) event.getObject());
		}
	}

	public void onReturnSelectUser(SelectEvent event) {
		if (event.getObject() != null && event.getObject() instanceof HistoUser) {
			if (!favouriteList.getUsers().stream().anyMatch(p -> p.getUser().equals(event.getObject()))) {
				FavouritePermissionsUser permission = new FavouritePermissionsUser((HistoUser) event.getObject());
				favouriteList.getUsers().add(permission);

				if (favouriteList.isGlobalView())
					permission.setReadable(true);
			}
		}
	}

	public void onReturnSelectGroup(SelectEvent event) {
		if (event.getObject() != null && event.getObject() instanceof HistoGroup) {
			if (!favouriteList.getGroups().stream().anyMatch(p -> p.getGroup().equals(event.getObject()))) {
				FavouritePermissionsGroup permission = new FavouritePermissionsGroup((HistoGroup) event.getObject());

				favouriteList.getGroups().add(permission);

				if (favouriteList.isGlobalView())
					permission.setReadable(true);
			}
		}
	}

	public void removeEntityFromList(List<?> list, Object toRemove) {
		list.remove(toRemove);
	}

	public void saveGroup() {

		// if (getGroup().getSettings().getDefaultView() == null) {
		// if (getGroup().getSettings().getAvailableViews() == null
		// && getGroup().getSettings().getAvailableViews().size() > 0)
		// getGroup().getSettings().setDefaultView(getGroup().getSettings().getAvailableViewsAsArray()[0]);
		// }
		//
		// try {
		// if (getGroup().getId() == 0) {
		// userDAO.save(getGroup(), resourceBundle.get("log.settings.group.new",
		// getGroup()));
		// } else {
		// userDAO.save(getGroup(),
		// resourceBundle.get("log.settings.group.edit", getGroup()));
		// }
		//
		// } catch (Exception e) {
		// e.printStackTrace();
		// onDatabaseVersionConflict();
		// }

	}
}

// public void prepareNewFavouriteList() {
// FavouriteList newList = new FavouriteList();
// newList.setItems(new ArrayList<FavouriteListItem>());
// newList.setDefaultList(false);
// newList.setEditAble(true);
// newList.setGlobal(true);
// prepareEditFavouriteList(newList);
// }
//
// public void prepareEditFavouriteList(FavouriteList favouriteList) {
// setTmpFavouriteList(favouriteList);
// setPage(FavouriteListPage.EDIT);
// setNewFavouriteList(favouriteList.getId() == 0 ? true : false);
// updateData();
// }
//
// public void saveFavouriteList() {
// try {
// // saving new list
// if (getTmpFavouriteList().getId() == 0) {
// genericDAO.save(getTmpFavouriteList(), "log.settings.favouriteList.new",
// new Object[] { getTmpFavouriteList().toString() });
// } else {
// // updating old list
// genericDAO.save(getTmpFavouriteList(), "log.settings.favouriteList.edit",
// new Object[] { getTmpFavouriteList().toString() });
// }
//
// } catch (CustomDatabaseInconsistentVersionException e) {
// onDatabaseVersionConflict();
// }
// }
//
// public void discardFavouriteList() {
// setTmpFavouriteList(null);
// setPage(FavouriteListPage.LIST);
// updateData();
// }