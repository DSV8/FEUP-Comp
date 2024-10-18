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

import java.util.Set;

public class MethodCallNotOnObject extends AnalysisVisitor {

    private String currentMethod;

    @Override
    public void buildVisitor() {
        addVisit(Kind.METHOD_DECL, this::visitMethodDecl);
        addVisit(Kind.MEMBER_METHOD_ACCESS, this::visitMemberMethodAcess);
    }

    private Void visitMethodDecl(JmmNode method, SymbolTable table) {
        currentMethod = method.get("name");
        return null;
    }

    private Void visitMemberMethodAcess(JmmNode memberMethodAccess, SymbolTable table) {
        SpecsCheck.checkNotNull(currentMethod, () -> "Expected current method to be set");

        var methodName = memberMethodAccess.get("method");

        var caller = memberMethodAccess.getChild(0);
        if (!caller.hasAttribute("type")) {
            caller.putObject("type", TypeUtils.getExprType(caller, table, currentMethod));
        }
        var callerType = caller.getObject("type", Type.class);

        if (methodName.equals("length")) {
            if (!callerType.isArray()) {
                var message = "'length' has to be called from an array type";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(memberMethodAccess),
                        NodeUtils.getColumn(memberMethodAccess),
                        message,
                        null)
                );
            }
        } else {
            var nonObjectNames = Set.of("int", "boolean", "String", "void");
            if (callerType.isArray() || nonObjectNames.contains(callerType.getName())) {
                var message = "Methods that are not 'length' can only be called from an Object type";
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
