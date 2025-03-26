package hk.job.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import hk.job.annoation.ExcelColumn;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobDetail implements Serializable {
    @ExcelColumn(value = 0, label = "Title", width = "26")
    private String title;

    @ExcelColumn(value = 100, label = "Bid Ref", width = "10")
    private String bidRef;

    @ExcelColumn(value = 200, label = "Location", width = "18")
    private String location;

    @ExcelColumn(value = 300, label = "Department", width = "14")
    private String department;

    @ExcelColumn(value = 400, label = "Project Nature", width = "22")
    private String nature;

    @ExcelColumn(value = 500, label = "Duty", width = "20")
    private String duty;

    @ExcelColumn(value = 600, label = "Bid Expiry Date", format = "yyyy-mm-dd", width = "15")
    private LocalDate bidExpiryDate;

    @ExcelColumn(value = 700, label = "Contract Period From", format = "yyyy-mm-dd", width = "21")
    private LocalDate contractPeriodFrom;

    @ExcelColumn(value = 800, label = "Contract Period To", format = "yyyy-mm-dd", width = "18")
    private LocalDate contractPeriodTo;

    @ExcelColumn(value = 900, label = "Requirement", width = "28")
    private String requirement;

    @ExcelColumn(value = 1000, label = "Post Date", format = "yyyy-mm-dd", width = "12")
    private LocalDate lastUpdateDate;

    @ExcelColumn(value = 1100, label = "Goverment Exp", width = "16")
    private String govermentExp;

    @ExcelColumn(value = 1200, label = "Vacancy", width = "10")
    private Integer vacancy;

    @ExcelColumn(value = 1300, label = "Bid Round", width = "11")
    private String bidRound;

    @ExcelColumn(value = 1400, label = "Bid", width = "10")
    private String bid;

//    @ExcelColumn(value = 1500, label="Relevant Field", width="11")
//    private String relevantField;

    // For analyzer possible reference
    @ExcelColumn(value = 1500, label = "Similar Requirement", width = "17")
    @JsonIgnore
    private List<String> possibleReferences = new ArrayList<>() {
        @Override
        public String toString() {
            return this.size() > 0 ? super.toString() : "";
        }
    };

    @JsonIgnore
    private String natureAnalyze;

    @JsonIgnore
    private String dutyAnalyze;

    @JsonIgnore
    private String requirementAnalyze;

    private String jjKey;

    @JsonIgnore
    private Set<String> keywords;

}
