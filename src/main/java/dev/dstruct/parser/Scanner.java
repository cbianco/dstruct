package dev.dstruct.parser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

public class Scanner {

	private final ReadableByteChannel read;
	private final ByteBuffer byteBuffer = ByteBuffer.allocate(8196).flip();
	private int size = Integer.MIN_VALUE;
	private byte current = '\0';
	private final List<Token> tokens = new ArrayList<>();
	private boolean parenthesisOpen = false;

	public Scanner(ReadableByteChannel read) {
		this.read = read;
	}

	static final byte BOOLEAN_CHAR = '#';
	static final byte INTEGER_CHAR = ':';
	static final byte DOUBLE_CHAR = ',';
	static final byte STRING_CHAR = '+';

	public void parse() {
		while (!isAtEnd()) {
			this.current = advance();
			if (current == '(') {
				parenthesisOpen = true;
				tokens.add(new Token(TokenType.OPEN_PARENTHESIS, null));
			}
			else if (current == ')') {
				parenthesisOpen = false;
				tokens.add(new Token(TokenType.CLOSE_PARENTHESIS, null));
			}
			else if (current == '\r' || current == '\n') {
				tokens.add(new Token(TokenType.ENDLINE, null));
				continue;
			}
			if (Character.isWhitespace((char)this.current)) {
				continue;
			}
			if (isTypeChar(this.current)) {
				tokenType();
			}
			else if (
				this.current == '-' ||
				this.current == '+' ||
				this.current == '"' ||
				this.current == '\'' ||
				Character.isLetterOrDigit(((char)this.current))
			) {
				text();
			}
		}
	}

	public List<Token> getTokens() {
		return tokens;
	}

	private void text() {
		StringBuilder sb = new StringBuilder();
		char quote = '\0';
		boolean quoteClosed = false;
		boolean closeParenthesis = false;
		if (this.current == '"' || this.current == '\'') {
			quote = (char)this.current;
		}
		else {
			sb.append((char) this.current);
		}
		while (!isAtEnd()) {
			byte c = advance();
			if (quote != '\0') {
				if (c == quote) {
					quoteClosed = true;
					break;
				}
			}
			else if (parenthesisOpen && c == ')') {
				closeParenthesis = true;
				break;
			}
			else {
				if (Character.isWhitespace((char)c)) {
					break;
				}
			}
			sb.append((char)c);
		}
		if (quote != '\0' && !quoteClosed) {
			throw new ParseException("missing closing quote: " + quote);
		}
		tokens.add(new Token(TokenType.TEXT, sb.toString()));
		if (parenthesisOpen && closeParenthesis) {
			tokens.add(new Token(TokenType.CLOSE_PARENTHESIS, null));
			parenthesisOpen = false;
		}
	}

	private void tokenType() {
		TokenType tokenType = switch (this.current) {
			case BOOLEAN_CHAR -> TokenType.BOOLEAN_TYPE;
			case DOUBLE_CHAR -> TokenType.DOUBLE_TYPE;
			case INTEGER_CHAR -> TokenType.INTEGER_TYPE;
			case STRING_CHAR -> TokenType.STRING_TYPE;
			default -> null;
		};
		tokens.add(new Token(tokenType, null));
	}

	private boolean isTypeChar(byte b) {
		return b == BOOLEAN_CHAR || b == INTEGER_CHAR || b == DOUBLE_CHAR || b == STRING_CHAR;
	}

	private boolean isAtEnd() {
		if (size == Integer.MIN_VALUE) {
			return false;
		}
		return !byteBuffer.hasRemaining() && (size == -1 || size == 0);
	}

	private byte advance() {
		try {
			if (isAtEnd())
				return '\0';
			if (!byteBuffer.hasRemaining()) {
				byteBuffer.clear();
				this.size = read.read(byteBuffer);
				byteBuffer.flip();
				if (isAtEnd())
					return '\0';
			}
			return byteBuffer.get();
		}
		catch (IOException ioe) {
			throw new ParseException(ioe.getMessage());
		}
	}

}
