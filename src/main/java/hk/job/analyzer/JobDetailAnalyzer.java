package hk.job.analyzer;

import hk.job.JobRequirement;
import hk.job.dto.JobDetail;
import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.jsoup.Jsoup;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class JobDetailAnalyzer {
    private final JobDetail jobDetail;

    public JobDetailAnalyzer(JobDetail jobDetail) {
        this.jobDetail = jobDetail;
    }

    public void analyze() {
        jobDetail.setDepartment(parseDepartment(jobDetail.getDuty()));
        jobDetail.setKeywords(parseKeywords(jobDetail.getRequirement()));
        jobDetail.setGovermentExp(parseGovermentExpMandatory(jobDetail.getRequirement()));
        jobDetail.setBidRound(parseBidRound(jobDetail.getBidRef()));

        if (jobDetail.getBidRef() != null) {
            jobDetail.setBid(jobDetail.getBidRef().split("-")[0]);
        }
    }

    private static String ignoreWhite(String text) {
        return text.replaceAll("[\r\n \\.-]", "");
    }

    public static void findPossibleBid(List<JobDetail> allJobDetails) throws InterruptedException {
        Map<Integer, List<JobDetail>> map = new HashMap<>();
        for (JobDetail j : allJobDetails) {
            Integer key = Objects.hash(j.getTitle(), j.getDepartment());
            j.setNatureAnalyze(
                    ignoreWhite(j.getNature())
            );
            j.setRequirementAnalyze(
                    ignoreWhite(j.getRequirement())
            );
            j.setDutyAnalyze(
                    ignoreWhite(j.getDuty())
            );

            if (map.containsKey(key)) {
                map.get(key).add(j);
            } else {
                List<JobDetail> list = new ArrayList<>();
                list.add(j);
                map.put(key, list);
            }
        }
        map.values().forEach(list -> list.sort(Comparator.comparing(JobDetail::getLastUpdateDate)));

        AtomicInteger counter = new AtomicInteger(0);

        var executor = Executors.newFixedThreadPool(8);
        for (JobDetail thisJobDetail : allJobDetails) {
            executor.submit(() -> {
                var distance = new JaroWinklerDistance();
                Integer key = Objects.hash(thisJobDetail.getTitle(), thisJobDetail.getDepartment());
                List<JobDetail> list = map.get(key);
                for (JobDetail refJobDetail : list) {
                    int dayDiff = (int) (thisJobDetail.getLastUpdateDate().toEpochDay() - refJobDetail.getLastUpdateDate().toEpochDay());
                    if (dayDiff < 30) {
                        break;
                    }

                    if (thisJobDetail.getBid().equals(refJobDetail.getBid())) {
                        continue;
                    }

                    int continuousContract = (int) thisJobDetail.getContractPeriodFrom().toEpochDay() - (int) refJobDetail.getContractPeriodTo().toEpochDay();
                    if (continuousContract > 1) {
                        continue;
                    }

                    double diff = distance.apply(thisJobDetail.getRequirementAnalyze(), refJobDetail.getRequirementAnalyze());
                    if (diff > 0.2d) {
                        continue;
                    }
                    int similarity = (int) ((1 - diff) * 100);
                    thisJobDetail.getPossibleReferences().add(refJobDetail.getBidRef() + "(" + similarity + "%)");
                }
                int current = counter.incrementAndGet();
                if (current % 500 == 0 || current == allJobDetails.size()) {
                    System.out.println(current + " / " + allJobDetails.size() + " analyzed.");
                }
            });
        }
        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.MINUTES);


    }

    private String parseDepartment(String duty) {
        Pattern pattern = Pattern.compile(".*Serve a contract.*the (.*);.*");
        try {
            String firstLine = duty.split("\n")[0];
            Matcher matcher = pattern.matcher(firstLine);
            if (matcher.matches()) {
                return Jsoup.parse(matcher.group(1)).text();
            }
        } catch (Exception ex) {
            System.err.println("no department");
        }
        return "";
    }

    private String parseGovermentExpMandatory(String requirement) {
        List<String> govKeyWords = JobRequirement.ALL_RULES.stream().filter(r -> "Government".equalsIgnoreCase(r.getLabel())).findFirst().get().getKeywords();
        String govKeyWordRegex = govKeyWords.stream().map(word -> word.replace("(", "\\(").replace(")", "\\)")).collect(Collectors.joining("|"));
        Pattern pattern = Pattern.compile(".*at least.*(" + govKeyWordRegex + ").*", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(requirement);
        if (govKeyWords.stream().anyMatch(requirement::contains)) {
            return matcher.find() ? "At least" : "Advantage";
        } else {
            return "";
        }
    }

    private Set<String> parseKeywords(String requirement) {
        Set<String> keywords = new HashSet<>();
        for (JobRequirement.JobKeyword jobKeyword : JobRequirement.ALL_RULES) {
            for (String keyword : jobKeyword.getKeywords()) {
                if (requirement.contains(keyword)) {
                    keywords.add(keyword);
                }
            }
        }
        return keywords;
    }

    private String parseBidRound(String bidRef) {
        int round = Integer.parseInt(bidRef.split("-")[1]);
        return new String[]{"", "1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "9th", "10th", "11th", "12th", "13th", "14th", "15th"}[round];
    }


}
