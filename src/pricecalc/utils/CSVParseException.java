/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pricecalc.utils;

/**
 *
 * @author arsene
 */

class CSVParseException extends CSVException {
    private final char type;
    private final int index;
    
    public CSVParseException(String msg, int fieldIndex, char type, int errorOffset){
        super(msg, errorOffset);
        this.index = fieldIndex;
        this.type = type;
    }
    
    public final char getType(){
        return this.type;
    }
    
    public final int getIndex() {
        return this.index;
    }
    
}
