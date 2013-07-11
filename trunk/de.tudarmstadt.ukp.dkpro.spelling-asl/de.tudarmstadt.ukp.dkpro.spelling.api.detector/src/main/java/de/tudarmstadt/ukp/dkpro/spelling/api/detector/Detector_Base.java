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
package de.tudarmstadt.ukp.dkpro.spelling.api.detector;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.api.resources.ResourceUtils;

/**
 * An abstract base class for RWSE detectors.
 * 
 * @author zesch
 *
 */
public abstract class Detector_Base
    extends JCasAnnotator_ImplBase
{
    
    public static final String PARAM_LANGUAGE_CODE = "LanguageCode";
    @ConfigurationParameter(name = PARAM_LANGUAGE_CODE, mandatory=true)
    protected String languageCode;

    public static final String PARAM_LOWER_CASE = "LowerCase";
    @ConfigurationParameter(name = PARAM_LOWER_CASE, mandatory=true, defaultValue="false")
    protected boolean lowerCase;

    /** The minimum length of a candidate in characters. **/
    public static final String PARAM_MIN_LENGTH = "MinLength";
    @ConfigurationParameter(name = PARAM_MIN_LENGTH, mandatory = true, defaultValue="2")
    protected int minLength;
 
    /** 
     * The maximum edit distance.
     */
    public static final String PARAM_MAX_EDIT_DISTANCE = "MaxEditDistance";
    @ConfigurationParameter(name = PARAM_MAX_EDIT_DISTANCE, mandatory=true, defaultValue="1")
    protected int maxEditDistance;

    /** 
     * A file containing the list of known words (= the vocabulary).
     * As this list will be used to test the validity of generated spelling variants,
     * it should contain word forms, not lemmas.
     * 
     * Such lists can be easily created from a corpus.
     * Google N-grams are another (noisy) source.
     */
    public static final String PARAM_VOCABULARY = "VocabularyFile";
    @ConfigurationParameter(name = PARAM_VOCABULARY, mandatory=true)
    private String vocabularyFileString;

    protected Set<String> vocabulary;

    @Override
    public void initialize(UimaContext context)
        throws ResourceInitializationException
    {
        super.initialize(context);
        
        this.vocabulary = new HashSet<String>();
        
        try {
            InputStream is = null;
            try {
                URL url = ResourceUtils.resolveLocation(vocabularyFileString, this, getContext());
                is = url.openStream();
                String content = IOUtils.toString(is, "UTF-8");
                for (String item : content.split("\n")) {
                    if (!item.startsWith("#")) {
                        vocabulary.add( item );
                    }
                }
            }
            finally{
                IOUtils.closeQuietly(is);
            }
        }
        catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }
}
