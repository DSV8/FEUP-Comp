package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;

import static pt.up.fe.comp2024.ast.Kind.CLASS_DECL;
import static pt.up.fe.comp2024.ast.Kind.IMPORT_DECL;
import static pt.up.fe.comp2024.ast.Kind.METHOD_DECL;
import static pt.up.fe.comp2024.ast.Kind.VAR_DECL;

public class JmmSymbolTableBuilder {


    public static JmmSymbolTable build(JmmNode root) {
        var imports = root.getChildren(IMPORT_DECL).stream()
                .map(importDecl -> importDecl.getObjectAsList("name", String.class))
                .map(ids -> String.join(".", ids))
                .toList();

        var classDecl = root.getChildren(CLASS_DECL).get(0);
        SpecsCheck.checkArgument(CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);

        String className = classDecl.get("name");

        String superName = classDecl.getOptional("ext").orElse(null);

        var fields = getFieldsList(classDecl);

        var methods = buildMethods(classDecl);

        var returnTypes = buildReturnTypes(classDecl);

        var params = buildParams(classDecl);

        var locals = buildLocals(classDecl);

        return new JmmSymbolTable(imports, superName, className, fields, methods, returnTypes, params, locals);
    }

    private static List<Symbol> getFieldsList(JmmNode classDecl) {
        List<Symbol> fields = new ArrayList<>();

        classDecl.getChildren(VAR_DECL).forEach(field ->
                fields.add(new Symbol(new Type(field.getChild(0).get("name"), field.getChild(0).getObject("isArray", Boolean.class)), field.get("name"))));

        return fields;
    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> map = new HashMap<>();

        classDecl.getChildren(METHOD_DECL)
                .forEach(method -> map.put(method.get("name"), new Type(method.getChild(0).get("name"), method.getChild(0).getObject("isArray", Boolean.class))));

        return map;
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();

        classDecl.getChildren(METHOD_DECL)
                .forEach(method -> {
                    var params = method.getChildren(Kind.PARAM).stream()
                            .map(param -> {
                                Object nameObj = param.getChild(0).getObject("name");
                                String name;
                                if (nameObj instanceof List) {
                                    name = String.join("", (List<String>) nameObj);
                                } else {
                                    name = (String) nameObj;
                                }
                                return new Symbol(new Type(name, param.getChild(0).getObject("isArray", Boolean.class)), param.get("name"));
                            })
                            .toList();
                    map.put(method.get("name"), params);
                });

        return map;
    }

    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        Map<String, List<Symbol>> map = new HashMap<>();

        classDecl.getChildren(METHOD_DECL)
                .forEach(method -> map.put(method.get("name"), getLocalsList(method)));

        return map;
    }

    private static List<String> buildMethods(JmmNode classDecl) {
        List<String> methodNames = new ArrayList<>();

        classDecl.getChildren(METHOD_DECL)
            .forEach(method -> methodNames.add(method.get("name")));

        return methodNames;
    }


    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        return methodDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> new Symbol(new Type(varDecl.getChild(0).get("name"), varDecl.getChild(0).getObject("isArray", Boolean.class)), varDecl.get("name")))
                .toList();
    }
}
