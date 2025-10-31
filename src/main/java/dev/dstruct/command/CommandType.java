package dev.dstruct.command;

/* generated at 2025-10-30T13:52:51.817190Z */
public enum CommandType {
	SADD,
	VSET,
	LLEN,
	VDELETE,
	SMEMBERS,
	LPOP,
	RPUSH,
	DEL,
	MGET,
	VGET,
	LPUSH,
	PING,
	TYPE,
	SREM,
	RPOP,
	MDELETE,
	CAST,
	LINDEX,
	MPUT,
	BATCH;

	public static CommandType ivalueOf(String name) {
		for (CommandType value : values()) {
			if (value.name().equalsIgnoreCase(name)) {
				return value;
			}
		}
		return null;
	}
}
