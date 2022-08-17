package mhsx;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import mhsx.EquipmentCombinationData.JewelCombinationData;

import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedList;

/*
 * 探索に使用する装備品の選別など
 * */

public class SearchItems {
	private SearchCondition SearchCondition;
	private Map<Integer, List<Boolean>> flagSkillPointMonotone; 
	private Map<Integer, Boolean> flagSkillPointInverted;

	//MEMO: この辺りはローカル変数でいい
	private List<List<ArmorData>> ArmorList;
	private List<CuffData> CuffList;
	private List<JewelData> JewelList;
	private List<EquipmentCombinationData> CuffCombinations;
	//private Map<Integer, List<Integer>> PlusJewelIndices;

	//public boolean UseJewelCombination = false;
	public List<Integer> SkillFillingOrder;
	public List<List<EquipmentDataTag>> ArmorDataTags;
	public List<EquipmentDataTag> CuffDataTags;
	public List<List<EquipmentDataTag>> PlusJewelDataTags;
	public int[] PlusJewelMaxEfficiency_Point;
	public int[] PlusJewelMaxEfficiency_Slot;
	public List<Boolean> JewelSlotUsage; 
	
	public static final int[] numInvokedSkillsWithGRankArmors = {10, 10, 10, 11, 11, 12};
	
	//* 基本的にはこれを呼べばいいようにしてある
	public void PrepareEquipmentDataTags(BaseData db, SearchCondition condition, boolean modifydb) throws IOException {
		SearchCondition = condition.copy();
		CheckPointCondition(SearchCondition);
		
		ExtractCandidateEquipment(db, SearchCondition, modifydb);
		
		CuffCombinations = MakeCuffPatterns(CuffList, SearchCondition);
		
		//UseJewelCombination = makejewelcomb;
		if (false) {
			List<JewelCombinationData> JewelCombinations = MakeJewelCombinations(JewelList, condition, 3);
			throw new InternalError("omitted");
		} else {
			Map<Integer, List<Integer>> PlusJewelIndices = PickupPlusEquipmentIndices(JewelList, SearchCondition);
			
			flagSkillPointMonotone = CheckSkillpointMonotone(ArmorList, CuffCombinations, JewelList, PlusJewelIndices, SearchCondition);
			SkillFillingOrder = SortSkillOrder(PlusJewelIndices, SearchCondition);
			
			ReducePlusEquipments(JewelList, PlusJewelIndices, SkillFillingOrder, SearchCondition);
			
			MakeEquipmentDataTags(ArmorList, CuffCombinations, JewelList, PlusJewelIndices, SearchCondition);
		}
	}
	//　Conditionの妥当性チェックと内部フラグの整理
	public void CheckPointCondition(SearchCondition condition) {
		if (condition.RequiredInvokedSkills().isEmpty()) {
			throw new UnsupportedOperationException("スキルを指定してください");
		}
		//NOTE: 上限設定されているものはDataTagのポイントを反転して正方向の探索に統一する
		flagSkillPointInverted = new java.util.HashMap<>(); 
		for(Map.Entry<Integer, SearchCondition.SkillPointCondition> cnd : condition.entrySetSPCond()) {
			Integer sid = cnd.getKey();
			SearchCondition.SkillPointCondition spc = cnd.getValue();
			if (spc.pointLower != null && spc.pointUpper != null) {
				throw new UnsupportedOperationException("(未実装)スキル指定と除外指定の両方に設定することはできません");
			}
			
			if (spc.pointUpper != null) {
				flagSkillPointInverted.put(sid, true);
			}
		}
	}
	
	// 検索条件に適合する装備品を抽出する
	public void ExtractCandidateEquipment(BaseData db, SearchCondition condition, boolean modifydb) throws IOException {
		//NOTE: conditionとのマッチはLoadXMLでもできるが判定メソッドを統一するべきだと思う
		{
			//TODO: 指定装飾品の扱いを考える
			List<Integer> ma = IntStream.range(0, db.getJewelList().size())
					.filter(i -> condition.isTargetJewelClass(db.getJewelList().get(i).getClassName()))
					.boxed()
					.collect(Collectors.toCollection(LinkedList::new));
			List<Integer> superiors = EquipmentData.ExtractSuperiorItems(db.getJewelList(), ma, condition);
			JewelList = (modifydb) ? db.reduceJewelData(superiors) : BaseData.selectByIndices(db.getJewelList(), superiors);
		}
		{
			List<Integer> superiors;
			String ordered = condition.getSpecifiedArmorName(6);
			if(ordered != null) {
				CuffData cd = db.getCuffData(ordered);
				if (cd == null) {
					throw new IOException("指定された装備品(スキルカフ)が見つかりません: " + ordered);
				}
				superiors = new java.util.ArrayList<>(1);
				superiors.add(db.getCuffList().indexOf(cd));
			} else {
				List<Integer> ma = IntStream.range(0, db.getCuffList().size())
						.filter(i -> condition.isTargetCuffClass(db.getCuffList().get(i).getClassName()))
						.boxed()
						.collect(Collectors.toCollection(LinkedList::new));
				superiors = EquipmentData.ExtractSuperiorItems(db.getCuffList(), ma, condition);
			}
			CuffList = (modifydb) ? db.reduceCuffData(superiors) : BaseData.selectByIndices(db.getCuffList(), superiors);
		}
		
		ArmorList = new java.util.ArrayList<>(db.getNumArmorKind());
		for(int pi = 0; pi < db.getNumArmorKind(); pi++) {
			List<Integer> superiors;
			String ordered = condition.getSpecifiedArmorName(pi);
			if (ordered != null) {
				ArmorData amd = db.getArmorData(ordered, pi);
				if (amd == null) {
					throw new IOException("指定された装備品が見つかりません: " + ordered);
				}
				superiors = new java.util.ArrayList<>(1);
				superiors.add(db.getArmorList(pi).indexOf(amd));
			} else {
				if(pi == 0) { //武器は指定がなければスロなし指定扱い
					superiors = new java.util.ArrayList<>(1);
					superiors.add(0);
				} else {
					List<ArmorData> aml = db.getArmorList(pi);
					List<Integer> ma = IntStream.range(0, aml.size())
							.filter(i -> condition.isTargetArmorClass(aml.get(i).getClassName()) && condition.isSexMatched(aml.get(i).Sex) && condition.isJobMatched(aml.get(i).Job))
							.boxed()
							.collect(Collectors.toCollection(LinkedList::new));
					superiors = EquipmentData.ExtractSuperiorItems(db.getArmorList(pi), ma, condition);
				}
			}
			ArmorList.add((modifydb) ? db.reduceArmorData(pi, superiors) : BaseData.selectByIndices(db.getArmorList(pi), superiors));
		}
	}
	
	// 2スロを埋めるパターンを作る
	// 優先順位は 2スロカフ > 1スロカフ1個 > 1スロカフ2個 
	public List<EquipmentCombinationData> MakeCuffPatterns(Iterable<CuffData> cufflist, SearchCondition condition) {
		List<EquipmentCombinationData> pats = new java.util.ArrayList<>();
		
		if (condition.getSpecifiedArmorName(6) == null) { //装備指定されていない場合のみ
			pats.add(new EquipmentCombinationData("空き", new java.util.ArrayList<EquipmentData>(0), false));
		}
		
		List<CuffData> oneslots = new java.util.ArrayList<>(); 
		for(CuffData cd : cufflist) {
			int slt = cd.getSlot();
			if (slt == 1) {
				oneslots.add(cd);
			} else {
				List<EquipmentData> ent = new java.util.ArrayList<>(1);
				ent.add(cd);
				pats.add(new EquipmentCombinationData(cd.getName(), ent, false));
			}
		}
		for(CuffData cd : oneslots) {
			List<EquipmentData> ent = new java.util.ArrayList<>(1);
			ent.add(cd);
			pats.add(new EquipmentCombinationData(cd.getName(), ent, false));
		}

		for(int i = 0; i < oneslots.size(); i++) {
			for(int j = i; j < oneslots.size(); j++) {
				List<EquipmentData> ent = new java.util.ArrayList<>(2);
				CuffData fst = oneslots.get(i), snd = oneslots.get(j);
				ent.add(fst);
				ent.add(snd);
				pats.add(new EquipmentCombinationData(fst.getName() + ", " + snd.getName(), ent, false));
			}
		}
		
		List<Integer> superiors = EquipmentData.ExtractSuperiorItems(pats, null, condition);
		List<EquipmentCombinationData> ret = BaseData.selectByIndices(pats, superiors); 
		
		return ret;
	}
	
	// 検索条件の各スキルごとに, 正方向のポイントを持っている装備品をまとめる(通常は装飾品に使う)
	public <T extends EquipmentData> Map<Integer, List<Integer>> PickupPlusEquipmentIndices(List<T> equiplist, SearchCondition condition) {
		Map<Integer, List<Integer>> ret = new java.util.HashMap<>();
		for(Map.Entry<Integer, SearchCondition.SkillPointCondition> cnd : condition.entrySetSPCond()) {
			Integer sid = cnd.getKey();
			int sg = flagSkillPointInverted.getOrDefault(sid, false) ? -1 : 1;
			List<Integer> pjl = new java.util.LinkedList<>();
			for(int i = 0; i < equiplist.size(); i++) {
				Integer pt = equiplist.get(i).getSkillPoint(sid);
				if (pt == 0) {continue;}
				if (sg * pt > 0) {
					pjl.add(i);
				}
			}
			ret.put(sid, pjl);
		}
		return ret;
	}

	// 探索候補となる装備品によってポイントが単調に増加するかどうか(負のポイントが存在しないかどうか)を調べる
	//  [0]: 検索条件の上で単調性を利用しないように指示するもの, [1-3]: 装飾品、カフ、防具にマイナスポイントをもつもの 
	public <TA extends EquipmentData, TC extends EquipmentData, TJ extends EquipmentData>
	 Map<Integer, List<Boolean>> CheckSkillpointMonotone(List<List<TA>> armors, List<TC> cuffs, List<TJ> jewels, Map<Integer, List<Integer>> jewelindices, SearchCondition condition) {
		Map<Integer, List<Boolean>> ret = new java.util.HashMap<>(); 
		for (Map.Entry<Integer, SearchCondition.SkillPointCondition> cnd : condition.entrySetSPCond()) {
			Integer sid = cnd.getKey();
			List<Boolean> mt = new java.util.ArrayList<>(4);
			mt.add(true); //NOTE: 上限と下限が同時に設定されている場合などがfalseだが今は対応していないので
			{
				//NOTE: PlusJewels全体を検査する(PlusJewelData.get(sid)は正方向のポイントを持つものだけなので当然)
				Stream<TJ> fs = jewelindices.entrySet().stream().flatMap(t -> t.getValue().stream()).map(j -> jewels.get(j));
				mt.add(isSkillpointMonotone(sid, fs));
			}
			mt.add(isSkillpointMonotone(sid, cuffs.stream()));
			//MEMO: 探索の初期値に正しく含めていれば指定装備品のみ使われる部位はカウントする必要はない .filter(l -> l.size() > 1)
			mt.add(isSkillpointMonotone(sid, armors.stream().flatMap(l -> l.stream())));
			ret.put(sid, mt);
		}
		return ret;
	}
	public <T extends EquipmentData> Boolean isSkillpointMonotone(Integer skillid, Stream<T> equips) {
		int sg = flagSkillPointInverted.getOrDefault(skillid, false) ? -1 : 1;
		return equips.allMatch(e -> sg * e.getSkillPoint(skillid) >= 0);
	}
	
	// 装飾品によってスキルを埋めていく順序を決定する
	public List<Integer> SortSkillOrder(Map<Integer, List<Integer>> plusjews, SearchCondition condition) {
		List<Integer> ret = new java.util.ArrayList<>(plusjews.keySet());
		ret.sort(Comparator.comparing((Integer sid) -> (condition.RequiredEliminatedSkills().get(sid) != null) ? 1 : 0).thenComparing(sid -> plusjews.get(sid).size()));
		return ret;
	}
	
	// スキルの順序を考慮して、PlusJewelを削減する
	//   monotoneなスキルは一旦埋まったら後続の探索で考慮する必要がなくなる
	//NOTE: 一旦これでPlusJewelをいじった後に探索順序を変えてはいけない
	//MEMO: これで減った後flagSkillPointMonotoneがfalse->trueになる場合があるのでは？
	public <T extends EquipmentData> void ReducePlusEquipments(List<T> jewellist, Map<Integer, List<Integer>> plusjewels, Iterable<Integer> skillorder, SearchCondition condition) throws IOException { 
		SearchCondition curcond = condition.copy();
		for(Integer sid : skillorder) {
			List<Integer> pjinds = plusjewels.get(sid);
			List<Integer> sup = EquipmentData.ExtractSuperiorItems(jewellist, pjinds, curcond);
			if (pjinds.size() != sup.size()) {
				//System.out.println(String.format("reduced(id=%d): %d => %d", sid, pjl.size(), sup.size()));
				//pjl = BaseData.selectByIndices(pjl, sup);
				plusjewels.put(sid, sup);
				pjinds = sup;
			}
			
			// PlusJewelsはPoint/Slot(大きい順), Slot(小さい順)に並べる
			int sg = flagSkillPointInverted.getOrDefault(sid, false) ? -1 : 1;
			Comparator<Integer> cmp = Comparator.comparing((Integer ji) -> sg * jewellist.get(ji).getSkillPoint(sid) * 6 / jewellist.get(ji).getSlot(), Comparator.reverseOrder()).thenComparing((Integer ji) -> jewellist.get(ji).getSlot());
			pjinds.sort(cmp);
			
			List<Boolean> mt = flagSkillPointMonotone.get(sid); 
			if (mt.get(0) && mt.get(1)) {
				curcond.removeSPCond(sid);
			}
		}
	}
	
	public void SortEquipmentItems() {
		//TODO: 防具やカフの並び順を考える(評価値)
		
		/*
		// PlusJewelsはPoint/Slot(大きい順), Slot(小さい順)に並べる
		for(Integer sid : SkillFillingOrder) {
			List<Integer> pjinds = PlusJewelIndices.get(sid);
			int sg = flagSkillPointInverted.getOrDefault(sid, false) ? -1 : 1;
			Comparator<Integer> cmp = Comparator.comparing((Integer ji) -> sg * jewellist.get(ji).getSkillPoint(sid) * 6 / jewellist.get(ji).getSlot(), Comparator.reverseOrder()).thenComparing((Integer ji) -> jewellist.get(ji).getSlot());
			pjinds.sort(cmp);
		}
		*/
	}
	public void CheckJewelSlotUsage(List<List<EquipmentDataTag>> plusjewtags) { //1-3それぞれ使う装飾品があるかどうか
		JewelSlotUsage = new java.util.ArrayList<>(3);
		for(int i = 0; i < 3; i++) {JewelSlotUsage.add(false);}
		
		for(List<EquipmentDataTag> pjl : plusjewtags) {
			for(EquipmentDataTag dt : pjl) {
				JewelSlotUsage.set(dt.slot - 1, true);
			}
		}
	}
	public <TA extends EquipmentData, TC extends EquipmentData, TJ extends EquipmentData>
	 void MakeEquipmentDataTags(List<List<TA>> armors, List<TC> cuffs, List<TJ> jewels, Map<Integer, List<Integer>> jewelindices, SearchCondition cond) {
		EquipmentDataTag.setSkillOrder(SkillFillingOrder);
		SortEquipmentItems();
		
		ArmorDataTags = new java.util.ArrayList<>(armors.size());
		for(List<TA> adl : armors) {
			ArmorDataTags.add(MakeDataTagList(adl));
		}
		
		CuffDataTags = MakeDataTagList(cuffs);
		
		PlusJewelDataTags = new java.util.ArrayList<>(SkillFillingOrder.size());
		for(Integer sid : SkillFillingOrder) {
			PlusJewelDataTags.add(MakeDataTagList(jewels, jewelindices.get(sid)));
		}
		
		//TODO: PlusJewel[x]が空の場合の処理をどこかで
		PlusJewelMaxEfficiency_Point = new int[SkillFillingOrder.size()];
		PlusJewelMaxEfficiency_Slot = new int[SkillFillingOrder.size()];
		for(int i = 0; i < PlusJewelDataTags.size(); i++) {
			PlusJewelMaxEfficiency_Point[i] = PlusJewelDataTags.get(i).get(0).skillPoints[i];
			PlusJewelMaxEfficiency_Slot[i] = PlusJewelDataTags.get(i).get(0).slot;
		}
		
		CheckJewelSlotUsage(PlusJewelDataTags);
	}
	private <T extends EquipmentData> List<EquipmentDataTag> MakeDataTagList(List<T> equiplist) {
		List<EquipmentDataTag> ret = new java.util.ArrayList<EquipmentDataTag>(equiplist.size());
		for(int i = 0; i < equiplist.size(); i++) {
			ret.add(new EquipmentDataTag(equiplist.get(i), flagSkillPointInverted));
		}
		return ret;
	}
	private <T extends EquipmentData> List<EquipmentDataTag> MakeDataTagList(List<T> equiplist, List<Integer> indices) {
		List<EquipmentDataTag> ret = new java.util.ArrayList<EquipmentDataTag>(indices.size());
		for(Integer i : indices) {
			ret.add(new EquipmentDataTag(equiplist.get(i), flagSkillPointInverted));
		}
		return ret;
	}
	
	public int[] initialRestPoint() { //TODO: 指定装備を考える(防具分は必要ない)
		int[] ret = new int[SkillFillingOrder.size()];
		for(int i = 0; i < SkillFillingOrder.size(); i++) {
			Integer sid = SkillFillingOrder.get(i);
			SearchCondition.SkillPointCondition spc = SearchCondition.getSPCond(sid);
			if (spc.pointLower != null && spc.pointUpper != null) {
				throw new UnsupportedOperationException("(未実装)スキル指定と除外指定の両方に設定することはできません");
			}
			if (flagSkillPointInverted.getOrDefault(sid, false)) {
				ret[i] = -spc.pointUpper;
			} else {
				ret[i] = spc.pointLower;
			}
		}
		return ret;
	}
	
	public List<JewelCombinationData> MakeJewelCombinations(List<JewelData> jewellist, SearchCondition condition, int maxslots) throws IOException {
		List<JewelCombinationData> ret = new java.util.ArrayList<>();
		
		final int jlen = jewellist.size();
		if (jlen == 0) return ret;
		
		//NOTE: [0, jlen]の重複組み合わせを生成する
		StringBuilder nmsb = new StringBuilder();
		int[] indices = new int[maxslots];
		while(true) {
			{
				List<EquipmentData> jews = new java.util.ArrayList<>(maxslots);
				nmsb.setLength(0);
				for(int i = 0; i < maxslots; i++) {
					if (indices[i] < jlen) {
						JewelData jd = jewellist.get(indices[i]);
						jews.add(jd);
						if(nmsb.length() > 0) {nmsb.append(", ");}
						nmsb.append(jd.getName());
					} else {
						//==jlenは空欄扱いとすることで0～maxslots個の組み合わせを表現する
					}
				}
				JewelCombinationData jc = new JewelCombinationData(nmsb.toString(), jews);
				if(jc.getSlot() > 0 && jc.getSlot() <= maxslots) { //元の装飾品が2スロ以上のものもあるので要求スロット数を超える場合があることに注意
					ret.add(jc);
				}
			}

			int lp;
			for(lp = maxslots - 1; lp >= 0; lp--) {
				if(indices[lp] != jlen) {break;}
			}
			if(lp < 0) break;
			
			int nx = indices[lp] + 1;
			for(int i = lp; i < maxslots; i++) {
				indices[i] = nx;
			}
		}
		
		//NOTE: ExtractSuperiorItemsは比較回数が個数の2乗オーダーなのでPlusJewelsに振り分けてからやった方が時間は短くなる公算が高い
		if(false) {
			List<Integer> superiors = EquipmentData.ExtractSuperiorItems(ret, null, condition);
			ret = BaseData.selectByIndices(ret, superiors);
		}

		return ret;
	}
}
