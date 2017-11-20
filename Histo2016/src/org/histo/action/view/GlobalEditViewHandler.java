package org.histo.action.view;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;

import org.apache.log4j.Logger;
import org.histo.action.UserHandlerAction;
import org.histo.config.enums.View;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.ui.StainingTableChooser;
import org.histo.ui.menu.MenuGenerator;
import org.histo.ui.task.TaskStatus;
import org.primefaces.model.menu.MenuModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Component
@Scope(value = "session")
@Getter
@Setter
public class GlobalEditViewHandler {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserHandlerAction userHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	@Lazy
	private WorklistViewHandlerAction worklistViewHandlerAction;

	// ************************ Navigation ************************
	/**
	 * View options, dynamically generated depending on the users role
	 */
	private List<View> navigationPages;
	// ************************ Patient ************************

	/**
	 * Currently selectedTask
	 */
	private Patient selectedPatient;

	/**
	 * Currently selectedTask
	 */
	private Task selectedTask;

	/**
	 * MenuModel for task editing
	 */
	private MenuModel taskMenuModel;

	/**
	 * Method called on postconstruct. Initializes all important variables.
	 */
	@PostConstruct
	public void init() {
		logger.debug("PostConstruct Init program");

		setNavigationPages(new ArrayList<View>());

		// settings views
		setNavigationPages(new ArrayList<View>(userHandlerAction.getCurrentUser().getSettings().getAvailableViews()));

		updateDataOfTask(false);

	}

	public void updateDataOfTask(boolean updateFavouriteLists) {
		if (selectedTask != null)
			selectedTask.generateTaskStatus();
		
		setTaskMenuModel((new MenuGenerator()).generateEditMenu(selectedPatient, selectedTask));
	}

	public void addTaskToFavouriteList(Task task, long id) {
		try {
			favouriteListDAO.addTaskToList(task, id);
			updateDataOfTask(true);
		} catch (CustomDatabaseInconsistentVersionException e) {
			worklistViewHandlerAction.onVersionConflictPatient(task.getPatient(), true);
		}
	}

	public void removeTaskFromFavouriteList(Task task, long id) {
		try {
			favouriteListDAO.removeTaskFromList(task, id);
			updateDataOfTask(true);
		} catch (CustomDatabaseInconsistentVersionException e) {
			worklistViewHandlerAction.onVersionConflictPatient(task.getPatient(), true);
		}
	}
}
