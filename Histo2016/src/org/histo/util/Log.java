package org.histo.util;

import java.util.Date;

import org.apache.log4j.Logger;
import org.histo.dao.GenericDAO;
import org.histo.model.History;
import org.histo.model.Patient;
import org.histo.model.UserAcc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Login Class for storing log-data
 * 
 * @author andi
 *
 */
@Component
public class Log {

	private Logger logger;

	@Autowired
	@Lazy
	private GenericDAO genericDAO;

	public Log() {
		logger = Logger.getLogger(Log.class.getName());
	}
	
	public Log(Class<?> classToLog) {
		logger = Logger.getLogger(classToLog.getName());
	}

	public void debug(String message) {
		log(message, null, History.LEVEL_DEBUG);
	}

	public void info(String message) {
		log(message, null, History.LEVEL_INFO);
	}

	public void info(String message, Patient patient) {
		log(message, patient, History.LEVEL_INFO);
	}

	public void log(String message, Patient patient, int level) {
		UserAcc user = (UserAcc) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

		History newHistroy = new History();
		newHistroy.setLevel(level);
		newHistroy.setUserAcc(user);
		newHistroy.setPatient(patient);
		newHistroy.setMessages(message);
		newHistroy.setDate(new Date(System.currentTimeMillis()));

		genericDAO.save(newHistroy);

		logger.info(user.getUsername() + " - " + message);
	}

	public void print(String message) {
		UserAcc user = (UserAcc) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		logger.info("#" + user.getUsername() + " - " + message);
	}
}
