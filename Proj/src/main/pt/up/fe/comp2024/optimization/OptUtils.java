package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.Instruction;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static pt.up.fe.comp2024.ast.Kind.*;

public class OptUtils {
    private static int tempNumber = -1;
    private static int ifNumber = -1;
    private static int whileNumber = -1;

    public static String getTemp() {

        return getTemp("tmp");
    }

    public static String getTemp(String prefix) {

        return prefix + getNextTempNum();
    }

    public static String getIfBody() {

        return "ifbody_" + ifNumber;
    }

    public static String getEndIf() {

        return "endif_" + ifNumber;
    }

    public static String getWhileBody() {

        return "whilebody_" + ifNumber;
    }

    public static String getEndWhile() {

        return "endwhile_" + ifNumber;
    }

    public static int getNextTempNum() {

        tempNumber += 1;
        return tempNumber;
    }

    public static void getNextIfNum() {
        ifNumber += 1;
    }

    public static void getNextWhileNum() {
        ifNumber += 1;
    }

    public static String toOllirType(JmmNode typeNode) {

        if(!Kind.checkIsType(typeNode)) {
            throw new RuntimeException("Node '" + typeNode + "' is not a 'type'");
        }

        if(INT_VAR_ARGS_TYPE.check(typeNode)) {
            return ".array.i32";
        }

        String typeName = typeNode.get("name");

        var arrayPrefix = typeNode.getObject("isArray", Boolean.class) ? ".array" : "";
        var ollirType = OBJ_TYPE.check(typeNode) ? "." + typeName : toOllirType(typeName);
        return arrayPrefix + ollirType;
    }

    public static String toOllirType(Type type) {
        if (type.getName().equals("int...")) {
            return ".array.i32";
        }

        var arrayPrefix = type.isArray() ? ".array" : "";
        Set<String> typeNames = Set.of("int", "boolean", "String", "void");
        var ollirType = typeNames.contains(type.getName()) ? toOllirType(type.getName()) : "." + type.getName();
        return arrayPrefix + ollirType;
    }

    private static String toOllirType(String typeName) {

        String type = "." + switch (typeName) {
            case "int" -> "i32";
            case "boolean" -> "bool";
            case "String" -> "String";
            case "void" -> "V";
            default -> throw new NotImplementedException(typeName);
        };

        return type;
    }


}
