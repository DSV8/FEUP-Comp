package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp.jmm.ast.antlr.JmmNodeCleanup;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    @Override
    protected void buildVisitor() {
        addVisit(MEMBER_METHOD_ACCESS, this::visitMemberMethodAccess);
        addVisit(ARRAY_ACCESS, this::visitArrayAccess);
        addVisit(PARENTHESES, this::visitParentheses);
        addVisit(NEGATION, this::visitNegation);
        addVisit(NEW_ARRAY_EXPR, this::visitNewArrayExpr);
        addVisit(NEW_OBJ_EXPR, this::visitNewObjExpr);
        addVisit(ARITHMETIC_EXPR, this::visitBinExpr);
        addVisit(RELATIONAL_EXPR, this::visitBinExpr);
        addVisit(LOGICAL_EXPR, this::visitLogicalExpr);
        addVisit(ARRAY_INIT_EXPR, this::visitArrayInit);
        addVisit(BOOL_LITERAL, this::visitLiteral);
        addVisit(INTEGER_LITERAL, this::visitLiteral);
        addVisit(IDENTIFIER, this::visitVarRef);
        addVisit(THIS, this::visitVarRef);

        setDefaultVisit(this::defaultVisit);
    }

    private OllirExprResult visitLiteral(JmmNode node, Void unused) {
        if (!node.hasAttribute("type")) {
            node.putObject("type", TypeUtils.getExprType(node, table, node.getAncestor(METHOD_DECL).get().get("name")));
        }
        Type type = node.getObject("type", Type.class);

        String ollirType = OptUtils.toOllirType(type);
        String code = node.get("value") + ollirType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitVarRef(JmmNode node, Void unused) {

        var id = node.get("value");
        if (!node.hasAttribute("type")) {
            node.putObject("type", TypeUtils.getExprType(node, table, node.getAncestor(METHOD_DECL).get().get("name")));
        }
        Type type = node.getObject("type", Type.class);
        String ollirType = OptUtils.toOllirType(type);
        String code;
        StringBuilder computation = new StringBuilder();

        String methodName = node.getAncestor(METHOD_DECL).get().get("name");
        boolean varIsField = table.getFields().stream().anyMatch(f -> f.getName().equals(id));
        boolean varIsLocal = table.getLocalVariables(methodName).stream().anyMatch(f -> f.getName().equals(id));
        boolean varIsParam = table.getParameters(methodName).stream().anyMatch(f -> f.getName().equals(id));

        if (varIsField && !varIsParam && !varIsLocal) {
            code = OptUtils.getTemp() + ollirType;
            computation.append(code).append(SPACE).append(ASSIGN).append(ollirType).append(SPACE)
                    .append("getfield(this.").append(table.getClassName()).append(", ").append(id)
                    .append(ollirType).append(")").append(ollirType).append(END_STMT);
        } else {
            var paramSymbol = "";
            var currentMethod = node.getAncestor(METHOD_DECL);
            if (currentMethod.isPresent()) {
                var currentMethodIsStatic = currentMethod.get().getObject("isStatic", Boolean.class);
                var currentMethodParameterNames = table.getParameters(currentMethod.get().get("name")).stream().map(Symbol::getName).toList();

                if (currentMethodParameterNames.contains(id)) {
                    paramSymbol += "$";
                    var parameterIdx = currentMethodParameterNames.indexOf(id);
                    paramSymbol += (currentMethodIsStatic ? parameterIdx : parameterIdx + 1) + ".";
                }
            }

            code = paramSymbol + id + ollirType;
        }

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitArrayInit(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();

        String temp = OptUtils.getTemp();
        String code = temp + ".array.i32";
        computation.append(code).append(" :=.array.i32 new(array, ").append(node.getNumChildren()).append(".i32).array.i32;\n");

        for (int i = 0; i < node.getNumChildren(); i++) {
            var arrayElem = visit(node.getChild(i));
            computation.append(arrayElem.getComputation());
            computation.append(temp).append("[").append(i).append(".i32].i32 :=.i32 ").append(arrayElem.getCode()).append(END_STMT);
        }

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitLogicalExpr(JmmNode node, Void unused) {
        String temp = OptUtils.getTemp();

        OllirExprResult leftOperandRes = visit(node.getChild(0));
        OllirExprResult rightOperandRes = visit(node.getChild(1));

        StringBuilder computation = new StringBuilder();

        computation.append(leftOperandRes.getComputation());

        String trueCode = rightOperandRes.getComputation() + temp + ".bool " + ASSIGN + ".bool " + rightOperandRes.getCode() + END_STMT;
        String falseCode = temp + ".bool " + ASSIGN + ".bool false.bool" + END_STMT;

        OptUtils.getNextIfNum();

        computation.append("if (").append(leftOperandRes.getCode()).append(") goto ").append(OptUtils.getIfBody()).append(END_STMT);
        computation.append(falseCode);
        computation.append("goto ").append(OptUtils.getEndIf()).append(END_STMT);

        computation.append(OptUtils.getIfBody()).append(":\n");
        computation.append(trueCode);
        computation.append(OptUtils.getEndIf()).append(":\n");

        String code = temp + ".bool";

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        var lhs = visit(node.getChild(0));
        var rhs = visit(node.getChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        if (!node.hasAttribute("type")) {
            node.putObject("type", TypeUtils.getExprType(node, table, node.getAncestor(METHOD_DECL).get().get("name")));
        }
        Type resType = node.getObject("type", Type.class);
        String resOllirType = OptUtils.toOllirType(resType);
        StringBuilder code = new StringBuilder();

        if (ASSIGN_STMT.check(node.getParent()) || (RELATIONAL_EXPR.check(node) && (IF_ELSE_STMT.check(node.getParent()) || WHILE_STMT.check(node.getParent())))) {
            code.append(lhs.getCode()).append(SPACE).append(node.get("op")).append(OptUtils.toOllirType(resType)).append(SPACE)
                    .append(rhs.getCode());
        } else {
            code.append(OptUtils.getTemp()).append(resOllirType);
            computation.append(code).append(SPACE)
                    .append(ASSIGN).append(resOllirType).append(SPACE)
                    .append(lhs.getCode()).append(SPACE).append(node.get("op"))
                    .append(OptUtils.toOllirType(resType)).append(SPACE)
                    .append(rhs.getCode()).append(END_STMT);
        }

        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitNewObjExpr(JmmNode node, Void unused) {

        StringBuilder computation = new StringBuilder();

        // code to compute self
        if (!node.hasAttribute("type")) {
            node.putObject("type", TypeUtils.getExprType(node, table, node.getAncestor(METHOD_DECL).get().get("name")));
        }
        Type resType = node.getObject("type", Type.class);
        String resOllirType = "." + resType.getName();
        String code = "";

        if (ASSIGN_STMT.check(node.getParent())) {
            code += "new(" + resType.getName() + ")" + resOllirType + END_STMT;
            String caller = node.getParent().getChild(0).get("value");
            code += "invokespecial(" + caller + resOllirType + ", \"<init>\").V";
        } else {
            code += OptUtils.getTemp() + resOllirType;
            computation.append(code).append(SPACE)
                    .append(ASSIGN).append(resOllirType).append(SPACE)
                    .append("new(").append(resType.getName()).append(")").append(resOllirType).append(END_STMT);
            computation.append("invokespecial(").append(code).append(", \"<init>\").V").append(END_STMT);
        }

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitNewArrayExpr(JmmNode node, Void unused) {
        OllirExprResult index = visit(node.getChild(0));

        StringBuilder computation = new StringBuilder();

        computation.append(index.getComputation());

        if (!node.hasAttribute("type")) {
            node.putObject("type", TypeUtils.getExprType(node, table, node.getAncestor(METHOD_DECL).get().get("name")));
        }
        Type arrayType = node.getObject("type", Type.class);
        String ollirArrayType = OptUtils.toOllirType(arrayType);
        StringBuilder code = new StringBuilder();

        if (ASSIGN_STMT.check(node.getParent())) {
            code.append(ollirArrayType).append(SPACE)
                    .append("new(array, ").append(index.getCode()).append(")").append(ollirArrayType);
        } else {
            code.append(OptUtils.getTemp()).append(ollirArrayType);
            computation.append(code).append(SPACE)
                    .append(ASSIGN).append(ollirArrayType).append(SPACE)
                    .append("new(array, ").append(index.getCode()).append(")").append(ollirArrayType).append(END_STMT);
        }

        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitNegation(JmmNode node, Void unused) {

        var negated = visit(node.getChild(0));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(negated.getComputation());

        // code to compute self
        if (!node.hasAttribute("type")) {
            node.putObject("type", TypeUtils.getExprType(node, table, node.getAncestor(METHOD_DECL).get().get("name")));
        }
        Type resType = node.getObject("type", Type.class);
        String resOllirType = OptUtils.toOllirType(resType);
        StringBuilder code = new StringBuilder();

        if (ASSIGN_STMT.check(node.getParent())) {
            code.append(node.get("op")).append(resOllirType).append(SPACE)
                    .append(negated.getCode());
        } else {
            code.append(OptUtils.getTemp()).append(resOllirType);
            computation.append(code).append(SPACE)
                    .append(ASSIGN).append(resOllirType).append(SPACE)
                    .append(node.get("op")).append(resOllirType).append(SPACE)
                    .append(negated.getCode()).append(END_STMT);
        }

        return new OllirExprResult(code.toString(), computation);
    }

    private OllirExprResult visitParentheses(JmmNode node, Void unused) {
        return visit(node.getChild(0));
    }

    private OllirExprResult visitArrayAccess(JmmNode node, Void unused) {
        OllirExprResult arrayIdResult = visit(node.getChild(0));
        String[] arrayIdParts = arrayIdResult.getCode().split("\\.");
        OllirExprResult accessExpr = visit(node.getChild(1));

        StringBuilder computation = new StringBuilder();

        computation.append(accessExpr.getComputation());

        arrayIdParts[arrayIdParts.length - 3] = arrayIdParts[arrayIdParts.length - 3] + "[" + accessExpr.getCode() + "]";
        arrayIdParts[arrayIdParts.length - 2] = arrayIdParts[arrayIdParts.length - 1];
        String arrayAccessCode = Arrays.stream(arrayIdParts, 0, arrayIdParts.length - 1)
                .collect(Collectors.joining("."));

        String ancestorMethod = node.getAncestor(METHOD_DECL).get().get("name");
        if (!node.hasAttribute("type")) {
            node.putObject("type", TypeUtils.getExprType(node, table, ancestorMethod));
        }
        Type resType = node.getObject("type", Type.class);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = "";

        if (ASSIGN_STMT.check(node.getParent())) {
            code += arrayAccessCode;
        } else {
            code += OptUtils.getTemp() + resOllirType;
            computation.append(code).append(SPACE).append(ASSIGN).append(resOllirType).append(SPACE)
                    .append(arrayAccessCode).append(END_STMT);
        }

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitMemberMethodAccess(JmmNode node, Void unused) {

        var ancestorMethod = node.getAncestor(METHOD_DECL).get().get("name");
        StringBuilder computation = new StringBuilder();

        var callerNode = node.getChild(0);
        var caller = visit(callerNode);
        var callerCode = "";
        if (NEW_OBJ_EXPR.check(callerNode) || table.getParameters(ancestorMethod).stream().map(Symbol::getName).toList().contains(callerNode.get("value")) || table.getLocalVariables(ancestorMethod).stream().map(Symbol::getName).toList().contains(callerNode.get("value"))) {
            callerCode = caller.getCode();
        } else {
            callerCode = callerNode.get("value");
        }
        computation.append(caller.getComputation());

        String methodName = node.get("method");

        String code;

        if ("length".equals(methodName)) {
            code = OptUtils.getTemp() + ".i32";
            computation.append(code).append(SPACE).append(ASSIGN)
                    .append(".i32").append(SPACE).append("arraylength(").append(callerCode)
                    .append(").i32").append(END_STMT);
        } else {
            List<Symbol> params = table.getParameters(methodName);
            StringBuilder arguments = new StringBuilder();
            for (int i = 1; i < params.size(); i++) {
                var arg = visit(node.getChild(i));
                computation.append(arg.getComputation());
                arguments.append(", ").append(arg.getCode());
            }

            boolean methodHasVarargs = !params.isEmpty() && params.getLast().getType().getName().equals("int...");
            boolean lastArgIsArray = false;

            if ((node.getNumChildren() - 1) > 0) {
                var lastArgNode = node.getChildren().getLast();
                if (!lastArgNode.hasAttribute("type")) {
                    lastArgNode.putObject("type", TypeUtils.getExprType(lastArgNode, table, ancestorMethod));
                }
                lastArgIsArray = lastArgNode.getObject("type", Type.class).isArray();

                if (methodHasVarargs && !lastArgIsArray) {
                    // Initialize new temporary array
                    String temp = OptUtils.getTemp();
                    int numVarargNumbers = (node.getNumChildren() - 1) - (params.size() - 1);
                    computation.append(temp).append(".array.i32 :=.array.i32 new(array, ").append(numVarargNumbers).append(".i32).array.i32;\n");

                    for (int i = 0; i < numVarargNumbers; i++) {
                        var varArg = visit(node.getChild(params.size() + i));
                        computation.append(varArg.getComputation());
                        computation.append(temp).append("[").append(i).append(".i32].i32 :=.i32 ").append(varArg.getCode()).append(END_STMT);
                    }

                    arguments.append(", ").append(temp).append(".array.i32");
                } else {
                    var lastArg = visit(node.getChild(node.getNumChildren() - 1));
                    computation.append(lastArg.getComputation());
                    arguments.append(", ").append(lastArg.getCode());
                }
            }

            boolean callerIsClass = false;

            if (callerCode.equals(table.getClassName()))
                callerIsClass = true;
            else {
                for (String importStr : table.getImports()) {
                    String[] parts = importStr.split("\\.");
                    String className = parts[parts.length - 1];
                    if (className.equals(callerCode)) {
                        callerIsClass = true;
                        break;
                    }
                }
            }

            String methodType = callerIsClass ? "invokestatic" : "invokevirtual";

            // code to compute self
            if (!node.hasAttribute("type")) {
                node.putObject("type", TypeUtils.getExprType(node, table, ancestorMethod));
            }
            Type resType = node.getObject("type", Type.class);
            String resOllirType = OptUtils.toOllirType(resType);
            String tempCode = OptUtils.getTemp() + resOllirType;
            code = "";

            if (EXPR_STMT.check(node.getParent())) {
                if (methodType.equals("invokevirtual")) {
                    code += tempCode + SPACE + ASSIGN + resOllirType + SPACE;
                }

                code += methodType + "(" + callerCode + ", \"" + methodName + "\"" + arguments + ")" + resOllirType + END_STMT;
            } else {
                code = tempCode;
                computation.append(code).append(SPACE)
                        .append(ASSIGN).append(resOllirType).append(SPACE);

                computation.append(methodType).append("(")
                        .append(callerCode).append(", ").append("\"").append(methodName)
                        .append("\"").append(arguments).append(")").append(resOllirType).append(END_STMT);
            }
        }

        return new OllirExprResult(code, computation);
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

}
