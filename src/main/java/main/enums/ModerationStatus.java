package main.enums;

public enum ModerationStatus
{
    NEW("New"),
    ACCEPTED("Accepted"),
    DECLINED("Declined");

    private final String TEXT_MESSAGE;

    private ModerationStatus(String text) {
        this.TEXT_MESSAGE = text;
    }
    public String getTextMessage() {
        return TEXT_MESSAGE;
    }
}
