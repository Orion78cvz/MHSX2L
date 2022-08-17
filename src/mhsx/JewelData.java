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

public class JewelData extends EquipmentData {

	public JewelData(String name) {
		super(name, false);
	}

	static private BaseData skilldb = new BaseData(); //*
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName());
		sb.append("{");
		for(int i = 0; i < getSlot(); i++) {sb.append("●");}
		for (Map.Entry<Integer,Integer> se : getSkillPointEntrySet()) {
			sb.append(" ");
			sb.append(skilldb.getSkill(se.getKey()).Name);
			sb.append(se.getValue());
		}
		sb.append(" }");
		return sb.toString();
	}

	@Override
	public boolean isSuperiorTo(EquipmentData tgt, SearchCondition cond) {
		JewelData target = (JewelData)tgt;
		if(target.getSlot() != getSlot() && getSlot() != 1) {return false;}
		return isPointSuperiorTo(target, target.getSlot(), getSlot(), cond);
	}

	public static List<JewelData> LoadXML(String filename, SearchCondition cond) throws ParserConfigurationException, SAXException, IOException {
		SAXParserFactory spfactory = SAXParserFactory.newInstance();
		SAXParser parser = spfactory.newSAXParser();
		
		SAXProcessor proc = new SAXProcessor(cond);
		InputStream infile = new java.io.FileInputStream(filename);
		parser.parse(infile, proc);
		infile.close();
		return proc.getResult();
	}
	
	private static class SAXProcessor extends DefaultHandler {
		private List<JewelData> parseResult;
		List<JewelData> getResult() {return parseResult;}

		private BaseData db = new BaseData(); //* スキル名->IDの変換に必要
		private SearchCondition searchCondition;

		private JewelData currentData;
		private List<String> stackedTag; 
		
		private Integer skillPoint;
		private StringBuilder skillName;

		private int currentCategory;
		
		public SAXProcessor(SearchCondition cond) {
			super();
			searchCondition = cond;
		}

		@Override
		public void startDocument() throws SAXException {
			parseResult = new java.util.ArrayList<>();
			
			currentData = null;
			stackedTag = new java.util.ArrayList<>(16);
			currentCategory = 0;
			skillName = new StringBuilder();
		}
		@Override
		public void endDocument() throws SAXException {
			if (currentData != null) {// || !stackedTag.isEmpty()) {
				throw new SAXException("unexpected EOF");
			}
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if (currentCategory > 1) {return;}
			//String parent = (stackedTag.size() > 0) ? stackedTag.get(stackedTag.size() - 1) : null;
			stackedTag.add(qName);
			
			if (qName.equals("Data")) {
				String name = attributes.getValue("Name");
				String clz = attributes.getValue("Class");
				if (clz == null) {clz = "無";}
				if (searchCondition == null ||
						searchCondition.isSpecifiedJewelToUse(name) ||
						searchCondition.isTargetJewelClass(clz)) {
					currentData = new JewelData(name);
					currentData.setClassName(clz);
					currentData.setSlot(Integer.parseInt(attributes.getValue("Slot")));
				}
			} else if(qName.equals("Skill")) {
				if (currentData != null) {
					skillPoint = Integer.parseInt(attributes.getValue("Point"));
				}
			} else if (qName.equals("Normal") && stackedTag.size() == 1) {
				currentCategory = 1;
			}
		}
		
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			if (currentCategory > 1) {return;}
			String etn = stackedTag.remove(stackedTag.size() - 1);
			if (etn != qName) {
				throw new SAXException(String.format("unexpected closing tag: %s (%s expected)", qName, etn));
			}
			
			if (qName.equals("Data")) {
				if (currentData != null) {
					parseResult.add(currentData);
				}
				currentData = null;
			} else if (qName.equals("Normal") && stackedTag.size() == 1) {
				currentCategory = 2;
				return;
			} else if(qName.equals("Skill")) {
				if (currentData != null) {
					if (skillName.length() == 0 || skillPoint == null) {
						throw new SAXException(String.format("<Skill> parse error"));
					}
					String sn = skillName.toString();
					currentData.setSkillPoint(db.getSkill(sn).SkillID, skillPoint);
				}
				skillName.setLength(0); //delete(0, length)
				skillPoint = null;
			}
			
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			if (currentCategory > 1) {return;}
			if (currentData != null) {
				String parent = (stackedTag.size() > 0) ? stackedTag.get(stackedTag.size() - 1) : null;
				if (parent.equals("Skill")) {
					skillName.append(ch, start, length);
				}
			}
		}
	}


}
