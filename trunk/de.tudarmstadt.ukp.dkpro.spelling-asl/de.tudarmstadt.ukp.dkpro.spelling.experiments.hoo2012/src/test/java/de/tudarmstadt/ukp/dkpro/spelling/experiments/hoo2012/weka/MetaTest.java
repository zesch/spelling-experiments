package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute.ClassAttribute;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute.FloatAttribute;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute.NominalAttribute;

public class MetaTest {

	@Test
	public void testGetClassAttribute() {
		Set<String> a1 = new HashSet<String>(Arrays.asList(new String[]{"a", "b"}));
		Set<String> out = new HashSet<String>(Arrays.asList(new String[]{"t", "f"}));
		
		Meta m = new Meta("test", Arrays.asList(new IWekaAttribute[]{
				new NominalAttribute("t", a1, "X"),
				new ClassAttribute("c", out)
		}));
		
		assertEquals("c", m.getClassAttribute().getName());
		assertArrayEquals(new String[]{"f", "t"}, m.getClassAttribute().getNominals().toArray());
	}
	
	@Test
	public void testGetClassIndex() {
		Set<String> a1 = new HashSet<String>(Arrays.asList(new String[]{"a", "b"}));
		Set<String> out = new HashSet<String>(Arrays.asList(new String[]{"t", "f"}));
		
		Meta m = new Meta("test", Arrays.asList(new IWekaAttribute[]{
				new NominalAttribute("t", a1, "X"),
				new ClassAttribute("c", out)
		}));
		
		assertEquals(1, m.getClassIndex());
	}
	
	@Test
	public void testToXML() throws Exception {
		Set<String> a1 = new HashSet<String>(Arrays.asList(new String[]{"a", "b"}));
		Set<String> out = new HashSet<String>(Arrays.asList(new String[]{"t", "f"}));
		
		Meta m = new Meta("test", Arrays.asList(new IWekaAttribute[]{
				new NominalAttribute("t", a1, "X"),
				new FloatAttribute("f"),
				new ClassAttribute("c", out)
		}));
		
		Meta.save("target/test_meta.xml", m);
		Meta m2 = Meta.load("target/test_meta.xml");
		
		assertEquals(m.getRelation(), m2.getRelation());
		assertEquals(m.getAttributes().size(), m2.getAttributes().size());
	}

}
