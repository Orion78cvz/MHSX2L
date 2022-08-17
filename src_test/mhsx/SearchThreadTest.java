package mhsx;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SearchThreadTest {
	private static mhsx.BaseData db = new mhsx.BaseData();
	
	private static List<SearchCondition> testcases;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		db.LoadXML_SkillBase("dat/");
		
		testcases = new java.util.ArrayList<>();
		
		{
			SearchCondition cond = new SearchCondition();
			cond.setLabel("テスト00");
			cond.setJob(ArmorData.HunterType.Blademaster);
			cond.setRequiredSkillRankup(true);
			cond.specifyArmorName(1, "ディオレGFキャップ");
			cond.specifyArmorName(2, "ヴァイスGXムスケル");
			cond.specifyArmorName(3, "雅深GX【篭手】");
			cond.specifyArmorName(4, "ヴァイスGXナーベル");
			cond.specifyArmorName(5, "オディバGFグリーヴ");
			cond.addInvokedSkill(Arrays.asList("一閃+3", "弱点特効", "真打+3", "豪放+3", "ブチギレ", "絶対防御態勢", "剣術+1", "業物+1", "見切り+4", "回避性能+2", "早食い"));
			cond.addTargetArmorClass("(パGP),(キGP),(ガGP),(ネGP),(特GP), (GX), (烈), (イ), (祭GX),(塔GX),(猟GX)");
			cond.addTargetCuffClass("(P), (P_Ⅱ), (P_Ｇ), (S), (S_イ), (S_韋), (S_猟), (S_祭), (S_Ｇ)");
			cond.addTargetJewelClass("無, (Ｇ), (GX), (Ｇ秘)");
			testcases.add(cond);
		}
		{
			SearchCondition cond = new SearchCondition();
			cond.setLabel("テスト01");
			cond.setJob(ArmorData.HunterType.Blademaster);
			cond.setRequiredNumGrankEquip(3);
			cond.setRequiredSkillRankup(true);
			cond.specifyArmorName(5, "ルヴナンGPフェルゼ");
			cond.addInvokedSkill(Arrays.asList("双剣技【双龍】", "一閃+3", "代償", "豪放+3", "紅焔の威光+2", "絶対防御態勢", "闘覇", "状態異常半減", "剣術+1", "業物+1", "ランナー"));
			cond.addTargetArmorClass("(パGP),(キGP),(ガGP),(ネGP),(特GP), (GX), (烈), (イ), (祭GX),(塔GX),(猟GX)");
			cond.addTargetCuffClass("(P), (P_Ⅱ), (P_Ｇ), (S), (S_イ), (S_韋), (S_猟), (S_祭), (S_Ｇ)");
			cond.addTargetJewelClass("無, (Ｇ), (GX), (Ｇ秘)");
			testcases.add(cond);
		}
		{
			SearchCondition cond = new SearchCondition();
			cond.setLabel("テスト02");
			cond.setJob(ArmorData.HunterType.Blademaster);
			cond.setRequiredSkillRankup(true);
			cond.specifyArmorName(5, "ヴァルGFグリーヴ");
			cond.addInvokedSkill(Arrays.asList("双剣技【双龍】", "一閃+3", "弱点特効", "代償", "一匹狼", "紅焔の威光+2", "闘覇", "属性攻撃強化", "剣術+1", "業物+1", "餓狼+2"));
			cond.addTargetArmorClass("(パGP),(キGP),(ガGP),(ネGP),(特GP), (GX), (烈), (イ), (祭GX),(塔GX),(猟GX)");
			cond.addTargetCuffClass("(P), (P_Ⅱ), (P_Ｇ), (S), (S_イ), (S_韋), (S_猟), (S_祭), (S_Ｇ)");
			cond.addTargetJewelClass("無, (Ｇ), (GX), (Ｇ秘)");
			testcases.add(cond);
		}
		{
			SearchCondition cond = new SearchCondition();
			cond.setLabel("テスト03");
			cond.setJob(ArmorData.HunterType.Blademaster);
			cond.setRequiredSkillRankup(true);
			cond.specifyArmorName(1, "ハーゼGPバンド");
			cond.addInvokedSkill(Arrays.asList("双剣技【双龍】", "一閃+3", "弱点特効", "真打+3", "豪放+3", "ブチギレ", "絶対防御態勢", "剣術+1", "業物+1", "見切り+4", "回避性能+1"));
			cond.addTargetArmorClass("(パGP),(キGP),(ガGP),(ネGP),(特GP), (GX), (烈), (イ), (祭GX),(塔GX),(猟GX)");
			cond.addTargetCuffClass("(P), (P_Ⅱ), (P_Ｇ), (S), (S_イ), (S_韋), (S_猟), (S_祭), (S_Ｇ)");
			cond.addTargetJewelClass("無, (Ｇ), (GX), (Ｇ秘)");
			testcases.add(cond);
		}
		{
			SearchCondition cond = new SearchCondition();
			cond.setLabel("テスト04(01+除外)");
			cond.setJob(ArmorData.HunterType.Blademaster);
			cond.setRequiredNumGrankEquip(3);
			cond.setRequiredSkillRankup(true);
			cond.specifyArmorName(5, "ルヴナンGPフェルゼ");
			cond.addInvokedSkill(Arrays.asList("双剣技【双龍】", "一閃+3", "代償", "豪放+3", "紅焔の威光+2", "絶対防御態勢", "闘覇", "状態異常半減", "剣術+1", "業物+1", "ランナー"));
			cond.addEliminatedSkill("見切り+1");
			cond.addTargetArmorClass("(パGP),(キGP),(ガGP),(ネGP),(特GP), (GX), (烈), (イ), (祭GX),(塔GX),(猟GX)");
			cond.addTargetCuffClass("(P), (P_Ⅱ), (P_Ｇ), (S), (S_イ), (S_韋), (S_猟), (S_祭), (S_Ｇ)");
			cond.addTargetJewelClass("無, (Ｇ), (GX), (Ｇ秘)");
			testcases.add(cond);
		}
		{
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
			testcases.add(cond);
		}
		{
			SearchCondition cond = new SearchCondition();
			cond.setLabel("テスト06");
			cond.setJob(ArmorData.HunterType.Blademaster);
			cond.setRequiredSkillRankup(true);
			cond.setRequiredNumGrankEquip(3);
			cond.specifyArmorName(1, "ハーゼGPバンド");
			//cond.specifyArmorName(4, "ボニトGXフォールド");
			//cond.specifyArmorName(6, "Ｇ三界カフＰＡ１");
			cond.addInvokedSkill(Arrays.asList("双剣技【双龍】", "一閃+3", "弱点特効", "真打+3", "豪放+3", "逆鱗", "適応撃+2", "剣術+1", "業物+1", "見切り+4", "早食い"));
			cond.addTargetArmorClass("(GX), (烈),(塔GX)");
			cond.addTargetCuffClass("(S), (S_イ), (S_韋), (S_猟), (S_祭), (S_Ｇ)");
			cond.addTargetJewelClass("無, (Ｇ), (GX), (Ｇ秘)");
			testcases.add(cond);
		}
		
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		long startt = System.currentTimeMillis();
		try {
			SearchCondition cond = testcases.get(6);
			cond.setFlagCheckNumInvokedSkills(true);
			System.out.println(cond.writeJsonAsString());

			if (!db.getArmorList().isEmpty() && !db.getArmorList(0).isEmpty()) {
				throw new AssertionError("LoadXML may be doubled");
			}
			db.LoadXML_ArmorData("dat/", cond);
			db.LoadXML_CuffData("dat/", cond);
			db.LoadXML_JewelData("dat/", cond);

			//----
			SearchThread sc = new SearchThread();
			PrintOutput stdout = new PrintOutput(System.out);
			sc.setMsgOutput(stdout); sc.setLogOutput(stdout);
			sc.setResultOutput(stdout);
			
			sc.Preprocess(cond);
			sc.SearchProcess();
			sc.Postprocess();

			if (sc.countArmorSet.get() != sc.expectedCountArmorSet) {
				fail("the number of search node have not matched");
			}

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		long endt = System.currentTimeMillis();
		System.out.println(String.format("elapsed time: %d sec", (endt-startt)/1000));
	}

}
