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

public class ArrayInitElementsNotIntType extends AnalysisVisitor {
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ARRAY_INIT_EXPR, this::visitArrayInit);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitArrayInit(JmmNode arrayInitExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        for (var expr : arrayInitExpr.getChildren()) {
            if (!expr.hasAttribute("type")) {
                expr.putObject("type", TypeUtils.getExprType(expr, table, currentMethod));
            }
            Type exprType = expr.getObject("type", Type.class);
            if (!exprType.equals(new Type("int", false))) {
                var message = "All array elements must be of type int, but found " + exprType.getName();
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(arrayInitExpr),
                        NodeUtils.getColumn(arrayInitExpr),
                        message,
                        null)
                );
            }
        }
        return null;
    }
}
