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
        
        this.handler = handler;
        this.fields = parse(values);
    }
    
    public CSVRecord(final CSVField[] values, final CSVFileHandler handler) {
        this.handler = handler;
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
    
    public void set(final String key, final CSVField value) {
        set(handler.getHeader().get(key), value);
    }
    
    public void set(final int i, final CSVField value) {
        fields[i] = value;
    }
    
    public void set(final String key, final String value) throws CSVParseException {
        set(handler.getHeader().get(key), value);
    }
    
    public void set(final int i, final String value) throws CSVParseException {
        fields[i] = parse(i, value);
    }
    
    private CSVField parse(final int i, final String value) throws CSVParseException{
        CSVType[] format = handler.getDataFormat();
        NumberFormat floatFormat = handler.getFloatFormat();
        SimpleDateFormat dateFormat = handler.getDateFormat();
        
        CSVType type = format[i];
        try {
            switch (type) {
                case STRING:
                    return new CSVField<>(value);
                case NUMBER:
                    return new CSVField<>(floatFormat.parse(value).floatValue());
                case CURRENCY:
                    return new CSVField<>(BigDecimal.valueOf(
                            floatFormat.parse(value).doubleValue()).setScale(
                            handler.getFloatFormat().getMaximumFractionDigits(),
                            BigDecimal.ROUND_HALF_UP).stripTrailingZeros());
                case DATE:
                    return new CSVField<>(dateFormat.parse(value));
                default:
                    return null;
            }
        } catch (ParseException ex) {
            throw new CSVParseException(
                    "Couldn't parse field to type given type",
                    i, type,
                    ex.getErrorOffset());
        }
    }
    
    private CSVField[] parse(final String[] values) throws CSVException {
        
        
        CSVField[] out = new CSVField[values.length];
        CSVField curr;
        
        for (int i=0,l=0; i<values.length; l+=values[i++].length()) {
            try {
                out[i] = parse(i, values[i]);
            } catch (CSVParseException ex) {
                throw new CSVParseException(
                        "Couldn't parse field to type given type",
                        i, ex.getType(),
                        l + ex.getErrorOffset());

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

    public CSVFileHandler getHandler() {
        return handler;
    }
    
}