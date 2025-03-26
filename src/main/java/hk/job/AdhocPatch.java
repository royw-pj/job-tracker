package hk.job;

import hk.job.dto.JobDetail;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class AdhocPatch {
    public static void main(String[] args) throws Exception {
        Properties prop = new Properties();
        prop.load(Main.class.getResourceAsStream("/config.properties"));

        Config config = new Config(prop);
//        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(config.getProxyHostname(), config.getProxyPort()));

        LocalStorage localStorage = new LocalStorage(config.getBackupJobListFilePath());
        List<JobDetail> jobDetailList = localStorage.getLocalJobList();
        Map<String, JobDetail> map = jobDetailList.stream().collect(Collectors.toMap(JobDetail::getBidRef, a -> a));
        try (FileInputStream fis = new FileInputStream("C:\\rationalsdp8\\tconDb-1.xlsx");
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0); // Get the first sheet

            // Iterate over rows, starting from the second row (index 1)
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                String bidRef = row.getCell(0).getStringCellValue();
                if (map.containsKey(bidRef)) {
                    continue;
                }
                if (!bidRef.matches("[0-9]{5}-[0-9]{1,2}")) {
                    continue;
                }

                JobDetail jobDetail = new JobDetail();
                jobDetail.setBidRef(row.getCell(0).getStringCellValue());
                jobDetail.setTitle(row.getCell(2).getStringCellValue());
                jobDetail.setLocation(row.getCell(3).getStringCellValue());

                jobDetail.setNature(row.getCell(9).getStringCellValue());

                jobDetail.setDuty(row.getCell(10).getStringCellValue());

                jobDetail.setRequirement(row.getCell(11).getStringCellValue());
                jobDetail.setVacancy((int) (row.getCell(4).getNumericCellValue()));
                jobDetail.setBidExpiryDate(toLocalDate(row.getCell(5)));

                jobDetail.setContractPeriodFrom(toLocalDate(row.getCell(6)));
                jobDetail.setContractPeriodTo(toLocalDate(row.getCell(7)));
                jobDetail.setLastUpdateDate(toLocalDate(row.getCell(8)));
                // JobDetailAnalyzer analyzer = new JobDetailAnalyzer(jobDetail);
                // analyzer.analyze();

                jobDetailList.add(jobDetail);


            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        localStorage.saveJobList(jobDetailList);
    }

    private static LocalDate toLocalDate(Cell cell) {
        CellType type = cell.getCellType();
        switch (type) {
            case STRING:
                return LocalDate.parse(cell.getStringCellValue());
            case NUMERIC:
                return cell.getLocalDateTimeCellValue().toLocalDate();
            default:
                throw new IllegalArgumentException(type.toString() + " not support");
        }
    }
}
