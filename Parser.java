/* WEBSERVER Parser.java
 * EE422C Project 7 submission by
 * Anthony Bauer
 * amb6869
 * 16480
 * Grant Uy
 * gau84
 * 16480
 * Slip days used: <1>
 * Fall 2016
 */
package assignment7;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Parser {
    public static String packageStrings(Object... arr) {
        return packageStrings(Arrays.asList(arr));
    }

    public static String packageStrings(List<Object> list) {
        return list.stream()
                .map(o -> ((o instanceof List) ? ((List)o).stream().map(e -> escapeString(e.toString())).collect(Collectors.joining(" ")).toString() : escapeString(o.toString())))
                .collect(Collectors.joining(" "));
    }

    public static String escapeString(String str) {
        return str.replaceAll(" ", "~ "); // escape with tilde
    }

    public static String[] parseString(String str) {
        return Stream.of(str.split("(?<!~) ")) // negative lookbehind
                .map(s -> s.replaceAll("~ ", " ")) // strip escape characters
                .toArray(String[]::new);
    }

    public static String cleanString(String str) {
        return str.replaceAll("[^A-Za-z0-9_ ]","");
    }
}
