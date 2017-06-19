package org.histo.action.dialog.task;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.time.DateUtils;
import org.histo.action.UserHandlerAction;
import org.histo.action.WorklistHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.dialog.PrintDialogHandler;
import org.histo.action.dialog.media.MediaDialog;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.CouncilState;
import org.histo.config.enums.DateFormat;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.enums.StaticList;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.PatientDao;
import org.histo.dao.PhysicianDAO;
import org.histo.dao.SettingsDAO;
import org.histo.dao.TaskDAO;
import org.histo.dao.UtilDAO;
import org.histo.model.Council;
import org.histo.model.ListItem;
import org.histo.model.PDFContainer;
import org.histo.model.Physician;
import org.histo.model.interfaces.HasDataList;
import org.histo.model.patient.Task;
import org.histo.ui.transformer.DefaultTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class CouncilDialogHandler extends AbstractDialog {

	@Autowired
	private PhysicianDAO physicianDAO;

	@Autowired
	private PatientDao patientDao;

	@Autowired
	private PrintDialogHandler printDialogHandler;

	@Autowired
	private TaskDAO taskDAO;

	@Autowired
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	private MediaDialog mediaDialog;

	@Autowired
	private SettingsDAO settingsDAO;

	/**
	 * Selected council from councilList
	 */
	private Council selectedCouncil;

	/**
	 * List of all councils of this tasks
	 */
	private List<Council> councilList;

	/**
	 * List of physician to address a council
	 */
	private List<Physician> physicianCouncilList;

	/**
	 * Transformer for phyisicianCouncilList
	 */
	private DefaultTransformer<Physician> physicianCouncilTransformer;

	/**
	 * List of physicians to sign the request
	 */
	private List<Physician> physicianSigantureList;

	/**
	 * Transformer for physicianSiangotureList
	 */
	private DefaultTransformer<Physician> physicianSigantureListTransformer;

	/**
	 * Contains all available attachments
	 */
	private List<ListItem> attachmentList;

	/**
	 * Initializes the bean and shows the council dialog
	 * 
	 * @param task
	 */
	public void initAndPrepareBean(Task task) {
		if (initBean(task))
			prepareDialog();
	}

	/**
	 * Initializes the bean and calles updatePhysicianLists at the end.
	 * 
	 * @param task
	 */
	public boolean initBean(Task task) {
		try {
			taskDAO.initializeTask(task, false);
			taskDAO.initializeCouncils(task);

			super.initBean(task, Dialog.COUNCIL);

			setCouncilList(new ArrayList<Council>(getTask().getCouncils()));

			// setting council as default
			if (getCouncilList().size() != 0) {
				setSelectedCouncil(getCouncilList().get(0));
			}

			updatePhysicianLists();

			setAttachmentList(settingsDAO.getAllStaticListItems(StaticList.COUNCIL_ATTACHMENT));

			return true;
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.replacePatientTaskInCurrentWorklistAndSetSelected(task);
			return false;
		}
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
		logger.info("Adding new council");
		setSelectedCouncil(new Council(getTask()));
		getSelectedCouncil().setDateOfRequest(System.currentTimeMillis());
		getSelectedCouncil().setName(generateName());
		getSelectedCouncil().setCouncilState(CouncilState.EditState);

		saveCouncilData();
	}

	public void onCouncilStateChange() {
		try {

			save();

			switch (getSelectedCouncil().getCouncilState()) {
			case EditState:
			case ValidetedState:
				logger.debug("EditState selected");
				// removing all fav lists
				removeListFromTask(PredefinedFavouriteList.CouncilLendingMTA,
						PredefinedFavouriteList.CouncilLendingSecretary, PredefinedFavouriteList.CouncilPending,
						PredefinedFavouriteList.CouncilCompleted);
				break;
			case LendingStateMTA:
			case LendingStateSecretary:
				logger.debug("LendingState selected");
				// removing pending and completed state
				removeListFromTask(PredefinedFavouriteList.CouncilPending, PredefinedFavouriteList.CouncilCompleted);
				favouriteListDAO.addTaskToList(getTask(),
						getSelectedCouncil().getCouncilState() == CouncilState.LendingStateMTA
								? PredefinedFavouriteList.CouncilLendingMTA
								: PredefinedFavouriteList.CouncilLendingSecretary);
				break;
			case PendingState:
				logger.debug("PendingState selected");
				// removing pending and completed state
				removeListFromTask(PredefinedFavouriteList.CouncilLendingMTA,
						PredefinedFavouriteList.CouncilLendingSecretary, PredefinedFavouriteList.CouncilCompleted);
				favouriteListDAO.addTaskToList(getTask(), PredefinedFavouriteList.CouncilPending);
				break;
			case CompletedState:
				logger.debug("CompletedState selected");
				// removing pending and completed state
				removeListFromTask(PredefinedFavouriteList.CouncilLendingMTA,
						PredefinedFavouriteList.CouncilLendingSecretary, PredefinedFavouriteList.CouncilPending);
				favouriteListDAO.addTaskToList(getTask(), PredefinedFavouriteList.CouncilCompleted);
				break;
			default:
				break;
			}
		} catch (CustomDatabaseInconsistentVersionException e) {
			onCouncilStateChange();
		}

	}

	public void removeListFromTask(PredefinedFavouriteList... predefinedFavouriteLists)
			throws CustomDatabaseInconsistentVersionException {

		for (PredefinedFavouriteList predefinedFavouriteList : predefinedFavouriteLists) {
			switch (predefinedFavouriteList) {
			case CouncilCompleted:
				if (!getTask().getCouncils().stream().anyMatch(p -> p.getCouncilState() == CouncilState.CompletedState))
					favouriteListDAO.removeTaskFromList(getTask(), predefinedFavouriteList);
				else
					logger.debug("Not removing from CouncilCompleted list, other councils are in this state");
				break;
			case CouncilPending:
				if (!getTask().getCouncils().stream().anyMatch(p -> p.getCouncilState() == CouncilState.PendingState))
					favouriteListDAO.removeTaskFromList(getTask(), predefinedFavouriteList);
				else
					logger.debug("Not removing from CouncilPending list, other councils are in this state");
				break;
			case CouncilLendingMTA:
			case CouncilLendingSecretary:
				if (!getTask().getCouncils().stream().anyMatch(p -> p.getCouncilState() == CouncilState.LendingStateMTA
						|| p.getCouncilState() == CouncilState.LendingStateSecretary))
					favouriteListDAO.removeTaskFromList(getTask(), predefinedFavouriteList);
				else
					logger.debug("Not removing from CouncilLendingMTA list, other councils are in this state");
				break;
			default:
				break;
			}
		}
	}

	public void onNameChange() {
		getSelectedCouncil().setName(generateName());
		saveCouncilData();
	}

	public String generateName() {
		StringBuffer str = new StringBuffer();

		// name
		if (getSelectedCouncil().getCouncilPhysician() != null)
			str.append(getSelectedCouncil().getCouncilPhysician().getPerson().getFullName());
		else
			str.append(resourceBundle.get("dialog.council.data.newCouncil"));

		str.append(" ");

		// adding date
		str.append(LocalDateTime.now().format(DateTimeFormatter.ofPattern(DateFormat.GERMAN_DATE.getDateFormat())));

		return str.toString();
	}

	public void saveCouncilData() {
		try {
			if (getSelectedCouncil() != null)
				save();
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	/**
	 * Saves a council. If id=0, the council is new and is added to the task, if
	 * id!=0 the council will only be saved.
	 * 
	 * @throws CustomDatabaseInconsistentVersionException
	 */
	private boolean save() throws CustomDatabaseInconsistentVersionException {
		// new
		if (getSelectedCouncil().getId() == 0) {
			logger.debug("Council Dialog: Creating new council");
			// TODO: Better loggin
			patientDao.savePatientAssociatedDataFailSave(getSelectedCouncil(), getTask(),
					"log.patient.task.council.create");

			task.getCouncils().add(getSelectedCouncil());

			patientDao.savePatientAssociatedDataFailSave(getTask(), "log.patient.task.council.attached",
					String.valueOf(getSelectedCouncil().getId()));

		} else {
			logger.debug("Council Dialog: Saving council");
			patientDao.savePatientAssociatedDataFailSave(getSelectedCouncil(), getTask(),
					"log.patient.task.council.update", String.valueOf(getSelectedCouncil().getId()));
		}

		// updating council list
		setCouncilList(new ArrayList<Council>(getTask().getCouncils()));
		return true;
	}

	/**
	 * hideDialog should be called first. This method opens a printer dialog, an
	 * let the gui click the button for opening the dialog. This is a workaround
	 * for opening other dialogs after closing the current dialog.
	 */
	public void printCouncilReport() {
		try {
			save();
			printDialogHandler.initBeanForCouncil(task, getSelectedCouncil());
			// workaround for showing and hiding two dialogues
			mainHandlerAction.setQueueDialog("#headerForm\\\\:printBtnShowOnly");

		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	public void showMediaSelectDialog() {
		showMediaSelectDialog(null);
	}

	public void showMediaSelectDialog(PDFContainer pdf) {
		try {
			// init dialog for patient and task
			mediaDialog.initBean(getTask().getPatient(), new HasDataList[] { getTask(), getTask().getPatient() }, pdf,
					true);

			// setting advance copy mode with move as true and target to task
			// and biobank
			mediaDialog.enableAutoCopyMode(new HasDataList[] { getTask(), getSelectedCouncil() }, true, true);

			// enabeling upload to task
			mediaDialog.enableUpload(new HasDataList[] { getTask() },
					new DocumentType[] { DocumentType.COUNCIL_REPLY });

			// setting info text
			mediaDialog.setActionDescription(
					resourceBundle.get("dialog.media.headline.info.council", getTask().getTaskID()));

			// show dialog
			mediaDialog.prepareDialog();
		} catch (CustomDatabaseInconsistentVersionException e) {
			// do nothing
			// TODO: infom user
		}
	}

	public void showMediaViewDialog(PDFContainer pdfContainer) {
		// init dialog for patient and task
		mediaDialog.initBean(getTask().getPatient(), getSelectedCouncil(), pdfContainer, false);

		// setting info text
		mediaDialog
				.setActionDescription(resourceBundle.get("dialog.media.headline.info.council", getTask().getTaskID()));

		// show dialog
		mediaDialog.prepareDialog();
	}

	// ************************ Getter/Setter ************************

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

	public Council getSelectedCouncil() {
		return selectedCouncil;
	}

	public void setSelectedCouncil(Council selectedCouncil) {
		this.selectedCouncil = selectedCouncil;
	}

	public List<ListItem> getAttachmentList() {
		return attachmentList;
	}

	public void setAttachmentList(List<ListItem> attachmentList) {
		this.attachmentList = attachmentList;
	}
}