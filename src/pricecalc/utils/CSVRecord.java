/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package pricecalc.utils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.text.ParseException;

import java.util.Map;

/**
 *
 * @author arsene
 */

public class CSVRecord {
    
    private final CSVFileHandler handler;
            
    protected final CSVField[] fields;

    public CSVRecord(final String[] values,
                     final CSVFileHandler handler) throws CSVException {
        
        this.handler = handler.clone();
        
        this.fields = parse(values);
    }
    
    public CSVRecord(final CSVField[] values, CSVFileHandler handler) {
        this.handler = handler;
        if (values.length != handler.getDataFormat().length())
            throw new IllegalArgumentException("Number of fielsd does not match length of handler's format");
        this.fields = values;
    }

    public CSVField get(final String key) {
        Map<String, Integer> keys = handler.getHeader();
        Integer index = keys.get(key);
        if (index == null) {
            throw new IllegalArgumentException(String.format(
                    "Mapping for %s not found, expected one of %s",
                    key, keys.keySet()));
        }
        try {
            return this.get(index.intValue());
        } catch (final ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException(String.format(
                    "Index for header '%s' is %d but CSVRecord only has %d values!",
                    key, index, Integer.valueOf(fields.length)));
        }
    }
    
    public CSVField get(final int i){
        return fields[i];
    }
    
    public void set(final String key, CSVField value) {
        fields[handler.getHeader().get(key)] = value;
    }
    
    public final CSVField[] parse(final String[] values) throws CSVException {
        String format = handler.getDataFormat();
        NumberFormat floatFormat = handler.getFloatFormat();
        SimpleDateFormat dateFormat = handler.getDateFormat();
        
        CSVField[] out = new CSVField[values.length];
        CSVField curr;
        char type;
        
        for (int i=0,l=0; i<values.length; l+=values[i++].length()) {
            try {
                type = format.charAt(i);
                try{
                    // TODO: CSVFileHandler.print újragondolni
                    switch (type) {
                        case 'S':
                            curr = new CSVField<>(values[i]);
                            break;
                        case 'N':
                            curr = new CSVField<>(floatFormat.parse(values[i]).floatValue());
                            break;
                        case 'C':
                            curr = new CSVField<>(BigDecimal.valueOf(
                                     floatFormat.parse(values[i]).doubleValue()));
                            break;
                        case 'D':
                            curr = new CSVField<>(dateFormat.parse(values[i]));
                            break;
                        default:
                            curr = null;
                    }
                    out[i] = curr;
                }catch (ParseException ex){
                    throw new CSVParseException(
                            "Couldn't parse field to type given type",
                            i, type,
                            l+ex.getErrorOffset());
                }
            } catch (StringIndexOutOfBoundsException ex) {
                throw new CSVException("Format index out of bounds", l);
            }

        }
        return out;
    }
    
    public String toStringField(String key){
        CSVField field = get(key);
        Map<String, Integer> keys = handler.getHeader();
        
        char type = handler.getDataFormat().charAt(keys.get(key));
        switch (type) {
            case 'S':
                return (String) field.valueOf();
            case 'N':
                return handler.getFloatFormat().format(field.valueOf());
            case 'C':
                return handler.getFloatFormat().format(
                        BigDecimal.valueOf((Float) field.valueOf()));
            case 'D':
                return handler.getDateFormat().format(field.valueOf());
            default:
                return new String();
        }
    }
    
    public String[] toStringArray(){
        String[] out = new String[fields.length];
        Map<Integer, String> rHeader = handler.reverseHeader();
        
        for (int i = 0; i < rHeader.size(); i++) {
            out[i] = toStringField(rHeader.get(i));
        }
        return out;
    }
    
}