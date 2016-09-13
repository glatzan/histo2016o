package org.histo.model.util;

import org.histo.model.Patient;

/**
 * Interfaces welches vom StainingTree (Task->Sample->Block->Staining) implementiert wird.
 * 
 * @author andi
 *
 * @param <T>
 */
public interface StainingTreeParent<T> extends ArchiveAble {
    public Patient getPatient();

    public T getParent();

    public void setParent(T parent);
}
