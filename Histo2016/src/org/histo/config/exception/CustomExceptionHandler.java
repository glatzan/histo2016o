package org.histo.config.exception;

import java.util.Iterator;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.application.NavigationHandler;
import javax.faces.context.ExceptionHandler;
import javax.faces.context.ExceptionHandlerWrapper;
import javax.faces.context.FacesContext;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * 	<!--  
		<factory>
			<exception-handler-factory>
				org.histo.config.exception.CustomExceptionHandlerFactory
			</exception-handler-factory>
		</factory>-->
		can be actived in faces config again if needed
		
		<mvc:interceptors>
		<bean id="webContentInterceptor"
			class="org.springframework.web.servlet.mvc.WebContentInterceptor">
			<property name="cacheSeconds" value="0" />
			<property name="useExpiresHeader" value="true" />
			<property name="useCacheControlHeader" value="true" />
			<property name="useCacheControlNoStore" value="true" />
		</bean>
	</mvc:interceptors>
	
	this is used instead in spring config
 * @author andi
 *
 */
public class CustomExceptionHandler extends ExceptionHandlerWrapper {
	private static final Logger log = Logger.getLogger(CustomExceptionHandler.class.getCanonicalName());
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
			Throwable t = context.getException();

			final FacesContext fc = FacesContext.getCurrentInstance();
			final Map<String, Object> requestMap = fc.getExternalContext().getRequestMap();
			final NavigationHandler nav = fc.getApplication().getNavigationHandler();

			// here you do what ever you want with exception
			try {

				// log error ?
				log.log(Level.ERROR, "Critical Exception!", t);

				// redirect error page
				requestMap.put("exceptionMessage", t.getMessage());
				nav.handleNavigation(fc, null, "/error");
				fc.renderResponse();

				// remove the comment below if you want to report the error in a
				// jsf error message
				// JsfUtil.addErrorMessage(t.getMessage());

			} finally {
				// remove it from queue
				i.remove();
			}
		}
		// parent hanle
		getWrapped().handle();
	}
}