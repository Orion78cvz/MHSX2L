package mhsx;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import mhsx.SearchOutput.ResultCode;

public class SearchThread implements Runnable {
	Thread thread = null;
	private boolean sigterm = false;
	public void start() {
		sigterm = false;
		thread = new Thread(this);
		thread.start();
	}
	public void stop() {
		sigterm = true;
		//thread = null;
	}
	
	public void run() {
		SearchProcess();
	}
	
	private void terminate() {
		thread = null;
		sigterm = false;
	}
	
	//----
	private mhsx.BaseData db = new mhsx.BaseData();
	
	private SearchOutput msgOutput, logOutput, resultOutput;
	public void setMsgOutput(SearchOutput out) {msgOutput = (out != null) ? out : SearchOutput.NullOutput.instance();}
	public void setLogOutput(SearchOutput out) {logOutput = (out != null) ? out : SearchOutput.NullOutput.instance();}
	public void setResultOutput(SearchOutput out) {resultOutput = (out != null) ? out : SearchOutput.NullOutput.instance();}
	
	public SearchCondition searchCondition;
	public SearchItems searchItems;
	
	public AtomicLong countArmorSet, countSkipped;
	public long expectedCountArmorSet; 
	public AtomicLong countEligibleSet, countFloodedSet;
	
	public int[] DebugCount = new int[2];
	
	//NOTE: preとpostはこのインスタンスを用意した側で呼び出す
	public void Preprocess(SearchCondition cond) {
		searchCondition = cond;
		searchItems = new SearchItems();
		try {
			searchItems.PrepareEquipmentDataTags(db, cond, false);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
		
		{
			StringBuilder sb = new StringBuilder();
			sb.setLength(0);
			sb.append("# of Armors:");
			for(int i = 0; i < searchItems.ArmorDataTags.size(); i++) {
				sb.append(" ");
				sb.append(searchItems.ArmorDataTags.get(i).size());
			}
			logOutput.log(sb.toString(), 0);
			logOutput.log(String.format("# of SkillCuffs: %d", searchItems.CuffDataTags.size()), 0);
			sb.setLength(0);
			sb.append("# of Jewels:");
			for(int i = 0; i < searchItems.PlusJewelDataTags.size(); i++) {
				sb.append(" ");
				sb.append(searchItems.PlusJewelDataTags.get(i).size());
			}
			logOutput.log(sb.toString(), 0);
		}
		logOutput.log("--------", 0);
		
		countArmorSet = new AtomicLong(0);
		countSkipped = new AtomicLong(0);
		expectedCountArmorSet = searchItems.ArmorDataTags.stream().reduce(1L, (c, tl) -> c * (long)tl.size(), (a,b)->a*b);
		//TODO: 防具組み合わせ数
		countEligibleSet = new AtomicLong(0);
		countFloodedSet = new AtomicLong(0);
	}
	public void Postprocess() {
		logOutput.log(String.format("total # of armor set: %,3d (%,3d skipped)", countArmorSet.get(), countSkipped.get()), 0);
		logOutput.log(String.format("# of eligible set: %,3d + flooded: %,3d", countEligibleSet.get(), countFloodedSet.get()), 0);

		if(sigterm) {
			resultOutput.resultEnd(ResultCode.Aborted);
		} else {
			if (countArmorSet.get() == expectedCountArmorSet) {
				resultOutput.resultEnd(ResultCode.Success);
			} else {
				resultOutput.resultEnd(ResultCode.Failed);
			}
		}
		terminate();
	}
	
	public void SearchProcess() {
		final int numarmorkind = searchItems.ArmorDataTags.size();
		
		//
		int[] currentRestPoint = initialRestPoint();
		int[] currentRestSlot = new int[4]; //0スロ～
		int[] tmpRestPoint = new int[currentRestPoint.length]; //作業用
		int[] tmpRestSlot = new int[12];
		int[] tmpEndPointers = new int[searchItems.PlusJewelDataTags.size()];

		int numgrank = 0, numgoushu = 0;
		
		resultOutput.resultBegin();

		//TODO: 防具の組み合わせをiterator風にする
		
		List<EquipmentDataTag> armorset = new java.util.ArrayList<>(numarmorkind);
		//初期値  下にうまく統合できないか？
		List<Iterator<EquipmentDataTag>> iterlist_armors = new java.util.ArrayList<>(numarmorkind); 
		for(int i = 0; i < numarmorkind; i++) {
			Iterator<EquipmentDataTag> init = searchItems.ArmorDataTags.get(i).iterator();
			iterlist_armors.add(init);
			EquipmentDataTag nitem = init.next();
			
			armorset.add(nitem);
			subtractEquipmentPoint(currentRestPoint, nitem);
			currentRestSlot[nitem.slot]++;
			numgrank += ((ArmorData)(nitem.base)).GRankCount;
			numgoushu += ((ArmorData)(nitem.base)).GoushuCount;
		}
		
		// 防具は候補の全組み合わせを探索
		while(true) {
			// 防具条件を満たす場合はカフと装飾品の探索
			boolean rankup = (numgoushu >= 3);
			if(numgrank >= searchCondition.getRequiredNumGrankEquip() &&
				(!searchCondition.isRequiredSkillRankup() || rankup)) {
				int maxinvs = SearchItems.numInvokedSkillsWithGRankArmors[numgrank];
				//   防具組み合わせに対して,条件を満たすようなカフと珠のパターンが一つ出たら終わり
				ResultEquipmentSet curresult = null;
				for(EquipmentDataTag cuff : searchItems.CuffDataTags) {
					subtractEquipmentPoint(currentRestPoint, cuff);
					ResultEquipmentSet fill;
					//if (searchItems.UseJewelCombination) {
					//	fill = FindEligibleJewelFilling_SlotFixed(currentRestPoint, currentRestSlot, tmpRestPoint, tmpRestSlot, tmpEndPointers, armorset, cuff, maxinvs);
					//} else {
						fill = FindEligibleJewelFilling(currentRestPoint, currentRestSlot, tmpRestPoint, tmpRestSlot, tmpEndPointers, armorset, cuff, maxinvs);
					//}
					retractEquipmentPoint(currentRestPoint, cuff); //* 
					
					if (fill != null) {
						if(fill.isFlooded()) {
							if (curresult == null || fill.getNumRequiredSkillsInvoked() > curresult.getNumRequiredSkillsInvoked()) {
								curresult = fill;
							}
						} else {
							curresult = fill;
							break;
						}
					}
				}
				if (curresult != null) {
					curresult.attrNumMaxSkills = maxinvs;
					curresult.attrSkillRankup = rankup;
					resultOutput.resultEntry(curresult);
					if(curresult.isFlooded()) {
						countFloodedSet.getAndIncrement();
					} else {
						countEligibleSet.getAndIncrement();
					}
				}
			} else {
				countSkipped.getAndIncrement();
			}
			countArmorSet.getAndIncrement();
			
			if (sigterm) {
				return;
			}
			
			// 次の防具を選ぶ
			int ki;
			for(ki = numarmorkind - 1; ki >= 0; ki--) {
				Iterator<EquipmentDataTag> cit = iterlist_armors.get(ki);
				
				boolean bktrack = false;
				if (!cit.hasNext()){
					bktrack = true;
					//先頭の候補に戻る
					cit = searchItems.ArmorDataTags.get(ki).iterator();
					iterlist_armors.set(ki, cit);
				}
				
				EquipmentDataTag nitem = cit.next();
				//色々と次の装備に入れ替える
				EquipmentDataTag oldeq = armorset.get(ki); 
				if(nitem != oldeq) {
					armorset.set(ki, nitem);
					retractEquipmentPoint(currentRestPoint, oldeq);
					subtractEquipmentPoint(currentRestPoint, nitem);
					currentRestSlot[oldeq.slot]--;
					currentRestSlot[nitem.slot]++;
					numgrank -= ((ArmorData)(oldeq.base)).GRankCount;
					numgoushu -= ((ArmorData)(oldeq.base)).GoushuCount;
					numgrank += ((ArmorData)(nitem.base)).GRankCount;
					numgoushu += ((ArmorData)(nitem.base)).GoushuCount;
				}
				
				if (!bktrack) {
					break;
				}
			}
			if (ki < 0) {
				break;
			}
		}

	}
	// 考えられるスロットの振り分けパターンで指定のポイントを埋められるかどうか
	// array...は作業領域(外でnewされて使いまわされる)としてサイズが決まっているので注意 （MEMO: 後でなんとかする？)
	public ResultEquipmentSet FindEligibleJewelFilling(int[] RestPoint, int[] RestSlot, int[] arrayRestPoint, int[] arrayRestSlot, int[] arrayEndPointers, List<EquipmentDataTag> armorSet, EquipmentDataTag cuffChoice, int maxskills) {
		ResultEquipmentSet currentResult = null;
		int currentNumSkills = 0;
		//System.out.println(String.format("[%d, %d, %d]:", RestSlot[1], RestSlot[2], RestSlot[3]));
		for(int i = 1; i < RestSlot.length; i++) {
			arrayRestSlot[i+8] = RestSlot[i];
		}
		
		//スロットのパターンを組み替えて充填を調べる
		if(!searchItems.JewelSlotUsage.get(2)){ //3スロ珠なければ2,1スロへ分解
			int nt = arrayRestSlot[3+8];
			arrayRestSlot[3+8] = 0;
			arrayRestSlot[2+8] += nt;
			arrayRestSlot[1+8] += nt;
		}
		while(arrayRestSlot[3+8] >= 0) {
			if(!searchItems.JewelSlotUsage.get(1)){ //2スロ珠なければ1スロ*2へ分解
				int nt = arrayRestSlot[2+8];
				arrayRestSlot[2+8] = 0;
				arrayRestSlot[1+8] += nt * 2;
			}
			for(int i = 1; i < RestSlot.length; i++) {
				arrayRestSlot[i+4] = arrayRestSlot[i+8];
			}
			while(arrayRestSlot[2+4] >= 0) {
				for(int i = 1; i < RestSlot.length; i++) {
					arrayRestSlot[i] = arrayRestSlot[i+4];
				}
				for(int i = 0; i < RestPoint.length; i++) {
					arrayRestPoint[i] = RestPoint[i];
				}
				//System.out.println(String.format("__[%d, %d, %d]", RestSlot[1], RestSlot[2], RestSlot[3]));
				//MEMO: arrayRestSlot[0-3]をこの中で使う
				ResultEquipmentSet fill = SearchJewelFilling(null, arrayRestPoint, arrayRestSlot, arrayEndPointers, armorSet, cuffChoice, maxskills);
				if (fill != null) {
					//printintarray(RestPoint);
					if(fill.isFlooded()) {
						int sc = fill.getNumRequiredSkillsInvoked();
						if (sc > currentNumSkills) {
							currentResult = fill;
							currentNumSkills = sc;
						}
					} else {
						return fill;
					}
				}
				
				// スロットを分解して次へ
				arrayRestSlot[2+4]--;
				arrayRestSlot[1+4] += 2;
			}
			
			// スロットを分解して次へ
			arrayRestSlot[3+8]--;
			arrayRestSlot[2+8]++;
			arrayRestSlot[1+8]++;
		}
		return currentResult;
	}
	public ResultEquipmentSet FindEligibleJewelFilling_SlotFixed(int[] RestPoint, int[] RestSlot, int[] arrayRestPoint, int[] arrayRestSlot, int[] arrayEndPointers, List<EquipmentDataTag> armorSet, EquipmentDataTag cuffChoice, int maxskills) {
		for(int i = 1; i < RestSlot.length; i++) {
			arrayRestSlot[i] = RestSlot[i];
		}
		for(int i = 0; i < RestPoint.length; i++) {
			arrayRestPoint[i] = RestPoint[i];
		}
		return SearchJewelFilling(null, arrayRestPoint, arrayRestSlot, arrayEndPointers, armorSet, cuffChoice, maxskills);
	}
	// 探索本番
	enum CombineState {init, append, remove;}
	public ResultEquipmentSet SearchJewelFilling(int[][] reservedUseCounts, int[] RestPoint, int[] RestSlot, int[] tmpEndPointers, List<EquipmentDataTag> armorSet, EquipmentDataTag cuffChoice, int maxskills) { //MEMO: Rest___の作業領域は元の状態に戻す必要はない
		int InitialEmptySlots = RestSlot[1] + RestSlot[2] * 2 + RestSlot[3] * 3;
		//int blankSlots = 0;
		if (PrunedByRestSlot(RestPoint, InitialEmptySlots, 0, RestPoint.length - 1)) {
			//残りスロットを全部使っても充足できないスキルがあるならば打ち切り
			return null;
		}
		
		// 優先度の関係でスキルが不発になっていても結果として返すために、
		// 要求スキルの発動数が少なくても一時保存しておく (条件を満たすものがみつかった場合は使わない)
		int currentNumSkills = 0;
		ResultEquipmentSet currentResult = null;
		
		//重複組み合わせをこの配列に生成
		int[][] usecounts = new int[searchItems.PlusJewelDataTags.size()][];
		for(int i = 0; i < searchItems.PlusJewelDataTags.size(); i++) {
			usecounts[i] = new int[searchItems.PlusJewelDataTags.get(i).size()];
		}
		
		CombineState state = CombineState.init;
		int index = 0;
		do {
			while(index < RestPoint.length && index >= 0) {
				List<EquipmentDataTag> pjlt = searchItems.PlusJewelDataTags.get(index); //NOTE: indexが変化したときだけでいい
				
				if(state == CombineState.init)
				{
					tmpEndPointers[index] = -1;
					state = CombineState.append;
					//continue;
				}
	
				if (state == CombineState.remove) {
					int usecount = 0;
					int m = tmpEndPointers[index];
					for(; m >= 0; m--) {
						if ((usecount = usecounts[index][m]) > 0) break;
					}
					if (m < 0) {
						//state = CombineState.remove;
						index--;
						continue;
					}
					
					int rest = RestPoint[index];
					int emps = RestSlot[1] + RestSlot[2] * 2 + RestSlot[3] * 3;// - blankSlots;
					
					EquipmentDataTag pjdt = pjlt.get(m);
					int cpt = pjdt.skillPoints[index];
					int csl = pjdt.slot;
					int nxp, nxs; //次候補のポイント効率
					if (m+1 < pjlt.size()) { //MEMO: 末尾はポイント0のtagを入れておいてもいいかもしれない
						nxp = pjlt.get(m+1).skillPoints[index];
						nxs = pjlt.get(m+1).slot;
					} else {
						nxp = 0;
						nxs = 1;
					}
					if (rest+cpt > 0 && (cpt*nxs > nxp*csl) && (nxp * (emps + csl) < nxs * (rest + cpt))) {
						// pjlt[m]をこれ以上減らしたパターンでは充足できない(末尾の候補である場合を含め)ときは、全て取り除く
						RestSlot[csl] += usecount;
						retractEquipmentPoint(RestPoint, pjdt, usecount);
						usecounts[index][m] = 0;
						tmpEndPointers[index] = m - 1;
						//state = CombineState.remove;
						continue;
					} else {
						// pjlt[m]を一つ減らす
						RestSlot[csl]++;
						retractEquipmentPoint(RestPoint, pjdt);
						usecounts[index][m]--;
						tmpEndPointers[index] = m;
						state = CombineState.append;
						//continue;
					}
				}
				
				//if (state == CombineState.append)
				int rest = RestPoint[index];
				if (rest <= 0)
				{
					state = CombineState.init;
					index++;
					continue;
				}
				int emps = RestSlot[1] + RestSlot[2] * 2 + RestSlot[3] * 3;// - blankSlots;
				int pjlen = pjlt.size();
				
				boolean inserted = false;
				for (int ptr = tmpEndPointers[index] + 1; ptr < pjlen; ptr++) { //NOTE: 一種詰めたらループを抜ける
					EquipmentDataTag pjdt = pjlt.get(ptr);
					int skpt = pjdt.skillPoints[index];
	
					//if ((skpt ^ rest) < 0) {continue;} // 有用なポイントでない(つまり負)場合
					if (skpt <= 0) {continue;}
					
					//NOTE: 装飾品がefficiency降順に並んでいることを前提として、
					// ptrをある個数詰めたとき、さらに後続の装飾品を使ってもrest=RestPoint[index]を充足できないならば、
					// ptrがより少ないパターンでもrestを埋めきれない
					int ts = RestSlot[pjdt.slot];
					if (ts == 0) {continue;}
					int rnum = (rest + skpt - 1) / skpt; //この珠だけでRestPoint[index]を埋めるとして必要な個数
					int pn = UpperLimitAddCount(pjdt, RestPoint, emps, Math.min(rnum, ts), index + 1); //MEMO: 後続のスキルについてチェックするのでindex+1から
					//System.out.println(String.format("[%d] restslot=%d, required=%d, possible=%d", index, ts, rnum, pn));
					if (pn == 0) {continue;}
					
					int resid = rest - pn * skpt;
					int csl = pjdt.slot; 
					if (pn < rnum) {
						int nxp, nxs; //次候補のポイント効率
						if (ptr + 1 < pjlen) {
							nxp = pjlt.get(ptr + 1).skillPoints[index];
							nxs = pjlt.get(ptr + 1).slot;
						} else {
							nxp = 0;
							nxs = 1;
						}
						
						if (nxp * (emps - pn * csl) < resid * nxs) {
							//この状態からptrを0～pn個つめたパターンでは充足不可能なので一つ前を減らす
							break;
						}
					}
					
					usecounts[index][ptr] += pn;
					RestSlot[pjdt.slot] -= pn;
					subtractEquipmentPoint(RestPoint, pjdt, pn);
					rest = resid;
					
					tmpEndPointers[index] = ptr;
					inserted = true;
					break;
				}
				if (!inserted) {
					state = CombineState.remove;
					continue;
				}
				
				if (rest <= 0) {
					//このスキルは充足できたので次スキルへ
					state = CombineState.init;
					index++;
					continue;
				}
	
				//次以降の珠候補でこのスキルを埋めていく
				//state = CombineState.append;
				//continue;
			}
			if (index < 0) { //全組み合わせを探索し終わった
				break;
			}
			
			ResultEquipmentSet filling = null;			
			// 全てのポイントが充足されているかどうか再確認 (負のポイントによってポイント条件を満たさなくなる場合があるため)
			boolean enough = true, retriable = false;
			CHKRESTP: 
			for(int j = 0; j < RestPoint.length; j++) {
				if(RestPoint[j] > 0) {
					enough = false;
					//残ったスロットに珠を詰めることで充足する可能性があれば再帰的に探索して試す
					for(EquipmentDataTag pjdt : searchItems.PlusJewelDataTags.get(j)) {
						if (RestSlot[pjdt.slot] > 0) {
							retriable = true;
							break CHKRESTP;
						}
					}
				}
			}
			if (!enough) {
				if (retriable) {
					//再帰探索のために作業領域などを作る
					int[] tmprpt = new int[RestPoint.length];
					int[] tmprslt = new int[RestSlot.length];
					for(int i = 0; i < RestPoint.length; i++) {
						tmprpt[i] = RestPoint[i];
					}
					for(int i = 1; i < RestSlot.length; i++) {
						tmprslt[i] = RestSlot[i];
					}
					int[] tmpendp = new int[searchItems.PlusJewelDataTags.size()];
					int[][] tmpcuruc;
					if (reservedUseCounts != null) {
						tmpcuruc = new int[usecounts.length][];
						for(int i = 0; i < usecounts.length; i++) {
							tmpcuruc[i] = new int[usecounts[i].length];
							for(int j = 0; j < usecounts[i].length; j++) {
								tmpcuruc[i][j] = reservedUseCounts[i][j] + usecounts[i][j];
							}
						}
					} else {
						tmpcuruc = usecounts;
					}
					filling = SearchJewelFilling(tmpcuruc, tmprpt, tmprslt, tmpendp, armorSet, cuffChoice, maxskills); //CalcTotalSkillPointsのためにここまで決定した分のusecountsを渡す
				}
			} else {
				filling = new ResultEquipmentSet("rset");
				filling.setupResultEquipments(armorSet, cuffChoice, searchItems.PlusJewelDataTags, usecounts, reservedUseCounts);
				if (searchCondition.getFlagCheckNumInvokedSkills()) {
					filling.CountRequiredSkillsInvoked(searchCondition.RequiredInvokedSkills(), maxskills);
				} //NOTE: チェックしない場合はデフォルトがisFlooded() == falseになっている
			}
			// 発動スキルの確認
			if (filling != null) {
				if (filling.isFlooded()) {
					//追い出されているスキルがある場合はひとまず保留しておき、よりよい結果を探索する
					int sc = filling.getNumRequiredSkillsInvoked();
					if (sc > currentNumSkills) {
						//MEMO: 下記にも書いたがスロット数の扱いはとりあえずしない
						currentNumSkills = sc;
						currentResult = filling;
					}
				} else {
					//発動数をチェックしない場合 あるいは 必要スキルを全て発動する結果であれば即座に返す
					// (オリジナルのMHSX2は消費スロット数がより少ないものをさらに探索している)
					return filling;
				}
			}
			
			state = CombineState.remove;
			index--;
		} while(true);
		return currentResult;
	}
	// 各スキルの最大pt効率を全てもった装飾品が仮にあったとしても充足不能な場合の探索を省く
	//NOTE: [starti, endi]の範囲を調べる
	private boolean PrunedByRestSlot(int[] restpoint, int sumempslot, int starti, int endi) {
		for (int i = starti; i <= endi; i++) {
			int rp = restpoint[i];
			if (rp > 0 && searchItems.PlusJewelMaxEfficiency_Point[i] * sumempslot < rp * searchItems.PlusJewelMaxEfficiency_Slot[i]) {
				return true;
			}
		}
		return false;
	}
	//同様に、充填の際に検討する最大個数を計算する
	private int UpperLimitAddCount(EquipmentDataTag subj, int[] RestPoint, int sumEmpSlot, int init, int startindex) {
		int ret = init;
		int slot = subj.slot;
		int lasti = RestPoint.length;
		
		for (int i = startindex; i < lasti; i++) { //NOTE: subjが後続スキルについて正のポイントを保有している場合は上限個数が大きくなるので、ある程度の再計算は必要
			int rp = RestPoint[i];
			if (rp <= 0) continue;
			//rp - N * subjのポイント <= 最大効率 * (sumEmpSlot - N * subj.Slot)
			int jp = subj.skillPoints[i];
			int Pm = searchItems.PlusJewelMaxEfficiency_Point[i];
			int Sm = searchItems.PlusJewelMaxEfficiency_Slot[i];
			int d = Pm * slot - Sm * jp;
			if (d == 0) continue;
			int N = (Pm * sumEmpSlot - Sm * rp) / d;
			if (N == 0) return 0;

			if (N < ret) ret = N;
		}
		return ret;
	}
	
	private int[] initialRestPoint() {
		return searchItems.initialRestPoint();
	}

	private static void subtractEquipmentPoint(int[] pointarray, EquipmentDataTag equip) {
		for(int i : equip.skillIndices) {
			pointarray[i] -= equip.skillPoints[i];
		}
	}
	private static void subtractEquipmentPoint(int[] pointarray, EquipmentDataTag equip, int n) {
		for(int i : equip.skillIndices) {
			pointarray[i] -= equip.skillPoints[i] * n;
		}
	}
	private static void retractEquipmentPoint(int[] pointarray, EquipmentDataTag equip) {
		for(int i : equip.skillIndices) {
			pointarray[i] += equip.skillPoints[i];
		}
	}
	private static void retractEquipmentPoint(int[] pointarray, EquipmentDataTag equip, int n) {
		for(int i : equip.skillIndices) {
			pointarray[i] += equip.skillPoints[i] * n;
		}
	}
	
	//for test
	private static void printintarray(int[] array) {
		System.out.print("[");
		for(int i = 0; i < array.length; i++) {
			System.out.print(" ");
			System.out.print(Integer.toString(array[i]));
		}
		System.out.println(" ]");
	}

}
