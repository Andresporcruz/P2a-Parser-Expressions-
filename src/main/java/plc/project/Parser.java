package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have its own function, and reference to other rules correspond
 * to calling that function.
 */
//Andres Portillo
//Sep 22nd, 2024
//Implemented Parser.java based on the recursive descent method. The aim is to parse tokens generated by the lexer into an AST structure.

public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    // Grammar Rules Implementation

    /**
     * Parses the {@code source} rule.
     * This will be implemented in Part 2b.
     */
    public Ast.Source parseSource() {
        throw new UnsupportedOperationException(); // Will be implemented in Part 2b
    }

    /**
     * Parses a statement. This will either be an assignment or an expression
     * statement based on the presence of an equal sign.
     */
    public Ast.Stmt parseStatement() {
        Ast.Expr expr = parseExpression();
        if (match("=")) {
            // It's an assignment statement
            Ast.Expr value = parseExpression();
            if (!match(";")) {
                throw new ParseException("Expected ';' after assignment.",
                        tokens.has(0) ? tokens.get(0).getIndex()
                                : tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
            if (!(expr instanceof Ast.Expr.Access)) {
                throw new ParseException("Invalid assignment target.",
                        getErrorIndex());
            }
            return new Ast.Stmt.Assignment(expr, value);
        } else {
            // It's an expression statement
            if (!match(";")) {
                throw new ParseException("Expected ';' after expression.",
                        tokens.has(0) ? tokens.get(0).getIndex()
                                : tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
            return new Ast.Stmt.Expression(expr);
        }
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expr parseExpression() {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expr parseLogicalExpression() {
        Ast.Expr left = parseComparisonExpression();
        while (match("AND", "OR")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expr right = parseComparisonExpression();
            left = new Ast.Expr.Binary(operator, left, right);
        }
        return left;
    }

    /**
     * Parses the {@code comparison-expression} rule.
     */
    public Ast.Expr parseComparisonExpression() {
        Ast.Expr left = parseAdditiveExpression();
        while (match("<", "<=", ">", ">=", "==", "!=")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expr right = parseAdditiveExpression();
            left = new Ast.Expr.Binary(operator, left, right);
        }
        return left;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expr parseAdditiveExpression() {
        Ast.Expr left = parseMultiplicativeExpression();
        while (match("+", "-")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expr right = parseMultiplicativeExpression();
            left = new Ast.Expr.Binary(operator, left, right);
        }
        return left;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expr parseMultiplicativeExpression() {
        Ast.Expr left = parseSecondaryExpression();
        while (match("*", "/")) {
            String operator = tokens.get(-1).getLiteral();
            Ast.Expr right = parseSecondaryExpression();
            left = new Ast.Expr.Binary(operator, left, right);
        }
        return left;
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expr parseSecondaryExpression() {
        Ast.Expr expr = parsePrimaryExpression();
        while (match(".")) {
            if (!match(Token.Type.IDENTIFIER)) {
                throw new ParseException("Expected identifier after '.'.",
                        tokens.has(0) ? tokens.get(0).getIndex()
                                : tokens.get(-1).getIndex());
            }
            String name = tokens.get(-1).getLiteral();
            if (peek("(")) {
                match("(");
                List<Ast.Expr> arguments = parseArguments();
                expr = new Ast.Expr.Function(Optional.of(expr), name, arguments);
            } else {
                expr = new Ast.Expr.Access(Optional.of(expr), name);
            }
        }
        return expr;
    }

    /**
     * Parses the {@code primary-expression} rule.
     */
    public Ast.Expr parsePrimaryExpression() {
        if (match("NIL")) {
            return new Ast.Expr.Literal(null);
        } else if (match("TRUE")) {
            return new Ast.Expr.Literal(true);
        } else if (match("FALSE")) {
            return new Ast.Expr.Literal(false);
        } else if (match(Token.Type.INTEGER)) {
            return new Ast.Expr.Literal(new BigInteger(tokens.get(-1).getLiteral()));
        } else if (match(Token.Type.DECIMAL)) {
            return new Ast.Expr.Literal(new BigDecimal(tokens.get(-1).getLiteral()));
        } else if (match(Token.Type.CHARACTER)) {
            String literal = tokens.get(-1).getLiteral();
            char value = processCharacterLiteral(literal);
            return new Ast.Expr.Literal(value);
        } else if (match(Token.Type.STRING)) {
            String literal = tokens.get(-1).getLiteral();
            String value = processStringLiteral(literal);
            return new Ast.Expr.Literal(value);
        } else if (match(Token.Type.IDENTIFIER)) {
            String name = tokens.get(-1).getLiteral();
            if (peek("(")) {
                match("(");
                List<Ast.Expr> arguments = parseArguments();
                return new Ast.Expr.Function(Optional.empty(), name, arguments);
            } else {
                return new Ast.Expr.Access(Optional.empty(), name);
            }
        } else if (match("(")) {
            Ast.Expr expr = parseExpression();
            if (!match(")")) {
                throw new ParseException("Expected ')'.",
                        tokens.has(0) ? tokens.get(0).getIndex()
                                : tokens.get(-1).getIndex());
            }
            return new Ast.Expr.Group(expr);
        } else {
            throw new ParseException("Expected primary expression.",
                    tokens.has(0) ? tokens.get(0).getIndex() : -1);
        }
    }

    /**
     * Parses function call arguments and ensures proper grouping.
     */
    private List<Ast.Expr> parseArguments() {
        List<Ast.Expr> arguments = new ArrayList<>();
        if (!match(")")) {
            do {
                arguments.add(parseExpression());
            } while (match(","));
            if (!match(")")) {
                throw new ParseException("Expected ')'.",
                        tokens.has(0) ? tokens.get(0).getIndex()
                                : tokens.get(-1).getIndex());
            }
        }
        return arguments;
    }

    /**
     * Processes the character literal, handling escape sequences.
     */
    private char processCharacterLiteral(String literal) {
        // Remove the surrounding single quotes
        String content = literal.substring(1, literal.length() - 1);
        if (content.length() == 1) {
            return content.charAt(0);
        } else if (content.startsWith("\\")) {
            switch (content.charAt(1)) {
                case 'b': return '\b';
                case 'n': return '\n';
                case 'r': return '\r';
                case 't': return '\t';
                case '\'': return '\'';
                case '"': return '"';
                case '\\': return '\\';
                default:
                    throw new ParseException("Invalid escape sequence in character literal.",
                            tokens.get(-1).getIndex());
            }
        } else {
            throw new ParseException("Invalid character literal.",
                    tokens.get(-1).getIndex());
        }
    }

    /**
     * Processes the string literal, handling escape sequences.
     */
    private String processStringLiteral(String literal) {
        // Remove the surrounding double quotes
        String content = literal.substring(1, literal.length() - 1);
        // Replace escape sequences
        content = content.replace("\\b", "\b")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\'", "'")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
        return content;
    }

    /**
     * Helper method to get the index for error reporting.
     */
    private int getErrorIndex() {
        if (tokens.has(0)) {
            return tokens.get(0).getIndex();
        } else if (tokens.has(-1)) {
            return tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length();
        } else {
            return 0; // Default to index 0 if no tokens are available
        }
    }

    // Updated peek and match methods

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     */
    private boolean peek(Object... patterns) {
        if (!tokens.has(0)) {
            return false;
        }
        Token token = tokens.get(0);
        for (Object pattern : patterns) {
            if (pattern instanceof Token.Type) {
                if (token.getType() == pattern) {
                    return true;
                }
            } else if (pattern instanceof String) {
                if (token.getLiteral().equals(pattern)) {
                    return true;
                }
            } else {
                throw new AssertionError("Invalid pattern object: " + pattern);
            }
        }
        return false;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        if (peek(patterns)) {
            tokens.advance();
            return true;
        }
        return false;
    }

    // TokenStream Class

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset >= 0 && index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
