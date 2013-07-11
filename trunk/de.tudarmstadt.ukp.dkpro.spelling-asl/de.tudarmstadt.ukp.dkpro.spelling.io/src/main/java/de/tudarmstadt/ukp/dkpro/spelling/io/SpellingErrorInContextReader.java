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
package de.tudarmstadt.ukp.dkpro.spelling.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import org.apache.uima.UimaContext;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

import de.tudarmstadt.ukp.dkpro.core.api.resources.ResourceUtils;

/**
 * Reads gold standard spelling errors in context.
 *
 * <ul>
 * <li><code>InputFile</code> - the file containing the spelling error corpus</li>
 * <li><code>Language</code> (optional) - language of the input documents</li>
 * <li><code>Encoding</code> (optional) - character encoding of the input files</li>
 * </ul>
 *
 * @author zesch
 * 
 */
public abstract class SpellingErrorInContextReader extends JCasCollectionReader_ImplBase {

    private static final String DEFAULT_LANGUAGE = "en";

    public static final String PARAM_INPUT_FILE = "InputFile";
    @ConfigurationParameter(name=PARAM_INPUT_FILE, mandatory=true)
    protected String inputFileString;

    public static final String PARAM_LANGUAGE = "Language";
    @ConfigurationParameter(name=PARAM_LANGUAGE, mandatory=false, defaultValue=DEFAULT_LANGUAGE)
    protected String language;

    public static final String PARAM_ENCODING = "Encoding";
    @ConfigurationParameter(name=PARAM_ENCODING, mandatory=false, defaultValue="UTF-8")
    protected String encoding;

    protected int currentIndex;

    protected BufferedReader bufferedReader;
    
    @Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {

        
        try {
            URL resolvedURL = ResourceUtils.resolveLocation(inputFileString, this, aContext);

            if (resolvedURL.getFile().endsWith(".gz")) {
                bufferedReader = new BufferedReader(
                        new InputStreamReader(
                                new GZIPInputStream(resolvedURL.openStream()),
                                encoding
                        )
                );
            }
            else {
                bufferedReader = new BufferedReader(
                        new InputStreamReader(
                                resolvedURL.openStream(),
                                encoding
                        )
                );
            }
        }
        catch (Exception e) {
            throw new ResourceInitializationException(e);
        }

        currentIndex = 0;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[] { new ProgressImpl(currentIndex, currentIndex, Progress.ENTITIES) };
    }
}
