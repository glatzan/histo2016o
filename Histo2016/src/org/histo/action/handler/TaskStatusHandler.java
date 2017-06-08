package org.histo.action.handler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.histo.action.UserHandlerAction;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.enums.Role;
import org.histo.model.patient.Block;
import org.histo.model.patient.DiagnosisContainer;
import org.histo.model.patient.DiagnosisRevision;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class TaskStatusHandler {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private UserHandlerAction userHandlerAction;

	public boolean isTaskEditable(Task task) {

		System.out.println("Task ist editable");
		if (task == null)
			return false;

		// users and guest can't edit anything
		if (!userHandlerAction.currentUserHasRoleOrHigher(Role.MTA)) {
			logger.debug("Task not editable, user has no permission");
			return false;
		}

		// finalized
		if (task.isFinalized()) {
			logger.debug("Task not editable, is finalized");
			return false;
		}

		if (isDiagnosisCompleted(task) && isStainingCompleted(task))
			return false;

		// Blocked
		// TODO: Blocking

		return true;
	}

	public boolean isStainingCompleted(Patient patient) {
		return patient.getTasks().stream().allMatch(p -> isStainingCompleted(p));
	}

	public boolean isStainingCompleted(Task task) {
		return task.getSamples().stream().allMatch(p -> isStainingCompleted(p));
	}

	public boolean isStainingCompleted(Sample sample) {
		return sample.getBlocks().stream().allMatch(p -> isStainingCompleted(p));
	}

	public boolean isStainingCompleted(Block block) {
		return block.getSlides().stream().allMatch(p -> p.isStainingCompleted());
	}

	public boolean isReStainingFlag(Patient patient) {
		return patient.getTasks().stream().anyMatch(p -> isReStainingFlag(p));
	}

	public boolean isReStainingFlag(Task task) {
		return task.getSamples().stream().anyMatch(p -> isReStainingFlag(p));
	}

	public boolean isReStainingFlag(Sample sample) {
		return sample.getBlocks().stream().anyMatch(p -> isReStainingFlag(p));
	}

	public boolean isReStainingFlag(Block block) {
		return block.getSlides().stream().anyMatch(p -> p.isReStaining());
	}

	public boolean isDiagnosisCompleted(Patient patient) {
		return patient.getTasks().stream().allMatch(p -> isDiagnosisCompleted(p));
	}

	public boolean isDiagnosisCompleted(Task task) {
		return isDiagnosisCompleted(task.getDiagnosisContainer());
	}

	public boolean isDiagnosisCompleted(DiagnosisContainer diagnosisContainer) {
		return diagnosisContainer.getDiagnosisRevisions().stream().allMatch(p -> isDiagnosisCompleted(p));
	}

	public boolean isDiagnosisCompleted(DiagnosisRevision diagnosisRevision) {
		if (diagnosisRevision.getDiagnoses().isEmpty())
			return false;

		return diagnosisRevision.isDiagnosisCompleted();
	}
}
