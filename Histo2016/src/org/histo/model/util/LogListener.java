package org.histo.model.util;

import javax.sound.midi.MidiDevice.Info;

import org.apache.log4j.Logger;
import org.hibernate.envers.RevisionListener;
import org.histo.config.SecurityContextHolderUtil;
import org.histo.model.HistoUser;
import org.histo.model.Log;
import org.histo.model.interfaces.LogInfo;
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

	private static Logger logger = Logger.getLogger("org.histo");

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
		try {
			// setting user who has changed the object
			if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof HistoUser) {
				HistoUser user = (HistoUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
				revEntity.setUserAcc(user);
			}

			// sets the log info if present, gets the info from the
			// securityContext, Workaround to pass additional data to this
			// listener
			Object logInfo = SecurityContextHolderUtil.getObjectFromSecurityContext(LOG_KEY_INFO);
			if (logInfo != null && logInfo instanceof LogInfo) {
				LogInfo info = (LogInfo) logInfo;
				logger.debug("Loginfo found: " + info.getInfo() + " for patient: "
						+ (info.getPatient() == null ? "none" : info.getPatient().toString() ));
				revEntity.setLogString(info.getInfo());
				revEntity.setPatient(info.getPatient());
				SecurityContextHolderUtil.setObjectToSecurityContext(LOG_KEY_INFO, null);
			}
		} catch (NullPointerException e) {
			logger.error("Nullpointer expection",e);
		}
	}

}
