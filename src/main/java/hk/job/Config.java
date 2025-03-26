package hk.job;

import lombok.Getter;

import java.util.Properties;

@Getter
public class Config {
    private final boolean proxyEnable;
    private final String proxyHostname;
    private final int proxyPort;
    private final String excelFilePath;
    private final boolean useBackup;
    private final String backupJobListFilePath;

    public Config(Properties prop) {
        proxyEnable = tryParseBoolean(prop.get("proxy.enable"), false);
        proxyHostname = prop.get("proxy.hostname").toString();
        proxyPort = tryParseInteger(prop.get("proxy.port"), 0);
        excelFilePath = prop.get("joblist.excel.path").toString();
        useBackup = tryParseBoolean(prop.get("joblist.backup.enable"), true);
        backupJobListFilePath = prop.get("joblist.backup.path").toString();
    }

    private static boolean tryParseBoolean(Object obj, boolean defaultValue) {
        try {
            return Boolean.parseBoolean(obj.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static int tryParseInteger(Object obj, int defaultValue) {
        try {
            return Integer.parseInt(obj.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
