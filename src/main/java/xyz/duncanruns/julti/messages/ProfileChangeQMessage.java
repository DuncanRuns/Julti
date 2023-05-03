package xyz.duncanruns.julti.messages;

public class ProfileChangeQMessage extends QMessage {

    private final String profileName;

    public ProfileChangeQMessage(String profileName) {
        this.profileName = profileName;
    }

    public String getProfileName() {
        return this.profileName;
    }
}
