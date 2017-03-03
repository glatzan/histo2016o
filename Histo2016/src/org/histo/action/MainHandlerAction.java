package org.histo.action;

import java.io.IOException;
import java.net.URL;
// http://stackoverflow.com/questions/3642919/javax-faces-application-viewexpiredexception-view-could-not-be-restored
import java.net.URLClassLoader;
// TODO urgent: status und info dialog
// TODO check patient fetch from jason form clinik -> zu viele patienten
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.histo.config.HistoSettings;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.DateFormat;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.config.enums.Role;
import org.histo.config.enums.View;
import org.histo.dao.GenericDAO;
import org.histo.model.interfaces.ArchivAble;
import org.histo.model.interfaces.LogAble;
import org.histo.model.interfaces.Parent;
import org.histo.model.interfaces.SaveAble;
import org.histo.model.patient.Block;
import org.histo.model.patient.Diagnosis;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.util.TaskUtil;
import org.histo.util.TimeUtil;
import org.primefaces.context.RequestContext;
import org.primefaces.model.UploadedFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class MainHandlerAction {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	@Lazy
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

		//TODO remove
		if (!getSettings().isOfflineMode()) {
			// setting preferred printer
			if (userHandlerAction.getCurrentUser().getPreferedPrinter() == null)
				userHandlerAction.getCurrentUser()
						.setPreferedPrinter(getSettings().getPrinterManager().getPrinters().get(0).getName());

			// setting label printer
			if (userHandlerAction.getCurrentUser().getPreferedLabelPritner() == null)
				userHandlerAction.getCurrentUser()
						.setPreferedLabelPritner(getSettings().getLabelPrinterManager().getPrinters().get(0).getName());
		}

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

	/********************************************************
	 * Session
	 ********************************************************/
	/**
	 * Destroys the current session
	 * 
	 * @throws IOException
	 */
	public void destroySession() throws IOException {
		logger.debug("Destroying Session");
		FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
		FacesContext.getCurrentInstance().getExternalContext()
				.redirect(HistoSettings.HISTO_BASE_URL + HistoSettings.HISTO_LOGIN_PAGE);
	}

	/**
	 * Refreshes the current Session
	 */
	public void keepSessionAlive() {
		logger.debug("Refreshing Session");
		FacesContext fc = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) fc.getExternalContext().getRequest();
		request.getSession();
	}

	/********************************************************
	 * Session
	 ********************************************************/

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
		logger.trace("Showing Dialog from queue called");
		if (getQueueDialog() != null) {
			logger.debug("Showing Dialog from queue: " + getQueueDialog());
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

		logger.debug("Showing Dialog: " + dialog);

		RequestContext.getCurrentInstance().openDialog(dialog.getPath(), options, null);
	}

	/**
	 * Closes a dialog using primefaces dialog framework
	 * 
	 * @param dialog
	 */
	public void hideDialog(Dialog dialog) {
		logger.debug("Hiding Dialog: " + dialog);
		RequestContext.getCurrentInstance().closeDialog(dialog);
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
	public void prepareDeleteObject(ArchivAble archive, boolean archived) {
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

		// TODO
		archive.setArchived(archived);

		String logString = "log.error";

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

	public void saveDataChange(SaveAble toSave, String resourcesKey, String... arr) {
		genericDAO.save(toSave, resourceBundle.get(resourcesKey, toSave.getLogPath(), arr), toSave.getPatient());
	}

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
