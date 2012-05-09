package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute;

import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.INumericAttribute;

/**
 * A double attribute
 */
public class DoubleAttribute implements INumericAttribute {

	private String name;
	
	public DoubleAttribute(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}
}
