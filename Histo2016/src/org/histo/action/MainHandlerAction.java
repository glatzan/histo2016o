package org.histo.action;

import java.net.URL;
import java.net.URLClassLoader;
// TODO urgent: status und info dialog
// TODO check patient fetch from jason form clinik -> zu viele patienten
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;

import org.histo.config.HistoSettings;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.DateFormat;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.Role;
import org.histo.config.enums.View;
import org.histo.dao.GenericDAO;
import org.histo.model.interfaces.ArchivAble;
import org.histo.model.interfaces.Parent;
import org.histo.model.patient.Block;
import org.histo.model.patient.Diagnosis;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.util.TaskUtil;
import org.histo.util.TimeUtil;
import org.primefaces.context.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
<<<<<<< HEAD
import org.springframework.web.bind.annotation.ResponseStatus;

import java.net.URLClassLoader;
// TODO urgent: status und info dialog
// TODO check patient fetch from jason form clinik -> zu viele patienten
// TODO List für Patienten übersicht
=======

>>>>>>> branch 'master' of https://github.com/blub4ever/histo2016o

//http://stackoverflow.com/questions/6149919/is-it-safe-to-start-a-new-thread-in-a-jsf-managed-bean
// ++++: Diagnose page
// TODO: Biobank
// TODO: favouriten
// ++++ edit page patient external
// TODO: Logout warn
// TODO: status display (in navigation)
// TODO: log in settings rework
// ++++: Edit external patient from menu bar
// ++++: Priorisierung
// TODO: prevent overwriting of data from clinic physicians if changed
// TODO: change event of histological record in diagnosis page, located in helphandler action
// ++++: Re-Diagnosis reduce options 
// TODO: fullName propertie of physician move to person

@Component
@Scope(value = "session")
public class MainHandlerAction {

	@Autowired
	private UserHandlerAction userHandlerAction;

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private ResourceBundle resourceBundle;

	/**
	 * View options, dynamically generated depending on the users role
	 */
	private List<View> navigationPages;

	/**
	 * The current view of the user
	 */
	private View currentView;

	/**
	 * 
	 */
	private String queueDialog;

	/********************************************************
	 * Archive able
	 ********************************************************/

	/**
	 * Object to archive
	 */
	private ArchivAble toArchive;

	/**
	 * the toArchive object will be archived if true
	 */
	private boolean archived;

	/********************************************************
	 * Archive able
	 ********************************************************/

	/**
	 * Dynamic Texts which are used rarely are stroed here.
	 */
	private HistoSettings settings;

	
	/**
	 * Method called on postconstruct. Initializes all important variables.
	 */
	@PostConstruct
	public void init() {

		navigationPages = new ArrayList<View>();
		navigationPages.add(View.USERLIST);

		setSettings(HistoSettings.factory());

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

		System.out.println("------------------------------------");

		ClassLoader cl = ClassLoader.getSystemClassLoader();

		URL[] urls = ((URLClassLoader) cl).getURLs();

		for (URL url : urls) {
			System.out.println(url.getFile());
		}

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

	public void showQueueDialog() {
		System.out.println("----> Shwing Quwuw " + getQueueDialog());
		if (getQueueDialog() != null) {
			RequestContext.getCurrentInstance().execute("showDialogFromFrontendByBean('" + getQueueDialog() + "')");
			setQueueDialog(null);
		}
	}

	/**
	 * Shows a dialog using the primefaces dialog framework
	 * 
	 * @param dialog
	 */
	public void showDialog(Dialog dialog) {
		HashMap<String, Object> options = new HashMap<String, Object>();

		if (dialog.getWidth() != 0) {
			options.put("width", dialog.getWidth());
			options.put("contentWidth", dialog.getWidth());
		} else
			options.put("width", "auto");

		if (dialog.getHeight() != 0) {
			options.put("contentHeight", dialog.getHeight());
			options.put("height", dialog.getHeight());
		} else
			options.put("height", "auto");

		if (dialog.isUseOptions()) {
			options.put("resizable", dialog.isResizeable());
			options.put("draggable", dialog.isDraggable());
			options.put("modal", dialog.isModal());
		}

		options.put("closable", false);

		if (dialog.getHeader() != null)
			options.put("headerElement", "dialogForm:header");

		System.out.println("----> Showing " + dialog);
		RequestContext.getCurrentInstance().openDialog(dialog.getPath(), options, null);
	}

	/**
	 * Closes a dialog using primefaces dialog framework
	 * 
	 * @param dialog
	 */
	public void hideDialog(Dialog dialog) {
		RequestContext.getCurrentInstance().closeDialog(dialog);
		System.out.println("----> Hiding " + dialog);
	}

	/*
	 * ************************** Dialog ****************************
	 */

	/*
	 * ************************** Time ****************************
	 */

	/**
	 * Takes a long timestamp and returns a formatted date in standard format.
	 * 
	 * @param date
	 * @return
	 */
	public String date(long date) {
		return date(new Date(date), DateFormat.GERMAN_DATE);
	}

	/**
	 * Takes a date and returns a formatted date in standard format.
	 * 
	 * @param date
	 * @return
	 */
	public String date(Date date) {
		return date(date, DateFormat.GERMAN_DATE);
	}

	/**
	 * Takes a long timestamp and a format string and returns a formatted date.
	 * 
	 * @param date
	 * @return
	 */
	public String date(long date, String format) {
		return date(new Date(date), format);
	}

	/**
	 * Takes a dateFormat an returns a formated string.
	 * 
	 * @param date
	 * @param format
	 * @return
	 */
	public String date(long date, DateFormat format) {
		return date(new Date(date), format.getDateFormat());
	}

	/**
	 * Takes a dateFormat an returns a formated string.
	 * 
	 * @param date
	 * @param format
	 * @return
	 */
	public String date(Date date, DateFormat format) {
		return date(date, format.getDateFormat());
	}

	/**
	 * Takes a date and a format string and returns a formatted date.
	 * 
	 * @param date
	 * @return
	 */
	public String date(Date date, String format) {
		return TimeUtil.formatDate(date, format);
	}

	/*
	 * ************************** Time ****************************
	 */
	/********************************************************
	 * Archive
	 ********************************************************/

	/**
	 * Shows a Dialog for deleting (archiving) the sample/task/bock/image
	 * 
	 * @param sample
	 * @param archived
	 */
	public void prepareArchiveObject(ArchivAble archive, boolean archived) {
		setArchived(archived);
		setToArchive(archive);
		// if no dialog is provieded the object will be archived immediately
		if (archive.getArchiveDialog() == null)
			archiveObject(archive, archived);
		else
			showDialog(archive.getArchiveDialog());
	}

	/**
	 * Archives a Object implementing Parent.
	 * 
	 * @param task
	 * @param archiveAble
	 * @param archived
	 */
	public void archiveObject(ArchivAble archive, boolean archived) {

		archive.setArchived(archived);

		String logString = "log.error";

		if (archive instanceof Slide)
			logString = resourceBundle.get("log.patient.task.sample.blok.slide.archived",
					((Slide) archive).getParent().getParent().getParent().getTaskID(),
					((Slide) archive).getParent().getParent().getSampleID(), ((Slide) archive).getParent().getBlockID(),
					((Slide) archive).getSlideID());
		else if (archive instanceof Diagnosis)
			logString = resourceBundle.get("log.patient.task.sample.diagnosis.archived",
					((Diagnosis) archive).getParent().getParent().getTaskID(),
					((Diagnosis) archive).getParent().getSampleID(), ((Diagnosis) archive).getName());
		else if (archive instanceof Block)
			logString = resourceBundle.get("log.patient.task.sample.blok.archived",
					((Block) archive).getParent().getParent().getTaskID(), ((Block) archive).getParent().getSampleID(),
					((Block) archive).getBlockID());
		else if (archive instanceof Sample)
			logString = resourceBundle.get("log.patient.task.sample.archived",
					((Sample) archive).getParent().getTaskID(), ((Sample) archive).getSampleID());
		else if (archive instanceof Task)
			logString = resourceBundle.get("log.patient.task.archived", ((Task) archive).getTaskID());

		Patient patient = null;

		if (archive instanceof Parent<?>) {
			patient = ((Parent<?>) archive).getPatient();
			// update the gui list for displaying in the receiptlog
			TaskUtil.generateSlideGuiList(patient.getSelectedTask());
		}

		genericDAO.save(archive, logString, patient);

		hideArchiveObjectDialog();
	}

	/**
	 * Hides the Dialog for achieving an object
	 */
	public void hideArchiveObjectDialog() {
		hideDialog(getToArchive().getArchiveDialog());
	}

	/********************************************************
	 * Archive
	 ********************************************************/

	/**
	 * Takes a object to save and an resourcesString with optional wildcards.
	 * This method will replace wildcard recursively ("log.test",
	 * "{'log.hallo', 'wuuu'}", "replace other"). If an object is associated
	 * with a patient but does not implement Parent the patient object can be
	 * passed separately.
	 * 
	 * @param toSave
	 * @param resourcesKey
	 * @param detailedInfoParams
	 */
	public void saveChangedData(Object toSave, Patient patient, String logInfo) {

		if (patient != null) {
			genericDAO.save(toSave, logInfo, patient);
		} else
			genericDAO.save(toSave, logInfo);

		System.out.println("saving data " + logInfo);
	}

	/**
	 * Takes a object to save and an resourcesString with optional wildcards.
	 * This method will replace wildcard recursively ("log.test",
	 * "{'log.hallo', 'wuuu'}", "replace other")
	 * 
	 * @param toSave
	 * @param resourcesKey
	 * @param detailedInfoParams
	 */
	public void saveChangedData(Object toSave, String resourcesKey) {

		if (toSave instanceof Parent) {
			saveChangedData(toSave, ((Parent<?>) toSave).getPatient(), resourcesKey);
		} else
			saveChangedData(toSave, null, resourcesKey);
	}

	/**
	 * Replaces wildcards in resoucesString.
	 * 
	 * @param baseStr
	 * @param arr
	 * @return
	 */
	public String replaceWildcard(String baseStr, Object... arr) {
		return resourceBundle.get(baseStr, arr);
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

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

	public String getQueueDialog() {
		return queueDialog;
	}

	public void setQueueDialog(String queueDialog) {
		System.out.println("----> Setting Quwuw " + queueDialog);
		this.queueDialog = queueDialog;
	}

	public ArchivAble getToArchive() {
		return toArchive;
	}

	public void setToArchive(ArchivAble toArchive) {
		this.toArchive = toArchive;
	}

	public boolean isArchived() {
		return archived;
	}

	public void setArchived(boolean archived) {
		this.archived = archived;
	}

	public HistoSettings getSettings() {
		return settings;
	}

	public void setSettings(HistoSettings settings) {
		this.settings = settings;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
