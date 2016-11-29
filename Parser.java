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
                .map(Object::toString)
                .map(Parser::escapeString)
                .collect(Collectors.joining(" "));
    }

    public static String escapeString(String str) {
        return str.replaceAll("\\s", "~ "); // escape with tilde
    }

    public static String[] parseString(String str) {
        return Stream.of(str.split("(?<!~)\\s")) // negative lookbehind
                .map(s -> s.replace("~ ", " ")) // strip escape characters
                .toArray(String[]::new);
    }
}
