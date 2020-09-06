package util;

import java.text.DecimalFormat;

public class FileSizeLongToStringFormatter {

    public static String format(long fileSize) {
        double doubleSize;
        final DecimalFormat decimalFormat = new DecimalFormat( "#.##" );
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            doubleSize = (double) fileSize / 1024;
            return decimalFormat.format(doubleSize) + " KB";
        } else if (fileSize < 1024 * 1024 * 1024) {
            doubleSize = (double) fileSize / (1024 * 1024);
            return decimalFormat.format(doubleSize) + " MB";
        } else {
            doubleSize = (double) fileSize / (1024 * 1024 * 1024);
            return decimalFormat.format(doubleSize) + " GB";
        }
    }
}
