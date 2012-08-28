package us.rddt.IRCBot.Implementations;

/**
 * Keep a history of announces as well as do throttling
 * @author Milton Thomas
 *
 */
public class RecentAnnounces {
	private String user;
	private int titleId;
	private String title;
	
	public RecentAnnounces(int titleId, String user, String title) {
		this.user = user;
		this.titleId = titleId;
		this.title = title;
	}

	public String getUser() {
		return user;
	}

	public int getTitleId() {
		return titleId;
	}

	public String getTitle() {
		return title;
	}
}
