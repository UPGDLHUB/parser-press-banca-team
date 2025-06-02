/**
 *
 * @author GABRIEL ZAID
 *         ISAC HUMBERTO
 *         BRANDON MAGANA
 * @version 0.5
 */

 import java.util.Hashtable;
 import java.util.Vector;
 
 public class SemanticAnalyzer {
     
     private static Vector<SymbolTableItem> globalSymbols = new Vector<>();
     private static Hashtable<String, Vector<SymbolTableItem>> localSymbols = new Hashtable<>();
     
     private static String currentScope = "global";
 
     public static class SymbolTableItem {
         private String id;
         private String type;
         private String scope;
         private Object value;
 
         public SymbolTableItem(String id, String type, String scope, Object value) {
             this.id = id;
             this.type = type;
             this.scope = scope;
             this.value = value;
         }
 
         public String getId() { return id; }
         public String getType() { return type; }
         public String getScope() { return scope; }
         public Object getValue() { return value; }
     }
 
     public static void setScope(String scope) {
         currentScope = (scope != null && !scope.isEmpty()) ? scope : "global";
     }
 
     public static Vector<String> getSymbolTableLines() {
         Vector<String> lines = new Vector<>();
 
         for (SymbolTableItem item : globalSymbols) {
             String valueStr = formatValue(item.getValue(), item.getType());
             if (item.getValue() == null) {
                 valueStr = "0";
             }
             lines.add(item.getId() + "," + item.getType() + ",global," + valueStr);
         }
 
         for (Vector<SymbolTableItem> items : localSymbols.values()) {
             for (SymbolTableItem item : items) {
                 String valueStr = formatValue(item.getValue(), item.getType());
                 if (item.getValue() == null) {
                     valueStr = "0";
                 }
                 lines.add(item.getId() + "," + item.getType() + "," + item.getScope() + "," + valueStr);
             }
         }
 
         return lines;
     }
 
     private static String formatValue(Object value, String type) {
         if (type.equalsIgnoreCase("boolean") || type.equalsIgnoreCase("bool")) {
             return value instanceof Boolean && ((Boolean) value) ? "1" : "0";
         }
         if (type.equalsIgnoreCase("string")) {
             return "\"" + value.toString() + "\"";
         }
         if (value instanceof Float) {
             Float f = (Float) value;
             return f % 1 == 0 ? String.valueOf(f.intValue()) : String.valueOf(f);
         }
         return value.toString().replace(",", "");
     }
 
     public static void CheckVariable(String type, String id) {
         Object defaultValue = getDefaultValue(type);
         SymbolTableItem item = new SymbolTableItem(id, type, currentScope, defaultValue);
 
         if ("global".equals(currentScope)) {
             for (SymbolTableItem globalItem : globalSymbols) {
                 if (globalItem.getId().equals(id)) {
                     error("Variable '" + id + "' ya está declarada en este ámbito.");
                     return;
                 }
             }
             globalSymbols.add(item);
         } else {
             String key = id + "@" + currentScope;
             Vector<SymbolTableItem> items = localSymbols.getOrDefault(key, new Vector<>());
 
             for (SymbolTableItem localItem : items) {
                 if (localItem.getId().equals(id)) {
                     error("Variable '" + id + "' ya está declarada en este ámbito.");
                     return;
                 }
             }
             items.add(item);
             localSymbols.put(key, items);
         }
     }
 
     private static Object getDefaultValue(String type) {
         switch (type.toLowerCase()) {
             case "int":
             case "integer":
                 return 0;
             case "boolean":
             case "bool":
                 return false;
             case "string":
                 return "";
             case "float":
             case "double":
                 return 0.0f;
             default:
                 return 0;
         }
     }
 
     public static void printSymbolTable() {
         System.out.println("=== SYMBOL TABLE (VALIDATION) ===");
         for (String line : getSymbolTableLines()) {
             String[] parts = line.split(",");
             if (parts.length != 4) {
                 System.err.println("Invalid symbol line: " + line);
             }
             System.out.println(line);
         }
     }
 
     public static boolean variableExists(String id) {
         String key = id + "@" + currentScope;
         if (localSymbols.containsKey(key)) {
             return true;
         }
 
         for (SymbolTableItem item : globalSymbols) {
             if (item.getId().equals(id)) return true;
         }
 
         return false;
     }
 
     public static String getVariableType(String id) {
         String key = id + "@" + currentScope;
         if (localSymbols.containsKey(key)) {
             for (SymbolTableItem item : localSymbols.get(key)) {
                 if (item.getId().equals(id)) return item.getType();
             }
         }
 
         for (SymbolTableItem item : globalSymbols) {
             if (item.getId().equals(id)) return item.getType();
         }
 
         return null;
     }
 
     private static Vector<String> semanticErrors = new Vector<>();
 
     public static void error(String message) {
         semanticErrors.add("[Semantic Error] " + message);
     }
 
     public static void printErrors() {
         if (semanticErrors.isEmpty()) return;
         System.out.println("=== SEMANTIC ERRORS ===");
         for (String err : semanticErrors) {
             System.out.println(err);
         }
     }
 
     public static String getCurrentScope() {
         return currentScope;
     }
 
 }
 