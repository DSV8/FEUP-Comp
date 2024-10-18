package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import pt.up.fe.comp.jmm.analysis.table.Type;

public class IncorrectReturnStatement extends AnalysisVisitor {
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.RETURN_STMT, this::visitReturnStmt);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        var currentMethodReturnType = table.getReturnType(currentMethod);
        if (!currentMethodReturnType.equals(new Type("void", false))) {
            var currentMethodReturnStmt = method.getDescendants(Kind.RETURN_STMT);
            if (currentMethodReturnStmt.isEmpty()) {
                var message = "Non void method '" + currentMethod + "' without a return statement";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );
            }
        }
        return null;
    }

    private Void visitReturnStmt(JmmNode returnStmt, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        var currentMethodReturnType = table.getReturnType(currentMethod);
        var returnExpr = returnStmt.getChild(0);
        if (!returnExpr.hasAttribute("type")) {
            returnExpr.putObject("type", TypeUtils.getExprType(returnExpr, table, currentMethod));
        }
        var returnStmtExprType = returnExpr.getObject("type", Type.class);

        if (!returnStmtExprType.equals(currentMethodReturnType)) {
            if (returnStmtExprType.equals(new Type(table.getClassName(), false))) {
                if (currentMethodReturnType.equals(new Type(table.getSuper(), false))) {
                    return null;
                }
            }
            var arrayMessage = returnStmtExprType.isArray() ? "array" : "";
            var message = "Return type of " + returnStmtExprType.getName() + arrayMessage + " is not compatible with return type of method '" + currentMethod + "'";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(returnStmt),
                    NodeUtils.getColumn(returnStmt),
                    message,
                    null)
            );
        }

        return null;
    }
}
