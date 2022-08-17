package mhsx;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ResultEquipmentSetTest {
	private static mhsx.BaseData db = new mhsx.BaseData();

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		db.LoadXML_SkillBase("dat/");
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
		try {
			{
				SearchCondition cond = new SearchCondition();
				cond.setLabel("テスト04(01+除外)");
				cond.setJob(ArmorData.HunterType.Blademaster);
				cond.setRequiredNumGrankEquip(3);
				cond.setRequiredSkillRankup(true);
				cond.specifyArmorName(1, "ラモールGPヘッド");
				cond.specifyArmorName(2, "ノワールGXジャケット");
				cond.specifyArmorName(3, "黒影ノ篭手・GX");
				cond.specifyArmorName(4, "オディバGFフォールド");
				cond.specifyArmorName(5, "ルヴナンGPフェルゼ");
				cond.addInvokedSkill(Arrays.asList("双剣技【双龍】", "一閃+3", "代償", "豪放+3", "紅焔の威光+2", "絶対防御態勢", "闘覇", "状態異常半減", "剣術+1", "業物+1", "ランナー"));
				cond.addEliminatedSkill("見切り+1");
				cond.addTargetArmorClass("(パGP),(キGP),(ガGP),(ネGP),(特GP), (Ｇ),(GX), (烈), (イ), (祭GX),(塔GX),(猟GX)");
				cond.addTargetCuffClass("(P), (P_Ⅱ), (P_Ｇ), (S), (S_イ), (S_韋), (S_猟), (S_祭), (S_Ｇ)");
				cond.addTargetJewelClass("無, (Ｇ), (GX), (Ｇ秘)");
				cond.setFlagCheckNumInvokedSkills(true);
				db.LoadXML_ArmorData("dat/", cond);
				db.LoadXML_CuffData("dat/", cond);
				db.LoadXML_JewelData("dat/", cond);
				SearchItems searchItems = new SearchItems();
				searchItems.PrepareEquipmentDataTags(db, cond, false);
			
				ResultEquipmentSet filling = new ResultEquipmentSet("test");
				List<EquipmentDataTag> armorset = searchItems.ArmorDataTags.stream().map(al -> al.get(0)).collect(Collectors.toList());
				EquipmentDataTag cuff = searchItems.CuffDataTags.stream().filter(t -> t.base.getName().equals("Ｇ一閃カフＳＡ８")).iterator().next();
				for(EquipmentDataTag am : armorset) {
					filling.addEquipment(am.base, 1, 1);
				}
				filling.addEquipment(cuff.base, 1, 0);
				filling.addEquipment(db.getJewelData("ゴゴ射珠GX3"), 1, -1);
				filling.addEquipment(db.getJewelData("ルチャル珠GX"), 3, -1);
				filling.addEquipment(db.getJewelData("双龍珠GX・白虎"), 4, -1);
				filling.addEquipment(db.getJewelData("双龍珠GX・玄武"), 1, -1);
				filling.addEquipment(db.getJewelData("エンプレ剣珠GX1"), 2, -1);
				filling.addEquipment(db.getJewelData("ヴァイス射珠GX2"), 1, -1);
				filling.addEquipment(db.getJewelData("ネコ【剣客】珠GX"), 1, -1);
	
				filling.attrNumMaxSkills = 11;
				filling.attrSkillRankup = true;
				filling.CountRequiredSkillsInvoked(cond.RequiredInvokedSkills(), filling.attrNumMaxSkills);
				
				String json = filling.toJsonString();
				System.out.println(filling.toString());
				System.out.println(json);
				
				ResultEquipmentSet test = ResultEquipmentSet.readJsonString(json);
				String testj = test.toJsonString();
				System.out.println(test);
				System.out.println(testj);

				filling.ComputeJewelLocation();
				System.out.println(filling.toEquipmentClip());
				
				assertTrue(json.equals(testj));
			}
			
			{
				SearchCondition cond = new SearchCondition();
				cond.setLabel("テスト");
				cond.setJob(ArmorData.HunterType.Blademaster);
				cond.setRequiredNumGrankEquip(3);
				cond.setRequiredSkillRankup(false);
				cond.specifyArmorName(1, "レイアＧヘルム");
				cond.specifyArmorName(2, "IS学園GPスーツ・※");
				cond.specifyArmorName(3, "アスールGXアーム");
				cond.specifyArmorName(4, "ヘッジGXフォールド");
				cond.specifyArmorName(5, "アスールGXグリーヴ");
				cond.addInvokedSkill(Arrays.asList("斬れ味レベル+1", "剛撃+1", "ガード性能+1", "逆鱗"));
				cond.addTargetArmorClass("(パGP),(キGP),(ガGP),(ネGP),(特GP), (Ｇ),(GX), (烈), (イ), (祭GX),(塔GX),(猟GX)");
				cond.addTargetCuffClass("(P), (P_Ⅱ), (P_Ｇ), (S), (S_イ), (S_韋), (S_猟), (S_祭), (S_Ｇ)");
				cond.addTargetJewelClass("無, (Ｇ), (GX), (Ｇ秘)");
				cond.setFlagCheckNumInvokedSkills(true);
				SearchItems searchItems = new SearchItems();
				searchItems.PrepareEquipmentDataTags(db, cond, false);
				
				ResultEquipmentSet filling = new ResultEquipmentSet("test");
				List<EquipmentDataTag> armorset = searchItems.ArmorDataTags.stream().map(al -> al.get(0)).collect(Collectors.toList());
				EquipmentDataTag cuff = searchItems.CuffDataTags.stream().filter(t -> t.base.getName().equals("空き")).iterator().next();
				for(EquipmentDataTag am : armorset) {
					filling.addEquipment(am.base, 1, 1);
				}
				filling.addEquipment(cuff.base, 1, 0);
				filling.addEquipment(db.getJewelData("天壁珠"), 1, -1);
				filling.addEquipment(db.getJewelData("匠珠"), 2, -1);
				filling.addEquipment(db.getJewelData("ゴゴ剣珠GX1"), 2, -1);
				filling.addEquipment(db.getJewelData("鉄壁珠"), 3, -1);
				filling.attrNumMaxSkills = 12;
				filling.attrSkillRankup = true;
				filling.CountRequiredSkillsInvoked(cond.RequiredInvokedSkills(), filling.attrNumMaxSkills);
				
				System.out.println(filling.toString());
				System.out.println(filling.toEquipmentClip());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}

	}

}
