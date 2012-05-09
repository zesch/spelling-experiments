/*******************************************************************************
 * Copyright 2012
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
package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.util;

import java.util.List;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.uimafit.util.JCasUtil;

public class MyJCasUtil {

	@SuppressWarnings("unchecked")
	public static <T extends Annotation> T selectRelative(JCas cas, Class<T> clazz, Annotation ann, int index) {
		if (index > 0) {
			List<T> xs = JCasUtil.selectFollowing(cas, clazz, ann, index);
			return (xs.size() >= index) ? xs.get(index-1) : null;
		} else if (index < 0) {
			List<T> xs = JCasUtil.selectPreceding(cas, clazz, ann, -index);
			return (xs.size() >= -index) ? xs.get(-index-1) : null;
		} else {
			if (clazz.isInstance(ann)) {
				return (T) ann;
			} else {
				List<T> covered = JCasUtil.selectCovered(clazz, ann);
				return (covered.size() > 0) ? covered.get(0) : null;
			}
		}
	}
}
