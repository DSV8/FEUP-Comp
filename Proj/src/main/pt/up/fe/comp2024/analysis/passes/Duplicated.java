package pt.up.fe.comp2024.analysis.passes;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.AnalysisVisitor;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.List;

public class Duplicated extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.CLASS_DECL, this::visitClassDecl);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");

        var methodName = method.get("name");

        var i = 1;

        for (var param : table.getParameters(methodName)) {
            if (table.getParameters(methodName).subList(i, table.getParameters(methodName).size()).contains(param)) {
                var message = "Method has duplicated parameters.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );
            }
            i++;
        }

        i = 1;
        for (var localVar : table.getLocalVariables(methodName)) {
            if (table.getLocalVariables(methodName).subList(i, table.getLocalVariables(methodName).size()).contains(localVar)) {
                var message = "Method has duplicated local variables.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(method),
                        NodeUtils.getColumn(method),
                        message,
                        null)
                );
            }
            i++;
        }

        //Check if the method has more than one return statement
        if (method.getDescendants(Kind.RETURN_STMT).size() > 1) {
            var message = "Method has more than one return statement.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message,
                    null)
            );
        }

        // Check if the return statement is the last statement in the method
        var lastStatement = method.getChildren().get(method.getNumChildren() - 1);
        var returnType = table.getReturnType(methodName);
        if (!Kind.RETURN_STMT.check(lastStatement) && !returnType.equals(new Type("void", false))) {
            var message = "Return statement is not the last statement in the method.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(method),
                    NodeUtils.getColumn(method),
                    message,
                    null)
            );
        }

        return null;
    }

    private Void visitClassDecl(JmmNode classNode, SymbolTable table) {
        var className = classNode.get("name");

        var prevImportClassName = "";
        for (String importStr : table.getImports()) {
            String[] parts = importStr.split("\\.");
            String importClassName = parts[parts.length - 1];
            if (importClassName.equals(prevImportClassName)) {
                var message = "Method has duplicated imports.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(classNode),
                        NodeUtils.getColumn(classNode),
                        message,
                        null)
                );
                break;
            }
            prevImportClassName = importClassName;
        }

        int i = 1;
        for (var field : table.getFields()) {
            if (table.getFields().subList(i, table.getFields().size()).contains(field)) {
                var message = "Method has duplicated fields.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(classNode),
                        NodeUtils.getColumn(classNode),
                        message,
                        null)
                );
            }
            i++;
        }

        i = 1;
        for (var method : table.getMethods()) {
            if (table.getMethods().subList(i, table.getMethods().size()).contains(method)) {
                var message = "Method has duplicated methods.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(classNode),
                        NodeUtils.getColumn(classNode),
                        message,
                        null)
                );
            }
            i++;
        }

        return null;
    }

}
