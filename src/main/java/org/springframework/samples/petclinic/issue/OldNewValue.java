package org.springframework.samples.petclinic.issue;

public class OldNewValue {
    private final Object oldValue;
    private final Object newValue;

    public OldNewValue(Object oldValue, Object newValue) {
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }
}
