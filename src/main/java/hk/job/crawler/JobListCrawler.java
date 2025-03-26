package hk.job.crawler;

import hk.job.dto.JobDetail;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.Proxy;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * A class to fetch its jjKey, bidRef and title only
 */
public class JobListCrawler {
    private final Connection connection;

    public JobListCrawler(Proxy proxy) {
        connection = Jsoup.connect("https://www.infotech.com.hk/itjs/job/fe-search.do")
                .data("method", "feSearch")
                .data("sortByField", "jjm_activedate")
                .data("changeOrder", "false")
                .data("keyword", "bid ref")
                .proxy(proxy);
    }

    public List<JobDetail> fetchJobList() throws IOException {
        Document document = connection.post();

        Elements jobs = document.select("table[class=data] tbody tr[class^=row]");
        List<JobDetail> jobDetails = new ArrayList<>();
        for (Element job : jobs) {
            Elements elements = job.select("td");
            String titleAndBidRef = elements.get(2).text();
            JobDetail jobDetail = new JobDetail();
            jobDetail.setTitle(parseTitle(titleAndBidRef));
            jobDetail.setBidRef(parseBidRef(titleAndBidRef));
            String jjKey = Arrays.stream(Objects.requireNonNull(elements.get(2).selectFirst("a"))
                            .attr("href")
                            .split("&"))
                    .filter(param -> param.startsWith("jjKey")).findFirst().get().split("=")[1];
            jobDetail.setJjKey(jjKey);
            jobDetails.add(jobDetail);
        }
        return jobDetails;
    }

    private static final Pattern titleAndBidRefPattern = Pattern.compile("(.*) \\(bid ref ([0-9-]*\\))(.*)");

    private String parseTitle(String titleAndBidRef) {
        Matcher matcher = titleAndBidRefPattern.matcher(titleAndBidRef);
        if (!matcher.matches()) {
            return "Invalid pattern";
        }
        return matcher.group(1);
    }

    private String parseBidRef(String titleAndBidRef) {
        Matcher matcher = titleAndBidRefPattern.matcher(titleAndBidRef);
        if (!matcher.matches()) {
            return "Invalid pattern";
        }
        String bidRef = matcher.group(2);
        return bidRef.substring(0, bidRef.length() - 1);
    }
}
