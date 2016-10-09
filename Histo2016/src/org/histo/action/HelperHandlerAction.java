package org.histo.action;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;
import org.histo.config.HistoSettings;
import org.histo.dao.GenericDAO;
import org.histo.dao.LogDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.History;
import org.histo.model.Log;
import org.histo.model.HistoUser;
import org.histo.model.patient.Patient;
import org.histo.model.util.ArchivAble;
import org.histo.model.util.LogAble;
import org.histo.model.util.LogListContainer;
import org.histo.util.TimeUtil;
import org.primefaces.context.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;

@Controller
@Scope("session")
public class HelperHandlerAction implements Serializable {

	private static final long serialVersionUID = -4083599293687828502L;

	@Autowired
	private TaskDAO taskDAO;
	
	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private LogDAO logDAO;
	
	/**
	 * Log Overlaypanel calls the getRevsionList method several times. This is a
	 * Buffer to increase performance. It contains LogListcontainers with a
	 * timestamp. If the last log fetch is to old it will be updated.
	 */
	private ArrayList<LogListContainer> logTmpMempory = new ArrayList<LogListContainer>();

	/**
	 * Returns a list with all Log for a passed object implementing LogAble. This
	 * method uses a caching mechanism because the overlaypanel calls this
	 * method several times. Logs will be updated after some time atomically. 
	 * 
	 * @param logAble
	 * @return
	 */
	public List<Log> getRevisionList(LogAble logAble) {
		return getRevisionList(logAble, false);
	}
	
	/**
	 * Saves the given Object to the database
	 * 
	 * @param object
	 */
	public void saveObject(Object object) {
		genericDAO.save(object);
	}
	
	/**
	 * Returns a list with all Log for a passed object implementing LogAble. This
	 * method uses a caching mechanism because the overlaypanel calls this
	 * method several times. Logs will be updated after some time atomically.
	 * @param logAble
	 * @param igonreTimestamp
	 * @return
	 */
	public List<Log> getRevisionList(LogAble logAble, boolean igonreTimestamp) {
		int index = logTmpMempory.indexOf(new LogListContainer(logAble));

		if (index != -1) {
			LogListContainer logListContainer = logTmpMempory.get(index);
			if (igonreTimestamp || System.currentTimeMillis() - 60000 > logListContainer.getTimestampOfUpdate()) {
				logListContainer.setLogs((logDAO.getRevisions(logAble)));
				logListContainer.setTimestampOfUpdate(System.currentTimeMillis());
			}
			return logListContainer.getLogs();
		} else {
			LogListContainer newContainer = new LogListContainer(logAble);
			newContainer.setLogs((logDAO.getRevisions(logAble)));
			newContainer.setTimestampOfUpdate(System.currentTimeMillis());
			logTmpMempory.add(newContainer);
			return newContainer.getLogs();
		}

	}
	
	/**
	 * Forces an update of the revsion list.
	 * @param logAble
	 */
	public void updateRevision(LogAble logAble){
		getRevisionList(logAble, true);
	}

	/**
	 * Objects implementing archiveAble can be manipulated with this method.
	 * 
	 * @param archiveAble
	 * @param archived
	 */
	public void archiveObject(ArchivAble archiveAble, boolean archived) {
		archiveAble.setArchived(archived);
	}

	public void timeout() throws IOException {
		//showDialog(HistoSettings.dialog(HistoSettings.DIALOG_LOGOUT));
		FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
		FacesContext.getCurrentInstance().getExternalContext()
				.redirect(HistoSettings.HISTO_BASE_URL + HistoSettings.HISTO_LOGIN_PAGE);
	}

}