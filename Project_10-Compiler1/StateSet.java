import java.util.ArrayList;

public class StateSet {
    public StringBuilder tokenString;
    private TokenReadingStates tokenReadingState;

    public ArrayList<Token> tokenList;
    private int lineIdx;

    public TokenReadingStates getTokenReadingState() {
        return tokenReadingState;
    }

    public void setTokenReadingState(TokenReadingStates tokenReadingState) {
        this.tokenReadingState = tokenReadingState;
    }

    public int getLineIdx() {
        return lineIdx;
    }
    public void incLineIdx(){
        ++lineIdx;
    }

    StateSet(){
        tokenList=new ArrayList<>();
        tokenString=new StringBuilder();
        tokenReadingState =TokenReadingStates.DEFAULT;
        lineIdx=1;
    }
}
