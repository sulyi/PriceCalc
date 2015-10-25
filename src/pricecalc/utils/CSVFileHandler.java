/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pricecalc.utils;


import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

/**
 *
 * @author arsene
 */

public class CSVFileHandler implements Cloneable {
    
    private CSVType[] format;
    private SimpleDateFormat dateFormat;
    private NumberFormat floatFormat;
    private String sep;
    private Map<String, Integer> header;
    private File file;

    public CSVFileHandler() {
        this(null, null, null, null, null);
    }
    
    public CSVFileHandler(final CSVType[] dataFormat,
                          final String delimiter,
                          final File source) {
        this(dataFormat, delimiter, null, null, source);
    }
    
    public CSVFileHandler(final CSVType[] dataFormat,
                          final String delimiter,
                          final SimpleDateFormat dateFormat,
                          final File source) {
        this(dataFormat, delimiter, dateFormat, null, source);
    }
    
    public CSVFileHandler(final CSVType[] dataFormat,
                          final String delimiter,
                          final NumberFormat floatFormat,
                          final File source) {
        this(dataFormat, delimiter, null, floatFormat, source);
    }
    
    public CSVFileHandler(final CSVType[] dataFormat,
                          final String delimiter) {
        this(dataFormat, delimiter, null, null, null);
    }

    public CSVFileHandler(final CSVType[] dataFormat,
                          final String delimiter,
                          final SimpleDateFormat dateFormat) {
        this(dataFormat, delimiter, dateFormat, null, null);
    }

    public CSVFileHandler(final CSVType[] dataFormat,
                          final String delimiter,
                          final NumberFormat floatFormat) {
        this(dataFormat, delimiter, null, floatFormat, null);
    }
    
    public CSVFileHandler(final CSVType[] dataFormat,
                          final String delimiter,
                          final SimpleDateFormat dateFormat,
                          final NumberFormat floatFormat
            ) {
        this(dataFormat, delimiter, dateFormat, floatFormat, null);
    }
    
    public CSVFileHandler(final CSVType[] dataFormat,
                          final String delimiter,
                          final SimpleDateFormat dateFormat,
                          final NumberFormat floatFormat,
                          final File source) {
        if (delimiter == null)
            throw new IllegalArgumentException("Invalid CSV delimiter: null");
        if (delimiter.length() > 1 || delimiter.isEmpty())
            throw new IllegalArgumentException("Invalid CSV delimiter: " + delimiter);
        
        if (dateFormat == null && Arrays.asList(dataFormat).contains(CSVType.DATE))
            throw new IllegalArgumentException("Date format is not set, while "+ dataFormat+" contains date");
        if (floatFormat == null && Arrays.asList(dataFormat).contains(CSVType.NUMBER))
            throw new IllegalArgumentException("Number format is not set, while " + dataFormat + " contains number");
        
        // TODO: format ellenörzése, mást is tartalmaz mint S,N vagy D
        
        this.file = source;
        this.format = dataFormat;
        this.sep = delimiter;
        this.dateFormat = dateFormat;
        this.floatFormat = floatFormat;
    }
    
    public List<CSVRecord> parse() throws IOException, CSVLineException{
        BufferedReader sourceReader = new BufferedReader(new InputStreamReader(
                new FileInputStream(this.file), "UTF-8"));
        
        // rendszer független, akár egy fájlon belül is változhat a sor vég, akkor is müködik
        // elvileg
        if (this.header == null)
            initHeader(sourceReader.readLine());
        else
            sourceReader.readLine();
        
        List<CSVRecord> out = new ArrayList<>();
        String line;
        String[] rawFields;
        
        for (int i = 0; (line = sourceReader.readLine()) != null; i++){
            try {
                
                if (line.contains(this.sep)) {
                    rawFields = line.split(Pattern.quote(this.sep),-1);
                } else {
                    if (line == null || line.isEmpty()) {
                        throw new IllegalArgumentException(String.format("Line %d is empty",i));
                    } else {
                        rawFields = new String[]{line};
                    }
                }
                
                out.add(new CSVRecord(rawFields, this));
            } catch (CSVParseException ex) {
                throw new CSVLineException(String.format(
                        "Couldn't parse line, because field %d can't be parsed to %c",
                        ex.getIndex(),
                        ex.getType()),
                    i,
                    ex.getErrorOffset());
            } catch (CSVException ex) {
                throw new CSVLineException(
                        "Line had more fields than expected",
                        i,
                        ex.getErrorOffset());
            }
            
        }
        
        sourceReader.close();
        return out;
    }
    
    public void print(List<CSVRecord> data)
            throws IOException, CSVFormatException{
        BufferedWriter targetWriter = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(this.file), "UTF-8"));
        
        Map<Integer, String> rHeader = reverseHeader();
        
        String line = rHeader.get(0);
        for (int i=1; i<rHeader.size(); i++) {
            line += this.sep+rHeader.get(i);
        }
        targetWriter.write(line, 0, line.length());
        
        CSVRecord record;
        String[] recordStingFields;
        char type;
        
        for(int i=0;i<data.size();i++){
            record = data.get(i);
            if (record.fields.length != this.format.length){
               throw new CSVFormatException("Line does not match format of file", i);
            }
            recordStingFields = record.toStringArray();
            line = recordStingFields[0];
            for (int j=1; j< recordStingFields.length; j++){
                line += this.sep;
                line += recordStingFields[j];
            }
            targetWriter.newLine();
            targetWriter.write(line, 0, line.length());
        }
        
        targetWriter.close();
    }
    
    private void initHeader(String headerStr) {
        String[] keys;
        
        if (headerStr.contains(this.sep)) {
            keys = headerStr.split(Pattern.quote(this.sep));
        } else {
            if (headerStr == null || headerStr.isEmpty()){
                throw new IllegalArgumentException("Header is empty");
            } else{
                keys = new String[]{headerStr};
            }
        }
        if (keys != null){
            Map<String, Integer> headerMap = new LinkedHashMap<>();
            for(int i=0;i<keys.length;i++){
                String key = keys[i];
                boolean containsKey = headerMap.containsKey(key);
                boolean emptyKeys = key == null || key.trim().isEmpty();
                if (containsKey
                        && !emptyKeys) {
                    throw new IllegalArgumentException("The header contains a duplicate key: \"" + key
                            + "\" in " + Arrays.toString(keys));
                }
                headerMap.put(key, Integer.valueOf(i));
            }
            
            if (headerMap.size() > this.format.length)
                throw new IllegalArgumentException(String.format(
                            "Size of header is %d, but only %d given in format",
                            headerMap.size(), this.format.length
                        ));
            
            this.header = headerMap;
        }
    }
    
    private Map<Integer, String> reverseHeader() {
        Map<Integer, String> rHeader = new LinkedHashMap<>();

        for (int i = 0; i < this.header.size(); i++) {
            for (String key : this.header.keySet()) {
                if (this.header.get(key) == i) {
                    rHeader.put(Integer.valueOf(i), key);
                    break;
                }
            }
        }

        return rHeader;
    }
    
    public SimpleDateFormat getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(SimpleDateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    public NumberFormat getFloatFormat() {
        return floatFormat;
    }

    public void setFloatFormat(NumberFormat floatFormat) {
        this.floatFormat = floatFormat;
    }

    public CSVType[] getDataFormat() {
        return format;
    }

    public void setDataFormat(CSVType[] format) {
        this.header = null;
        this.format = format;
    }

    public String getDelimiter() {
        return sep;
    }

    public void setDelimiter(String delimiter) {
        if (delimiter == null) {
            throw new IllegalArgumentException("Invalid CSV delimiter: null");
        }
        if (delimiter.length() > 1 || delimiter.isEmpty()) {
            throw new IllegalArgumentException("Invalid CSV delimiter: " + delimiter);
        }
        this.sep = delimiter;
    }

    public Map<String, Integer> getHeader() {
        return header;
    }
    
    public String[] getHeaderToStringArray(){
        String[] result = new String[header.size()];
        Integer[] indicies = new Integer[header.size()];
        Map<Integer, String> rHeader = reverseHeader();
        
        rHeader.keySet().toArray(indicies);
        Arrays.sort(indicies);
        
        for (int i=0; i<indicies.length; i++){
            result[i] = rHeader.get(indicies[i]);
        }
        
        return result;
    }
    
    public void setHeader(Map<String, Integer> header) {
        this.header = header;
    }
    
    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    public CSVFileHandler clone() {
        try {
            return (CSVFileHandler) super.clone();
        } catch (CloneNotSupportedException ex) {
            return null;
        }
    }
}