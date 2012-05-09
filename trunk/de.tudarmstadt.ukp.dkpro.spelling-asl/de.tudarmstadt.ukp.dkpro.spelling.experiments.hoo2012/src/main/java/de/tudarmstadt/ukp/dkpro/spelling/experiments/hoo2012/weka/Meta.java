package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import weka.core.Attribute;
import weka.core.Instances;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute.ClassAttribute;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute.DateAttribute;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute.DoubleAttribute;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute.FloatAttribute;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute.IntegerAttribute;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute.LongAttribute;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute.NominalAttribute;

/**
 * Data object for the meta information
 * 
 * It stores all attributes and the relation name. Also
 * some indexes are created to enabled faster access
 */
public class Meta {

	private String relation;
	private List<IWekaAttribute> attributes;
	private Map<String, IWekaAttribute> attributeIndex = new HashMap<String, IWekaAttribute>();
	private Map<String, NominalAttribute> nominalIndex = new HashMap<String, NominalAttribute>();
	private Instances basicInstances;
	
	public Meta(String relation, List<IWekaAttribute> attributes) {
		this.relation = relation;
		this.attributes = attributes;
		
		for (IWekaAttribute attr: attributes) {
			attributeIndex.put(attr.getName(), attr);
			
			if (attr instanceof NominalAttribute) {
				nominalIndex.put(attr.getName(), (NominalAttribute) attr);
			}
		}
	}
	
	/**
	 * Creates a new Weka Instances object from the 
	 * attributess
	 * @return
	 */
	public Instances createInstances() {
		if (basicInstances == null) {
			ArrayList<Attribute> attrs = new ArrayList<Attribute>();
			for (IWekaAttribute a: this.attributes) {
				attrs.add(WekaConverter.convertAttribute(a));
			}
			
			basicInstances = new Instances(relation, attrs,0);
			int index = getClassIndex();
			if (index > -1) {
				basicInstances.setClassIndex(index);
			}
		}
		
		return basicInstances;
	}
	
	/**
	 * Returns the class attribute
	 * @return
	 */
	public ClassAttribute getClassAttribute() {
		int i = getClassIndex();
		if (i < 0) {
			return null;
		}
		
		return (ClassAttribute) attributes.get(i);
	}
	
	/**
	 * Returns the class index
	 * @return
	 */
	public int getClassIndex() {
		int i = 0;
		for (IWekaAttribute attr: this.attributes) {
			if (attr instanceof ClassAttribute) {
				return i;
			}
			i++;
		}
		
		return -1;
	}
	
	/**
	 * Returns the attributes index
	 * @return
	 */
	public Map<String, IWekaAttribute> getAttributeIndex() {
		return this.attributeIndex;
	}
	
	/**
	 * Returns the nominal index
	 * @return
	 */
	public Map<String, NominalAttribute> getNominalIndex() {
		return this.nominalIndex;
	}
	
	/**
	 * Converts this meta information to xml
	 * @return
	 */
	public String toXml() {
		StringBuilder s = new StringBuilder();
		
		s.append("<wekameta>\n");
		s.append("  <relation>");
		s.append(relation);
		s.append("</relation>\n");
		s.append("  <attributes>\n");
		for (IWekaAttribute attr : attributes) {
			this.attributeToXml(attr, s);
		}
        s.append("  </attributes>\n");
        s.append("</wekameta>");
		
		return s.toString();
	}

	private void attributeToXml(IWekaAttribute attr, StringBuilder s) {
		if (attr instanceof NominalAttribute) {
			NominalAttribute a = (NominalAttribute) attr;
			
			s.append("    <attribute type=\"nominal\">\n");
			s.append("      <name>");
			s.append(attr.getName());
			s.append("</name>\n");
			s.append("      <nominals>\n");
			for (String n: a.getNominals()) {
				s.append("        <nominal>");
				s.append(n);
				s.append("</nominal>\n");
			}
			s.append("      </nominals>\n");
			s.append("      <fallback>");
			s.append(a.getFallback());
			s.append("</fallback>\n");
			s.append("    </attribute>\n");
			
		} else if (attr instanceof DateAttribute) {
			DateAttribute a = (DateAttribute) attr;
			
			s.append("    <attribute type=\"float\">\n");
			s.append("      <name>");
			s.append(attr.getName());
			s.append("</name>\n");
			s.append("      <format>");
			s.append(a.getFormat());
			s.append("</format>\n");
			s.append("    </attribute>\n");
		} else if (attr instanceof IntegerAttribute) {
			s.append("    <attribute type=\"integer\">\n");
			s.append("      <name>");
			s.append(attr.getName());
			s.append("</name>\n");
			s.append("    </attribute>\n");
		} else if (attr instanceof LongAttribute) {
			s.append("    <attribute type=\"long\">\n");
			s.append("      <name>");
			s.append(attr.getName());
			s.append("</name>\n");
			s.append("    </attribute>\n");
		} else if (attr instanceof DoubleAttribute) {
			s.append("    <attribute type=\"double\">\n");
			s.append("      <name>");
			s.append(attr.getName());
			s.append("</name>\n");
			s.append("    </attribute>\n");
		} else if (attr instanceof FloatAttribute) {
			s.append("    <attribute type=\"float\">\n");
			s.append("      <name>");
			s.append(attr.getName());
			s.append("</name>\n");
			s.append("    </attribute>\n");
		} else if (attr instanceof ClassAttribute) {
			ClassAttribute a = (ClassAttribute) attr;
			s.append("    <attribute type=\"class\">\n");
			s.append("      <name>");
			s.append(attr.getName());
			s.append("</name>\n");
			s.append("      <nominals>\n");
			for (String n: a.getNominals()) {
				s.append("        <nominal>");
				s.append(n);
				s.append("</nominal>\n");
			}
			s.append("      </nominals>\n");
			s.append("    </attribute>\n");
		} else {
			throw new IllegalArgumentException("can not convert this attribute to xml: " + attr);
		}
	}

	/**
	 * Saves a meta object to an XML file
	 * @param path
	 * @param m
	 * @throws IOException
	 */
	public static void save(String path, Meta m) throws IOException {
		FileUtils.writeStringToFile(new File(path), m.toXml());
		
	}

	/**
	 * Loads a Meta object from an XML file
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public static Meta load(String path) throws Exception {
		// Open document
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(new FileInputStream(path));
		doc.normalizeDocument();
		
		String relation = doc.getElementsByTagName("relation").item(0).getTextContent();

		List<IWekaAttribute> attrs = new ArrayList<IWekaAttribute>();
		NodeList nl = doc.getElementsByTagName("attribute");
		
		for (int i = 0; i < nl.getLength(); i++) {
			Element n = (Element) nl.item(i);
			String type = n.getAttribute("type");
			String name = n.getElementsByTagName("name").item(0).getTextContent();
			String format = "";
			Set<String> nominals = new HashSet<String>();
			String fallback = "";
			
			if (type.equals("nominal") || type.equals("class")) {
				NodeList nominalNl = n.getElementsByTagName("nominal");
				for (int j = 0; j < nominalNl.getLength(); j++) {
					nominals.add(nominalNl.item(j).getTextContent());
				}
			}
			
			if (type.equals("nominal")) {
				fallback = n.getElementsByTagName("fallback").item(0).getTextContent();
			}
			
			if (type.equals("date")) {
				format = n.getElementsByTagName("format").item(0).getTextContent();
			}
			
			if (type.equals("nominal")) {
				attrs.add(new NominalAttribute(name, nominals, fallback));
			} else if (type.equals("class")) {
				attrs.add(new ClassAttribute(name, nominals));
			} else if (type.equals("date")) {
				attrs.add(new DateAttribute(name, format));
			} else if (type.equals("double")) {
				attrs.add(new DoubleAttribute(name));
			} else if (type.equals("integer")) {
				attrs.add(new IntegerAttribute(name));
			} else if (type.equals("long")) {
				attrs.add(new LongAttribute(name));
			} else if (type.equals("float")) {
				attrs.add(new FloatAttribute(name));
			} else {
				throw new IllegalArgumentException("Can not convert '"+type+"' to attribute");
			}
		}
		
		
		return new Meta(relation, attrs);
	}

	/**
	 * Returns the relation name
	 * @return
	 */
	public String getRelation() {
		return this.relation;
	}

	/**
	 * Returns the all attributes
	 * @return
	 */
	public List<IWekaAttribute> getAttributes() {
		return this.attributes;
	}
}
