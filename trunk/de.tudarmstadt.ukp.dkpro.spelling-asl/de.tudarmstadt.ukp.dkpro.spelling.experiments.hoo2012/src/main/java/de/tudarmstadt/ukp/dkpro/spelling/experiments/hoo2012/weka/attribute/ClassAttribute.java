package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute;

import java.util.Set;

import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.IWekaAttribute;

/**
 * The attribute for the outcome
 */
public class ClassAttribute implements IWekaAttribute {

	private String name;
	private Set<String> nominals;
	
	public ClassAttribute(String name, Set<String> nominals) {
		this.name = name;
		this.nominals = nominals;
	}

	public Set<String> getNominals() {
		return nominals;
	}

	@Override
	public String getName() {
		return name;
	}
}