package org.histo.model.util;


public interface EditAbleEntity<T> {
    /**
     * Copies the values of the current object into an other object
     * @param object
     */
    public void update(T object);
    
    /**
     * Returns the content of object as json string.
     * @return
     */
    public String asGson();
}
