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

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
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
import pricecalc.utils.CSVType;

/**
 *
 * @author arsene
 */

public class PriceCalc {
    
    private DecimalFormat customNumberFormat;
    private SimpleDateFormat customDateFormat;
    
    private File dbRoot;
    
    private File dbBasePriceRatioTab;
    private File dbIntervalTab;
    private File dbServTypeTab;
    
    private File dbAPClassTab;
    private File dbAPTab;
    private File dbContracts;
    private File dbOutputs;
    
    private Float basePriceRatio;
    private List<CSVRecord> intervals;
    private List<CSVRecord> serviceTypes;
    private List<CSVRecord> apClasses;
    private List<CSVRecord> aps;
    
    public UI userInterface;
    
    private final static String defaultConfigFile =  "/config.property";
    private static String rootDir;
    
    protected void initConfig() throws IOException {
        this.loadConfig(this.getClass().getResourceAsStream(defaultConfigFile));
        File targetConfigFile = new File(rootDir + File.separatorChar + "config.ini");
        
        try (OutputStream configOutStream = new FileOutputStream(targetConfigFile);
            InputStream configInStream = PriceCalc.class.getResourceAsStream(defaultConfigFile)){
            byte[] buffer = new byte[8 * 1024];
            int bytesRead;

            while ((bytesRead = configInStream.read(buffer)) != -1) {
                configOutStream.write(buffer, 0, bytesRead);
            }
        }
        
    }

    protected void loadConfig(InputStream configFileStream)
            throws NullPointerException {
        if (configFileStream == null) {
            throw new NullPointerException();
        }
        
        Properties config = new Properties();
        try {
            config.load(configFileStream);
            configFileStream.close();
        } catch (IOException ex) {
            this.userInterface.showError("A konfigurációs fájlt nem lehetett megnyitni");
            throw new NullPointerException();
        }
        
        // itt akár egy (jelenleg 7 hosszú) tömböt és egy hibaszámlálót is
        // létre lehetne hozni, és csak egy hibaüzenetetet dobni,
        // de szerintem ez így mégis használhatóbb, mint egy hosszú üzenet
        
        boolean allGood = true;
        
        // TODO: ne állítsa azokat, amik argumentumból beállított
        try{
            dbRoot = new File(rootDir, (String) config.get("databaseRoot"));
        } catch (NullPointerException ex) {
            this.userInterface.showError("Nem található meg a \"databaseRoot\" kulcs");
            allGood = false;
        }
        try {
            dbBasePriceRatioTab = new File(dbRoot, (String) config.get("basePriceRatio") );
        } catch (NullPointerException ex) {
            this.userInterface.showError("Nem található meg a \"basePriceRatio\" kulcs");
            allGood = false;
        }
        try {
            dbIntervalTab  = new File(dbRoot, (String) config.get("intervals"));
        } catch (NullPointerException ex) {
            this.userInterface.showError("Nem található meg az \"intervals\" kulcs");
            allGood = false;
        }
        try {
            dbServTypeTab  = new File(dbRoot, (String) config.get("serviceTypes"));
        } catch (NullPointerException ex) {
            this.userInterface.showError("Nem található meg a \"serviceTypes\" kulcs");
            allGood = false;
        }
        
        try {
            dbAPTab        = new File(dbRoot, (String) config.get("accessPoints"));
            } catch (NullPointerException ex) {
            this.userInterface.showError("Nem található meg az \"accessPoints\" kulcs");
            allGood = false;
        }
        try {
            dbAPClassTab   = new File(dbRoot, (String) config.get("accessPointClasses"));
            } catch (NullPointerException ex) {
            this.userInterface.showError("Nem található meg az \"accessPointClasses\" kulcs");
            allGood = false;
        }
        try {
            dbContracts = new File(dbRoot, (String) config.get("contracts"));
        }catch(NullPointerException ex){
            this.userInterface.showError("Nem található meg a \"contracts\" kulcs");
             allGood = false;
        }
        try {
            dbOutputs = new File(dbRoot, (String) config.get("outputs"));
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
        handler = new CSVFileHandler(new CSVType[]{CSVType.NUMBER},
                                     ";", 
                                     this.customDateFormat,
                                     this.customNumberFormat
                );
        
        List<CSVRecord> result;
        
        try {
            result = handler.parse(dbBasePriceRatioTab);
            basePriceRatio = (Float) result.get(0).get("Ár százalék").valueOf();
            this.userInterface.showBasePriceRatio(handler, result);
        } catch (IOException ex) {
            this.userInterface.showError(dbBasePriceRatioTab.getName() + " fájlt nem lehetett megnyitni");
            throw new DatabaseException();
        } catch (CSVLineException ex) {
            this.userInterface.showError(dbBasePriceRatioTab.getName() + " fájl " + ex.getIndex()
                    + " sorában hiba történt a " + ex.getErrorOffset() + " pozicióban");
            throw new DatabaseException();
        }
        try{
            handler = new CSVFileHandler(
                    new CSVType[]{
                        CSVType.STRING,
                        CSVType.DATE,
                        CSVType.DATE,
                        CSVType.NUMBER
                    },
                    ";",
                    new SimpleDateFormat("MM-dd"),
                    this.customNumberFormat
                    );

            // TODO: átlapolt időtartamok szűrése
            intervals = handler.parse(dbIntervalTab);
            this.userInterface.showIntervals(handler, intervals);
        } catch (IOException ex) {
            this.userInterface.showError(dbIntervalTab.getName() + " fájlt nem lehetett megnyitni");
            throw new DatabaseException();
        } catch (CSVLineException ex) {
            this.userInterface.showError(dbIntervalTab.getName() + " fájl " + ex.getIndex()
                    + " sorában hiba történt a " + ex.getErrorOffset() + " pozicióban");
            throw new DatabaseException();
        }
        try {
            handler = new CSVFileHandler(
                        new CSVType[]{CSVType.STRING, CSVType.NUMBER},
                        ";",
                        this.customDateFormat,
                        this.customNumberFormat
                    );
            serviceTypes = handler.parse(dbServTypeTab);
            this.userInterface.showServiceTypes(handler, serviceTypes);
        } catch (IOException ex) {
            this.userInterface.showError(dbServTypeTab.getName() + " fájlt nem lehetett megnyitni");
            throw new DatabaseException();
        } catch (CSVLineException ex) {
            this.userInterface.showError(dbServTypeTab.getName() + " fájl " + ex.getIndex()
                    + " sorában hiba történt a " + ex.getErrorOffset() + " pozicióban");
            throw new DatabaseException();
        }
        try {
            handler = new CSVFileHandler(
                        new CSVType[]{
                            CSVType.STRING,
                            CSVType.CURRENCY,
                            CSVType.STRING,
                            CSVType.STRING
                        },
                        ";",
                        this.customNumberFormat
                    );
            apClasses = handler.parse(dbAPClassTab);
            this.userInterface.showApClasses(handler, apClasses);
        } catch (IOException ex) {
            this.userInterface.showError(dbAPClassTab.getName() + " fájlt nem lehetett megnyitni");
            throw new DatabaseException();
        } catch (CSVLineException ex) {
            this.userInterface.showError(dbAPClassTab.getName() + " fájl " + ex.getIndex()
                    + " sorában hiba történt a " + ex.getErrorOffset() + " pozicióban");
            throw new DatabaseException();
        }
        try {    
            handler = new CSVFileHandler(
                        new CSVType[]{CSVType.STRING, CSVType.STRING},
                        ";"
                    );
            aps = handler.parse(dbAPTab);
            this.userInterface.showAPs(handler, aps);

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
                this.userInterface.showError(dbOutputs.getName() + " nem egy könyvtár");
                throw new DatabaseException();
            }
        } else if (this.userInterface.askYesNo(dbOutputs.getName()
                + " mappa nem létezik. Létre akarja hozni?")){
            try {
                dbOutputs.mkdir();
                this.userInterface.showMessage("A mappa sikeresen léterejött.");
            } catch (SecurityException ex) {
                this.userInterface.showError("Nem lehetett létrehozni a " +
                        dbOutputs.getName() + " könyvtárat");
                throw new DatabaseException();
            }
        }else{
            throw new DatabaseException();
        }
        
    }
    
    protected List<CSVRecord> calculateContract(String name, List<CSVRecord> rows) {
        CSVFileHandler handler;
        
        String apName;
        Date from;
        Date to;
        String apServiceType;
        Float yearQuantity;
        Float dayQuantity;

        Float intervalPriceRatio;
        Float serviceTypePriceRatio;
        String apApClass;
        BigDecimal apClassPrice;
        Float quantity;
        String apClassUnit;

        Map<String, Float> apSummerLookUp = new LinkedHashMap<>();

        Float i;
        Float sum;

        List<CSVRecord> apResult;

        Map<String, Integer> resultHeader;
        
        for (CSVRecord record : rows) {

            intervalPriceRatio = basePriceRatio;
            serviceTypePriceRatio = null;
            apApClass = null;
            apClassPrice = null;
            quantity = null;
            apClassUnit = null;

            apName = (String) record.get("Pont kódja").valueOf();
            from = (Date) record.get("Érvényesség kezdete").valueOf();
            to = (Date) record.get("Érvényesség vége").valueOf();
            apServiceType = (String) record.get("Kapacitástípus").valueOf();
            dayQuantity = (Float) record.get("Mennyiség MJ/nap").valueOf();
            yearQuantity = (Float) record.get("Mennyiség MJ/óra").valueOf();

            if (!checkInterval(from, to)) {
                this.userInterface.showError(apName
                        + " hozzáférésiponthoz tartozó lekötés érvényessége "
                        + "kívül esik az érvényességi tartományon a "
                        + name + ".csv fájlban");
                continue;
            }

            for (CSVRecord interval : intervals) {

                if (inInterval(from, to,
                        (Date) interval.get("Időtartam kezdete").valueOf(),
                        (Date) interval.get("Időtartam vége").valueOf(), true)) {
                    intervalPriceRatio = (Float) interval.get("Ár százalék").valueOf();
                }
            }

            for (CSVRecord serviceType : serviceTypes) {
                if (apServiceType.equals((String) serviceType.get("Kapacitástípus").valueOf())) {
                    serviceTypePriceRatio = (Float) serviceType.get("Ár százalék").valueOf();
                }
            }

            if (serviceTypePriceRatio == null) {
                this.userInterface.showError(apName + " hozzáférésipont szolgáltatás típusához ("
                        + apServiceType + ") nem találhatóak adatok a " + name + ".csv fájlban");
                continue;
            }

            for (CSVRecord ap : aps) {
                if (apName.equals((String) ap.get("Pont kódja").valueOf())) {
                    apApClass = (String) ap.get("Pontcsoport").valueOf();
                }
            }

            if (apApClass == null) {
                this.userInterface.showError(apName + " hozáférési pont típus nem található a "
                        + name + ".csv fájlban");
                continue;
            }

            for (CSVRecord apClass : apClasses) {
                if (apApClass.equals((String) apClass.get("Pontcsoport").valueOf())) {
                    apClassPrice = (BigDecimal) apClass.get("Ár").valueOf();
                    apClassUnit = (String) apClass.get("Mértékegység").valueOf();
                }
            }

            try {
                quantity = (Float) record.get("Mennyiség " + apClassUnit).valueOf();
            } catch (IllegalArgumentException ex) {
                this.userInterface.showError(apApClass
                        + " hozzáférési típus mértékegységével ("
                        + apClassUnit + ") nincs megadva a lekötés mennyisége a "
                        + name + ".csv fájlban");
                continue;
            }

            if (apSummerLookUp.containsKey(apName)) {
                sum = apSummerLookUp.get(apName);
                sum += apClassPrice.floatValue() * intervalPriceRatio * serviceTypePriceRatio * quantity;
                apSummerLookUp.put(apName, sum);
            } else {
                apSummerLookUp.put(apName, apClassPrice.floatValue() * intervalPriceRatio * serviceTypePriceRatio * quantity);
            }

        }
        
        // Konvertálás : Map<String, Float>  => List<CSVRecord>
        
        apResult = new ArrayList<>();

        resultHeader = new LinkedHashMap<>();
        resultHeader.put("Pont kódja", 0);
        resultHeader.put("Összeg", 1);
        
        handler = new CSVFileHandler(
                new CSVType[]{CSVType.STRING, CSVType.CURRENCY},
                ";",
                this.customNumberFormat);
        handler.setHeader(resultHeader);
        
        sum = Float.valueOf(0);

        for (String key : apSummerLookUp.keySet()) {
            i = apSummerLookUp.get(key);
            sum += i;
            apResult.add(new CSVRecord(
                    new CSVField[]{
                        new CSVField<>(key),
                        new CSVField<>(BigDecimal.valueOf(i).setScale(
                                customNumberFormat.getMaximumFractionDigits() - 1,
                                BigDecimal.ROUND_HALF_UP
                            ).stripTrailingZeros()
                        )
                    },
                    handler));
        }
        apResult.add(new CSVRecord(
                new CSVField[]{
                    new CSVField<>("Összesen"),
                    new CSVField<>(BigDecimal.valueOf(sum).setScale(
                            customNumberFormat.getMaximumFractionDigits() - 1,
                            BigDecimal.ROUND_HALF_UP
                        ).stripTrailingZeros()
                    )
                },
                handler));
        
        return apResult;
    }
    
    protected List<CSVRecord> calculateContractByClass(String name, List<CSVRecord> rows) {
        CSVFileHandler handler;
        
        String apName;
        Date from;
        Date to;
        String apServiceType;
        Float yearQuantity;
        Float dayQuantity;

        Float intervalPriceRatio;
        Float serviceTypePriceRatio;
        String apApClass;
        BigDecimal apClassPrice;
        Float quantity;
        String apClassUnit;

        Map<String, Float> apClassSummerLookUp = new LinkedHashMap<>();

        Float i;
        Float sum;

        List<CSVRecord> apClassResult;

        Map<String, Integer> resultHeader;

        for (CSVRecord record : rows) {

            intervalPriceRatio = basePriceRatio;
            serviceTypePriceRatio = null;
            apApClass = null;
            apClassPrice = null;
            quantity = null;
            apClassUnit = null;

            apName = (String) record.get("Pont kódja").valueOf();
            from = (Date) record.get("Érvényesség kezdete").valueOf();
            to = (Date) record.get("Érvényesség vége").valueOf();
            apServiceType = (String) record.get("Kapacitástípus").valueOf();
            dayQuantity = (Float) record.get("Mennyiség MJ/nap").valueOf();
            yearQuantity = (Float) record.get("Mennyiség MJ/óra").valueOf();

            if (!checkInterval(from, to)) {
                this.userInterface.showError(apName
                        + " hozzáférésiponthoz tartozó lekötés érvényessége "
                        + "kívül esik az érvényességi tartományon a "
                        + name + ".csv fájlban");
                continue;
            }

            for (CSVRecord interval : intervals) {

                if (inInterval(from, to,
                        (Date) interval.get("Időtartam kezdete").valueOf(),
                        (Date) interval.get("Időtartam vége").valueOf(), true)) {
                    intervalPriceRatio = (Float) interval.get("Ár százalék").valueOf();
                }
            }

            for (CSVRecord serviceType : serviceTypes) {
                if (apServiceType.equals((String) serviceType.get("Kapacitástípus").valueOf())) {
                    serviceTypePriceRatio = (Float) serviceType.get("Ár százalék").valueOf();
                }
            }

            if (serviceTypePriceRatio == null) {
                this.userInterface.showError(apName + " hozzáférésipont szolgáltatás típusához ("
                        + apServiceType + ") nem találhatóak adatok a " + name +".csv fájlban");
                continue;
            }

            for (CSVRecord ap : aps) {
                if (apName.equals((String) ap.get("Pont kódja").valueOf())) {
                    apApClass = (String) ap.get("Pontcsoport").valueOf();
                }
            }

            if (apApClass == null) {
                this.userInterface.showError(apName + " hozáférési pont típus nem található a "
                        + name + ".csv fájlban");
                continue;
            }

            for (CSVRecord apClass : apClasses) {
                if (apApClass.equals((String) apClass.get("Pontcsoport").valueOf())) {
                    apClassPrice = (BigDecimal) apClass.get("Ár").valueOf();
                    apClassUnit = (String) apClass.get("Mértékegység").valueOf();
                }
            }

            try {
                quantity = (Float) record.get("Mennyiség " + apClassUnit).valueOf();
            } catch (IllegalArgumentException ex) {
                this.userInterface.showError(apApClass
                        + " hozzáférési típus mértékegységével ("
                        + apClassUnit + ") nincs megadva a lekötés mennyisége a "
                        + name + ".csv fájlban");
                continue;
            }

            if (apClassSummerLookUp.containsKey(apApClass)) {
                sum = apClassSummerLookUp.get(apApClass);
                sum += apClassPrice.floatValue() * intervalPriceRatio * serviceTypePriceRatio * quantity;
                apClassSummerLookUp.put(apApClass, sum);
            } else {
                apClassSummerLookUp.put(apApClass, apClassPrice.floatValue() * intervalPriceRatio * serviceTypePriceRatio * quantity);
            }
        }

            apClassResult = new ArrayList<>();
            resultHeader = new LinkedHashMap<>();
            resultHeader.put("Pontcsoport", 0);
            resultHeader.put("Összeg", 1);

            handler = new CSVFileHandler(
                    new CSVType[]{CSVType.STRING, CSVType.NUMBER},
                    ";",
                    this.customNumberFormat);
            handler.setHeader(resultHeader);

            sum = Float.valueOf(0);

            for (String key : apClassSummerLookUp.keySet()) {
                i = apClassSummerLookUp.get(key);
                sum += i;
                apClassResult.add(new CSVRecord(
                        new CSVField[]{
                            new CSVField<>(key),
                            new CSVField<>(BigDecimal.valueOf(i).setScale(
                                    customNumberFormat.getMaximumFractionDigits() - 1,
                                    BigDecimal.ROUND_HALF_UP
                                ).stripTrailingZeros()
                            )
                        },
                        handler));
            }
            apClassResult.add(new CSVRecord(
                    new CSVField[]{
                        new CSVField<>("Összesen"),
                        new CSVField<>(BigDecimal.valueOf(sum).setScale(
                                customNumberFormat.getMaximumFractionDigits() - 1,
                                BigDecimal.ROUND_HALF_UP
                            ).stripTrailingZeros()
                        )
                    },
                    handler));
            
            return apClassResult;
    }
    
    public void saveTable(final String name, List<CSVRecord> rows){
        CSVFileHandler handler;
        
        File outFile = new File(dbOutputs, name);
        
        if (!(rows.isEmpty() || rows == null)){
            handler = rows.get(0).getHandler();
            if (!outFile.exists() || this.userInterface.askYesNo(name
                    + " már létezik, felül akarja írni?")) {
                try {
                    handler.print(outFile, rows);
                } catch (IOException ex) {
                    this.userInterface.showError(name + " fájlt nem lehetett megnyitni");
                } catch (CSVFormatException ex) {
                    this.userInterface.showError("A kimeneti fájl " + ex.getLine() + " sorát nem sikerült formátumhoz igaítani");
                }
                
            }
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
            iFrom.add(Calendar.YEAR, dayFrom.get(Calendar.YEAR) - 1970);
            iTo.add(Calendar.YEAR, dayFrom.get(Calendar.YEAR) - 1970);
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
        PriceCalc calc = new PriceCalc();
        CSVFileHandler handler;
        
        calc.userInterface = new GUI(); //new BasicUI();
        calc.userInterface.start();
        // így a loadConfig-ban vagy parseArgs-ban is könyen lehetne állítani
        calc.customNumberFormat = setNumberFormat(',', ' ', 2, 3, true);
        calc.customDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            rootDir = new File(URLDecoder.decode(PriceCalc.class.getProtectionDomain()
                    .getCodeSource().getLocation().getPath(), "UTF-8")).getParent();
        } catch (UnsupportedEncodingException ex) {
            calc.userInterface.showError("Nem lehet dekódolni a program elérési útvonalát!");
        }
        
        // TODO: config fájl beállítása argumentumból
        //calc.parseArgs(args);
        
        String configFileName = File.separatorChar + "config.ini";
        
        try (InputStream configStream = new FileInputStream(rootDir + configFileName)){
            calc.loadConfig(configStream);
        } catch (IOException | NullPointerException ex1) {
            if (ex1 instanceof NullPointerException){
                calc.userInterface.showError("Hibás konfigurációs fájl: ."
                        + configFileName);
            }else if (ex1 instanceof IOException){
                calc.userInterface.showError("Nem található a ."
                        + configFileName + " fájl.");
            }
            
            try {
                if (calc.userInterface.askYesNo("Alapértelmezésbe akarja "
                        + "állítani a kofigurációs fájlt?")) {
                    calc.initConfig();
                    calc.userInterface.showMessage("A kofigurációs fájl"
                            + " sikeresen alapértelmezésbe állt.");
                } else {
                    System.exit(1);
                }
            } catch (IOException ex2) {
                calc.userInterface.showError("Nem lehet létrehozni a ."
                        + File.separatorChar + "config.ini fájt");
            } catch (NullPointerException ex2) {
                calc.userInterface.showError("Korrupt fájl: ." + defaultConfigFile);
                System.exit(2);
            }
        }
            
        // mehet-e tovább?
        
        try {
            calc.connectDB();
        } catch (DatabaseException ex) {
            System.exit(3);
        }
        
        handler = new CSVFileHandler(
                    new CSVType[]{
                        CSVType.STRING,
                        CSVType.DATE,
                        CSVType.DATE,
                        CSVType.STRING,
                        CSVType.NUMBER,
                        CSVType.NUMBER
                    },
                    ";",
                    calc.customDateFormat,
                    calc.customNumberFormat
                );
        
        for (File contract : calc.dbContracts.listFiles(new FileExtensionFilter("csv"))) {
            System.out.println(contract.getName());
            
            String name = contract.getName();
            int extPos = name.lastIndexOf(".");

            if (extPos != -1) {
                name = name.substring(0, extPos);
            }

            try {
                calc.userInterface.showContract(name, handler, handler.parse(contract), calc);
            } catch (IOException ex) {
                calc.userInterface.showError(contract.getName() + " fájlt nem lehetett megnyitni");
                throw new DatabaseException();
            } catch (CSVLineException ex) {
                calc.userInterface.showError(contract.getName() + " fájl " + ex.getIndex()
                        + " sorában hiba történt a " + ex.getErrorOffset() + " pozicióban");
                throw new DatabaseException();
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