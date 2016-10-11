package org.histo.action;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.histo.config.HistoSettings;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.Display;
import org.histo.config.enums.View;
import org.histo.config.enums.Role;
import org.histo.config.enums.Worklist;
import org.histo.model.HistoUser;
import org.histo.model.patient.Patient;
import org.histo.model.transitory.SearchOptions;
import org.histo.util.ResourceBundle;
import org.histo.util.TimeUtil;
import org.primefaces.context.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

// TODO: Diagnose page
// TODO: Biobank
// TODO: favouriten
// ++++ edit page patient external
// TODO: Logout warn
// TODO: status display (in navigation)
// TODO: log in settings rework
// TODO: Edit external patient from menu bar
// TODO: Priorisierung

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
	private List<View> navigationPages;

	/**
	 * The current view of the user
	 */
	private View currentView;

	/**
	 * Method called on postconstruct. Initializes all important variables.
	 */
	@PostConstruct
	public void init() {
		navigationPages = new ArrayList<View>();
		navigationPages.add(View.USERLIST);

		if (userHandlerAction.currentUserHasRoleOrHigher(Role.MTA)) {
			navigationPages.add(View.WORKLIST_PATIENT);
			navigationPages.add(View.WORKLIST_RECEIPTLOG);
			navigationPages.add(View.WORKLIST_DIAGNOSIS);
		}

		// setting the current view depending on the users role
		if (userHandlerAction.currentUserHasRole(Role.GUEST))
			// guest need to be unlocked first
			setCurrentView(View.GUEST);
		else if (userHandlerAction.currentUserHasRole(Role.SCIENTIST))
			// no names are displayed
			setCurrentView(View.SCIENTIST);
		else if (userHandlerAction.currentUserHasRoleOrHigher(Role.USER)) {
			// if a default view is selected for the user
			if (userHandlerAction.getCurrentUser().getDefaultView() != null)
				setCurrentView(userHandlerAction.getCurrentUser().getDefaultView());

			// normal work environment
			setCurrentView(View.WORKLIST_PATIENT);
		} else
			setCurrentView(View.GUEST);
	}

	/*
	 * ************************** Navigation ****************************
	 */

	/**
	 * Method is called for chaning the current view with an p:selectOneMenu
	 * (e.g. worklist/header.xthml). If the view is a subview of a parent,
	 * return the parent url.
	 * 
	 * @param view
	 * @return
	 */
	public String goToNavigation(View view) {
		setCurrentView(view);
		return view.getParentView() == null ? view.getPath() : view.getParentView().getPath();
	}
	/*
	 * ************************** Navigation ****************************
	 */

	/*
	 * ************************** Dialog ****************************
	 */

	/**
	 * Shows a dialog using the primefaces dialog framework
	 * 
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
	 * ************************** Time ****************************
	 */

	/**
	 * Takes a long timestamp and returns a formatted date in standard format.
	 * @param date
	 * @return
	 */
	public String date(long date){
		return date(new Date(date), HistoSettings.STANDARD_DATEFORMAT_DAY_ONLY);
	}
	
	/**
	 * Takes a date and returns a formatted date in standard format.
	 * @param date
	 * @return
	 */
	public String date(Date date){
		return date(date, HistoSettings.STANDARD_DATEFORMAT_DAY_ONLY);
	}
	
	/**
	 * Takes a long timestamp and a format string and returns a formatted date.
	 * @param date
	 * @return
	 */
	public String date(long date, String format){
		return date(new Date(date),format);
	}
	
	/**
	 * Takes a date and a format string and returns a formatted date.
	 * @param date
	 * @return
	 */
	public String date(Date date, String format){
		return TimeUtil.formatDate(date, format);
	}
	
	/*
	 * ************************** Time ****************************
	 */
	
	
	/*
	 * ************************** Getters/Setters ****************************
	 */

	/**
	 * Returns the current active worklist.
	 * 
	 * @return
	 */
	public List<View> getNavigationPages() {
		return navigationPages;
	}

	public void setNavigationPages(List<View> navigationPages) {
		this.navigationPages = navigationPages;
	}

	/**
	 * The current view
	 * 
	 * @return
	 */
	public View getCurrentView() {
		return currentView;
	}

	public void setCurrentView(View currentView) {
		this.currentView = currentView;
	}
	/*
	 * ************************** Getters/Setters ****************************
	 */
}
