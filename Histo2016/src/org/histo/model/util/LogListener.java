package org.histo.model.util;

import org.hibernate.envers.RevisionListener;
import org.histo.model.Log;
import org.histo.model.UserAcc;
import org.histo.util.SecurityContextHolderUtil;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.security.core.context.SecurityContextHolder;

@Configurable
public class LogListener implements RevisionListener {

	/**
	 * Key for the securityContext, workaround in order to pass a string to this
	 * listener.
	 */
	public static final String LOG_KEY = "logInfo";

	/**
	 * Method is called if an object revision is saved to the database.
	 */
	@Override
	public void newRevision(Object revisionEntity) {
		Log exampleRevEntity = (Log) revisionEntity;

		// setting user who has changed the object
		UserAcc user = (UserAcc) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		exampleRevEntity.setUserAcc(user);

		// sets the log string if present, gets the string from the securityContext, Workaround
		Object logString = SecurityContextHolderUtil.getObjectFromSecurityContext(LOG_KEY);
		if(logString != null){
			exampleRevEntity.setLogString((String)logString);
			SecurityContextHolderUtil.setObjectToSecurityContext(LOG_KEY, null);
		}
	}

}
