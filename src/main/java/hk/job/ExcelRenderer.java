package hk.job;

import hk.job.annoation.ExcelColumn;
import hk.job.dto.JobDetail;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExcelRenderer {
    private final List<JobDetail> jobDetails;

    public ExcelRenderer(List<JobDetail> jobDetails) {
        this.jobDetails = jobDetails;
    }

    public void writeTo(File file) throws IOException, IllegalAccessException {
        try (Workbook workbook = new XSSFWorkbook()) {

            List<JobDetail> expiredJobs = jobDetails.stream().filter(j -> j.getBidExpiryDate() != null && j.getBidExpiryDate().isBefore(LocalDate.now())).collect(Collectors.toList());

            List<JobDetail> activeJobs = jobDetails.stream().filter(j -> !expiredJobs.contains(j)).collect(Collectors.toList());

            Sheet allJobSheet = workbook.createSheet("All Jobs");
            renderSheet(workbook, allJobSheet, jobDetails);

            Sheet activeJobSheet = workbook.createSheet("Active Jobs");
            renderSheet(workbook, activeJobSheet, activeJobs);

            Sheet expiredJobSheet = workbook.createSheet("Expired Jobs");
            renderSheet(workbook, expiredJobSheet, expiredJobs);

            try (OutputStream outputStream = new FileOutputStream(file)) {
                workbook.write(outputStream);
            }
        }
    }

    private void renderSheet(Workbook workbook, Sheet sheet, List<JobDetail> jobDetails) throws IllegalAccessException {
        renderHeader(workbook, sheet);
        System.out.println("Render header completed");

        renderContent(workbook, sheet, jobDetails);
        System.out.println("Render content completed");

        resizeSheetColumns(sheet);
        System.out.println("resize column completed");
    }

    private void renderContent(Workbook workbook, Sheet sheet, List<JobDetail> jobDetails) throws IllegalAccessException {
        CreationHelper creationHelper = workbook.getCreationHelper();


        for (JobDetail detail : jobDetails) {
            int rowIdx = sheet.getLastRowNum() + 1;
            Row row = sheet.createRow(rowIdx);
            boolean isExpired = detail.getBidExpiryDate().isBefore(LocalDate.now());

            List<Field> jobDetailFields = Arrays.stream(detail.getClass().getDeclaredFields())
                    .filter(field -> field.isAnnotationPresent(ExcelColumn.class))
                    .sorted(Comparator.comparing(a -> a.getAnnotation(ExcelColumn.class).value()))
                    .collect(Collectors.toList());

            // 1. write job detail field
            for (int i = 0; i < jobDetailFields.size(); i++) {
                Field field = jobDetailFields.get(i);
                field.setAccessible(true);
                Object value = field.get(detail);
                ExcelColumn columnInfo = field.getAnnotation(ExcelColumn.class);

                Cell cell = row.createCell(i);
                CellStyle cellStyle = null;
                cell.setCellValue(value != null ? Objects.toString(value) : "");
                if (columnInfo.format() != null && !columnInfo.format().isEmpty()) {
                    String format = columnInfo.format();
                    cellStyle = workbook.createCellStyle();
                    cellStyle.setDataFormat(creationHelper.createDataFormat().getFormat(format));
                    cell.setCellStyle(cellStyle);
                }
            }

            // 2. write job keyword
            int keywordColumnOffset = row.getLastCellNum();
            for (int j = 0; j < JobRequirement.ALL_RULES.size(); j++) {
                Stream<String> ruleKeywords = JobRequirement.ALL_RULES.get(j).getKeywords().stream();
                Set<String> requirementKeywords = detail.getKeywords();
                List<String> intersection = ruleKeywords.filter(requirementKeywords::contains).collect(Collectors.toList());
                row.createCell(j + keywordColumnOffset).setCellValue(String.join("", intersection));
            }

            if (rowIdx % 500 == 0 || rowIdx == jobDetails.size()) {
                System.out.println(rowIdx + " / " + jobDetails.size() + " rendered");
            }
        }
    }

    private void renderHeader(Workbook workbook, Sheet sheet) {
        Row headerRow = sheet.createRow(sheet.getLastRowNum() + 1);
        for (int i = 0; i < JOB_DETAIL_LABELS.size(); i++) {
            headerRow.createCell(i).setCellValue(JOB_DETAIL_LABELS.get(i));
        }
        for (int i = 0; i < KEYWORD_LABELS.size(); i++) {
            headerRow.createCell(i + JOB_DETAIL_LABELS.size()).setCellValue(KEYWORD_LABELS.get(i));
        }

        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setFont(font);
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            headerRow.getCell(i).setCellStyle(cellStyle);
        }
    }

    private static final List<String> JOB_DETAIL_LABELS;
    private static final List<String> KEYWORD_LABELS;

    static {

        JOB_DETAIL_LABELS = Arrays.stream(JobDetail.class.getDeclaredFields())
                .map(field -> field.getAnnotation(ExcelColumn.class))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ExcelColumn::value))
                .map(ExcelColumn::label)
                .collect(Collectors.toList());

        KEYWORD_LABELS = JobRequirement.ALL_RULES.stream()
                .map(k -> k.getLabel().concat(" ").concat(String.join("", k.getKeywords())))
                .collect(Collectors.toList());
    }

    private void resizeSheetColumns(Sheet sheet) {
        List<ExcelColumn> jobDetailColumns = Arrays.stream(JobDetail.class.getDeclaredFields())
                .map(field -> field.getAnnotation(ExcelColumn.class))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ExcelColumn::value))
                .collect(Collectors.toList());

        for (int i = 0; i < jobDetailColumns.size(); i++) {
            String width = jobDetailColumns.get(i).width();
            if ("auto".equals(width)) {
                sheet.autoSizeColumn(i);
            } else {
                sheet.setColumnWidth(i, Integer.parseInt(width) * 256);
            }
        }
        for (int i = JOB_DETAIL_LABELS.size(); i < JOB_DETAIL_LABELS.size() + KEYWORD_LABELS.size(); i++) {
            sheet.setColumnWidth(i, 12 * 256);
        }
        for (int i = 0; i < jobDetailColumns.size() + JOB_DETAIL_LABELS.size() + KEYWORD_LABELS.size(); i++) {
            sheet.setColumnWidth(i, (int) (sheet.getColumnWidth(i) * 1.1f));
        }
        sheet.setAutoFilter(new CellRangeAddress(0, sheet.getLastRowNum(), 0, JOB_DETAIL_LABELS.size() + KEYWORD_LABELS.size() - 1));
    }
}
