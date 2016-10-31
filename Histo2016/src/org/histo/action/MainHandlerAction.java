package org.histo.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;

import org.histo.config.HistoSettings;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.Role;
import org.histo.config.enums.View;
import org.histo.dao.GenericDAO;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.model.util.TaskTree;
import org.histo.util.ResourceBundle;
import org.histo.util.TimeUtil;
import org.primefaces.context.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

//http://stackoverflow.com/questions/6149919/is-it-safe-to-start-a-new-thread-in-a-jsf-managed-bean
// TODO: Diagnose page
// TODO: Biobank
// TODO: favouriten
// ++++ edit page patient external
// TODO: Logout warn
// TODO: status display (in navigation)
// TODO: log in settings rework
// TODO: Edit external patient from menu bar
// ++++: Priorisierung
// TODO: prevent overwriting of data from clinic physicians if changed
// TODO: change event of histological record in diagnosis page, located in helphandler action
// TODO: Re-Diagnosis reduce options 
//<!-- Buttons -->
//<h:panelGrid columns="2">
//	<!-- Save and finalize buttons -->
//
//	<h:panelGrid styleClass="collapsedBorders" columns="1"
//		rendered="#{!diagnosis.finalized}">
//		<p:commandButton process="contentForm"
//			value="#{msg['body.receiptlog.tab.diangonsis.data.finalize']}"
//			icon="fa fa-fw fa-ban"
//			actionListener="#{diagnosisHandlerAction.prepareFinalizeDiagnosisDialog(diagnosis)}">
//			<p:ajax event="dialogReturn"
//				update="navigationForm contentForm" />
//		</p:commandButton>
//	</h:panelGrid>
//
//	<p:commandButton
//		value="#{msg['body.receiptlog.tab.diangonsis.data.unfinalize']}"
//		rendered="#{diagnosis.finalized}" process="contentForm"
//		actionListener="#{diagnosisHandlerAction.prepareUnfinalizeDiagnosisDialog(diagnosis)}">
//		<p:ajax event="dialogReturn"
//			update="navigationForm contentForm" />
//	</p:commandButton>
//	<h:panelGroup>
//		<p:commandLink id="diangosisLogInfo">
//			<i class="fa fa-fw fa-info-circle" />
//		</p:commandLink>
//	</h:panelGroup>
//</h:panelGrid>
//<div style="left: 0; position: fixed; top: 0;">
//<p:overlayPanel for="diangosisLogInfo"
//	showEvent="mouseover" hideEvent="mouseout"
//	styleClass="logOverlay">
//	<p:dataTable var="log" styleClass="logDatapannel"
//		value="#{helperHandlerAction.getRevisionList(diagnosis)}"
//		resizableColumns="false">
//		<p:column headerText="Datum" style="width:155px;">
//			<h:outputLabel value="#{log.timestampAsDate}"></h:outputLabel>
//		</p:column>
//		<p:column headerText="Benutzer" style="width:100px;">
//			<h:outputLabel value="#{log.userAcc.username}"></h:outputLabel>
//		</p:column>
//		<p:column headerText="Aktion">
//			<h:outputLabel value="#{log.logString}"></h:outputLabel>
//		</p:column>
//	</p:dataTable>
//</p:overlayPanel>
//</div>
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
	 * List of currently opened dialogs
	 */
	private ArrayList<Dialog> currentDialogs;

	/**
	 * Method called on postconstruct. Initializes all important variables.
	 */
	@PostConstruct
	public void init() {

		navigationPages = new ArrayList<View>();
		navigationPages.add(View.USERLIST);
		
		setCurrentDialogs(new ArrayList<Dialog>());

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
		getCurrentDialogs().add(dialog);
		RequestContext.getCurrentInstance().openDialog(dialog.getPath(), options, null);
	}

	/**
	 * Closes a dialog using primefaces dialog framework
	 * 
	 * @param dialog
	 */
	public void hideDialog(Dialog dialog) {
		getCurrentDialogs().remove(dialog);
		RequestContext.getCurrentInstance().closeDialog(dialog.getPath());
	}

	/**
	 * Closes all opened dialogs
	 */
	public void hideAllDialogs() {
		for (Dialog dialog : getCurrentDialogs()) {
			RequestContext.getCurrentInstance().closeDialog(dialog.getPath());
		}

		getCurrentDialogs().clear();
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
		return date(new Date(date), HistoSettings.STANDARD_DATEFORMAT_DAY_ONLY);
	}

	/**
	 * Takes a date and returns a formatted date in standard format.
	 * 
	 * @param date
	 * @return
	 */
	public String date(Date date) {
		return date(date, HistoSettings.STANDARD_DATEFORMAT_DAY_ONLY);
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

	/**
	 * Takes a object to save and an resourcesString with optional wildcards.
	 * This method will replace wildcard recursively ("log.test",
	 * "{'log.hallo', 'wuuu'}", "replace other"). If an object is associated
	 * with a patient but does not implement TaskTree the patient object can be
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

		if (toSave instanceof TaskTree) {
			saveChangedData(toSave, ((TaskTree<?>) toSave).getPatient(), resourcesKey);
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

	public ArrayList<Dialog> getCurrentDialogs() {
		return currentDialogs;
	}

	public void setCurrentDialogs(ArrayList<Dialog> currentDialogs) {
		this.currentDialogs = currentDialogs;
	}

	/*
	 * ************************** Getters/Setters ****************************
	 */
}
