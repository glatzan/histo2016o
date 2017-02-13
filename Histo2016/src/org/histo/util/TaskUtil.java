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
import org.histo.model.patient.Patient;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.ui.StainingTableChooser;

public class TaskUtil {

	private static Logger logger = Logger.getRootLogger();


	/**
	 * Creats a new block and adds it to the sample
	 * 
	 * @param sample
	 * @return
	 */
	public final static Block createNewBlock(Sample sample) {
		Block block = new Block();
		block.setBlockID(getCharNumber(sample.getBlocks().size()));
		block.setParent(sample);
		sample.getBlocks().add(block);
		return block;
	}

	/**
	 * Creats a staining and adds it to the sample.
	 * 
	 * @param sample
	 * @param prototype
	 * @return
	 */
	public final static Slide createNewStaining(Block block, StainingPrototype prototype) {
		Slide staining = new Slide();

		staining.setReStaining(block.getParent().isReStainingPhase());
		staining.setCreationDate(System.currentTimeMillis());
		staining.setSlidePrototype(prototype);
		staining.setParent(block);

		// generating block id
		String number = "";
		int stainingsInBlock = getNumerOfSameStainings(block, prototype);

		if (stainingsInBlock > 1)
			number = " " + String.valueOf(stainingsInBlock);
		staining.setSlideID(block.getParent().getSampleID() + block.getBlockID() + " " + prototype.getName() + number);

		// setting unique slide number
		staining.setUniqueIDinBlock(block.getNextSlideNumber());

		block.getSlides().add(staining);

		logger.info("New staining created " + staining.getSlideID());

		return staining;
	}

	/**
	 * Returns the number of the same stainings used within this block
	 * 
	 * @param block
	 * @param prototype
	 * @return
	 */
	public final static int getNumerOfSameStainings(Block block, StainingPrototype prototype) {
		int count = 1;
		for (Slide staining : block.getSlides()) {
			if (staining.getSlidePrototype().getId() == prototype.getId())
				count++;
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
	 * Creates linear list of all slides of the given task. The
	 * StainingTableChosser is used as holder class in order to offer an option
	 * to select the slides by clicking on a checkbox. Archived elements will
	 * not be shown if showArchived is false.
	 */
	public static final void generateSlideGuiList(Task task) {
		generateSlideGuiList(task, false);
	}

	/**
	 * Creates linear list of all slides of the given task. The
	 * StainingTableChosser is used as holder class in order to offer an option
	 * to select the slides by clicking on a checkbox. Archived elements will
	 * not be shown if showArchived is false.
	 * 
	 * @param showArchived
	 */
	public static final void generateSlideGuiList(Task task, boolean showArchived) {
		if (task.getStainingTableRows() == null)
			task.setStainingTableRows(new ArrayList<>());
		else
			task.getStainingTableRows().clear();

		boolean even = false;

		for (Sample sample : task.getSamples()) {
			// skips archived tasks
			if (sample.isArchived() && !showArchived)
				continue;

			StainingTableChooser sampleChooser = new StainingTableChooser(sample, even);
			task.getStainingTableRows().add(sampleChooser);

			for (Block block : sample.getBlocks()) {
				// skips archived blocks
				if (block.isArchived() && !showArchived)
					continue;

				StainingTableChooser blockChooser = new StainingTableChooser(block, even);
				task.getStainingTableRows().add(blockChooser);
				sampleChooser.addChild(blockChooser);

				for (Slide staining : block.getSlides()) {
					// skips archived sliedes
					if (staining.isArchived() && !showArchived)
						continue;

					StainingTableChooser stainingChooser = new StainingTableChooser(staining, even);
					task.getStainingTableRows().add(stainingChooser);
					blockChooser.addChild(stainingChooser);
				}
			}
			even = !even;
		}
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

	public static final String getDiagnosisName(List<Diagnosis> diagnoses, Diagnosis diagnosis, ResourceBundle resourceBundle) {
		int number = 1;

		for (Diagnosis diagnosisOfSample : diagnoses) {
			if (diagnosisOfSample.getType() == diagnosis.getType()) {
				number++;
			}
		}

		return resourceBundle.get("enum.diagnosisType." + diagnosis.getType()) + (number == 1 ? "" : " " + number);
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
