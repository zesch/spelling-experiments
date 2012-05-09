package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.cleartk.classifier.Feature;
import org.junit.Test;

import weka.core.Attribute;
import weka.core.Instance;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute.ClassAttribute;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute.DateAttribute;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute.FloatAttribute;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute.NominalAttribute;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.feature.FloatFeature;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.feature.NominalFeature;

public class WekaConverterTest {

	@Test
	public void testConvertNominalToWeka() {
		Set<String> a1 = new HashSet<String>(Arrays.asList(new String[]{"a", "b", "c"}));
		NominalAttribute n = new NominalAttribute("test", a1, "X");
		Attribute attr = WekaConverter.convertAttribute(n);
		
		assertTrue(attr.isNominal());
		assertEquals("test", attr.name());
		assertEquals(4, attr.numValues());
	}

	@Test
	public void testConvertClassToWeka() {
		Set<String> a1 = new HashSet<String>(Arrays.asList(new String[]{"t", "f"}));
		ClassAttribute n = new ClassAttribute("test", a1);
		Attribute attr = WekaConverter.convertAttribute(n);
		
		assertTrue(attr.isNominal());
		assertEquals("test", attr.name());
		assertEquals(2, attr.numValues());
	}
	
	@Test
	public void testConvertNumericToWeka() {
		FloatAttribute n = new FloatAttribute("test");
		Attribute attr = WekaConverter.convertAttribute(n);
		
		assertTrue(attr.isNumeric());
		assertEquals("test", attr.name());
	}
	
	@Test
	public void testConvertDateToWeka() {
		DateAttribute n = new DateAttribute("test");
		Attribute attr = WekaConverter.convertAttribute(n);
		
		assertTrue(attr.isDate());
		assertEquals("test", attr.name());
	}
	
	@Test
	public void createInstance() {
		Set<String> a = new HashSet<String>(Arrays.asList(new String[]{"a", "b"}));
		Set<String> c = new HashSet<String>(Arrays.asList(new String[]{"e", "f"}));
		Set<String> d = new HashSet<String>(Arrays.asList(new String[]{"t", "f"}));
		
		Meta m = new Meta("test", Arrays.asList(new IWekaAttribute[]{
				new NominalAttribute("a", a, "X"),
				new FloatAttribute("b"),
				new NominalAttribute("c", c, "X"),
				new ClassAttribute("d", d)
		}));
		
		List<Feature> features = new ArrayList<Feature>();
		features.add(new NominalFeature("a", "a"));
		features.add(new FloatFeature("b", 10f));
		features.add(new NominalFeature("c", "z"));
		
		Instance inst = WekaConverter.featuresToInstance(features, m);
		
		assertTrue(inst.classIsMissing());
		assertEquals("a", inst.stringValue(0));
		assertEquals(10d, inst.value(1), 0.001d);
		assertEquals("X", inst.stringValue(2));
	}
}
