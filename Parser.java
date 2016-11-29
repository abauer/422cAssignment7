package assignment7;

import java.util.stream.Stream;

public class Parser {
    public static String escapeString(String str) {
        return str.replaceAll("\\s", "~ "); // escape with tilde
    }

    public static String[] parseString(String str) {
        return Stream.of(str.split("(?<!~)\\s")) // negative lookbehind
                .map(s -> s.replace("~ ", " ")) // strip escape characters
                .toArray(String[]::new);
    }
}
