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

public class ArrayAccessIdxNotIntExpr extends AnalysisVisitor {
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ARRAY_ACCESS, this::visitArrayAccess);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitArrayAccess(JmmNode arrayAccess, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        var arrayAccessIdxExpr = arrayAccess.getChild(1);
        if (!arrayAccessIdxExpr.hasAttribute("type")) {
            arrayAccessIdxExpr.putObject("type", TypeUtils.getExprType(arrayAccessIdxExpr, table, currentMethod));
        }
        Type arrayAccessIdxExprType = arrayAccessIdxExpr.getObject("type", Type.class);
        String arrayAccessIdxExprTypeName = arrayAccessIdxExprType.getName();
        boolean arrayAccessIdxExprTypeIsArray = arrayAccessIdxExprType.isArray();

        if (!arrayAccessIdxExprType.equals(new Type("int", false))) {
            // Create error report
            var message = "Array Access expression of type " + arrayAccessIdxExprTypeName;
            if (arrayAccessIdxExprTypeIsArray) {
                message += " array";
            }
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(arrayAccess),
                    NodeUtils.getColumn(arrayAccess),
                    message,
                    null)
            );
        }

        return null;
    }
}
