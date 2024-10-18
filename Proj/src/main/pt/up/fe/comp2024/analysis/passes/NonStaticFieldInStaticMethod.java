package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.specs.util.SpecsCheck;

public class NonStaticFieldInStaticMethod extends AnalysisVisitor {
    private String currentMethod;
    private boolean currentMethodIsStatic;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.IDENTIFIER, this::visitIdentifier);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        currentMethodIsStatic = method.getObject("isStatic", Boolean.class);
        return null;
    }

    private Void visitIdentifier(JmmNode identifier, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        if (currentMethodIsStatic) {
            var identifierName = identifier.get("value");
            boolean varIsLocal = table.getLocalVariables(currentMethod).stream().map(Symbol::getName).toList().contains(identifierName);
            boolean varIsParameter = table.getParameters(currentMethod).stream().map(Symbol::getName).toList().contains(identifierName);
            boolean varIsField = table.getFields().stream().map(Symbol::getName).toList().contains(identifierName);

            if (!varIsLocal && !varIsParameter && varIsField) {
                // Create error report
                var message = "Static method '" + currentMethod + " is accessing a non static field.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(identifier),
                        NodeUtils.getColumn(identifier),
                        message,
                        null)
                );
            }
        }

        return null;
    }
}
