/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pricecalc;

import java.util.Scanner;

/**
 *
 * @author arsene
 */
public class BasicUI implements UI{

    @Override
    public void start() {
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
