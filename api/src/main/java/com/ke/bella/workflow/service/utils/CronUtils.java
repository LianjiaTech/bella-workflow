package com.ke.bella.workflow.service.utils;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

public class CronUtils {

    public static Cron parse(String cronExpression) {
        CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
        return parser.parse(cronExpression);
    }

    @Nullable
    public static LocalDateTime nextExecution(Cron cron) {
        ExecutionTime executionTime = ExecutionTime.forCron(cron);
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime nextExecution = executionTime.nextExecution(now).orElse(null);
        if(Objects.isNull(nextExecution)) {
            return null;
        }
        return nextExecution.toLocalDateTime();
    }

    public static LocalDateTime nextExecution(String cronExpression) {
        return nextExecution(parse(cronExpression));
    }

    public static List<LocalDateTime> nextExecutions(String cronExpression, Integer n) {
        List<LocalDateTime> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            LocalDateTime nextTime = nextExecution(cronExpression);
            if(Objects.isNull(nextTime)) {
                break;
            }
            result.add(nextTime);
        }
        return result;
    }
}
