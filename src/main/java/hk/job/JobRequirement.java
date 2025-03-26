package hk.job;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class JobRequirement {
    @AllArgsConstructor
    @Getter
    public static class JobKeyword implements Comparable<JobKeyword> {
        private String label;
        private List<String> keywords;

        @Override
        public int compareTo(JobKeyword o) {
            return label.compareTo(o.label);
        }
    }

    public static List<JobKeyword> ALL_RULES;

    static {
        ALL_RULES = new ArrayList<>();
        ALL_RULES.add(new JobKeyword("Government", List.of("(GOV)", "(GSS)","(MGP)","(IEG)")));
        ALL_RULES.add(new JobKeyword("SQL", List.of("(SQL)", "(ODB)","(DB2)", "(MSQ)", "(OPL)")));
        ALL_RULES.add(new JobKeyword("JavaScript", List.of("(JAP)")));
        ALL_RULES.add(new JobKeyword("Java", List.of("(JAV)", "(JAF)", "(JSP)", "(JDE)", "(JSF)", "(EJB)")));
        ALL_RULES.add(new JobKeyword(".Net", List.of("(MSA)", "(C#P)", "(NEC)", "(NED)", "(NDE)")));
        ALL_RULES.add(new JobKeyword("Vendor Management", List.of("(VMG)", "(OUT)")));
        ALL_RULES.add(new JobKeyword("IT Procurement", List.of("(PRO)", "(GPD)","(GTP)")));
        ALL_RULES.add(new JobKeyword("Infrastructure", List.of("(INF)", "(W10)", "(W12)", "(SVT)", "(LIN)", "(CIP)", "(ADA)", "(SAN)", "(NDN)", "(NSM)","(PLS)")));
    }
}
