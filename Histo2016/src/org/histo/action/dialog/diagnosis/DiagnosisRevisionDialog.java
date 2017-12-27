package org.histo.action.dialog.diagnosis;

import java.util.ArrayList;
import java.util.List;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.handler.TaskManipulationHandler;
import org.histo.action.view.GlobalEditViewHandler;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.DiagnosisRevisionType;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.patient.DiagnosisRevision;
import org.histo.model.patient.Task;
import org.histo.service.DiagnosisService;
import org.histo.ui.RevisionHolder;
import org.histo.util.StreamUtils;
import org.histo.util.TaskUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class DiagnosisRevisionDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskDAO taskDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskManipulationHandler taskManipulationHandler;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private FavouriteListDAO favouriteListDAO;
	
	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GlobalEditViewHandler globalEditViewHandler;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private DiagnosisService diagnosisService;
	/**
	 * Type of the new revision
	 */
	private DiagnosisRevisionType newRevisionType;

	/**
	 * Types of all available revisionTypes to create
	 */
	private DiagnosisRevisionType[] selectableRevisionTypes;

	/**
	 * List containing all old revisions and a new revision. The string contains
	 * the proposed new name
	 */
	private List<RevisionHolder> revisionList;

	public void initAndPrepareBean(Task task) {
		if (initBean(task))
			prepareDialog();
	}

	public boolean initBean(Task task) {
		try {
			taskDAO.initializeTask(task, false);
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.onVersionConflictTask(task, false);
		}

		super.initBean(task, Dialog.DIAGNOSIS_REVISION_CREATE);

		DiagnosisRevisionType[] types = new DiagnosisRevisionType[3];
		types[0] = DiagnosisRevisionType.DIAGNOSIS_REVISION;
		types[1] = DiagnosisRevisionType.DIAGNOSIS_CORRECTION;
		types[2] = DiagnosisRevisionType.DIAGNOSIS_COUNCIL;

		setSelectableRevisionTypes(types);
		setNewRevisionType(types[0]);

		updateDiagnosisRevisionType();

		return true;
	}

	/**
	 * Generating list of revisions and suggests name
	 */
	public void updateDiagnosisRevisionType() {
		setRevisionList(new ArrayList<RevisionHolder>());

		List<DiagnosisRevision> newList = new ArrayList<DiagnosisRevision>(
				getTask().getDiagnosisContainer().getDiagnosisRevisions());
		newList.add(new DiagnosisRevision(getTask().getDiagnosisContainer(), getNewRevisionType()));

		for (DiagnosisRevision revision : newList) {
			getRevisionList().add(
					new RevisionHolder(revision, TaskUtil.getDiagnosisRevisionName(newList, revision, resourceBundle)));
		}
	}

	public void copyOldNameFromDiagnosisRevision(DiagnosisRevision diagnosisRevision) {
		RevisionHolder result = getRevisionList().stream().filter(p -> p.getRevision() == diagnosisRevision)
				.collect(StreamUtils.singletonCollector());
		result.setName(diagnosisRevision.getName());
	}

	public void createDiagnosisRevision() {
		try {
			for (RevisionHolder revisionHolder : getRevisionList()) {
				if (!revisionHolder.getName().equals(revisionHolder.getRevision().getName())) {
					logger.debug("Updating revision name from " + revisionHolder.getRevision().getName() + " to "
							+ revisionHolder.getName());
					// updating name
					revisionHolder.getRevision().setName(revisionHolder.getName());

					// new revision
					if (revisionHolder.getRevision().getId() == 0) {
						diagnosisService.addDiagnosisRevision(revisionHolder.getRevision().getParent(),
								revisionHolder.getRevision());
					} else {
						// update revision
						genericDAO.savePatientData(revisionHolder.getRevision(),
								"log.patient.task.diagnosisContainer.diagnosisRevision.update",
								revisionHolder.getRevision().toString());
					}
				}
			}
			
			// adding the task to the diagnosis list
			favouriteListDAO.addTaskToList(task, PredefinedFavouriteList.DiagnosisList);
			globalEditViewHandler.updateDataOfTask(true, false, true, false);
			
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}
}
