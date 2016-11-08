package org.histo.action;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.event.ValueChangeListener;

import org.histo.config.HistoSettings;
import org.histo.dao.GenericDAO;
import org.histo.dao.LogDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.Log;
import org.histo.model.interfaces.ArchivAble;
import org.histo.model.interfaces.LogAble;
import org.histo.model.util.LogListContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

@Controller
@Scope("session")
public class HelperHandlerAction implements Serializable {

	private static final long serialVersionUID = -4083599293687828502L;

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

	  public void valueChanged() {
	      System.out.println("called: valueChanged()");
	   }
	   
	   public void valueChanged(ValueChangeEvent event) {
		   System.out.println(event.getOldValue());
		   System.out.println(event.getNewValue());
		   
	      System.out.println("called: valueChanged(ValueChangeEvent event)");
	   }
	   
	   public void valueChanged(ValueChangeListener listener) {
	      System.out.println("called: valueChanged(ValueChangeListener listener)");
	   }
}