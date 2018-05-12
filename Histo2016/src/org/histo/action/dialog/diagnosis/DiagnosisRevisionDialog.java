package org.histo.action.dialog.diagnosis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.GlobalEditViewHandler;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.DiagnosisRevisionType;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.exception.HistoDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.patient.DiagnosisRevision;
import org.histo.model.patient.Task;
import org.histo.service.DiagnosisService;
import org.histo.ui.RevisionHolder;
import org.histo.util.StreamUtils;
import org.histo.util.TaskUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Component
@Scope(value = "session")
@Setter
@Getter
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
	 * List containing all old revisions and a new revision. The string contains the
	 * proposed new name
	 */
	private List<RevisionHolder> revisionList;

	/**
	 * If true the dialog will only allow renaming
	 */
	private boolean newRevisions;

	/**
	 * If true the names will be autogenerated
	 */
	private boolean generateNames;

	public void initAndPrepareBean(Task task) {
		if (initBean(task, true))
			prepareDialog();
	}

	public void initAndPrepareBean(Task task, boolean newRevisions) {
		if (initBean(task, newRevisions))
			prepareDialog();
	}

	/**
	 * If rename is true no new diagnosis revision will be created
	 * 
	 * @param task
	 * @param rename
	 * @return
	 */
	public boolean initBean(Task task, boolean newRevisions) {
		try {
			taskDAO.initializeTask(task, false);
		} catch (HistoDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.replaceTaskInCurrentWorklist(task, false);
		}

		super.initBean(task, Dialog.DIAGNOSIS_REVISION_CREATE);

		this.newRevisions = newRevisions;

		DiagnosisRevisionType[] types = new DiagnosisRevisionType[3];
		types[0] = DiagnosisRevisionType.DIAGNOSIS_REVISION;
		types[1] = DiagnosisRevisionType.DIAGNOSIS_CORRECTION;
		types[2] = DiagnosisRevisionType.DIAGNOSIS_COUNCIL;

		setSelectableRevisionTypes(types);
		setNewRevisionType(types[0]);

		setRevisionList(new ArrayList<RevisionHolder>());

		updateDiagnosisRevisionList();

		if (newRevisions)
			addNewDiagnosisRevision();

		return true;
	}

	/**
	 * Updates the revision list, will remove or add revision from the task, will not
	 * remove new tasks
	 */
	public void updateDiagnosisRevisionList() {

		List<RevisionHolder> removeRevision = new ArrayList<RevisionHolder>(getRevisionList());

		// adding new revisions to list
		int i = 0;
		outer: for (DiagnosisRevision diagnosisRevision : task.getDiagnosisRevisions()) {
			for (RevisionHolder revisionHolder : removeRevision) {
				if (diagnosisRevision.getId() == revisionHolder.getRevision().getId()) {
					removeRevision.remove(revisionHolder);
					continue outer;
				}
			}

			// new revision
			getRevisionList().add(i, new RevisionHolder(diagnosisRevision, diagnosisRevision.getName()));
			i++;
		}

		// removing new diagnoses from delete list
		Iterator<RevisionHolder> iter = removeRevision.iterator();
		while (iter.hasNext()) {
			RevisionHolder tmp = iter.next();
			if (tmp.getRevision().getId() == 0)
				iter.remove();
		}

		// removing not existing revisions
		getRevisionList().removeAll(removeRevision);
	}

	/**
	 * Generating list of revisions and suggests name if generateNames is true
	 */
	public void updateDiagnosisRevisionName() {
		for (RevisionHolder revision : getRevisionList()) {
			revision.setName(generateNames ? TaskUtil.getDiagnosisRevisionName(
					getRevisionList().stream().map(p -> p.getRevision()).collect(Collectors.toList()),
					revision.getRevision(), resourceBundle) : revision.getName());
		}
	}

	/**
	 * Adds a new diagnosis revision
	 */
	public void addNewDiagnosisRevision() {
		getRevisionList().add(new RevisionHolder(new DiagnosisRevision(getTask(), getNewRevisionType()), ""));
		this.generateNames = true;
		updateDiagnosisRevisionName();
	}

	/**
	 * Removes a new diagnosis revision holder
	 * 
	 * @param holder
	 */
	public void removeNewDiagnosisRevision(RevisionHolder holder) {
		getRevisionList().remove(holder);
	}

	/**
	 * Copies the original name as the new name
	 * 
	 * @param diagnosisRevision
	 */
	public void copyOldNameFromDiagnosisRevision(DiagnosisRevision diagnosisRevision) {
		RevisionHolder result = getRevisionList().stream().filter(p -> p.getRevision() == diagnosisRevision)
				.collect(StreamUtils.singletonCollector());
		result.setName(diagnosisRevision.getName());
	}

	/**
	 * Saves name changes and adds new revision
	 */
	public void updateDiagnosisRevision() {
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
								"log.patient.task.diagnosisRevision.update", revisionHolder.getRevision().toString());
					}
				}
			}

			// adding the task to the diagnosis list
			favouriteListDAO.addReattachedTaskToList(task, PredefinedFavouriteList.DiagnosisList);
			globalEditViewHandler.updateDataOfTask(true, false, true, false);

		} catch (HistoDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}
}
