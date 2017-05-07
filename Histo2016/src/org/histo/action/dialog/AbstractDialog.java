package org.histo.action.dialog;

import org.apache.log4j.Logger;
import org.histo.action.MainHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.dao.GenericDAO;
import org.histo.model.patient.Task;
import org.springframework.beans.factory.annotation.Autowired;

public  abstract class AbstractDialog {

	protected static Logger logger = Logger.getLogger("org.histo");
	
	@Autowired
	protected GenericDAO genericDAO;
	
	@Autowired
	protected MainHandlerAction mainHandlerAction;
	
	protected Task task;
	
	protected Dialog dilaog;
	
	public void initAndPrepareBean(Task task, Dialog dialog){
		initBean(task,dialog);
		prepareDialog();
	}
	
	public void initBean(Task task, Dialog dialog){
		setTask(task);
		setDilaog(dialog);
	}
	
	/**
	 * Method for displaying the associated dialog.
	 */
	public void prepareDialog(){
		mainHandlerAction.showDialog(dilaog);
	}
	
	/**
	 * Method for hiding the associated dialog.
	 */
	public void hideDialog(){
		mainHandlerAction.hideDialog(dilaog);
	}

	// ************************ Getter/Setter ************************
	public Task getTask() {
		return task;
	}

	public void setTask(Task task) {
		this.task = task;
	}

	public Dialog getDilaog() {
		return dilaog;
	}

	public void setDilaog(Dialog dilaog) {
		this.dilaog = dilaog;
	}
	// ************************ Getter/Setter ************************
	
}
