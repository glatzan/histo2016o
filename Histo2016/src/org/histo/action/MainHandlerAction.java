package org.histo.action;

import java.io.IOException;
// TODO urgent: status und info dialog
// TODO check patient fetch from jason form clinik -> zu viele patienten
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerRepository;
import org.histo.action.handler.GlobalSettings;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.DateFormat;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomUserNotificationExcepetion;
import org.histo.util.TimeUtil;
import org.primefaces.context.RequestContext;
import org.primefaces.push.EventBus;
import org.primefaces.push.EventBusFactory;
import org.primefaces.push.PushContext;
import org.primefaces.push.PushContextFactory;
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

	/**
	 * ID of the global info growl
	 */
	private static final String GLOBAL_GROWL_ID = "globalGrowl";

	@Autowired
	@Lazy
	private UserHandlerAction userHandlerAction;

	@Autowired
	protected ResourceBundle resourceBundle;

	/********************************************************
	 * Navigation
	 ********************************************************/

	@Getter
	@Setter
	private List<FacesMessage> queueGrowlMessages = new ArrayList<FacesMessage>();

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
				.redirect(GlobalSettings.HISTO_BASE_URL + GlobalSettings.HISTO_LOGIN_PAGE);
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

	public void test() {
		EventBus eventBus = EventBusFactory.getDefault().eventBus();
		System.out.println(eventBus);

		if (eventBus != null) {
			System.out.println("sending");
			// eventBus.publish("/chat", "hallo wie gehts");

			sendGrowlMessages("error", "test", FacesMessage.SEVERITY_ERROR);
			eventBus.publish("/chat", "update:growl");
		}
	}

	public void processQueues() {
		showQueueGrowlMessage();
	}

	public void sendGrowlMessages(String headline, String message, FacesMessage.Severity servertiy) {
		sendGrowlMessages(new FacesMessage(servertiy, headline, message));
	}

	public void sendGrowlMessages(FacesMessage message) {
		FacesContext context = FacesContext.getCurrentInstance();
		context.addMessage(GLOBAL_GROWL_ID, message);
		logger.debug("Growl (" + GLOBAL_GROWL_ID + ") Messagen (" + message.getSeverity() + "): " + message.getSummary()
				+ " " + message.getDetail());
	}

	public void showQueueGrowlMessage() {
		for (FacesMessage facesMessage : getQueueGrowlMessages()) {
			sendGrowlMessages(facesMessage);
		}

		if (!getQueueGrowlMessages().isEmpty()) {
			logger.debug("Growl update");
			getQueueGrowlMessages().clear();
			// RequestContext.getCurrentInstance().update("growlForm:globalgrowl");
		}

	}

	public void addQueueGrowlMessageAsResource(CustomUserNotificationExcepetion e) {
		addQueueGrowlMessageAsResource(e.getHeadline(), e.getMessage(), FacesMessage.SEVERITY_ERROR);
	}

	public void addQueueGrowlMessageAsResource(String headline, String message) {
		addQueueGrowlMessage(headline, message, FacesMessage.SEVERITY_INFO);
	}

	public void addQueueGrowlMessageAsResource(String headline, String message, FacesMessage.Severity servertiy) {
		addQueueGrowlMessage(resourceBundle.get(headline), resourceBundle.get(message), servertiy);
	}

	public void addQueueGrowlMessage(String headline, String message) {
		addQueueGrowlMessage(headline, message, FacesMessage.SEVERITY_INFO);
	}

	public void addQueueGrowlMessage(String headline, String message, FacesMessage.Severity servertiy) {
		getQueueGrowlMessages().add(new FacesMessage(servertiy, headline, message));
		// showQueueGrowlMessage();
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
