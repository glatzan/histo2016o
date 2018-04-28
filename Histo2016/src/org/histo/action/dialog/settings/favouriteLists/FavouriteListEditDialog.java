package org.histo.action.dialog.settings.favouriteLists;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.histo.action.UserHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.model.dto.FavouriteListMenuItem;
import org.histo.model.favouriteList.FavouriteList;
import org.histo.model.favouriteList.FavouriteListItem;
import org.histo.model.favouriteList.FavouritePermissions;
import org.histo.model.favouriteList.FavouritePermissionsGroup;
import org.histo.model.favouriteList.FavouritePermissionsUser;
import org.histo.model.user.HistoGroup;
import org.histo.model.user.HistoUser;
import org.histo.ui.FavouriteListContainer;
import org.histo.ui.transformer.DefaultTransformer;
import org.primefaces.event.SelectEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Component
@Scope(value = "session")
@Setter
@Getter
public class FavouriteListEditDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ResourceBundle resourceBundle;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private FavouriteListDAO favouriteListDAO;

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

	private List<FavouritePermissions> toDeleteList;

	private boolean adminMode;

	private FavouriteListContainer userPermission;

	private List<FavouriteList> dumpLists;

	private DefaultTransformer<FavouriteList> dumpListTransformer;

	public void initAndPrepareBean() {
		initAndPrepareBean(false);
	}

	public void initAndPrepareBean(boolean adminMode) {
		FavouriteList favouriteList = new FavouriteList();
		favouriteList.setDefaultList(false);
		favouriteList.setUsers(new HashSet<FavouritePermissionsUser>());
		favouriteList.setGroups(new HashSet<FavouritePermissionsGroup>());
		favouriteList.setItems(new ArrayList<FavouriteListItem>());
		favouriteList.setOwner(userHandlerAction.getCurrentUser());

		if (initBean(favouriteList, false, false))
			prepareDialog();
	}

	public void initAndPrepareBean(FavouriteList favouriteList) {
		initAndPrepareBean(favouriteList, false);
	}

	public void initAndPrepareBean(FavouriteList favouriteList, boolean adminMode) {
		if (initBean(favouriteList, adminMode, true))
			prepareDialog();
	}

	public boolean initBean(FavouriteList favouriteList, boolean adminMode, boolean initialize) {

		if (initialize) {
			try {
				setFavouriteList(favouriteListDAO.initFavouriteList(favouriteList, true));
			} catch (CustomDatabaseInconsistentVersionException e) {
				logger.debug("Version conflict, updating entity");
				setFavouriteList(favouriteListDAO.getFavouriteList(favouriteList.getId(), true, true));
			}
		} else {
			setFavouriteList(favouriteList);
		}

		setNewFavouriteList(favouriteList.getId() == 0);

		setToDeleteList(new ArrayList<FavouritePermissions>());

		setAdminMode(adminMode);

		// getting lists for dumplist option, if admin mod all lists are available, if
		// user only the writeable lists are displayed
		if (adminMode) {
			dumpLists = favouriteListDAO.getAllFavouriteLists();
		} else {
			dumpLists = favouriteListDAO.getFavouriteListsForUser(userHandlerAction.getCurrentUser(), true, false);
		}

		setDumpListTransformer(new DefaultTransformer<FavouriteList>(dumpLists));
		
		setUserPermission(new FavouriteListContainer(favouriteList, userHandlerAction.getCurrentUser()));

		super.initBean(task, Dialog.SETTINGS_FAVOURITE_LIST_EDIT);
		return true;
	}

	public void onSelectAdmin(FavouritePermissions permission) {
		if (permission.isAdmin()) {
			permission.setEditable(true);
			permission.setReadable(true);
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
				permission.setFavouriteList(favouriteList);
				permission.setReadable(true);

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
				permission.setFavouriteList(favouriteList);
				permission.setReadable(true);

				if (favouriteList.isGlobalView())
					permission.setReadable(true);
			}
		}
	}

	public void removeEntityFromList(Set<? extends FavouritePermissions> list, FavouritePermissions toRemove) {
		if (toRemove.getId() != 0)
			toDeleteList.add(toRemove);

		list.remove(toRemove);
	}

	public void saveFavouriteList() {

		try {

			transactionTemplate.execute(new TransactionCallbackWithoutResult() {

				public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {
					if (!toDeleteList.isEmpty()) {
						for (FavouritePermissions favouritePermissions : toDeleteList) {
							favouriteListDAO.delete(favouritePermissions);
						}
					}

					if (getFavouriteList().getId() == 0) {
						favouriteListDAO.save(getFavouriteList(),
								resourceBundle.get("log.settings.favouriteList.new", getFavouriteList()));
					} else {
						favouriteListDAO.save(getFavouriteList(),
								resourceBundle.get("log.settings.favouriteList.edit", getFavouriteList()));
					}
				}
			});

		} catch (Exception e) {
			onDatabaseVersionConflict();
		}
	}

	public enum Mode {
		ADMIN, USER;
	}
}
