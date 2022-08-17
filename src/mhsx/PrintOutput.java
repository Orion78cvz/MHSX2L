package mhsx;

import java.io.PrintStream;

public class PrintOutput implements SearchOutput {
	PrintStream stream = System.out;
	
	public PrintOutput() {
	}
	public PrintOutput(PrintStream pstream) {
		stream = pstream;
	}

	@Override
	public void msg(String text) {
		stream.println(text);
	}

	@Override
	public void log(String text, int level) {
		stream.println(text);
	}

	@Override
	public void resultBegin() {}
	@Override
	public void resultEntry(ResultEquipmentSet result) {
		stream.println(result.toInfoJsonString());
		stream.println(result.toEquipmentClip());
		stream.println();
	}
	@Override
	public void resultEnd(ResultCode code) {
		stream.println(code.toString());
	}

}
