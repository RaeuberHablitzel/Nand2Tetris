import java.util.ArrayList;
import java.util.regex.Pattern;


public class CompilationEngine {
    private final ArrayList <Token> tokens;
    private final StringBuilder indent= new StringBuilder();
    private final String indentString = "  ";
    private Token currentToken;
     private int currentTokenIdx;
    private final StringBuilder xmlString;

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
        if (str.startsWith("</")){
            indent.delete(0,indentString.length());
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


    CompilationEngine(ArrayList <Token> tokens) {
        this.tokens=tokens;
        this.currentTokenIdx = 0;
        this.currentToken=tokens.get(0);
        this.xmlString = new StringBuilder();

    }

    public void compile(){
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
                default -> throw new RuntimeException("Syntax Error in line: "+currentToken.lineIdx() +" @tokenIdx: "+ currentTokenIdx+" expected Statement instead found: " + currentToken.content());
            }
        }
        printXMLString("</statements>");
    }
    private void compileDo(){
        printXMLString("<doStatement>");
        process("do");
        compileSubroutineName();
        if(currentToken.content().equals(".")){
            process(".");
            compileSubroutineName();
        }
        process("(");
        compileExpressionList();
        process(")");
        process(";");
        printXMLString("</doStatement>");
    }
    private void compileIf(){
        printXMLString("<ifStatement>");
        process("if");
        process("(");
        compileExpression();
        process(")");
        process("{");
        compileStatements();
        process("}");
        if(currentToken.content().equals("else")){
            process("else");
            process("{");
            compileStatements();
            process("}");
        }
        printXMLString("</ifStatement>");
    }
    private void compileWhile(){
        printXMLString("<whileStatement>");
        process("while");
        process("(");
        compileExpression();
        process(")");
        process("{");
        compileStatements();
        process("}");
        printXMLString("</whileStatement>");
    }
    private void compileReturn(){
        printXMLString("<returnStatement>");
        process("return");
        if (!currentToken.content().equals(";"))
            compileExpression();
        process(";");
        printXMLString("</returnStatement>");
    }
    private void compileLetStatement(){
        printXMLString("<letStatement>");
        process("let");
        compileVarName();
        if (currentToken.content().equals("[")){
            process("[");
            compileExpression();
            process("]");
        }
        process("=");
        compileExpression();
        process(";");
        printXMLString("</letStatement>");
    }



    private void compileVarName(){
        compileIdentifier();
    }
    private void compileClassName(){
       compileIdentifier();
    }
    private void compileSubroutineName(){
        compileIdentifier();
    }
    private void compileIdentifier(){
        if (currentToken.type()==TokenType.identifier){
            process(currentToken.content());
        }else{
            throw new RuntimeException("Syntax Error in line: "+currentToken.lineIdx() +" @tokenIdx: "+ currentTokenIdx+" expected identifier, instead found: "+currentToken.content());
        }
    }
    private void compileIntegerConstant(){
        if (currentToken.type()==TokenType.integerConstant){
            process(currentToken.content());
        }else{
            throw new RuntimeException("Syntax Error in line: "+currentToken.lineIdx() +" @tokenIdx: "+ currentTokenIdx+" expected integerConstant, instead found: "+currentToken.content());
        }
    }
    private void compileStringConstant(){
        if (currentToken.type()==TokenType.stringConstant){
            process(currentToken.content());
        }else{
            throw new RuntimeException("Syntax Error in line: "+currentToken.lineIdx() +" @tokenIdx: "+ currentTokenIdx+" expected stringConstant, instead found: "+currentToken.content());
        }
    }
    private void compileKeywordConstant(){
        if (isKeywordConstant(currentToken)){
            process(currentToken.content());
        }else{
            throw new RuntimeException("Syntax Error in line: "+currentToken.lineIdx() +" @tokenIdx: "+ currentTokenIdx+" expected keywordConstant, instead found: "+currentToken.content());
        }
    }
    private void compileExpression(){
        printXMLString("<expression>");
        compileTerm();
        while (isOP(currentToken)){
            compileOP();
            compileTerm();
        }
        printXMLString("</expression>");

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
            compileVarName();
            switch (currentToken.content()) {
                case "[" -> {
                    process("[");
                    compileExpression();
                    process("]");
                }
                case "(" -> {
                    process("(");
                    compileExpressionList();
                    process(")");
                }
                case "." -> {
                    process(".");
                    compileSubroutineName();
                    process("(");
                    compileExpressionList();
                    process(")");
                }
            }
        } else if (currentToken.content().equals("(")) {
            process("(");
            compileExpression();
            process(")");
        } else if (isUnaryOP(currentToken)) {
            compileUnaryOP();
            compileTerm();
        }
        printXMLString("</term>");
    }

    private void compileExpressionList(){
        printXMLString("<expressionList>");
        if (!currentToken.content().equals(")")){
            compileExpression();
            while (currentToken.content().equals(",")) {
                process(",");
                compileExpression();
            }
        }
        printXMLString("</expressionList>");
    }
    private void compileOP(){
        if (isOP(currentToken))
            process(currentToken.content());
        else
            throw new RuntimeException("Syntax Error in line: "+currentToken.lineIdx() +" @tokenIdx: "+ currentTokenIdx+" expected op, instead found: "+currentToken.content());
    }
    private void compileUnaryOP(){
        if (isUnaryOP(currentToken))
            process(currentToken.content());
        else
            throw new RuntimeException("Syntax Error in line: "+currentToken.lineIdx() +" @tokenIdx: "+ currentTokenIdx+" expected unaryOP, instead found: "+currentToken.content());
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
        printXMLString("<class>");
        process("class");
        compileClassName();
        process("{");
        while (currentToken.content().matches("static|field")){
            compileClassVarDec();
        }
        while (currentToken.content().matches("constructor|function|method")){
            compileSubroutineDec();
        }
        process("}");
        printXMLString("</class>");
    }

    private void compileClassVarDec(){
        printXMLString("<classVarDec>");
        switch (currentToken.content()) {
            case "static" -> process("static");
            case "field" -> process("field");
            default -> throw new RuntimeException("Syntax Error in line: "+currentToken.lineIdx() +" @tokenIdx: "+ currentTokenIdx+" expected static or field, instead found: " + currentToken.content());
        }
        compileType();
        compileVarName();
        while (currentToken.content().equals(",")){
            process(",");
            compileVarName();
        }
        process(";");
        printXMLString("</classVarDec>");
    }

    private void compileType(){
        if (currentToken.content().matches("int|char|boolean") || currentToken.type()==TokenType.identifier){
            process(currentToken.content());
        }else {
            throw new RuntimeException("Syntax Error in line: "+currentToken.lineIdx() +" @tokenIdx: "+ currentTokenIdx+" expected a type, instead found: " + currentToken.content());
        }
    }
    private void compileSubroutineDec(){
        printXMLString("<subroutineDec>");
        if (currentToken.content().matches("constructor|function|method")){
            process(currentToken.content());
        }else{
            throw new RuntimeException("Syntax Error in line: "+currentToken.lineIdx() +" @tokenIdx: "+ currentTokenIdx+" expected (constructor|function|method), instead found: " + currentToken.content());
        }
        if (currentToken.content().equals("void")){
            process("void");
        }else {
            compileType();
        }
        compileSubroutineName();
        process("(");
        compileParameterList();
        process(")");
        compileSubroutineBody();
        printXMLString("</subroutineDec>");
    }
    private void compileParameterList(){
        printXMLString("<parameterList>");
        if (!currentToken.content().equals(")")){
            compileType();
            compileVarName();
            while (currentToken.content().equals(",")){
                process(",");
                compileType();
                compileVarName();
            }
        }
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
        printXMLString("<varDec>");
        process("var");
        compileType();
        compileVarName();
        while (currentToken.content().equals(",")){
            process(",");
            compileVarName();
        }
        process(";");
        printXMLString("</varDec>");
    }
    public void printTokenXML(){
        System.out.println("<tokens>");
        for (Token token : tokens) {
            String contentStr = escapeXmlString(token);
            System.out.println("<"+token.type()+"> "+contentStr+" </"+token.type()+">");
        }
        System.out.println("</tokens>");
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
}
