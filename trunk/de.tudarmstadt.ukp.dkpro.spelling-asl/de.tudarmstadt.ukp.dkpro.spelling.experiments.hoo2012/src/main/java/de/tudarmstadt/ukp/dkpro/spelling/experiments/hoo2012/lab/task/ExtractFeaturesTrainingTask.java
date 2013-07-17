package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.lab.task;

import static de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.lab.task.MetaInfoTask.META_KEY;
import static org.uimafit.factory.AnalysisEngineFactory.createAggregateDescription;
import static org.uimafit.factory.AnalysisEngineFactory.createPrimitiveDescription;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.classifier.CleartkAnnotator;
import org.cleartk.classifier.jar.DefaultDataWriterFactory;
import org.cleartk.classifier.weka.singlelabel.StringWekaSerializedDataWriter;

import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContext;
import de.tudarmstadt.ukp.dkpro.lab.storage.StorageService.AccessMode;
import de.tudarmstadt.ukp.dkpro.lab.task.Discriminator;
import de.tudarmstadt.ukp.dkpro.lab.uima.task.impl.UimaTaskBase;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.HOO2012Experiments;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.featureextraction.AllFeaturesExtractor;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.featureextraction.ngram.NgramFeatureExtractor;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.meta.NGramMetaCollector;

public class ExtractFeaturesTrainingTask
    extends UimaTaskBase
{
    public static final String TRAINING_KEY = "training";

    @Discriminator String langCode;
    @Discriminator Integer topNgramsK;
    @Discriminator Boolean lowerCase;
    @Discriminator String stopwordList;
    
    private CollectionReaderDescription reader;
    private String errorClass;
    private String[] confusionSet;
    private String ngramModel;
    private Object[] additionalParameters;
    
    public ExtractFeaturesTrainingTask(
            CollectionReaderDescription reader,
            String errorClass,
            String[] confusionSet,
            Object[] additionalParameters)
    {
        this.reader = reader;
        this.errorClass = errorClass;
        this.confusionSet = confusionSet;
        this.additionalParameters = additionalParameters;
        
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
        
        File metaDir = aContext.getStorageLocation(META_KEY, AccessMode.READONLY);
        
        List<Object> parameters = new ArrayList<Object>();
        parameters.addAll(Arrays.asList(additionalParameters));
        
        parameters.addAll(Arrays.asList(
                NgramFeatureExtractor.PARAM_NGRAM_FD_FILE, metaDir.getAbsolutePath() + "/" + NGramMetaCollector.NGRAM_FD_KEY));
        parameters.addAll(Arrays.asList(
                NgramFeatureExtractor.PARAM_USE_TOP_K, topNgramsK));
        parameters.addAll(Arrays.asList(
                NgramFeatureExtractor.PARAM_LOWER_CASE, lowerCase));
        parameters.addAll(Arrays.asList(
                NgramFeatureExtractor.PARAM_STOPWORD_LIST, stopwordList));

        parameters.addAll(Arrays.asList(
                AllFeaturesExtractor.PARAM_IS_TRAINING, true,
                AllFeaturesExtractor.PARAM_ERROR_CLASS, errorClass,
                AllFeaturesExtractor.PARAM_CONFUSION_SET, confusionSet,
                CleartkAnnotator.PARAM_DATA_WRITER_FACTORY_CLASS_NAME, DefaultDataWriterFactory.class.getName(),
                DefaultDataWriterFactory.PARAM_DATA_WRITER_CLASS_NAME, StringWekaSerializedDataWriter.class.getName(),
                DefaultDataWriterFactory.PARAM_OUTPUT_DIRECTORY, arffDir.getAbsolutePath()
        ));
        
        return createAggregateDescription(
                HOO2012Experiments.getPreprocessing(langCode),
                createPrimitiveDescription(
                        AllFeaturesExtractor.class,
                        parameters.toArray()
                )
        );
    }
}