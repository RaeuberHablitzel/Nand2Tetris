import java.util.ArrayList;

public class TokenReader {
    public ArrayList<Token> readTokesFromString (String inputString){
         StateSet state = new StateSet();
         for (char c : inputString.toCharArray()) {
             readTokens(state, c);
         }

         return state.tokenList;
     }

    private static void readTokens (StateSet state, char currentChar){
        //System.out.println("State "+ state.getTokenReadingState()+ " currentChar: "+ currentChar + " line: "+state.getLineIdx());


        if (state.getTokenReadingState()==TokenReadingStates.STRING){ // check if currently reading string
            if (currentChar=='"'){                                    // check if string end
                state.tokenList.add(new Token(TokenType.stringConstant,state.tokenString.toString(),state.getLineIdx()));
                state.setTokenReadingState(TokenReadingStates.DEFAULT);
                state.tokenString=new StringBuilder();
            }else
                state.tokenString.append(currentChar);

        } else if (state.getTokenReadingState()==TokenReadingStates.LINE_COMMENT) { // check if currently reading line comment
            if (currentChar=='\n')
                state.setTokenReadingState(TokenReadingStates.DEFAULT);
        } else if (state.getTokenReadingState()==TokenReadingStates.BLOCK_COMMENT) { // check if currently reading block or api comment
            if (currentChar=='/'&& state.tokenString.charAt(state.tokenString.length()-1)=='*'){ // check if comment end by looking at previous read char
                state.setTokenReadingState(TokenReadingStates.DEFAULT);
                state.tokenString=new StringBuilder();
            }else{
                state.tokenString.append(currentChar);
            }
        } else {
            // if current char is '/' check if previous token is also '/' -> enter lineComment mode
            if (currentChar == '/' && state.tokenList.size()>0 &&state.tokenList.get(state.tokenList.size()-1).content().equals("/")) {
                state.setTokenReadingState(TokenReadingStates.LINE_COMMENT);
                state.tokenList.remove(state.tokenList.size()-1);

            //else if current char is '*' check if previous token is also '/' -> enter blockComment mode
            } else if (currentChar == '*' && state.tokenList.size()>0 && state.tokenList.get(state.tokenList.size()-1).content().equals("/")){
                state.setTokenReadingState(TokenReadingStates.BLOCK_COMMENT);
                state.tokenList.remove(state.tokenList.size()-1);

            // else DEFAULT MODE
            } else {
                // is breaking char and the TokenString is not empty
                if (TokenMetaData.isBreakingChar(currentChar) && !state.tokenString.isEmpty()) {
                    String tokenString = state.tokenString.toString();
                    state.tokenList.add(new Token(TokenMetaData.getTokenType(tokenString), tokenString, state.getLineIdx()));
                    state.tokenString = new StringBuilder();
                }
                // if Beginning of String enter String mode
                if (currentChar == '"') {
                    state.setTokenReadingState(TokenReadingStates.STRING);
                } else {
                    //ignore whitespace, tab, and linefeed
                    if (TokenMetaData.isPrintableChar(currentChar))
                        state.tokenString.append(currentChar);

                    //check if char is singleChar token
                    if (TokenMetaData.isSingleCharToken(currentChar)) {
                        String tokenString = state.tokenString.toString();
                        state.tokenList.add(new Token(TokenMetaData.getTokenType(tokenString), tokenString, state.getLineIdx()));
                        state.tokenString = new StringBuilder();
                    }
                }
            }

        }

        //increment Line counter
        if (currentChar=='\n')
            state.incLineIdx();

    }
}
