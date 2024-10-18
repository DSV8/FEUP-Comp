package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.Optional;

public class TypeUtils {

    private static final String INT_TYPE_NAME = "int";
    private static final String BOOL_TYPE_NAME = "boolean";
    private static final String VOID_TYPE_NAME = "void";
    private static final String INT_VAR_ARGS_TYPE_NAME = "int...";

    public static String getIntTypeName() {
        return INT_TYPE_NAME;
    }

    public static String getBoolTypeName() {
        return BOOL_TYPE_NAME;
    }

    public static String getVoidTypeName() {
        return VOID_TYPE_NAME;
    }

    public static String getIntVarArgsTypeName() {
        return INT_VAR_ARGS_TYPE_NAME;
    }

    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @param table
     * @return
     */
    public static Type getExprType(JmmNode expr, SymbolTable table, String currentMethod) {
        var kind = Kind.fromString(expr.getKind());

        Type type = switch (kind) {
            case MEMBER_METHOD_ACCESS -> getMemberMethodAccessType(expr, table, currentMethod);
            case ARRAY_ACCESS -> getArrayAccessType(expr, table, currentMethod);
            case PARENTHESES -> getExprType(expr.getChild(0), table, currentMethod);
            case NEGATION, BOOL_LITERAL -> new Type(BOOL_TYPE_NAME, false);
            case NEW_ARRAY_EXPR, ARRAY_INIT_EXPR -> new Type(INT_TYPE_NAME, true);
            case NEW_OBJ_EXPR -> new Type(expr.get("qualifier"), false);
            case ARITHMETIC_EXPR, RELATIONAL_EXPR, LOGICAL_EXPR -> getBinExprType(expr);
            case INTEGER_LITERAL -> new Type(INT_TYPE_NAME, false);
            case IDENTIFIER -> getIdentifierType(expr, table, currentMethod);
            case THIS -> new Type(table.getClassName(), false);
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        return type;
    }

    private static Type getMemberMethodAccessType(JmmNode memberMethodAccess, SymbolTable table, String currentMethod) {
        var caller = memberMethodAccess.getChild(0);
        var callerType = getExprType(caller, table, currentMethod);
        var methodName = memberMethodAccess.get("method");

        if (methodName.equals("length")) {
            return new Type(INT_TYPE_NAME, false);
        } else {
            if (callerType.equals(new Type(table.getClassName(), false))) {
                if (!table.getMethods().contains(methodName)) {
                    throw new RuntimeException("Incorrect Member Method Access. Caller: " + callerType.getName() + ", Method: " + methodName);
                }
                return table.getReturnType(methodName);
            } else {
                var parentNode = memberMethodAccess.getParent();
                if (parentNode.isInstance(Kind.ASSIGN_STMT)) {
                    var assignee = parentNode.getChild(0);
                    return getExprType(assignee, table, currentMethod);
                } else if (parentNode.isInstance(Kind.RETURN_STMT)) {
                    return table.getReturnType(currentMethod);
                } else {
                    return new Type(VOID_TYPE_NAME, false);
                }
            }
        }
    }

    private static Type getArrayAccessType(JmmNode arrayAccess, SymbolTable table, String currentMethod) {
        var arrayName = arrayAccess.getChild(0).get("value");
        String arrayTypeName = null;

        Optional<Type> fieldType = table.getFields().stream()
                .filter(field -> field.getName().equals(arrayName))
                .map(Symbol::getType)
                .findFirst();

        if (fieldType.isPresent()) {
            arrayTypeName = fieldType.get().getName();
        }

        Optional<Type> paramType = table.getParameters(currentMethod).stream()
                .filter(param -> param.getName().equals(arrayName))
                .map(Symbol::getType)
                .findFirst();

        if (paramType.isPresent()) {
            arrayTypeName = paramType.get().getName();
        }

        Optional<Type> localVarType = table.getLocalVariables(currentMethod).stream()
                .filter(localVar -> localVar.getName().equals(arrayName))
                .map(Symbol::getType)
                .findFirst();

        if (localVarType.isPresent()) {
            arrayTypeName = localVarType.get().getName();
        }

        if (arrayTypeName == null) {
            throw new RuntimeException("Unknown Array Reference '" + arrayName + "'");
        } else if (arrayTypeName.equals(TypeUtils.getIntVarArgsTypeName())) {
            arrayTypeName = TypeUtils.getIntTypeName();
        }

        return new Type(arrayTypeName, false);
    }

    private static Type getBinExprType(JmmNode binaryExpr) {
        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "-", "*", "/" -> new Type(INT_TYPE_NAME, false);
            case "&&", "<" -> new Type(BOOL_TYPE_NAME, false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }

    private static Type getIdentifierType(JmmNode identifier, SymbolTable table, String currentMethod) {
        var identifierName = identifier.get("value");
        Type identifierType = null;

        Optional<Type> fieldType = table.getFields().stream()
                .filter(field -> field.getName().equals(identifierName))
                .map(Symbol::getType)
                .findFirst();

        if (fieldType.isPresent()) {
            identifierType = fieldType.get();
        }

        Optional<Type> paramType = table.getParameters(currentMethod).stream()
                .filter(param -> param.getName().equals(identifierName))
                .map(Symbol::getType)
                .findFirst();

        if (paramType.isPresent()) {
            identifierType = paramType.get();
        }

        Optional<Type> localVarType = table.getLocalVariables(currentMethod).stream()
                .filter(localVar -> localVar.getName().equals(identifierName))
                .map(Symbol::getType)
                .findFirst();

        if (localVarType.isPresent()) {
            identifierType = localVarType.get();
        }

        if (identifierType == null) {
            identifierType = new Type(identifierName, false);
        } else if (identifierType.getName().equals(TypeUtils.getIntVarArgsTypeName())) {
            identifierType = new Type(TypeUtils.getIntTypeName(), true);
        }

        return identifierType;

    }

    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType) {
        // TODO: Simple implementation that needs to be expanded
        return sourceType.getName().equals(destinationType.getName());
    }
}
