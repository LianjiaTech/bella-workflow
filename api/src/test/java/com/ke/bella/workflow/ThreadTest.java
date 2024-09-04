package com.ke.bella.workflow;

import com.ke.bella.workflow.service.Configs;
import com.ke.bella.workflow.utils.OpenAiUtils;
import com.theokanning.openai.assistants.thread.Thread;
import com.theokanning.openai.assistants.thread.ThreadRequest;
import com.theokanning.openai.service.OpenAiService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ThreadTest {

    @BeforeAll
    public static void createThreadGiveNull() {
        Configs.API_BASE = "https://example.com/v1/";
    }

    @Test
    public void retrieveThreadGiveNull() {
        OpenAiService client = OpenAiUtils.defaultOpenAiService("8FjM3pvkLccQiAVov6VSa5P8xd9UULAa");
        Thread created = client.createThread(new ThreadRequest());
        Assertions.assertNotNull(created);
        Assertions.assertNotNull(created.getId());

        Thread retrieved = client.retrieveThread(created.getId());
        Assertions.assertNotNull(retrieved);
    }
}
