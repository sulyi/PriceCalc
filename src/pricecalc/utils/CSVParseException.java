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
    private final CSVType type;
    private final int index;
    
    public CSVParseException(String msg, int fieldIndex, CSVType type, int errorOffset){
        super(msg, errorOffset);
        this.index = fieldIndex;
        this.type = type;
    }
    
    public final CSVType getType(){
        return this.type;
    }
    
    public final int getIndex() {
        return this.index;
    }
    
}
