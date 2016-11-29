package assignment7;

import java.util.stream.Stream;

public class Parser {
    public String escapeString(String str) {
        return str.replaceAll("\\s", "~ "); // escape with tilde
    }

    public String[] parseString(String str) {
        return Stream.of(str.split("(?<!~)\\s")) // negative lookbehind
                .map(s -> s.replace("~ ", " ")) // strip escape characters
                .toArray(String[]::new);
    }
}
