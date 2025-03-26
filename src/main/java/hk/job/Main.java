package hk.job;

import hk.job.analyzer.JobDetailAnalyzer;
import hk.job.crawler.JobDetailCrawler;
import hk.job.crawler.JobListCrawler;
import hk.job.dto.JobDetail;

import java.io.*;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws Exception {

        long start = System.currentTimeMillis();

        Properties prop = new Properties();
        prop.load(Main.class.getResourceAsStream("/config.properties"));

        Config config = new Config(prop);

        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(config.getProxyHostname(), config.getProxyPort()));
        LocalStorage localStorage = new LocalStorage(config.getBackupJobListFilePath());

        // 1. fetch the job list
        JobListCrawler jobListCrawler = new JobListCrawler(config.isProxyEnable() ? proxy : null);
        List<JobDetail> jobDetails = new ArrayList<>();

        try {
            jobDetails = jobListCrawler.fetchJobList();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Unable to fetch job");
        }

        // 2. get local job list
        if (config.isUseBackup()) {
            List<JobDetail> localJobList = localStorage.getLocalJobList();
            jobDetails = mergeJobList(localJobList, jobDetails);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(4);

        // 3a. for each job, fetch its job detail if department is null
        List<JobDetail> jobWithoutDeaprtments = jobDetails.stream().filter(j -> Objects.isNull(j.getDepartment())).collect(Collectors.toList());
        for (JobDetail jobDetail : jobWithoutDeaprtments) {
            JobDetailCrawler crawler = new JobDetailCrawler(jobDetail, proxy);
            crawler.fetchJobDetail();
        }

        // 3b. for each job, analyze keywords
        for (JobDetail jobDetail : jobDetails) {
            executorService.submit(() -> new JobDetailAnalyzer(jobDetail).analyze());
        }

        executorService.shutdown();
        boolean terminated = executorService.awaitTermination(3, TimeUnit.MINUTES);

        // 4. sort
        jobDetails.sort(
                Comparator.comparing(JobDetail::getLastUpdateDate).reversed()
                        .thenComparing(JobDetail::getDepartment, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(JobDetail::getTitle, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(JobDetail::getBidRef, Comparator.nullsLast(Comparator.reverseOrder())));

        JobDetailAnalyzer.findPossibleBid(jobDetails);

        // 5. save to local
        if (config.isUseBackup()) {
            localStorage.saveJobList(jobDetails);
            File file = localStorage.getFile();
            String newPath = filenameAppend(file.toPath().toString(), LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
            Files.copy(file.toPath(), Path.of(newPath), StandardCopyOption.REPLACE_EXISTING);
        }

        // 6. export to excel, appending date as suffix
        ExcelRenderer excelRenderer = new ExcelRenderer(jobDetails);

        String path = filenameAppend(config.getExcelFilePath(), LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
        File file = new File(path);

        excelRenderer.writeTo(file);

        System.out.println("Time used: " + (System.currentTimeMillis() - start) + "ms");
    }

    private static String filenameAppend(String path, String suffix) {
        int at = path.lastIndexOf(".");
        return path.substring(0, at) +
                "_" +
                suffix +
                path.substring(at);
    }

    // union two list (using bidRef as the key)
    private static List<JobDetail> mergeJobList(List<JobDetail> localJobList, List<JobDetail> jobList) throws IllegalAccessException {
        Map<String, JobDetail> localJobMap = localJobList.stream().collect(Collectors.toMap(JobDetail::getBidRef, j -> j));
        Map<String, JobDetail> jobMap = jobList.stream().collect(Collectors.toMap(JobDetail::getBidRef, j -> j));

        Map<String, JobDetail> result = new HashMap<>(localJobMap);
        for (String key : jobMap.keySet()) {
            JobDetail value = jobMap.get(key);
            if (result.containsKey(key)) {
                result.put(key, mergeObject(result.get(key), value));
            } else {
                result.put(key, value);
            }
        }
        return new ArrayList<>(result.values());
    }

    private static JobDetail mergeObject(JobDetail jobDetail1, JobDetail jobDetail2) throws IllegalAccessException {
        JobDetail jobDetail = new JobDetail();
        Field[] fields = jobDetail.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            Object value = field.get(jobDetail1);
            value = value == null ? field.get(jobDetail2) : value;
            field.set(jobDetail, value);
        }
        return jobDetail;
    }

}
