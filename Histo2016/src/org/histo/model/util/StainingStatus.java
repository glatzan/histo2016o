package org.histo.model.util;

public interface StainingStatus {
    public boolean isNew();
    
    public boolean isStainingPerformed();
    public boolean isStainingNeeded();
    public boolean isReStainingNeeded();
}
