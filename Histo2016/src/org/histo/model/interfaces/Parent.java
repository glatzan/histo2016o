package org.histo.model.interfaces;

import org.histo.model.patient.Patient;

/**
 * Interface for every object of the task tree (Task->Sample->Block->Staining).
 * Enables the returning of the parent and implements the archivable interface.
 * 
 * @author andi
 *
 * @param <T>
 */
public interface Parent<T>{
    public Patient getPatient();

    public T getParent();

    public void setParent(T parent);
}
