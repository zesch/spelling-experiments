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

public class ExtractFeaturesTrainingTask
    extends UimaTaskBase
{
    public static final String TRAINING_KEY = "training";

    @Discriminator String langCode;
    
    private CollectionReaderDescription reader;
    private String errorClass;
    private String[] confusionSet;
    private String ngramModel;
    
    public ExtractFeaturesTrainingTask(
            CollectionReaderDescription reader,
            String errorClass,
            String[] confusionSet,
            String ngramModel)
    {
        this.reader = reader;
        this.errorClass = errorClass;
        this.confusionSet = confusionSet;
        this.ngramModel = ngramModel;
        
        this.setType("TrainingPreprocessing" + errorClass);
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
        File arffDir = aContext.getStorageLocation(
                TRAINING_KEY,
                AccessMode.READWRITE
        );

        return createAggregateDescription(
                HOO2012Experiments.getPreprocessing(langCode),
                createPrimitiveDescription(
                        AllFeaturesExtractor.class,
                        AllFeaturesExtractor.PARAM_IS_TRAINING, true,
                        AllFeaturesExtractor.PARAM_ERROR_CLASS, errorClass,
                        AllFeaturesExtractor.PARAM_CONFUSION_SET, confusionSet,
                        CleartkAnnotator.PARAM_DATA_WRITER_FACTORY_CLASS_NAME, DefaultDataWriterFactory.class.getName(),
                        DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME, StringWekaDataWriter.class.getName(),
                        DefaultDataWriterFactory.PARAM_OUTPUT_DIRECTORY, arffDir.getAbsolutePath()
                )
        );
    }
}