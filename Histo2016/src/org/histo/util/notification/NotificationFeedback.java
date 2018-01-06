package org.histo.util.notification;

/**
 * Interface for returning feedback from notification process.
 * 
 * @author andi
 *
 */
public interface NotificationFeedback {

	/**
	 * Sets a resKey for resources manager with optional inserts
	 * 
	 * @param resKey
	 * @param string
	 */
	public void setFeedback(String resKey, String... string);

	/**
	 * Returns the current feedback string
	 * 
	 * @return
	 */
	public String getFeedback();

	/**
	 * Is called if one notification is completed
	 */
	public void progressStep();
}
