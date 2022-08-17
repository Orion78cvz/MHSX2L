package mhsx;

import java.util.List;
import java.util.Map;

public class EquipmentDataTag {
	private static List<Integer> SkillList = null; 
	public static void setSkillOrder(List<Integer> order) {SkillList = order;}
	
	public EquipmentData base;
	public int slot;
	public int[] skillPoints;
	public int[] skillIndices;
	public EquipmentDataTag(EquipmentData data, Map<Integer, Boolean> inverted_skills) {
		base = data;
		
		slot = data.getSlot();
		
		//TODO: ここも探索順序のアレを使って軽減する？
		skillPoints = new int[SkillList.size()];
		int sc = 0;
		for(int i = 0; i < SkillList.size(); i++) {
			Integer sid = SkillList.get(i);
			int pt = base.getSkillPoint(sid);
			skillPoints[i] = (inverted_skills.getOrDefault(sid, false)) ? -pt : pt;
			if (pt != 0) {sc++;}
		}
		
		skillIndices = new int[sc];
		sc = 0;
		for(int i = 0; i < skillPoints.length; i++) {
			if (skillPoints[i] != 0) {
				skillIndices[sc] = i;
				sc++;
			}
		}
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(base.getName());
		sb.append("{");
		for(int i = 0; i < slot; i++) {sb.append("●");}
		sb.append("[");
		for(int p : skillPoints) {
			sb.append(Integer.toString(p));
			sb.append(",");
		}
		sb.append("][");
		for(int i : skillIndices) {
			sb.append(" ");
			sb.append(Integer.toString(i));
		}
		sb.append(" ]");
		sb.append(" }");
		return sb.toString();
	}
}
