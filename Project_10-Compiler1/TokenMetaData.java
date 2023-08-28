import java.util.Map;

public class TokenMetaData {
    private static final Map<String,TokenType> tokenTypeMap=Map.ofEntries(
            Map.entry("class",TokenType.keyword),
            Map.entry("constructor",TokenType.keyword),
            Map.entry("function",TokenType.keyword),
            Map.entry("method",TokenType.keyword),
            Map.entry("field",TokenType.keyword),
            Map.entry("static",TokenType.keyword),
            Map.entry("var",TokenType.keyword),
            Map.entry("int",TokenType.keyword),
            Map.entry("char",TokenType.keyword),
            Map.entry("boolean",TokenType.keyword),
            Map.entry("void",TokenType.keyword),
            Map.entry("true",TokenType.keyword),
            Map.entry("false",TokenType.keyword),
            Map.entry("null",TokenType.keyword),
            Map.entry("this",TokenType.keyword),
            Map.entry("that",TokenType.keyword),
            Map.entry("let",TokenType.keyword),
            Map.entry("do",TokenType.keyword),
            Map.entry("if",TokenType.keyword),
            Map.entry("else",TokenType.keyword),
            Map.entry("while",TokenType.keyword),
            Map.entry("return",TokenType.keyword),
            Map.entry("{",TokenType.symbol),
            Map.entry("}",TokenType.symbol),
            Map.entry("(",TokenType.symbol),
            Map.entry(")",TokenType.symbol),
            Map.entry("[",TokenType.symbol),
            Map.entry("]",TokenType.symbol),
            Map.entry(".",TokenType.symbol),
            Map.entry(",",TokenType.symbol),
            Map.entry(";",TokenType.symbol),
            Map.entry("+",TokenType.symbol),
            Map.entry("-",TokenType.symbol),
            Map.entry("*",TokenType.symbol),
            Map.entry("/",TokenType.symbol),
            Map.entry("&",TokenType.symbol),
            Map.entry("|",TokenType.symbol),
            Map.entry("<",TokenType.symbol),
            Map.entry(">",TokenType.symbol),
            Map.entry("=",TokenType.symbol),
            Map.entry("~",TokenType.symbol)
    );
    public static boolean isBreakingChar(char pChar){
        String sChar= String.valueOf(pChar);
        return pChar=='"' || pChar==' '|| pChar=='\n' || pChar=='\t' ||(tokenTypeMap.containsKey( sChar)&&tokenTypeMap.get(sChar)==TokenType.symbol);
    }
    public static boolean isSingleCharToken(char pChar){
        String sChar= String.valueOf(pChar);
        return tokenTypeMap.containsKey(sChar)&&tokenTypeMap.get(sChar)==TokenType.symbol;
    }
    public static boolean isPrintableChar(char pChar){
        return pChar!= ' ' && pChar!= '\n' && pChar != '\t';
    }

    public static TokenType getTokenType (String tokenString){
        TokenType tokenType = TokenType.UNKNOWN;
        if (tokenTypeMap.containsKey(tokenString))
            tokenType=tokenTypeMap.get(tokenString);
        // if isIntegerConstant
        else if (tokenString.matches("\\d+")){
            tokenType=TokenType.integerConstant;
        //id isIdentifier
        } else if (tokenString.matches("[a-zA-Z_][a-zA-Z_\\d]*")) {
            tokenType=TokenType.identifier;
        }

        return tokenType;
    }
}
