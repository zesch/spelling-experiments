package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute;

import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.INumericAttribute;

/**
 * A integer attribute
 */
public class IntegerAttribute implements INumericAttribute {

	private String name;
	
	public IntegerAttribute(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}
}