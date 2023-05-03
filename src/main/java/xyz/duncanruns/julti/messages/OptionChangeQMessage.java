package xyz.duncanruns.julti.messages;

public class OptionChangeQMessage extends QMessage {
    private final String optionName;
    private final Object value;

    public OptionChangeQMessage(String optionName, Object value) {
        this.optionName = optionName;
        this.value = value;
    }

    public String getOptionName() {
        return this.optionName;
    }

    public Object getValue() {
        return this.value;
    }
}
