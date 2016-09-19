package org.histo.model.util;

import java.util.List;

import org.histo.model.Log;

/**
 * Class for caching logs. Logs retrieval is very time consuming. This class can
 * store a log, the object of the log and an timestamp. While compared to an
 * other object the equals method checks if the logAble Object is equal not the
 * LogListcontainer.
 * 
 * @author glatza
 *
 */
public class LogListContainer {

	/**
	 * The Log Object
	 */
	private List<Log> logs;

	/**
	 * The Object which was logged
	 */
	private LogAble logAble;

	/**
	 * Timestamp of the log
	 */
	private long timestampOfUpdate;

	public LogListContainer(LogAble logAble) {
		this.logAble = logAble;
	}

	public List<Log> getLogs() {
		return logs;
	}

	public void setLogs(List<Log> logs) {
		this.logs = logs;
	}

	public LogAble getLogAble() {
		return logAble;
	}

	public void setLogAble(LogAble logAble) {
		this.logAble = logAble;
	}

	public long getTimestampOfUpdate() {
		return timestampOfUpdate;
	}

	public void setTimestampOfUpdate(long timestampOfUpdate) {
		this.timestampOfUpdate = timestampOfUpdate;
	}

	/**
	 * Overwritten equals method. Compares the logAble Object instead of the
	 * actual this object.
	 */
	@Override
	public boolean equals(Object obj) {
		LogAble toCompare = null;

		// checks if logAble oder LogListContaienr
		if (obj instanceof LogListContainer)
			toCompare = ((LogListContainer) obj).getLogAble();
		else if (obj instanceof LogAble)
			toCompare = (LogAble) obj;

		if (toCompare == null)
			return false;

		// compares class and it. Because of detachments the object identifier must no be the same.
		if (logAble.getClass().equals(toCompare.getClass()) && logAble.getId() == toCompare.getId())
			return true;
		return false;
	}

}
