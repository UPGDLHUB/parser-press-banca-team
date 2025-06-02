import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;
/**
 *
 * @author GABRIEL ZAID
 *         ISAC HUMBERTO
 *         BRANDON MAGANA
 * @version 0.5
 */

public class TheMain {
    
    public static void main(String[] args) throws IOException {
        System.out.println("=== Java Subset Compiler ===\n");


        File file = new File("src/main/resources/input.txt");
        if (!file.exists()) {
            System.err.println("Error: input.txt not found.");
            return;
        }


        System.out.println("STEP 1: LEXICAL ANALYSIS");
        TheLexer lexer = new TheLexer(file);
        lexer.run();
        Vector<TheToken> tokens = lexer.getTokens();
        System.out.println("Tokens found: " + tokens.size());
        lexer.printTokens();

        System.out.println("\n" + "=".repeat(50) + "\n");


        System.out.println("STEP 2: SYNTAX ANALYSIS & CODE GENERATION");
        TheParser parser = new TheParser(tokens);
        parser.run();
        CodeGenerator codeGen = parser.getCodeGenerator();

        System.out.println("\n" + "=".repeat(50) + "\n");


        System.out.println("STEP 3: SYMBOL TABLE");
        SemanticAnalyzer.printSymbolTable();
        SemanticAnalyzer.printErrors();
        System.out.println("\n" + "=".repeat(50) + "\n");


        System.out.println("STEP 4: GENERATED CODE");

        Vector<String> validInstructions = new Vector<>();
        for (String inst : codeGen.getInstructions()) {
            if (inst.startsWith("lit ") || 
                inst.startsWith("sto ") || 
                inst.startsWith("lod ") || 
                inst.startsWith("opr ")) {
                validInstructions.add(inst);
            }
        }
        for (String line : SemanticAnalyzer.getSymbolTableLines()) {
            System.out.println(line);
        }
        System.out.println("@");
        for (String instruction : validInstructions) {
            System.out.println(instruction);
        }

        System.out.println("\n" + "=".repeat(50) + "\n");


        System.out.println("STEP 5: SAVING OUTPUT TO generated_code.vm");
        saveOutput("generated_code.vm", codeGen);

        System.out.println("Compilation completed successfully!");
    }

    private static void saveOutput(String filename, CodeGenerator codeGen) {
        try (FileWriter writer = new FileWriter(filename)) {

            Vector<String> symbolTableLines = SemanticAnalyzer.getSymbolTableLines();
            for (String line : symbolTableLines) {
                writer.write(line + "\n");
            }


            writer.write("@\n");
            for (String instruction : codeGen.getInstructions()) {
                writer.write(instruction + "\n");
            }
        } catch (IOException e) {
            System.err.println("Error saving file: " + e.getMessage());
        }
    }
}
