package mhsx;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

public class ArmorData extends EquipmentData {
	public SexType Sex;
	public HunterType Job;
	public int GoushuCount;
	public int GRankCount;
	
	public ArmorData(String name) {
		super(name, false);

		Sex = SexType.Common;
		Job = HunterType.Common;
		GoushuCount = 0;
		GRankCount = 0;
	}
	
	enum SexType {
		Common, Male, Female;
		@Override
		public String toString() {
			switch(this) {
			case Common: return "(共)";
			case Male: return "(男)";
			case Female: return "(女)";
			default: throw new IllegalStateException();
			}
		}
		public final static Map<String, SexType> Values = new java.util.HashMap<String, SexType>() {{
			put("共", Common);
			put("男", Male);
			put("女", Female);
		}}; 
	}
	enum HunterType {
		Common, Blademaster, Gunner;
		@Override
		public String toString() {
			switch(this) {
			case Common: return "(共通)";
			case Blademaster: return "(剣士)";
			case Gunner: return "(ガン)";
			default: throw new IllegalStateException();
			}
		}
		public final static Map<String, HunterType> Values = new java.util.HashMap<String, HunterType>() {{
			put("共", Common);
			put("剣士", Blademaster);
			put("ガンナー", Gunner);
		}}; 
	}
	
	static private BaseData skilldb = new BaseData(); //*
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName());
		sb.append("["); sb.append(getSlot()); sb.append("]{");
		for (Map.Entry<Integer,Integer> se : getSkillPointEntrySet()) {
			sb.append(" ");
			sb.append(skilldb.getSkill(se.getKey()).Name);
			sb.append(se.getValue());
		}
		sb.append(" }");
		/*
		sb.append(Sex.toString());
		sb.append(Job.toString());
		sb.append(" 剛");
		sb.append(Integer.toString(GoushuCount));
		if(GRankCount > 0) {
			sb.append(", G防具");
		}
		*/
		return sb.toString();
	}

	@Override
	public boolean isSuperiorTo(EquipmentData tgt, SearchCondition cond) {
		ArmorData target = (ArmorData)tgt;
		//NOTE: 性別と武器種は選別に使うだけでここでの比較はしない
		if(cond.isRequiredSkillRankup() && GoushuCount < target.GoushuCount) return false;
		if(cond.getRequiredNumGrankEquip() > 0 && GRankCount < target.GRankCount) return false;
		if(getSlot() < target.getSlot()) return false;
		return isPointSuperiorTo(target, 1, 1, cond); 
	}
	
	public static List<ArmorData> LoadXML(String filename, SearchCondition cond) throws ParserConfigurationException, SAXException, IOException {
		SAXParserFactory spfactory = SAXParserFactory.newInstance();
		SAXParser parser = spfactory.newSAXParser();
		
		SAXProcessor proc = new SAXProcessor(cond);
		InputStream infile = new java.io.FileInputStream(filename);
		parser.parse(infile, proc);
		infile.close();
		return proc.getResult();
	}
	
	private static class SAXProcessor extends DefaultHandler {
		private List<ArmorData> parseResult;
		List<ArmorData> getResult() {return parseResult;}

		private BaseData db = new BaseData(); //* スキル名->IDの変換に必要
		private SearchCondition searchCondition;
		
		private ArmorData currentData;
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
			//System.out.println("startDocument"); System.out.flush();
			parseResult = new java.util.ArrayList<>();
			
			currentData = null;
			stackedTag = new java.util.ArrayList<>(16);
			currentCategory = 0;
			skillName = new StringBuilder();
		}
		@Override
		public void endDocument() throws SAXException {
			//System.out.println("endDocument"); System.out.flush();
			if (currentData != null) {// || !stackedTag.isEmpty()) {
				throw new SAXException("unexpected EOF");
			}
		}

		private final static Map<String, Integer> TypeGoushu = new java.util.HashMap<String, Integer>() {{
			put("剛種防具", 1);
			put("天嵐防具", 1);
			put("覇種防具", 1);
			put("Ｇ級覇種防具", 1);
			put("烈種防具", 3);
			put("始種防具", 3);
		}};
		private final static Map<String, Integer> TypeGRank = new java.util.HashMap<String, Integer>() {{  
			put("Ｇ級防具",  1);
			put("Ｇ級狩護防具",  1);
			put("遷悠防具",  1);
			put("天廊防具",  1);
		}};

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if (currentCategory > 1) {return;}
			String parent = (stackedTag.size() > 0) ? stackedTag.get(stackedTag.size() - 1) : null;
			stackedTag.add(qName);
			
			if (qName.equals("Data")) {
				String name = attributes.getValue("Name");
				String clz = attributes.getValue("Class");
				SexType sex = SexType.Values.get(attributes.getValue("Sex"));
				HunterType job = HunterType.Values.get(attributes.getValue("Job"));
				if (clz == null) {clz = "無";}
				if (searchCondition == null ||
						searchCondition.isSpecifiedArmor(name) ||
						(searchCondition.isTargetArmorClass(clz) && searchCondition.isSexMatched(sex) && searchCondition.isJobMatched(job))) {
					currentData = new ArmorData(name);
					currentData.setClassName(clz);
					currentData.Sex = sex;
					currentData.Job = job;
					String tp = attributes.getValue("Type");
					currentData.GoushuCount = TypeGoushu.getOrDefault(tp, 0);
					currentData.GRankCount = TypeGRank.getOrDefault(tp, 0);
				}
			} else if(qName.startsWith("L") && parent.equals("Level")) {
				if(currentData != null) {
					currentData.setSlot(Integer.parseInt(attributes.getValue("Slot")));
				}
			} else if(qName.equals("Skill") && parent.equals("Skills")) {
				if(currentData != null) {
					skillPoint = Integer.parseInt(attributes.getValue("Point"));
				}
			} else if (qName.equals("normal") && stackedTag.size() == 1) {
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
				if(currentData != null) {
					parseResult.add(currentData);
					//System.out.println(currentData);
				}
				currentData = null;
			} else if (qName.equals("normal") && stackedTag.size() == 1) {
				currentCategory = 2;
				return;
			} else if(qName.equals("Skill")) {
				if(currentData != null) {
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
