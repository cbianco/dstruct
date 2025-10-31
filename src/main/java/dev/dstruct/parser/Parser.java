package dev.dstruct.parser;

import dev.dstruct.command.Command;
import dev.dstruct.command.Command.Cast;
import dev.dstruct.command.Command.Del;
import dev.dstruct.command.Command.LIndex;
import dev.dstruct.command.Command.LLen;
import dev.dstruct.command.Command.LPop;
import dev.dstruct.command.Command.LPush;
import dev.dstruct.command.Command.MDelete;
import dev.dstruct.command.Command.MGet;
import dev.dstruct.command.Command.MPut;
import dev.dstruct.command.Command.Ping;
import dev.dstruct.command.Command.RPop;
import dev.dstruct.command.Command.RPush;
import dev.dstruct.command.Command.SAdd;
import dev.dstruct.command.Command.SMembers;
import dev.dstruct.command.Command.SRem;
import dev.dstruct.command.Command.Type;
import dev.dstruct.command.Command.VDelete;
import dev.dstruct.command.Command.VGet;
import dev.dstruct.command.Command.VSet;
import dev.dstruct.command.CommandType;
import dev.dstruct.util.Binaries;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Parses DStruct command syntax into Command objects.
 *
 * <p>The parser supports two main constructs:
 * <ul>
 *   <li><b>Commands:</b> Database operations like VSET, MPUT, LPUSH, etc.
 *   <li><b>Cast expressions:</b> Type-wrapped commands in the form: TYPE(Cast COMMAND)
 * </ul>
 *
 * <p><b>Grammar:</b>
 * <pre>
 * program        = { line }
 * line           = ( cast_expression | command ) ENDLINE
 * cast_expression = type_token "(" "Cast" type_token command ")"
 * command        = command_name [ name ] { value }
 * value          = TEXT | typed_value
 * typed_value    = type_token TEXT
 * type_token     = "STRING_TYPE" | "INTEGER_TYPE" | "BOOLEAN_TYPE" | "DOUBLE_TYPE"
 * </pre>
 *
 * <p><b>Example usage:</b>
 * <pre>
 * Scanner scanner = new Scanner(input);
 * scanner.parse();
 * List&lt;Token&gt; tokens = scanner.getTokens();
 * Parser parser = new Parser(tokens);
 * parser.parse();
 * List&lt;Command&gt; commands = parser.getCommands();
 * </pre>
 *
 * @throws ParseException if the token stream doesn't conform to the grammar
 */
public class Parser {

	private final List<Command> commands = new ArrayList<>();
	private final ArrayDeque<Token> tokens;

	public Parser(List<Token> tokens) {
		this.tokens = new ArrayDeque<>(Objects.requireNonNull(tokens, "tokens is null"));
	}

	public void parse() {
		try {
			if (tokens.isEmpty()) return;
			while (!isAtEnd()) {
				line();
			}
		}
		catch (ParseException parseException) {
			throw parseException;
		}
		catch (Exception e) {
			throw new ParseException(e.getMessage());
		}
	}

	public List<Command> getCommands() {
		return commands;
	}

	private void line() {
		if (isTypeToken()) {
			Token typeToken = advance();
			consume(TokenType.OPEN_PARENTHESIS);
			tokens.addFirst(typeToken);
			tokens.addFirst(new Token(TokenType.TEXT, "Cast"));
			commands.add(command());
			consume(TokenType.CLOSE_PARENTHESIS);
		}
		else {
			commands.add(command());
		}
		consume(TokenType.ENDLINE);
		consumeAll(TokenType.ENDLINE);
	}

	private boolean isTypeToken() {
		Token current = peek();
		if (current == null) return false;
		TokenType tokenType = current.tokenType();
		return switch (tokenType) {
			case BOOLEAN_TYPE, INTEGER_TYPE, STRING_TYPE, DOUBLE_TYPE -> true;
			default -> false;
		};
	}

	private int type(TokenType tokenType) {
		return switch (tokenType) {
			case STRING_TYPE -> 0;
			case INTEGER_TYPE -> 1;
			case BOOLEAN_TYPE -> 2;
			case DOUBLE_TYPE -> 3;
			default -> -1;
		};
	}

	private Command command() {
		if (match(TokenType.TEXT)) {
			Token commandToken = advance();
			String commandName = commandToken.text();
			CommandType commandType = CommandType.ivalueOf(commandName);

			if (commandType == null) {
				throw new ParseException("command " + commandName + " not supported");
			}

			return switch (commandType) {
				case MPUT -> mput();
				case MGET -> mget();
				case VSET -> vset();
				case RPUSH -> rpush();
				case SADD -> sadd();
				case MDELETE -> mdelete();
				case TYPE -> type();
				case BATCH -> batch();
				case RPOP -> rpop();
				case VDELETE -> vdelete();
				case LPOP -> lpop();
				case LPUSH -> lpush();
				case LINDEX -> lindex();
				case SREM -> srem();
				case LLEN -> llen();
				case DEL -> del();
				case VGET -> vget();
				case SMEMBERS -> smembers();
				case CAST -> cast();
				case PING -> ping();
			};
		}
		throw new ParseException("command not found");
	}

	private Command ping() {
		if (match(TokenType.TEXT)) {
			Token message = advance();
			return new Ping(message.text());
		}
		else {
			return new Ping(null);
		}
	}

	private Command cast() {
		if (!isTypeToken()) throw new ParseException("parse error");
		Token typeToken = advance();
		int type = type(typeToken.tokenType());
		return new Cast(type, command());
	}

	private Command smembers() {
		if (!match(TokenType.TEXT)) throw new ParseException("parse error");
		Token name = advance();
		return new SMembers(name.text());
	}

	private Command vget() {
		if (!match(TokenType.TEXT)) throw new ParseException("parse error");
		Token name = advance();
		return new VGet(name.text());
	}

	private Command del() {
		if (!match(TokenType.TEXT)) throw new ParseException("parse error");
		Token name = advance();
		return new Del(name.text());
	}

	private Command llen() {
		if (!match(TokenType.TEXT)) throw new ParseException("parse error");
		Token name = advance();
		return new LLen(name.text());
	}

	private Command srem() {
		if (!match(TokenType.TEXT)) throw new ParseException("parse error");
		Token name = advance();
		byte[] value = value();
		return new SRem(name.text(), value);
	}

	private Command lindex() {
		if (!match(TokenType.TEXT)) throw new ParseException("parse error");
		Token name = advance();
		byte[] value = value();
		return new LIndex(name.text(), value);
	}

	private Command lpush() {
		if (!match(TokenType.TEXT)) throw new ParseException("parse error");
		Token name = advance();
		byte[] value = value();
		return new LPush(name.text(), value);
	}

	private Command lpop() {
		if (!match(TokenType.TEXT)) throw new ParseException("parse error");
		Token name = advance();
		return new LPop(name.text());
	}

	private Command mget() {
		if (!match(TokenType.TEXT)) throw new ParseException("parse error");
		Token name = advance();
		byte[] key = value();
		return new MGet(name.text(), key);
	}

	private Command mput() {
		if (!match(TokenType.TEXT)) throw new ParseException("parse error");
		Token name = advance();
		byte[] key = value();
		byte[] value = value();
		return new MPut(name.text(), key, value);
	}

	private Command vdelete() {
		if (!match(TokenType.TEXT)) throw new ParseException("parse error");
		Token name = advance();
		return new VDelete(name.text());
	}

	private Command rpop() {
		if (!match(TokenType.TEXT)) throw new ParseException("parse error");
		Token name = advance();
		return new RPop(name.text());
	}

	private Command batch() {
		return null;
	}

	private Command type() {
		if (!match(TokenType.TEXT)) throw new ParseException("parse error");
		Token name = advance();
		return new Type(name.text());
	}

	private Command mdelete() {
		if (!match(TokenType.TEXT)) throw new ParseException("parse error");
		Token name = advance();
		byte[] key = value();
		return new MDelete(name.text(), key);
	}

	private Command sadd() {
		if (!match(TokenType.TEXT)) throw new ParseException("parse error");
		Token name = advance();
		byte[] value = value();
		return new SAdd(name.text(), value);
	}

	private Command rpush() {
		if (!match(TokenType.TEXT)) throw new ParseException("parse error");
		Token name = advance();
		byte[] value = value();
		return new RPush(name.text(), value);
	}

	private Command vset() {
		if (!match(TokenType.TEXT)) throw new ParseException("parse error");
		Token name = advance();
		byte[] value = value();
		return new VSet(name.text(), value);
	}

	private byte[] value() {
		Token current = advance();
		return switch (current.tokenType()) {
			case TEXT -> current.text().getBytes(StandardCharsets.UTF_8);
			case BOOLEAN_TYPE -> booleanType();
			case INTEGER_TYPE -> integerType();
			case STRING_TYPE -> stringType();
			case DOUBLE_TYPE -> doubleType();
			default -> throw new ParseException("parse error");
		};
	}

	private byte[] doubleType() {
		if (!match(TokenType.TEXT)) throw new ParseException("parse error");
		Token current = advance();
		return Binaries.toBytes(Double.parseDouble(current.text()));
	}

	private byte[] stringType() {
		if (!match(TokenType.TEXT)) throw new ParseException("parse error");
		Token current = advance();
		return current.text().getBytes(StandardCharsets.UTF_8);
	}

	private byte[] integerType() {
		if (!match(TokenType.TEXT)) throw new ParseException("parse error");
		Token current = advance();
		return Binaries.toBytes(Integer.parseInt(current.text()));
	}

	private byte[] booleanType() {
		if (!match(TokenType.TEXT)) throw new ParseException("parse error");
		Token current = advance();
		if (current.text().equals("t")) {
			return Binaries.toBytes(true);
		}
		else if (current.text().equals("f")) {
			return Binaries.toBytes(false);
		}
		throw new ParseException("parse error");
	}

	private boolean isAtEnd() {
		return tokens.isEmpty();
	}

	private void consumeAll(TokenType tokenType) {
		while (match(tokenType)) tokens.pop();
	}

	private void consume(TokenType tokenType) {
		if (match(tokenType)) tokens.pop();
		else throw new ParseException("unaspected argument");
	}

	private Token advance() {
		return isAtEnd() ? null : tokens.pop();
	}

	private Token peek() {
		return isAtEnd() ? null : tokens.getFirst();
	}

	private boolean match(TokenType tokenType) {
		Token current = peek();
		if (current == null) return false;
		return current.tokenType() == tokenType;
	}

}
