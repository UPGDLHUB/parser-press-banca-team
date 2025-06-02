import java.util.*;

/**
 *
 * @author GABRIEL ZAID
 *         ISAC HUMBERTO
 *         BRANDON MAGANA
 * @version 0.5
 */

public class TheParser {
    private Vector<TheToken> tokens;
    private int currentToken;
    private Map<String, Set<String>> firstSets = new HashMap<>();
    private Map<String, Set<String>> followSets = new HashMap<>();
    private CodeGenerator codeGen;

    public TheParser(Vector<TheToken> tokens) {
        this.tokens = tokens;
        currentToken = 0;
        this.codeGen = new CodeGenerator();
        initializeFirstSets();
        initializeFollowSets();
    }

    private void initializeFirstSets() {
        firstSets.put("RULE_PROGRAM", new HashSet<>(Arrays.asList("class")));
        firstSets.put("RULE_GLOBAL_ATTRIBUTE", new HashSet<>(Arrays.asList("int", "float", "double", "boolean", "String", "char")));
        firstSets.put("RULE_METHOD", new HashSet<>(Arrays.asList("void", "int", "float", "double", "boolean", "String", "char")));
    }

    private void initializeFollowSets() {
        followSets.put("RULE_PROGRAM", new HashSet<>(Collections.singletonList("$")));
        followSets.put("RULE_GLOBAL_ATTRIBUTE", new HashSet<>(Arrays.asList("int", "float", "double", "boolean", "String", "char", "void", "}")));
        followSets.put("RULE_METHOD", new HashSet<>(Arrays.asList("int", "float", "double", "boolean", "String", "char", "void", "}")));
    }

    private boolean checkFirst(String ruleName) {
        Set<String> first = firstSets.getOrDefault(ruleName, new HashSet<>());
        Set<String> follow = followSets.getOrDefault(ruleName, new HashSet<>());
    
        if (currentToken >= tokens.size()) {
            if (first.contains("$")) return true;
            reportError(ruleName + " expected end of input");
            return false;
        }
    
        String currentVal = tokens.get(currentToken).getValue();
        if (first.contains(currentVal)) return true;
    
        reportError("Expected one of " + first + " for " + ruleName + ", found " + currentVal);
        synchronize(ruleName, first, follow);
        return currentToken < tokens.size() && first.contains(tokens.get(currentToken).getValue());
    }
    
    private void synchronize(String ruleName, Set<String> first, Set<String> follow) {
        while (currentToken < tokens.size()) {
            String tokenVal = tokens.get(currentToken).getValue();
            if (first.contains(tokenVal) || follow.contains(tokenVal)) break;
            currentToken++;
        }
    }
    
    private void reportError(String message) {
        System.out.println("Error: " + message);
    }
    
    public void run() {
        RULE_PROGRAM();
    }

    public CodeGenerator getCodeGenerator() {
        return codeGen;
    }
    
    private void RULE_PROGRAM() {
        if (!checkFirst("RULE_PROGRAM")) return;
        match("class");
        if (tokens.get(currentToken).getType().equals("IDENTIFIER")) {
            currentToken++;
        }
        match("{");
        while (!tokens.get(currentToken).getValue().equals("}")) {
            if (isType(tokens.get(currentToken).getValue())) {
                int lookahead = currentToken + 1;
                if (lookahead < tokens.size() && tokens.get(lookahead).getType().equals("IDENTIFIER")) {
                    int lookahead2 = lookahead + 1;
                    if (lookahead2 < tokens.size() && tokens.get(lookahead2).getValue().equals("(")) {
                        RULE_METHOD();
                    } else {
                        RULE_GLOBAL_ATTRIBUTE();
                    }
                } else {
                    reportError("Expected method name identifier");
                    currentToken++;
                }
            }
            else {
                RULE_METHOD();
            }
        }
        match("}");
        codeGen.generateHalt();
    }
    
    private void RULE_GLOBAL_ATTRIBUTE() {
        if (!checkFirst("RULE_GLOBAL_ATTRIBUTE")) return;
        String type = tokens.get(currentToken).getValue();
        RULE_TYPE();
        if (tokens.get(currentToken).getType().equals("IDENTIFIER")) {
            String varName = tokens.get(currentToken).getValue();
            SemanticAnalyzer.CheckVariable(type, varName);
            codeGen.setLevel(0);
            codeGen.generateVariableDeclaration(type, varName, getDefaultValue(type));
            currentToken++;
        }
        match(";");
    }
    
    
    private void RULE_METHOD() {
        String returnType = "";
        if (tokens.get(currentToken).getValue().equals("void")) {
            returnType = "void";
            currentToken++;
        } else {
            returnType = tokens.get(currentToken).getValue();
            RULE_TYPE();
        }

        String methodName = "";
        if (tokens.get(currentToken).getType().equals("IDENTIFIER")) {
            methodName = tokens.get(currentToken).getValue();
            codeGen.addInstruction(methodName + ":");
            currentToken++;
        } else {
            reportError("Expected method name identifier");
        }

        SemanticAnalyzer.setScope(methodName);
        codeGen.setLevel(1);

        match("(");
        if (!tokens.get(currentToken).getValue().equals(")")) {
            RULE_PARAMS(); 
        }
        match(")");
        match("{");
        RULE_BODY();
        match("}");

        if (!returnType.equals("void")) {
            codeGen.generateLit("0");
        }
        codeGen.generateOpr(1); 
        SemanticAnalyzer.setScope("global");

    }


    private void RULE_TYPE() {
        if (isType(tokens.get(currentToken).getValue())) {
            currentToken++;
        }
    }
    
    public boolean isType(String word) {
    return word.equals("int") ||
           word.equals("float") ||
           word.equals("boolean") ||
           word.equals("char") ||
           word.equals("String") ||
           word.equals("void");
}

    
    private String getDefaultValue(String type) {
        if (type.equals("boolean")) return "0";
        if (type.equals("float") || type.equals("double")) return "0.0";
        if (type.equals("String")) return "\"\"";
        return "0";
    }
    
    private void RULE_PARAMS() {
        String paramType = tokens.get(currentToken).getValue();
        RULE_TYPE();
        if (tokens.get(currentToken).getType().equals("IDENTIFIER")) {
            String paramName = tokens.get(currentToken).getValue();
            codeGen.generateVariableDeclaration(paramType, paramName, getDefaultValue(paramType));
            currentToken++;
        }
        while (tokens.get(currentToken).getValue().equals(",")) {
            currentToken++;
            paramType = tokens.get(currentToken).getValue();
            RULE_TYPE();
            if (tokens.get(currentToken).getType().equals("IDENTIFIER")) {
                String paramName = tokens.get(currentToken).getValue();
                codeGen.generateVariableDeclaration(paramType, paramName, getDefaultValue(paramType));
                currentToken++;
            }
        }
    }
    
    private void RULE_BODY() {
    while (currentToken < tokens.size() && !tokens.get(currentToken).getValue().equals("}")) {

        if (isType(tokens.get(currentToken).getValue())) {
            RULE_VARIABLE();
        } else if (tokens.get(currentToken).getType().equals("IDENTIFIER")) {
            String identifier = tokens.get(currentToken).getValue();
            currentToken++;

            if (currentToken < tokens.size()) {
                String nextValue = tokens.get(currentToken).getValue();

                if (nextValue.equals("=")) {
                    currentToken--; 
                    RULE_ASSIGNMENT();
                } else if (nextValue.equals("(")) {
                    currentToken--; 
                    RULE_CALL_METHOD();
                    match(";");
                } else {
                    error("Unexpected token after identifier: '" + nextValue + "'");
                }
            }
        } else if (tokens.get(currentToken).getValue().equals("if")) {
            RULE_IF();
        } else if (tokens.get(currentToken).getValue().equals("while")) {
            RULE_WHILE();
        } else if (tokens.get(currentToken).getValue().equals("return")) {
            RULE_RETURN();
        } else if (tokens.get(currentToken).getValue().equals("print")) {
            RULE_PRINT();
        } else {
            error("Unexpected token: '" + tokens.get(currentToken).getValue() + "'");
            currentToken++;
        }
    }
}

    
    private void RULE_VARIABLE() {
        codeGen.setLevel(1);

        String type = tokens.get(currentToken).getValue();
        RULE_TYPE();
        if (tokens.get(currentToken).getType().equals("IDENTIFIER")) {
            String varName = tokens.get(currentToken).getValue();
            SemanticAnalyzer.CheckVariable(type, varName);
            
            if (currentToken + 1 < tokens.size() && tokens.get(currentToken + 1).getValue().equals("=")) {
                currentToken++; 
                match("=");
                RULE_EXPRESSION();
                codeGen.generateSto(varName);
            } else {
                codeGen.generateVariableDeclaration(type, varName, getDefaultValue(type));
                currentToken++;
            }
        } else {
            reportError("Expected variable name");
        }
        match(";");
    }

    
    private void RULE_ASSIGNMENT() {
        String varName = tokens.get(currentToken).getValue();
        currentToken++;
        match("=");
        RULE_EXPRESSION();
        codeGen.generateSto(varName);
        match(";");
    }
    


    private void RULE_EXPRESSION() {
        RULE_REL();
        while (currentToken < tokens.size() &&
            (tokens.get(currentToken).getValue().equals("&&") ||
                tokens.get(currentToken).getValue().equals("||"))) {

            String op = tokens.get(currentToken).getValue();
            currentToken++;
            RULE_REL(); 
            codeGen.generateOpr(op.equals("&&") ? 9 : 8);
        }
    }


    private void RULE_REL() {
        RULE_E();
        while (currentToken < tokens.size() &&
            (tokens.get(currentToken).getValue().equals(">") ||
            tokens.get(currentToken).getValue().equals("<") ||
            tokens.get(currentToken).getValue().equals(">=") ||
            tokens.get(currentToken).getValue().equals("<=") ||
            tokens.get(currentToken).getValue().equals("==") ||
            tokens.get(currentToken).getValue().equals("!="))) {

            String op = tokens.get(currentToken).getValue();
            currentToken++;
            RULE_E();

            switch (op) {
                case ">":  codeGen.generateOpr(15); break;
                case "<":  codeGen.generateOpr(16); break;
                case ">=": codeGen.generateOpr(21); break;
                case "<=": codeGen.generateOpr(22); break;
                case "==": codeGen.generateOpr(13); break;
                case "!=": codeGen.generateOpr(14); break;
            }
        }
    }
    private void RULE_E() {
        RULE_A();
        while (currentToken < tokens.size() && 
              (tokens.get(currentToken).getValue().equals("+") ||
                   tokens.get(currentToken).getValue().equals("-"))) {
            
            String operator = tokens.get(currentToken).getValue();
            currentToken++;
            RULE_A();
            
            if (operator.equals("+")) {
                codeGen.generateOpr(codeGen.ADD);
            } else if (operator.equals("-")) {
                codeGen.generateOpr(codeGen.SUB);
            }
        }
    }
    
    private void RULE_A() {
        RULE_B();
        while (currentToken < tokens.size() && 
              (tokens.get(currentToken).getValue().equals("*") ||
                   tokens.get(currentToken).getValue().equals("/"))) {
            String operator = tokens.get(currentToken).getValue();
            currentToken++;
            RULE_B();
            if (operator.equals("*")) {
                codeGen.generateOpr(4);
            } else {
                codeGen.generateOpr(5);
            }
        }
    }
    
    private void RULE_B() {
        if (currentToken < tokens.size() && tokens.get(currentToken).getValue().equals("-")) {
            currentToken++;
            RULE_C();
            codeGen.generateOpr(16);
        } else {
            RULE_C();
        }
    }
    
    private void RULE_C() {
        if (currentToken < tokens.size()) {
            if (tokens.get(currentToken).getType().equals("IDENTIFIER")) {
                String identifier = tokens.get(currentToken).getValue();
                currentToken++;
                if (currentToken < tokens.size() && tokens.get(currentToken).getValue().equals("(")) {
                    currentToken--; // Retroceder
                    RULE_CALL_METHOD();
                } else {
                    if (!SemanticAnalyzer.variableExists(identifier)) {
                        SemanticAnalyzer.error("Variable '" + identifier + "' not declared in scope '" + SemanticAnalyzer.getCurrentScope() + "'");
                    }
                    codeGen.generateLod(identifier);

                }
            } else if (tokens.get(currentToken).getType().equals("INTEGER") || 
                       tokens.get(currentToken).getType().equals("FLOAT")) {
                codeGen.generateLit(tokens.get(currentToken).getValue());
                currentToken++;
            } else if (tokens.get(currentToken).getType().equals("STRING")) {
                codeGen.generateLit(tokens.get(currentToken).getValue());
                currentToken++;
            }
            else if (tokens.get(currentToken).getValue().equals("true")) {
                codeGen.generateLit("1");
                currentToken++;
            } else if (tokens.get(currentToken).getValue().equals("false")) {
                codeGen.generateLit("0");
                currentToken++;
            } else if (tokens.get(currentToken).getValue().equals("(")) {
                currentToken++;
                RULE_EXPRESSION();
                match(")");
            }
        }
    }
    
    private void RULE_WHILE() {
    match("while");
    match("(");
    codeGen.generateWhileStart(); 
    RULE_EXPRESSION();          
    codeGen.generateWhileCondition(); 
    match(")");
    match("{");
    RULE_BODY(); 
    match("}");
    codeGen.generateWhileEnd();
}

    
    private void RULE_IF() {
        match("if");
        match("(");
        RULE_EXPRESSION();
        match(")");
        match("{");
        codeGen.generateIfStart();
        while (!tokens.get(currentToken).getValue().equals("}")) {
            RULE_BODY();
        }
        match("}");
        if (currentToken < tokens.size() && tokens.get(currentToken).getValue().equals("else")) {
            codeGen.generateElseStart();
            match("else");
            match("{");
            while (!tokens.get(currentToken).getValue().equals("}")) {
                RULE_BODY();
            }
            match("}");
            codeGen.generateElseEnd();
        } else {
            codeGen.generateIfEnd();
        }
    }

    

    private void RULE_RETURN() {
        match("return");
        if (!tokens.get(currentToken).getValue().equals(";")) {
            RULE_EXPRESSION();
        }
        codeGen.generateReturn();
        match(";");
    }
    
    private void RULE_CALL_METHOD() {
        String methodName = "";
        if (tokens.get(currentToken).getType().equals("IDENTIFIER")) {
            methodName = tokens.get(currentToken).getValue();
            currentToken++;
        }
        match("(");
        if (!tokens.get(currentToken).getValue().equals(")")) {
            RULE_PARAM_VALUES();
        }
        match(")");
        codeGen.addInstruction("call " + methodName);
    }
    
    private void RULE_PARAM_VALUES() {
        RULE_EXPRESSION();
        while (tokens.get(currentToken).getValue().equals(",")) {
            currentToken++;
            RULE_EXPRESSION();
        }
    }
    
    private void RULE_PRINT() {
        match("print");
        match("(");
        RULE_EXPRESSION();
        codeGen.generateOpr(21);
        match(")");
        match(";");
    }
    
    private void match(String expected) {
        if (currentToken < tokens.size() && tokens.get(currentToken).getValue().equals(expected)) {
            currentToken++;
        } else {
            reportError("Expected '" + expected + "' but found '" + 
                       (currentToken < tokens.size() ? tokens.get(currentToken).getValue() : "EOF") + "'");
        }
    }
    
    private void error(String errorCode) {
        reportError("Syntax error: " + errorCode);
    }
}