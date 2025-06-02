import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 *
 * @author GABRIEL ZAID
 *         ISAC HUMBERTO
 *         BRANDON MAGANA
 * @version 0.5
 */
public class TheLexer {
    private File file;
    private Vector<TheToken> tokens;
    private Automata dfa;
    private static final Set<String> KEYWORDS;
    private static final Set<Character> DELIMITERS;
    private static final Set<Character> OPERATORS;

    static {
        KEYWORDS = new HashSet<>();
        String[] keywords = {"if", "else", "while", "for", "class", "public", "private", "protected", "void", "int", "float", "double", "boolean", "string", "return", "true", "false"};
        for (String keyword : keywords) {
            KEYWORDS.add(keyword);
        }
        
        DELIMITERS = new HashSet<>();
        char[] delims = {';', ',', '{', '}', '(', ')', '[', ']', ':'};
        for (char delim : delims) {
            DELIMITERS.add(delim);
        }
        
        OPERATORS = new HashSet<>();
        char[] ops = {'+', '-', '*', '/', '=', '<', '>', '!', '&', '|', '%', '^'};
        for (char op : ops) {
            OPERATORS.add(op);
        }
    }

    public TheLexer(File file) {
        this.file = file;
        this.tokens = new Vector<>();
        this.dfa = new Automata();
        initializeAutomata();
    }

    private void initializeAutomata() {
        addLetterTransitions("START", "ID");
        dfa.addTransition("START", "_", "ID");
        dfa.addTransition("START", "$", "ID");
        addLetterTransitions("ID", "ID");
        addDigitTransitions("ID", "ID");
        dfa.addTransition("ID", "_", "ID");
        dfa.addTransition("ID", "$", "ID");
        
        addDigitTransitions("START", "NUM");
        addDigitTransitions("NUM", "NUM");
        dfa.addTransition("NUM", ".", "FLOAT_START");
        addDigitTransitions("FLOAT_START", "FLOAT");
        addDigitTransitions("FLOAT", "FLOAT");
        
        dfa.addTransition("START", "0", "HEX_START");
        dfa.addTransition("HEX_START", "x", "HEX");
        dfa.addTransition("HEX_START", "X", "HEX");
        addHexTransitions("HEX", "HEX");
        
        dfa.addTransition("HEX_START", "b", "BIN");
        dfa.addTransition("HEX_START", "B", "BIN");
        dfa.addTransition("BIN", "0", "BIN");
        dfa.addTransition("BIN", "1", "BIN");
        
        addOctalTransitions("HEX_START", "OCT");
        addOctalTransitions("OCT", "OCT");
        
        dfa.addTransition("START", "\"", "STRING");
        dfa.addTransition("STRING", "\"", "STRING_ACCEPT");

        for (int i = 32; i < 127; i++) {
            if (i != 34) { 
                dfa.addTransition("STRING", String.valueOf((char)i), "STRING");
            }
        }
        
        dfa.addTransition("START", "'", "CHAR");
        for (int i = 32; i < 127; i++) {
            if (i != 39) {
                dfa.addTransition("CHAR", String.valueOf((char)i), "CHAR_CONTENT");
            }
        }
        dfa.addTransition("CHAR_CONTENT", "'", "CHAR_ACCEPT");
        
        for (Character op : OPERATORS) {
            dfa.addTransition("START", op.toString(), "OP_ACCEPT");
        }
        
        dfa.addTransition("OP_ACCEPT", "=", "OP_ACCEPT");
        
        for (Character delim : DELIMITERS) {
            dfa.addTransition("START", delim.toString(), "DELIM_ACCEPT");
        }
        
        dfa.addAcceptState("ID", "IDENTIFIER");
        dfa.addAcceptState("NUM", "INTEGER");
        dfa.addAcceptState("FLOAT", "FLOAT");
        dfa.addAcceptState("HEX_START", "INTEGER"); 
        dfa.addAcceptState("HEX", "HEXADECIMAL");
        dfa.addAcceptState("BIN", "BINARY");
        dfa.addAcceptState("OCT", "OCTAL");
        dfa.addAcceptState("STRING_ACCEPT", "STRING");
        dfa.addAcceptState("CHAR_ACCEPT", "CHAR");
        dfa.addAcceptState("OP_ACCEPT", "OPERATOR");
        dfa.addAcceptState("DELIM_ACCEPT", "DELIMITER");
    }

    private void addLetterTransitions(String fromState, String toState) {
        for (char c = 'a'; c <= 'z'; c++) {
            dfa.addTransition(fromState, String.valueOf(c), toState);
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            dfa.addTransition(fromState, String.valueOf(c), toState);
        }
    }

    private void addDigitTransitions(String fromState, String toState) {
        for (char c = '0'; c <= '9'; c++) {
            dfa.addTransition(fromState, String.valueOf(c), toState);
        }
    }

    private void addHexTransitions(String fromState, String toState) {
        addDigitTransitions(fromState, toState);
        for (char c = 'a'; c <= 'f'; c++) {
            dfa.addTransition(fromState, String.valueOf(c), toState);
        }
        for (char c = 'A'; c <= 'F'; c++) {
            dfa.addTransition(fromState, String.valueOf(c), toState);
        }
    }

    private void addOctalTransitions(String fromState, String toState) {
        for (char c = '0'; c <= '7'; c++) {
            dfa.addTransition(fromState, String.valueOf(c), toState);
        }
    }

    public void run() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                processLine(line);
            }
        }
    }

    private void processLine(String line) {
        int i = 0;
        while (i < line.length()) {
            if (isWhitespace(line.charAt(i))) {
                i++;
                continue;
            }
            
            if (line.charAt(i) == '"') {
                i = processStringLiteral(line, i);
                continue;
            }
            
            if (line.charAt(i) == '\'') {
                i = processCharLiteral(line, i);
                continue;
            }
            
            if (i + 1 < line.length()) {
                String compound = line.substring(i, i + 2);
                if (isCompoundOperator(compound)) {
                    tokens.add(new TheToken(compound, "OPERATOR"));
                    i += 2;
                    continue;
                }
            }
            
            if (isDelimiterOrOperator(line.charAt(i))) {
                String currentState = "START";
                String nextState = dfa.getNextState(currentState, line.charAt(i));
                if (nextState != null) {
                    currentState = nextState;
                    if (dfa.isAcceptState(currentState)) {
                        tokens.add(new TheToken(String.valueOf(line.charAt(i)), dfa.getAcceptStateName(currentState)));
                    } else {
                        tokens.add(new TheToken(String.valueOf(line.charAt(i)), "ERROR"));
                    }
                } else {
                    tokens.add(new TheToken(String.valueOf(line.charAt(i)), "ERROR"));
                }
                i++;
                continue;
            }
            
            i = processRegularToken(line, i);
        }
    }
    
    private int processStringLiteral(String line, int startPos) {
        StringBuilder lexeme = new StringBuilder();
        lexeme.append('"');
        int i = startPos + 1;
        
        while (i < line.length()) {
            char c = line.charAt(i);
            lexeme.append(c);
            
            if (c == '"') {
                tokens.add(new TheToken(lexeme.toString(), "STRING"));
                return i + 1;
            }
            i++;
        }
        
        tokens.add(new TheToken(lexeme.toString(), "ERROR"));
        return i;
    }
    
    private int processCharLiteral(String line, int startPos) {
        StringBuilder lexeme = new StringBuilder();
        lexeme.append('\'');
        int i = startPos + 1;
        
        if (startPos > 0 && (line.charAt(startPos - 1) == 'b' || line.charAt(startPos - 1) == 'B')) {
            return startPos;
        }
        
        if (i < line.length()) {
            char content = line.charAt(i);
            lexeme.append(content);
            i++;
            
            if (i < line.length() && line.charAt(i) == '\'') {
                lexeme.append('\'');
                i++;
                tokens.add(new TheToken(lexeme.toString(), "CHAR"));
                return i;
            }
        }
        
        while (i < line.length() && !isWhitespace(line.charAt(i)) && 
               !isDelimiterOrOperator(line.charAt(i))) {
            lexeme.append(line.charAt(i));
            i++;
        }
        tokens.add(new TheToken(lexeme.toString(), "ERROR"));
        return i;
    }
    
    private int processRegularToken(String line, int startPos) {
        StringBuilder lexeme = new StringBuilder();
        String currentState = "START";
        boolean isError = false;
        int i = startPos;
        
        if (i + 1 < line.length() && 
            (line.charAt(i) == 'b' || line.charAt(i) == 'B') && 
            line.charAt(i + 1) == '\'') {
            lexeme.append(line.charAt(i));
            lexeme.append('\'');
            i += 2;
            
            while (i < line.length() && !isWhitespace(line.charAt(i)) && 
                   !isDelimiterOrOperator(line.charAt(i))) {
                lexeme.append(line.charAt(i));
                i++;
            }
            tokens.add(new TheToken(lexeme.toString(), "ERROR"));
            return i;
        }
        
        while (i < line.length()) {
            char currentChar = line.charAt(i);
            
            if (isWhitespace(currentChar) || isDelimiterOrOperator(currentChar)) {
                break;
            }
            
            String nextState = dfa.getNextState(currentState, currentChar);
            
            if (nextState == null) {
                isError = true;
            }
            
            lexeme.append(currentChar);
            if (nextState != null) {
                currentState = nextState;
            }
            
            i++;
        }
        
        String lexemeStr = lexeme.toString();
        if (!lexemeStr.isEmpty()) {
            if (isError || !dfa.isAcceptState(currentState)) {
                tokens.add(new TheToken(lexemeStr, "ERROR"));
            } else {
                String type = dfa.getAcceptStateName(currentState);
                if (type.equals("IDENTIFIER") && KEYWORDS.contains(lexemeStr)) {
                    type = "KEYWORD";
                }
                tokens.add(new TheToken(lexemeStr, type));
            }
        }
        
        return i;
    }
    
    private boolean isCompoundOperator(String op) {
        return op.equals("&&") || op.equals("||") || op.equals("==") || 
               op.equals("!=") || op.equals("<=") || op.equals(">=") ||
               op.equals("++") || op.equals("--") || op.equals("+=") ||
               op.equals("-=") || op.equals("*=") || op.equals("/=") ||
               op.equals("%=") || op.equals("&=") || op.equals("|=") ||
               op.equals("^=") || op.equals("<<") || op.equals(">>");
    }

    private boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }
    
    private boolean isDelimiterOrOperator(char c) {
        return DELIMITERS.contains(c) || OPERATORS.contains(c);
    }

    public Vector<TheToken> getTokens() {
        return tokens;
    }

    public void printTokens() {
        System.out.println("LEXEMA\t\t|\tTIPO");
        System.out.println("--------------------------");
        for (TheToken token : tokens) {
            System.out.printf("%-15s|\t%s%n", token.getValue(), token.getType());
        }
    }
}