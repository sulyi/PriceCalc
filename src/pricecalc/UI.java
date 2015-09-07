/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pricecalc;

import java.util.Date;

/**
 *
 * @author arsene
 */

public interface UI {
    
    abstract void start();
    
    abstract void showError(String msg);
    
    abstract void showMessage(String msg);
    
    abstract boolean askYesNo(String msg);
    
}