package lazytest;

public class ExpectationFailed extends AssertionError {
    public final Object reason;

    public ExpectationFailed(Object reason) {
	this.reason = reason;
    }
}
