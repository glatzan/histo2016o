package org.histo.action.exception;

import javax.faces.FacesException;
import javax.servlet.http.HttpServletRequest;

import org.histo.action.MainHandlerAction;
import org.histo.config.exception.CustomNotUniqueReqest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
@Order(1)
public class ExceptionHandlerAction {

	@Autowired
	private MainHandlerAction mainHandlerAction;

	@Order(1)
	@ExceptionHandler(Throwable.class)
	public String exception(Throwable e) {
		System.out.println("------------------------------");
		return "error";
	}

	@ExceptionHandler({ Exception.class, CustomNotUniqueReqest.class, FacesException.class })
	public ModelAndView handleError(HttpServletRequest req, Exception exception) throws Exception {
		System.out.println("------------------tut" + mainHandlerAction);
		return null;
	}

}
