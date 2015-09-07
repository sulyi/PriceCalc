/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package pricecalc.utils;

import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.text.ParseException;

import java.util.Map;

/**
 *
 * @author arsene
 */

public class CSVRecord {
    
    private final Map<String, Integer> keys;
    protected final String format;
    private final SimpleDateFormat dateFormat;
    private final NumberFormat floatFormat;
    
    private final CSVField[] fields;

    public CSVRecord(final String[] values,
                     final Map<String, Integer> keys,
                     final String dataFormat,
                     final SimpleDateFormat dateFormat,
                     final NumberFormat floatFormat) throws CSVException {
        
        this.keys = keys;
        this.format = dataFormat;
        this.dateFormat = dateFormat;
        this.floatFormat = floatFormat;

        this.fields = parse(values);
    }
    
    public CSVRecord(final CSVField[] values,
                     final Map<String, Integer> keys) {

        this.keys = keys;
        this.dateFormat = null;
        this.floatFormat = null;

        this.fields = values;
        
        String fieldFormat = "";
        String debug;
        
        for (CSVField f : fields){
            debug = f.getValueClass().getName();
            switch (debug){
                case "java.lang.String":
                    fieldFormat += 'S';
                    break;
                case "java.lang.Float":
                    fieldFormat += 'N';
                    break;
                case "java.util.Date":
                    fieldFormat += 'D';
            }
        }
        
        this.format = fieldFormat;
        
    }

    public CSVField get(final String key) {
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
    

    public final CSVField[] parse(final String[] values) throws CSVException {
        CSVField[] out = new CSVField[values.length];
        CSVField curr;
        char type;
        
        for (int i=0,l=0; i<values.length; l+=values[i++].length()) {
            try {
                type = format.charAt(i);
                try{
                    // TODO: CSVFileHandler.format újragondolni
                    switch (type) {
                        case 'S':
                            curr = new CSVField<>(values[i]);
                            break;
                        case 'N':
                            curr = new CSVField<>(floatFormat.parse(values[i]).floatValue());
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
}