/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package pricecalc.utils;


import java.io.File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
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

public class CSVFileHandler {
    
    private String format;
    private SimpleDateFormat dateFormat;
    private NumberFormat floatFormat;
    private String sep;
    private Map<String, Integer> header;
    
    public CSVFileHandler() {
        this(null, null, null, null);
    }
    
    public CSVFileHandler(final String dataFormat, final String delimiter) {
        this(dataFormat, delimiter, null, null);
    }
    
    public CSVFileHandler(final String dataFormat, final String delimiter, final SimpleDateFormat dateFormat) {
        this(dataFormat, delimiter, dateFormat, null);
    }
    
    public CSVFileHandler(final String dataFormat, final String delimiter, final NumberFormat floatFormat) {
        this(dataFormat, delimiter, null, floatFormat);
    }
    
    public CSVFileHandler(final String dataFormat,
                     final String delimiter,
                     final SimpleDateFormat dateFormat,
                     final NumberFormat floatFormat) {
        if (delimiter == null)
            throw new IllegalArgumentException("Invalid CSV delimiter: null");
        if (delimiter.length() > 1 || delimiter.isEmpty())
            throw new IllegalArgumentException("Invalid CSV delimiter: " + delimiter);
        
        if (dateFormat == null && dataFormat.contains("D"))
            throw new IllegalArgumentException("Date format is not set, while "+ dataFormat+" contains date");
        if (floatFormat == null && dataFormat.contains("N"))
            throw new IllegalArgumentException("Number format is not set, while " + dataFormat + " contains number");
        
        // TODO: format ellenörzése, mást is tartalmaz mint S,N vagy D
        
        this.format = dataFormat;
        this.sep = delimiter;
        this.dateFormat = dateFormat;
        this.floatFormat = floatFormat;
    }
    
    public List<CSVRecord> parse(File source) throws IOException, CSVLineException{
        BufferedReader sourceReader = new BufferedReader(new FileReader(source));
        
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
                
                out.add(new CSVRecord(rawFields,
                                      this.header,
                                      this.format,
                                      this.dateFormat,
                                      this.floatFormat));
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
    
    public void print(List<CSVRecord> data, File target) throws IOException, CSVFormatException{
        String[] lines = new String[data.size()+1];
        String line;
        
        FileWriter fileWriter = new FileWriter(target.getAbsoluteFile());
        BufferedWriter targetWriter = new BufferedWriter(fileWriter);
        
        Map<Integer, String> rHeader = reverseHeader();
        
        line = rHeader.get(0);
        for (int i=1; i<rHeader.size(); i++) {
            line += this.sep+rHeader.get(i);
        }
        lines[0] = line;
        
        CSVRecord record;
        char type;
        
        for(int i=0;i<data.size();i++){
            record = data.get(i);
            if (record.format == null ? this.format != null : !record.format.equals(this.format)){
               throw new CSVFormatException("Line does not match format of file", i);
            }
            type = this.format.charAt(0);
            switch (type) {
                case 'S':
                    line = (String) record.get(rHeader.get(0)).valueOf();
                    break;
                case 'N':
                    line = floatFormat.format(record.get(rHeader.get(0)).valueOf());
                    break;
                case 'D':
                    line = dateFormat.format(record.get(rHeader.get(0)).valueOf());
            }
            for(int j=1; j<rHeader.size(); j++){
                line += this.sep;
                type = this.format.charAt(j);
                switch (type){
                    case 'S':
                        line += record.get(rHeader.get(j)).valueOf();
                        break;
                    case 'N':
                        line += floatFormat.format(record.get(rHeader.get(j)).valueOf());
                        break;
                    case 'D':
                        line += dateFormat.format(record.get(rHeader.get(j)).valueOf());
                }
            }
            lines[i+1] = line;
        }
        
        for (int i=0; i<lines.length; i++) {
            targetWriter.write(lines[i], 0, lines[i].length());
            // rendszer függő
            targetWriter.newLine();
        }
        targetWriter.close();
    }
    
    private Map<Integer, String> reverseHeader(){
        Map<Integer, String> rHeader = new LinkedHashMap<>();
        
        for(int i=0;i<this.header.size();i++){
            for (String key : this.header.keySet()){
                if (this.header.get(key) == i){
                    rHeader.put(Integer.valueOf(i), key);
                    break;
                }
            }
        }
        
        return rHeader;
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
            
            if (headerMap.size() > this.format.length())
                throw new IllegalArgumentException(String.format(
                            "Size of header is %d, but only %d given in format",
                            headerMap.size(), this.format.length()
                        ));
            
            this.header = headerMap;
        }
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

    public String getDataFormat() {
        return format;
    }

    public void setDataFormat(String format) {
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

    public void setHeader(Map<String, Integer> header) {
        this.header = header;
    }
    
}