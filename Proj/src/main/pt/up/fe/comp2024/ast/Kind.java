package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.specs.util.SpecsStrings;

import java.util.Arrays;
import java.util.Set;

public enum Kind {
    PROGRAM,
    IMPORT_DECL,
    CLASS_DECL,
    VAR_DECL,
    INT_ARRAY_TYPE,
    INT_VAR_ARGS_TYPE,
    BOOL_TYPE,
    INT_TYPE,
    STR_TYPE,
    STR_ARRAY_TYPE,
    OBJ_TYPE,
    VOID_TYPE,
    METHOD_DECL,
    PARAM,
    BLOCK_STMT,
    IF_ELSE_STMT,
    WHILE_STMT,
    EXPR_STMT,
    ASSIGN_STMT,
    RETURN_STMT,
    MEMBER_METHOD_ACCESS,
    ARRAY_ACCESS,
    PARENTHESES,
    NEGATION,
    NEW_ARRAY_EXPR,
    NEW_OBJ_EXPR,
    ARITHMETIC_EXPR,
    RELATIONAL_EXPR,
    LOGICAL_EXPR,
    ARRAY_INIT_EXPR,
    INTEGER_LITERAL,
    BOOL_LITERAL,
    IDENTIFIER,
    THIS;

    private static final Set<Kind> TYPE = Set.of(INT_ARRAY_TYPE, INT_VAR_ARGS_TYPE, BOOL_TYPE, INT_TYPE, STR_TYPE, STR_ARRAY_TYPE, OBJ_TYPE, VOID_TYPE);
    private static final Set<Kind> STATEMENTS = Set.of(BLOCK_STMT, IF_ELSE_STMT, WHILE_STMT, EXPR_STMT, ASSIGN_STMT, RETURN_STMT);
    private static final Set<Kind> EXPRESSIONS = Set.of(MEMBER_METHOD_ACCESS, ARRAY_ACCESS, PARENTHESES, NEGATION, NEW_ARRAY_EXPR, NEW_OBJ_EXPR, ARITHMETIC_EXPR, RELATIONAL_EXPR, LOGICAL_EXPR, ARRAY_INIT_EXPR, INTEGER_LITERAL, BOOL_LITERAL, IDENTIFIER, THIS);

    private final String name;

    private Kind(String name) {
        this.name = name;
    }

    private Kind() {
        this.name = SpecsStrings.toCamelCase(name(), "_", true);
    }

    public static Kind fromString(String kind) {

        for (Kind k : Kind.values()) {
            if (k.getNodeName().equals(kind)) {
                return k;
            }
        }
        throw new RuntimeException("Could not convert string '" + kind + "' to a Kind");
    }

    public String getNodeName() {
        return name;
    }

    @Override
    public String toString() {
        return getNodeName();
    }

    public boolean isType() {
        return TYPE.contains(this);
    }

    /**
     * @return true if this kind represents a statement, false otherwise
     */
    public boolean isStmt() {
        return STATEMENTS.contains(this);
    }

    /**
     * @return true if this kind represents an expression, false otherwise
     */
    public boolean isExpr() {
        return EXPRESSIONS.contains(this);
    }

    public static boolean isNameExpr(String name) {
        return EXPRESSIONS.stream().map(Kind::toString).toList().contains(name);
    }

    public static boolean isNameStmt(String name) {
        return STATEMENTS.stream().map(Kind::toString).toList().contains(name);
    }

    /**
     * Tests if the given JmmNode has the same kind as this type.
     *
     * @param node
     * @return
     */
    public boolean check(JmmNode node) {
        return node.getKind().equals(getNodeName());
    }

    /**
     * Performs a check and throws if the test fails. Otherwise, does nothing.
     *
     * @param node
     */
    public void checkOrThrow(JmmNode node) {

        if (!check(node)) {
            throw new RuntimeException("Node '" + node + "' is not a '" + getNodeName() + "'");
        }
    }

    /**
     * Performs a check on all kinds to test and returns false if none matches. Otherwise, returns true.
     *
     * @param node
     * @param kindsToTest
     * @return
     */
    public static boolean check(JmmNode node, Kind... kindsToTest) {

        for (Kind k : kindsToTest) {

            // if any matches, return successfully
            if (k.check(node)) {

                return true;
            }
        }

        return false;
    }

    /**
     * Performs a check an all kinds to test and throws if none matches. Otherwise, does nothing.
     *
     * @param node
     * @param kindsToTest
     */
    public static void checkOrThrow(JmmNode node, Kind... kindsToTest) {
        if (!check(node, kindsToTest)) {
            // throw if none matches
            throw new RuntimeException("Node '" + node + "' is not any of " + Arrays.asList(kindsToTest));
        }
    }

    public static boolean checkIsType(JmmNode node) {
        return TYPE.stream().map(Kind::getNodeName).anyMatch(type_name -> type_name.equals(node.getKind()));
    }
}
