package com.ke.bella.workflow.utils;

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
    private static final CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));

    @Nullable
    public static ZonedDateTime nextExecution(Cron cron) {
        ZonedDateTime now = ZonedDateTime.now();
        return nextExecution(cron, now);
    }

    @Nullable
    public static ZonedDateTime nextExecution(Cron cron, ZonedDateTime from) {
        ExecutionTime executionTime = ExecutionTime.forCron(cron);
        return executionTime.nextExecution(from).orElse(null);
    }

    public static LocalDateTime nextExecution(String cronExpression) {
        ZonedDateTime time = nextExecution(parser.parse(cronExpression));
        if(time == null) {
            return null;
        }
        return time.toLocalDateTime();
    }

    public static List<LocalDateTime> nextExecutions(String cronExpression, Integer n) {
        List<LocalDateTime> result = new ArrayList<>();
        ZonedDateTime from = ZonedDateTime.now();
        Cron cron = parser.parse(cronExpression);
        for (int i = 0; i < n; i++) {
            ZonedDateTime nextTime = nextExecution(cron, from);
            if(Objects.isNull(nextTime)) {
                break;
            }
            result.add(nextTime.toLocalDateTime());
            from = nextTime;
        }
        return result;
    }
}
