package net.marfgamer.raknet.example.chat.server.command;

public abstract class Command {

	private final boolean overridable;
	private final String label;
	private final String usage;

	protected Command(boolean overridable, String label, String usage) {
		this.overridable = overridable;
		this.label = label;
		this.usage = usage;
	}

	public Command(String name, String usage) {
		this(true, name, usage);
	}

	protected boolean isOverridable() {
		return this.overridable;
	}

	public String getLabel() {
		return this.label;
	}

	public String getUsage() {
		return this.usage;
	}

	protected String remainingArguments(int startIndex, String[] stringArray) {
		StringBuilder builder = new StringBuilder();
		for (int i = startIndex; i < stringArray.length; i++) {
			builder.append(stringArray[i] + (i + 1 < stringArray.length ? " " : ""));
		}
		return builder.toString();
	}

	public abstract boolean handleCommand(String[] args);

}
