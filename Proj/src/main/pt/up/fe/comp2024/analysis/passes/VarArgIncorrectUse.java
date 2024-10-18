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

import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.Collections;
import java.util.List;

public class VarArgIncorrectUse extends AnalysisVisitor {
    @Override
    public void buildVisitor() {
        addVisit(Kind.INT_VAR_ARGS_TYPE, this::visitVarArgsType);
    }

    private Void visitVarArgsType(JmmNode intVarArgsType, SymbolTable table) {
        var parent = intVarArgsType.getParent();

        if (parent.isInstance(Kind.PARAM)) {
            var methodDecl = parent.getParent();
            var methodName = methodDecl.get("name");
            List<Type> methodParamTypes = table.getParameters(methodName).stream().map(Symbol::getType).toList();
            Type varArgsType = new Type(TypeUtils.getIntVarArgsTypeName(), false);

            if (Collections.frequency(methodParamTypes, varArgsType) == 1) {
                if (!methodParamTypes.get(methodParamTypes.size() - 1).equals(varArgsType)) {
                    // Create error report
                    var message = "A VarArgs parameter must be the last one in a method declaration.";
                    addReport(Report.newError(
                            Stage.SEMANTIC,
                            NodeUtils.getLine(intVarArgsType),
                            NodeUtils.getColumn(intVarArgsType),
                            message,
                            null)
                    );
                }
            } else {
                // Create error report
                var message = "A method declaration can only have 1 VarArgs parameter.";
                addReport(Report.newError(
                        Stage.SEMANTIC,
                        NodeUtils.getLine(intVarArgsType),
                        NodeUtils.getColumn(intVarArgsType),
                        message,
                        null)
                );
            }
        } else {
            // Create error report
            var message = "VarArgs must be used as a parameter in a method declaration.";
            addReport(Report.newError(
                    Stage.SEMANTIC,
                    NodeUtils.getLine(intVarArgsType),
                    NodeUtils.getColumn(intVarArgsType),
                    message,
                    null)
            );
        }

        return null;
    }
}
