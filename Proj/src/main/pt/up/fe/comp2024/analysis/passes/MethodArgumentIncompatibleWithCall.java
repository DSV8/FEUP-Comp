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

public class MethodArgumentIncompatibleWithCall extends AnalysisVisitor {

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

        var message = "The type of the arguments in the method call do not correspond to the type of the parameters in the method declaration.";

        var methodName = memberMethodAccess.get("method");

        if (!methodName.equals("length")) {
            var caller = memberMethodAccess.getChild(0);
            if (!caller.hasAttribute("type")) {
                caller.putObject("type", TypeUtils.getExprType(caller, table, currentMethod));
            }
            var callerType = caller.getObject("type", Type.class);

            if (callerType.equals(new Type(table.getClassName(), false)) && table.getMethods().contains(methodName)) {
                List<Type> methodParamTypes = table.getParameters(methodName).stream().map(Symbol::getType).toList();
                List<JmmNode> callArgs = memberMethodAccess.getChildren().stream().skip(1).toList();

                for (int i = 0; i < callArgs.size(); i++) {
                    try {
                        JmmNode arg = callArgs.get(i);
                        if (!arg.hasAttribute("type")) {
                            arg.putObject("type", TypeUtils.getExprType(arg, table, currentMethod));
                        }
                        Type argType = arg.getObject("type", Type.class);
                        Type methodParamType = methodParamTypes.get(i);

                        if (!argType.equals(methodParamType)) {
                            if (methodParamType.equals(new Type(TypeUtils.getIntVarArgsTypeName(), false))) {
                                if (!argType.equals(new Type(TypeUtils.getIntTypeName(), true))) {
                                    for (int j = i; j < callArgs.size(); j++) {
                                        arg = callArgs.get(j);
                                        if (!arg.hasAttribute("type")) {
                                            arg.putObject("type", TypeUtils.getExprType(arg, table, currentMethod));
                                        }
                                        argType = arg.getObject("type", Type.class);
                                        if (!argType.equals(new Type(TypeUtils.getIntTypeName(), false))) {
                                            // Create error report
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
                            } else {
                                var thisClassType = new Type(table.getClassName(), false);
                                if (argType.equals(thisClassType)) {
                                    var superClassType = new Type(table.getSuper(), false);
                                    if (methodParamType.equals(superClassType)) {
                                        continue;
                                    }
                                }
                                // Create error report
                                addReport(Report.newError(
                                        Stage.SEMANTIC,
                                        NodeUtils.getLine(memberMethodAccess),
                                        NodeUtils.getColumn(memberMethodAccess),
                                        message,
                                        null)
                                );
                            }
                        }
                    } catch (IndexOutOfBoundsException e) {
                        message = "Incompatible number of arguments and parameters.";
                        // Create error report
                        addReport(Report.newError(
                                Stage.SEMANTIC,
                                NodeUtils.getLine(memberMethodAccess),
                                NodeUtils.getColumn(memberMethodAccess),
                                message,
                                null)
                        );
                    }
                }
            }
        }
        return null;
    }
}
