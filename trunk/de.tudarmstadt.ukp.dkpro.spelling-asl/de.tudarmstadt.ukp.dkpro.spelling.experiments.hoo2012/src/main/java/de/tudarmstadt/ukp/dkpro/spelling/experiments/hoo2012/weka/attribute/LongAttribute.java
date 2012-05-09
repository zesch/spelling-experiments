package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute;

import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.INumericAttribute;

/**
 * A long attribute
 */
public class LongAttribute implements INumericAttribute {

	private String name;
	
	public LongAttribute(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}
}
