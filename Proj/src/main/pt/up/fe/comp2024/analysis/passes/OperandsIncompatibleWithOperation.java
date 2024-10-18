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

public class OperandsIncompatibleWithOperation extends AnalysisVisitor {
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ARITHMETIC_EXPR, this::visitBinaryExpr);
        addVisit(Kind.RELATIONAL_EXPR, this::visitBinaryExpr);
        addVisit(Kind.LOGICAL_EXPR, this::visitBinaryExpr);
        addVisit(Kind.NEGATION, this::visitNegation);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitBinaryExpr(JmmNode binExpr, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        if (!binExpr.hasAttribute("type")) {
            binExpr.putObject("type", TypeUtils.getExprType(binExpr, table, currentMethod));
        }
        var operator = binExpr.get("op");
        Type necessaryType = operator.equals("<") ? new Type(TypeUtils.getIntTypeName(), false) : binExpr.getObject("type", Type.class);

        JmmNode leftOperand = binExpr.getChild(0);
        JmmNode rightOperand = binExpr.getChild(1);

        if (!leftOperand.hasAttribute("type")) {
            leftOperand.putObject("type", TypeUtils.getExprType(leftOperand, table, currentMethod));
        }
        if (!rightOperand.hasAttribute("type")) {
            rightOperand.putObject("type", TypeUtils.getExprType(rightOperand, table, currentMethod));
        }
        Type leftType = leftOperand.getObject("type", Type.class);
        Type rightType = rightOperand.getObject("type", Type.class);

        if (!leftType.equals(necessaryType) || !rightType.equals(necessaryType)) {
            // Create error report
            var arrayMessageLeft = leftType.isArray() ? " array" : "";
            var arrayMessageRight = rightType.isArray() ? " array" : "";
            var message = "Operands of type " + leftType.getName() + arrayMessageLeft + " and " + rightType.getName() + arrayMessageRight + ", for operation of type " + necessaryType.getName();
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(binExpr),
                    NodeUtils.getColumn(binExpr),
                    message,
                    null)
            );
        }

        return null;
    }

    private Void visitNegation(JmmNode negation, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        if (!negation.hasAttribute("type")) {
            negation.putObject("type", TypeUtils.getExprType(negation, table, currentMethod));
        }
        Type exprType = negation.getObject("type", Type.class);

        JmmNode operand = negation.getChild(0);
        if (!operand.hasAttribute("type")) {
            operand.putObject("type", TypeUtils.getExprType(operand, table, currentMethod));
        }
        Type operandType = operand.getObject("type", Type.class);

        if (!operandType.equals(exprType)) {
            // Create error report
            var message = "Operand of type " + operandType.getName() + " doesn't match negation of type " + exprType.getName();
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(negation),
                    NodeUtils.getColumn(negation),
                    message,
                    null)
            );
        }

        return null;
    }
}
