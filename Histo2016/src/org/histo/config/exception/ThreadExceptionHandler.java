package org.histo.config.exception;

import org.apache.log4j.Logger;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;

public class ThreadExceptionHandler implements AsyncUncaughtExceptionHandler{

	private static Logger logger = Logger.getLogger("org.histo");

	public ThreadExceptionHandler() {
		System.out.println("------------------------------------------------------------------------");
	}
	
    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... obj) {
    	 System.out.println("Exception message - " + throwable.getMessage());
         System.out.println("Method name - " + method.getName());
         for (Object param : obj) {
             System.out.println("Parameter value - " + param);
         }
        logger.debug("Exception occurred::"+ throwable);
    }
    
}
