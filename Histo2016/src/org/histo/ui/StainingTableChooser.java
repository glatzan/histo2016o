package org.histo.ui;

import java.util.ArrayList;
import java.util.List;

import org.histo.model.Block;
import org.histo.model.Sample;
import org.histo.model.Staining;

public class StainingTableChooser {

    private boolean even;
    private boolean choosen;

    private Sample sample;
    private Block block;
    private Staining staining;

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

    public StainingTableChooser(Staining staining, boolean even) {
	this.staining = staining;
	this.even = even;
    }

    public Sample getSample() {
	return sample;
    }

    public void setSample(Sample sample) {
	this.sample = sample;
    }

    public Block getBlock() {
	return block;
    }

    public void setBlock(Block block) {
	this.block = block;
    }

    public Staining getStaining() {
	return staining;
    }

    public void setStaining(Staining staining) {
	this.staining = staining;
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

    public boolean isEven() {
	return even;
    }

    public void setEven(boolean even) {
	this.even = even;
    }

    public boolean isChoosen() {
	return choosen;
    }

    public void setChoosen(boolean choosen) {
	this.choosen = choosen;
    }

    public List<StainingTableChooser> getChildren() {
	return children;
    }

    public void setChildren(List<StainingTableChooser> children) {
	this.children = children;
    }
    
    public void addChild(StainingTableChooser child){
	getChildren().add(child);
    }

}
