/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pricecalc.utils;

import java.text.ParseException;

/**
 *
 * @author arsene
 */
public class CSVException extends ParseException {
    public CSVException(String msg, int errorOffset){
        super(msg, errorOffset);
    }
    
}
