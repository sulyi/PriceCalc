/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pricecalc;

import java.util.List;
import pricecalc.utils.CSVFileHandler;
import pricecalc.utils.CSVRecord;

/**
 *
 * @author arsene
 */

public interface UI {
    
    void start();
    
    void showBasePriceRatio(CSVFileHandler handler, List<CSVRecord> rows);
    
    void showIntervals(CSVFileHandler handler, List<CSVRecord> rows);
    
    void showServiceTypes(CSVFileHandler handler, List<CSVRecord> rows);
    
    void showApClasses(CSVFileHandler handler, List<CSVRecord> rows);
    
    void showAPs(CSVFileHandler handler, List<CSVRecord> rows);
    
    void showContract(String name, CSVFileHandler handler, List<CSVRecord> rows, PriceCalc callback);
    
    void showError(String msg);
    
    void showMessage(String msg);
    
    boolean askYesNo(String msg);
    
}