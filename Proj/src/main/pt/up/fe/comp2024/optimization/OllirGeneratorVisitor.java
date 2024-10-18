package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.List;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";


    private final SymbolTable table;

    private final OllirExprGeneratorVisitor exprVisitor;

    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM, this::visitProgram);
        addVisit(IMPORT_DECL, this::visitImportDecl);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(VAR_DECL, this::visitFieldDecl);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);
        addVisit(BLOCK_STMT, this::visitBlockStmt);
        addVisit(IF_ELSE_STMT, this::visitIfElseStmt);
        addVisit(WHILE_STMT, this::visitWhileStmt);
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(EXPR_STMT, this::visitExprStmt);

        setDefaultVisit(this::defaultVisit);
    }

    private String visitExprStmt(JmmNode node, Void unused) {

        var expr = exprVisitor.visit(node.getChild(0));

        return expr.getComputation() +
                expr.getCode();
    }

    private String visitAssignStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        var rhs = exprVisitor.visit(node.getChild(1));

        // code to compute the children
        code.append(rhs.getComputation());

        // code to compute self
        // statement has type of lhs
        Type thisType = node.getChild(0).getObject("type", Type.class);
        String typeString = OptUtils.toOllirType(thisType);

        if (IDENTIFIER.check(node.getChild(0))) {
            String varName = node.getChild(0).get("value");
            String methodName = node.getAncestor(METHOD_DECL).get().get("name");
            boolean varIsField = table.getFields().stream().anyMatch(f -> f.getName().equals(varName));
            boolean varIsLocal = table.getLocalVariables(methodName).stream().anyMatch(f -> f.getName().equals(varName));
            boolean varIsParam = table.getParameters(methodName).stream().anyMatch(f -> f.getName().equals(varName));

            if (varIsField && !varIsLocal && !varIsParam) {
                code.append("putfield(this.").append(table.getClassName()).append(", ").append(varName).append(typeString)
                        .append(", ").append(rhs.getCode()).append(").V").append(END_STMT);

                return code.toString();
            }
        }

        var lhs = exprVisitor.visit(node.getChild(0));
        code.append(lhs.getComputation());

        code.append(lhs.getCode());
        code.append(SPACE);

        code.append(ASSIGN);
        code.append(typeString);
        code.append(SPACE);

        code.append(rhs.getCode());

        code.append(END_STMT);

        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {

        String methodName = node.getAncestor(METHOD_DECL).map(method -> method.get("name")).orElseThrow();
        Type retType = table.getReturnType(methodName);

        StringBuilder code = new StringBuilder();

        var expr = OllirExprResult.EMPTY;

        if (node.getNumChildren() > 0) {
            expr = exprVisitor.visit(node.getChild(0));
        }

        code.append(expr.getComputation());
        code.append("ret");
        code.append(OptUtils.toOllirType(retType));
        code.append(SPACE);

        code.append(expr.getCode());

        code.append(END_STMT);

        return code.toString();
    }

    private String visitWhileStmt(JmmNode node, Void unused) {
        var condExpr = exprVisitor.visit(node.getChild(0));

        StringBuilder code = new StringBuilder();

        // code to compute the children
        code.append(condExpr.getComputation());

        var bodyCode = visit(node.getChild(1));

        OptUtils.getNextWhileNum();

        var condString = "if (" + condExpr.getCode() + ") goto " + OptUtils.getWhileBody() + END_STMT;

        code.append(condString);
        code.append("goto ").append(OptUtils.getEndWhile()).append(END_STMT);

        code.append(OptUtils.getWhileBody()).append(":").append(NL);
        code.append(bodyCode);
        code.append(condString);
        code.append(OptUtils.getEndWhile()).append(":").append(NL);

        return code.toString();
    }

    private String visitIfElseStmt(JmmNode node, Void unused) {
        var condExpr = exprVisitor.visit(node.getChild(0));

        StringBuilder code = new StringBuilder();

        // code to compute the children
        code.append(condExpr.getComputation());

        var ifCode = visit(node.getChild(1));
        var elseCode = visit(node.getChild(2));

        OptUtils.getNextIfNum();

        code.append("if (").append(condExpr.getCode()).append(") goto ").append(OptUtils.getIfBody()).append(END_STMT);
        code.append(elseCode);
        code.append("goto ").append(OptUtils.getEndIf()).append(END_STMT);

        code.append(OptUtils.getIfBody()).append(":").append(NL);
        code.append(ifCode);
        code.append(OptUtils.getEndIf()).append(":").append(NL);

        return code.toString();
    }

    private String visitBlockStmt(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        for (var statement : node.getChildren()) {
            var stmtCode = visit(statement);
            code.append(stmtCode);
        }

        return code.toString();
    }

    private String visitParam(JmmNode node, Void unused) {

        var typeCode = OptUtils.toOllirType(node.getChild(0));
        var paramName = node.get("name");

        return paramName + typeCode;
    }


    private String visitMethodDecl(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder(".method ");

        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");

        if (isPublic) {
            code.append("public ");
        }

        boolean isStatic = NodeUtils.getBooleanAttribute(node, "isStatic", "false");

        if (isStatic) {
            code.append("static ");
        }

        List<Symbol> params = table.getParameters(node.get("name"));
        boolean hasVarargs = !params.isEmpty() && params.getLast().getType().getName().equals("int...");

        if (hasVarargs) {
            code.append("varargs ");
        }

        // name
        var name = node.get("name");
        code.append(name);

        // params
        code.append("(");
        var paramNodes  = node.getChildren(PARAM);
        for (int i = 0; i < paramNodes.size(); i++) {
            var paramCode = visit(paramNodes.get(i));
            code.append(paramCode);

            var after = i == paramNodes.size() - 1 ? "" : ", ";
            code.append(after);
        }
        code.append(")");

        // type
        var retType = OptUtils.toOllirType(node.getChild(0));
        code.append(retType);
        code.append(L_BRACKET);

        var numParams = table.getParameters(name).size();
        var numLocals = table.getLocalVariables(name).size();
        // rest of its children stmts
        for (int i = (1 + numParams + numLocals); i < node.getChildren().size(); i++) {
            var childStmt = node.getChild(i);
            var childCode = visit(childStmt);
            code.append(childCode);
        }

        if (retType.equals(".V")) {
            code.append("ret.V").append(END_STMT);
        }

        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }

    private String visitFieldDecl(JmmNode node, Void unused) {
        var typeCode = OptUtils.toOllirType(node.getChild(0));
        var fieldName = node.get("name");

        return ".field public " + fieldName + typeCode + ";\n";
    }

    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(table.getClassName());
        var superClassName = table.getSuper();
        if (superClassName != null) {
            code.append(" extends ");
            code.append(superClassName);
        }
        code.append(L_BRACKET);

        var needConstructor = true;

        for (var child : node.getChildren()) {
            var result = visit(child);

            if (METHOD_DECL.check(child) && needConstructor) {
                if (VAR_DECL.check(child.getParent())) {
                    code.append(NL);
                }
                code.append(buildConstructor()).append(NL);
                needConstructor = false;
            }

            code.append(result);
        }

        code.deleteCharAt(code.length() - 1);
        code.append(R_BRACKET);

        return code.toString();
    }

    private String buildConstructor() {

        return ".construct " + table.getClassName() + "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}\n";
    }

    private String visitImportDecl(JmmNode node, Void unused) {

        var importName = String.join(".", node.getObjectAsList("name", String.class));

        return "import " + importName + END_STMT;
    }

    private String visitProgram(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);

        return code.toString();
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }
}
