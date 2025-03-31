import java.util.Vector;

/**
 * Parser class for a Java subset
 */

/**
 * Parser class to analyze rules
 *
 * @author GABRIEL ZAID
 *         ISAC HUMBERTO
 *         BRANDON MAGANA
 * @version 0.5
 */
public class TheParser {
    private Vector<TheToken> tokens;
    private int currentToken;
    
    public TheParser(Vector<TheToken> tokens) {
        this.tokens = tokens;
        currentToken = 0;
    }
    
    public void run() {
        RULE_PROGRAM();
    }
    
    private void RULE_PROGRAM() {
        System.out.println("- RULE_PROGRAM");
        match("class");
        if (tokens.get(currentToken).getType().equals("IDENTIFIER")) currentToken++;
        match("{");
        while (!tokens.get(currentToken).getValue().equals("}")) {
            if (isType(tokens.get(currentToken).getValue())) {
                RULE_GLOBAL_ATTRIBUTE();
            } else {
                RULE_METHOD();
            }
        }
        match("}");
    }
    
    private void RULE_GLOBAL_ATTRIBUTE() {
        System.out.println("-- RULE_GLOBAL_ATTRIBUTE");
        RULE_TYPE();
        if (tokens.get(currentToken).getType().equals("IDENTIFIER")) currentToken++;
        match(";");
    }
    
    private void RULE_METHOD() {
        System.out.println("-- RULE_METHOD");
        if (tokens.get(currentToken).getValue().equals("void")) {
            currentToken++;
        } else {
            RULE_TYPE();
        }
        if (tokens.get(currentToken).getType().equals("IDENTIFIER")) currentToken++;
        match("(");
        if (!tokens.get(currentToken).getValue().equals(")")) {
            RULE_PARAMS();
        }
        match(")");
        match("{");
        RULE_BODY();
        match("}");
    }
    
    private void RULE_TYPE() {
        System.out.println("-- RULE_TYPE");
        if (isType(tokens.get(currentToken).getValue())) currentToken++;
    }
    
    private boolean isType(String value) {
        return value.equals("int") || value.equals("float") || 
               value.equals("double") || value.equals("boolean") || 
               value.equals("String") || value.equals("char");
    }
    
    private void RULE_PARAMS() {
        System.out.println("-- RULE_PARAMS");
        RULE_TYPE();
        if (tokens.get(currentToken).getType().equals("IDENTIFIER")) currentToken++;
        while (tokens.get(currentToken).getValue().equals(",")) {
            currentToken++;
            RULE_TYPE();
            if (tokens.get(currentToken).getType().equals("IDENTIFIER")) currentToken++;
        }
    }
    
    private void RULE_BODY() {
        System.out.println("-- RULE_BODY");
        while (!tokens.get(currentToken).getValue().equals("}")) {
            if (isType(tokens.get(currentToken).getValue())) {
                RULE_VARIABLE();
            } else if (tokens.get(currentToken).getType().equals("IDENTIFIER")) {
                String identifier = tokens.get(currentToken).getValue();
                currentToken++;
                if (tokens.get(currentToken).getValue().equals("=")) {
                    currentToken--; // Retroceder al identificador
                    RULE_ASSIGNMENT();
                } else if (tokens.get(currentToken).getValue().equals("(")) {
                    currentToken--; // Retroceder al identificador
                    RULE_CALL_METHOD();
                    match(";");
                }
            } else if (tokens.get(currentToken).getValue().equals("if")) {
                RULE_IF();
            } else if (tokens.get(currentToken).getValue().equals("while")) {
                RULE_WHILE();
            } else if (tokens.get(currentToken).getValue().equals("do")) {
                RULE_DO_WHILE();
            } else if (tokens.get(currentToken).getValue().equals("for")) {
                RULE_FOR();
            } else if (tokens.get(currentToken).getValue().equals("switch")) {
                RULE_SWITCH();
            } else if (tokens.get(currentToken).getValue().equals("return")) {
                RULE_RETURN();
            } else {
                currentToken++; // Avanzar si hay un token no reconocido
            }
        }
    }
    
    private void RULE_VARIABLE() {
        System.out.println("-- RULE_VARIABLE");
        RULE_TYPE();
        if (tokens.get(currentToken).getType().equals("IDENTIFIER")) currentToken++;
        if (tokens.get(currentToken).getValue().equals("=")) {
            currentToken++;
            RULE_EXPRESSION();
        }
        match(";");
    }
    
    private void RULE_ASSIGNMENT() {
        System.out.println("-- RULE_ASSIGNMENT");
        if (tokens.get(currentToken).getType().equals("IDENTIFIER")) currentToken++;
        match("=");
        RULE_EXPRESSION();
        match(";");
    }
    
    private void RULE_EXPRESSION() {
        System.out.println("--- RULE_EXPRESSION");
        RULE_X();
        while (currentToken < tokens.size() && tokens.get(currentToken).getValue().equals("|")) {
            currentToken++;
            RULE_X();
        }
    }
    
    private void RULE_X() {
        System.out.println("---- RULE_X");
        RULE_Y();
        while (currentToken < tokens.size() && tokens.get(currentToken).getValue().equals("&")) {
            currentToken++;
            RULE_Y();
        }
    }
    
    private void RULE_Y() {
        System.out.println("----- RULE_Y");
        if (currentToken < tokens.size() && tokens.get(currentToken).getValue().equals("!")) {
            currentToken++;
        }
        RULE_R();
    }
    
    private void RULE_R() {
        System.out.println("------ RULE_R");
        RULE_E();
        while (currentToken < tokens.size() && 
              (tokens.get(currentToken).getValue().equals("<") ||
               tokens.get(currentToken).getValue().equals(">") ||
               tokens.get(currentToken).getValue().equals("=") ||
               tokens.get(currentToken).getValue().equals("!"))) {
            currentToken++;
            if (currentToken < tokens.size() && tokens.get(currentToken).getValue().equals("=")) {
                currentToken++;
            }
            RULE_E();
        }
    }
    
    private void RULE_E() {
        System.out.println("------- RULE_E");
        RULE_A();
        while (currentToken < tokens.size() && 
              (tokens.get(currentToken).getValue().equals("+") ||
               tokens.get(currentToken).getValue().equals("-"))) {
            currentToken++;
            RULE_A();
        }
    }
    
    private void RULE_A() {
        System.out.println("-------- RULE_A");
        RULE_B();
        while (currentToken < tokens.size() && 
              (tokens.get(currentToken).getValue().equals("*") ||
               tokens.get(currentToken).getValue().equals("/"))) {
            currentToken++;
            RULE_B();
        }
    }
    
    private void RULE_B() {
        System.out.println("--------- RULE_B");
        if (currentToken < tokens.size() && tokens.get(currentToken).getValue().equals("-")) {
            currentToken++;
        }
        RULE_C();
    }
    
    private void RULE_C() {
        System.out.println("---------- RULE_C");
        if (currentToken < tokens.size()) {
            if (tokens.get(currentToken).getType().equals("IDENTIFIER")) {
                currentToken++;
                if (currentToken < tokens.size() && tokens.get(currentToken).getValue().equals("(")) {
                    currentToken--; // Retroceder
                    RULE_CALL_METHOD();
                }
            } else if (tokens.get(currentToken).getType().equals("INTEGER") || 
                       tokens.get(currentToken).getType().equals("FLOAT") ||
                       tokens.get(currentToken).getType().equals("STRING") ||
                       tokens.get(currentToken).getType().equals("CHAR") ||
                       tokens.get(currentToken).getValue().equals("true") ||
                       tokens.get(currentToken).getValue().equals("false")) {
                currentToken++;
            } else if (tokens.get(currentToken).getValue().equals("(")) {
                currentToken++;
                RULE_EXPRESSION();
                match(")");
            }
        }
    }
    
    private void RULE_WHILE() {
        System.out.println("-- RULE_WHILE");
        match("while");
        match("(");
        RULE_EXPRESSION();
        match(")");
        if (tokens.get(currentToken).getValue().equals("{")) {
            match("{");
            RULE_BODY();
            match("}");
        } else {
            RULE_STATEMENT();
        }
    }
    
    private void RULE_IF() {
        System.out.println("-- RULE_IF");
        match("if");
        match("(");
        RULE_EXPRESSION();
        match(")");
        if (tokens.get(currentToken).getValue().equals("{")) {
            match("{");
            RULE_BODY();
            match("}");
        } else {
            RULE_STATEMENT();
        }
        
        if (currentToken < tokens.size() && tokens.get(currentToken).getValue().equals("else")) {
            match("else");
            if (tokens.get(currentToken).getValue().equals("{")) {
                match("{");
                RULE_BODY();
                match("}");
            } else {
                RULE_STATEMENT();
            }
        }
    }
    
    private void RULE_RETURN() {
        System.out.println("-- RULE_RETURN");
        match("return");
        if (!tokens.get(currentToken).getValue().equals(";")) {
            RULE_EXPRESSION();
        }
        match(";");
    }
    
    private void RULE_CALL_METHOD() {
        System.out.println("-- RULE_CALL_METHOD");
        if (tokens.get(currentToken).getType().equals("IDENTIFIER")) currentToken++;
        match("(");
        if (!tokens.get(currentToken).getValue().equals(")")) {
            RULE_PARAM_VALUES();
        }
        match(")");
    }
    
    private void RULE_PARAM_VALUES() {
        System.out.println("-- RULE_PARAM_VALUES");
        RULE_EXPRESSION();
        while (currentToken < tokens.size() && tokens.get(currentToken).getValue().equals(",")) {
            currentToken++;
            RULE_EXPRESSION();
        }
    }
    
    private void RULE_DO_WHILE() {
        System.out.println("-- RULE_DO_WHILE");
        match("do");
        if (tokens.get(currentToken).getValue().equals("{")) {
            match("{");
            RULE_BODY();
            match("}");
        } else {
            RULE_STATEMENT();
        }
        match("while");
        match("(");
        RULE_EXPRESSION();
        match(")");
        match(";");
    }
    
    private void RULE_FOR() {
        System.out.println("-- RULE_FOR");
        match("for");
        match("(");
        
        // Inicialización (opcional)
        if (!tokens.get(currentToken).getValue().equals(";")) {
            if (isType(tokens.get(currentToken).getValue())) {
                RULE_TYPE();
                if (tokens.get(currentToken).getType().equals("IDENTIFIER")) currentToken++;
                if (tokens.get(currentToken).getValue().equals("=")) {
                    currentToken++;
                    RULE_EXPRESSION();
                }
            } else if (tokens.get(currentToken).getType().equals("IDENTIFIER")) {
                currentToken++;
                if (tokens.get(currentToken).getValue().equals("=")) {
                    currentToken++;
                    RULE_EXPRESSION();
                }
            }
        }
        match(";");
        
        // Condición (opcional)
        if (!tokens.get(currentToken).getValue().equals(";")) {
            RULE_EXPRESSION();
        }
        match(";");
        
        // Incremento (opcional)
        if (!tokens.get(currentToken).getValue().equals(")")) {
            if (tokens.get(currentToken).getType().equals("IDENTIFIER")) {
                currentToken++;
                if (tokens.get(currentToken).getValue().equals("=")) {
                    currentToken++;
                    RULE_EXPRESSION();
                }
            }
        }
        match(")");
        
        // Cuerpo
        if (tokens.get(currentToken).getValue().equals("{")) {
            match("{");
            RULE_BODY();
            match("}");
        } else {
            RULE_STATEMENT();
        }
    }
    
    private void RULE_SWITCH() {
        System.out.println("-- RULE_SWITCH");
        match("switch");
        match("(");
        RULE_EXPRESSION();
        match(")");
        match("{");
        
        // Procesar casos
        while (currentToken < tokens.size() && 
              (tokens.get(currentToken).getValue().equals("case") || 
               tokens.get(currentToken).getValue().equals("default"))) {
            if (tokens.get(currentToken).getValue().equals("case")) {
                match("case");
                // Avanzar hasta el :
                while (currentToken < tokens.size() && !tokens.get(currentToken).getValue().equals(":")) {
                    currentToken++;
                }
                match(":");
            } else { 
                match("default");
                match(":");
            }
            
            // Procesar el cuerpo del caso
            while (currentToken < tokens.size() && 
                  !tokens.get(currentToken).getValue().equals("case") && 
                  !tokens.get(currentToken).getValue().equals("default") && 
                  !tokens.get(currentToken).getValue().equals("}")) {
                RULE_STATEMENT();
            }
        }
        match("}");
    }
    
    // Para instrucciones individuales (if/while sin llaves)
    private void RULE_STATEMENT() {
        System.out.println("-- RULE_STATEMENT");
        if (isType(tokens.get(currentToken).getValue())) {
            RULE_VARIABLE();
        } else if (tokens.get(currentToken).getType().equals("IDENTIFIER")) {
            currentToken++;
            if (currentToken < tokens.size() && tokens.get(currentToken).getValue().equals("=")) {
                currentToken--; // Retroceder
                RULE_ASSIGNMENT();
            } else if (currentToken < tokens.size() && tokens.get(currentToken).getValue().equals("(")) {
                currentToken--; // Retroceder
                RULE_CALL_METHOD();
                match(";");
            }
        } else if (tokens.get(currentToken).getValue().equals("if")) {
            RULE_IF();
        } else if (tokens.get(currentToken).getValue().equals("while")) {
            RULE_WHILE();
        } else if (tokens.get(currentToken).getValue().equals("do")) {
            RULE_DO_WHILE();
        } else if (tokens.get(currentToken).getValue().equals("for")) {
            RULE_FOR();
        } else if (tokens.get(currentToken).getValue().equals("switch")) {
            RULE_SWITCH();
        } else if (tokens.get(currentToken).getValue().equals("return")) {
            RULE_RETURN();
        } else if (tokens.get(currentToken).getValue().equals("break") || 
                 tokens.get(currentToken).getValue().equals("continue")) {
            currentToken++;
            match(";");
        } else {
            currentToken++; // Avanzar si hay un token no reconocido
        }
    }
    
    // Método auxiliar para consumir un token esperado
    private void match(String expected) {
        if (currentToken < tokens.size() && tokens.get(currentToken).getValue().equals(expected)) {
            currentToken++;
            System.out.println("- " + expected);
        } else {
            error("Expected: " + expected);
        }
    }
    
    private void error(String message) {
        System.out.println("Error: " + message + " at token: " + 
            (currentToken < tokens.size() ? tokens.get(currentToken).getValue() : "End of input"));
        System.exit(1);
    }
}