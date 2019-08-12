


public class Action {
	
	
	public Intent action;
	public int x;
	public int y;

	public Action(Intent action, int x, int y) {
		this.action = action;
		this.x = x;
		this.y = y;
	}
	
	public Action(Intent action) {
		this.action = action;
		this.x = this.y = 1;
	}

	public String toString() {
		String retString = this.action + " ";
		if (this.action != Intent.LEAVE) {
			retString = retString + "(" + this.x + "," + this.y + ")";
		}
		return retString;
		
	}
}
