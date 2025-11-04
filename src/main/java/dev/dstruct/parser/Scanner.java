package dev.dstruct.parser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;

public class Scanner {

	private final ReadableByteChannel read;
	private final ByteBuffer byteBuffer;
	private byte current = '\0';
	private final List<Token> tokens = new ArrayList<>();
	private int parenthesisCount = 0;
	private boolean endOfStream = false;

	public Scanner(ReadableByteChannel read) {
		this.read = read;
		this.byteBuffer = ByteBuffer.allocate(8196).flip();
	}

	static final byte BOOLEAN_CHAR = '#';
	static final byte INTEGER_CHAR = ':';
	static final byte DOUBLE_CHAR = ',';
	static final byte STRING_CHAR = '+';

	public void parse() {
		while (!isAtEnd()) {
			skipSpaces();
			this.current = advance();
			if (current == '\0') break;

			if (current == '(') {
				parenthesisCount++;
				tokens.add(new Token(TokenType.OPEN_PARENTHESIS, null));
			}
			else if (current == ')') {
				parenthesisCount--;
				tokens.add(new Token(TokenType.CLOSE_PARENTHESIS, null));
			}
			else if (current == '\r' || current == '\n') {
				tokens.add(new Token(TokenType.ENDLINE, null));
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

		if (parenthesisCount > 0) {
			throw new ParseException("missing closing parenthesis");
		}
		else if (parenthesisCount < 0) {
			throw new ParseException("too much closing parenthesis");
		}

	}

	public List<Token> getTokens() {
		return tokens;
	}

	private void skipSpaces() {
		while (!isAtEnd() && isSpace(peek())) advance();
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
			else if (parenthesisCount > 0 && c == ')') {
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
		if (parenthesisCount > 0 && closeParenthesis) {
			tokens.add(new Token(TokenType.CLOSE_PARENTHESIS, null));
			parenthesisCount--;
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
		return !ensureBuffer();
	}

	private byte advance() {
		if (isAtEnd()) {
			return '\0';
		}
		return byteBuffer.get();
	}

	private byte peek() {
		if (isAtEnd()) {
			return '\0';
		}
		return byteBuffer.get(byteBuffer.position());
	}

	private boolean ensureBuffer() {
		if (byteBuffer.hasRemaining()) {
			return true;
		}

		if (endOfStream) {
			return false;
		}

		try {
			byteBuffer.clear();
			int bytesRead = read.read(byteBuffer);
			byteBuffer.flip();

			if (bytesRead == -1 || bytesRead == 0) {
				endOfStream = true;
				return false;
			}

			return byteBuffer.hasRemaining();
		}
		catch (IOException ioe) {
			throw new ParseException(ioe.getMessage());
		}
	}

	private boolean isSpace(byte b) {
		return Character.isSpaceChar((char)b) || b == '\t';
	}

}
