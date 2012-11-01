package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.meta;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;

import de.tudarmstadt.ukp.dkpro.core.api.frequency.util.FrequencyDistribution;
import de.tudarmstadt.ukp.dkpro.core.api.resources.ResourceUtils;

public abstract class FreqDistBasedMetaCollector
    extends JCasAnnotator_ImplBase
{

    public static final String PARAM_LOWER_CASE = "lowerCase";
    @ConfigurationParameter(name = PARAM_LOWER_CASE, mandatory = true, defaultValue="true")
    protected boolean lowerCase;
    
    public static final String PARAM_STOPWORD_LIST = "stopwordList";
    @ConfigurationParameter(name = PARAM_STOPWORD_LIST, mandatory = true)
    private String stopwordListLocation;
    protected Set<String> stopwords;
    protected FrequencyDistribution<String> fd;
    
    protected File fdFile;

    @Override
    public void initialize(UimaContext context)
        throws ResourceInitializationException
    {
        super.initialize(context);
        
        stopwords = getStopwordList(stopwordListLocation);
        
        fd = new FrequencyDistribution<String>();
    }

    @Override
    public void collectionProcessComplete()
        throws AnalysisEngineProcessException
    {
        super.collectionProcessComplete();
        
        try {
            fd.save(fdFile);
        }
        catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    private Set<String> getStopwordList(String stopwordListLocation)
        throws ResourceInitializationException
    {
        Set<String> stopwords = new TreeSet<String>();
        if (stopwordListLocation != null) {
            InputStream is = null;
            try {
                URL url = ResourceUtils.resolveLocation(stopwordListLocation, this, null);
                is = url.openStream();
                String content = IOUtils.toString(is, "UTF-8");
                for (String line : Arrays.asList(content.split("\n"))) {
                    if (line.length() > 0) {
                        stopwords.add(StringUtils.chomp(line));
                    }
                }
            }
            catch (IOException e) {
                throw new ResourceInitializationException(e);
            }
            finally{
                IOUtils.closeQuietly(is);
            }
        }
        return stopwords;
    }
}