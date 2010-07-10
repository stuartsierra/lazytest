package lazytest;

public class ExpectationFailed extends Error {
    public final Object reason;

    public ExpectationFailed(Object reason) {
	this.reason = reason;
    }
}
