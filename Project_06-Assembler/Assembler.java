import java.util.*;
import java.io.*;

import static java.lang.Math.min;

public class Assembler {
	public static void main(String[] args) {
		// Map of predefined symbols
		Map<String, Integer> predefinedSymbols = Map.ofEntries(
				Map.entry("SP", 0),
				Map.entry("LCL", 1),
				Map.entry("ARG", 2),
				Map.entry("THIS", 3),
				Map.entry("THAT", 4),
				Map.entry("SCREEN", 16384),
				Map.entry("KBD", 24576),
				Map.entry("R0", 0),
				Map.entry("R1", 1),
				Map.entry("R2", 2),
				Map.entry("R3", 3),
				Map.entry("R4", 4),
				Map.entry("R5", 5),
				Map.entry("R6", 6),
				Map.entry("R7", 7),
				Map.entry("R8", 8),
				Map.entry("R9", 9),
				Map.entry("R10", 10),
				Map.entry("R11", 11),
				Map.entry("R12", 12),
				Map.entry("R13", 13),
				Map.entry("R14", 14),
				Map.entry("R15", 15)
		);
		Map<String, Integer> jmpMap = Map.of(
				"JGT",1,
				"JEQ",2,
				"JGE",3,
				"JLT",4,
				"JNE",5,
				"JLE",6,
				"JMP",7
		);
		Map<String, Integer> compMap = Map.ofEntries(
				Map.entry(  "0",0x0A80),
				Map.entry(  "1",0x0FC0),
				Map.entry( "-1",0x0E80),
				Map.entry(  "D",0x0300),
				Map.entry(  "A",0x0C00),
				Map.entry( "!D",0x0340),
				Map.entry( "!A",0x0C40),
				Map.entry( "-D",0x03C0),
				Map.entry( "-A",0x0CC0),
				Map.entry("D+1",0x07C0),
				Map.entry("A+1",0x0DC0),
				Map.entry("D-1",0x0380),
				Map.entry("A-1",0x0C80),
				Map.entry("D+A",0x0080),
				Map.entry("D-A",0x04C0),
				Map.entry("A-D",0X01C0),
				Map.entry("D&A",0x0000),
				Map.entry("D|A",0x0540)
		);

		HashMap<String, Integer> symbols = new HashMap<>();
		int variableIdxOffset = 16;
		ArrayList<Integer> translatedInstructions  = new ArrayList<>();


		ArrayList<String> instructions = new ArrayList<String>();

		//Einlesen der Datei

		try {
			BufferedReader reader = new BufferedReader(new FileReader(args[0]));
			String instruction = reader.readLine();
			while (instruction != null) {
				instructions.add(instruction);
				instruction = reader.readLine();
			}
			reader.close();

		} catch (IOException e) {
			e.printStackTrace();
		}


		for (int i = 0; i < instructions.size(); ++i) {
			String command = instructions.get(i);
			int idx = command.indexOf("//");
			if (idx >= 0) {
				command = command.substring(0, idx);
			}
			command = command.strip();
			instructions.set(i, command);
		}
		// remove empty lines
		instructions.removeAll(List.of(""));


		// calc Label addr
		for (int i = 0; i < instructions.size(); ++i) {
			String command = instructions.get(i);
			if (command.startsWith("(") && command.endsWith(")") && command.length() > 2) {
				symbols.put(command.substring(1, command.length() - 1), i);
				instructions.remove(i);
				--i; // yes I know it's a sin, but it was the fastest way I could think of (in like 20 sec and no I won't turn it into a while loop...)
			}
		}
		// calc memLocations
		for (String command : instructions) {
			if (command.startsWith("@") && command.length() > 1) {
				String symbol = command.substring(1);

				if (!predefinedSymbols.containsKey(symbol) &&
						!symbols.containsKey(symbol) && !symbol.matches("\\d*")) {
					symbols.put(symbol, variableIdxOffset++);
				}
			}
		}

		// translate
		for (String instruction : instructions) {
			if (instruction.startsWith("@")) {

				// translate A-Instruction TODO: Fix highest bit maybe 1
				String symbol = instruction.substring(1);
				if(symbol.matches("\\d*")){
					translatedInstructions.add(Integer.parseInt(symbol)&0x7fff);
				}else if (symbols.containsKey(symbol)){
					translatedInstructions.add(symbols.get(symbol));
				}else{
					translatedInstructions.add(predefinedSymbols.get(symbol));
				}
			}else{
				// translate c-Instructions
				int translatedInstruction = 0xE000;
				int destIdx = instruction.indexOf("=");
				int destValue = 0;
				int jumpValue = 0;
				int compValue = 0;

				if (destIdx>0){
					String dest = instruction.substring(0, destIdx);

					destValue= 8*(min(dest.indexOf("M")+1,1)+min(dest.indexOf("D")+1,1)*2+min(dest.indexOf("A")+1,1)*4);

					instruction = instruction.substring(destIdx+1);
				}
				int jumpIdx = instruction.indexOf(";");

				if (jumpIdx>0 && jmpMap.containsKey( instruction.substring(jumpIdx+1))){
					jumpValue = jmpMap.get(instruction.substring(jumpIdx+1));
					instruction=instruction.substring(0,jumpIdx);
				}
				if (instruction.contains("M")){
					compValue = 0x1000;
					instruction = instruction.replace("M","A");

				}

				if (compMap.containsKey(instruction)){
					compValue+=compMap.get(instruction);
				}
				translatedInstruction += destValue+compValue+jumpValue;
				translatedInstructions.add(translatedInstruction);
			}
		}


		StringBuilder outputString = new StringBuilder();
		for (int instruction : translatedInstructions) {
			outputString.append( Integer.toBinaryString(instruction+0x10000).substring(1));
			outputString.append("\n");
		}

		try {
		String path = args[0];
		path = path.substring(0, path.lastIndexOf("."))+".hack";
		BufferedWriter writer = new BufferedWriter(new FileWriter(path));
		writer.write(outputString.toString());
		writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}



