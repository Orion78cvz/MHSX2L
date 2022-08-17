/**
 * 
 */
package mhsx;

/**
 * ログ, 検索結果などテキストの出力用インターフェイス
 *
 */
public interface SearchOutput {
	
	public void msg(String text);
	public void log(String text, int level);
	
	public void resultBegin();
	public void resultEntry(ResultEquipmentSet result);
	public void resultEnd(ResultCode code);
	enum ResultCode {
		Success, Failed, Aborted;
		@Override
		public String toString() {
			switch(this) {
			case Success: return "完了";
			case Failed: return "失敗";
			case Aborted: return "中断";
			default: throw new IllegalStateException();
			}
		}
	}
	
	public static class NullOutput implements SearchOutput {
		final static private NullOutput _inst = new NullOutput();
		static public NullOutput instance() {return _inst;} //適当に
		
		public void msg(String text) {}
		public void log(String text, int level) {}

		public void resultBegin() {}
		public void resultEntry(ResultEquipmentSet result) {}
		public void resultEnd(ResultCode code) {}
	}
}
