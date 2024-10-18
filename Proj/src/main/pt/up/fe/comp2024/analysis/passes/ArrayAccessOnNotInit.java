package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
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

import java.util.HashSet;
import java.util.Set;

public class ArrayAccessOnNotInit extends AnalysisVisitor {
    private String currentMethod;
    private final Set<String> arraysInitialized = new HashSet<>();

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.NEW_ARRAY_EXPR, this::visitArrayInitialization);
        addVisit(Kind.ARRAY_INIT_EXPR, this::visitArrayInitialization);
        addVisit(Kind.MEMBER_METHOD_ACCESS, this::visitArrayInitialization);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
        addVisit(Kind.ARRAY_ACCESS, this::visitArrayAccess);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitArrayInitialization(JmmNode arrayInitialization, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        var arrayInitParent = arrayInitialization.getParent();

        if (Kind.RETURN_STMT.check(arrayInitParent)) {
            var ancestorMethodName = arrayInitParent.getAncestor(Kind.METHOD_DECL).get().get("name");
            arraysInitialized.add(ancestorMethodName);
        } else if (Kind.ASSIGN_STMT.check(arrayInitParent)) {
            var identifierName = arrayInitParent.getChild(0).get("value");
            arraysInitialized.add(identifierName);
        }

        return null;
    }

    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        var assigned = assignStmt.getChild(1);

        if (!assigned.hasAttribute("type")) {
            assigned.putObject("type", TypeUtils.getExprType(assigned, table, currentMethod));
        }
        Type assignedType = assigned.getObject("type", Type.class);

        if (!Kind.ARRAY_INIT_EXPR.check(assigned) && !Kind.NEW_ARRAY_EXPR.check(assigned) && assignedType.isArray()) {
            var assigneeName = assignStmt.getChild(0).get("value");
            arraysInitialized.add(assigneeName);
        }

        return null;
    }

    private Void visitArrayAccess(JmmNode arrayAccess, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        var arrayAccessId = arrayAccess.getChild(0).get("value");
        var isArrayParam = table.getParameters(currentMethod).stream().map(Symbol::getName).toList().contains(arrayAccessId);

        if (!arraysInitialized.contains(arrayAccessId) && !isArrayParam) {
            // Create error report
            var message = "Trying to access an array that has not been initialized.";
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
