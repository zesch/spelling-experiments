package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.lab.task;

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

import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContext;
import de.tudarmstadt.ukp.dkpro.lab.storage.StorageService.AccessMode;
import de.tudarmstadt.ukp.dkpro.lab.task.Discriminator;
import de.tudarmstadt.ukp.dkpro.lab.uima.task.impl.UimaTaskBase;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.HOO2012Experiments;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.meta.NGramMetaCollector;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.meta.PosNGramMetaCollector;

public class MetaInfoTask
    extends UimaTaskBase
{	
    public static final String META_KEY = "meta";

    @Discriminator boolean lowerCase;
    @Discriminator String stopwordList;
    @Discriminator String langCode;

    private CollectionReaderDescription reader;
    private Object[] additionalParameters;

    public MetaInfoTask(CollectionReaderDescription reader, Object[] additionalParameters)
        throws InstantiationException, IllegalAccessException
    {
        this.reader = reader;
        this.additionalParameters = additionalParameters;
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
        File ngramsFile = new File(aContext.getStorageLocation(META_KEY, AccessMode.READWRITE), NGramMetaCollector.NGRAM_FD_KEY);
        File posNgramsFile = new File(aContext.getStorageLocation(META_KEY, AccessMode.READWRITE), PosNGramMetaCollector.NGRAM_FD_KEY);

        List<Object> ngramParameters = new ArrayList<Object>();
        ngramParameters.addAll(Arrays.asList(additionalParameters));
        ngramParameters.addAll(Arrays.asList(
                NGramMetaCollector.PARAM_NGRAM_FD_FILE, ngramsFile,
                NGramMetaCollector.PARAM_LOWER_CASE, lowerCase,
                NGramMetaCollector.PARAM_STOPWORD_LIST, stopwordList));
        
        AnalysisEngineDescription ngramMetaCollector = createPrimitiveDescription(
                NGramMetaCollector.class, ngramParameters.toArray()
        );
        
        List<Object> posNgramParameters = new ArrayList<Object>();
        posNgramParameters.addAll(Arrays.asList(additionalParameters));
        posNgramParameters.addAll(Arrays.asList(
                PosNGramMetaCollector.PARAM_POS_NGRAM_FD_FILE, posNgramsFile,
                PosNGramMetaCollector.PARAM_STOPWORD_LIST, stopwordList));
        
        AnalysisEngineDescription posNgramMetaCollector = createPrimitiveDescription(
                PosNGramMetaCollector.class, posNgramParameters.toArray()
        );
        
        return createAggregateDescription(
                HOO2012Experiments.getPreprocessing(langCode),
                ngramMetaCollector,
                posNgramMetaCollector
        );
    }
}