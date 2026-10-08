package toy.pki.ca.domain.policy;

/**
 * @param required  필수 여부
 * @param fixedValue  고정 값 (null이면 고정 값 없음) */
public record DnAttributePolicy(
    boolean required,
    String fixedValue) {
    //    private final String defaultValue;

    public DnAttributePolicy(boolean required) {
        this(required, null);
    }

    public DnAttributePolicy(String fixedValue) {
        this(false, fixedValue);
    }
}
