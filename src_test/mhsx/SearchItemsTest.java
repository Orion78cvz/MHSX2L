package mhsx;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import mhsx.EquipmentCombinationData.JewelCombinationData;

public class SearchItemsTest {
	static private mhsx.BaseData db = new mhsx.BaseData();

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

	private static void printNumEquipments(SearchItems items) {
		if (items == null) {
			System.out.print("# of Armors:");
			for(int i = 0; i < db.getNumArmorKind(); i++) {
				System.out.print(String.format(" %d", db.getArmorList(i).size()));
			} System.out.println("");
			System.out.println(String.format("# of Jewels: %d", db.getJewelList().size()));
			System.out.println(String.format("# of SkillCuffs: %d", db.getCuffList().size()));
		} else {
			System.out.print("# of Armors:");
			for(int i = 0; i < items.ArmorDataTags.size(); i++) {
				System.out.print(String.format(" %d", items.ArmorDataTags.get(i).size()));
			} System.out.println("");
			System.out.println(String.format("# of SkillCuffs: %d", items.CuffDataTags.size()));
			System.out.print("# of Jewels: ");
			for(int i = 0; i < items.PlusJewelDataTags.size(); i++) {
				System.out.print(String.format(" %d", items.PlusJewelDataTags.get(i).size()));
			} System.out.println("");
		}
	}
	@Test
	public void testExtractCandidateEquipment() {
		try {
			db.LoadXML_ArmorData("dat/", null);
			db.LoadXML_CuffData("dat/", null);
			db.LoadXML_JewelData("dat/", null);
			printNumEquipments(null);

			for(int ci = 0; ci < testcases.size(); ci++) {
				System.out.println("--------");
				SearchCondition cond = testcases.get(ci);
				cond.setFlagCheckNumInvokedSkills(true);
				System.out.println(cond.toString());
				
				//Thread.sleep(5000);
	
				SearchItems items = new SearchItems();
				items.PrepareEquipmentDataTags(db, cond, false);
				
				//Thread.sleep(10000);
				System.out.print("Filling order: ");
				for (Integer si : items.SkillFillingOrder) {
					System.out.print(db.getSkill(si).Name);
					System.out.print(", ");
				}
				System.out.println("");
				
				for(EquipmentDataTag cd : items.CuffDataTags) {
					if (((EquipmentCombinationData)cd.base).getEquipmentList().size() == 2) {
						System.out.println(cd.base.toString());
					}
				}
				
				for (int si = 0; si < items.PlusJewelDataTags.size(); si++) {
					List<EquipmentDataTag> pjl = items.PlusJewelDataTags.get(si);
					System.out.println(String.format("-- %s (%d jewels)--", db.getSkill(items.SkillFillingOrder.get(si)).Name, pjl.size()));
					//System.out.println(items.monotone.get(si).toString());
					for (EquipmentDataTag jt : pjl) {
						System.out.println(String.format("\t%s", jt.base.toString()));
					}
				}
				
				//Thread.sleep(10000);
				printNumEquipments(items);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
