package ma.emsi.tp1saadmosaif.llm;

public class RequeteException extends RuntimeException {
    public RequeteException(String message, String prettyPrinting) {
      super(message + "\n" + prettyPrinting);
    }
}
