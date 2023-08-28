
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.function.BinaryOperator;
public class VmTranslator {
    public static void main(String[] args) {
        if (args.length<1){
            throw new RuntimeException("no file or folder Specified");
        }else if (args.length>1){
            throw new RuntimeException("too many arguments");
        }

        StringBuilder outputStringBuilder = new StringBuilder();
        File input = new File(args[0]);
        ArrayList<String> instructions = new ArrayList<>();

        //check if target exsits
        if (!input.exists()){
            throw new RuntimeException("Target does not exist");
        }

        // if target is Directory -> readAll .vm files and create Bootstrap Code
        // else read file
        if (input.isDirectory()) {
            File[] files = input.listFiles();
            if (files!=null)
                for (File file : files) {
                    if (file.getName().endsWith(".vm")) {
                        ArrayList<String> tempInstructions =new ArrayList<>();
                        readInstructionsFromFileToArrayList(file, tempInstructions);
                        instructions.addAll(tempInstructions);
                    }
            }

            outputStringBuilder.append("@256\nD=A\n@SP\nM=D\n");
            outputStringBuilder.append(Translation.translations.get("call").apply("Sys.init","0"));
            if (!hasInitFunction(instructions)){
                instructions.addAll(List.of("function Sys.init 0","call Main.main 0","label loop","goto loop"));
            }
        } else if (input.isFile() && input.getName().endsWith(".vm")) {

            readInstructionsFromFileToArrayList(input, instructions);
        }

        //translate instructions
        for (String instruction : instructions) {
            instruction = instruction.trim();
            ArrayList<String> s = new ArrayList<>(List.of(instruction.split(" ")));
            s.removeAll(List.of(""));
            if (s.size() < 3){
                s.add("");
                s.add("");
            }
            if (Translation.translations.containsKey(s.get(0))){
                String translation = Translation.translations.get(s.get(0)).apply(s.get(1), s.get(2));
                outputStringBuilder.append(translation);
                //System.out.println("\n---------------------------------\nInstruction: "+instruction+"\n"+translation);//Debug output
            }
            else {
                System.out.println("unknown operator: " + instruction);
                throw new RuntimeException("unknown operator: " + instruction);
            }

        }

        //Write OutputFile
        String outputName = input.getName();

        if (input.isDirectory()) {
            outputName = input.getPath()+File.separator+outputName+ ".asm";
        } else if (outputName.contains(".")) {
            outputName = input.getParent()+File.separator+outputName.substring(0, outputName.lastIndexOf('.')) + ".asm";
        } else {
            outputName += ".asm";
        }


        File output = new File(outputName);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(output));
            writer.write(outputStringBuilder.toString());
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("done!\nOutput written to: "+outputName);

    }

    //read input, remove comments and add ClassNames to static Segments
    // (File, &ArrayList)->()

    private static void readInstructionsFromFileToArrayList(File input, ArrayList<String> instructions) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(input));
            String line;
            while ((line = reader.readLine()) != null) {
                instructions.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String className = calcClassName(input);
        removeComments(instructions);
        renameStaticAddr(instructions, className);
    }

    private static boolean hasInitFunction(ArrayList<String> pInstructions){
        for (String s : pInstructions) {
            if (s.contains("function Sys.init"))
                return true;
        }
        return false;
    }
    private static void removeComments(ArrayList<String> instructions){
        //remove comments
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

    }
    private static void renameStaticAddr(ArrayList<String> instructions, String className){
        String replaceRegex =className+"\\.$0";
        String regexFindStaticAddr="\\d+";
        Pattern pattern = Pattern.compile(regexFindStaticAddr);
        for (int i = 0; i < instructions.size(); ++i) {
            String instruction = instructions.get(i);
            if (instruction.contains("static")){
                Matcher instructionMatch = pattern.matcher(instruction);
                instruction = instructionMatch.replaceAll(replaceRegex);
            }
            instructions.set(i, instruction);
        }
    }
    private static String calcClassName (File file){
        return file.getName().substring(0,file.getName().lastIndexOf('.'));
    }

    private class Translation {
        public static Map<String, BinaryOperator<String>> translations = Map.ofEntries(
                Map.entry("add",(String p1 , String p2)-> add() ),
                Map.entry("sub",(String p1 , String p2)-> sub()),
                Map.entry("and",(String p1 , String p2)->and()),
                Map.entry("or",(String p1 , String p2)->or()),
                Map.entry("neg",(String p1 , String p2)->neg()),
                Map.entry("not",(String p1 , String p2)->not()),
                Map.entry("eq",(String p1 , String p2)-> compare("JEQ")),
                Map.entry("gt",(String p1 , String p2)->compare("JGT")),
                Map.entry("lt",(String p1 , String p2)->compare("JLT")),
                Map.entry("push", Translation::push),
                Map.entry("pop", Translation::pop),
                Map.entry("label",(String p1,String p2)->label(p1)),
                Map.entry("goto",(String p1,String p2)-> gotoLabel(p1)),
                Map.entry("if-goto",(String p1,String p2)->ifGoto(p1)),
                Map.entry("function",Translation::function),
                Map.entry("call", Translation::call),
                Map.entry("return",(String p1,String p2)->returnLabel())
        );
        private static int callCount=-1;
        private static final Map<String,String> segment =Map.of(
                "local", """
                        D=A
                        @LCL
                        A=M+D
                        """,
                "argument", """
                        D=A
                        @ARG
                        A=M+D
                        """,
                "static", "",
                "constant", "",
                "this", """
                        D=A
                        @THIS
                        A=M+D
                        """,
                "that", """
                        D=A
                        @THAT
                        A=M+D
                        """,
                "pointer", """
                        D=A
                        @THIS
                        A=A+D
                        """,
                "temp", """
                        D=A
                        @5
                        A=A+D
                        """
                );
        //pushes D to Stack
        private static final String pushString = """
                @SP
                A=M
                M=D
                @SP
                M=M+1
                """;
        private static final String popToA = """
                @SP
                AM=M-1
                A=M
                """;
        private static final String popToD = """
                @SP
                AM=M-1
                D=M
                """;
        public static String push (String pSegment, String pI){
            String returnString = "//push "+pSegment+" "+pI +"\n@"+pI+"\n"+segment.get(pSegment);
            if (pSegment.equals("constant")){
                returnString += "D=A\n";
            }
            else {
                returnString += "D=M\n";
            }


            return returnString+pushString;
        }
        public static String pop (String pSegment, String pI){
            return "//push "+pSegment+" "+pI +"\n@"+pI+"\n"+segment.get(pSegment)+"D=A\n@13\nM=D\n"+popToD+"@13\nA=M\nM=D\n";
        }
        public static String add(){
            return "//add\n"+popToD+popToA+"D=D+A\n"+pushString;
        }
        public static String sub(){
            return "//sub\n"+popToD+popToA+"D=A-D\n"+pushString;
        }
        public static String and(){
            return "//and\n"+popToD+popToA+"D=D&A\n"+pushString;
        }
        public static String or(){
            return "//or\n"+popToD+popToA+"D=D|A\n"+pushString;
        }
        public static String neg(){
            return "//neg\n"+popToD+"D=-D\n"+pushString;
        }
        public static String not(){
            return "//not\n"+popToD+"D=!D\n"+pushString;
        }
        public static String compare(String jmpCondition){
            callCount++;
            String comment ="//"+jmpCondition.substring(1).toLowerCase()+"\n";
            return comment+popToD+popToA+"D=A-D\n@trueLabelIdx"+callCount+"\nD;"+jmpCondition+"\nD=0\n@falseExitLabelIdx"+callCount+"\n0;JMP\n(trueLabelIdx"+callCount+")\nD=-1\n(falseExitLabelIdx"+callCount+")\n"+pushString;
        }
        public static String label (String pLabel){
            return "("+pLabel+")\n";
        }
        public static String gotoLabel(String pLabel){
            return "//goto "+pLabel+"\n@"+pLabel+"\n0;JMP\n";
        }
        public static String ifGoto (String pLabel){
            return "//if-goto "+pLabel+"\n"+popToD+"@"+pLabel+"\nD;JNE\n";
        }
        public static String call (String pFunctionName,String pNArgs){
            callCount++;
            return "//call "+pFunctionName+" "+pNArgs+"\n"+
                    "@returnLabelIdx"+callCount+"\nD=A\n"+pushString+ //push retAddrLabel
                    "@LCL\nD=M\n"+pushString+ //push LCL
                    "@ARG\nD=M\n"+pushString+ //push ARG//
                    "@THIS\nD=M\n"+pushString+ //push THIS//
                    "@THAT\nD=M\n"+pushString+ //push THAT//
                    //"@LCL\nD=M\n"+pushString+ //push LCL//
                    //"@SP\nM=M-1\n"+ // SP-1 DEBUG
                    "//ARG=SP-5-nArgs\n@SP\nD=M\n@5\nD=D-A\n@"+pNArgs+"\nD=D-A\n@ARG\nM=D\n"+ //ARG=SP-5-nArgs
                    "@SP\nD=M\n@LCL\nM=D\n"+
                    gotoLabel(pFunctionName)+
                    "(returnLabelIdx"+callCount+")\n";
        }
        public static String function(String pFunctionName, String pNVars){
            callCount++;
            return  "//function "+pFunctionName+" \n("+pFunctionName+")\n"+ // createFunction startLabel
                    "//push nVars 0s to @LCL using SP=LCL\n@SP\nD=M\n@LCL\nM=D\n@"+pNVars+"\nD=A\n(loopIdx"+callCount+")\n@endLoopIdx"+callCount+"\n" + //push nVars 0s to @LCL
                    "D;JEQ\n@SP\nA=M\nM=0\n@SP\nM=M+1\nD=D-1\n@loopIdx"+callCount+"\n0;JMP\n(endLoopIdx"+callCount+")\n";

        }
        public static String returnLabel (){
            return """
                    //return
                    //calcReturnAddrPointer
                    @LCL
                    D=M
                    @5
                    A=D-A
                    D=M
                    @R14
                    M=D
                    //pop returnValue in @Arg
                    """+popToD+"""
                    @ARG
                    A=M
                    M=D
                    //Set SP
                    D=A
                    @SP
                    M=D+1
                    //Set THAT
                    @LCL
                    AM=M-1
                    D=M
                    @THAT
                    M=D
                    //Set THIS
                    @LCL
                    AM=M-1
                    D=M
                    @THIS
                    M=D
                    //Set ARG
                    @LCL
                    AM=M-1
                    D=M
                    @ARG
                    M=D
                    //Set LCL
                    @LCL
                    A=M-1
                    D=M
                    @LCL
                    M=D
                    //goto returnAddr
                    @R14
                    A=M
                    0;JMP
                    """;
        }

    }
}
