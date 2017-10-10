package org.histo.ui;

import java.util.ArrayList;
import java.util.List;

import org.histo.model.patient.Block;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Slide;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StainingTableChooser {

	private boolean choosen;
	private boolean even;

	private Sample sample;
	private Block block;
	private Slide staining;

	private List<StainingTableChooser> children;

	public StainingTableChooser(Sample sample, boolean even) {
		this.setSample(sample);
		this.even = even;
		children = new ArrayList<StainingTableChooser>();
	}

	public StainingTableChooser(Block block, boolean even) {
		this.block = block;
		this.even = even;
		children = new ArrayList<StainingTableChooser>();
	}

	public StainingTableChooser(Slide staining, boolean even) {
		this.staining = staining;
		this.even = even;
	}

	public void setChildren(List<StainingTableChooser> children) {
		this.children = children;
	}

	public void addChild(StainingTableChooser child) {
		getChildren().add(child);
	}

	public boolean isSampleType() {
		return sample != null;
	}

	public boolean isBlockType() {
		return block != null;
	}

	public boolean isStainingType() {
		return staining != null;
	}

	public void setIDText(String text) {
		if (isSampleType()) {
			sample.setSampleID(text);
			return;
		}

		if (isBlockType()) {
			block.setBlockID(text);
			return;
		}

		if (isStainingType()) {
			staining.setSlideID(text);
			return;
		}
	}

	public String getIDText() {
		if (isSampleType())
			return sample.getSampleID();
		if (isBlockType())
			return block.getBlockID();
		if (isStainingType())
			return staining.getSlideID();

		return "";
	}
}
