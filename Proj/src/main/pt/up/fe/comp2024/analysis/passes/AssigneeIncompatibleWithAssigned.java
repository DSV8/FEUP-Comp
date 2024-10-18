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

public class AssigneeIncompatibleWithAssigned extends AnalysisVisitor {
    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.ASSIGN_STMT, this::visitAssignStmt);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitAssignStmt(JmmNode assignStmt, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");
        var assignee = assignStmt.getChild(0);
        var assigned = assignStmt.getChild(1);

        if (!Kind.IDENTIFIER.check(assignee) && !Kind.ARRAY_ACCESS.check(assignee)) {
            //Create error report
            var message = "Assignee must be an identifier.";
            addReport(Report.newError(
                Stage.SEMANTIC,
                NodeUtils.getLine(assignee),
                NodeUtils.getColumn(assignee),
                message,
                null)
            );
        }

        if (!assignee.hasAttribute("type")) {
            assignee.putObject("type", TypeUtils.getExprType(assignee, table, currentMethod));
        }
        Type assigneeType = assignee.getObject("type", Type.class);
        if (!assigned.hasAttribute("type")) {
            assigned.putObject("type", TypeUtils.getExprType(assigned, table, currentMethod));
        }
        Type assignedType = assigned.getObject("type", Type.class);

        if (!assigneeType.equals(assignedType)) {
            var thisClassType = new Type(table.getClassName(), false);
            if (assignedType.equals(thisClassType)) {
                var superClassType = new Type(table.getSuper(), false);
                if (assigneeType.equals(superClassType)) {
                    return null;
                }
            } else {
                var assigneeImported = false;
                var assignedImported = false;
                for (String importStr : table.getImports()) {
                    String[] parts = importStr.split("\\.");
                    String className = parts[parts.length - 1];

                    if (assigneeType.equals(new Type(className, false))) {
                        assigneeImported = true;
                    } else if (assignedType.equals(new Type(className, false))) {
                        assignedImported = true;
                    }

                    if (assigneeImported && assignedImported) {
                        return null;
                    }
                }
            }

            // Create error report
            var message = "Assigned type incompatible with assignee type.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(assignStmt),
                    NodeUtils.getColumn(assignStmt),
                    message,
                    null)
            );
        }

        return null;
    }
}
