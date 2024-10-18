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

public class ArrayAccessOverArray extends AnalysisVisitor {
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

        var arrayAccessId = arrayAccess.getChild(0);
        if (!arrayAccessId.hasAttribute("type")) {
            arrayAccessId.putObject("type", TypeUtils.getExprType(arrayAccessId, table, currentMethod));
        }
        Type arrayAccessType = arrayAccessId.getObject("type", Type.class);

        if (!arrayAccessType.isArray()) {
            // Create error report
            var message = "Variable " + arrayAccessId.get("value") + " is of type " + arrayAccessType.getName()+ ", but it's being used for an Array Access.";
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
