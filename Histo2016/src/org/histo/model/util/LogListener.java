package org.histo.model.util;

import org.hibernate.envers.RevisionListener;
import org.histo.model.Log;
import org.histo.model.HistoUser;
import org.histo.model.patient.Patient;
import org.histo.util.SecurityContextHolderUtil;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Listener is called every Time an object is saved to database. Creates a log
 * entry.
 * 
 * @author glatza
 *
 */
@Configurable
public class LogListener implements RevisionListener {

	/**
	 * Key for the securityContext, workaround in order to pass a string to this
	 * listener.
	 */
	public static final String LOG_KEY_INFO = "logInfo";

	/**
	 * Method is called if an object revision is saved to the database.
	 */
	@Override
	public void newRevision(Object revisionEntity) {
		Log revEntity = (Log) revisionEntity;
		// setting user who has changed the object
		if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof HistoUser) {
			HistoUser user = (HistoUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			revEntity.setUserAcc(user);
		}

		// sets the log info if present, gets the info from the
		// securityContext, Workaround to pass additional data to this listener
		Object logInfo = SecurityContextHolderUtil.getObjectFromSecurityContext(LOG_KEY_INFO);
		if (logInfo != null && logInfo instanceof LogInfo) {
			LogInfo info = (LogInfo) logInfo;
			revEntity.setLogString(info.getInfo());
			revEntity.setPatient(info.getPatient());
			SecurityContextHolderUtil.setObjectToSecurityContext(LOG_KEY_INFO, null);
		}
	}

}
