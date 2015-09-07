/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pricecalc.utils;

/**
 *
 * @author arsene
 */
public class CSVFormatException extends Exception {
    private final int line;
    private final String msg;
    
    public CSVFormatException(String msg, int line){
        this.msg = msg;
        this.line = line;
    }
    
    public int getLine() {
        return line;
    }

    
}
