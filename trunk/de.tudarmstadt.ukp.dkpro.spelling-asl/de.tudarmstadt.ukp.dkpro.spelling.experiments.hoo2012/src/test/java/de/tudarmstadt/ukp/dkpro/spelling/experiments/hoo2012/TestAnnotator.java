package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.utils.SpellingUtils;

public class TestAnnotator 
	extends JCasAnnotator_ImplBase
{

		@Override
		public void process(JCas arg0) throws AnalysisEngineProcessException {
			for (Token t: JCasUtil.select(arg0, Token.class)) {
				if (t.getCoveredText().equals("the")) {
					SpellingAnomaly a = new SpellingAnomaly(arg0, t.getBegin(), t.getEnd());
					a.setCategory("RD");
					a.setSuggestions(SpellingUtils.getSuggestedActionArray(arg0, "a"));
					a.addToIndexes();
				}
			}
		}
}
