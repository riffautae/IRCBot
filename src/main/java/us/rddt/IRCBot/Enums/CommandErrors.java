package us.rddt.IRCBot.Enums;

public enum CommandErrors {
	WRONG_PERMISSIONS("You do not have access to that.", false),
	NOT_ON_SELF("You are not allowed to use that on yourself.", false);
	
	public final String response;
	public final Boolean inChan;
	
	CommandErrors(String response, boolean inChan) {
		this.response = response;
		this.inChan = inChan;
	}
	
	public String toString() {
		return response;
	}
}
