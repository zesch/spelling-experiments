package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2011;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.pipeline.SimplePipeline;


public class RunHOO2011Evaluator extends RunHOO2011Experiments {
    
    public static void main(String[] args) throws Exception
    {
        WRITE_EDITS = false;
        
        for (int runId = 0; runId<9; runId++) {
            runEvaluator(
                    runId,
                    DATASET
            );
        }
    }
    
    private static void runEvaluator(
            int runId,
            String dataset
    )
        throws UIMAException, IOException
    {

        SimplePipeline.runPipeline(
                getReader(dataset),
                getEvaluator(runId)
        );
    }
}