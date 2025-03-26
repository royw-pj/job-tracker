package hk.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import hk.job.dto.JobDetail;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LocalStorage {

    @Getter
    private final File file;
    private final ObjectMapper objectMapper;

    public LocalStorage(String path) {
        this.file = new File(path);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    public List<JobDetail> getLocalJobList() throws IOException {
        if (!file.exists()) {
            return new ArrayList<>();
        }
        return objectMapper.readValue(file, objectMapper.getTypeFactory().constructCollectionType(List.class, JobDetail.class));
    }

    public void saveJobList(List<JobDetail> jobDetails) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, jobDetails);
    }

}
