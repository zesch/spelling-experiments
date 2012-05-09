package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute;

import java.util.Set;

import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.IWekaAttribute;

/**
 * A nominal attribute
 */
public class NominalAttribute implements IWekaAttribute {

	private String name;
	private Set<String> nominals;
	private String fallback;	
	
	public NominalAttribute(String name, Set<String> nominals, String fallback) {
		this.name = name;
		this.nominals = nominals;
		this.fallback = fallback;
	}

	public NominalAttribute(String name, Set<String> nominals) {
		this(name, nominals, "XXX");
	}

	public Set<String> getNominals() {
		return nominals;
	}

	public String getFallback() {
		return fallback;
	}

	@Override
	public String getName() {
		return name;
	}
}
