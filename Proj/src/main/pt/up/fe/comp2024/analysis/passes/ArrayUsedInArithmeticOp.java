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

public class ArrayUsedInArithmeticOp extends AnalysisVisitor {
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ARITHMETIC_EXPR, this::visitArithmeticExpr);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitArithmeticExpr(JmmNode arithmeticExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        JmmNode leftOperand = arithmeticExpr.getChild(0);
        JmmNode rightOperand = arithmeticExpr.getChild(1);

        if (!leftOperand.hasAttribute("type")) {
            leftOperand.putObject("type", TypeUtils.getExprType(leftOperand, table, currentMethod));
        }
        Type leftType = leftOperand.getObject("type", Type.class);
        if (!rightOperand.hasAttribute("type")) {
            rightOperand.putObject("type", TypeUtils.getExprType(rightOperand, table, currentMethod));
        }
        Type rightType = rightOperand.getObject("type", Type.class);

        if (leftType.isArray() || rightType.isArray()) {
            // Create error report
            var message = "Array cannot be used in arithmetic operations.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(arithmeticExpr),
                    NodeUtils.getColumn(arithmeticExpr),
                    message,
                    null)
            );
        }

        return null;
    }
}
