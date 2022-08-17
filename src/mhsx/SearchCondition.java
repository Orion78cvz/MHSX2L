package mhsx;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 検索条件データ
 *
 */
@JsonPropertyOrder(alphabetic = true) 
public class SearchCondition {
	private String label;
	private Map<Integer, SkillPointCondition> SkillPointConditionTable; //skillid->point
	private Map<Integer, String> RequiredInvokedSkillMap;
	private Map<Integer, String> RequiredEliminatedSkillMap;
	private ArmorData.SexType sex;
	private ArmorData.HunterType job;
	private boolean requiredSkillRankup;
	private int requiredNumGrankEquip;
	
	private List<String> specifiedArmorNameList; //武器(スロット)、頭～脚、カフ(カフはJewelNameの方ではなくこちらで名称指定する)
	private List<List<String>> specifiedJewelNameList; //武器～脚の部位ごと
	private List<List<String>> targetEquipmentClasses; //防具,カフ,装飾品の検索対象分類 例: ["(GP)", "(未)"]
	//NOTE: 防具はシリーズ指定とかしたいな
	
	private boolean flagCheckNumInvokedSkills; //スキルが優先度で追い出されているのをチェックするかどうか(正直 珠の選別をした分が無駄になるのでOFFがデフォルトでもいいと思う)
	
	@JsonIgnore
	static private BaseData db = new BaseData(); //* 実際これってどうすればいいんだろうか？
	
	static public class SkillPointCondition {
		public Integer pointLower = null;
		public Integer pointUpper = null;
		SkillPointCondition() {}
		SkillPointCondition(Integer lpt) {pointLower = lpt;}
		SkillPointCondition(Integer lpt, Integer upt) {pointLower = lpt; pointUpper = upt;}
	}
	
	public SearchCondition() {
		java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyyMMdd_HHmmssSSS");
		label = "sc_" + df.format(java.util.Calendar.getInstance().getTime());
		SkillPointConditionTable = new java.util.TreeMap<>(); //new java.util.HashMap<>();
		RequiredInvokedSkillMap = new java.util.TreeMap<>();
		RequiredEliminatedSkillMap = new java.util.TreeMap<>();
		sex = ArmorData.SexType.Male;
		job = ArmorData.HunterType.Blademaster;
		requiredSkillRankup = false;
		requiredNumGrankEquip = 0;
		
		flagCheckNumInvokedSkills = false;
		
		specifiedArmorNameList = new java.util.ArrayList<String>(7);
		for(int i = 0; i < 7; i++) {
			specifiedArmorNameList.add(null);
		}
		specifiedJewelNameList = new java.util.ArrayList<List<String>>(6);
		for(int i = 0; i < 6; i++) {
			specifiedJewelNameList.add(new java.util.ArrayList<String>());
		}
		targetEquipmentClasses = new java.util.ArrayList<List<String>>(3);
		for(int i = 0; i < 3; i++) {
			targetEquipmentClasses.add(new java.util.ArrayList<String>());
		}
	}
	
	public void addInvokedSkill(String invoked_title) {
		SkillBase sbase = db.getSkillFromInvokedTitle(invoked_title);
		if (sbase == null) {
			throw new RuntimeException("スキルが見つかりません: " + invoked_title);
		}
		RequiredInvokedSkillMap.put(sbase.SkillID, invoked_title);
		Integer pt = sbase.InvocationPoints.get(invoked_title);
		if (pt <= 0) {
			setUpperSkillPoint(sbase.SkillID, pt);
		} else {
			setLowerSkillPoint(sbase.SkillID, pt);
		}
	}
	public void addInvokedSkill(Iterable<String> title_list) {
		for(String skl : title_list) {
			addInvokedSkill(skl);
		}
	}
	public void addEliminatedSkill(String invoked_title) {
		SkillBase sbase = db.getSkillFromInvokedTitle(invoked_title);
		if (sbase == null) {
			throw new RuntimeException("スキルが見つかりません: " + invoked_title);
		}
		RequiredEliminatedSkillMap.put(sbase.SkillID, invoked_title);
		Integer pt = sbase.InvocationPoints.get(invoked_title);
		if (pt <= 0) {
			setLowerSkillPoint(sbase.SkillID, pt+1);
		} else {
			setUpperSkillPoint(sbase.SkillID, pt-1);
		}
	}
	public void addEliminatedSkill(Iterable<String> title_list) {
		for(String skl : title_list) {
			addEliminatedSkill(skl);
		}
	}
	public void setLowerSkillPoint(Integer skillID, Integer pointlower) {
		SkillPointCondition ent = SkillPointConditionTable.get(skillID);
		if(ent == null) {
			SkillPointConditionTable.put(skillID, new SkillPointCondition(pointlower, null));
		} else {
			ent.pointLower = pointlower; 
		}
	}
	public void setUpperSkillPoint(Integer skillID, Integer pointupper) {
		SkillPointCondition ent = SkillPointConditionTable.get(skillID);
		if(ent == null) {
			SkillPointConditionTable.put(skillID, new SkillPointCondition(null, pointupper));
		} else {
			ent.pointUpper = pointupper; 
		}
	}
	public SkillPointCondition getSPCond(Integer skillID) {
		return SkillPointConditionTable.get(skillID);
	}
	public void removeSPCond(Integer skillID) {
		SkillPointConditionTable.remove(skillID);
	}
	public Map<Integer, String> RequiredInvokedSkills() {return RequiredInvokedSkillMap;} 
	public Map<Integer, String> RequiredEliminatedSkills() {return RequiredEliminatedSkillMap;} 
	public Set<Map.Entry<Integer, SkillPointCondition>> entrySetSPCond() {return SkillPointConditionTable.entrySet();}
	public ArmorData.SexType getSex() {return sex;}
	public void setSex(ArmorData.SexType s) {
		if(s == ArmorData.SexType.Common) {
			throw new IllegalArgumentException("検索では男/女いずれかの指定が必要です");
		}
		sex = s;
	}
	public boolean isSexMatched(ArmorData.SexType s) {
		return (s == ArmorData.SexType.Common || sex == s);
	}
	public ArmorData.HunterType getJob() {return job;}
	public void setJob(ArmorData.HunterType j) {
		if(j == ArmorData.HunterType.Common) {
			throw new IllegalArgumentException("検索では剣士/ガンナーいずれかの指定が必要です");
		}
		job = j;
	}
	public boolean isJobMatched(ArmorData.HunterType j) {
		return (j == ArmorData.HunterType.Common || job == j);
	}
	public String getLabel() {return label;}
	public void setLabel(String name) {label = name;}
	public boolean isRequiredSkillRankup() {return requiredSkillRankup;}
	public void setRequiredSkillRankup(boolean r) {requiredSkillRankup = r;}
	public int getRequiredNumGrankEquip() {return requiredNumGrankEquip;}
	public void setRequiredNumGrankEquip(int gn) {
		if(gn != 0 && gn != 3 && gn != 5) {
			throw new IllegalArgumentException("G級防具の指定個数は0,3,5のいずれかです");
		}
		requiredNumGrankEquip = gn;
	}
	public void specifyArmorName(int i, String name) {
		specifiedArmorNameList.set(i, name);
	}
	public String getSpecifiedArmorName(int i) {
		return specifiedArmorNameList.get(i);
	}
	public boolean isSpecifiedArmor(String name) {
		for(int i = 1; i <= 5; i++) {
			String spa = specifiedArmorNameList.get(i);
			if (spa != null && spa.equals(name)) {return true;}
		}
		return false;
	}
	public boolean isSpecifiedCuff(String name) {
		String spa = specifiedArmorNameList.get(6);
		return (spa != null && spa.equals(name));
	}
	//public List<List<String>> getSpecifiedJewelNames() {
	//	return specifiedJewelNameList;
	//}
	public boolean isSpecifiedJewelToUse(String name) {
		for (List<String> sl : specifiedJewelNameList) {
			if (sl.contains(name)) {return true;}
		}
		return false;
	}
	public void addTargetArmorClass(String cls) {
		for(String c : cls.split(",\\s*")) {
			targetEquipmentClasses.get(0).add(c);
		}
	}
	public void addTargetCuffClass(String cls) {
		for(String c : cls.split(",\\s*")) {
			targetEquipmentClasses.get(1).add(c);
		}
	}
	public void addTargetJewelClass(String cls) {
		for(String c : cls.split(",\\s*")) {
			targetEquipmentClasses.get(2).add(c);
		}
	}
	public boolean isTargetArmorClass(String cls) { //名前が
		//NOTE: 空リストなら全て有効とみなす
		if (targetEquipmentClasses.get(0).isEmpty()) {return true;}
		return targetEquipmentClasses.get(0).contains(cls);
	}
	public boolean isTargetCuffClass(String cls) {
		if (targetEquipmentClasses.get(1).isEmpty()) {return true;}
		return targetEquipmentClasses.get(1).contains(cls);
	}
	public boolean isTargetJewelClass(String cls) {
		if (targetEquipmentClasses.get(2).isEmpty()) {return true;}
		return targetEquipmentClasses.get(2).contains(cls);
	}
	
	public boolean getFlagCheckNumInvokedSkills() {return flagCheckNumInvokedSkills;}
	public void setFlagCheckNumInvokedSkills(boolean c) {flagCheckNumInvokedSkills = c;}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{ ");

		sb.append(label);
		sb.append(": ");
		sb.append(sex.toString());
		sb.append(job.toString());
		sb.append(", { ");
		for(Map.Entry<Integer, SkillPointCondition> e : SkillPointConditionTable.entrySet()) {
			SkillBase s = db.getSkill(e.getKey().intValue());
			if(e.getValue().pointUpper != null) {
				sb.append(e.getValue().pointUpper.toString());
				sb.append(">=");
			}
			if(s == null) {
				sb.append("S[");
				sb.append(e.getKey().toString());
				sb.append("]");
			} else {
				sb.append(s.Name);
			}
			if(e.getValue().pointLower != null) {
				sb.append(">=");
				sb.append(e.getValue().pointLower.toString());
			}
			sb.append(", ");
		}
		sb.append("}");
		if(requiredSkillRankup) {
			sb.append(", Rankup");
		}
		if(requiredNumGrankEquip > 0) {
			sb.append(", G");
			sb.append(Integer.toString(requiredNumGrankEquip));
			sb.append("部位以上");
		}
		
		sb.append(" | 指定[");
		for(String an : specifiedArmorNameList) {
			if(an != null) {
				sb.append(" ");
				sb.append(an);
			}
		}
		sb.append(" ] [");
		for(List<String> nl : specifiedJewelNameList) {
			for(String an: nl) {
				sb.append(" ");
				sb.append(an);
			}
		}
		sb.append(" ]");
		
		sb.append(" | 分類[");
		for(int i = 0; i < targetEquipmentClasses.size(); i++) {
			if(targetEquipmentClasses.get(i).isEmpty()) {
				sb.append(" ALL");
			} else {
				sb.append(" [");
				for(String cl : targetEquipmentClasses.get(i)) {
					sb.append(cl);
					sb.append(", ");
				}
				sb.append("]");
			}
		}
		sb.append(" ]");
		
		sb.append(" }");
		return sb.toString();
	}
	
	private static ObjectMapper newJsonMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
	    //mapper.enableDefaultTypingAsProperty(ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE, "type");
	    //mapper.enable(SerializationFeature.INDENT_OUTPUT);
	    return mapper;
	}
	public SearchCondition copy() {
		try {
			ObjectMapper mapper = newJsonMapper(); 
			String json = mapper.writeValueAsString(this);
			//MEMO: String経由せずにできないか？
			return mapper.readValue(json, SearchCondition.class);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
	}
	public String writeJsonAsString() {
		try {
			ObjectMapper mapper = newJsonMapper(); 
			return mapper.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e.getMessage());
		}
	}
	public static SearchCondition readJsonString(String json) throws IOException {
		ObjectMapper mapper = newJsonMapper(); 
		return mapper.readValue(json, SearchCondition.class);
	}
}
