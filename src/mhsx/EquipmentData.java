package mhsx;

import java.util.Iterator;
import java.util.List;

/*
 * 装備品のデータベースエントリ
 * */

import java.util.Map;
import java.util.Set;

public abstract class EquipmentData {
	private String Name;
	private String Classification;
	private Map<Integer, Integer> SkillPoint;
	private int Slot;

	public EquipmentData(String name, boolean treemap) {
		Name = name;
		SkillPoint = (treemap) ? new java.util.TreeMap<>() : new java.util.HashMap<>();
		//Attributes = null;
	}
	public String getName() {return Name;}
	public String getClassName() {return Classification;}
	public void setClassName(String clz) {Classification = clz;}
	public void setSlot(int slot) {Slot = slot;}
	public int getSlot() {return Slot;}
	public void setSkillPoint(Integer skillId, Integer point) {SkillPoint.put(skillId, point);}
	public Integer getSkillPoint(Integer skillId) {
		return SkillPoint.getOrDefault(skillId, 0);
	}
	public Set<Map.Entry<Integer, Integer>> getSkillPointEntrySet() {
		return SkillPoint.entrySet();
	}
	public double getSkillEfficiency(Integer skillId) {
		return (double)getSkillPoint(skillId) / getSlot();
	}
	//public static List<EquipmentData> LoadXML(String filename);

	//比較
	// ポイント比較部分は共通として、それ以外の扱いはisSuperiorToに実装する
	//TODO: マイナスポイントスキルと除外スキルの差異がある？のでちょっとおかしい気がする
	public boolean isPointSuperiorTo(EquipmentData tgt, int scalex, int scaley, SearchCondition cond) {
		for(Map.Entry<Integer, Integer> y : tgt.SkillPoint.entrySet()) {
			Integer yk = y.getKey();
			SearchCondition.SkillPointCondition spc = cond.getSPCond(yk);
			if(spc == null || (spc.pointLower == null && spc.pointUpper == null)) continue;

			Integer yv = y.getValue();
			Integer xv = SkillPoint.get(yk);

			if(xv == null) {
                if (yv > 0 || spc.pointUpper != null) return false;
			} else {
				if (xv * scalex < yv * scaley) return false;
				if (spc.pointUpper != null && xv * scalex != yv * scaley) return false;
			}
		}
		for(Map.Entry<Integer, Integer> x : SkillPoint.entrySet()) {
			Integer xk = x.getKey();
			SearchCondition.SkillPointCondition spc = cond.getSPCond(xk);
			if(spc == null || spc.pointLower == null) continue;
			
			Integer xv = x.getValue();
			if(!tgt.SkillPoint.containsKey(xk))	{
				if(xv < 0 || spc.pointUpper != null) return false;
			} //NOTE: 共にポイントを持つ場合は上で検査済み 
		}
		
		return true;
	}
	abstract public boolean isSuperiorTo(EquipmentData tgt, SearchCondition cond);
	public static <T extends EquipmentData> boolean isSuperiorTo(T a, T b, SearchCondition cond) {
		return a.isSuperiorTo(b, cond);
	}

	// 装備同士を比較し優越されないものを選ぶ (返り値はインデックス)
	// 今残っているインデックスをrestindicesに渡す  nullなら初期状態として全指定扱い
	//    ひとまずitemsの実体がArrayListでrestindicesがLinkedListであるという感覚で書いてある
	public static <T extends EquipmentData> List<Integer> ExtractSuperiorItems(List<T> items, List<Integer> restindices, SearchCondition cond) {
		if (restindices == null) {
			restindices = new java.util.LinkedList<>();
			for (int i = 0; i < items.size(); i++) {
				restindices.add(i);
			}
		}
		
		List<Integer> superiors = new java.util.LinkedList<>();
		while(true) {
			Iterator<Integer> it = restindices.iterator();
			if (!it.hasNext()) {break;} // rest.size() == 0
			
			Integer i = it.next();
			boolean sup = true;
			it.remove();
			
			EquipmentData fst = items.get(i);
			while(it.hasNext()) {
				EquipmentData tgt = items.get(it.next());
				if(EquipmentData.isSuperiorTo(fst, tgt, cond)) {
					it.remove();
					//System.out.println(String.format("%s > %s", fst.toString(), tgt.toString()));
					continue;
				} else if(EquipmentData.isSuperiorTo(tgt, fst, cond)) {
					sup = false;
					//System.out.println(String.format("%s < %s", fst.toString(), tgt.toString()));
					break;
				}
			}
			if(sup) {
				superiors.add(i);
			}
		}
		
		return superiors;
	}

}

