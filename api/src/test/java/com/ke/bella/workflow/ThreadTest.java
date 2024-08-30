package com.ke.bella.workflow;

import com.theokanning.openai.assistants.thread.Thread;
import com.theokanning.openai.assistants.thread.ThreadRequest;
import com.theokanning.openai.service.OpenAiService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ThreadTest {

    @Test
    public void createThreadGiveNull() {
        OpenAiService client = new OpenAiService("8FjM3pvkLccQiAVov6VSa5P8xd9UULAa", "https://example.com/v1/");
        Thread created = client.createThread(new ThreadRequest());
        Assertions.assertNotNull(created);
        Assertions.assertNotNull(created.getId());

        Thread retrieved = client.retrieveThread(created.getId());
        Assertions.assertNotNull(retrieved);
    }
}
