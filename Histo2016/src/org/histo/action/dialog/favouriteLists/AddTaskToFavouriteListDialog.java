package org.histo.action.dialog.favouriteLists;

import java.util.ArrayList;
import java.util.List;

import org.histo.action.UserHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.FavouriteList;
import org.histo.model.patient.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class AddTaskToFavouriteListDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskDAO taskDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserHandlerAction userHandlerAction;

	private List<FavouriteList> defaultFavouriteLists;

	private List<FavouriteList> userFavouriteLists;

	
	
	public void initAndPrepareBean(Task task) {
		if (initBean(task))
			prepareDialog();
	}

	public boolean initBean(Task task) {
		try {
			taskDAO.initializeTask(task, false);

			super.initBean(task, Dialog.FAVOURITE_LIST_ADD);

			// loading lists of current user
			setUserFavouriteLists(favouriteListDAO.getFavouriteListsOfUser(userHandlerAction.getCurrentUser()));
			
			// getting default lists
			defaultFavouriteLists = new ArrayList<FavouriteList>();

			List<FavouriteList> favouriteLists = favouriteListDAO
					.getAllFavouriteLists(PredefinedFavouriteList.getIdArr());

			for (FavouriteList favouriteList : favouriteLists) {
				if (!task.isListedInFavouriteList(favouriteList)) {
					defaultFavouriteLists.add(favouriteList);
				}
			}

			return true;
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.onVersionConflictTask(task, false);
			return false;
		}
	}
}
