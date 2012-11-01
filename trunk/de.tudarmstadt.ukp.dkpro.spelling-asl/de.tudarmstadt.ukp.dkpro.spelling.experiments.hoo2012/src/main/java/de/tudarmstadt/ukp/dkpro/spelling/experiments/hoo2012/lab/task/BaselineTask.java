package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.lab.task;

import static org.uimafit.factory.AnalysisEngineFactory.createAggregateDescription;
import static org.uimafit.factory.AnalysisEngineFactory.createPrimitiveDescription;

import java.io.File;
import java.io.IOException;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContext;
import de.tudarmstadt.ukp.dkpro.lab.storage.StorageService.AccessMode;
import de.tudarmstadt.ukp.dkpro.lab.task.Discriminator;
import de.tudarmstadt.ukp.dkpro.lab.uima.task.impl.UimaTaskBase;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.HOO2012Experiments;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.baseline.BaselineCorrectionAnnotator;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.hoo2011.FixedCandidateAnnotator;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.io.HOO2012Evaluator;

public class BaselineTask
    extends UimaTaskBase
{
    
    @Discriminator String langCode;
    @Discriminator String teamId;
    @Discriminator String testDataPath;
    @Discriminator String goldDataPath;
    
    { setType("Baseline"); }

    private CollectionReaderDescription reader;
    private String targetType;
    private String confusionSetFile;
    private String replacement;

    public BaselineTask(CollectionReaderDescription reader, String targetType, String confusionSetFile, String replacement)
    {
        this.reader = reader;
        this.targetType = targetType;
        this.confusionSetFile = confusionSetFile;
        this.replacement = replacement;
    }

    @Override
    public CollectionReaderDescription getCollectionReaderDescription(TaskContext aContext)
            throws ResourceInitializationException, IOException
    {
            return reader;
    }

    @Override
    public AnalysisEngineDescription getAnalysisEngineDescription(TaskContext aContext)
            throws ResourceInitializationException, IOException
    {
        File outputDir = aContext.getStorageLocation(
                HOO2012Experiments.OUTPUT_KEY,
                AccessMode.READWRITE
        );

        return createAggregateDescription(
                createPrimitiveDescription(BreakIteratorSegmenter.class),
                createPrimitiveDescription(
                        OpenNlpPosTagger.class,
                        OpenNlpPosTagger.PARAM_LANGUAGE, langCode
                ),
                createPrimitiveDescription(
                        FixedCandidateAnnotator.class,
                        FixedCandidateAnnotator.PARAM_TYPE, targetType,
                        FixedCandidateAnnotator.PARAM_CANDIDATE_FILE, confusionSetFile
                ),
                createPrimitiveDescription(
                        BaselineCorrectionAnnotator.class,
                        BaselineCorrectionAnnotator.PARAM_REPLACEMENT, replacement
                ),
                createPrimitiveDescription(
                        HOO2012Evaluator.class,
                        HOO2012Evaluator.PARAM_OUTPUT_PATH, outputDir + "/",
                        HOO2012Evaluator.PARAM_EXTRACTION_PATH, outputDir + "/extraction/",
                        HOO2012Evaluator.PARAM_GOLD_PATH, goldDataPath,
                        HOO2012Evaluator.PARAM_TEAM_ID, teamId,
                        HOO2012Evaluator.PARAM_RUN_ID, "0",
                        HOO2012Evaluator.PARAM_WRITE_EDITS, HOO2012Experiments.WRITE_EDITS
                )
        );
    }
}
