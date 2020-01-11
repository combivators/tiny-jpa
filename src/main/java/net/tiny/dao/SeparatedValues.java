package net.tiny.dao;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SeparatedValues {

	public static enum Type {
		CSV,
		TSV
	}

	public static final char CSV_BREAK    = ',';
	public static final char TSV_BREAK    = '\t';
	public static final char VALUE_BOUNDARY = '"';

	protected static final String QUOTE = "\"";
	protected static final String ESCAPED_QUOTE = "\"\"";
	protected static final char[] CHARACTERS_THAT_MUST_BE_QUOTED = { ',', '"', '\t', '\r', '\n' };

	public static String escape(String value) {
		if ( value.contains( QUOTE ) )
			value = value.replace( QUOTE, ESCAPED_QUOTE );
		for(char c : CHARACTERS_THAT_MUST_BE_QUOTED) {
	        if ( value.indexOf(c) > -1 ) {
	        	value = QUOTE + value + QUOTE;
	        	break;
	        }
		}
        return value;
	}

	public static String unescape(String value) {
        if ( value.startsWith( QUOTE ) && value.endsWith( QUOTE ) ) {
        	value = value.substring( 1, value.length() - 1 );
        	if ( value.contains( ESCAPED_QUOTE ) ) {
        		value = value.replace( ESCAPED_QUOTE, QUOTE );
        	}
        }
        return value;
	}

	private String line = null;
	private List<String> array = new ArrayList<>();
	private char spliter = CSV_BREAK;

	SeparatedValues(String line, char delimiter) {
		if(null == line) {
			throw new IllegalArgumentException("Input argument is null");
		}
		this.spliter = delimiter;
		this.array = split(line, spliter);
		this.line = line;
	}

	public SeparatedValues(String line, Type type) {
		if(null == line || null == type) {
			throw new IllegalArgumentException("Input argument is null");
		}
		switch(type) {
		case TSV:
			this.spliter = TSV_BREAK;
			break;
		case CSV:
		default:
			this.spliter = CSV_BREAK;
			break;
		}
		this.array = split(line, spliter);
		this.line = line;
	}

	public SeparatedValues(String line) {
		this(line, Type.CSV);
	}

	public String[] toArray() {
		return this.array.toArray(new String[this.array.size()]);
	}

	public String get(int index) {
		return (String)this.array.get(index);
	}

	public int size() {
		return this.array.size();
	}

	public void add(String item) {
		this.array.add(item);
	}

	public void add(int index, String item) {
		this.array.add(index, item);
	}

	public String toString() {
		return this.line;
	}

	public static Iterator<List<String>> parse(Reader reader, Type type) throws IOException {
		char delimiter = CSV_BREAK;
		switch(type) {
		case TSV:
			delimiter = TSV_BREAK;
			break;
		case CSV:
		default:
			delimiter = CSV_BREAK;
			break;
		}
		LineNumberReader lineReader;
		if(reader instanceof LineNumberReader) {
			lineReader = LineNumberReader.class.cast(reader);
		} else {
			lineReader = new LineNumberReader(reader);
		}
		return new ListRecords(lineReader, delimiter);
	}

	public static Iterator<SeparatedValues> parse(Reader reader, char delimiter) throws IOException {
		LineNumberReader lineReader;
		if(reader instanceof LineNumberReader) {
			lineReader = LineNumberReader.class.cast(reader);
		} else {
			lineReader = new LineNumberReader(reader);
		}
		return new Records(lineReader, delimiter);
	}

	public static String[] csv(final String text) {
		List<String> list = split(text, CSV_BREAK);
		return list.toArray(new String[list.size()]);
	}

	public static String[] tsv(final String text) {
		List<String> list = split(text, TSV_BREAK);
		return list.toArray(new String[list.size()]);
	}

	static List<String> split(final String text, final char delimiter) {
		List<String> list = new ArrayList<>();
		StringBuilder s = new StringBuilder();
	    boolean escaped  = false;
	    boolean inQuotes = false;
	    for (char c : text.toCharArray())  {
	        if (c == delimiter && !inQuotes) {
	            list.add(s.toString());
	            s.setLength(0);
	        } else if (c == '\\' && !escaped) {
	            escaped = true;
	        } else if (c == VALUE_BOUNDARY && !escaped) {
	            inQuotes = !inQuotes;
	        } else {
	            escaped = false;
	            s.append(c);
	        }
	    }
	    list.add(s.toString());
	    return list;
	}

    static boolean brokenLine(final String text, final char delimiter) {
		StringBuilder s = new StringBuilder();
	    boolean escaped  = false;
	    boolean inQuotes = false;
	    for (char c : text.toCharArray())  {
	        if (c == delimiter && !inQuotes) {
	            s.setLength(0);
	        } else if (c == '\\' && !escaped) {
	            escaped = true;
	        } else if (c == VALUE_BOUNDARY && !escaped) {
	            inQuotes = !inQuotes;
	        } else {
	            escaped = false;
	            s.append(c);
	        }
	    }
	    return inQuotes;
	}

	static class ListRecords implements Iterator<List<String>> {
		private final LineNumberReader reader;
		private final char delimiter;
		private Throwable error;
		private String lastLine;

		public ListRecords(LineNumberReader reader,  char delimiter) {
			this.reader = reader;
			this.delimiter = delimiter;
		}

		@Override
		public boolean hasNext() {
			try {
				String nextLine;
				if( null != (this.lastLine = reader.readLine()) ) {
					while ( brokenLine(this.lastLine, this.delimiter)
							&& null != ( nextLine = reader.readLine() ) ) {
						this.lastLine = this.lastLine.concat("\n").concat(nextLine);
					}
					return true;
				} else {
					return false;
				}

			} catch (IOException ex) {
				this.error = ex;
				return false;
			}
		}

		@Override
		public List<String> next() {
			if(null == this.lastLine)
				return null;
			SeparatedValues values = new SeparatedValues(this.lastLine, delimiter);
			return values.array;
		}

		public boolean hasError() {
			return this.error != null;
		}

		public Throwable error() {
			return this.error;
		}
	}

	static class Records implements Iterator<SeparatedValues> {
		private final LineNumberReader reader;
		private final char delimiter;
		private Throwable error;
		private String lastLine;

		public Records(LineNumberReader reader,  char delimiter) {
			this.reader = reader;
			this.delimiter = delimiter;
		}

		@Override
		public boolean hasNext() {
			try {
				String nextLine;
				if( null != (this.lastLine = reader.readLine()) ) {
					while ( brokenLine(this.lastLine, this.delimiter)
							&& null != ( nextLine = reader.readLine() ) ) {
						this.lastLine = this.lastLine.concat("\n").concat(nextLine);
					}
					return true;
				} else {
					return false;
				}

			} catch (IOException ex) {
				this.error = ex;
				return false;
			}
		}

		@Override
		public SeparatedValues next() {
			if(null == this.lastLine)
				return null;
			return new SeparatedValues(this.lastLine, delimiter);
		}

		public boolean hasError() {
			return this.error != null;
		}

		public Throwable error() {
			return this.error;
		}
	}

}
