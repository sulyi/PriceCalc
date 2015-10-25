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
    
    public CSVRecord(final CSVField[] values, final CSVFileHandler handler) {
        this.handler = handler.clone();
        if (values.length != handler.getDataFormat().length)
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
        CSVType[] format = handler.getDataFormat();
        NumberFormat floatFormat = handler.getFloatFormat();
        SimpleDateFormat dateFormat = handler.getDateFormat();
        
        CSVField[] out = new CSVField[values.length];
        CSVField curr;
        CSVType type;
        
        for (int i=0,l=0; i<values.length; l+=values[i++].length()) {
            try {
                type = format[i];
                try{
                    // TODO: CSVFileHandler.print újragondolni
                    switch (type) {
                        case STRING:
                            curr = new CSVField<>(values[i]);
                            break;
                        case NUMBER:
                            curr = new CSVField<>(floatFormat.parse(values[i]).floatValue());
                            break;
                        case CURRENCY:
                            curr = new CSVField<>(BigDecimal.valueOf(
                                     floatFormat.parse(values[i]).doubleValue()));
                            break;
                        case DATE:
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
    
    public String toStringField(String key) {
        Map<String, Integer> keys = handler.getHeader();
        Integer index = keys.get(key);
        return toStringField(index);
    }
    
    public String toStringField(int i){
        CSVField field = get(i);
        
        CSVType type = handler.getDataFormat()[i];
        switch (type) {
            case STRING:
                return (String) field.valueOf();
            case NUMBER:
                return handler.getFloatFormat().format(field.valueOf());
            case CURRENCY:
                return handler.getFloatFormat().format(
                        ((BigDecimal) field.valueOf()).doubleValue());
            case DATE:
                return handler.getDateFormat().format(field.valueOf());
            default:
                return new String();
        }
    }
    
    public String[] toStringArray(){
        String[] out = new String[fields.length];
        String[] rHeader = handler.getHeaderToStringArray();
        
        for (int i = 0; i < rHeader.length; i++) {
            out[i] = toStringField(rHeader[i]);
        }
        return out;
    }
    
}