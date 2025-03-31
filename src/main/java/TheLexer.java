import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * Lexer class to analyze the input file
 * This version correctly handles token splitting and error identification
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
        String[] keywords = {"if", "else", "while", "for", "class", "public", "private", "protected", "void", "int", "float", "double", "boolean", "string", "return"};
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
        // Identificadores - Add $ as valid identifier character
        addLetterTransitions("START", "ID");
        dfa.addTransition("START", "_", "ID");
        dfa.addTransition("START", "$", "ID"); // Add $ as valid identifier start
        addLetterTransitions("ID", "ID");
        addDigitTransitions("ID", "ID");
        dfa.addTransition("ID", "_", "ID");
        dfa.addTransition("ID", "$", "ID"); // Allow $ in identifiers
        
        // Números
        addDigitTransitions("START", "NUM");
        addDigitTransitions("NUM", "NUM");
        dfa.addTransition("NUM", ".", "FLOAT_START");
        addDigitTransitions("FLOAT_START", "FLOAT");
        addDigitTransitions("FLOAT", "FLOAT");
        
        // Hexadecimales
        dfa.addTransition("START", "0", "HEX_START");
        dfa.addTransition("HEX_START", "x", "HEX");
        dfa.addTransition("HEX_START", "X", "HEX");
        addHexTransitions("HEX", "HEX");
        
        // Binarios
        dfa.addTransition("HEX_START", "b", "BIN");
        dfa.addTransition("HEX_START", "B", "BIN");
        dfa.addTransition("BIN", "0", "BIN");
        dfa.addTransition("BIN", "1", "BIN");
        
        // Octales
        addOctalTransitions("HEX_START", "OCT");
        addOctalTransitions("OCT", "OCT");
        
        // Strings
        dfa.addTransition("START", "\"", "STRING");
        dfa.addTransition("STRING", "\"", "STRING_ACCEPT");

        for (int i = 32; i < 127; i++) {
            if (i != 34) { 
                dfa.addTransition("STRING", String.valueOf((char)i), "STRING");
            }
        }
        
        // Chars - Improved to better handle errors
        dfa.addTransition("START", "'", "CHAR");
        for (int i = 32; i < 127; i++) {
            if (i != 39) { // except single quote
                dfa.addTransition("CHAR", String.valueOf((char)i), "CHAR_CONTENT");
            }
        }
        dfa.addTransition("CHAR_CONTENT", "'", "CHAR_ACCEPT");
        
        // Operadores
        for (Character op : OPERATORS) {
            dfa.addTransition("START", op.toString(), "OP_ACCEPT");
        }
        
        // Operadores compuestos
        dfa.addTransition("OP_ACCEPT", "=", "OP_ACCEPT"); // Para operadores como +=, -=, etc.
        
        // Delimitadores
        for (Character delim : DELIMITERS) {
            dfa.addTransition("START", delim.toString(), "DELIM_ACCEPT");
        }
        
        // Estados de aceptación
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
            // Skip whitespace
            if (isWhitespace(line.charAt(i))) {
                i++;
                continue;
            }
            
            // Check if character is an operator or delimiter (splitting characters)
            if (isDelimiterOrOperator(line.charAt(i))) {
                // Process the delimiter or operator as a token
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
            
            // Process tokens that are not delimiters or operators
            StringBuilder lexeme = new StringBuilder();
            String currentState = "START";
            boolean isError = false;
            
            int startPos = i;
            
            // Special handling for character literals (improved)
            if (i < line.length() && line.charAt(i) == '\'') {
                lexeme.append('\'');
                i++;
                
                // Valid char has exactly one character between quotes
                if (i < line.length()) {
                    char content = line.charAt(i);
                    lexeme.append(content);
                    i++;
                    
                    // Check for closing quote
                    if (i < line.length() && line.charAt(i) == '\'') {
                        lexeme.append('\'');
                        i++;
                        tokens.add(new TheToken(lexeme.toString(), "CHAR"));
                    } else {
                        // Continue consuming until whitespace/delimiter to complete the error token
                        while (i < line.length() && !isWhitespace(line.charAt(i)) && 
                               !isDelimiterOrOperator(line.charAt(i))) {
                            lexeme.append(line.charAt(i));
                            i++;
                        }
                        tokens.add(new TheToken(lexeme.toString(), "ERROR"));
                    }
                } else {
                    // Unclosed quote
                    tokens.add(new TheToken(lexeme.toString(), "ERROR"));
                }
                continue;
            }
            
            // Special case for b' which should be ERROR
            if (i + 1 < line.length() && 
                (line.charAt(i) == 'b' || line.charAt(i) == 'B') && 
                line.charAt(i + 1) == '\'') {
                lexeme.append(line.charAt(i));
                lexeme.append('\'');
                i += 2;
                
                // Continue consuming until whitespace/delimiter to complete the error token
                while (i < line.length() && !isWhitespace(line.charAt(i)) && 
                       !isDelimiterOrOperator(line.charAt(i))) {
                    lexeme.append(line.charAt(i));
                    i++;
                }
                tokens.add(new TheToken(lexeme.toString(), "ERROR"));
                continue;
            }
            
            // Regular token processing
            while (i < line.length()) {
                char currentChar = line.charAt(i);
                
                // Break on whitespace or delimiters/operators
                if (isWhitespace(currentChar) || isDelimiterOrOperator(currentChar)) {
                    break;
                }
                
                String nextState = dfa.getNextState(currentState, currentChar);
                
                if (nextState == null) {
                    // If no valid transition, mark as error
                    isError = true;
                }
                
                lexeme.append(currentChar);
                if (nextState != null) {
                    currentState = nextState;
                }
                
                i++;
            }
            
            // Process the accumulated lexeme
            String lexemeStr = lexeme.toString();
            if (!lexemeStr.isEmpty()) {
                if (isError || !dfa.isAcceptState(currentState)) {
                    tokens.add(new TheToken(lexemeStr, "ERROR"));
                } else {
                    String type = dfa.getAcceptStateName(currentState);
                    // Check if identifier is actually a keyword
                    if (type.equals("IDENTIFIER") && KEYWORDS.contains(lexemeStr)) {
                        type = "KEYWORD";
                    }
                    tokens.add(new TheToken(lexemeStr, type));
                }
            }
        }
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