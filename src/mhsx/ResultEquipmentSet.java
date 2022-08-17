package mhsx;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ResultEquipmentSet extends EquipmentCombinationData {
	private static mhsx.BaseData db = new mhsx.BaseData();
	
	private TreeMap<Integer, String> skillsInvoked = null;
	private int numRequiredSkillsInvoked; // 要求スキル中で発動している数(最大発動数も考慮)
	private boolean flooded = false; // 要求スキルの中で優先度の関係であふれたスキルがあるかどうか
	public int attrNumMaxSkills = 10;
	public boolean attrSkillRankup = false;
	public int getNumRequiredSkillsInvoked() {return numRequiredSkillsInvoked;}
	public boolean isFlooded() {return flooded;}
	public TreeMap<Integer, String> getInvokedSkills() {
		if (skillsInvoked == null) {
			skillsInvoked = ComputeInvokedSkills();
		}
		return skillsInvoked;
	}
	private boolean jewelssorted = false;
	
	public ResultEquipmentSet(String name) {
		super(name, new java.util.ArrayList<EquipmentData>(27), true);
	}
	
	//TODO: スロットの埋め方を確認するメソッドを作る

	public void setupResultEquipments(
			List<EquipmentDataTag> armorset,
			EquipmentDataTag cuff,
			List<List<EquipmentDataTag>> plusjewels, int[][] jewelusecountsA, int[][] jewelusecountsB) {
		addArmor(armorset);
		addCuff(cuff);
		addJewels(plusjewels, jewelusecountsA, jewelusecountsB);
	}
	// 考慮外のスキルを含めたポイント合計の計算 (正規化しないオリジナルのptで計算する)
	private TreeMap<Integer, String> ComputeInvokedSkills() { //NOTE: ここでは最大スキル数は考慮しない
		TreeMap<Integer, String> ret = new TreeMap<>();
		for (Map.Entry<Integer, Integer> sp : this.getSkillPointEntrySet()) {
			Integer sid = sp.getKey();
			String ttl = db.identifyInvokedSkill(sid, sp.getValue());
			if(ttl == null) {continue;}
			ret.put(sid, ttl);
		}
		return ret;
	}
	//NOTE: ポイント条件は探索で満たしていることを前提として、要求スキルの発動数をチェックする
	//      (上位のスキルが発動することもあるのでkey(skillid)だけ見る)
	//MEMO: requirements.size() > maxskillsでないことは前提
	public int CountRequiredSkillsInvoked(Map<Integer, String> requirements, int maxskills) {
		if (getInvokedSkills().size() <= maxskills) {
			//この場合ポイント条件が満たされているなら全スキルが発動している
			numRequiredSkillsInvoked = requirements.size();
			flooded = false;
			return requirements.size();
		}
		List<Integer> invs = new java.util.ArrayList<>(getInvokedSkills().keySet());
		
		int ret = 0;
		int cc = 0;
		for(Map.Entry<Integer, String> rs : requirements.entrySet()) {
			if(cc >= maxskills) {break;}
			int o = invs.indexOf(rs.getKey());
			if (o < 0) {throw new InternalError("required point is not satisfied (logic error)");}
			if (o < maxskills) {
				ret++;
			} else {
				//NOTE: requirementsの実体もTreeMapなのでbreakでも問題ない(これ以降のスキルは追い出される)が一応
			}
			cc++;
		}
		numRequiredSkillsInvoked = ret;
		flooded = (ret < requirements.size());
		return ret;
	}
	
	private void addArmor(List<EquipmentDataTag> armorset) {
		for(EquipmentDataTag am : armorset) {
			addEquipment(am.base, 1, 1);
		}
	}
	private void addCuff(EquipmentDataTag cuff) {
		addEquipment(cuff.base, 1, 0);
	}
	private void addJewels(List<List<EquipmentDataTag>> plusjewels, int[][] jewelusecountsA, int[][] jewelusecountsB) {
		if (jewelusecountsB != null) {
			for(int si = 0; si < jewelusecountsA.length; si++) {
				for(int j = 0; j < jewelusecountsA[si].length; j++) {
					int n = jewelusecountsA[si][j] + jewelusecountsB[si][j];
					if (n == 0) continue;
					addEquipment(plusjewels.get(si).get(j).base, n, -1);
				}
			}
		} else {
			for(int si = 0; si < jewelusecountsA.length; si++) {
				for(int j = 0; j < jewelusecountsA[si].length; j++) {
					int n = jewelusecountsA[si][j];
					if (n == 0) continue;
					addEquipment(plusjewels.get(si).get(j).base, n, -1);
				}
			}
		}
	}
	
	@Override
	public boolean isPointSuperiorTo(EquipmentData tgt, int scalex, int scaley, SearchCondition cond) {
		throw new InternalError("not implemented"); 
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString()); sb.append("\n");
		for(EquipmentData eq : getEquipmentList()) {
			sb.append(eq.toString());
			sb.append("\n");
		}
		sb.append(String.join(", ", getInvokedSkills().values()));
		if(flooded) {sb.append(" (*)");}
		return sb.toString();
	}
	
	public void ComputeJewelLocation() {
		final int jsin = 7;
		
		//装飾品を必要スロット数で分類
		List<java.util.LinkedList<EquipmentData>> jewels = new java.util.ArrayList<>(4);
		for(int s = 0; s < 4; s++) {jewels.add(new java.util.LinkedList<>());}
		for(int i = jsin; i < getEquipmentList().size(); i++) {
			EquipmentData eq = getEquipmentList().get(i);
			jewels.get(eq.getSlot()).add(eq);
		}
		for(int s = 1; s < 4; s++) {
			jewels.get(s).sort(Comparator.comparing(e -> e.getName()));
		}
		
		//武具のスロット数を見て、装飾品をスロット数が多い方からgreedyに取っていく
		List<EquipmentData> seqj = new java.util.ArrayList<>(getEquipmentList().size() - jsin); //埋める順装飾品を並び替えた結果のリスト
		for(int i = 0 ; i < jsin; i++) {
			int rslt = getEquipmentList().get(i).getSlot();
			int cs = rslt;
			while(cs > 0) {
				EquipmentData pick = jewels.get(cs).pollFirst();
				if(pick == null) {
					cs--;
				} else {
					seqj.add(pick);
					rslt -= cs;
					if(rslt < cs) {cs = rslt;}
				}
			}
		}
		if (seqj.size() != getEquipmentList().size() - jsin) {
			throw new InternalError("logic error: all jewels must to be picked");
		}
		
		//* 並びを上書きする
		//MEMO: 別途リストに保存した方がいいかもしれない
		for(int i = 0; i < seqj.size(); i++) {
			getEquipmentList().set(jsin + i, seqj.get(i));
		}
		jewelssorted = true;
	}
	public String toEquipmentClip() {
		if (!jewelssorted) {ComputeJewelLocation();}
		StringBuilder sb = new StringBuilder();
		sb.append("装備クリップ\n");
		int ji = 7;
		for(int i = 0; i < 6; i++) {
			EquipmentData eq = getEquipmentList().get(i);
			sb.append(eq.getName());
			if(eq.getClassName() != null) {
				sb.append("\t");
				sb.append(eq.getClassName());
			}
			sb.append("\t");
			int rsl = eq.getSlot();
			StringBuilder jns = new StringBuilder();
			while(rsl > 0) {
				if (getEquipmentList().size() <= ji) {break;}
				EquipmentData jd = getEquipmentList().get(ji);
				if(rsl < jd.getSlot()) {break;}
				for(int s = 0; s < jd.getSlot(); s++) {sb.append("●");}
				if (jns.length() > 0) {jns.append(", ");}
				jns.append(jd.getName());
				rsl -= jd.getSlot();
				ji++;
			}
			for(int s = 0; s < rsl; s++) {sb.append("○");}
			sb.append("\t");
			sb.append(jns.toString());
			sb.append("\n");
		}
		{
			EquipmentData eq = getEquipmentList().get(6); 
			sb.append("服Pスロット２\t\t");
			for(int s = 0; s < eq.getSlot(); s++) {sb.append("★");}
			for(int s = 0; s < 2-eq.getSlot(); s++) {sb.append("☆");}
			sb.append("\t");
			sb.append(eq.getName());
			sb.append("\n");
		}
		{
			int sc = 0;
			for(Map.Entry<Integer, String> is : getInvokedSkills().entrySet()) {
				if(sc == attrNumMaxSkills) {sb.append('(');}
				if(sc % 5 != 0) {sb.append(',');}
				sb.append(is.getValue());
				sc++;
				if(sc % 5 == 0) {sb.append('\n');}
			}
			if(flooded) {sb.append(')');}
		}
		return sb.toString();
	}
	
	//serialization
	//  Map<String, List<String>>だと思って扱う
	public String toJsonString() {
		return toJsonString(false);
	}
	public String toInfoJsonString() {
		return toJsonString(true);
	}
	public String toJsonString(boolean infoonly) {
		if (!jewelssorted) {ComputeJewelLocation();}
		ObjectMapper mapper = newJsonMapper();
		Map<String, List<String>> data = new java.util.HashMap<>();
		//TODO: いちいちオブジェクトを設定するのではなくフォーマットどおりに出力するようにする
		//TODO: カフ名が組み合わせになってしまうので受け取り側で処理するなりどうにかする
		//TODO: 装飾品を出力しない場合に対応する?
		data.put("name", Arrays.asList(getName()));
		if (infoonly) {
			data.put("equips", getEquipmentList().stream().limit(7).map(e -> e.getName()).collect(Collectors.toList()));
		} else {
			data.put("equips", getEquipmentList().stream().map(e -> e.getName()).collect(Collectors.toList()));
			data.put("skills", new java.util.ArrayList<>(getInvokedSkills().values()));
		}
		data.put("eqattr_maxskills", Arrays.asList(Integer.toString(attrNumMaxSkills)));
		data.put("eqattr_rankup", Arrays.asList(Boolean.toString(attrSkillRankup)));
		data.put("numreqskills", Arrays.asList(Integer.toString(numRequiredSkillsInvoked)));
		data.put("flooded", Arrays.asList(Boolean.toString(flooded)));
		try {
			return mapper.writeValueAsString(data);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e.getMessage());
		}
	}
	public static ResultEquipmentSet readJsonString(String json) throws IOException {
		if (db.getArmorList(0).size() == 0) {throw new InternalError("database has not been loaded yet");}
		ObjectMapper mapper = newJsonMapper();
		Map<String, List<String>> data = mapper.readValue(json, new TypeReference<Map<String, List<String>>>(){});
		
		ResultEquipmentSet ret = new ResultEquipmentSet(data.get("name").get(0));
		
		List<String> equips = data.get("equips");
		for(int i = 0; i < 6; i++) {
			String n = equips.get(i);
			EquipmentData eq = db.getArmorData(n, i);
			if (eq == null) {throw new NullPointerException(String.format("armor equipment not found: %s", n));}
			ret.addEquipment(eq, 1, 1);
		}
		{ //XXX: 組み合わせを作らないといけない
			String n = equips.get(6);
			EquipmentData eq = db.getCuffData(n);
			if (eq == null) {throw new NullPointerException(String.format("cuff equipment not found: %s", n));}
			ret.addEquipment(eq, 1, 0);
		}
		for(int i = 7; i < equips.size() ; i++) {
			String n = equips.get(i);
			EquipmentData eq = db.getJewelData(n);
			if (eq == null) {throw new NullPointerException(String.format("jewel equipment not found: %s", n));}
			ret.addEquipment(eq, 1, -1);
		}
		List<String> skills = data.get("skills");
		ret.skillsInvoked = new TreeMap<>();
		for(String ttl : skills) {
			SkillBase sb = db.getSkillFromInvokedTitle(ttl);
			if (sb == null) {throw new NullPointerException(String.format("skill not found: %s", ttl));}
			ret.skillsInvoked.put(sb.SkillID, ttl); 
		}
		ret.numRequiredSkillsInvoked = Integer.parseInt(data.get("numreqskills").get(0));
		ret.flooded = Boolean.parseBoolean(data.get("flooded").get(0));
		ret.attrNumMaxSkills = Integer.parseInt(data.get("eqattr_maxskills").get(0));
		ret.attrSkillRankup = Boolean.parseBoolean(data.get("eqattr_rankup").get(0));
		
		return ret;
	}
	private static ObjectMapper newJsonMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
	    //mapper.enable(SerializationFeature.INDENT_OUTPUT);
	    return mapper;
	}

	
}
 