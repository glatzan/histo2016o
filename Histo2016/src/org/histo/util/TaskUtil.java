package org.histo.util;

import java.util.Date;
import java.util.List;

import javax.persistence.Transient;

import org.apache.log4j.Logger;
import org.histo.model.StainingPrototype;
import org.histo.model.patient.Block;
import org.histo.model.patient.Diagnosis;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;

public class TaskUtil {

	private static Logger log = Logger.getLogger(TaskUtil.class.getName());

	/**
	 * Creats an empty task
	 * 
	 * @param taskNumer
	 * @return
	 */
	public final static Task createNewTask(Patient patient, int taskNumer) {
		Task result = new Task();
		result.setCreationDate(new Date(System.currentTimeMillis()));
		result.setDateOfReceipt(new Date(System.currentTimeMillis()));
		result.setDueDate(new Date(System.currentTimeMillis()));
		// 20xx -2000 = tasknumber
		result.setTaskID(Integer.toString(TimeUtil.getCurrentYear()-2000) + fitString(taskNumer, 4));
		result.setParent(patient);

		return result;
	}

	/**
	 * Creates a new sample with one diagnosis an standard staingings. Adds the
	 * sample to the given tasks and returns it-
	 * 
	 * @param task
	 * @param standardStainings
	 * @return
	 */
	public final static Sample createNewSample(Task task) {
		Sample sample = new Sample();
		sample.setGenerationDate(new Date(System.currentTimeMillis()));
		sample.setSampleID(getRomanNumber(task.getSampleNumer()));
		sample.setParent(task);
		task.getSamples().add(sample);
		task.incrementSampleNumber();
		return sample;
	}

	/**
	 * Creates a diagnosis an adds it to the given sample
	 * 
	 * @param sample
	 * @return
	 */
	public final static Diagnosis createNewDiagnosis(Sample sample, int type) {
		Diagnosis diagnosis = new Diagnosis();
		diagnosis.setGenerationDate(new Date(System.currentTimeMillis()));
		diagnosis.setType(type);
		diagnosis.setDiagnosisOrder(sample.getDiagnosisNumber());
		diagnosis.setName(getDiagnosisName(sample, diagnosis));
		diagnosis.setParent(sample);
		
		sample.incrementDiagnosisNumber();
		sample.getDiagnoses().add(diagnosis);

		return diagnosis;
	}

	/**
	 * Creats a new block and adds it to the sample
	 * 
	 * @param sample
	 * @return
	 */
	public final static Block createNewBlock(Sample sample) {
		Block block = new Block();
		block.setBlockID(getCharNumber(sample.getBlockNumber()));
		block.setParent(sample);
		sample.getBlocks().add(block);
		sample.incrementBlockNumber();
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
		staining.setGenerationDate(new Date(System.currentTimeMillis()));
		staining.setSlidePrototype(prototype);
		staining.setParent(block);

		// generating block id
		String number = "";
		int stainingsInBlock = getNumerOfSameStainings(block, prototype);
		if (stainingsInBlock > 1)
			number = " " + String.valueOf(stainingsInBlock);
		staining.setSlideID(
				block.getParent().getSampleID() + block.getBlockID() + " " + prototype.getName() + number);

		block.getSlides().add(staining);
		block.incrementSlideNumber();

		return staining;
	}

	/**
	 * Returns the number of the same stainings used within this block
	 * 
	 * @param block
	 * @param prototype
	 * @return
	 */
	private final static int getNumerOfSameStainings(Block block, StainingPrototype prototype) {
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

	public final static Task getLastTask(List<Task> tasks) {
		Task result = null;
		if (tasks != null && tasks.size() > 0) {

			for (Task task : tasks) {
				if (result == null) {
					result = task;
				} else if (result.getDateOfReceipt().before(task.getDateOfReceipt())) {
					result = task;
				}
			}
		}
		return result;
	}

	public final static String fitString(int value, int len) {
		String result = String.valueOf(value);
		while (result.length() < len) {
			result = "0" + result;
		}
		return result;
	}

	@Transient
	public static final String getDiagnosisName(Sample sample, Diagnosis diagnosis) {
		int number = 1;

		for (Diagnosis diagnosisOfSample : sample.getDiagnoses()) {
			if (diagnosisOfSample.getType() == diagnosis.getType()) {
				number++;
			}
		}

		return diagnosis.getDiagnosisTypAsName() + (number == 1 ? "" : " " + number);
	}
}
