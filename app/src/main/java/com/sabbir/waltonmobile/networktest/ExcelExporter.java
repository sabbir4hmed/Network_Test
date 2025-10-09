package com.sabbir.waltonmobile.networktest;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import com.sabbir.waltonmobile.networktest.FieldTest.SignalData;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExcelExporter {

    // Progress callback interface
    public interface ProgressCallback {
        void onProgress(int progress, String message);
    }

    public static File exportSignalDataWithProgress(Context ctx, String fileName, List<SignalData> data,
                                                    String bandChoice, int testedSim,
                                                    SharedPreferences sharedPreferences,
                                                    ProgressCallback callback) throws Exception {

        if (callback != null) callback.onProgress(35, "Creating workbook...");

        XSSFWorkbook wb = new XSSFWorkbook();

        // Create Styles
        if (callback != null) callback.onProgress(40, "Applying styles...");

        CellStyle headerStyle = createHeaderStyle(wb);
        CellStyle subHeaderStyle = createSubHeaderStyle(wb);
        CellStyle normalStyle = createNormalStyle(wb);
        CellStyle excellentStyle = createColoredStyle(wb, IndexedColors.GREEN);
        CellStyle goodStyle = createColoredStyle(wb, IndexedColors.LIGHT_GREEN);
        CellStyle fairStyle = createColoredStyle(wb, IndexedColors.ORANGE);
        CellStyle poorStyle = createColoredStyle(wb, IndexedColors.RED);
        CellStyle veryPoorStyle = createColoredStyle(wb, IndexedColors.DARK_RED);

        // Sheet 1: Summary & Statistics
        if (callback != null) callback.onProgress(50, "Creating summary sheet...");
        createSummarySheet(wb, headerStyle, subHeaderStyle, normalStyle, data, bandChoice, testedSim, sharedPreferences);

        // Sheet 2: Field Test Info (from SimTestActivity)
        if (callback != null) callback.onProgress(60, "Creating field test info sheet...");
        createFieldTestInfoSheet(wb, headerStyle, normalStyle, sharedPreferences);

        // Sheet 3: Signal Data Samples
        if (callback != null) callback.onProgress(70, "Creating samples sheet...");
        createSamplesSheet(wb, headerStyle, normalStyle, excellentStyle, goodStyle, fairStyle, poorStyle, veryPoorStyle, data);

        // Sheet 4: Statistics Table
        if (callback != null) callback.onProgress(80, "Creating statistics sheet...");
        createStatisticsSheet(wb, headerStyle, subHeaderStyle, normalStyle, data);

        // Save to Documents folder
        if (callback != null) callback.onProgress(90, "Saving file...");

        // Get Documents directory
        File documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);

        // Create Documents folder if it doesn't exist
        if (!documentsDir.exists()) {
            documentsDir.mkdirs();
        }

        // Create the output file in Documents folder
        File outFile = new File(documentsDir, sanitize(fileName));

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            wb.write(fos);
        }
        wb.close();

        if (callback != null) callback.onProgress(100, "Report saved to Documents folder!");

        return outFile;
    }

    // Keep the old method for backward compatibility (without progress)
    public static File exportSignalData(Context ctx, String fileName, List<SignalData> data,
                                        String bandChoice, int testedSim, SharedPreferences sharedPreferences) throws Exception {
        return exportSignalDataWithProgress(ctx, fileName, data, bandChoice, testedSim, sharedPreferences, null);
    }

    private static void createSummarySheet(XSSFWorkbook wb, CellStyle headerStyle, CellStyle subHeaderStyle,
                                           CellStyle normalStyle, List<SignalData> data, String bandChoice,
                                           int testedSim, SharedPreferences sharedPreferences) {
        Sheet summary = wb.createSheet("Summary");

        // Set column widths manually (in units of 1/256th of a character width)
        summary.setColumnWidth(0, 6000);  // ~23 characters
        summary.setColumnWidth(1, 10000); // ~39 characters

        int r = 0;

        // Title
        Row row = summary.createRow(r++);
        createCell(row, 0, "FIELD TEST REPORT - SUMMARY", headerStyle);
        summary.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));
        r++; // Empty row

        // Report Generation Info
        String currentDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        row = summary.createRow(r++);
        createCell(row, 0, "Report Generated", subHeaderStyle);
        createCell(row, 1, currentDateTime, normalStyle);

        row = summary.createRow(r++);
        createCell(row, 0, "Band Choice", subHeaderStyle);
        createCell(row, 1, bandChoice, normalStyle);

        row = summary.createRow(r++);
        createCell(row, 0, "Tested SIM", subHeaderStyle);
        createCell(row, 1, testedSim == 0 ? "N/A" : "SIM " + testedSim, normalStyle);

        row = summary.createRow(r++);
        createCell(row, 0, "Total Samples", subHeaderStyle);
        createCell(row, 1, String.valueOf(data.size()), normalStyle);

        r++; // Empty row

        // Statistics
        if (!data.isEmpty()) {
            row = summary.createRow(r++);
            createCell(row, 0, "SIGNAL STATISTICS", headerStyle);
            summary.addMergedRegion(new CellRangeAddress(r - 1, r - 1, 0, 1));

            int bestDbm = Integer.MIN_VALUE;
            int worstDbm = Integer.MAX_VALUE;
            long sumDbm = 0;

            for (SignalData sd : data) {
                int dbm = sd.getDbm();
                if (dbm > bestDbm) bestDbm = dbm;
                if (dbm < worstDbm) worstDbm = dbm;
                sumDbm += dbm;
            }

            double avgDbm = (double) sumDbm / data.size();

            row = summary.createRow(r++);
            createCell(row, 0, "Best Signal (dBm)", subHeaderStyle);
            createCell(row, 1, bestDbm + " dBm (" + getSignalQuality(bestDbm) + ")", normalStyle);

            row = summary.createRow(r++);
            createCell(row, 0, "Worst Signal (dBm)", subHeaderStyle);
            createCell(row, 1, worstDbm + " dBm (" + getSignalQuality(worstDbm) + ")", normalStyle);

            row = summary.createRow(r++);
            createCell(row, 0, "Average Signal (dBm)", subHeaderStyle);
            createCell(row, 1, String.format(Locale.getDefault(), "%.2f dBm (%s)", avgDbm, getSignalQuality((int) avgDbm)), normalStyle);
        }

        r++; // Empty row

        // Test Count from SharedPreferences
        int testCount = sharedPreferences.getInt("test_count", 0);
        int reportCount = sharedPreferences.getInt("report_count", 0);

        row = summary.createRow(r++);
        createCell(row, 0, "HISTORICAL DATA", headerStyle);
        summary.addMergedRegion(new CellRangeAddress(r - 1, r - 1, 0, 1));

        row = summary.createRow(r++);
        createCell(row, 0, "Total Tests Conducted", subHeaderStyle);
        createCell(row, 1, String.valueOf(testCount), normalStyle);

        row = summary.createRow(r++);
        createCell(row, 0, "Total Reports Generated", subHeaderStyle);
        createCell(row, 1, String.valueOf(reportCount), normalStyle);
    }

    private static void createFieldTestInfoSheet(XSSFWorkbook wb, CellStyle headerStyle,
                                                 CellStyle normalStyle, SharedPreferences sharedPreferences) {
        Sheet sheet = wb.createSheet("Field Test Info");

        // Set column widths manually
        sheet.setColumnWidth(0, 6000);  // ~23 characters
        sheet.setColumnWidth(1, 10000); // ~39 characters

        int r = 0;

        // Title
        Row row = sheet.createRow(r++);
        createCell(row, 0, "FIELD TEST INFORMATION", headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));
        r++; // Empty row

        // Get data from SimTestActivity SharedPreferences
        String lastId = sharedPreferences.getString("last_id", "N/A");
        String lastModel = sharedPreferences.getString("last_model", "N/A");
        String lastBuildVersion = sharedPreferences.getString("last_build_version", "N/A");
        String lastSoftwareType = sharedPreferences.getString("last_software_type", "N/A");
        String lastLocation = sharedPreferences.getString("last_location", "N/A");
        String lastOperators = sharedPreferences.getString("last_operators", "N/A");
        String lastTimestamp = sharedPreferences.getString("last_timestamp", "N/A");

        row = sheet.createRow(r++);
        createCell(row, 0, "Test ID", headerStyle);
        createCell(row, 1, lastId, normalStyle);

        row = sheet.createRow(r++);
        createCell(row, 0, "Device Model", headerStyle);
        createCell(row, 1, lastModel, normalStyle);

        row = sheet.createRow(r++);
        createCell(row, 0, "Software Type", headerStyle);
        createCell(row, 1, lastSoftwareType, normalStyle);

        row = sheet.createRow(r++);
        createCell(row, 0, "Test Location", headerStyle);
        createCell(row, 1, lastLocation, normalStyle);

        row = sheet.createRow(r++);
        createCell(row, 0, "Selected Operators", headerStyle);
        createCell(row, 1, lastOperators, normalStyle);

        row = sheet.createRow(r++);
        createCell(row, 0, "Test Timestamp", headerStyle);
        createCell(row, 1, lastTimestamp, normalStyle);

        r++; // Empty row

        // Build Version Details
        row = sheet.createRow(r++);
        createCell(row, 0, "BUILD VERSION DETAILS", headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(r - 1, r - 1, 0, 1));

        String[] buildLines = lastBuildVersion.split("\n");
        for (String line : buildLines) {
            row = sheet.createRow(r++);
            String[] parts = line.split(":", 2);
            if (parts.length == 2) {
                createCell(row, 0, parts[0].trim(), headerStyle);
                createCell(row, 1, parts[1].trim(), normalStyle);
            } else {
                createCell(row, 0, line, normalStyle);
                sheet.addMergedRegion(new CellRangeAddress(r - 1, r - 1, 0, 1));
            }
        }
    }

    private static void createSamplesSheet(XSSFWorkbook wb, CellStyle headerStyle, CellStyle normalStyle,
                                           CellStyle excellentStyle, CellStyle goodStyle, CellStyle fairStyle,
                                           CellStyle poorStyle, CellStyle veryPoorStyle, List<SignalData> data) {
        Sheet sheet = wb.createSheet("Signal Samples");

        // Set column widths manually
        sheet.setColumnWidth(0, 2000);  // # column
        sheet.setColumnWidth(1, 5000);  // Model
        sheet.setColumnWidth(2, 6000);  // Timestamp
        sheet.setColumnWidth(3, 3000);  // dBm
        sheet.setColumnWidth(4, 4000);  // Signal Quality
        sheet.setColumnWidth(5, 5000);  // Operator
        sheet.setColumnWidth(6, 5000);  // Location

        int rowIdx = 0;

        // Header
        Row h = sheet.createRow(rowIdx++);
        String[] headers = new String[]{"#", "Model", "Timestamp", "dBm", "Signal Quality", "Operator", "Location"};
        for (int i = 0; i < headers.length; i++) {
            Cell c = h.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        // Data rows
        int sampleNum = 1;
        for (SignalData sd : data) {
            Row dr = sheet.createRow(rowIdx++);
            int col = 0;

            createCell(dr, col++, sampleNum++, normalStyle);
            createCell(dr, col++, sd.getModel(), normalStyle);
            createCell(dr, col++, sd.getTimestamp().replace("\n", " "), normalStyle);

            // dBm with color coding
            Cell dbmCell = dr.createCell(col++);
            dbmCell.setCellValue(sd.getDbm());
            dbmCell.setCellStyle(getStyleForSignal(sd.getSignalQuality(), excellentStyle, goodStyle, fairStyle, poorStyle, veryPoorStyle));

            // Signal Quality with color coding
            Cell qualityCell = dr.createCell(col++);
            qualityCell.setCellValue(sd.getSignalQuality());
            qualityCell.setCellStyle(getStyleForSignal(sd.getSignalQuality(), excellentStyle, goodStyle, fairStyle, poorStyle, veryPoorStyle));

            createCell(dr, col++, sd.getOperatorName(), normalStyle);
            createCell(dr, col++, sd.getLocation(), normalStyle);
        }
    }

    private static void createStatisticsSheet(XSSFWorkbook wb, CellStyle headerStyle, CellStyle subHeaderStyle,
                                              CellStyle normalStyle, List<SignalData> data) {
        Sheet sheet = wb.createSheet("Statistics");

        // Set column widths manually
        sheet.setColumnWidth(0, 5000);  // Metric/Quality Level
        sheet.setColumnWidth(1, 4000);  // Value/Count
        sheet.setColumnWidth(2, 4000);  // Quality/Percentage
        sheet.setColumnWidth(3, 5000);  // Percentage/dBm Range

        int r = 0;

        // Title
        Row row = sheet.createRow(r++);
        createCell(row, 0, "DETAILED STATISTICS", headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));
        r++; // Empty row

        if (data.isEmpty()) {
            row = sheet.createRow(r++);
            createCell(row, 0, "No data available", normalStyle);
            return;
        }

        // Calculate statistics
        int bestDbm = Integer.MIN_VALUE;
        int worstDbm = Integer.MAX_VALUE;
        long sumDbm = 0;
        int excellentCount = 0, goodCount = 0, fairCount = 0, poorCount = 0, veryPoorCount = 0;

        for (SignalData sd : data) {
            int dbm = sd.getDbm();
            if (dbm > bestDbm) bestDbm = dbm;
            if (dbm < worstDbm) worstDbm = dbm;
            sumDbm += dbm;

            String quality = sd.getSignalQuality();
            switch (quality) {
                case "Excellent": excellentCount++; break;
                case "Good": goodCount++; break;
                case "Fair": fairCount++; break;
                case "Poor": poorCount++; break;
                case "Very Poor": veryPoorCount++; break;
            }
        }

        double avgDbm = (double) sumDbm / data.size();

        // Signal Strength Statistics
        row = sheet.createRow(r++);
        createCell(row, 0, "Metric", headerStyle);
        createCell(row, 1, "Value", headerStyle);
        createCell(row, 2, "Quality", headerStyle);
        createCell(row, 3, "Percentage", headerStyle);

        row = sheet.createRow(r++);
        createCell(row, 0, "Best Signal", subHeaderStyle);
        createCell(row, 1, bestDbm + " dBm", normalStyle);
        createCell(row, 2, getSignalQuality(bestDbm), normalStyle);
        createCell(row, 3, "-", normalStyle);

        row = sheet.createRow(r++);
        createCell(row, 0, "Worst Signal", subHeaderStyle);
        createCell(row, 1, worstDbm + " dBm", normalStyle);
        createCell(row, 2, getSignalQuality(worstDbm), normalStyle);
        createCell(row, 3, "-", normalStyle);

        row = sheet.createRow(r++);
        createCell(row, 0, "Average Signal", subHeaderStyle);
        createCell(row, 1, String.format(Locale.getDefault(), "%.2f dBm", avgDbm), normalStyle);
        createCell(row, 2, getSignalQuality((int) avgDbm), normalStyle);
        createCell(row, 3, "-", normalStyle);

        row = sheet.createRow(r++);
        createCell(row, 0, "Signal Range", subHeaderStyle);
        createCell(row, 1, (bestDbm - worstDbm) + " dBm", normalStyle);
        createCell(row, 2, "-", normalStyle);
        createCell(row, 3, "-", normalStyle);

        r++; // Empty row

        // Signal Quality Distribution
        row = sheet.createRow(r++);
        createCell(row, 0, "SIGNAL QUALITY DISTRIBUTION", headerStyle);
        sheet.addMergedRegion(new CellRangeAddress(r - 1, r - 1, 0, 3));

        row = sheet.createRow(r++);
        createCell(row, 0, "Quality Level", headerStyle);
        createCell(row, 1, "Count", headerStyle);
        createCell(row, 2, "Percentage", headerStyle);
        createCell(row, 3, "dBm Range", headerStyle);

        row = sheet.createRow(r++);
        createCell(row, 0, "Excellent", subHeaderStyle);
        createCell(row, 1, String.valueOf(excellentCount), normalStyle);
        createCell(row, 2, String.format(Locale.getDefault(), "%.1f%%", (excellentCount * 100.0 / data.size())), normalStyle);
        createCell(row, 3, ">= -70 dBm", normalStyle);

        row = sheet.createRow(r++);
        createCell(row, 0, "Good", subHeaderStyle);
        createCell(row, 1, String.valueOf(goodCount), normalStyle);
        createCell(row, 2, String.format(Locale.getDefault(), "%.1f%%", (goodCount * 100.0 / data.size())), normalStyle);
        createCell(row, 3, "-70 to -85 dBm", normalStyle);

        row = sheet.createRow(r++);
        createCell(row, 0, "Fair", subHeaderStyle);
        createCell(row, 1, String.valueOf(fairCount), normalStyle);
        createCell(row, 2, String.format(Locale.getDefault(), "%.1f%%", (fairCount * 100.0 / data.size())), normalStyle);
        createCell(row, 3, "-85 to -100 dBm", normalStyle);

        row = sheet.createRow(r++);
        createCell(row, 0, "Poor", subHeaderStyle);
        createCell(row, 1, String.valueOf(poorCount), normalStyle);
        createCell(row, 2, String.format(Locale.getDefault(), "%.1f%%", (poorCount * 100.0 / data.size())), normalStyle);
        createCell(row, 3, "-100 to -110 dBm", normalStyle);

        row = sheet.createRow(r++);
        createCell(row, 0, "Very Poor", subHeaderStyle);
        createCell(row, 1, String.valueOf(veryPoorCount), normalStyle);
        createCell(row, 2, String.format(Locale.getDefault(), "%.1f%%", (veryPoorCount * 100.0 / data.size())), normalStyle);
        createCell(row, 3, "< -110 dBm", normalStyle);
    }

    // Helper methods for styling
    private static CellStyle createHeaderStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private static CellStyle createSubHeaderStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private static CellStyle createNormalStyle(XSSFWorkbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setWrapText(false);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private static CellStyle createColoredStyle(XSSFWorkbook wb, IndexedColors color) {
        CellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setColor(color.getIndex());
        font.setBold(true);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private static CellStyle getStyleForSignal(String quality, CellStyle excellent, CellStyle good,
                                               CellStyle fair, CellStyle poor, CellStyle veryPoor) {
        switch (quality) {
            case "Excellent": return excellent;
            case "Good": return good;
            case "Fair": return fair;
            case "Poor": return poor;
            case "Very Poor": return veryPoor;
            default: return fair;
        }
    }

    private static String getSignalQuality(int dbm) {
        if (dbm >= -70) return "Excellent";
        else if (dbm >= -85) return "Good";
        else if (dbm >= -100) return "Fair";
        else if (dbm >= -110) return "Poor";
        else return "Very Poor";
    }

    private static void createCell(Row row, int col, String val, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(val == null ? "" : val);
        cell.setCellStyle(style);
    }

    private static void createCell(Row row, int col, int val, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(val);
        cell.setCellStyle(style);
    }

    private static String sanitize(String name) {
        if (name == null || name.trim().isEmpty()) name = "field_test_report.xlsx";
        if (!name.toLowerCase().endsWith(".xlsx")) name += ".xlsx";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}