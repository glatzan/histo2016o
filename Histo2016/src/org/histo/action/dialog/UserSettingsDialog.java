package org.histo.action.dialog;

import java.util.ArrayList;
import java.util.List;

import org.histo.action.UserHandlerAction;
import org.histo.action.handler.GlobalSettings;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.View;
import org.histo.config.enums.WorklistSearchOption;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.model.favouriteList.FavouriteList;
import org.histo.model.favouriteList.FavouritePermissionsGroup;
import org.histo.model.favouriteList.FavouritePermissionsUser;
import org.histo.model.user.HistoUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Component
@Scope(value = "session")
@Getter
@Setter
public class UserSettingsDialog extends AbstractTabDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserHandlerAction userHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GlobalSettings globalSettings;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private FavouriteListDAO favouriteListDAO;

	private GeneralTab generalTab;
	private PrinterTab printTab;
	private FavouriteListTab favouriteListTab;

	private HistoUser user;

	public UserSettingsDialog() {
		generalTab = new GeneralTab();
		printTab = new PrinterTab();
		favouriteListTab = new FavouriteListTab();

		tabs = new AbstractTab[] { generalTab, printTab, favouriteListTab };
	}

	public void initAndPrepareBean() {
		if (initBean())
			prepareDialog();
	}

	public boolean initBean() {
		super.initBean(null, Dialog.USER_SETTINGS);

		setUser(userHandlerAction.getCurrentUser());

		for (int i = 0; i < tabs.length; i++) {
			tabs[i].initTab();
		}

		if (activeIndex >= 0 && activeIndex < getTabs().length) {
			onTabChange(null);
		}

		return true;
	}

	public void saveUserSettings() {
		logger.debug("Saving user Settings");

		try {
			genericDAO.save(getUser());
			userHandlerAction.updateSelectedPrinters();
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	public void resetUserSettings() {
		logger.debug("Resetting user Settings");
		genericDAO.refresh(getUser());
	}

	@Getter
	@Setter
	public class GeneralTab extends AbstractTab {

		private List<View> availableViews;

		private List<WorklistSearchOption> availableWorklistsToLoad;

		public GeneralTab() {
			setTabName("GeneralTab");
			setName("dialog.userSettings.general");
			setViewID("generalTab");
			setCenterInclude("include/general.xhtml");
		}

		public boolean initTab() {

			setAvailableViews(
					new ArrayList<View>(userHandlerAction.getCurrentUser().getSettings().getAvailableViews()));

			setAvailableWorklistsToLoad(new ArrayList<WorklistSearchOption>());
			getAvailableWorklistsToLoad().add(WorklistSearchOption.DIAGNOSIS_LIST);
			getAvailableWorklistsToLoad().add(WorklistSearchOption.STAINING_LIST);
			getAvailableWorklistsToLoad().add(WorklistSearchOption.NOTIFICATION_LIST);
			getAvailableWorklistsToLoad().add(WorklistSearchOption.EMTY);

			return true;
		}

		@Override
		public void updateData() {
		}

	}

	@Getter
	@Setter
	public class PrinterTab extends AbstractTab {

		public PrinterTab() {
			setTabName("PrinterTab");
			setName("dialog.userSettings.printer");
			setViewID("printerTab");
			setCenterInclude("include/printer.xhtml");
		}

		@Override
		public void updateData() {
		}

	}

	@Getter
	@Setter
	public class FavouriteListTab extends AbstractTab {

		private List<FavouriteListContainer> containers;

		public FavouriteListTab() {
			setTabName("FavouriteListTab");
			setName("dialog.userSettings.favouriteLists");
			setViewID("favouriteTab");
			setCenterInclude("include/favouriteLists.xhtml");
		}

		@Override
		public void updateData() {
			List<FavouriteList> list = favouriteListDAO.getFavouriteListsForUser(user);

			for (FavouriteList favouriteList : list) {
				favouriteListDAO.initFavouriteList(favouriteList, true);
			}
			
			containers = new ArrayList<FavouriteListContainer>();

			for (FavouriteList favouriteList : list) {
				containers.add(new FavouriteListContainer(favouriteList));
			}
		}

		@Getter
		@Setter
		public class FavouriteListContainer {

			private FavouriteList favouriteList;

			private boolean owner;
			private boolean global;

			private boolean userPermission;
			private boolean groupPermission;

			private boolean editable;
			private boolean readable;
			private boolean admin;
			
			private int type;

			public FavouriteListContainer(FavouriteList favouriteList) {
				this.favouriteList = favouriteList;

				if (favouriteList.getOwner().equals(userHandlerAction.getCurrentUser())) {
					this.editable = true;
					this.readable = true;
					this.admin = true;
					
					type = 1;
				}

				// only updating if not already true
				for (FavouritePermissionsUser user : favouriteList.getUsers()) {
					if (user.getUser().equals(userHandlerAction.getCurrentUser())) {
						this.admin = this.admin ? true : user.isAdmin();
						this.editable = this.editable ? true : user.isEditable();
						this.readable = this.readable ? true : user.isReadable();
						this.userPermission = true;
						
						type = type > 0 ? type : 2;
						break;
					}
				}

				// only updating if not already true
				for (FavouritePermissionsGroup group : favouriteList.getGroups()) {
					if (group.getGroup().equals(userHandlerAction.getCurrentUser().getGroup())) {
						this.admin = this.admin ? true : group.isAdmin();
						this.editable = this.editable ? true : group.isEditable();
						this.readable = this.readable ? true : group.isReadable();
						this.groupPermission = true;
						
						type = type > 0 ? type : 3;
						break;
					}
				}
			}
		}
	}

}
