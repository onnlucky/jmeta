// copyright 2009 ActiveVideo; license: MIT; see license.txt
package jmeta;

import java.util.*;
import java.io.*;

public class Utils {
    public static String readFile(String f) throws IOException {
        InputStreamReader in = null;
        try {
            StringBuilder sb = new StringBuilder();
            in = new InputStreamReader(new FileInputStream(f), "UTF-8");

            int count = 0;
            char[] buf = new char[1024*10];
            while ((count = in.read(buf, 0, buf.length)) > -1)
                sb.append(buf, 0, count);

            return sb.toString();
        } finally {
            try {
                if (in != null) in.close();
            } catch (Exception ex) { }
        }
    }

    public static void writeFile(String f, String s) throws IOException {
        OutputStreamWriter out = null;
        try {
            out = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
            out.write(s);
        } finally {
            try {
                if (out != null) out.close();
            } catch (Exception ex) { }
        }
    }
    public static String escape(Object o) {
        String s = (String)o;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i ++) {
            switch (s.charAt(i)) {
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\f': sb.append("\\f"); break;
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                default: sb.append(s.charAt(i)); break;
            }
        }
        return sb.toString();
    }
    public static char unescape(char c) {
        switch(c) {
            case '"': return '"';
            case '\'': return '\'';
            case 'n': return '\n';
            case 'r': return '\r';
            case 't': return '\t';
            case 'f': return '\f';
            case '\\': return '\\';
        }
        throw new AssertionError("unknown escape char: \\"+ c);
    }
}

