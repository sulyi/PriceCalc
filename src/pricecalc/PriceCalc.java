/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pricecalc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;

import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.Map;
import java.util.LinkedHashMap;

import pricecalc.utils.CSVFileHandler;
import pricecalc.utils.CSVFormatException;
import pricecalc.utils.CSVLineException;
import pricecalc.utils.CSVRecord;
import pricecalc.utils.CSVField;

/**
 *
 * @author arsene
 */

public class PriceCalc {
    
    private DecimalFormat customNumberFormat;
    private SimpleDateFormat customDateFormat;
    
    private File dbRoot;
    
    private File dbBasePriceTab;
    private File dbIntervalTab;
    private File dbServTypeTab;
    
    private File dbAPClassTab;
    private File dbAPTab;
    private File dbContracts;
    private File dbOutputs;
    
    private Float basePrice;
    private List<CSVRecord> intervals;
    private List<CSVRecord> serviceTypes;
    private List<CSVRecord> apClasses;
    private List<CSVRecord> aps;
    
    public UI userInterface;
    
    private final static String defaultConfigFile = "/config.property";
    private static String rootDir;
    
    protected void initConfig() {
        this.loadConfig(this.getClass().getResourceAsStream(defaultConfigFile));
        
        File targetConfigFile = new File(rootDir + "/config.ini");
        try (OutputStream configOutStream = new FileOutputStream(targetConfigFile);
             InputStream configInStream = PriceCalc.class.getResourceAsStream(defaultConfigFile)){
            byte[] buffer = new byte[8 * 1024];
            int bytesRead;

            while ((bytesRead = configInStream.read(buffer)) != -1) {
                configOutStream.write(buffer, 0, bytesRead);
            }
        } catch (IOException ex) {
            this.userInterface.showError("Nem lehet létrehozni a "+rootDir+"/config.ini fájt");
        }
        
    }

    protected void loadConfig(InputStream configFileStream)
            throws NullPointerException {
        if (configFileStream == null) {
            throw new NullPointerException();
        }
        
        Properties p = new Properties();
        try {
            p.load(configFileStream);
            configFileStream.close();
        } catch (IOException ex) {
            this.userInterface.showError("A konfigurációs fájlt nem lehetett megnyitni");
            throw new NullPointerException();
        }
        
        // itt akár egy jelenleg 7 hosszú tömböt és egy hibaszámlálót is
        // létre lehetne hozni, és csak egy hibaüzenetetet dobni,
        // de szerintem ez így mégis használhatóbb, mint egy hosszú üzenet
        
        boolean allGood = true;
        
        // TODO: ne állítsa azokat, amik argumentumból beállított
        try{
            dbRoot = new File(rootDir, (String) p.get("databaseRoot"));
        } catch (NullPointerException ex) {
            this.userInterface.showError("Nem található meg a \"databaseRoot\" kulcs");
            allGood = false;
        }
        try {
            dbBasePriceTab = new File(dbRoot, (String) p.get("basePrice") );
        } catch (NullPointerException ex) {
            this.userInterface.showError("Nem található meg a \"basePrice\" kulcs");
            allGood = false;
        }
        try {
            dbIntervalTab  = new File(dbRoot, (String) p.get("intervals"));
        } catch (NullPointerException ex) {
            this.userInterface.showError("Nem található meg az \"intervals\" kulcs");
            allGood = false;
        }
        try {
            dbServTypeTab  = new File(dbRoot, (String) p.get("serviceTypes"));
        } catch (NullPointerException ex) {
            this.userInterface.showError("Nem található meg a \"serviceTypes\" kulcs");
            allGood = false;
        }
        
        try {
            dbAPTab        = new File(dbRoot, (String) p.get("accessPoints"));
            } catch (NullPointerException ex) {
            this.userInterface.showError("Nem található meg az \"accessPoints\" kulcs");
            allGood = false;
        }
        try {
            dbAPClassTab   = new File(dbRoot, (String) p.get("accessPointClasses"));
            } catch (NullPointerException ex) {
            this.userInterface.showError("Nem található meg az \"accessPointClasses\" kulcs");
            allGood = false;
        }
        try {
            dbContracts = new File(dbRoot, (String) p.get("contracts"));
        }catch(NullPointerException ex){
            this.userInterface.showError("Nem található meg a \"contracts\" kulcs");
             allGood = false;
        }
        try {
            dbOutputs = new File(dbRoot, (String) p.get("outputs"));
        } catch (NullPointerException ex) {
            this.userInterface.showError("Nem található meg az \"outputs\" kulcs");
            allGood = false;
        }
        
        if (!allGood){
            throw new NullPointerException(); 
        }
        
    }
    
    private void connectDB() throws DatabaseException {
        CSVFileHandler handler;
        handler = new CSVFileHandler("N", ";",
                this.customDateFormat,
                this.customNumberFormat);

        try {
            basePrice = (Float) handler.parse(dbBasePriceTab).get(0).get("Ár százalék").valueOf();
        } catch (IOException ex) {
            this.userInterface.showError(dbBasePriceTab.getName() + " fájlt nem lehetett megnyitni");
            throw new DatabaseException();
        } catch (CSVLineException ex) {
            this.userInterface.showError(dbBasePriceTab.getName() + " fájl " + ex.getIndex()
                    + " sorában hiba történt a " + ex.getErrorOffset() + " pozicióban");
            throw new DatabaseException();
        }
        try{
            handler.setDataFormat("SDDN");
            // TODO: átlapolt időtartamok szűrése
            intervals = handler.parse(dbIntervalTab);
        } catch (IOException ex) {
            this.userInterface.showError(dbIntervalTab.getName() + " fájlt nem lehetett megnyitni");
            throw new DatabaseException();
        } catch (CSVLineException ex) {
            this.userInterface.showError(dbIntervalTab.getName() + " fájl " + ex.getIndex()
                    + " sorában hiba történt a " + ex.getErrorOffset() + " pozicióban");
            throw new DatabaseException();
        }
        try {
            handler.setDataFormat("SN");
            serviceTypes = handler.parse(dbServTypeTab);
        } catch (IOException ex) {
            this.userInterface.showError(dbServTypeTab.getName() + " fájlt nem lehetett megnyitni");
            throw new DatabaseException();
        } catch (CSVLineException ex) {
            this.userInterface.showError(dbServTypeTab.getName() + " fájl " + ex.getIndex()
                    + " sorában hiba történt a " + ex.getErrorOffset() + " pozicióban");
            throw new DatabaseException();
        }
        try {
            handler.setDataFormat("SNSS");
            apClasses = handler.parse(dbAPClassTab);
        } catch (IOException ex) {
            this.userInterface.showError(dbAPClassTab.getName() + " fájlt nem lehetett megnyitni");
            throw new DatabaseException();
        } catch (CSVLineException ex) {
            this.userInterface.showError(dbAPClassTab.getName() + " fájl " + ex.getIndex()
                    + " sorában hiba történt a " + ex.getErrorOffset() + " pozicióban");
            throw new DatabaseException();
        }
        try {    
            handler.setDataFormat("SS");
            aps = handler.parse(dbAPTab);

        } catch (IOException ex) {
            this.userInterface.showError(dbAPTab.getName() + " fájlt nem lehetett megnyitni");
            throw new DatabaseException();
        } catch (CSVLineException ex) {
            this.userInterface.showError(dbAPTab.getName() + " fájl " + ex.getIndex()
                    + " sorában hiba történt a " + ex.getErrorOffset() + " pozicióban");
            throw new DatabaseException();
        }
        
        if (dbOutputs.exists()) {
            if (!dbOutputs.isDirectory()) {
                this.userInterface.showError(dbOutputs.getName()+" nem egy könyvtár");
                throw new DatabaseException();
            }
        } else if (this.userInterface.askYesNo(dbOutputs+" nem létezik létre akarja hozni? (i/n)")){
            try {
                Files.createDirectory(dbOutputs.toPath());
            } catch (IOException ex) {
                this.userInterface.showError("Nem lehetett létrehozni a "+dbOutputs.getName() + " könyvtárati");
                throw new DatabaseException();
            }
        }else{
            throw new DatabaseException();
        }
        
    }

    protected void calculateContract(File contract) throws DatabaseException{
        CSVFileHandler handler = new CSVFileHandler("SDDSNN", ";",
                                                    this.customDateFormat,
                                                    this.customNumberFormat);

        String apName;
        Date from;
        Date to;
        String apServiceType;
        Float yearQ;
        Float dayQ;

        Float intervalPrice;
        Float serviceTypePrice;
        String apApClass;
        Float apClassPrice;
        Float Q;
        String apClassQ;
        
        Map<String, Float> apSummerLookUp = new LinkedHashMap<>();
        Map<String, Float> apClassSummerLookUp = new LinkedHashMap<>();
        
        Float sum;
        Float i;
        
        List<CSVRecord> apResult;
        List<CSVRecord> apClassResult;
        
        Map<String, Integer> resultHeader;
        
        File outFile;
        String outName;
        int extPos;
        
        try {
            for (CSVRecord record : handler.parse(contract)){
                
                intervalPrice = basePrice;
                serviceTypePrice = null;
                apApClass = null;
                apClassPrice = null;
                Q = null;
                apClassQ = null;
                
                apName =(String) record.get("Pont kódja").valueOf();
                from = (Date) record.get("Érvényesség kezdete").valueOf();
                to = (Date) record.get("Érvényesség vége").valueOf();
                apServiceType = (String) record.get("Kapacitástípus").valueOf();
                dayQ = (Float) record.get("Mennyiség MJ/nap").valueOf();
                yearQ = (Float) record.get("Mennyiség MJ/óra").valueOf();
                
                if(!checkInterval(from, to)){
                    this.userInterface.showError(apName +
                            " hozzáférésiponthoz tartozó lekötés érvényessége "+
                            "kívül esik az érvényességi tartományon a "+
                            contract.getName()+" fájlban");
                    continue;
                }

                for (CSVRecord interval : intervals) {
                    
                    if(inInterval(from, to,
                                  (Date) interval.get("Időtartam kezdete").valueOf(),
                                  (Date) interval.get("Időtartam vége").valueOf(), true)){
                        intervalPrice = (Float) interval.get("Ár százalék").valueOf();
                    }
                }
                
                for (CSVRecord serviceType : serviceTypes){
                    if ( apServiceType.equals((String) serviceType.get("Kapacitástípus").valueOf()))
                        serviceTypePrice = (Float) serviceType.get("Ár százalék").valueOf();
                }
                
                if( serviceTypePrice == null ){
                    this.userInterface.showError(apName+" hozzáférésipont szolgáltatás típusához ("+
                            apServiceType+") nem találhatóak adatok a "+contract.getName()+" fájlban");
                    continue;
                }
                
                for (CSVRecord ap : aps){
                    if (apName.equals((String) ap.get("Pont kódja").valueOf())){
                        apApClass = (String) ap.get("Pontcsoport").valueOf();
                    }
                }
                
                if (apApClass == null){
                    this.userInterface.showError(apName+" hozáférési pont típus nem található a "
                            +contract.getName()+" fájlban");
                    continue;
                }
                
                for (CSVRecord apClass : apClasses) {
                    if (apApClass.equals((String) apClass.get("Pontcsoport").valueOf())) {
                        apClassPrice = (Float) apClass.get("Ár").valueOf();
                        apClassQ = (String) apClass.get("Mértékegység").valueOf();
                    }
                }
                
                try{
                    Q = (Float) record.get("Mennyiség " + apClassQ).valueOf();
                } catch (IllegalArgumentException ex){
                    this.userInterface.showError(apApClass+" hozzáférési típus mértékegységével (" +
                            apClassQ + ") nincs megadva a lekötés mennyisége a "+contract.getName()+" fájlban");
                    continue;
                }
                
                if (apSummerLookUp.containsKey(apName)){
                    sum = apSummerLookUp.get(apName);
                    sum += apClassPrice * intervalPrice * serviceTypePrice * Q;
                    apSummerLookUp.put(apName, sum);
                } else{
                    apSummerLookUp.put(apName, apClassPrice * intervalPrice * serviceTypePrice * Q);
                }
                
                if (apClassSummerLookUp.containsKey(apApClass)) {
                    sum = apClassSummerLookUp.get(apApClass);
                    sum += apClassPrice * intervalPrice * serviceTypePrice * Q;
                    apClassSummerLookUp.put(apApClass, sum);
                } else {
                    apClassSummerLookUp.put(apApClass, apClassPrice * intervalPrice * serviceTypePrice * Q);
                }
            }
            
            apResult = new ArrayList<>();
            resultHeader = new LinkedHashMap<>();
            resultHeader.put("Pont kódja", 0);
            resultHeader.put("Összeg", 1);
            
            sum = Float.valueOf(0);
            
            for (String key : apSummerLookUp.keySet()){
                i = apSummerLookUp.get(key);
                sum += i;
                apResult.add(new CSVRecord(new CSVField[]{new CSVField<>(key),
                                                          new CSVField<>(Float.valueOf(i))},
                                           resultHeader));
            }

            apResult.add(new CSVRecord(new CSVField[]{new CSVField<>("Összesen"),
                                                      new CSVField<>(Float.valueOf(sum))},
                                       resultHeader));
            
            outName = contract.getName();
            extPos = outName.lastIndexOf(".");
            
            if (extPos != -1)
                outName = outName.substring(0, extPos);
            
            handler.setDataFormat("SN");
            handler.setHeader(resultHeader);
            
            outFile = new File(dbOutputs, outName + "-result.csv");
            
            if (!outFile.exists() ||
                this.userInterface.askYesNo(outName + "-result.csv létezik, felül akarja írni? (i/n)")) {
                try {
                    handler.print(apResult, outFile);
                } catch (CSVFormatException ex) {
                    this.userInterface.showError("A kimeneti fájl "+ex.getLine()+" sorát nem sikerült formátumhoz igaítani" );
                }
            }

            apClassResult = new ArrayList<>();
            resultHeader = new LinkedHashMap<>();
            resultHeader.put("Pontcsoport", 0);
            resultHeader.put("Összeg", 1);

            sum = Float.valueOf(0);

            for (String key : apClassSummerLookUp.keySet()) {
                i = apClassSummerLookUp.get(key);
                sum += i;
                apClassResult.add(new CSVRecord(new CSVField[]{new CSVField<>(key),
                                                               new CSVField<>(Float.valueOf(i))},
                                                resultHeader));
            }

            apClassResult.add(new CSVRecord(new CSVField[]{new CSVField<>("Összesen"),
                                            new CSVField<>(Float.valueOf(sum))},
                              resultHeader));

            handler.setHeader(resultHeader);

            outFile = new File(dbOutputs, outName + "-class-result.csv");

            if (!outFile.exists() ||
                this.userInterface.askYesNo(outName + "-class-result.csv létezik, felül akarja írni? (i/n)")){
                try {
                    handler.print(apClassResult, outFile);
                } catch (CSVFormatException ex) {
                    this.userInterface.showError("A kimeneti fájl " + ex.getLine() + " sorát nem sikerült formátumhoz igaítani");
                }
            }
        } catch (IOException ex) {
            this.userInterface.showError(contract.getName() + " fájlt nem lehetett megnyitni");
            throw new DatabaseException();
        } catch (CSVLineException ex) {
            this.userInterface.showError(contract.getName() + " fájl " + ex.getIndex()
                    + " sorában hiba történt a " + ex.getErrorOffset() + " pozicióban");
            throw new DatabaseException();
        }
    }
    
    private void parseArgs(String[] opts) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    
    public static boolean checkInterval(Date from, Date to){
        Calendar f = Calendar.getInstance();
        f.setTime(from);
        Calendar t = Calendar.getInstance();
        t.setTime(to);
        
        f.roll(Calendar.MONTH, true);
        f.roll(Calendar.DAY_OF_YEAR, false);
        
        return f.equals(t);
    }

    public static boolean inInterval(Date dateFrom, Date dateTo, Date from, Date to) {
        return inInterval(dateFrom, dateTo, from, to, false);
    }
    
    public static boolean inInterval(Date dateFrom, Date dateTo, Date from, Date to, boolean addYear){
        Calendar dayFrom = Calendar.getInstance();
        dayFrom.setTime(dateFrom);
        Calendar dayTo = Calendar.getInstance();
        dayTo.setTime(dateTo);
        
        Calendar iFrom = Calendar.getInstance();
        iFrom.setTime(from);
        Calendar iTo = Calendar.getInstance();
        iTo.setTime(to);
        
        if (addYear){
            iFrom.add(Calendar.YEAR, dayFrom.get(Calendar.YEAR));
            iTo.add(Calendar.YEAR, dayFrom.get(Calendar.YEAR));
        }
        
        return (dayFrom.after(iFrom) || dayFrom.equals(iFrom)) &&
               (dayTo.before(iTo)    || dayTo.equals(iTo));
    }
    
    private static DecimalFormat setNumberFormat(char decSep, char groupSep) {
        return setNumberFormat(decSep, groupSep, 1, 3, true);
    }

    private static DecimalFormat setNumberFormat(char decSep, char groupSep, int decNum, int groupSize, boolean alwaysGroup) {

        DecimalFormatSymbols myHunNoFormatSym = new DecimalFormatSymbols();
        DecimalFormat newNumberFormat = new DecimalFormat();


        myHunNoFormatSym.setDecimalSeparator(decSep);
        myHunNoFormatSym.setGroupingSeparator(groupSep);

        newNumberFormat.setDecimalFormatSymbols(myHunNoFormatSym);
        newNumberFormat.setGroupingUsed(alwaysGroup);
        newNumberFormat.setGroupingSize(groupSize);
        newNumberFormat.setMaximumFractionDigits(decNum);

        return newNumberFormat;

    }
    
    public static void main(String[] args) {
        rootDir=new File(PriceCalc.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();
        PriceCalc calc = new PriceCalc();
        // így a loadConfig-ban vagy parseArgs-ban is könyen lehetne állítani
        calc.customNumberFormat = setNumberFormat(',', ' ', 1, 3, true);
        calc.customDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        
        calc.userInterface = new BasicUI();
        calc.userInterface.start();
        
        // TODO: config fájl beállítása argumentumból
        //calc.parseArgs(args);
        
        String configFileName = rootDir + "/config.ini";
        
        try (InputStream configStream = new FileInputStream(configFileName)){
            calc.loadConfig(configStream);
        } catch (IOException ex1) {
            calc.userInterface.showError("Nem található a "+ rootDir+"/config.ini");
            try {
                calc.initConfig();
            } catch (NullPointerException ex2) {
                calc.userInterface.showError("Korrupt fájl: " + defaultConfigFile);
                System.exit(2);
            }
        } catch (NullPointerException ex1) {
            // initConfig?
            calc.userInterface.showError("Hibás konfigurációs fájl: " + configFileName);
            System.exit(1);
        }
        
        // mehet-e tovább?
        
        try {
            calc.connectDB();
        } catch (DatabaseException ex) {
            System.exit(3);
        }
        
        for (File contract : calc.dbContracts.listFiles(new FileExtensionFilter("csv"))) {
            System.out.println(contract.getName());
            try {
                calc.calculateContract(contract);
            } catch (DatabaseException ignore) {
            }
        }
        
    }
    
}

class FileExtensionFilter implements FileFilter {

    private String[] exts;

    public FileExtensionFilter() {
    }

    public FileExtensionFilter(String... extensions) {
        int length = extensions.length;
        exts = new String[length];
        for (int i = 0; i < length; i++) {
            exts[i] = extensions[i].toLowerCase();
        }
    }

    @Override
    public boolean accept(File file) {
        if (file.isDirectory()) {
            return false;
        }
        if (exts != null) {
            String path = file.getAbsolutePath().toLowerCase();
            for (int i = 0, n = exts.length; i < n; i++) {
                String extension = exts[i];
                if (path.endsWith(extension) && (path.charAt(path.length()
                        - extension.length() - 1)) == '.') {
                    return true;
                }
            }
            return false;
        } else {
            return true;
        }
    }
}