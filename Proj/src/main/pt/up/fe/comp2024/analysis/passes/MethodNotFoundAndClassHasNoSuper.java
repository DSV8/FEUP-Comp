package pt.up.fe.comp2024.analysis.passes;

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

public class MethodNotFoundAndClassHasNoSuper extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.MEMBER_METHOD_ACCESS, this::visitMemberMethodAccess);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitMemberMethodAccess(JmmNode memberMethodAccess, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        var methodName = memberMethodAccess.get("method");

        if (!methodName.equals("length")) {
            var caller = memberMethodAccess.getChild(0);
            if (!caller.hasAttribute("type")) {
                caller.putObject("type", TypeUtils.getExprType(caller, table, currentMethod));
            }
            var callerType = caller.getObject("type", Type.class);

            if (callerType.equals(new Type(table.getClassName(), false)) && !table.getMethods().contains(methodName)) {
                for (String importStr : table.getImports()) {
                    String[] parts = importStr.split("\\.");
                    String importClassName = parts[parts.length - 1];
                    if (table.getSuper().equals(importClassName)) {
                        return null;
                    }
                }
                var message = "Method " + methodName + " is not declared in class " + table.getClassName() + ", which also doesn't have a super class";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(memberMethodAccess),
                        NodeUtils.getColumn(memberMethodAccess),
                        message,
                        null)
                );
            }
        }
        return null;
    }
}
