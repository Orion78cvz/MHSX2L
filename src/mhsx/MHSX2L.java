package mhsx;

import java.util.Arrays;

public class MHSX2L {
	public static void main(String[] args) {
		//TODO: コマンドラインオプションのパース
		// 入力や出力とかの指定→factory method
		
		MHSX2L app = new MHSX2L();
		app.run();
	}

	//----
	private mhsx.BaseData db;
	
	public MHSX2L() {
		db = new mhsx.BaseData();
	}
	
	public void run() {
		db.LoadXML("dat/"); //TODO: 指定方法
		
		//MEMO: 検索条件を取得してスレッド作成
		//MEMO: 複数スレッドに対応
		
		//TEST
		SearchCondition cond = new SearchCondition();
		cond.setLabel("テスト05");
		cond.setJob(ArmorData.HunterType.Blademaster);
		cond.setRequiredSkillRankup(true);
		cond.specifyArmorName(1, "ハーゼGPバンド");
		cond.specifyArmorName(4, "ボニトGXフォールド");
		cond.specifyArmorName(6, "Ｇ三界カフＰＡ１");
		cond.addInvokedSkill(Arrays.asList("双剣技【双龍】", "一閃+3", "弱点特効", "真打+3", "豪放+3", "逆鱗", "適応撃+2", "剣術+1", "業物+1", "見切り+4", "早食い"));
		cond.addTargetArmorClass("(パGP),(キGP),(ガGP),(ネGP),(特GP), (GX), (烈), (イ), (祭GX),(塔GX),(猟GX)");
		cond.addTargetCuffClass("(P), (P_Ⅱ), (P_Ｇ), (S), (S_イ), (S_韋), (S_猟), (S_祭), (S_Ｇ)");
		cond.addTargetJewelClass("無, (Ｇ), (GX), (Ｇ秘)");
		cond.setFlagCheckNumInvokedSkills(true);

		SearchThread sc = new SearchThread();
		PrintOutput stdout = new PrintOutput(System.out);
		sc.setMsgOutput(SearchOutput.NullOutput.instance());
		sc.setLogOutput(stdout);
		sc.setResultOutput(stdout);
		
		sc.Preprocess(cond);
		sc.SearchProcess();
		sc.Postprocess();
	}
}
