package pt.up.fe.comp2024.backend;

import org.specs.comp.ollir.*;
import org.specs.comp.ollir.tree.TreeNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.specs.util.classmap.FunctionClassMap;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.utilities.StringLines;

import java.util.*;
import java.util.stream.Collectors;



/**
 * Generates Jasmin code from an OllirResult.
 * <p>
 * One JasminGenerator instance per OllirResult.
 */
public class JasminGenerator {

    private static final String NL = "\n";
    private static final String TAB = "   ";

    private final OllirResult ollirResult;

    List<Report> reports;

    String code;

    Method currentMethod;

    private final FunctionClassMap<TreeNode, String> generators;

    int currStack = 0;
    int maxStack = 0;

    public JasminGenerator(OllirResult ollirResult) {
        this.ollirResult = ollirResult;

        reports = new ArrayList<>();
        code = null;
        currentMethod = null;

        this.generators = new FunctionClassMap<>();
        generators.put(ClassUnit.class, this::generateClassUnit);
        generators.put(Method.class, this::generateMethod);
        generators.put(AssignInstruction.class, this::generateAssign);
        generators.put(SingleOpInstruction.class, this::generateSingleOp);
        generators.put(LiteralElement.class, this::generateLiteral);
        generators.put(Operand.class, this::generateOperand);
        generators.put(BinaryOpInstruction.class, this::generateBinaryOp);
        generators.put(ReturnInstruction.class, this::generateReturn);
        generators.put(PutFieldInstruction.class, this::generatePutField);
        generators.put(GetFieldInstruction.class, this::generateGetField);
        generators.put(CallInstruction.class, this::generateCall);
        generators.put(SingleOpCondInstruction.class, this::generateSingleOpCond);
        generators.put(OpCondInstruction.class, this::generateOpCond);
        generators.put(GotoInstruction.class, this::generateGoto);
        generators.put(UnaryOpInstruction.class, this::generateUnaryOp);
    }

    public List<Report> getReports() {
        return reports;
    }

    public String build() {
        // This way, build is idempotent
        if (code == null) {
            code = generators.apply(ollirResult.getOllirClass());
        }
        return code;
    }

    private String generateClassUnit(ClassUnit classUnit) {
        var code = new StringBuilder();

        // generate class name
        var className = ollirResult.getOllirClass().getClassName();
        code.append(".class public ").append(className).append(NL);

        var superClass = ollirResult.getOllirClass().getSuperClass() != null ?
                ollirResult.getOllirClass().getSuperClass() :
                "java/lang/Object";
        code.append(".super ").append(superClass).append(NL).append(NL);
        var defaultContructor = """
                ;default constructor
                .method public <init>()V
                    aload_0
                    invokespecial %s/<init>()V
                    return
                .end method
                """.formatted(superClass);
        code.append(defaultContructor).append(NL);

        // generate code for all other methods
        for (var method : ollirResult.getOllirClass().getMethods()) {
            /*
                Ignore constructor, since there is always one constructor
                that receives no arguments, and has been already added
                previously
            */
            if (method.isConstructMethod()) {continue;}

            code.append(generators.apply(method));
        }
        return code.toString();
    }

    private String generateMethod(Method method) {
        // set method
        currentMethod = method;

        var code = new StringBuilder();

        // calculate modifier
        var modifier = method.getMethodAccessModifier() != AccessModifier.DEFAULT ?
                method.getMethodAccessModifier().name().toLowerCase() + " " :
                "";

        var methodName = method.getMethodName();

        if (methodName.equals("main")){
            modifier += "static ";
        }

        String paramsSignatures = method.getParams().stream().map(param -> TypeToSignParam(param.getType())).collect(Collectors.joining());

        String returnSignatures = TypeToSignRet(method.getReturnType());

        code.append("\n.method ").append(modifier).append(methodName).append("(").append(paramsSignatures).append(")").append(returnSignatures).append(NL);

        Set<Integer> virtualRegs = new HashSet<>();
        if (!method.isStaticMethod()) { //is instance method => add 1 to the limit locals (this)
            virtualRegs.add(0); // this
        }

        for (Descriptor var : method.getVarTable().values()) {
            virtualRegs.add(var.getVirtualReg());
        }
        int limitLocals = virtualRegs.size();

        // First loop to calculate limit stack
        for (var inst : method.getInstructions()) {
            if (!method.getLabels(inst).isEmpty()) {
                code.append(method.getLabels(inst).get(0)).append(":").append(NL);
            }

            StringLines.getLines(generators.apply(inst)).stream()
                       .collect(Collectors.joining(NL + TAB, TAB, NL));
        }

        int limitStack = maxStack;

        // Add limits
        code.append(TAB).append(".limit stack ").append(limitStack).append(NL);
        code.append(TAB).append(".limit locals ").append(limitLocals).append(NL);

        // Second loop to append the code
        for (var inst : method.getInstructions()) {
            if (!method.getLabels(inst).isEmpty()) {
                code.append(method.getLabels(inst).get(0)).append(":").append(NL);
            }

            var instCode = StringLines.getLines(generators.apply(inst)).stream()
                    .collect(Collectors.joining(NL + TAB, TAB, NL));

            code.append(instCode);
        }

        code.append(".end method\n");

        // unset method
        currentMethod = null;

        resetStack();

        return code.toString();
    }

    private String TypeToSignParam(Type type) {
        return switch (type.toString()) {
            case "INT32" -> "I";
            case "BOOLEAN" -> "Z";
            case "STRING[]" -> "[Ljava/lang/String;";
            case "VOID" -> "V";
            case "INT32[]" -> "[I";
            default -> throw new NotImplementedException("Unsupported parameter type: " + type);
        };
    }

    private String TypeToSignRet(Type type) {
        return switch (type.toString()) {
            case "INT32" -> "I";
            case "BOOLEAN" -> "Z";
            case "STRING[]" -> "[Ljava/lang/String;";
            case "VOID" -> "V";
            case "INT32[]" -> "[I";
            default -> throw new NotImplementedException("Unsupported return type: " + type);
        };
    }

    private String generateAssign(AssignInstruction assign) {
        var code = new StringBuilder();

        var lhs = assign.getDest();
        var rhs = assign.getRhs();

        if (!(lhs instanceof Operand)) {
            throw new NotImplementedException(lhs.getClass());
        }

        var operand = (Operand) lhs;
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        var rhsCode = new StringBuilder();
        if (rhs.getChildren().toString().contains("ArrayOperand")) {
            var array = rhs.getChildren().toString();
            var arrayname = array.substring(array.indexOf(':') + 2, array.lastIndexOf('['));
            var arrayReg = currentMethod.getVarTable().get(arrayname).getVirtualReg();

            if (arrayReg >= 0 && arrayReg <= 3) {
                incrementStack();
                rhsCode.append("aload_");
            } else {
                incrementStack();
                rhsCode.append("aload ");
            }
            rhsCode.append(arrayReg).append(NL);

            var arrayIdx = rhs.getChildren().get(0).getChildren().toString();

            if (arrayIdx.contains("LiteralElement")) {
                var val = Integer.parseInt(arrayIdx.substring(arrayIdx.indexOf(':') + 2, arrayIdx.lastIndexOf('.')));
                if (val == -1) {
                    incrementStack();
                    rhsCode.append("iconst_m1").append(NL);
                } else if (val > -1 && val <= 5) {
                    incrementStack();
                    rhsCode.append("iconst_").append(val).append(NL);
                } else if (val >= -128 && val <= 127) {
                    incrementStack();
                    rhsCode.append("bipush ").append(val).append(NL);
                } else if (val >= -32768 && val <= 32767) {
                    incrementStack();
                    rhsCode.append("sipush ").append(val).append(NL);
                } else if (val >= -2147483648 && val <= 2147483647) {
                    incrementStack();
                    rhsCode.append("ldc ").append(val).append(NL);
                }
            } else {
                var temp = arrayIdx.substring(arrayIdx.indexOf(':') + 2, arrayIdx.lastIndexOf('.'));
                var tempReg = currentMethod.getVarTable().get(temp).getVirtualReg();

                if (tempReg >= 0 && tempReg <= 3) {
                    incrementStack();
                    rhsCode.append("iload_");
                } else {
                    incrementStack();
                    rhsCode.append("iload ");
                }
                rhsCode.append(tempReg).append(NL);
            }
            rhsCode.append("iaload").append(NL);
        } else {
            rhsCode.append(generators.apply(rhs));
        }

        if (operand instanceof ArrayOperand){
            if (reg >= 0 && reg <= 3) {
                incrementStack();
                code.append("aload_");
            }
            else {
                incrementStack();
                code.append("aload ");
            }
            code.append(reg).append(NL);

            var arrayIdx = lhs.getChildren().toString();

            if (arrayIdx.contains("LiteralElement")) {
                var val = Integer.parseInt(arrayIdx.substring(arrayIdx.indexOf(':') + 2, arrayIdx.lastIndexOf('.')));
                if (val == -1) {
                    incrementStack();
                    code.append("iconst_m1").append(NL);
                } else if (val > -1 && val <= 5) {
                    incrementStack();
                    code.append("iconst_").append(val).append(NL);
                } else if (val >= -128 && val <= 127) {
                    incrementStack();
                    code.append("bipush ").append(val).append(NL);
                } else if (val >= -32768 && val <= 32767) {
                    incrementStack();
                    code.append("sipush ").append(val).append(NL);
                } else if (val >= -2147483648 && val <= 2147483647) {
                    incrementStack();
                    code.append("ldc ").append(val).append(NL);
                }
            } else {
                var temp = arrayIdx.substring(arrayIdx.indexOf(':') + 2, arrayIdx.lastIndexOf('.'));
                var tempReg = currentMethod.getVarTable().get(temp).getVirtualReg();


                if (tempReg >= 0 && tempReg <= 3) {
                    incrementStack();
                    code.append("iload_");
                } else {
                    incrementStack();
                    code.append("iload ");
                }
                code.append(tempReg).append(NL);
            }
            for (int i = 0; i < 3; i++) {
                decrementStack();
            }
            code.append(rhsCode).append("iastore").append(NL);
        } else {
            code.append(rhsCode);

            ElementType elemType = operand.getType().getTypeOfElement();

            if (elemType == ElementType.INT32 || elemType == ElementType.BOOLEAN) {
                if (reg >= 0 && reg <= 3) {
                    decrementStack();
                    code.append("istore_");
                }
                else {
                    decrementStack();
                    code.append("istore ");
                }

                code.append(reg).append(NL);
            } else if (elemType == ElementType.OBJECTREF || elemType == ElementType.ARRAYREF) {
                if (reg >= 0 && reg <= 3) {
                    decrementStack();
                    code.append("astore_");
                }
                else {
                    decrementStack();
                    code.append("astore ");
                }
                code.append(reg).append(NL);
            } else {
                throw new NotImplementedException(elemType);
            }
        }
        return code.toString();
    }

    private String generateSingleOp(SingleOpInstruction singleOp) {
        return generators.apply(singleOp.getSingleOperand());
    }

    private String generateLiteral(LiteralElement literal) {
        var code = new StringBuilder();
        var literalValue = literal.getLiteral();

        if (literalValue != null) {
            int val = Integer.parseInt(literalValue);
            if (val == -1) {
                incrementStack();
                code.append("iconst_m1").append(NL);
            } else if (val > -1 && val <= 5) {
                incrementStack();
                code.append("iconst_").append(literalValue).append(NL);
            } else if (val >= -128 && val <= 127) {
                incrementStack();
                code.append("bipush ").append(literalValue).append(NL);
            } else if (val >= -32768 && val <= 32767) {
                incrementStack();
                code.append("sipush ").append(literalValue).append(NL);
            } else if (val >= -2147483648 && val <= 2147483647) {
                incrementStack();
                code.append("ldc ").append(literalValue).append(NL);
            }
        } else {
            throw new NotImplementedException("Unsupported literal type: " + literalValue.getClass());
        }

        return code.toString();
    }

    private String generateOperand(Operand operand) {
        // get register
        var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

        switch(operand.getType().getTypeOfElement()){
            case INT32, BOOLEAN -> {
                if (reg >= 0 && reg <= 3) {
                    incrementStack();
                    return "iload_" + reg + NL;
                } else {
                    incrementStack();
                    return "iload " + reg + NL;
                }
            }
            case OBJECTREF, ARRAYREF -> {
                if (reg >= 0 && reg <= 3) {
                    incrementStack();
                    return "aload_" + reg + NL;
                } else {
                    incrementStack();
                    return "aload " + reg + NL;
                }
            }
            case THIS -> {
                incrementStack();
                return "aload_0" + NL;
            }
            default -> throw new NotImplementedException(operand.getType().getTypeOfElement());
        }
    }

    private String generateBinaryOp(BinaryOpInstruction binaryOp) {
        var code = new StringBuilder();

        // load values on the left and on the right
        code.append(generators.apply(binaryOp.getLeftOperand()));
        code.append(generators.apply(binaryOp.getRightOperand()));

        // apply operation
        var op = switch (binaryOp.getOperation().getOpType()) {
            case ADD -> "iadd";
            case SUB -> "isub";
            case MUL -> "imul";
            case DIV -> "idiv";
            case LTH -> "if_icmplt";
            case AND -> "iand";
            default -> throw new NotImplementedException(binaryOp.getOperation().getOpType());
        };

        decrementStack();
        code.append(op).append(NL);

        return code.toString();
    }

    private String generateReturn(ReturnInstruction returnInst) {
        var code = new StringBuilder();

        if (returnInst.hasReturnValue()){
            code.append(generators.apply(returnInst.getOperand()));

            var returnType = returnInst.getOperand().getType().toString();
            switch (returnType) {
                case "INT32", "BOOLEAN" -> code.append("ireturn").append(NL);
                case "OBJECTREF", "INT32[]" -> code.append("areturn").append(NL);
                case "VOID" -> code.append("return").append(NL);
                default -> throw new NotImplementedException("Type " + returnType + " not supported yet");
            }
        }
        else{
            code.append("return").append(NL);
        }
        return code.toString();
    }

    private String generateCall(CallInstruction call) {
        var code = new StringBuilder();
        var type = call.getInvocationType();

        var methodName = call.getMethodNameTry().toString();
        var simplifiedMethodName = methodName.substring(methodName.indexOf(':') + 3, methodName.lastIndexOf('.') - 1);
        var parameters = call.getOperands().stream().skip(2).toList();

        switch (type) {
            case NEW -> {
                    var className = call.getOperands().get(0).toString().split(" ")[1].split("\\.")[0];

                    if (className.equals("array")) {
                        var arrayIdx = call.getOperands().get(1).toString();
                        if (arrayIdx.contains("LiteralElement")) {
                            var temp = arrayIdx.substring(arrayIdx.indexOf(':') + 2, arrayIdx.lastIndexOf('.'));
                            var val = Integer.parseInt(temp);
                            if (val == -1) {
                                incrementStack();
                                code.append("iconst_m1").append(NL);
                            } else if (val > -1 && val <= 5) {
                                incrementStack();
                                code.append("iconst_").append(temp).append(NL);
                            } else if (val >= -128 && val <= 127) {
                                incrementStack();
                                code.append("bipush ").append(temp).append(NL);
                            } else if (val >= -32768 && val <= 32767) {
                                incrementStack();
                                code.append("sipush ").append(temp).append(NL);
                            } else if (val >= -2147483648 && val <= 2147483647) {
                                incrementStack();
                                code.append("ldc ").append(temp).append(NL);
                            }
                            code.append("newarray int").append(NL);
                        } else {
                            var temp = arrayIdx.substring(arrayIdx.indexOf(':') + 2, arrayIdx.lastIndexOf('.'));
                            var tempReg = currentMethod.getVarTable().get(temp).getVirtualReg();

                            if (tempReg >= 0 && tempReg <= 3) {
                                incrementStack();
                                code.append("iload_").append(tempReg).append(NL);
                            } else {
                                incrementStack();
                                code.append("iload ").append(tempReg).append(NL);
                            }
                            code.append("new").append(className).append(" ").append("int").append(NL);
                        }
                    } else {
                        code.append("new ").append(className).append(NL)
                            .append("dup ").append(NL);
                    }
            }
            case invokespecial -> {
                String str = call.getOperands().get(0).toString();
                var className = str.substring(str.indexOf('(') + 1, str.lastIndexOf(')'));

                    incrementStack();
                    code.append(generators.apply(call.getOperands().get(0)))
                    .append("invokespecial ").append(className)
                    .append("/<init>").append("(").append(getParametersType(parameters)).append(")")
                    .append(TypeToSignRet(call.getReturnType())).append(NL);
            }
            case invokevirtual -> {
                String str = call.getOperands().get(0).toString();
                var className = str.substring(str.indexOf('(') + 1, str.lastIndexOf(')'));

                code.append(generators.apply(call.getOperands().get(0)));

                for (var param : parameters) {
                    decrementStack();
                    code.append(generators.apply(param));
                }

                incrementStack();
                code.append("invokevirtual ").append(className)
                .append("/").append(simplifiedMethodName).append("(").append(getParametersType(parameters)).append(")")
                .append(TypeToSignRet(call.getReturnType())).append(NL);
            }
            case invokestatic -> {
                var className = call.getOperands().get(0).toString().split(" ")[1].split("\\.")[0];

                for (var param : parameters) {
                    decrementStack();
                    code.append(generators.apply(param));
                }

                incrementStack();
                code.append("invokestatic ").append(className)
                .append("/").append(simplifiedMethodName).append("(").append(getParametersType(parameters)).append(")")
                .append(TypeToSignRet(call.getReturnType())).append(NL);
            }
            case arraylength -> {
                var operand = (Operand) call.getOperands().get(0);
                var reg = currentMethod.getVarTable().get(operand.getName()).getVirtualReg();

                code.append(generators.apply(operand));
                code.append("arraylength").append(NL);
            }
            default -> throw new NotImplementedException("Unsupported call type: " + type);
        }
        return code.toString();
    }

    private String generatePutField(PutFieldInstruction putField) {
        var code = new StringBuilder();
        var field = (Operand) putField.getField();
        var object = (Operand) putField.getObject();
        Element value = putField.getValue();

        var reg = currentMethod.getVarTable().get(object.getName()).getVirtualReg();

        if (reg >= 0 && reg <= 3) {
            incrementStack();
            code.append("aload_").append(reg).append(NL);
        } else {
            incrementStack();
            code.append("aload ").append(reg).append(NL);
        }

        if (value instanceof LiteralElement) {
            int val = Integer.parseInt(((LiteralElement) value).getLiteral());

            if (val == -1) {
                incrementStack();
                code.append("iconst_m1").append(NL);
            } else if (val > -1 && val <= 5) {
                incrementStack();
                code.append("iconst_").append(((LiteralElement) value).getLiteral()).append(NL);
            } else if (val >= -128 && val <= 127) {
                incrementStack();
                code.append("bipush ").append(((LiteralElement) value).getLiteral()).append(NL);
            } else if (val >= -32768 && val <= 32767) {
                incrementStack();
                code.append("sipush ").append(((LiteralElement) value).getLiteral()).append(NL);
            } else if (val >= -2147483648 && val <= 2147483647) {
                incrementStack();
                code.append("ldc ").append(((LiteralElement) value).getLiteral()).append(NL);
            }
        } else if (value instanceof Operand) {
            int val = Integer.parseInt(((Operand) value).getName());

            if (val >= 0 && val <= 3) {
                code.append(TAB).append(getInstructionsLoad(((Operand) value).getType())).append("_").append(currentMethod.getVarTable().get(((Operand) value).getName()).getVirtualReg()).append(NL);
            } else {
                code.append(TAB).append(getInstructionsLoad(((Operand) value).getType())).append(" ").append(currentMethod.getVarTable().get(((Operand) value).getName()).getVirtualReg()).append(NL);
            }
        }
        for (int i = 0; i < 2; i++) {
            decrementStack();
        }
        code.append("putfield").append(" ").append(currentMethod.getOllirClass().getClassName().replace(".", "/")).append("/").append(field.getName()).append(" ").append(getTypeDesc(field.getType())).append(NL);
        return code.toString();
    }

    private String generateGetField(GetFieldInstruction getField) {
        var code = new StringBuilder();
        var field = (Operand) getField.getField();
        var object = (Operand) getField.getObject();

        var reg = currentMethod.getVarTable().get(object.getName()).getVirtualReg();

        if (reg >= 0 && reg <= 3) {
            incrementStack();
            code.append("aload_").append(reg).append(NL);
        } else {
            incrementStack();
            code.append("aload ").append(reg).append(NL);
        }

        decrementStack();
        code.append("getfield").append(" ").append(ollirResult.getOllirClass().getClassName().replace(".", "/")).append("/").append(field.getName()).append(" ").append(getTypeDesc(field.getType())).append(NL);
        return code.toString();
    }

    private String generateSingleOpCond(SingleOpCondInstruction singleOpCond) {
        var code = new StringBuilder();

        code.append(generators.apply(singleOpCond.getOperands().get(0)));

        var op = switch (singleOpCond.getCondition().getInstType()) {
            case NOPER -> "ifne";
            default -> throw new NotImplementedException(singleOpCond.getCondition().getInstType());
        };

        code.append(op).append(" ").append(singleOpCond.getLabel()).append(NL);

        return code.toString();
    }

    private String generateOpCond(OpCondInstruction opCond) {
        var code = new StringBuilder();

        var leftOperand = opCond.getOperands().get(0);
        var rightOperand = opCond.getOperands().get(1);

        code.append(generators.apply(leftOperand));
        code.append(generators.apply(rightOperand));

        var op = switch (opCond.getCondition().getOperation().getOpType()) {
            case LTH -> "if_icmplt";
            case LTE -> "if_icmple";
            case GTE -> "if_icmpge";
            case GTH -> "if_icmpgt";

            default -> throw new NotImplementedException(opCond.getCondition().getInstType());
        };

        decrementStack();
        code.append(op).append(" ").append(opCond.getLabel()).append(NL);

        return code.toString();
    }

    private String generateUnaryOp(UnaryOpInstruction unaryOp) {
        var code = new StringBuilder();
        var operand = unaryOp.getOperand();

        code.append(generators.apply(operand));

        String op;
        switch (unaryOp.getOperation().getOpType()) {
            case NOTB -> {
                if (operand.toString().equals("1.bool")) {
                    incrementStack();
                    decrementStack();
                    op = "iconst_1\nixor";
                } else {
                    incrementStack();
                    decrementStack();
                    op = "iconst_1\nixor";
                }
            }
            default -> throw new NotImplementedException(unaryOp.getOperation().getOpType());
        };

        code.append(op).append(NL);

        return code.toString();
    }

    private String generateGoto(GotoInstruction gotoInst) {
        var code = new StringBuilder();

        code.append("goto ").append(gotoInst.getLabel()).append(NL);
        return code.toString();
    }


    private String getTypeDesc(Type type) {
        switch (type.toString()) {
            case "INT32" -> {
                return "I";
            }
            case "BOOLEAN" -> {
                return "Z";
            }
            case "STRING" -> {
                return "Ljava/lang/String;";
            }
            case "VOID" -> {
                return "V";
            }
            case "OBJECTREF", "CLASS" -> {
                return "L" + ((ClassType)type).getName() + ";";
            }
            case "INT32[]" -> {
                return "[I";
            }

            default -> throw new NotImplementedException("Unsupported type: " + type);
        }
    }

    private String getParametersType(List<Element> parameters){
        var code = new StringBuilder();
        for (Element parameter : parameters) {
            var param = parameter.getType();
            var paramType = getTypeDesc(param);
            code.append(paramType);
        }
        return code.toString();
    }

    private String getInstructionsLoad(Type type){
        switch (type.toString()){
            case "INT32", "BOOLEAN" -> {
                incrementStack();
                return "iload";
            }
            case "OBJECTREF" -> {
                incrementStack();
                return "aload";
            }
            default -> throw new NotImplementedException("Type " + type + " not supported yet");
        }
    }

    public void incrementStack() {
        currStack++;
        if (currStack > maxStack) {maxStack = currStack;}
    }

    public void decrementStack() {
        currStack--;
    }

    public void resetStack() {
        currStack = 0;
        maxStack = 0;
    }
}
