package edu.colorado.cs.epic.eventsapi.tasks;

import com.google.common.collect.ImmutableMultimap;
import edu.colorado.cs.epic.eventsapi.api.Event;
import edu.colorado.cs.epic.eventsapi.core.DatabaseController;
import edu.colorado.cs.epic.eventsapi.core.KubernetesController;
import io.dropwizard.servlets.tasks.Task;
import io.kubernetes.client.ApiException;
import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by admin on 19/3/19.
 */
public class SyncEventsTask extends Task {

    private final Logger logger;
    private KubernetesController k8scontroller;
    private DatabaseController dbcontroller;

    public SyncEventsTask(KubernetesController k8scontroller, DatabaseController dbcontroller) {
        super("sync");
        this.k8scontroller = k8scontroller;
        this.dbcontroller = dbcontroller;
        this.logger = Logger.getLogger(SyncEventsTask.class.getName());
    }

    @Override
    public void execute(ImmutableMultimap<String, String> immutableMultimap, PrintWriter printWriter) throws Exception {
        List<Event> k8sEventsList = k8scontroller.getActiveEvents();
        List<Event> dbActiveList = dbcontroller.getActiveEvents();
        Set<Event> toBeStopped = new HashSet<>(k8sEventsList);
        toBeStopped.removeAll(dbActiveList);
        Set<Event> toBeStarted = new HashSet<>(dbActiveList);
        toBeStarted.removeAll(k8sEventsList);

        for (Event event : toBeStopped) {
            try {
                k8scontroller.stopEvent(event.getNormalizedName());
                String out = String.format("Stopped event filter for %s", event.getNormalizedName());
                logger.info(out);
                if (printWriter != null) {
                    printWriter.println(out);
                }
            }  catch (ApiException e) {
                dbcontroller.setStatus(event.getNormalizedName(), Event.Status.FAILED);
                String out = String.format("Failed to stop filter for %s", event.getNormalizedName());
                logger.error(out, e);
                if (printWriter != null) {
                    printWriter.println(out);
                }
                throw e;
            }
        }
        for (Event event : toBeStarted) {
            try {
                k8scontroller.startEvent(event);
                String out = String.format("Started event filter for %s", event.getNormalizedName());
                logger.info(out);
                if (printWriter != null) {
                    printWriter.println(out);
                }
            } catch (ApiException e) {
                dbcontroller.setStatus(event.getNormalizedName(), Event.Status.FAILED);
                String out = String.format("Failed to start filter for %s", event.getNormalizedName());
                logger.error(out, e);
                if (printWriter != null) {
                    printWriter.println(out);
                }
                throw e;
            }
        }


        try {
            k8scontroller.setActiveStreamKeywords(dbcontroller.getActiveKeywords());
        } catch (ApiException e) {
            String out = "Failed to update keywords";
            logger.error(out, e);
            if (printWriter != null) {
                printWriter.println(out);
            }
            throw e;
        }


    }
}
