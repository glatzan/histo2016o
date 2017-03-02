package org.histo.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.histo.action.SlideHandlerAction;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.DiagnosisRevisionType;
import org.histo.model.StainingPrototype;
import org.histo.model.patient.Block;
import org.histo.model.patient.Diagnosis;
import org.histo.model.patient.DiagnosisContainer;
import org.histo.model.patient.DiagnosisRevision;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.ui.StainingTableChooser;

public class TaskUtil {

	private static Logger logger = Logger.getLogger("org.histo");

	/**
	 * Returns the number of the same stainings used within this block
	 * 
	 * @param block
	 * @param prototype
	 * @return
	 */
	public final static int getNumerOfSameStainings(Slide slide) {
		int count = 0;
		for (Slide slideInBlock : slide.getParent().getSlides()) {
			if (slideInBlock.getSlidePrototype().getId() == slide.getSlidePrototype().getId())
				count++;

			if (slideInBlock == slide)
				break;
		}
		return count;
	}

	/**
	 * Converts a Number into a roman number.
	 * 
	 * @param number
	 * @return
	 */
	public final static String getRomanNumber(int number) {

		StringBuffer result = new StringBuffer();

		while ((number / 5000) > 0) {
			result.append("A");
			number -= 5000;
		}
		while ((number / 1000) > 0) {
			result.append("M");
			number -= 1000;
		}
		while ((number / 500) == 1 && (number / 100) != 9) {
			result.append("D");
			number -= 500;
		}
		if ((number / 100) > 0) {
			if ((number / 100) == 9) {
				result.append("CM");
				number -= 900;
			} else if ((number / 100) == 4) {
				result.append("CD");
				number -= 400;
			} else {
				while ((number / 100) > 0) {
					result.append("C");
					number -= 100;
				}
			}
		}
		while ((number / 50) == 1 && (number / 10) != 9) {
			result.append("L");
			number -= 50;
		}
		if ((number / 10) > 0) {
			if ((number / 10) == 9) {
				result.append("XC");
				number -= 90;
			} else if ((number / 10) == 4) {
				result.append("XL");
				number -= 40;
			} else {
				while ((number / 10) > 0) {
					result.append("X");
					number -= 10;
				}
			}
		}
		while ((number / 5) == 1 && (number / 1) != 9) {
			result.append("V");
			number -= 5;
		}
		if ((number / 1) > 0) {
			if ((number / 1) == 9) {
				result.append("IX");
				number -= 9;
			} else if ((number / 1) == 4) {
				result.append("IV");
				number -= 4;
			} else {
				while ((number / 1) > 0) {
					result.append("I");
					number -= 1;
				}
			}
		}
		return result.toString();
	}

	/**
	 * Converts a number into chars. The number has to be smaller than 26
	 * 
	 * @param number
	 * @return
	 */
	public static final String getCharNumber(int number) {
		if (number > 26)
			return "";

		return String.valueOf(Character.toChars(((int) 'a') + number));
	}

	/**
	 * Returns the task with the highest taskID. (Is always the first task
	 * because of the descending order)
	 * 
	 * @param tasks
	 * @return
	 */
	public final static Task getLastTask(List<Task> tasks, boolean active) {
		if (tasks == null || tasks.isEmpty())
			return null;

		// List is ordere desc by taskID per default so return first (and
		// latest) task in List
		if (tasks != null && !tasks.isEmpty()) {
			if (active == false)
				return tasks.get(0);

			for (Task task : tasks) {
				if (task.isActiveOrActionToPerform())
					return task;
			}
		}
		return null;
	}

	public final static Task getFirstTask(List<Task> tasks, boolean active) {
		if (tasks == null || tasks.isEmpty())
			return null;

		// List is ordere desc by taskID per default so return first (and
		// latest) task in List
		if (tasks != null && !tasks.isEmpty()) {

			if (active == false)
				return tasks.get(tasks.size() - 1);

			for (int i = tasks.size() - 1; i >= 0; i--) {
				if (tasks.get(i).isActiveOrActionToPerform())
					return tasks.get(i);
				else
					continue;
			}
		}
		return null;
	}

	public final static Task getPrevTask(List<Task> tasks, Task currentTask, boolean activeOnle) {

		int index = tasks.indexOf(currentTask);
		if (index == -1 || index == 0)
			return null;

		for (int i = index - 1; i >= 0; i--) {
			if (activeOnle) {
				if (tasks.get(i).isActiveOrActionToPerform())
					return tasks.get(i);
			} else
				return tasks.get(i);
		}
		return null;
	}

	public final static Task getNextTask(List<Task> tasks, Task currentTask, boolean activeOnle) {

		int index = tasks.indexOf(currentTask);
		if (index == -1 || index == tasks.size() - 1)
			return null;

		for (int i = index + 1; i < tasks.size(); i++) {
			if (activeOnle) {
				if (tasks.get(i).isActiveOrActionToPerform())
					return tasks.get(i);
			} else
				return tasks.get(i);
		}
		return null;
	}

	/**
	 * Returns a name for a diagnosis revision
	 * 
	 * @param revisions
	 * @param revision
	 * @param resourceBundle
	 * @return
	 * 
	 */
	public static final String getDiagnosisRevisionName(List<DiagnosisRevision> revisions, DiagnosisRevision revision,
			ResourceBundle resourceBundle) {

		// if default diagnosis
		if (revision.getType() == DiagnosisRevisionType.DIAGNOSIS) {
			// if there are other diagnoses then the default one
			if (revisions.size() > 1) {
				logger.debug("DiagnosisRevision name " + resourceBundle.get("enum.diagnosisType.DIAGNOSIS.temp"));
				return resourceBundle.get("enum.diagnosisType.DIAGNOSIS.temp");
			} else {
				// default diagnosis only
				logger.debug("DiagnosisRevision name " + resourceBundle.get("enum.diagnosisType.DIAGNOSIS"));
				return resourceBundle.get("enum.diagnosisType.DIAGNOSIS");
			}

		} else {
			int number = 0;
			for (DiagnosisRevision diagnosisRevision : revisions) {
				if (diagnosisRevision == revision)
					break;

				if (diagnosisRevision.getType() == revision.getType())
					number++;
			}
			String result = resourceBundle.get("enum.diagnosisType." + revision.getType())
					+ (number == 0 ? "" : " " + number);
			logger.debug("DiagnosisRevision name " + result);
			return result;
		}
	}

	/**
	 * Returns the task with the highest priority. If several tasks share the
	 * same priority the first one is returned.
	 * 
	 * @param tasks
	 * @return
	 */
	public static final Task getTaskByHighestPriority(List<Task> tasks) {
		if (tasks == null || tasks.size() == 0)
			return null;

		Task highest = tasks.get(0);

		for (int i = 1; i < tasks.size(); i++) {
			if (highest.getTaskPriority().getPriority() < tasks.get(i).getTaskPriority().getPriority())
				highest = tasks.get(i);
		}

		return highest;
	}
}
