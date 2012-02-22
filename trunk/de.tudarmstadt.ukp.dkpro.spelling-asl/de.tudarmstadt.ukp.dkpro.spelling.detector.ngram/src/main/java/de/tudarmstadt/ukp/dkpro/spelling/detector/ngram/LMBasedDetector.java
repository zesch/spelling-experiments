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
package de.tudarmstadt.ukp.dkpro.spelling.detector.ngram;

import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.descriptor.ExternalResource;

import de.tudarmstadt.ukp.dkpro.core.api.frequency.provider.FrequencyCountProvider;
import de.tudarmstadt.ukp.dkpro.spelling.api.detector.Detector_Base;

/**
 * An abstract base class for LM-based RWSE detectors.
 * 
 * @author zesch
 *
 */
public abstract class LMBasedDetector
    extends Detector_Base
{
 
    public final static String FREQUENCY_PROVIDER_RESOURCE = "FrequencyProvider";
    @ExternalResource(key = FREQUENCY_PROVIDER_RESOURCE)
    protected FrequencyCountProvider provider;

    /** 
     * The parameter alpha of the MDM model which controls how much probability mass is assigned to the spelling variations.
     */
    public static final String PARAM_ALPHA = "Alpha";
    @ConfigurationParameter(name = PARAM_ALPHA, mandatory=true, defaultValue="0.99")
    protected float alpha;    

    protected static final String BOS = "<S>";

}
