package org.histo.action.dialog.diagnosis;

import java.util.ArrayList;
import java.util.List;

import org.histo.action.WorklistHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.handler.TaskManipulationHandler;
import org.histo.config.enums.DiagnosisRevisionType;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.PatientDao;
import org.histo.dao.TaskDAO;
import org.histo.model.patient.DiagnosisRevision;
import org.histo.model.patient.Task;
import org.histo.ui.RevisionHolder;
import org.histo.util.StreamUtils;
import org.histo.util.TaskUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class DiagnosisRevisionDialog extends AbstractDialog {

	@Autowired
	private TaskDAO taskDAO;

	@Autowired
	private WorklistHandlerAction worklistHandlerAction;

	@Autowired
	private TaskManipulationHandler taskManipulationHandler;

	@Autowired
	private PatientDao patientDao;

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
			logger.debug("!! Version inconsistent with Database updating");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistHandlerAction.updatePatientInCurrentWorklist(task.getPatient());
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
						taskManipulationHandler.addDiagnosisRevision(revisionHolder.getRevision().getParent(),
								revisionHolder.getRevision());
					} else {
						// update revision
						patientDao.savePatientAssociatedDataFailSave(revisionHolder.getRevision(),
								"log.patient.task.diagnosisContainer.diagnosisRevision.update",
								revisionHolder.getRevision().toString());
					}
				}
			}
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	// ************************ Getter/Setter ************************
	public DiagnosisRevisionType getNewRevisionType() {
		return newRevisionType;
	}

	public void setNewRevisionType(DiagnosisRevisionType newRevisionType) {
		this.newRevisionType = newRevisionType;
	}

	public DiagnosisRevisionType[] getSelectableRevisionTypes() {
		return selectableRevisionTypes;
	}

	public void setSelectableRevisionTypes(DiagnosisRevisionType[] selectableRevisionTypes) {
		this.selectableRevisionTypes = selectableRevisionTypes;
	}

	public List<RevisionHolder> getRevisionList() {
		return revisionList;
	}

	public void setRevisionList(List<RevisionHolder> revisionList) {
		this.revisionList = revisionList;
	}

}