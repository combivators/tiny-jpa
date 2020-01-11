package net.tiny.dao.test;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FileConvertor {

    public void chgTimestamp(File path) throws Exception {
        toUTF8(path, "*");
    }

    public void toUTF8(File path) throws Exception {
        toUTF8(path, "*");
    }

    public void toUTF8(File path, String regex) throws Exception {
        convertPath(path, regex, false);
    }

    public void toSJIS(File path) throws Exception {
        convertPath(path, "*", true);
    }

    public void toSJIS(File path, String regex) throws Exception {
        convertPath(path, regex, true);
    }

    void convertPath(File path, String suffix, boolean utf8) throws Exception {
        File[] files = path.listFiles(new SuffixFileFilter(suffix));
        for(File file : files) {
            convert(file, utf8);
        }
           files = path.listFiles(new DirectoryFilter());
           for(File file : files) {
               convertPath(file, suffix, utf8);
        }
    }

    void convertTsvPath(File path, String suffix) throws Exception {
        File[] files = path.listFiles(new SuffixFileFilter(suffix));
        for(File file : files) {
            convertTsv(file);
        }
           files = path.listFiles(new DirectoryFilter());
           for(File file : files) {
               convertTsvPath(file, suffix);
        }
    }

    private void convertTsv(File file) throws Exception {

        File dest = new File(file.getParentFile(), "~" + file.getName());
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(new FileInputStream(file)));
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(dest)));
        try {
            String line= reader.readLine();
            String pre = line.substring(0, 26);
             pre = pre.concat(CURRENT_TIME);
            writer.print(pre);
            while((line = reader.readLine()) != null) {
                writer.print("\n");
                writer.print(line);
            }
        } finally{
            reader.close();
            writer.close();
        }
        if(file.delete()) {
            if(dest.renameTo(file)) {
                //System.out.println(file.getName());
            }
        }
    }

    private void convert(File file, boolean utf8) throws Exception {
        File dest = new File(file.getParentFile(), "~" + file.getName());
        String inEndcoding  = "SJIS";
        String outEndcoding = "UTF-8";
        if(utf8) {
            inEndcoding  = "UTF-8";
            outEndcoding = "SJIS";
        }
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(new FileInputStream(file), inEndcoding));
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(dest), outEndcoding));
        try {
            String line;
            while((line = reader.readLine()) != null) {
                writer.println(line);
            }
        } finally{
            reader.close();
            writer.close();
        }
        if(file.delete()) {
            if(dest.renameTo(file)) {
                //System.out.println(file.getName());
            }
        }
    }

    void replacePath(File path, String suffix, boolean utf8, Replacement replacement) throws Exception {
        File[] files = path.listFiles(new SuffixFileFilter(suffix));
        for(File file : files) {
            replace(file, utf8, replacement);
        }
           files = path.listFiles(new DirectoryFilter());
           for(File file : files) {
               replacePath(file, suffix, utf8, replacement);
        }
    }

    private void replace(File file, boolean utf8, Replacement replacement) throws Exception {
        File dest = new File(file.getParentFile(), "~" + file.getName());
        String inEndcoding  = "SJIS";
        String outEndcoding = "SJIS";
        if(utf8) {
            inEndcoding  = "UTF-8";
            outEndcoding = "UTF-8";
        }
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(new FileInputStream(file), inEndcoding));
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(dest), outEndcoding));
        try {
            String line;
            while((line = reader.readLine()) != null) {
                writer.println(replacement.replace(line));
            }
        } finally{
            reader.close();
            writer.close();
        }
        if(file.delete()) {
            if(dest.renameTo(file)) {
                System.out.println(file.getName());
            }
        }
    }

    static class Replacement {
        List<String> target = new ArrayList<String>();
        List<String> replacement = new ArrayList<String>();

        public Replacement() {
        }

        public Replacement(List<String> target, List<String> replacement) {
            this.target.addAll(target);
            this.replacement.addAll(replacement);
            if(this.target.size() != this.replacement.size()) {
                throw new IllegalArgumentException("Must be same size. " +
                        this.target.size() + " <> "  +this.replacement.size());
            }
        }

        public void add(String org, String rep) {
            this.target.add(org);
            this.replacement.add(rep);
        }

        public String replace(String line) {
            if(target.isEmpty()) {
                return line;
            }
            String ret = new String(line);
            for(int i= 0; i<target.size(); i++) {
                ret = ret.replace(target.get(i), replacement.get(i));
            }
            return ret;
        }
    }

    static class SuffixFileFilter implements FileFilter {
        final String suffix;

        SuffixFileFilter(String suffix) {
            this.suffix = "." + suffix;
        }

        @Override
        public boolean accept(File file) {
            return (file.isFile() &&
                (".*".equals(suffix) || file.getName().endsWith(suffix)));
        }
    }


    static class DirectoryFilter implements FileFilter {
        @Override
        public boolean accept(File file) {
            return file.isDirectory();
        }
    }

    static String CURRENT_TIME = null;

    public static void main(String[] args) throws Exception {
        String suffix = "*";
        File path = null;
        for(int i=0; i<args.length; i++) {
            if(args[i].equalsIgnoreCase("-help") || args[i].equalsIgnoreCase("-h")) {
                usage();
            }
            if(args[i].equalsIgnoreCase("-ext")) {
                if(args.length > (i+1)) {
                    suffix = args[++i];
                } else {
                    usage();
                }
            } else if(args[i].equalsIgnoreCase("-ts")) {
                if(args.length > (i+1)) {
                    CURRENT_TIME = args[++i];
                } else {
                    usage();
                }
            } else {
                path = new File(args[i]);
            }
        }
        if(path == null || !path.exists()) {
            usage();
        } else {
            if(null == CURRENT_TIME) {
                SimpleDateFormat sdf = new  SimpleDateFormat("yyyyMMddHHmmss");
                CURRENT_TIME = sdf.format(new Date());
            }
            FileConvertor convertor = new FileConvertor();
            convertor.convertTsvPath(path, suffix);
        }
    }

    static void usage() {
        System.out.println("Usage: java net.tiny.dao.test.FileConvertor [option]");
        System.out.println("Option:");
        System.out.println("        -ext  File name suffix (Default '*')");
        System.out.println("        -ts   Change file timestampe (Default current time)");
        System.out.println("        -help This message.");
        System.exit(1);
    }
}
