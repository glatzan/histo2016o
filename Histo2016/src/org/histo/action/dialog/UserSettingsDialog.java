package org.histo.action.dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.histo.action.UserHandlerAction;
import org.histo.action.handler.GlobalSettings;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.View;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.model.favouriteList.FavouriteList;
import org.histo.model.user.HistoUser;
import org.histo.ui.FavouriteListContainer;
import org.histo.worklist.search.WorklistSimpleSearch.SimpleSearchOption;
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
			onTabChange();
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

		private List<SimpleSearchOption> availableWorklistsToLoad;

		public GeneralTab() {
			setTabName("GeneralTab");
			setName("dialog.userSettings.general");
			setViewID("generalTab");
			setCenterInclude("include/general.xhtml");
		}

		public boolean initTab() {

			setAvailableViews(
					new ArrayList<View>(userHandlerAction.getCurrentUser().getSettings().getAvailableViews()));

			setAvailableWorklistsToLoad(new ArrayList<SimpleSearchOption>());
			getAvailableWorklistsToLoad().add(SimpleSearchOption.DIAGNOSIS_LIST);
			getAvailableWorklistsToLoad().add(SimpleSearchOption.STAINING_LIST);
			getAvailableWorklistsToLoad().add(SimpleSearchOption.NOTIFICATION_LIST);
			getAvailableWorklistsToLoad().add(SimpleSearchOption.EMTY_LIST);

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
			long test = System.currentTimeMillis();
			logger.info("start - > 0");

			List<FavouriteList> list = favouriteListDAO.getFavouriteListsForUser(user, false, false, true, true, true);

			containers = list.stream().map(p -> new FavouriteListContainer(p, user)).collect(Collectors.toList());

			logger.info("end -> " + (System.currentTimeMillis() - test));
		}

	}

}
