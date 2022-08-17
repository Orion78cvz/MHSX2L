package mhsx;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

//　(主に)datのXMLを読んで持っておくデータベース(MonoStateパターン)
public class BaseData {
	private static Map<String, List<SkillBase>> SkillList; //SkillTypename->skillbase
	private static Map<String, SkillBase> SkillNameMap = new java.util.HashMap<>();
	private static Map<Integer, SkillBase> SkillIDMap = new java.util.HashMap<>();
	private static Map<String, SkillBase> SkillTitleMap = new java.util.HashMap<>(); //InvokedSkill->skillbase

	private static List<List<ArmorData>> ArmorList = new java.util.ArrayList<>();
	private static List<CuffData> CuffList;
	private static List<JewelData> JewelList;
	private static List<Map<String, ArmorData>> ArmorNameMap = new java.util.ArrayList<>();
	private static Map<String, CuffData> CuffNameMap;
	private static Map<String, JewelData> JewelNameMap;
	
	public SkillBase getSkill(String name) {return SkillNameMap.get(name);}
	public SkillBase getSkill(Integer id) {return SkillIDMap.get(id);}
	public SkillBase getSkillFromInvokedTitle(String name) {return SkillTitleMap.get(name);}
	public String identifyInvokedSkill(Integer id, Integer pt) {return SkillIDMap.get(id).identifyInvokedSkill(pt);}
	public Map<String, List<SkillBase>> getSkillList() {return SkillList;}
	public List<ArmorData> getArmorList(int i) {return ArmorList.get(i);}
	public List<List<ArmorData>> getArmorList() {return ArmorList;}
	public int getNumArmorKind() {return ArmorList.size();}
	public ArmorData getArmorData(String name, int p) {return ArmorNameMap.get(p).get(name);}
	public List<CuffData> getCuffList() {return CuffList;}
	public CuffData getCuffData(String name) {return CuffNameMap.get(name);}
	public List<JewelData> getJewelList() {return JewelList;}
	public JewelData getJewelData(String name) {return JewelNameMap.get(name);}
	
	//---XML読み込み
	//MEMO: SearchConditionの構築などに使用するため、SkillBaseはとにかく最初に読み込むことが必要
	public void LoadXML(String dir) { //全部
		try {
			LoadXML_SkillBase(dir);
			LoadXML_ArmorData(dir, null);
			LoadXML_CuffData(dir, null);
			LoadXML_JewelData(dir, null);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new RuntimeException(e.getMessage());
		}
	}
	public void LoadXML_SkillBase(String dir) throws ParserConfigurationException, SAXException, IOException {
		SkillNameMap.clear();
		SkillIDMap.clear();
		SkillTitleMap.clear();
		SkillList = SkillBase.LoadXML(Paths.get(dir, "SkillBase.xml").toString());
		for(Map.Entry<String, List<SkillBase>> tps : SkillList.entrySet()) {
			tps.getValue().forEach(sb -> {
				SkillNameMap.put(sb.Name, sb);
				SkillIDMap.put(sb.SkillID, sb);
				for(Map.Entry<String, Integer> iv : sb.InvocationPoints.entrySet()) {
					SkillTitleMap.put(iv.getKey(), sb);
				}
			});
		}
	}
	
	public void LoadXML_ArmorData(String dir, SearchCondition cond) throws ParserConfigurationException, SAXException, IOException {
		List<String> amfiles = java.util.Arrays.asList("EquipHead.xml", "EquipBody.xml", "EquipArm.xml", "EquipWst.xml", "EquipLeg.xml");
		ArmorList.clear();
		ArmorNameMap.clear();
		
		{
			//NOTE: 武器はcondを考慮しない
			List<ArmorData> wl = ArmorData.LoadXML(Paths.get(dir, "Weapon.xml").toString(), null);
			wl.add(0, new ArmorData("武器スロットなし")); //MEMO: 元のXMLに追加してもいいと思うが
			ArmorList.add(wl);
			ArmorNameMap.add(SetupNameMap(wl));
		}
		for(String xf : amfiles) {
			List<ArmorData> list = ArmorData.LoadXML(Paths.get(dir, xf).toString(), cond);
			ArmorList.add(list);
			ArmorNameMap.add(SetupNameMap(list));
		}
	}
	public void LoadXML_CuffData(String dir, SearchCondition cond) throws ParserConfigurationException, SAXException, IOException {
		CuffList = CuffData.LoadXML(Paths.get(dir, "SkillCuff.xml").toString(), cond);
		CuffNameMap = SetupNameMap(CuffList);
	}
	public void LoadXML_JewelData(String dir, SearchCondition cond) throws ParserConfigurationException, SAXException, IOException {
		JewelList = JewelData.LoadXML(Paths.get(dir, "Jewel.xml").toString(), cond);
		JewelNameMap = SetupNameMap(JewelList);
	}
	
	// 優越されない装備品のみ残すのに用いる(占有メモリを削減するため)
	public List<ArmorData> reduceArmorData(int p, List<Integer> rest_indices) {
		List<ArmorData> na = selectByIndices(ArmorList.get(p), rest_indices);
		ArmorList.set(p, na); 
		ArmorNameMap.set(p, SetupNameMap(na));
		return na;
	}
	public List<CuffData> reduceCuffData(List<Integer> rest_indices) {
		CuffList = selectByIndices(CuffList, rest_indices); 
		CuffNameMap = SetupNameMap(CuffList);
		return CuffList;
	}
	public List<JewelData> reduceJewelData(List<Integer> rest_indices) {
		JewelList = selectByIndices(JewelList, rest_indices); 
		JewelNameMap = SetupNameMap(JewelList);
		return JewelList;
	}
	public static <T extends EquipmentData> List<T> selectByIndices(List<T> original, List<Integer> indices) {
		List<T> ret = new java.util.ArrayList<>(indices.size());
		for(Integer i : indices) {
			ret.add(original.get(i));
		}
		return ret;
	}

	private static <T extends EquipmentData> Map<String, T> SetupNameMap(Iterable<T> items) {
		if(items == null) {return null;}
		Map<String, T> map = new java.util.HashMap<>();
		for(T e : items) {
			map.put(e.getName(), e);
		}
		return map;
	}

}
