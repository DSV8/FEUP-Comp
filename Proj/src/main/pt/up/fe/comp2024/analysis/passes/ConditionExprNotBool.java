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

public class ConditionExprNotBool extends AnalysisVisitor {
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.IF_ELSE_STMT, this::visitCondStmt);
        addVisit(Kind.WHILE_STMT, this::visitCondStmt);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitCondStmt(JmmNode condStmt, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        var conditionExpr = condStmt.getChild(0);
        if (!conditionExpr.hasAttribute("type")) {
            conditionExpr.putObject("type", TypeUtils.getExprType(conditionExpr, table, currentMethod));
        }
        Type conditionExprType = conditionExpr.getObject("type", Type.class);
        String conditionExprTypeName = conditionExprType.getName();
        boolean conditionExprTypeIsArray = conditionExprType.isArray();

        if (!conditionExprType.equals(new Type(TypeUtils.getBoolTypeName(), false))) {
            // Create error report
            var message = "Condition expression of type " + conditionExprTypeName;
            if (conditionExprTypeIsArray) {
                message += " array";
            }
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(condStmt),
                    NodeUtils.getColumn(condStmt),
                    message,
                    null)
            );
        }

        return null;
    }
}
