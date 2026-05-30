// File 3: ExpressionEvaluator.java
package com.tcveinminer.util;

import java.util.ArrayList;
import java.util.List;

public class ExpressionEvaluator {
    private static final int MAX_DEPTH = 30;
    private static final double MAX_POWER = 100.0;

    private String error = null;
    private ASTNode root = null;
    private boolean usesX, usesY, usesZ;
    private String complexityError = null;
    private String rootOperator = "";
    private boolean hasMultipleEquations = false;

    public ExpressionEvaluator(String expression) {
        try {
            Lexer lexer = new Lexer(expression);
            Parser parser = new Parser(lexer);
            root = parser.parse();
            usesX = parser.usesX;
            usesY = parser.usesY;
            usesZ = parser.usesZ;
            rootOperator = parser.rootOp;
            hasMultipleEquations = parser.multipleEq;
            complexityError = parser.complexityError;

            if (complexityError != null) { error = complexityError; return; }
            Object res = root.eval(0, 0, 0);
            if (!(res instanceof Boolean)) {
                error = "Kết quả phải là điều kiện đúng/sai, không phải số.";
            }
        } catch (Exception e) {
            error = e.getMessage();
        }
    }

    public boolean isValid() { return error == null; }
    public String getError() { return error; }
    public boolean usesX() { return usesX; }
    public boolean usesY() { return usesY; }
    public boolean usesZ() { return usesZ; }
    public String getRootOperator() { return rootOperator; }
    public boolean hasMultipleEquations() { return hasMultipleEquations; }

    public boolean evalBoolean(double x, double y, double z) {
        if (root == null) return false;
        try {
            Object res = root.eval(x, y, z);
            return res instanceof Boolean && (Boolean) res;
        } catch (Exception ignored) { return false; }
    }

    public boolean eval(int x, int y, int z) {
        return evalBoolean(x, y, z);
    }

    private interface ASTNode { Object eval(double x, double y, double z); }
    private enum TokenType { NUM, ID, OP, LPAREN, RPAREN, COMMA, EOF }

    private static class Token {
        TokenType type; String val; double numVal;
        Token(TokenType t, String v) { type = t; val = v; }
        Token(TokenType t, double n) { type = t; numVal = n; }
    }

    private static class Lexer {
        String src; int pos = 0;
        Lexer(String src) { this.src = src; }

        Token next() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) pos++;
            if (pos >= src.length()) return new Token(TokenType.EOF, "");
            char c = src.charAt(pos);

            if (Character.isDigit(c) || c == '.') {
                int start = pos;
                while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) pos++;
                try { return new Token(TokenType.NUM, Double.parseDouble(src.substring(start, pos))); }
                catch (NumberFormatException e) { throw new RuntimeException("Lỗi số tại: " + src.substring(start, pos)); }
            }

            if (Character.isLetter(c) || c == '_') {
                int start = pos;
                while (pos < src.length() && (Character.isLetterOrDigit(src.charAt(pos)) || src.charAt(pos) == '_')) pos++;
                String id = src.substring(start, pos);
                if (id.equals("and")) return new Token(TokenType.OP, "&&");
                if (id.equals("or")) return new Token(TokenType.OP, "||");
                if (id.equals("not")) return new Token(TokenType.OP, "!");
                return new Token(TokenType.ID, id);
            }

            if (c == '=') {
                if (pos + 1 < src.length() && src.charAt(pos+1) == '=') { pos+=2; return new Token(TokenType.OP, "=="); }
                throw new RuntimeException("Dùng '==' để so sánh");
            }

            if ("!=<>|&".indexOf(c) != -1) {
                if (pos + 1 < src.length()) {
                    String op2 = src.substring(pos, pos + 2);
                    if (op2.equals("!=") || op2.equals("<=") || op2.equals(">=") || op2.equals("&&") || op2.equals("||")) {
                        pos += 2; return new Token(TokenType.OP, op2);
                    }
                }
            }

            pos++;
            if (c == '(') return new Token(TokenType.LPAREN, "(");
            if (c == ')') return new Token(TokenType.RPAREN, ")");
            if (c == ',') return new Token(TokenType.COMMA, ",");
            return new Token(TokenType.OP, String.valueOf(c));
        }
    }

    private static class Parser {
        Lexer lex; Token cur;
        boolean usesX, usesY, usesZ;
        String complexityError = null;
        String rootOp = "";
        boolean multipleEq = false;
        int depth = 0;

        Parser(Lexer l) { lex = l; cur = lex.next(); }

        void eat(TokenType t) {
            if (cur.type == t) cur = lex.next();
            else throw new RuntimeException("Lỗi cú pháp: thiếu " + t);
        }

        int getPrecedence(String op) {
            switch(op) {
                case "||": return 1; case "&&": return 2;
                case "==": case "!=": return 3;
                case "<": case "<=": case ">": case ">=": return 4;
                case "+": case "-": return 5;
                case "*": case "/": case "%": return 6;
                case "^": return 7;
            }
            return 0;
        }

        ASTNode parse() {
            if (cur.type == TokenType.EOF) throw new RuntimeException("Biểu thức trống");
            ASTNode node = parseExpression(0);
            if (cur.type != TokenType.EOF) throw new RuntimeException("Cú pháp thừa ở cuối: " + cur.val);
            return node;
        }

        ASTNode parsePrimary() {
            depth++;
            if (depth > MAX_DEPTH) throw new RuntimeException("Biểu thức quá phức tạp");
            try {
                if (cur.type == TokenType.NUM) {
                    double v = cur.numVal; cur = lex.next();
                    return (x, y, z) -> v;
                }
                if (cur.type == TokenType.ID) {
                    String id = cur.val; cur = lex.next();
                    if (cur.type == TokenType.LPAREN) {
                        cur = lex.next();
                        List<ASTNode> args = new ArrayList<>();
                        if (cur.type != TokenType.RPAREN) {
                            args.add(parseExpression(0));
                            while (cur.type == TokenType.COMMA) {
                                cur = lex.next(); args.add(parseExpression(0));
                            }
                        }
                        eat(TokenType.RPAREN);
                        return buildFunction(id, args);
                    } else {
                        switch (id) {
                            case "x": usesX = true; return (x, y, z) -> x;
                            case "y": usesY = true; return (x, y, z) -> y;
                            case "z": usesZ = true; return (x, y, z) -> z;
                            case "pi": return (x, y, z) -> Math.PI;
                            case "e": return (x, y, z) -> Math.E;
                            default: throw new RuntimeException("Biến không hợp lệ: " + id);
                        }
                    }
                }
                if (cur.type == TokenType.OP && cur.val.equals("-")) {
                    cur = lex.next(); ASTNode n = parsePrimary();
                    return (x, y, z) -> -((Double) n.eval(x, y, z));
                }
                if (cur.type == TokenType.OP && cur.val.equals("!")) {
                    cur = lex.next(); ASTNode n = parsePrimary();
                    return (x, y, z) -> !((Boolean) n.eval(x, y, z));
                }
                if (cur.type == TokenType.LPAREN) {
                    cur = lex.next(); ASTNode n = parseExpression(0); eat(TokenType.RPAREN); return n;
                }
                throw new RuntimeException("Cú pháp không hợp lệ tại: " + cur.val);
            } finally { depth--; }
        }

        ASTNode parseExpression(int minPrec) {
            ASTNode left = parsePrimary();
            while (cur.type == TokenType.OP && getPrecedence(cur.val) >= minPrec) {
                String op = cur.val; int prec = getPrecedence(op); cur = lex.next();
                if (minPrec == 0) {
                    rootOp = op;
                    if (op.equals("&&")) multipleEq = true;
                }
                int nextMinPrec = op.equals("^") ? prec : prec + 1;
                ASTNode right = parseExpression(nextMinPrec);
                left = buildBinaryOp(op, left, right);
            }
            return left;
        }

        ASTNode buildBinaryOp(String op, ASTNode l, ASTNode r) {
            return (x, y, z) -> {
                Object v1 = l.eval(x, y, z);
                if (op.equals("||")) {
                    if (!(v1 instanceof Boolean)) throw new RuntimeException("|| cần boolean");
                    if ((Boolean) v1) return true;
                    Object v2 = r.eval(x, y, z);
                    return (Boolean) v2;
                }
                if (op.equals("&&")) {
                    if (!(v1 instanceof Boolean)) throw new RuntimeException("&& cần boolean");
                    if (!(Boolean) v1) return false;
                    Object v2 = r.eval(x, y, z);
                    return (Boolean) v2;
                }
                Object v2 = r.eval(x, y, z);
                if (v1 instanceof Double && v2 instanceof Double) {
                    double d1 = (Double) v1, d2 = (Double) v2;
                    switch (op) {
                        case "+": return d1 + d2; case "-": return d1 - d2;
                        case "*": return d1 * d2; case "/": if (d2 == 0) throw new RuntimeException("Chia cho 0"); return d1 / d2;
                        case "%": return d1 % d2;
                        case "^": return Math.pow(d1, d2);
                        case "==": return Math.abs(d1 - d2) < 0.001;
                        case "!=": return Math.abs(d1 - d2) >= 0.001;
                        case "<": return d1 < d2; case "<=": return d1 <= d2;
                        case ">": return d1 > d2; case ">=": return d1 >= d2;
                    }
                } else if (v1 instanceof Boolean && v2 instanceof Boolean) {
                    boolean b1 = (Boolean) v1, b2 = (Boolean) v2;
                    if (op.equals("==")) return b1 == b2;
                    if (op.equals("!=")) return b1 != b2;
                }
                throw new RuntimeException("Toán tử '" + op + "' sai kiểu dữ liệu.");
            };
        }

        ASTNode buildFunction(String name, List<ASTNode> args) {
            int n = args.size();
            switch (name) {
                case "sphere": case "cylinder": case "cube": case "torus":
                    usesX = usesY = usesZ = true; break;
                case "abs": case "sqrt": case "sin": case "cos":
                    break;
                default:
                    throw new RuntimeException(net.minecraft.network.chat.Component.translatable("tc_veinminer.error.unknown_function", name).getString());
            }
            return (x, y, z) -> {
                double[] a = new double[n];
                for (int i = 0; i < n; i++) {
                    Object o = args.get(i).eval(x, y, z);
                    if (!(o instanceof Double)) {
                        throw new RuntimeException(net.minecraft.network.chat.Component.translatable("tc_veinminer.error.invalid_argument_type", name, (i + 1)).getString());
                    }
                    a[i] = (Double) o;
                }
                switch (name) {
                    case "abs":    return Math.abs(a[0]);
                    case "sqrt":   return Math.sqrt(a[0]);
                    case "sin":    return Math.sin(a[0]);
                    case "cos":    return Math.cos(a[0]);
                    case "sphere": return Math.sqrt(x*x + y*y + z*z) <= a[0];
                    case "cube":   return Math.abs(x) <= a[0]/2.0 && Math.abs(y) <= a[0]/2.0 && Math.abs(z) <= a[0]/2.0;
                    default: throw new RuntimeException(net.minecraft.network.chat.Component.translatable("tc_veinminer.error.unknown_function", name).getString()); // unreachable, but compiler requires it
                }
            };
        }
    }
}
