package org.histo.model.interfaces;

import org.histo.config.enums.Dialog;

public interface DeleteAble {
	
	/**
	 * Returns the name of the object
	 * 
	 * @return
	 */
	public String getTextIdentifier();

	/**
	 * Returns an dialog for deleting the object
	 * 
	 * @return
	 */
	public Dialog getArchiveDialog();
}
