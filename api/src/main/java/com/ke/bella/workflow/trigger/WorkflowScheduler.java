package com.ke.bella.workflow.trigger;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class WorkflowScheduler implements ApplicationRunner, DisposableBean {

    @Autowired
    WorkflowSchedulingTriggerHelper schedulingTriggerHelper;

    private volatile boolean scheduleThreadToStop = false;

    private Thread schedulingThread;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        schedulingThread = new Thread(this::start);
        schedulingThread.setDaemon(true);
        schedulingThread.setName("workflow-scheduler");
        schedulingThread.start();
        schedulingTriggerHelper.start();
    }

    @SuppressWarnings("all")
    public void start() {
        while (!scheduleThreadToStop) {
            try {
                int triggerNums = schedulingTriggerHelper.trySchedulingTrigger();
                while (triggerNums > 0) {
                    triggerNums = schedulingTriggerHelper.trySchedulingTrigger();
                }
                TimeUnit.MINUTES.sleep(1);
            } catch (InterruptedException e) {
                if(!scheduleThreadToStop) {
                    LOGGER.error(e.getMessage());
                }
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LOGGER.error(e.getMessage());
            }
        }
    }

    @Override
    public void destroy() throws Exception {
        scheduleThreadToStop = true;
        try {
            // give a maximum of 1 second of the thread to complete the cleanup
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage());
            Thread.currentThread().interrupt();
        }
        if(schedulingThread.getState() != Thread.State.TERMINATED) {
            schedulingThread.interrupt();
            try {
                // wait for the thread to die
                schedulingThread.join();
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        }
        schedulingTriggerHelper.stop();
    }
}
