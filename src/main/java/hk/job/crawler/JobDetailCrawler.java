package hk.job.crawler;

import hk.job.dto.JobDetail;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.Proxy;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JobDetailCrawler {
    private final Connection connection;
    private final JobDetail jobDetail;

    public JobDetailCrawler(JobDetail jobDetail, Proxy proxy) {
        this.jobDetail = jobDetail;
        connection = Jsoup.connect("https://www.infotech.com.hk/itjs/job/fe-view.do")
                .data("method", "feView")
                .data("jjKey", jobDetail.getJjKey())
                .proxy(proxy);
    }

    public void fetchJobDetail() {
        try {
            fetchJobDetailInternal();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetchJobDetailInternal() throws Exception {
        Document document = connection.get();
        Elements elements = document.select("form[name=jobForm] tbody tr");
        for (Element element : elements) {
            String label = element.selectFirst("td").text().trim();
            Elements content = element.select("td:nth-child(2)");
            switch (label) {
                case "Number Of Vacancy":
                    jobDetail.setVacancy(Integer.parseInt(content.text()));
                    break;
                case "Deadline":
                    jobDetail.setBidExpiryDate(parseDate(content.text()));
                    break;
                case "Contract Period":
                    Pattern pattern = Pattern.compile("([0-9]{2} [A-Z][a-z]{2} [0-9]{4}) to ([0-9]{2} [A-Z][a-z]{2} [0-9]{4}).*");
                    Matcher matcher = pattern.matcher(content.text());
                    if (matcher.matches()) {
                        jobDetail.setContractPeriodFrom(parseDate(matcher.group(1)));
                        jobDetail.setContractPeriodTo(parseDate(matcher.group(2)));
                    }
                    break;
                case "Location Base":
                    jobDetail.setLocation(content.text());
                    break;
                case "Project Nature":
                    jobDetail.setNature(content.text());
                    break;
                case "Duties":
                    jobDetail.setDuty(parseDuty(content.html()));
                    break;
                case "Requirements":
                    jobDetail.setRequirement(parseRequirement(content.html()));
                    break;
//                case "Relevant Field":
//                    jobDetail.setRelevantField(content.text());
//                    break;
                case "Last Update":
                    jobDetail.setLastUpdateDate(parseDate(content.text()));
                    break;
            }
        }
        System.out.println(jobDetail.getBidRef() + " finished fetched");
    }

    private String parseDuty(String html) {
        return Jsoup.parse(html).wholeText().replaceAll("(\r\n|\r)", "\n").replaceAll("\n\n", "\n");
    }


    private LocalDate parseDate(String str) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy").localizedBy(Locale.ENGLISH);
        return LocalDate.parse(str, formatter);
    }


    private String parseRequirement(String html) {
        return Jsoup.parse(html).wholeText().replaceAll("(\r\n|\r)", "\n").replaceAll("\n\n", "\n");
    }


}
