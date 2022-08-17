/**
 * 
 */
package mhsx;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.List;

import java.util.Random;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author aoi
 *
 */
public class BaseDataTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
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
	public void testLoadXML() {
		try {
			mhsx.BaseData db = new mhsx.BaseData();
			
			System.out.println("-- Loading SkillBase --");
			db.LoadXML_SkillBase("dat/");
			for(Map.Entry<String, List<SkillBase>> sks : db.getSkillList().entrySet()) {
				System.out.println(String.format("* %s [%d]", sks.getKey(), sks.getValue().size()));
				for(SkillBase sb : sks.getValue()) {
					System.out.println(String.format("%s\t(ID:%d)", sb.Name, sb.SkillID));
					for(Map.Entry<String, Integer> inv : sb.InvocationPoints.entrySet()) {
						System.out.print(String.format("\t%d: %s", inv.getValue(), inv.getKey()));
					}
					System.out.println("");
				}
			}
			assertEquals(db.getSkill("三界の護り").SkillID, 220);
			assertEquals(db.getSkill(1660).Name, "氷結剣");
			assertEquals(db.getSkillFromInvokedTitle("剛撃+5").SkillID, 130);
			//Thread.sleep(10000);

			System.out.println("-- Loading Armors --");
			db.LoadXML_ArmorData("dat/", null);
			Random rnd = new Random();
			for(int i = 0; i < 32; i++) {
				List<ArmorData> al = db.getArmorList(rnd.nextInt(db.getNumArmorKind()));
				ArmorData e = al.get(rnd.nextInt(al.size()));
				System.out.println(e);
			}
			System.out.println(db.getArmorList(0).get(db.getArmorList(0).size()-1));
			{
				ArmorData e = db.getArmorData("アルテラGFフォールド", 4);
				assertNotNull(e);
				System.out.println(e);
				assertEquals(e.Sex, ArmorData.SexType.Common);
				assertEquals(e.GoushuCount, 3);
				assertEquals(e.getSkillPoint(db.getSkill("適応撃").SkillID), Integer.valueOf(5));
				assertTrue(e.GoushuCount > 0);
				assertTrue(e.GRankCount == 0);
			}
			//Thread.sleep(10000);
			
			System.out.println("-- Loading Cuffs --");
			db.LoadXML_CuffData("dat/", null);
			for(int i = 0; i < 16; i++) {
				CuffData e = db.getCuffList().get(rnd.nextInt(db.getCuffList().size()));
				System.out.println(e);
			}
			System.out.println(db.getCuffList().get(db.getCuffList().size()-1));
			{
				CuffData e = db.getCuffData("強爆カフⅡＳＣ９");
				assertNotNull(e);
				System.out.println(e);
				assertEquals(e.getSlot(), 1);
				assertEquals(e.getSkillPoint(db.getSkill("広域").SkillID), Integer.valueOf(-10));
			}
			//Thread.sleep(10000);

			System.out.println("-- Loading Jewels --");
			db.LoadXML_JewelData("dat/", null);
			for(int i = 0; i < 32; i++) {
				JewelData e = db.getJewelList().get(rnd.nextInt(db.getJewelList().size()));
				System.out.println(e);
			}
			System.out.println(db.getJewelList().get(db.getJewelList().size()-1));
			{
				JewelData e = db.getJewelData("ポカラ射珠GX5");
				assertNotNull(e);
				System.out.println(e);
				assertEquals(e.getSlot(), 1);
				assertEquals(e.getSkillPoint(db.getSkill("三界の護り").SkillID), Integer.valueOf(2));
				assertTrue(e.getSkillPoint(db.getSkill("移動速度").SkillID) == 2);
				assertTrue(!e.getSkillPoint(db.getSkill("溜め短縮").SkillID).equals(2));
			}
			//System.gc();
			//Thread.sleep(10000);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
