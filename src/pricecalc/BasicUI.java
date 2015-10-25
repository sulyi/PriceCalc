/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pricecalc;

import java.util.List;
import java.util.Scanner;
import pricecalc.utils.CSVFileHandler;
import pricecalc.utils.CSVRecord;

/**
 *
 * @author arsene
 */
public class BasicUI implements UI{

    @Override
    public void start() {
    }
    
    @Override
    public void showBasePriceRatio(CSVFileHandler handler, List<CSVRecord> rows) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public void showIntervals(CSVFileHandler handler, List<CSVRecord> rows) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void showServiceTypes(CSVFileHandler handler, List<CSVRecord> rows) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void showApClasses(CSVFileHandler handler, List<CSVRecord> rows) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void showAPs(CSVFileHandler handler, List<CSVRecord> rows) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public void showContract(CSVFileHandler handler, List<CSVRecord> rows, PriceCalc calculator) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public void showError(String msg) {
        System.err.println(msg);
    }

    @Override
    public void showMessage(String msg) {
        System.out.println(msg);
    }

    @Override
    public boolean askYesNo(String msg) {
        if (!msg.endsWith(" "))
            msg+=" ";
        
        String yn;
        Scanner scan = new Scanner(System.in);

        while (true){
            System.out.print(msg);
            yn = scan.nextLine().trim().toLowerCase();
            
            switch (yn) {
                case "y":
                case "i":
                case "yes":
                case "igen":
                    return true;
                case "n":
                case "no":
                case "nem":
                    return false;
            }
            
        }
    }

}
