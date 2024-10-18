package pt.up.fe.comp2024;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

public class JavaCalcGenerator extends AJmmVisitor<String, String> {
    private String className;

    public JavaCalcGenerator(String className) {
        this.className = className;
    }

    protected void buildVisitor() {
        addVisit("Program", this::dealWithProgram);
        addVisit("Assignment", this::dealWithAssignment);
        addVisit("Integer", this::dealWithLiteral);
        addVisit("Identifier", this::dealWithLiteral);
        addVisit("ExprStmt", this::dealWithExprStmt);
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("Parenthesis", this::dealWithParenthesis);
    }

    private String dealWithProgram(JmmNode jmmNode, String s) {
        s = (s != null ? s : "");
        String ret = s + "public class " + this.className + " {\n";
        String s2 = s + "\t";
        ret += s2 + "public static void main(String[] args) {\n";
        for (JmmNode child : jmmNode.getChildren()) {
            ret += visit(child, s2 + "\t");
            ret += "\n";
        }
        ret += s2 + "}\n";
        ret += s + "}\n";
        return ret;
    }

    private String dealWithAssignment(JmmNode jmmNode, String s) {
        return s + "int " + jmmNode.get("var")
                + " = " + jmmNode.get("value")
                + ";";
    }

    private String dealWithLiteral(JmmNode jmmNode, String s) {
        return jmmNode.get("value");
    }

    private String dealWithExprStmt(JmmNode jmmNode, String s) {
        String expr = visit(jmmNode.getJmmChild(0), s);
        return s + "System.out.println(" + expr + ")" + ";";
    }

    private String dealWithParenthesis(JmmNode jmmNode, String s) {
        String expr = visit(jmmNode.getJmmChild(0), s);
        return "(" + expr + ")";
    }

    private String dealWithBinaryOp(JmmNode jmmNode, String s) {
        String op = jmmNode.get("op");
        String left = visit(jmmNode.getJmmChild(0), s);
        String right = visit(jmmNode.getJmmChild(1), s);

        return left + " " + op + " " + right;
    }
}
