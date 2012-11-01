package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.lab.task;

import static org.uimafit.factory.AnalysisEngineFactory.createAggregateDescription;
import static org.uimafit.factory.AnalysisEngineFactory.createPrimitiveDescription;

import java.io.File;
import java.io.IOException;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.classifier.CleartkAnnotator;
import org.cleartk.classifier.jar.DefaultDataWriterFactory;
import org.cleartk.classifier.weka.StringWekaDataWriter;

import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContext;
import de.tudarmstadt.ukp.dkpro.lab.storage.StorageService.AccessMode;
import de.tudarmstadt.ukp.dkpro.lab.task.Discriminator;
import de.tudarmstadt.ukp.dkpro.lab.uima.task.impl.UimaTaskBase;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.HOO2012Experiments;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.featureextraction.AllFeaturesExtractor;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.io.HOO2012Evaluator;

public class ExtractFeaturesTestTask
    extends UimaTaskBase
{

    public static final String TEST_KEY = "test";

    @Discriminator String langCode;
    @Discriminator String teamId;
    @Discriminator String testDataPath;
    @Discriminator String goldDataPath;
    @Discriminator String classifier;


    private CollectionReaderDescription reader;
    private String errorClass;
    private String[] confusionSet;
    private String ngramModel;
    
    public ExtractFeaturesTestTask(CollectionReaderDescription reader, String errorClass, String[] confusionSet, String ngramModel)
    {
        this.reader = reader;
        this.errorClass = errorClass;
        this.confusionSet = confusionSet;
        this.ngramModel = ngramModel;
        
        this.setType("Test" + errorClass);
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
        File metaDir = aContext.getStorageLocation(
                MetaInfoTask.META_KEY,
                AccessMode.READONLY
        );
        
        File trainingArffDir = aContext.getStorageLocation(
                ExtractFeaturesTrainingTask.TRAINING_KEY,
                AccessMode.READWRITE
        );

        File testArffDir = aContext.getStorageLocation(
                TEST_KEY,
                AccessMode.READWRITE
        );
        
        File outputDir = aContext.getStorageLocation(
                HOO2012Experiments.OUTPUT_KEY,
                AccessMode.READWRITE
        );

        return createAggregateDescription(
                HOO2012Experiments.getPreprocessing(langCode),
                createPrimitiveDescription(
                        AllFeaturesExtractor.class,
                        AllFeaturesExtractor.PARAM_IS_TRAINING, true,
                        AllFeaturesExtractor.PARAM_IS_TEST, true,   // training and test need both to be true here -> it's a hack :/
                        AllFeaturesExtractor.PARAM_TRAINING_ARFF, trainingArffDir + "/training-data.arff.gz",
                        AllFeaturesExtractor.PARAM_CLASSIFIER, classifier,
                        AllFeaturesExtractor.PARAM_ERROR_CLASS, errorClass,
                        AllFeaturesExtractor.PARAM_CONFUSION_SET, confusionSet,
                        CleartkAnnotator.PARAM_DATA_WRITER_FACTORY_CLASS_NAME, DefaultDataWriterFactory.class.getName(),
                        DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME, StringWekaDataWriter.class.getName(),
                        DefaultDataWriterFactory.PARAM_OUTPUT_DIRECTORY, testArffDir.getAbsolutePath()
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