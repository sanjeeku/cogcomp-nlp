package edu.illinois.cs.cogcomp.pipeline.server;

import edu.illinois.cs.cogcomp.annotation.AnnotatorException;
import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.SerializationHelper;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.pipeline.common.PipelineConfigurator;
import edu.illinois.cs.cogcomp.pipeline.main.PipelineFactory;

import java.io.IOException;

import static spark.Spark.*;

public class MainServer {

    public static void main(String[] args) {
        port(8080);

        AnnotatorService pipeline = null;
        try {
            System.out.println("Starting to load the pipeline . . . ");

            int mb = 1024*1024;

            //Getting the runtime reference from system
            Runtime runtime = Runtime.getRuntime();

            System.out.println("##### Heap utilization statistics [MB] #####");

            //Print used memory
            System.out.println("Used Memory:"
                    + (runtime.totalMemory() - runtime.freeMemory()) / mb);

            //Print free memory
            System.out.println("Free Memory:"
                    + runtime.freeMemory() / mb);

            //Print total available memory
            System.out.println("Total Memory:" + runtime.totalMemory() / mb);

            //Print Maximum available memory
            System.out.println("Max Memory:" + runtime.maxMemory() / mb);

            ResourceManager rm = new PipelineConfigurator().getDefaultConfig();
            pipeline = PipelineFactory.buildPipeline(rm);
            System.out.println("Done with loading the pipeline  . . .");
        } catch (IOException | AnnotatorException e) {
            e.printStackTrace();
        }

        AnnotatorService finalPipeline = pipeline;
        get("/annotate", "application/json", (request, response)->{
            String text = request.queryParams("text");
            String views = request.queryParams("views");
            String output = "";
            if(views == null || text == null) {
                output = "The parameters 'text' and/or 'views' are not specified. Here is a sample input:  \n ?text=\"This is a sample sentence. I'm happy.\"&views=\"pos,ner\"";
            }
            else {
                System.out.println("Text: " + text);
                System.out.println("Views to add: " + views);
                String[] viewsInArray = views.split(",");
                System.out.println("Adding the basic annotations . . . ");
                TextAnnotation ta = finalPipeline.createBasicTextAnnotation("", "", text);
                for(String vuName : viewsInArray) {
                    System.out.println("Adding the view" + vuName);
                    finalPipeline.addView(ta, vuName);
                }
                System.out.println("Done adding the views. Deserializing the view now.");
                output = SerializationHelper.serializeToJson(ta);
                System.out.println("Done. Sending the result back. ");
            }
            return output;
        });

        post("/annotate", (request, response) ->
                "Not implemented yet: " + request.body()
        );
    }
}
