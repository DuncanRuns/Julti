package xyz.duncanruns.julti.command;

public class CommandFailedException extends RuntimeException {

    public CommandFailedException(String message) {
        super(message);
    }

    public CommandFailedException(Throwable throwable) {
        super(throwable);
    }

    @Override
    public String toString() {
        String s = "CommandFailedException";
        String message = this.getLocalizedMessage();
        return (message != null) ? (s + ": " + message) : s;
    }
}
