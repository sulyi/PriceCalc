/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pricecalc.utils;

/**
 *
 * @author arsene
 */
public class CSVField<T> {

    private final T value;

    public CSVField(final T value) {
        this.value = value;
    }

    public Class getValueClass() {
        return value.getClass();
    }

    public T valueOf() {
        return value;
    }
}