/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pricecalc.utils;

/**
 *
 * @author arsene
 */

public class CSVLineException extends CSVException {
    private final int index;

    public CSVLineException(String msg, int lineIndex, int errorOffset) {
        super(msg, errorOffset);
        this.index = lineIndex;
    }
    
    public final int getIndex() {
        return this.index;
    }
}
