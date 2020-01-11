package net.tiny.dao;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

public class SeparatedIterator<E> implements Iterator<E> {

    private static final Logger LOGGER = Logger.getLogger(SeparatedIterator.class.getName());

    private static final int MAX_ERROR_SIZE = 1000;

    private final LineNumberReader reader;
    private final char delimiter;
    private final Class<E> entityType;
    private final Map<String, Set<ConstraintViolation<?>>> errors;
    private final EntityParser parser;
    private int row = 0;
    private E last = null;
    private String lastLine = null;
    private Validator validator = null;
    private boolean skip = true;


    private SeparatedIterator(Class<E> type, LineNumberReader reader,  char delimiter, EntityParser parser, boolean validating, Map<String, Set<ConstraintViolation<?>>> errors) {
        this.entityType = type;
        this.reader = reader;
        this.delimiter = delimiter;
        this.parser = parser;
        this.errors = errors;
        if(validating) {
            ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
            this.validator = validatorFactory.getValidator();
        }
    }

    @Override
    public boolean hasNext() {
        try {
            this.last = getLast();
            return (null != this.last);
        } catch (IOException ex) {
            LOGGER.warning(String.format("Line %1$d : '%2$s' error cause:%3$s ", this.row, this.lastLine, ex.getMessage()));
            return false;
        }
    }

    @Override
    public E next() {
        return this.last;
    }

    private E getLast() throws IOException {
        while( null != (this.lastLine = getLastLine()) ) {
            this.last = getLast(this.lastLine);
            if(this.last != null) {
                return this.last;
            }
        }
        return null;
    }

    private String getLastLine() throws IOException {
        String line;
        String nextLine;
        if( null != (line = reader.readLine()) ) {
            row++;
            while ( SeparatedValues.brokenLine(line, this.delimiter)
                    && null != ( nextLine = reader.readLine() ) ) {
                row++;
                line = line.concat("\n").concat(nextLine);
            }
        }
        return line;
    }

    private E getLast(String line) {
        List<String> values = SeparatedValues.split(this.lastLine, this.delimiter);
        try {
            return parser.parse(this.entityType, values, this.validator);
        } catch (ConstraintViolationException ex) {
            if(this.errors != null) {
                this.errors.put(line, ex.getConstraintViolations());
                if(this.errors.size() >= MAX_ERROR_SIZE) {
                    throw new RuntimeException("Too many errors(>1000).");
                }
            }
            if(this.skip) {
                return null;
            } else {
                String msg = String.format("Vialidation error '%1$s' on %2$d line : '%3$s'", ex.getMessage(), this.row, values.toString());
                LOGGER.log(Level.SEVERE, msg, ex);
                throw ex;
            }
        } catch (RuntimeException ex) {
            String msg = String.format("Parser error '%1$s' on %2$d line : '%3$s'", ex.getMessage(), this.row, values.toString());
            LOGGER.log(Level.SEVERE, msg, ex);
            throw ex;
        } catch (Exception ex) {
            String msg = String.format("Unexpected error '%1$s' on %2$d line : '%3$s'", ex.getMessage(), this.row, values.toString());
            LOGGER.log(Level.SEVERE, msg, ex);
            throw new RuntimeException(msg);
        }
    }

    public static <T> Iterator<T> parse(Reader reader, Class<T> classType) throws IOException {
        return parse(reader, classType, null);
    }

    public static <T> Iterator<T> parse(Reader reader, Class<T> classType, Map<String, Set<ConstraintViolation<?>>> errors) throws IOException {
        LineNumberReader lineReader;
        if(reader instanceof LineNumberReader) {
            lineReader = LineNumberReader.class.cast(reader);
        } else {
            lineReader = new LineNumberReader(reader);
        }
        String line = lineReader.readLine();
        if(null == line) {
            return null;
        }
        String[] columns = SeparatedValues.csv(line);
        EntityParser parser = new EntityParser(classType, columns);
        return parse(lineReader, classType, SeparatedValues.Type.CSV, parser, 0, errors);
    }

    public static <T> Iterator<T> parse(Reader reader, Class<T> classType, int skips, Map<String, Set<ConstraintViolation<?>>> errors) throws IOException {
        EntityParser parser = new EntityParser(classType);
        return parse(reader, classType, SeparatedValues.Type.CSV, parser, skips, errors);
    }


    public static <T> Iterator<T> parse(Reader reader, Class<T> classType, SeparatedValues.Type type, int skips, Map<String, Set<ConstraintViolation<?>>> errors) throws IOException {
        EntityParser parser = new EntityParser(classType);
        return parse(reader, classType, type, parser, skips, errors);
    }

    public static <T> Iterator<T> parse(Reader reader, Class<T> classType, SeparatedValues.Type type, String[] names, int skips, Map<String, Set<ConstraintViolation<?>>> errors) throws IOException {
        EntityParser parser = new EntityParser(classType, names, true);
        return parse(reader, classType, type, parser, skips, errors);
    }


    static <T> Iterator<T> parse(Reader reader, Class<T> classType, SeparatedValues.Type type, EntityParser parser, int skips, Map<String, Set<ConstraintViolation<?>>> errors) throws IOException {
        char delimiter = SeparatedValues.CSV_BREAK;
        switch(type) {
        case TSV:
            delimiter = SeparatedValues.TSV_BREAK;
            break;
        case CSV:
        default:
            delimiter = SeparatedValues.CSV_BREAK;
            break;
        }

        LineNumberReader lineReader;
        if(reader instanceof LineNumberReader) {
            lineReader = LineNumberReader.class.cast(reader);
        } else {
            lineReader = new LineNumberReader(reader);
        }
        int count = 0;
        while(skips > 0 && count<skips && (null != lineReader.readLine())) {
            ++count;
        }
        return new SeparatedIterator<T>(classType, lineReader, delimiter, parser, (null != errors), errors);
    }
}
