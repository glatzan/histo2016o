package org.histo.ui;

import org.histo.model.StainingPrototype;

public class StainingListChooser {
    
    private StainingPrototype stainingPrototype;
    
    private boolean choosen;

    public StainingListChooser(StainingPrototype stainingPrototype){
	this.stainingPrototype = stainingPrototype;
    }
    
    public StainingPrototype getStainingPrototype() {
        return stainingPrototype;
    }

    public void setStainingPrototype(StainingPrototype stainingPrototype) {
        this.stainingPrototype = stainingPrototype;
    }

    public boolean isChoosen() {
        return choosen;
    }

    public void setChoosen(boolean choosen) {
        this.choosen = choosen;
    }
    
    
}
