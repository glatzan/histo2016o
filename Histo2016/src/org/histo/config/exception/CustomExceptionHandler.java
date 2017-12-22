package org.histo.config.exception;

import java.util.Iterator;
import java.util.Map;

import javax.el.ELException;
import javax.faces.FacesException;
import javax.faces.application.FacesMessage;
import javax.faces.application.NavigationHandler;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;

import org.apache.log4j.Logger;
import org.histo.action.MainHandlerAction;
import org.histo.action.UserHandlerAction;
import org.histo.action.handler.GlobalSettings;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.ResourceBundle;
import org.histo.model.interfaces.Parent;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.primefaces.context.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * <!-- <factory> <exception-handler-factory> org.histo.config.exception.
 * CustomExceptionHandlerFactory </exception-handler-factory> </factory>--> can
 * be actived in faces config again if needed
 * 
 * <mvc:interceptors> <bean id="webContentInterceptor" class=
 * "org.springframework.web.servlet.mvc.WebContentInterceptor">
 * <property name= "cacheSeconds" value="0" />
 * <property name="useExpiresHeader" value="true" />
 * <property name="useCacheControlHeader" value="true" />
 * <property name= "useCacheControlNoStore" value="true" /> </bean>
 * </mvc:interceptors>
 * 
 * this is used instead in spring config
 * 
 * @author andi
 *
 */
@Configurable
public class CustomExceptionHandler extends ExceptionHandlerWrapper {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	@Lazy
	private MainHandlerAction mainHandlerAction;

	@Autowired
	@Lazy
	private ResourceBundle resourceBundle;

	@Autowired
	@Lazy
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	@Lazy
	private GlobalSettings globalSettings;

	@Autowired
	@Lazy
	private UserHandlerAction userHandlerAction;

	private ExceptionHandler wrapped;

	CustomExceptionHandler(ExceptionHandler exception) {
		this.wrapped = exception;
	}

	@Override
	public ExceptionHandler getWrapped() {
		return wrapped;
	}

	@Override
	public void handle() throws FacesException {

		final Iterator<ExceptionQueuedEvent> i = getUnhandledExceptionQueuedEvents().iterator();
		while (i.hasNext()) {
			ExceptionQueuedEvent event = i.next();
			ExceptionQueuedEventContext context = (ExceptionQueuedEventContext) event.getSource();

			// get the exception from context
			Throwable cause = context.getException();

			final FacesContext fc = FacesContext.getCurrentInstance();
			final Map<String, Object> requestMap = fc.getExternalContext().getRequestMap();
			final NavigationHandler nav = fc.getApplication().getNavigationHandler();

			logger.debug("Global exeption handler - " + cause);

			// getting root excepetion
			while (cause instanceof FacesException || cause instanceof ELException) {
				if (cause instanceof FacesException)
					cause = ((FacesException) cause).getCause();
				else
					cause = ((ELException) cause).getCause();
			}

			boolean hanled = true;

			logger.debug("Global exeption handler - " + cause);

			if (cause != null) {

				if (cause instanceof CustomNotUniqueReqest) {
					logger.debug("Not Unique Reqest Error");
					RequestContext.getCurrentInstance().closeDialog(null);
					mainHandlerAction.addQueueGrowlMessage("Fehler!", "Doppelte Anfrage", FacesMessage.SEVERITY_ERROR);
				} else if (cause instanceof CustomDatabaseInconsistentVersionException) {

					logger.debug("Database Version Conflict");

					if (((CustomDatabaseInconsistentVersionException) cause).getOldVersion() instanceof Patient) {
						logger.debug("Version Error, replacing Patient");
						worklistViewHandlerAction.onVersionConflictPatient(
								((Patient) ((CustomDatabaseInconsistentVersionException) cause).getOldVersion()));
					} else if (((CustomDatabaseInconsistentVersionException) cause).getOldVersion() instanceof Task) {
						logger.debug("Version Error, replacing task");
						worklistViewHandlerAction.onVersionConflictTask(
								((Task) ((CustomDatabaseInconsistentVersionException) cause).getOldVersion()));
					} else if (((CustomDatabaseInconsistentVersionException) cause)
							.getOldVersion() instanceof Parent<?>) {
						logger.debug("Version Error, replacing parent -> task");
						worklistViewHandlerAction.onVersionConflictTask(
								((Parent<?>) ((CustomDatabaseInconsistentVersionException) cause).getOldVersion())
										.getTask());
					} else {
						logger.debug("Version Error,"
								+ ((CustomDatabaseInconsistentVersionException) cause).getOldVersion().getClass());
					}

					mainHandlerAction.addQueueGrowlMessage(resourceBundle.get("growl.version.error"),
							resourceBundle.get("growl.version.error.text"));

					RequestContext.getCurrentInstance()
							.execute("clickButtonFromBean('#headerForm\\\\:updateAllContent')");

					System.out.println("Versionfehler!");

					// TODO implement
				} else if (cause instanceof AbortProcessingException) {
					logger.debug("Error aboring all actions!");
				} else if (cause instanceof BadCredentialsException) {
					hanled = false;
				} else {
					logger.debug("Other exception!");
					cause.printStackTrace();
				}

//				ErrorMail mail = new ErrorMail();
//				mail.prepareTemplate(userHandlerAction.getCurrentUser(), "Ehandler " + cause.getMessage(),
//						new Date(System.currentTimeMillis()));
//				mail.fillTemplate();
//				globalSettings.getMailHandler().sendErrorMail(mail);
			}

			if (hanled)
				i.remove();
		}
		// parent hanle
		getWrapped().handle();
	}
}