package mhsx;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SearchConditionTest {
	static private mhsx.BaseData db = new mhsx.BaseData();

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		db.LoadXML_SkillBase("dat/");
		System.out.println("loading skillbase: done.");
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
	public void testCopy() {
		try {
			System.out.println("-- Copy test --");
			System.out.print("original: ");
			SearchCondition cond = new SearchCondition();
			cond.setLabel("testテスト");
			try {
				cond.setJob(ArmorData.HunterType.Common);
				fail("HunterType.Common must be rejected");
			} catch (IllegalArgumentException e){}
			cond.setJob(ArmorData.HunterType.Blademaster);
			try {
				cond.setSex(ArmorData.SexType.Common);
				fail("SexType.Common must be rejected");
			} catch (IllegalArgumentException e){}
			try {
				cond.setRequiredNumGrankEquip(2);
				fail("NumGrankEquip=2 must be rejected");
			} catch (IllegalArgumentException e){}
			cond.setRequiredNumGrankEquip(3);
			cond.setRequiredSkillRankup(true);
			cond.specifyArmorName(5, "ルヴナンGPフェルゼ");
			cond.addTargetArmorClass("(GX), (烈)");
			cond.addTargetJewelClass("(GX)"); 
			cond.addInvokedSkill(Arrays.asList("双剣技【双龍】", "一閃+3", "代償", "豪放+3", "紅焔の威光+2", "絶対防御態勢", "闘覇", "状態異常半減", "剣術+1", "業物+1", "ランナー"));
			cond.addEliminatedSkill("見切り+1");
			System.out.println(cond);
			//System.out.println(cond.writeJsonAsString());
			
			System.out.print("clone: ");
			SearchCondition copycond = cond.copy();
			System.out.println(copycond);
			
			assertTrue(cond.toString().equals(copycond.toString()));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testIsSuperiorTo() {
		System.out.println("-- EquipmentData.isSuperiorTo test --");
		SearchCondition cond = new SearchCondition();
		cond.setLabel("compare test");
		cond.setJob(ArmorData.HunterType.Blademaster);
		cond.addInvokedSkill(Arrays.asList("一閃+3", "豪放+3", "剛撃+3", "見切り+4", "ブチギレ", "弱点特効", "剣術+1", "業物+1"));
		cond.addEliminatedSkill("回避性能+1");
		cond.setRequiredNumGrankEquip(0);
		cond.setRequiredSkillRankup(false);
		
		ArmorData atest1 = new ArmorData("テスト防具A");
		ArmorData atest2 = new ArmorData("テスト防具B");
		atest1.setSlot(3);
		atest1.GoushuCount = 3;
		atest1.GRankCount = 1;
		atest2.setSlot(2);
		atest2.GoushuCount = 3;
		atest2.GRankCount = 1;
		assertTrue(EquipmentData.isSuperiorTo(atest1, atest2, cond));
		assertFalse(EquipmentData.isSuperiorTo(atest2, atest1, cond));
		
		atest2.setSlot(3);
		assertTrue(EquipmentData.isSuperiorTo(atest1, atest2, cond));
		assertTrue(EquipmentData.isSuperiorTo(atest2, atest1, cond));
		
		atest1.GoushuCount = 0;
		assertTrue(EquipmentData.isSuperiorTo(atest1, atest2, cond));
		assertTrue(EquipmentData.isSuperiorTo(atest2, atest1, cond));
		cond.setRequiredSkillRankup(true);
		assertFalse(EquipmentData.isSuperiorTo(atest1, atest2, cond));
		assertTrue(EquipmentData.isSuperiorTo(atest2, atest1, cond));
		atest1.GoushuCount = 3;
		
		atest2.GRankCount = 0;
		assertTrue(EquipmentData.isSuperiorTo(atest1, atest2, cond));
		assertTrue(EquipmentData.isSuperiorTo(atest2, atest1, cond));
		cond.setRequiredNumGrankEquip(3);
		assertTrue(EquipmentData.isSuperiorTo(atest1, atest2, cond));
		assertFalse(EquipmentData.isSuperiorTo(atest2, atest1, cond));
		atest2.GRankCount = 1;
		
		
		atest1.setSkillPoint(db.getSkill("剛撃").SkillID, 5);
		atest1.setSkillPoint(db.getSkill("三界の護り").SkillID, 4);
		atest1.setSkillPoint(db.getSkill("斬れ味").SkillID, 3);
		atest1.setSkillPoint(db.getSkill("ガード性能").SkillID, 5);
		atest2.setSkillPoint(db.getSkill("剛撃").SkillID, 5);
		atest2.setSkillPoint(db.getSkill("三界の護り").SkillID, 4);
		atest2.setSkillPoint(db.getSkill("斬れ味").SkillID, 3);
		atest2.setSkillPoint(db.getSkill("ガード性能").SkillID, 4);
		System.out.print("cond: "); System.out.println(cond);
		System.out.println(atest1);
		System.out.println(atest2);
		assertTrue(EquipmentData.isSuperiorTo(atest1, atest2, cond));
		assertTrue(EquipmentData.isSuperiorTo(atest2, atest1, cond));
		System.out.println("A >= B, and A <= B: OK");
		
		atest1.setSkillPoint(db.getSkill("三界の護り").SkillID, 2);
		System.out.println(atest1);
		System.out.println(atest2);
		assertFalse(EquipmentData.isSuperiorTo(atest1, atest2, cond));
		assertTrue(EquipmentData.isSuperiorTo(atest2, atest1, cond));
		System.out.println("not A >= B, and A <= B: OK");
		
		atest2.setSkillPoint(db.getSkill("剛撃").SkillID, 4);
		System.out.println(atest1);
		System.out.println(atest2);
		assertFalse(EquipmentData.isSuperiorTo(atest1, atest2, cond));
		assertFalse(EquipmentData.isSuperiorTo(atest2, atest1, cond));
		System.out.println("not A >= B, and not A <= B: OK");
		
		cond.removeSPCond(db.getSkill("剛撃").SkillID);
		assertFalse(EquipmentData.isSuperiorTo(atest1, atest2, cond));
		assertTrue(EquipmentData.isSuperiorTo(atest2, atest1, cond));

		atest1.setSkillPoint(db.getSkill("回避性能").SkillID, 3);
		atest2.setSkillPoint(db.getSkill("回避性能").SkillID, 5);
		assertFalse(EquipmentData.isSuperiorTo(atest1, atest2, cond));
		assertFalse(EquipmentData.isSuperiorTo(atest2, atest1, cond));
		
	}
}
