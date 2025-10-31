package dev.dstruct.command;
import java.util.List;

/* generated at 2025-10-30T13:52:51.817190Z */
public sealed interface Command {
	default String name() {return "";}
	default boolean isPersisted() { return true; }
	interface Visitor<R> {
		R visitMPutCommand(MPut command);
		R visitMDeleteCommand(MDelete command);
		R visitMGetCommand(MGet command);
		R visitVSetCommand(VSet command);
		R visitVDeleteCommand(VDelete command);
		R visitVGetCommand(VGet command);
		R visitLPushCommand(LPush command);
		R visitLPopCommand(LPop command);
		R visitRPushCommand(RPush command);
		R visitRPopCommand(RPop command);
		R visitLLenCommand(LLen command);
		R visitLIndexCommand(LIndex command);
		R visitSAddCommand(SAdd command);
		R visitSRemCommand(SRem command);
		R visitSMembersCommand(SMembers command);
		R visitDelCommand(Del command);
		R visitTypeCommand(Type command);
		R visitBatchCommand(Batch command);
		R visitCastCommand(Cast command);
		R visitPingCommand(Ping command);
	}
	record MPut(String name, byte[] key, byte[] value) implements Command {
		@Override
		public <R> R accept(Visitor<R> visitor) {
		  return visitor.visitMPutCommand(this);
		}
	}
	record MDelete(String name, byte[] key) implements Command {
		@Override
		public <R> R accept(Visitor<R> visitor) {
		  return visitor.visitMDeleteCommand(this);
		}
	}
	record MGet(String name, byte[] key) implements Command {
		@Override
		public <R> R accept(Visitor<R> visitor) {
		  return visitor.visitMGetCommand(this);
		}
		@Override
		public boolean isPersisted() {
			return false;
		}
	}
	record VSet(String name, byte[] value) implements Command {
		@Override
		public <R> R accept(Visitor<R> visitor) {
		  return visitor.visitVSetCommand(this);
		}
	}
	record VDelete(String name) implements Command {
		@Override
		public <R> R accept(Visitor<R> visitor) {
		  return visitor.visitVDeleteCommand(this);
		}
	}
	record VGet(String name) implements Command {
		@Override
		public <R> R accept(Visitor<R> visitor) {
		  return visitor.visitVGetCommand(this);
		}
		@Override
		public boolean isPersisted() {
			return false;
		}
	}
	record LPush(String name, byte[] value) implements Command {
		@Override
		public <R> R accept(Visitor<R> visitor) {
		  return visitor.visitLPushCommand(this);
		}
	}
	record LPop(String name) implements Command {
		@Override
		public <R> R accept(Visitor<R> visitor) {
		  return visitor.visitLPopCommand(this);
		}
	}
	record RPush(String name, byte[] value) implements Command {
		@Override
		public <R> R accept(Visitor<R> visitor) {
		  return visitor.visitRPushCommand(this);
		}
	}
	record RPop(String name) implements Command {
		@Override
		public <R> R accept(Visitor<R> visitor) {
		  return visitor.visitRPopCommand(this);
		}
	}
	record LLen(String name) implements Command {
		@Override
		public <R> R accept(Visitor<R> visitor) {
		  return visitor.visitLLenCommand(this);
		}
		@Override
		public boolean isPersisted() {
			return false;
		}
	}
	record LIndex(String name, byte[] index) implements Command {
		@Override
		public <R> R accept(Visitor<R> visitor) {
		  return visitor.visitLIndexCommand(this);
		}
		@Override
		public boolean isPersisted() {
			return false;
		}
	}
	record SAdd(String name, byte[] value) implements Command {
		@Override
		public <R> R accept(Visitor<R> visitor) {
		  return visitor.visitSAddCommand(this);
		}
	}
	record SRem(String name, byte[] value) implements Command {
		@Override
		public <R> R accept(Visitor<R> visitor) {
		  return visitor.visitSRemCommand(this);
		}
	}
	record SMembers(String name) implements Command {
		@Override
		public <R> R accept(Visitor<R> visitor) {
		  return visitor.visitSMembersCommand(this);
		}
		@Override
		public boolean isPersisted() {
			return false;
		}
	}
	record Del(String name) implements Command {
		@Override
		public <R> R accept(Visitor<R> visitor) {
		  return visitor.visitDelCommand(this);
		}
	}
	record Type(String name) implements Command {
		@Override
		public <R> R accept(Visitor<R> visitor) {
		  return visitor.visitTypeCommand(this);
		}
		@Override
		public boolean isPersisted() {
			return false;
		}
	}
	record Batch(List<Command> commands) implements Command {
		@Override
		public <R> R accept(Visitor<R> visitor) {
		  return visitor.visitBatchCommand(this);
		}
	}
	record Cast(int type, Command command) implements Command {
		@Override
		public <R> R accept(Visitor<R> visitor) {
		  return visitor.visitCastCommand(this);
		}
		@Override
		public boolean isPersisted() {
			return false;
		}
	}
	record Ping(String message) implements Command {
		@Override
		public <R> R accept(Visitor<R> visitor) {
		  return visitor.visitPingCommand(this);
		}
		@Override
		public boolean isPersisted() {
			return false;
		}
	}
	<R> R accept(Visitor<R> visitor);
}
