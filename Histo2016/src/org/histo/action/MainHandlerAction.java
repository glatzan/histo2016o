package org.histo.action;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.histo.config.enums.Dialog;
import org.histo.config.enums.Display;
import org.histo.config.enums.Pages;
import org.histo.config.enums.Role;
import org.histo.config.enums.Worklist;
import org.histo.model.patient.Patient;
import org.histo.util.ResourceBundle;
import org.histo.util.SearchOptions;
import org.primefaces.context.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class MainHandlerAction {

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	private UserHandlerAction userHandlerAction;

	/**
	 * View options, dynamically generated depending on the users role
	 */
	private List<Pages> navigationPages;

	/**
	 * The current view of the user
	 */
	private Pages currentView;
	
	/**
	 * Method called on postconstruct. Initializes all important variables.
	 */
	@PostConstruct
	public void init() {
		navigationPages = new ArrayList<Pages>();
		navigationPages.add(Pages.USERLIST);

		if (userHandlerAction.currentUserHasRoleOrHigher(Role.MTA)) {
			navigationPages.add(Pages.WORKLIST_PATIENT);
			navigationPages.add(Pages.WORKLIST_RECEITLOG);
			navigationPages.add(Pages.WORKLIST_DIAGNOSIS);
		}
		
		// setting the current view depending on the users role
		if (userHandlerAction.currentUserHasRole(Role.GUEST))
			// guest need to be unlocked first
			setCurrentView(Pages.GUEST);
		else if (userHandlerAction.currentUserHasRole(Role.SCIENTIST))
			// no names are displayed
			setCurrentView(Pages.SCIENTIST);
		else if (userHandlerAction.currentUserHasRoleOrHigher(Role.USER)) {
			// if a default view is selected for the user
			if (userHandlerAction.getCurrentUser().getDefaultView() != null)
				setCurrentView(userHandlerAction.getCurrentUser().getDefaultView());

			// normal work environment
			setCurrentView(Pages.WORKLIST_PATIENT);
		} else
			setCurrentView(Pages.GUEST);
	}

	/*
	 * ************************** Navigation ****************************
	 */
	
	/**
	 * Method is called for chaning the current view with an p:selectOneMenu (e.g. worklist/header.xthml)
	 * @param pages
	 * @return
	 */
	public String goToNavigation(Pages pages){
		setCurrentView(pages);
		return pages.getPath();
	}
	/*
	 * ************************** Navigation ****************************
	 */
	
	/*
	 * ************************** Dialog ****************************
	 */
	
	/**
	 * Shows a dialog using the primefaces dialog framework
	 * @param dilalog
	 */
	public void showDialog(Dialog dilalog) {
		HashMap<String, Object> options = new HashMap<String, Object>();

		if (dilalog.getWidth() != 0) {
			options.put("width", dilalog.getWidth());
			options.put("contentWidth", dilalog.getWidth());
		} else
			options.put("width", "auto");

		if (dilalog.getHeight() != 0) {
			options.put("contentHeight", dilalog.getHeight());
			options.put("height", dilalog.getHeight());
		} else
			options.put("height", "auto");

		if (dilalog.isUseOptions()) {
			options.put("resizable", dilalog.isResizeable());
			options.put("draggable", dilalog.isDraggable());
			options.put("modal", dilalog.isModal());
		}
		
		RequestContext.getCurrentInstance().openDialog(dilalog.getPath(), options, null);
	}

	/**
	 * Closes a dialog using primefaces dialog framework
	 * 
	 * @param dialog
	 */
	public void hideDialog(Dialog dialog) {
		RequestContext.getCurrentInstance().closeDialog(dialog.getPath());
	}
	
	/*
	 * ************************** Dialog ****************************
	 */
	
	/*
	 * ************************** Getters/Setters ****************************
	 */

	/**
	 * Returns the current active worklist.
	 * 
	 * @return
	 */
	public List<Pages> getNavigationPages() {
		return navigationPages;
	}

	public void setNavigationPages(List<Pages> navigationPages) {
		this.navigationPages = navigationPages;
	}

	/**
	 * The current view
	 * @return
	 */
	public Pages getCurrentView() {
		return currentView;
	}

	public void setCurrentView(Pages currentView) {
		this.currentView = currentView;
	}
	/*
	 * ************************** Getters/Setters ****************************
	 */
}
