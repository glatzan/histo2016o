package org.histo.action.dialog.settings.favouriteLists;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.GlobalEditViewHandler;
import org.histo.config.enums.DateFormat;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.HistoDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.model.favouriteList.FavouriteList;
import org.histo.model.favouriteList.FavouriteListItem;
import org.histo.model.favouriteList.FavouritePermissions;
import org.histo.model.favouriteList.FavouritePermissionsGroup;
import org.histo.model.favouriteList.FavouritePermissionsUser;
import org.histo.model.patient.Task;
import org.histo.template.Template;
import org.histo.ui.FavouriteListContainer;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.util.HistoUtil;
import org.histo.util.TimeUtil;
import org.primefaces.PrimeFaces;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
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

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	@Lazy
	private GlobalEditViewHandler globalEditViewHandler;

	private FavouriteList favouriteList;

	private String commentary;

	public void initAndPrepareBean(Task task, Long favouriteListID) {
		if (initBean(task, favouriteListID))
			prepareDialog();
	}

	public void initAndPrepareBean(FavouriteList favouriteList, Task task) {
		if (initBean(favouriteList, task))
			prepareDialog();
	}

	public boolean initBean(Task task, Long favouriteListID) {
		FavouriteList list = favouriteListDAO.getFavouriteList(favouriteListID, false, false, true);
		return initBean(list, task);
	}

	public boolean initBean(FavouriteList favouriteList, Task task) {

		this.favouriteList = favouriteList;
		this.task = task;

		if (favouriteList.getDumpCommentary() != null) {
			Template.initVelocity();

			/* create a context and add data */
			VelocityContext context = new VelocityContext();

			context.put("date", TimeUtil.formatDate(new Date(), DateFormat.GERMAN_DATE.getDateFormat()));
			context.put("time", TimeUtil.formatDate(new Date(), DateFormat.TIME.getDateFormat()));
			context.put("oldList", favouriteList.getName());

			/* now render the template into a StringWriter */
			StringWriter writer = new StringWriter();

			Velocity.evaluate(context, writer, "", favouriteList.getDumpCommentary());

			this.commentary = writer.toString();
		} else
			this.commentary = "";

		super.initBean(task, Dialog.FAVOURITE_LIST_ITEM_REMOVE);
		return true;

	}

	@Transactional
	public void removeTaskFromList() {
		favouriteListDAO.removeReattachedTaskFromList(task, favouriteList.getId());
		mainHandlerAction.sendGrowlMessagesAsResource("growl.favouriteList.removed", "growl.favouriteList.removed.text",
				new Object[] { task.getTaskID(), favouriteList.getName() });
	}

	@Transactional
	public void moveTaskToList() {
		favouriteListDAO.moveReattachedTaskToList(favouriteList, favouriteList.getDumpList(), task, getCommentary());
		mainHandlerAction.sendGrowlMessagesAsResource("growl.favouriteList.move", "growl.favouriteList.move.text",
				new Object[] { task.getTaskID(), favouriteList.getDumpList().getName() });
	}

}
