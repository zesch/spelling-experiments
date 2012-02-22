/*******************************************************************************
 * Copyright 2010
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.dkpro.spelling.experiments.eacl2012;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.resource.ExternalResourceDescription;
import org.uimafit.factory.ExternalResourceFactory;

import de.tudarmstadt.ukp.dkpro.core.api.resources.DKProContext;
import de.tudarmstadt.ukp.dkpro.core.frequency.resources.Web1TFrequencyCountResource;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.core.SpellingPipeline_Base;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.core.util.MeasureConfig;
import de.tudarmstadt.ukp.relatedness.lsr.resources.GlossOverlapRelatednessResource;
import de.tudarmstadt.ukp.relatedness.lsr.resources.JiangConrathRelatednessResource;
import de.tudarmstadt.ukp.relatedness.lsr.resources.LinRelatednessResource;
import de.tudarmstadt.ukp.relatedness.vsm.resource.VectorIndexSourceRelatednessResource;

public abstract class EACL_ExperimentsBase
    extends SpellingPipeline_Base
{
    public static final String LAB_RESULTS_FILE = "results.prop";

    public enum SupportedFrequencyProviders {
        google,
        acl,
        wikipedia
    }
    
    @SuppressWarnings("serial")
    protected static final List<Object[]> datasets = new ArrayList<Object[]>() {{
        add(new Object[] {
                "datasetPath",  "classpath*:/datasets/de/de_artificial_noun.txt",
                "languageCode", "de"});
    }};

    public static Object[][] getDatasets() {
        Object[][] datasetArray = new Object[datasets.size()][];
        for (int i=0; i<datasets.size(); i++) {
            datasetArray[i] = datasets.get(i);
        }
        return datasetArray;
    }
        
    public static Object[][] getDatasets(String languageCode) {
        List<Object[]> tmpDatasets = new ArrayList<Object[]>();
        for (int i=0; i<datasets.size(); i++) {
            if (datasets.get(i)[3].equals(languageCode)) {
                tmpDatasets.add(datasets.get(i));
            }
        }
        Object[][] datasetArray = new Object[tmpDatasets.size()][];
        for (int i=0; i<tmpDatasets.size(); i++) {
            datasetArray[i] = tmpDatasets.get(i);
        }
        
        return datasetArray;
    }
    
    @SuppressWarnings("serial")
    protected static final List<MeasureConfig> enMeasures = new ArrayList<MeasureConfig>() {{
        add(new MeasureConfig(
                JiangConrathRelatednessResource.class,
                JiangConrathRelatednessResource.PARAM_RESOURCE_NAME, "wordnet",
                JiangConrathRelatednessResource.PARAM_RESOURCE_LANGUAGE, "en"));
        add(new MeasureConfig(
                LinRelatednessResource.class,
                LinRelatednessResource.PARAM_RESOURCE_NAME, "wordnet",
                LinRelatednessResource.PARAM_RESOURCE_LANGUAGE, "en"));
        add(new MeasureConfig(
                GlossOverlapRelatednessResource.class,
                GlossOverlapRelatednessResource.PARAM_USE_PSEUDO_GLOSSES, "false",
                GlossOverlapRelatednessResource.PARAM_RESOURCE_NAME, "wordnet",
                GlossOverlapRelatednessResource.PARAM_RESOURCE_LANGUAGE, "en"));
        add(new MeasureConfig(
                VectorIndexSourceRelatednessResource.class,
                VectorIndexSourceRelatednessResource.PARAM_MODEL_LOCATION,
                getWorkspacePath("esaIndexesVector" + "/en/wp/")));
        add(new MeasureConfig(
                VectorIndexSourceRelatednessResource.class,
                VectorIndexSourceRelatednessResource.PARAM_MODEL_LOCATION,
                getWorkspacePath("esaIndexesVector" + "/en/wkt/")));
        add(new MeasureConfig(
                VectorIndexSourceRelatednessResource.class,
                VectorIndexSourceRelatednessResource.PARAM_MODEL_LOCATION,
                getWorkspacePath("esaIndexesVector" + "/en/wordnet/")));
    }};    

    @SuppressWarnings("serial")
    protected static final List<MeasureConfig> deMeasures = new ArrayList<MeasureConfig>() {{
        add(new MeasureConfig(
                JiangConrathRelatednessResource.class,
                JiangConrathRelatednessResource.PARAM_RESOURCE_NAME, "germanet",
                JiangConrathRelatednessResource.PARAM_RESOURCE_LANGUAGE, "de"));
        add(new MeasureConfig(
                LinRelatednessResource.class,
                LinRelatednessResource.PARAM_RESOURCE_NAME, "germanet",
                LinRelatednessResource.PARAM_RESOURCE_LANGUAGE, "de"));
        add(new MeasureConfig(
                GlossOverlapRelatednessResource.class,
                GlossOverlapRelatednessResource.PARAM_USE_PSEUDO_GLOSSES, "true",
                GlossOverlapRelatednessResource.PARAM_RESOURCE_NAME, "germanet",
                GlossOverlapRelatednessResource.PARAM_RESOURCE_LANGUAGE, "de"));
        add(new MeasureConfig(
                VectorIndexSourceRelatednessResource.class,
                VectorIndexSourceRelatednessResource.PARAM_MODEL_LOCATION,
                getWorkspacePath("esaIndexesVector" + "/de/wp/")));
        add(new MeasureConfig(
                VectorIndexSourceRelatednessResource.class,
                VectorIndexSourceRelatednessResource.PARAM_MODEL_LOCATION,
                getWorkspacePath("esaIndexesVector" + "/de/wkt/")));
        add(new MeasureConfig(
                VectorIndexSourceRelatednessResource.class,
                VectorIndexSourceRelatednessResource.PARAM_MODEL_LOCATION,
                getWorkspacePath("esaIndexesVector" + "/de/germanet/")));
    }};

    protected static ExternalResourceDescription getSRResource(
            MeasureConfig measure)
    {
        return ExternalResourceFactory.createExternalResourceDescription(
                measure.getResourceClass(),
                measure.getConfigParameters()
        );
    }
    
    protected static ExternalResourceDescription getFrequencyProviderResource(
            SupportedFrequencyProviders binding,
            String language) throws IOException
    {
        return getFrequencyProviderResource(binding, language, 1);
    }
    
    protected static ExternalResourceDescription getFrequencyProviderResource(
            SupportedFrequencyProviders binding,
            String language,
            int downscaleFactor) throws IOException
    {
        String workspace = "";
        if (language.equals("en")) {
            if (binding.equals(SupportedFrequencyProviders.google)) {
                workspace = "en";
            }
            else if (binding.equals(SupportedFrequencyProviders.acl)){
                workspace = "ACL_ANTHOLOGY";
            }
            else if (binding.equals(SupportedFrequencyProviders.wikipedia)){
                workspace = "wiki_en";
            }
        }
        else if (language.equals("de")) {
            if (binding.equals(SupportedFrequencyProviders.google)) {
                workspace = "de";
            }
            else if (binding.equals(SupportedFrequencyProviders.acl)){
                System.err.println("This is not going to work, but we add it for reasons of symmetry in experiments.");
                workspace = "ACL_ANTHOLOGY";
            }
            else if (binding.equals(SupportedFrequencyProviders.wikipedia)){
                workspace = "wiki_de";
            }
        }
        
        File context = DKProContext.getContext().getWorkspace("web1t");
        
        return ExternalResourceFactory.createExternalResourceDescription(
                Web1TFrequencyCountResource.class,
                Web1TFrequencyCountResource.PARAM_MIN_NGRAM_LEVEL, "1",
                Web1TFrequencyCountResource.PARAM_MAX_NGRAM_LEVEL, "3",
                Web1TFrequencyCountResource.PARAM_INDEX_PATH, new File(context, workspace).getAbsolutePath(),
                Web1TFrequencyCountResource.PARAM_SCALE_DOWN_FACTOR, Integer.toString(downscaleFactor)
        );
    }
    
    public static String getWorkspacePath(String name) {
        try {
            return DKProContext.getContext().getWorkspace(name).getAbsolutePath();
        }
        catch (IOException e) {
            System.out.println("Probably wrong workspace: " + name);
            return "";
        }
    }
}
