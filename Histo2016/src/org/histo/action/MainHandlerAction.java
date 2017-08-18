package org.histo.action;

import java.io.IOException;
// TODO urgent: status und info dialog
// TODO check patient fetch from jason form clinik -> zu viele patienten
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.histo.action.handler.SettingsHandler;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.DateFormat;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.View;
import org.histo.dao.GenericDAO;
import org.histo.model.interfaces.PatientRollbackAble;
import org.histo.model.transitory.PredefinedRoleSettings;
import org.histo.util.TimeUtil;
import org.primefaces.context.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@Scope(value = "session")
public class MainHandlerAction {

	public static FacesContext test;

	private static Logger logger = Logger.getLogger("org.histo");
	@Autowired
	private CommonDataHandlerAction commonDataHandlerAction;

	@Autowired
	@Lazy
	private UserHandlerAction userHandlerAction;

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	private SettingsHandler settingsHandler;

	/********************************************************
	 * Navigation
	 ********************************************************/

	@Getter
	@Setter
	private String queueDialog;

	@Getter
	@Setter
	private List<FacesMessage> queueGrowlMessages;


	/**
	 * Method called on postconstruct. Initializes all important variables.
	 */
	@PostConstruct
	public void init() {
		logger.debug("PostConstruct Init program");

		commonDataHandlerAction.setNavigationPages(new ArrayList<View>());

		settingsHandler.initBean();

		PredefinedRoleSettings roleSetting = settingsHandler
				.getRoleSettingsForRole(userHandlerAction.getCurrentUser().getRole());

		commonDataHandlerAction.setNavigationPages(roleSetting.getAvailableViews());

		setQueueGrowlMessages(new ArrayList<FacesMessage>());
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
				.redirect(SettingsHandler.HISTO_BASE_URL + SettingsHandler.HISTO_LOGIN_PAGE);
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

	/********************************************************
	 * Dialog
	 ********************************************************/

	public void processQueues() {
		showQueueGrowlMessage();
		showQueueDialog();
	}

	public void showQueueDialog() {
		logger.trace("Showing Dialog from queue called");
		if (getQueueDialog() != null) {
			logger.debug("Showing Dialog from queue: " + getQueueDialog());
			RequestContext.getCurrentInstance().execute("clickButtonFromBean('" + getQueueDialog() + "')");
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

		RequestContext.getCurrentInstance().openDialog(dialog.getPath(), options, null);

		logger.debug("Showing Dialog: " + dialog);
	}

	/**
	 * Closes a dialog using primefaces dialog framework
	 * 
	 * @param dialog
	 */
	public void hideDialog(Dialog dialog) {
		logger.debug("Hiding Dialog: " + dialog);
		RequestContext.getCurrentInstance().closeDialog(null);
	}

	/********************************************************
	 * Dialog
	 ********************************************************/
	public void sendGrowlMessages(String headline, String message) {
		sendGrowlMessages(headline, message, FacesMessage.SEVERITY_INFO);
	}

	public void sendGrowlMessages(String headline, String message, FacesMessage.Severity servertiy) {
		FacesContext context = FacesContext.getCurrentInstance();
		context.addMessage("globalgrowl", new FacesMessage(servertiy, headline, message));
		logger.debug("Growl Messagen (" + servertiy + "): " + headline + " " + message);
	}

	public void showQueueGrowlMessage() {

		for (FacesMessage facesMessage : getQueueGrowlMessages()) {
			FacesContext context = FacesContext.getCurrentInstance();
			context.addMessage("globalgrowl", facesMessage);
		}

		getQueueGrowlMessages().clear();

	}

	public void addQueueGrowlMessage(String headline, String message) {
		addQueueGrowlMessage(headline, message, FacesMessage.SEVERITY_INFO);
	}

	public void addQueueGrowlMessage(String headline, String message, FacesMessage.Severity servertiy) {
		getQueueGrowlMessages().add(new FacesMessage(servertiy, headline, message));
	}

	/********************************************************
	 * Date
	 ********************************************************/

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

	/********************************************************
	 * Date
	 ********************************************************/
}
