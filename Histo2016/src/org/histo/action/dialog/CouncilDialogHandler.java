package org.histo.action.dialog;

import java.util.ArrayList;
import java.util.List;

import org.histo.action.UserHandlerAction;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.dao.PatientDao;
import org.histo.dao.PhysicianDAO;
import org.histo.dao.TaskDAO;
import org.histo.dao.UtilDAO;
import org.histo.model.Council;
import org.histo.model.Physician;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.ui.transformer.DefaultTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sun.corba.se.impl.javax.rmi.CORBA.Util;

@Component
@Scope(value = "session")
public class CouncilDialogHandler extends AbstractDialog {

	@Autowired
	private UtilDAO utilDAO;

	@Autowired
	private UserHandlerAction userHandlerAction;

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	private PhysicianDAO physicianDAO;

	@Autowired
	private PatientDao patientDao;

	@Autowired
	private PrintDialogHandler printDialogHandler;
	
	private Council council;

	private List<Council> councilList;

	private DefaultTransformer<Council> councilListTransformer;

	private List<Physician> physicianCouncilList;

	private DefaultTransformer<Physician> physicianCouncilTransformer;

	private List<Physician> physicianSigantureList;

	private DefaultTransformer<Physician> physicianSigantureListTransformer;

	/**
	 * Initializes the bean and shows the council dialog
	 * 
	 * @param task
	 */
	public void initAndPrepareBean(Task task) {
		initBean(task);
		prepareDialog();
	}

	/**
	 * Initializes the bean and calles updatePhysicianLists at the end.
	 * 
	 * @param task
	 */
	public void initBean(Task task) {
		super.initBean((Task)patientDao.savePatientAssociatedData(task), Dialog.COUNCIL);

		utilDAO.initializeDataList(task);
		utilDAO.initializeCouncilData(task);

		setCouncilList(new ArrayList<Council>(getTask().getCouncils()));

		// setting council as default
		if (getCouncilList().size() == 0) {
			logger.debug("Council Dialog: Creating new council");
			setCouncil(new Council());
			getCouncilList().add(getCouncil());
			getCouncil().setPhysicianRequestingCouncil(userHandlerAction.getCurrentUser().getPhysician());
		} else {
			// selected council is need for selectlist, temporary council is for
			// editing (new council can't be in task list)
			setCouncil(getCouncilList().get(0));
		}

		setCouncilListTransformer(new DefaultTransformer<Council>(getCouncilList()));

		updatePhysicianLists();
	}

	/**
	 * Renews the physician lists
	 */
	public void updatePhysicianLists() {
		// list of physicians which are the counselors
		setPhysicianCouncilList(physicianDAO.getPhysicians(ContactRole.CASE_CONFERENCE, false));
		setPhysicianCouncilTransformer(new DefaultTransformer<Physician>(getPhysicianCouncilList()));

		// list of physicians to sign the request
		setPhysicianSigantureList(physicianDAO.getPhysicians(ContactRole.SIGNATURE, false));
		setPhysicianSigantureListTransformer(new DefaultTransformer<Physician>(getPhysicianSigantureList()));
	}

	/**
	 * Creates a new council and saves it
	 */
	public void addNewCouncil() {
		Council newCouncil = new Council();
		setCouncil(newCouncil);
		saveCouncilData();
	}

	/**
	 * Saves a council. If id=0, the council is new and is added to the task, if
	 * id!=0 the council will only be saved.
	 */
	public void saveCouncilData() {
		// new
		if (council.getId() == 0) {
			council.setDateOfRequest(System.currentTimeMillis());
			logger.debug("Council Dialog: Creating new council");
			// TODO: Better loggin
			patientDao.savePatientAssociatedData(council, getTask(), "log.patient.task.council.create");

			task.getCouncils().add(council);

			genericDAO.saveDataChange(getTask(), "log.patient.task.council.attached", String.valueOf(council.getId()));

		} else {
			logger.debug("Council Dialog: Saving council");
			patientDao.savePatientAssociatedData(council, getTask(), "log.patient.task.council.update",
					String.valueOf(council.getId()));
		}

		// updating council list
		setCouncilList(new ArrayList<Council>(getTask().getCouncils()));
		setCouncilListTransformer(new DefaultTransformer<Council>(getCouncilList()));
	}

	/**
	 * hideDialog should be called first. This method opens a printer dialog, an
	 * let the gui click the button for opening the dialog. This is a workaround
	 * for opening other dialogs after closing the current dialog.
	 */
	public void printCouncilReport() {
		saveCouncilData();
		printDialogHandler.initBeanForCouncil(task, council);
		// workaround for showing and hiding two dialogues
		mainHandlerAction.setQueueDialog("#headerForm\\\\:printBtnShowOnly");

	}

	// ************************ Getter/Setter ************************
	public Council getCouncil() {
		return council;
	}

	public void setCouncil(Council council) {
		this.council = council;
	}

	public DefaultTransformer<Council> getCouncilListTransformer() {
		return councilListTransformer;
	}

	public void setCouncilListTransformer(DefaultTransformer<Council> councilListTransformer) {
		this.councilListTransformer = councilListTransformer;
	}

	public List<Physician> getPhysicianCouncilList() {
		return physicianCouncilList;
	}

	public void setPhysicianCouncilList(List<Physician> physicianCouncilList) {
		this.physicianCouncilList = physicianCouncilList;
	}

	public DefaultTransformer<Physician> getPhysicianCouncilTransformer() {
		return physicianCouncilTransformer;
	}

	public void setPhysicianCouncilTransformer(DefaultTransformer<Physician> physicianCouncilTransformer) {
		this.physicianCouncilTransformer = physicianCouncilTransformer;
	}

	public List<Physician> getPhysicianSigantureList() {
		return physicianSigantureList;
	}

	public void setPhysicianSigantureList(List<Physician> physicianSigantureList) {
		this.physicianSigantureList = physicianSigantureList;
	}

	public DefaultTransformer<Physician> getPhysicianSigantureListTransformer() {
		return physicianSigantureListTransformer;
	}

	public void setPhysicianSigantureListTransformer(DefaultTransformer<Physician> physicianSigantureListTransformer) {
		this.physicianSigantureListTransformer = physicianSigantureListTransformer;
	}

	public List<Council> getCouncilList() {
		return councilList;
	}

	public void setCouncilList(List<Council> councilList) {
		this.councilList = councilList;
	}
}
