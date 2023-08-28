import java.util.*;
import java.util.regex.Pattern;


public class CompilationEngine {



    private final ArrayList <Token> tokens;
    private final StringBuilder indent= new StringBuilder();
    private Token currentToken;
    private int currentTokenIdx;
    private final StringBuilder xmlString;
    private final HashMap<String,VarRecord> classVarTable;
    private final HashMap<String,VarRecord> subroutineVarTable;
    private VarRecord tempVarRecord;
    private final EnumMap<VarKind,Integer> varIndexMap;
    private final boolean printVarTables;
    private final ArrayList<String> vmInstructions;
    private final ArrayList<String> tempInstructions;
    private String workingClassName;
    private final HashMap<String,Boolean> definedFunctionSet;
    private final HashMap<String,Integer> expectedArgCount;
    private int ifIdx=0;
    private int whileIdx=0;


    private void findAllSubroutines(){
        for (int i=0; i< tokens.size(); ++i){
            if (tokens.get(i).content().matches("constructor|function|method")&&i+2<tokens.size()){
                definedFunctionSet.put(tokens.get(i+2).content(),tokens.get(i).content().equals("method"));
                int rawVarCount=0;
                for (int j=i+4; j < tokens.size();++j){
                    if (tokens.get(j).content().equals(")"))
                        break;
                    rawVarCount++;
                }
                int varCount=Math.max(0,(rawVarCount-2) /3 + Math.min(rawVarCount-1,1));
                expectedArgCount.put(tokens.get(i+2).content(),varCount);
            }
        }
    }
    private void printXMLToken(Token token){
        String contentStr = escapeXmlString(token);
        xmlString.append(indent);
        xmlString.append("<");
        xmlString.append(token.type());
        xmlString.append("> ");
        xmlString.append(contentStr);
        xmlString.append(" </");
        xmlString.append(token.type());
        xmlString.append(">\n");
        //System.out.println(indent+"<"+token.type()+"> "+contentStr+" </"+token.type()+">");
    }
    private void printXMLString(String str){
        //reduce indentation if closing Statement
        String indentString = "  ";
        if (str.startsWith("</")){
            indent.delete(0, indentString.length());
        }
        xmlString.append(indent);
        xmlString.append(str);
        xmlString.append("\n");

        //increase indentation if opening Statement
        if (str.startsWith("<")&&!str.startsWith("</")){
            indent.append(indentString);
        }
    }
    private void process(String str){
        if (str.equals(currentToken.content())){
            printXMLToken(currentToken);
        }else{
            throw new RuntimeException("Syntax Error!  in line: "+currentToken.lineIdx() +" @tokenIdx: "+ currentTokenIdx+" expected: "+str+" instead found: "+currentToken.content());
        }

        // get next Token
        if (currentTokenIdx<tokens.size()-1)
            currentToken= tokens.get(++currentTokenIdx);
    }


    CompilationEngine(ArrayList <Token> tokens, boolean printVarTables) {
        this.tokens=tokens;
        this.currentTokenIdx = 0;
        this.currentToken = tokens.get(0);
        this.xmlString = new StringBuilder();
        varIndexMap = new EnumMap<>(VarKind.class);
        classVarTable = new HashMap<>();
        subroutineVarTable = new HashMap<>();
        vmInstructions = new ArrayList<>();
        tempInstructions = new ArrayList<>();
        this.printVarTables = printVarTables;
        definedFunctionSet = new HashMap<>();
        expectedArgCount = new HashMap<>();
    }

    public void compile(){
        findAllSubroutines();
        //definedFunctionSet.entrySet().forEach(System.out::println);
        compileClass();
    }

    private void compileStatements(){
        printXMLString("<statements>");
        while (!currentToken.content().equals("}")){
            switch (currentToken.content()) {
                case "let" -> compileLetStatement();
                case "if" -> compileIf();
                case "while" -> compileWhile();
                case "do" -> compileDo();
                case "return" -> compileReturn();
                default -> errorHandler(" expected Statement instead found: ", "");
            }
        }
        printXMLString("</statements>");
    }
    private void compileDo(){
        int numberOfArguments=0;
        int numberOfPrePassedArguments=0;
        boolean isLocalSubroutine=false;
        printXMLString("<doStatement>");
        process("do");
        String varName = compileSubroutineName();
        String funcName="";
        if(currentToken.content().equals(".")){
            process(".");


            funcName = compileSubroutineName();
            isLocalSubroutine = varName.equals(workingClassName);
            if (classVarTable.containsKey(varName)||subroutineVarTable.containsKey(varName)){
                numberOfPrePassedArguments++;
                checkIfVarExistsAddToInstList(varName,"push ",vmInstructions);
                String type;
                if(subroutineVarTable.containsKey(varName)){
                    type = subroutineVarTable.get(varName).type();
                }else{
                    type = classVarTable.get(varName).type();
                }
                funcName= type+"."+funcName;
            }else{
                funcName= varName + "." +funcName;
            }
        }else {

            if (definedFunctionSet.containsKey(varName)) {
                isLocalSubroutine=true;
                if (definedFunctionSet.get(varName)){
                    numberOfPrePassedArguments++;
                    vmInstructions.add("push pointer 0");
                }
                funcName = workingClassName + "." + varName;
            }
            else
                errorHandler(" undefined function: ", "");
        }
        process("(");
        numberOfArguments += compileExpressionList();


        if (isLocalSubroutine){
            String s = funcName.substring(funcName.indexOf('.')+1);
            if(expectedArgCount.get(s)!= numberOfArguments){
                errorHandler(" Argument mismatch at ","expected "+ expectedArgCount.get(s)+" Arguments instead found "+numberOfArguments);
            }
        }
        process(")");
        process(";");
        printXMLString("</doStatement>");
        vmInstructions.add("call "+ funcName+" "+(numberOfArguments+numberOfPrePassedArguments));
        vmInstructions.add("pop temp 0");
    }
    private void compileIf(){
        printXMLString("<ifStatement>");
        String elseLabel ="elseLabel"+ifIdx;
        String endIfLabel= "endIfLabel"+ifIdx++;
        process("if");
        process("(");
        compileCommonIfWhile(endIfLabel, elseLabel);
        if(currentToken.content().equals("else")){
            process("else");
            process("{");
            compileStatements();
            process("}");
        }
        vmInstructions.add("label "+endIfLabel);
        printXMLString("</ifStatement>");
    }
    private void compileWhile(){
        printXMLString("<whileStatement>");
        String whileStartLabel = "whileStart"+whileIdx;
        String whileEndLabel = "whileEnd"+whileIdx++;
        process("while");
        process("(");
        vmInstructions.add("label "+whileStartLabel);
        compileCommonIfWhile(whileStartLabel, whileEndLabel);
        printXMLString("</whileStatement>");
    }

    private void compileCommonIfWhile(String StartLabel, String EndLabel) {
        compileExpression();
        vmInstructions.add("not");
        vmInstructions.add("if-goto "+ EndLabel);
        process(")");
        process("{");
        compileStatements();
        process("}");
        vmInstructions.add("goto "+ StartLabel);
        vmInstructions.add("label "+ EndLabel);
    }

    private void compileReturn(){
        printXMLString("<returnStatement>");
        process("return");
        if (!currentToken.content().equals(";"))
            compileExpression();
        else
            vmInstructions.add("push constant 0");
        vmInstructions.add("return");
        process(";");
        printXMLString("</returnStatement>");
    }
    private void compileLetStatement(){
        ArrayList<String> letTempInstructions = new ArrayList<>();
        printXMLString("<letStatement>");
        process("let");
        String varName = compileVarName();
        if (currentToken.content().equals("[")){
            checkIfVarExistsAddToInstList(varName, "push ", vmInstructions);
            process("[");
            compileExpression();
            process("]");
            vmInstructions.add("add");
            letTempInstructions.add("pop temp 0");
            letTempInstructions.add("pop pointer 1");
            letTempInstructions.add("push temp 0");
            letTempInstructions.add("pop that 0");
        }else{
            checkIfVarExistsAddToInstList(varName, "pop ", letTempInstructions);
        }
        process("=");
        compileExpression();
        process(";");
        printXMLString("</letStatement>");
        vmInstructions.addAll(letTempInstructions);
    }

    private void checkIfVarExistsAddToInstList(String varName, String instruction, ArrayList<String> instructionList) {
        if (subroutineVarTable.containsKey(varName)){
            instructionList.add(instruction + subroutineVarTable.get(varName).kind()+" "+subroutineVarTable.get(varName).index());
        }else if (classVarTable.containsKey(varName)){
            instructionList.add(instruction + classVarTable.get(varName).kind()+" "+classVarTable.get(varName).index());
        }else {
            errorHandler(" undefined variable: ", "");
        }
    }


    private String compileVarName(){
        String varName = currentToken.content();
        compileIdentifier();
        return varName;
    }
    private String compileClassName(){
        String className = currentToken.content();
        compileIdentifier();
        return className;
    }
    private String compileSubroutineName(){
        String subroutineName = currentToken.content();
        compileIdentifier();
        return subroutineName;
    }
    private void compileIdentifier(){
        if (currentToken.type()==TokenType.identifier){
            process(currentToken.content());
        }else{
            errorHandler(" expected identifier, instead found: ", "");
        }
    }
    private void compileIntegerConstant(){
        if (currentToken.type()==TokenType.integerConstant){
            vmInstructions.add("push constant "+ currentToken.content());
            process(currentToken.content());
        }else{
            errorHandler(" expected integerConstant, instead found: ", "");
        }
    }
    private void compileStringConstant(){
        if (currentToken.type()==TokenType.stringConstant){
            String content = currentToken.content();
            // push string length
            vmInstructions.add("push constant "+ content.length());
            vmInstructions.add("call String.new 1");
            content.chars().forEach((int c)->{
                vmInstructions.add("push constant "+c);
                vmInstructions.add("call String.appendChar 2");
            });

            process(currentToken.content());
        }else{
            errorHandler(" expected stringConstant, instead found: ", "");
        }
    }
    private void compileKeywordConstant(){
        if (isKeywordConstant(currentToken)){

            switch (currentToken.content()){
                case "true"-> {
                    vmInstructions.add("push constant 1");
                    vmInstructions.add("neg");
                }
                case "false", "null" -> vmInstructions.add("push constant 0");
                case "this" -> vmInstructions.add("push pointer 0");
            }

            process(currentToken.content());
        }else{
            errorHandler(" expected keywordConstant, instead found: ", "");
        }
    }
    private void compileExpression(){
        ArrayList<String> tempCopy = new ArrayList<>(tempInstructions);
        tempInstructions.clear();
        printXMLString("<expression>");
        compileTerm();
        while (isOP(currentToken)){
            compileOP();
            compileTerm();
            vmInstructions.addAll(tempInstructions);
            tempInstructions.clear();
        }
        printXMLString("</expression>");
        tempInstructions.addAll(tempCopy);

    }
    private void compileTerm(){
        printXMLString("<term>");
        if(currentToken.type()==TokenType.integerConstant){
            compileIntegerConstant();
        } else if (currentToken.type()==TokenType.stringConstant) {
            compileStringConstant();
        } else if (isKeywordConstant(currentToken)) {
            compileKeywordConstant();
        } else if (currentToken.type()==TokenType.identifier) {
            String varName =  compileVarName();
            switch (currentToken.content()) {
                case "[" -> {
                    process("[");
                    compileExpression();
                    process("]");
                    checkIfVarExistsAddToInstList(varName, "push ", vmInstructions);
                    vmInstructions.add("add");
                    vmInstructions.add("pop pointer 1");
                    vmInstructions.add("push that 0");
                }
                case "(" -> {
                    process("(");
                    int numberOfArguments;
                    int numberOfPrePassedArguments=0;
                    if (definedFunctionSet.containsKey(varName) && definedFunctionSet.get(varName)){
                        numberOfPrePassedArguments=1;
                        vmInstructions.add("push pointer 0");
                    }
                    numberOfArguments = compileExpressionList();
                    if(expectedArgCount.get(varName)!= numberOfArguments){
                        errorHandler(" Argument mismatch at ","expected "+ expectedArgCount.get(varName)+" Arguments instead found "+numberOfArguments);
                    }
                    process(")");
                    if (definedFunctionSet.containsKey(varName))
                        vmInstructions.add("call "+workingClassName+"."+varName+" "+(numberOfArguments+numberOfPrePassedArguments));
                    else
                        errorHandler(" undefined function: ", "");

                }
                case "." -> {
                    process(".");
                    int numberOfArguments=0;
                    int numberOfPrePassedArguments=0;
                    String type=varName;
                    String subroutineName = compileSubroutineName();
                    if (classVarTable.containsKey(varName)||subroutineVarTable.containsKey(varName)){
                        numberOfPrePassedArguments=1;
                        checkIfVarExistsAddToInstList(varName,"push ",vmInstructions);

                        if(subroutineVarTable.containsKey(varName)){
                            type = subroutineVarTable.get(varName).type();
                        }else{
                            type = classVarTable.get(varName).type();
                        }
                    }
                    process("(");
                    numberOfArguments = compileExpressionList();
                    process(")");
                    if (varName.equals(workingClassName)){
                        if(expectedArgCount.get(subroutineName)!= numberOfArguments){
                            errorHandler(" Argument mismatch at ","expected "+ expectedArgCount.get(subroutineName)+" Arguments instead found "+numberOfArguments);
                        }
                    }
                    vmInstructions.add("call "+ type+"."+subroutineName+" "+(numberOfArguments+numberOfPrePassedArguments));

                }
                default -> checkIfVarExistsAddToInstList(varName, "push ", vmInstructions);
            }
        } else if (currentToken.content().equals("(")) {
            process("(");
            compileExpression();

            process(")");
        } else if (isUnaryOP(currentToken)) {
            compileUnaryOP();
            compileTerm();
            vmInstructions.addAll(tempInstructions);
            tempInstructions.clear();
        }
        printXMLString("</term>");
    }

    private int compileExpressionList(){
        printXMLString("<expressionList>");
        int expressionListCount=0;
        if (!currentToken.content().equals(")")){
            compileExpression();
            expressionListCount++;
            while (currentToken.content().equals(",")) {
                process(",");
                compileExpression();
                expressionListCount++;
            }
        }
        printXMLString("</expressionList>");
        return expressionListCount;
    }
    private void compileOP(){
        if (isOP(currentToken)) {
            switch (currentToken.content()){
                case "+" -> tempInstructions.add("add");
                case "-" -> tempInstructions.add("sub");
                case "*" -> tempInstructions.add("call Math.multiply 2");
                case "/" -> tempInstructions.add("call Math.divide 2");
                case "&" -> tempInstructions.add("and");
                case "|" -> tempInstructions.add("or");
                case "<" -> tempInstructions.add("lt");
                case ">" -> tempInstructions.add("gt");
                case "=" -> tempInstructions.add("eq");
            }
            process(currentToken.content());
        }
        else
            errorHandler(" expected op, instead found: ", "");
    }
    private void compileUnaryOP(){
        if (isUnaryOP(currentToken)) {
            switch (currentToken.content()) {
                case "-" -> tempInstructions.add("neg");
                case "~" -> tempInstructions.add("not");
                default -> errorHandler(" expected unaryOp, instead found: ", "");
            }
            process(currentToken.content());
        }
        else
            errorHandler(" expected unaryOP, instead found: ", "");
    }

    private void errorHandler(String preContentMsg, String postContentMsg) {
        throw new RuntimeException("Syntax Error in line: "+currentToken.lineIdx() +" @tokenIdx: "+ currentTokenIdx+ preContentMsg +currentToken.content()+ postContentMsg);
    }

    private boolean isOP (Token token){
        String str = token.content();
        return str.matches("[+\\-*/=<>&|]");
    }

    private boolean isKeywordConstant(Token token){
        return token.content().matches("true|false|null|this");
    }
    private boolean isUnaryOP(Token token){
        return token.content().matches("[\\-~]");
    }
    private void compileClass(){
        classVarTable.clear();
        varIndexMap.clear();
        varIndexMap.put(VarKind.STATIC,0);
        varIndexMap.put(VarKind.FIELD, 0);
        printXMLString("<class>");
        process("class");
        String className = compileClassName();
        workingClassName = className;
        process("{");
        while (currentToken.content().matches("static|field")){
            compileClassVarDec();
        }
        while (currentToken.content().matches("constructor|function|method")){
            compileSubroutineDec();
        }
        process("}");
        printXMLString("</class>");

        if(printVarTables)
            printClassVarTable(className);

    }

    private void compileClassVarDec(){
        int varIdx=-1;
        VarKind varKind=null;
        tempVarRecord = new VarRecord(null,null, -1);
        printXMLString("<classVarDec>");
        switch (currentToken.content()) {
            case "static" -> {
                varIdx= varIndexMap.get(VarKind.STATIC);
                varKind= VarKind.STATIC;
                process("static");
            }
            case "field" -> {
                varIdx=varIndexMap.get(VarKind.FIELD);
                varKind=VarKind.FIELD;

                process("field");
            }
            default -> errorHandler(" expected static or field, instead found: ", "");
        }
        tempVarRecord= new VarRecord(null,varKind,varIdx++);

        compileType();
        String varName = compileVarName();

        addVarToClassVarTable(varName);
        while (currentToken.content().equals(",")){
            process(",");
            tempVarRecord= new VarRecord(tempVarRecord.type(),varKind,varIdx++);
            varName = compileVarName();
            addVarToClassVarTable(varName);
        }
        process(";");


        varIndexMap.put(varKind,varIdx);

        printXMLString("</classVarDec>");
    }

    private void addVarToClassVarTable(String varName) {
        if(classVarTable.containsKey(varName)){
            errorHandler(" Variable: ", " already exists");
        }else{
            classVarTable.put(varName,tempVarRecord);
        }
    }
    private void addVarToSubroutineVarTable(String varName) {
        if(subroutineVarTable.containsKey(varName)){
            errorHandler(" Variable: ", " already exists");
        }else{
            subroutineVarTable.put(varName,tempVarRecord);
        }
    }




    private void compileType(){
        if (currentToken.content().matches("int|char|boolean") || currentToken.type()==TokenType.identifier){
            tempVarRecord = new VarRecord(currentToken.content(),tempVarRecord.kind(),tempVarRecord.index());
            process(currentToken.content());
        }else {
            errorHandler(" expected a type, instead found: ", "");
        }
    }
    private void compileSubroutineDec(){
        // Reset VarTable and indexMap for subroutine
        varIndexMap.put(VarKind.ARGUMENT,0);
        varIndexMap.put(VarKind.LOCAL,0);
        subroutineVarTable.clear();

        int funcInstructionIndex = vmInstructions.size();


        printXMLString("<subroutineDec>");
        String functionType = currentToken.content();
        if (functionType.matches("method"))
            varIndexMap.put(VarKind.ARGUMENT, 1);
        if (currentToken.content().matches("constructor|function|method")){
            process(currentToken.content());

        }else{
            errorHandler(" expected (constructor|function|method), instead found: ", "");
        }
        if (currentToken.content().equals("void")){
            process("void");
        }else {
            compileType();
        }
        String subroutineName = compileSubroutineName();
        vmInstructions.add("function "+ workingClassName+"."+subroutineName+" ");
        process("(");
        compileParameterList(); //TODO add numbersOfArguments Check
        process(")");

        switch (functionType) {
            case "constructor" -> {

                if (varIndexMap.get(VarKind.FIELD)>0) {
                    vmInstructions.add("push constant " + varIndexMap.get(VarKind.FIELD));
                    vmInstructions.add("call Memory.alloc 1");
                    vmInstructions.add("pop pointer 0");
                }
            }
            case "method" -> {
                vmInstructions.add("push argument 0");
                vmInstructions.add("pop pointer 0");
            }
        }
        compileSubroutineBody();
        printXMLString("</subroutineDec>");
        vmInstructions.set(funcInstructionIndex, vmInstructions.get(funcInstructionIndex)+varIndexMap.get(VarKind.LOCAL));
        if (printVarTables)
            printSubroutineVarTable(subroutineName);
    }
    private void compileParameterList(){
        int varIdx = varIndexMap.get(VarKind.ARGUMENT);

        printXMLString("<parameterList>");
        if (!currentToken.content().equals(")")){
            tempVarRecord = new VarRecord(null,VarKind.ARGUMENT,varIdx++);
            compileType();
            String varName = compileVarName();
            addVarToSubroutineVarTable(varName);
            while (currentToken.content().equals(",")){
                process(",");
                tempVarRecord = new VarRecord(null,VarKind.ARGUMENT,varIdx++);
                compileType();
                varName = compileVarName();
                addVarToSubroutineVarTable(varName);
            }
        }
        varIndexMap.put(VarKind.ARGUMENT,varIdx);
        printXMLString("</parameterList>");
    }
    private void compileSubroutineBody(){

        printXMLString("<subroutineBody>");
        process("{");


        while (currentToken.content().equals("var")) {
            compileVarDec();
        }
        compileStatements();
        process("}");
        printXMLString("</subroutineBody>");
    }
    private void compileVarDec(){
        int varIdx = varIndexMap.get(VarKind.LOCAL);
        tempVarRecord = new VarRecord(null,VarKind.LOCAL,varIdx++);
        printXMLString("<varDec>");
        process("var");
        compileType();
        String varName = compileVarName();
        addVarToSubroutineVarTable(varName);
        while (currentToken.content().equals(",")){
            process(",");
            varName = compileVarName();
            tempVarRecord = new VarRecord(tempVarRecord.type(),VarKind.LOCAL,varIdx++);
            addVarToSubroutineVarTable(varName);
        }
        process(";");
        printXMLString("</varDec>");
        varIndexMap.put(VarKind.LOCAL,varIdx);
    }
    public void printTokenXML(){
        System.out.println("<tokens>");
        for (Token token : tokens) {
            String contentStr = escapeXmlString(token);
            System.out.println("<"+token.type()+"> "+contentStr+" </"+token.type()+">");
        }
        System.out.println("</tokens>");
    }

    public void printVmInstructions(){
        System.out.println("VmInstructions: ");
        vmInstructions.forEach(System.out::println);
    }

    public void printClassVarTable(String className){ // TODO make nice output
        System.out.println("\nClassVarTable "+className + ":");
        System.out.println("| Name | Type | Kind | # |");
        System.out.println("------------------------------");
        classVarTable.forEach((key, value) -> System.out.println("| "+key+" | "+value.type()+" | "+value.kind()+" | "+value.index()+" |"));
    }
    private void printSubroutineVarTable(String subroutineName){
        System.out.println("\n subroutineVarTable: "+subroutineName + ":");
        System.out.println("| Name | Type | Kind | # |");
        System.out.println("------------------------------");
        subroutineVarTable.forEach((key, value) -> System.out.println("| "+key+" | "+value.type()+" | "+value.kind()+" | "+value.index()+" |"));
    }

    public String getTokenXML(){
        StringBuilder returnStringBuilder = new StringBuilder("<tokens>\n");
        for (Token token : tokens) {
            String contentStr = escapeXmlString(token);

            returnStringBuilder.append("<");
            returnStringBuilder.append(token.type());
            returnStringBuilder.append("> ");
            returnStringBuilder.append(contentStr);
            returnStringBuilder.append(" </");
            returnStringBuilder.append(token.type());
            returnStringBuilder.append(">\n");

        }
        returnStringBuilder.append("</tokens>\n");
        return returnStringBuilder.toString();
    }

    public String escapeXmlString(Token token) {
        String contentStr = token.content();
        Pattern ltPattern = Pattern.compile("<");
        Pattern gtPattern = Pattern.compile(">");
        Pattern ampPattern = Pattern.compile("&");
        Pattern quotPattern = Pattern.compile("\"");
        contentStr = ampPattern.matcher(contentStr).replaceAll("&amp;");
        contentStr = ltPattern.matcher(contentStr).replaceAll("&lt;");
        contentStr = gtPattern.matcher(contentStr).replaceAll("&gt;");
        contentStr = quotPattern.matcher(contentStr).replaceAll("&quot;");
        return contentStr;
    }

    public String getXmlString() {
        return xmlString.toString();
    }
    public String getInstructionString(){
        StringBuilder instructionStringBuilder = new StringBuilder();
        vmInstructions.forEach((String instruction)-> {
            instructionStringBuilder.append(instruction);
            instructionStringBuilder.append("\n");
        });
        return instructionStringBuilder.toString();
    }

}
