package net.tiny.el;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Functions {

	public static String append(final String... args) {
		return join(null, args);
	}

	public static String repeat(String item, Integer loop) {
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<loop; i++) {
			sb.append(item);
		}
		return sb.toString();
	}

	public static String join(final String delim, final String... args) {
		boolean flag = (delim != null && !delim.isEmpty());
		StringBuilder sb = new StringBuilder();
		for(String arg : args) {
			if(null != arg && !arg.isEmpty()) {
				if(flag && sb.length() > 0) {
					sb.append(delim);
				}
				sb.append(arg);
			}
		}
		return sb.toString();
	}

    public static String string(final Date date, final String pattern) {
    	if(date == null) return "";
    	SimpleDateFormat sdf = new SimpleDateFormat(pattern);
    	return sdf.format(date);
    }

}
