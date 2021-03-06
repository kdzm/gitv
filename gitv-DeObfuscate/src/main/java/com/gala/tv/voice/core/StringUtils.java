package com.gala.tv.voice.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
    static String m531a(String str, String str2) {
        try {
            String[] split = str2.split(",");
            for (int i = 0; i < split.length; i++) {
                if (Pattern.compile(split[i]).matcher(str).find()) {
                    str = m535d(str, split[i]);
                    break;
                }
            }
        } catch (Exception e) {
        }
        return str;
    }

    private static String m534c(String str, String str2) {
        try {
            String[] split = str2.split(",");
            int i = 0;
            String str3 = str;
            while (i < split.length) {
                try {
                    str = str3.replace(split[i].toLowerCase(), "");
                    i++;
                    str3 = str;
                } catch (Exception e) {
                    return str3;
                }
            }
            return str3;
        } catch (Exception e2) {
            return str;
        }
    }

    static String m532a(String str, String str2, String str3) {
        try {
            Matcher matcher = Pattern.compile(str2 + "(.*?)" + str3).matcher(str);
            if (matcher.find()) {
                str = m534c(matcher.group(0), str2 + "," + str3);
            }
        } catch (Exception e) {
        }
        return str;
    }

    private static String m535d(String str, String str2) {
        try {
            str = "#" + str;
            return m532a(str, "#", str2.toLowerCase());
        } catch (Exception e) {
            return str;
        }
    }

    static String m533b(String str, String str2) {
        try {
            str = str + "#";
            return m532a(str, str2.toLowerCase(), "#");
        } catch (Exception e) {
            return str;
        }
    }
}
