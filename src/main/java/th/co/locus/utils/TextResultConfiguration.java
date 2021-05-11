package th.co.locus.utils;

public enum TextResultConfiguration {
	FROM(0),
	DISPLAY_NAME(1),
	TO(2),
	CC(3),
	BCC(4),
	SUBJECT(5),
	BODY(6),
	BODY_FORMAT(7);
	
	public final Integer index;
	private TextResultConfiguration(Integer index) {
		this.index = index;
	}
	
}
