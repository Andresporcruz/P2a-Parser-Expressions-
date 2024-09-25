package plc.project;

public class Token {
    public enum Type {
        IDENTIFIER, OPERATOR, INTEGER, DECIMAL, CHARACTER, STRING
    }

    private final Type type;
    private final String literal;
    private final int index;

    public Token(Type type, String literal, int index) {
        this.type = type;
        this.literal = literal;
        this.index = index;
    }

    public Type getType() {
        return type;
    }

    public String getLiteral() {
        return literal;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Token)) {
            return false;
        }
        Token other = (Token) obj;
        return type == other.type && literal.equals(other.literal) && index == other.index;
    }

    @Override
    public String toString() {
        return type + " " + literal + " @ " + index;
    }
}
