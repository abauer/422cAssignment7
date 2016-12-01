/* WEBSERVER DatabaseAccessor.java
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.net.URLEncoder;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DatabaseAccessor {
    private static final String DATABASE_URL = "http://www.grantuy.com/422c/project7.php";
    private static final String DELIMITER_REGEX = "\n";

    public static int intQuery(Map<String,Object> params) {
        try {
            return Integer.parseInt(query(params));
        } catch (Exception e) {
            return -1;
        }
    }

    public static List<Integer> intListQuery(Map<String,Object> params) {
        try {
            return Stream.of(query(params).split(DELIMITER_REGEX))
                    .mapToInt(Integer::parseInt)
                    .boxed()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static String strQuery(Map<String,Object> params) {
        try {
            return query(params);
        } catch (Exception e) {
            return "";
        }
    }

    public static List<String> strListQuery(Map<String,Object> params) {
        try {
            return Arrays.asList(query(params).split(DELIMITER_REGEX));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // http://stackoverflow.com/questions/7467568/parsing-json-from-url
    private static String query(Map<String,Object> params) throws Exception {
        BufferedReader reader = null;
        try {
            String urlStr = DATABASE_URL+encodeParams(params);
            URL url = new URL(urlStr);
            System.out.println(urlStr);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer buffer = new StringBuffer();
            int read;
            char[] chars = new char[1024];
            while ((read = reader.read(chars)) != -1)
                buffer.append(chars, 0, read);

            return buffer.toString();
        } finally {
            if (reader != null)
                reader.close();
        }
    }

    private static String encodeParams(Map<String,Object> params) {
        StringBuilder result = new StringBuilder("?");
        for (Map.Entry<String,Object> entry : params.entrySet()) {
            result.append(String.format("%s=%s&", URLEncode(entry.getKey()), URLEncode(entry.getValue().toString())));
        }
        return result.substring(0, result.length()-1);
    }

    private static String URLEncode(String str) {
        try {
            return URLEncoder.encode(str, "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }
}
