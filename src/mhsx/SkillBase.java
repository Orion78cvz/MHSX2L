package mhsx;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SkillBase {//implements Comparable<SkillBase> {
	public int SkillID;
	public String Name;
	public Map<String, Integer> InvocationPoints;
	
	public SkillBase(int id, String name) {
		SkillID = id;
		Name = name;
		InvocationPoints = new java.util.LinkedHashMap<>();
	}
	public void AddInvocation(String title, int point) {
		InvocationPoints.put(title,  point);
	}
	
	//与えられたポイントで発動するスキル名を返す
	public String identifyInvokedSkill(int pt) {
		String ret = null;
		for(Map.Entry<String, Integer> inv : InvocationPoints.entrySet()) {
			//NOTE: ポイントが大きい方から並んでいてその順にチェックしている(LinkedHashMapなので)前提
			int th = inv.getValue().intValue();
			if(th > 0) {
				if (pt >= th) {
					ret = inv.getKey();
					break;
				}
			} else {
				if (pt <= th) {
					ret = inv.getKey();
				} else {
					break;
				}
			}
		}
		return ret;
	}
	
	@Override
	public String toString() {
		return Name;
	}
	/*
	@Override
	public int compareTo(SkillBase sb) {
		return Integer.compare(SkillID, sb.SkillID); 
	}
	*/
	
	public static Map<String, List<SkillBase>> LoadXML(String filename) throws ParserConfigurationException, SAXException, IOException {
		SAXParserFactory spfactory = SAXParserFactory.newInstance();
		SAXParser parser = spfactory.newSAXParser();
		
		SAXProcessor proc = new SAXProcessor();
		InputStream infile = new java.io.FileInputStream(filename);
		parser.parse(infile, proc);
		infile.close();
		return proc.getResult();
	}
	
	private static class SAXProcessor extends DefaultHandler {
		private Map<String, List<SkillBase>> parseResult;
		Map<String, List<SkillBase>> getResult() {return parseResult;}

		private String currentType;
		private List<SkillBase> currentSkills;
		private SkillBase currentData;
		private List<String> stackedTag; 
		
		private Integer skillPoint;
		private StringBuilder skillTitle;
		
		@Override
		public void startDocument() throws SAXException {
			parseResult = new java.util.LinkedHashMap<>();
			
			currentType = null;
			currentSkills = null;
			
			currentData = null;
			stackedTag = new java.util.ArrayList<>(16);
			skillTitle = new StringBuilder();
		}
		@Override
		public void endDocument() throws SAXException {
			if (currentSkills != null || currentData != null) {// || !stackedTag.isEmpty()) {
				throw new SAXException("unexpected EOF");
			}
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			//String parent = (stackedTag.size() > 0) ? stackedTag.get(stackedTag.size() - 1) : null;
			stackedTag.add(qName);
			
			if (qName.equals("SkillType")) {
				currentType = attributes.getValue("TypeName");
				currentSkills = new java.util.ArrayList<>();
			} else if (qName.equals("Data")) {
				currentData = new SkillBase(Integer.parseInt(attributes.getValue("ID")), attributes.getValue("Name"));
			} else if(qName.equals("Option")) {
				skillPoint = Integer.parseInt(attributes.getValue("Point"));
			}
		}
		
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			String etn = stackedTag.remove(stackedTag.size() - 1);
			if (etn != qName) {
				throw new SAXException(String.format("unexpected closing tag: %s (%s expected)", qName, etn));
			}
			
			if (qName.equals("SkillType")) {
				parseResult.put(currentType, currentSkills);
				currentType = null;
				currentSkills = null;
			} else if (qName.equals("Data")) {
				currentSkills.add(currentData);
				currentData = null;
			} else if(qName.equals("Option")) {
				if (skillTitle.length() == 0 || skillPoint == null) {
					throw new SAXException(String.format("<Skill> parse error"));
				}
				String ttl = skillTitle.toString();
				currentData.AddInvocation(ttl, skillPoint);
				skillTitle.setLength(0); //delete(0, length)
				skillPoint = null;
			}
			
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			String parent = (stackedTag.size() > 0) ? stackedTag.get(stackedTag.size() - 1) : null;
			if (parent.equals("Option")) {
				skillTitle.append(ch, start, length);
			}
		}
	}

}
