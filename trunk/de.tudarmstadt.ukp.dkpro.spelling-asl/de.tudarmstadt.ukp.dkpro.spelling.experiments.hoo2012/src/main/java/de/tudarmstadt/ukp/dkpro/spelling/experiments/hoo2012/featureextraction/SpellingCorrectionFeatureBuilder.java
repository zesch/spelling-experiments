package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.featureextraction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.classifier.Feature;
import org.cleartk.classifier.Instance;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.frequency.provider.FrequencyCountProvider;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.chunk.Chunk;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.util.MyJCasUtil;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.WekaFeature;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.feature.ClassFeature;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.feature.FloatFeature;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.feature.NominalFeature;

public class SpellingCorrectionFeatureBuilder {

	private FrequencyCountProvider provider;
	private Set<String> confusionSet;
	
	public SpellingCorrectionFeatureBuilder(FrequencyCountProvider provider, String ... confusionSet) throws IOException {
		this.provider = provider;
		
		this.confusionSet = new HashSet<String>();
		for (String setItem : confusionSet) {
			this.confusionSet.add(setItem);
		}
	}
	
	/**
	 * Returns the target words for which features should be extracted
	 * @return
	 */
	public Set<String> getConfusionSet() {
		return this.confusionSet;
	}
	
	/**
	 * Extracts all features for a given target annotation
	 * @param jcas
	 * @param target The annotaion for which the features should be extracted
	 * @param withClass If we should add the class feature (only for training)
	 * @return
	 * @throws Exception
	 */
	public Instance<ClassFeature> extract(JCas jcas, Annotation target, boolean withClass) throws Exception {
		List<Feature> data = new ArrayList<Feature>();
		
		data.addAll(extractPos(jcas, target));
		data.addAll(extractChunk(jcas, target));
		data.addAll(extractFreq(jcas, target));
		data.addAll(extractFirstChar(jcas, target));
		
		if (withClass) {
			return new Instance<ClassFeature>(new ClassFeature("outcome", target.getCoveredText()), data);
		} else {
			return new Instance<ClassFeature>(data);
		}
	}
	
	/**
	 * Extracts all features for a given target annotation. Also, adds the class
	 * features to the list
	 * @param jcas
	 * @param target
	 * @return
	 * @throws Exception
	 */
	public Instance<ClassFeature> extract(JCas jcas, Annotation target) throws Exception {
		return extract(jcas, target, true);
	}

	/**
	 * Extracts three binary classes depending on 
	 * the first char of the next word
	 * @param jcas
	 * @param target
	 * @return
	 */
	private List<Feature> extractFirstChar(JCas jcas,
			Annotation target) {
		Token t = MyJCasUtil.selectRelative(jcas, Token.class, target, +1);
		
		List<Feature> xs = new ArrayList<Feature>();
		xs.add(new NominalFeature("vowel_+1", (t.getCoveredText().toLowerCase().matches("^[aeiou].*")) ? "t" : "f"));
		xs.add(new NominalFeature("cons_+1", (t.getCoveredText().toLowerCase().matches("^[bcdfghjklmnpqrstvwxyz].*$")) ? "t" : "f"));
		xs.add(new NominalFeature("sign_+1", (t.getCoveredText().toLowerCase().matches("^[^a-z].*")) ? "t" : "f"));
		
		return xs;
	}

	/**
	 * Extracts different frequency values from the Web1T corpus
	 * @param jcas
	 * @param target
	 * @return
	 * @throws Exception
	 */
	private List<WekaFeature<?>> extractFreq(JCas jcas,
			Annotation target) throws Exception {
		List<WekaFeature<?>> xs = new ArrayList<WekaFeature<?>>();
		
		for (String a: getConfusionSet()) {
			for (String b: getConfusionSet()) {
				if (!a.equals(b)) {
					Token tm2 = MyJCasUtil.selectRelative(jcas, Token.class, target, -2);
					Token tm1 = MyJCasUtil.selectRelative(jcas, Token.class, target, -1);
					Token tp1 = MyJCasUtil.selectRelative(jcas, Token.class, target, 1);
					Token tp2 = MyJCasUtil.selectRelative(jcas, Token.class, target, 2);
					
					float f1;
					if (tm1 != null && tp1 != null) {
						f1 = getFreq(new String[]{tm1.getCoveredText(), a, tp1.getCoveredText()}, new String[]{tm1.getCoveredText(), tp1.getCoveredText()}) -
								getFreq(new String[]{tm1.getCoveredText(), b, tp1.getCoveredText()}, new String[]{tm1.getCoveredText(), tp1.getCoveredText()});
					} else {
						f1 = 0f;
					}
					xs.add(new FloatFeature(String.format("gram_-1+1_%s->%s", a, b), f1));
					
					float f2;
					if (tm1 != null && tm2 != null) {
						f2 = getFreq(new String[]{tm2.getCoveredText(), tm1.getCoveredText(), a}, new String[]{tm2.getCoveredText(), tm1.getCoveredText()}) -
								getFreq(new String[]{tm2.getCoveredText(), tm1.getCoveredText(), b}, new String[]{tm2.getCoveredText(), tm1.getCoveredText()});
					} else {
						f2 = 0f;
					}
					xs.add(new FloatFeature(String.format("gram_-2-1_%s->%s", a, b), f2));
					
					float f3;
					if (tp1 != null && tp2 != null) {
						f3 = getFreq(new String[]{a, tp1.getCoveredText(), tp2.getCoveredText()}, new String[]{tp1.getCoveredText(), tp2.getCoveredText()}) -
								getFreq(new String[]{b, tp1.getCoveredText(), tp2.getCoveredText()}, new String[]{tp1.getCoveredText(), tp2.getCoveredText()});
					} else {
						f3 = 0f;
					}
					xs.add(new FloatFeature(String.format("gram_+1+2_%s->%s", a, b), f3));
					
					float f4;
					if (tm1 != null) {
						f4 = getFreq(new String[]{tm1.getCoveredText(), a}, new String[]{tm1.getCoveredText()}) -
								getFreq(new String[]{tm1.getCoveredText(), b}, new String[]{tm1.getCoveredText()});
					} else {
						f4 = 0f;
					}
					xs.add(new FloatFeature(String.format("gram_-1_%s->%s", a, b), f4));
					
					float f5;
					if (tp1 != null) {
						f5 = getFreq(new String[]{a, tp1.getCoveredText()}, new String[]{tp1.getCoveredText()}) -
								getFreq(new String[]{b, tp1.getCoveredText()}, new String[]{tp1.getCoveredText()});
					} else {
						f5 = 0f;
					}
					xs.add(new FloatFeature(String.format("gram_+1_%s->%s", a, b), f5));
				}
			}
		}
		
		return xs;
	}

	/**
	 * Returns the normalized frequency of the given words
	 * f = freq(a) / freq(b)
	 * @param a
	 * @param b
	 * @return
	 * @throws Exception
	 */
	private float getFreq(String[] a, String[] b) throws Exception {
		long v1 = provider.getFrequency(StringUtils.join(a, " "));
		long v2 = provider.getFrequency(StringUtils.join(a, " "));
		
		if (v1 > 0 && v2 > 0) {
			return (float) v1 / (float) v2;
		} else {
			return 0;
		}
	}

	/**
	 * Extracts chunk value for the target annoation
	 * @param jcas
	 * @param target
	 * @return
	 */
	private List<WekaFeature<?>> extractChunk(JCas jcas,
			Annotation target) {
		List<WekaFeature<?>> xs = new ArrayList<WekaFeature<?>>();
		
		xs.add(new NominalFeature("chunk_-1", getChunk(jcas, target, -1)));
		xs.add(new NominalFeature("chunk_+1", getChunk(jcas, target, 1)));
		xs.add(new NominalFeature("chunk_+2", getChunk(jcas, target, 1)));
		xs.add(new NominalFeature("chunk_+1+2", getChunk(jcas, target, 1)+"-"+getChunk(jcas, target, 2)));
		
		return xs;
	}

	/**
	 * Gets the i-th chunk for the target annotaion
	 * @param jcas
	 * @param target
	 * @param i
	 * @return
	 */
	private String getChunk(JCas jcas, Annotation target, int i) {
		Token t = MyJCasUtil.selectRelative(jcas, Token.class, target, i);
		List<Chunk> chunks = JCasUtil.selectCovering(jcas, Chunk.class, t.getBegin(), t.getEnd());
		
		if (chunks.size() < 1) {
			return "O";
		}
		else {
			Chunk chunk = chunks.get(0);
			if (chunk.getBegin() == t.getBegin()) {
				return "B";
			}
			else {
				return "I";
			}
		}
	}

	/**
	 * Extracts all POS Features
	 * @param jcas
	 * @param target
	 * @return
	 */
	private List<WekaFeature<?>> extractPos(JCas jcas,
			Annotation target) {
		List<WekaFeature<?>> xs = new ArrayList<WekaFeature<?>>();
		
		xs.add(new NominalFeature("pos_-1", getPos(jcas, target, -1)));
		xs.add(new NominalFeature("pos_+1", getPos(jcas, target, 1)));
		xs.add(new NominalFeature("pos_-2", getPos(jcas, target, -2)));
		xs.add(new NominalFeature("pos_+2", getPos(jcas, target, 2)));
		xs.add(new NominalFeature("pos_-2-1", getPos(jcas, target, -2)+"-"+getPos(jcas, target, -1)));
		xs.add(new NominalFeature("pos_+1+2", getPos(jcas, target, 1)+"-"+getPos(jcas, target, 2)));
		
		return xs;
	}
	
	private String getPos(JCas jcas, Annotation target, int i) {
		Token t = MyJCasUtil.selectRelative(jcas, Token.class, target, i);
		return (t != null) ? t.getPos().getPosValue() : "XXX";
	}
}
