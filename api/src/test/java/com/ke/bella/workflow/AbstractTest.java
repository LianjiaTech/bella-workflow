package com.ke.bella.workflow;

import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@SpringJUnitConfig(TestApplication.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
	"spring.profiles.active=junit,test" })
public abstract class AbstractTest {
}
