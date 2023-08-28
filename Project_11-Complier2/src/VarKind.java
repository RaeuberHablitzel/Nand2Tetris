import java.util.Locale;

public enum VarKind {
    STATIC ,
    FIELD,
    ARGUMENT,
    LOCAL;

    @Override
    public String toString() {
        if (this.name().equals("FIELD"))
            return "this";
        else
            return this.name().toLowerCase(Locale.ROOT);
    }
}
