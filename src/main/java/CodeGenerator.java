/**
 *
 * @author GABRIEL ZAID
 *         ISAC HUMBERTO
 *         BRANDON MAGANA
 * @version 0.5
 */

 import java.util.Vector;
 import java.util.HashMap;
 import java.util.Map;
 import java.util.Stack;
 
 public class CodeGenerator {
 
     private Vector<String> instructions = new Vector<>();
     private int currentLevel = 0;
 
     static final int ADD = 2;
     static final int SUB = 3;
     static final int MUL = 4;
     static final int DIV = 5;
     static final int PRINT = 21;
     static final int HALT = 0;
     static final int RETURN = 1;
     static final int GT = 11;
     static final int LT = 12;
 
     public void setLevel(int level) {
         currentLevel = level;
     }
 
     public void addInstruction(String instruction) {
         if (instruction != null && !instruction.trim().isEmpty()) {
             instructions.add(instruction);
         }
     }
 
     private int labelCount = 0;
 
     public String getNextLabel() {
         return "L" + (labelCount++);
     }
 
     public void generateLit(String value) {
         instructions.add("lit " + value + "," + currentLevel);
     }
 
     public void generateSto(String varName) {
         instructions.add("sto " + varName + "," + currentLevel);
     }
 
     public void generateLod(String varName) {
         instructions.add("lod " + varName + "," + currentLevel);
     }
 
     public void generateOpr(int operationCode) {
         instructions.add("opr " + operationCode + "," + currentLevel);
     }
 
     public void generateVariableDeclaration(String type, String varName, String defaultValue) {
         generateLit(defaultValue);
         generateSto(varName);
     }
 
     public void generatePrint(String value) {
         if (isNumeric(value) || value.equals("true") || value.equals("false")) {
             generateLit(value);
         } else {
             generateLod(value);
         }
         generateOpr(PRINT);
     }
 
     public void generateHalt() {
         generateOpr(HALT);
     }
 
     public void generateReturn() {
         generateOpr(RETURN);
     }
 
     public void generateComparison(String operator) {
         switch (operator) {
             case ">": generateOpr(GT); break;
             case "<": generateOpr(LT); break;
             case "==": generateOpr(15); break;
         }
     }
 
     public Vector<String> getInstructions() {
         return instructions;
     }
 
     public void printInstructions() {
         System.out.println("@");
         for (String instruction : instructions) {
             System.out.println(instruction);
         }
     }
 
     public void saveToFile(String filename) {
         try (java.io.FileWriter writer = new java.io.FileWriter(filename)) {
             writer.write("@\n");
             for (String instruction : instructions) {
                 writer.write(instruction + "\n");
             }
             System.out.println("Code saved to: " + filename);
         } catch (java.io.IOException e) {
             System.err.println("Error saving file: " + e.getMessage());
         }
     }
 
     private boolean isNumeric(String str) {
         try {
             Double.parseDouble(str);
             return true;
         } catch (NumberFormatException e) {
             return false;
         }
     }
 
     private Stack<String[]> labelStack = new Stack<>();
 
     public void generateIfStart() {
         String elseLabel = "ELSE_" + labelCount;
         String endLabel = "ENDIF_" + labelCount;
         instructions.add("jpc " + elseLabel);
         labelStack.push(new String[]{elseLabel, endLabel});
     }
 
     public void generateElseStart() {
         String[] labels = labelStack.peek();
         instructions.add("jmp " + labels[1]);
         instructions.add(labels[0] + ":");
     }
 
     public void generateElseEnd() {
         String[] labels = labelStack.pop();
         instructions.add(labels[1] + ":");
     }
 
     public void generateIfEnd() {
         String[] labels = labelStack.pop();
         instructions.add(labels[0] + ":");
         instructions.add(labels[1] + ":");
     }
 
     public void generateWhileStart() {
         String startLabel = "WHILE_START_" + labelCount;
         String endLabel = "WHILE_END_" + labelCount;
         labelCount++;
         labelStack.push(new String[]{startLabel, endLabel});
         instructions.add(startLabel + ":");
     }
 
     public void generateWhileCondition() {
         String[] labels = labelStack.peek();
         instructions.add("jpc " + labels[1]);
     }
 
     public void generateWhileEnd() {
         String[] labels = labelStack.pop();
         instructions.add("jmp " + labels[0]);
         instructions.add(labels[1] + ":");
     }
 
 }
 