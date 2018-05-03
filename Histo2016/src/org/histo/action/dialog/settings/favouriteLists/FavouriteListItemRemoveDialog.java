package org.histo.action.dialog.settings.favouriteLists;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.histo.action.dialog.AbstractDialog;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.model.favouriteList.FavouriteList;
import org.histo.model.favouriteList.FavouriteListItem;
import org.histo.model.favouriteList.FavouritePermissions;
import org.histo.model.favouriteList.FavouritePermissionsGroup;
import org.histo.model.favouriteList.FavouritePermissionsUser;
import org.histo.model.patient.Task;
import org.histo.ui.FavouriteListContainer;
import org.histo.ui.transformer.DefaultTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Component
@Scope(value = "session")
@Setter
@Getter
public class FavouriteListItemRemoveDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private FavouriteListDAO favouriteListDAO;

	private FavouriteList favouriteList;

	private String commentary;

	public void initAndPrepareBean(FavouriteList favouriteList, Task task) {
		if (initBean(favouriteList, task))
			prepareDialog();
	}

	public boolean initBean(FavouriteList favouriteList, Task task) {

		this.favouriteList = favouriteList;
		this.task = task;

		this.commentary = favouriteList.getDumpCommentary();

		super.initBean(task, Dialog.FAVOURITE_LIST_ITEM_REMOVE);
		return true;

	}

	@Transactional
	public void removeTaskFromList() {
		favouriteListDAO.removeTaskFromList(task, favouriteList.getId());
	}
	
	@Transactional
	public void moveTaskToList() {
		favouriteListDAO.removeTaskFromList(task, favouriteList.getId());
		favouriteListDAO.addTaskToList(task, favouriteList.getDumpList().getId());
	}

}
