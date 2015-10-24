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
    
    void start();
    
    void showError(String msg);
    
    void showMessage(String msg);
    
    boolean askYesNo(String msg);
    
}