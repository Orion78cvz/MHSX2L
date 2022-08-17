package mhsx;

import java.util.List;
import java.util.Map;

public class EquipmentCombinationData extends EquipmentData {
	//MEMO: 中に入っている実際の型は使う側が把握している必要がある
	//MEMO: interfaceにした方がいいのだろうか？
	private List<EquipmentData> listEquipment;
	public List<EquipmentData> getEquipmentList() {return listEquipment;}
		
	public EquipmentCombinationData(String name, List<EquipmentData> comb, boolean usetreemap) {
		super(name, usetreemap);
		listEquipment = comb; //XXX: 本当はcopyすべきかも
		
		setSlot(0);
		for(EquipmentData eq : comb) {
			addSkillPoints(eq, 1);
			setSlot(getSlot() + eq.getSlot());
		}
	}
	public void addEquipment(EquipmentData equip, int num, int addslotsc) {
		for(int i = 0; i < num; i++) {
			listEquipment.add(equip);
		}
		addSkillPoints(equip, num);
		setSlot(getSlot() + equip.getSlot() * num * addslotsc);
	}
	private void addSkillPoints(EquipmentData equip, int n) {
		for (Map.Entry<Integer, Integer> se : equip.getSkillPointEntrySet()) {
			Integer sid = se.getKey();
			Integer np = getSkillPoint(sid) + se.getValue() * n;
			this.setSkillPoint(sid, np);
		}
	}
	
	@Override
	public boolean isSuperiorTo(EquipmentData tgt, SearchCondition cond) {
		EquipmentCombinationData target = (EquipmentCombinationData)tgt;
		// TODO: スロットとかの色々
		if(getSlot() != target.getSlot()) return false; //* 不十分だがとりあえず
		return isPointSuperiorTo(target, 1, 1, cond);
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
	
	public static class JewelCombinationData extends EquipmentCombinationData {
		public JewelCombinationData(String name, List<EquipmentData> comb) {
			super(name, comb, true);
		}
		@Override
		public boolean isSuperiorTo(EquipmentData tgt, SearchCondition cond) {
			JewelCombinationData target = (JewelCombinationData)tgt;
			//NOTE: スロットの振り分けをせずに埋めるので、スロット数が違うものは比較しない
			if(target.getSlot() != getSlot()) {return false;}
			return isPointSuperiorTo(target, 1, 1, cond);
		}
	}
}
