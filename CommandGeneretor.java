/**
 * Simple script that generates dstruct commands in a specific path
 *
 * ```
 * > java CommandGeneretor.java path/to/commands/package
 * ```
 *
 */
static final Set<String> readCommands = Set.of(
	"MGet", "LLen", "LIndex", "Type", "VGet", "SMembers", "Cast", "Ping"
);
static final Set<String> allCommands = Set.of(
	"MPut", "MDelete", "MGet", "VSet", "Cast",
	"VDelete", "LPush", "LPop", "RPush", "RPop",
	"LLen", "LIndex", "SAdd", "SRem", "Del",
	"Type", "Batch", "VGet", "SMembers", "Ping"
);

static final Instant instant = Instant.now();

void main(String[] args) throws IOException {
	if (args.length != 1) {
		System.err.println("Usage: generate_ast <output directory>");
		System.exit(64);
	}
	String outputDir = args[0];
	defineCommands(outputDir, "Command", List.of(
		"MPut     : String name, byte[] key, byte[] value",
		"MDelete  : String name, byte[] key",
		"MGet     : String name, byte[] key",
		"VSet     : String name, byte[] value",
		"VDelete  : String name",
		"VGet     : String name",
		"LPush    : String name, byte[] value",
		"LPop     : String name",
		"RPush    : String name, byte[] value",
		"RPop     : String name",
		"LLen     : String name",
		"LIndex   : String name, byte[] index",
		"SAdd     : String name, byte[] value",
		"SRem     : String name, byte[] value",
		"SMembers : String name",
		"Del      : String name",
		"Type     : String name",
		"Batch    : List<Command> commands",
		"Cast     : int type, Command command",
		"Ping     : String message"
	));

	defineEnum(outputDir, "CommandType", allCommands);

	defineSerde(outputDir, "Serde", List.of(
		"MPut     : String name, byte[] key, byte[] value",
		"MDelete  : String name, byte[] key",
		"MGet     : String name, byte[] key",
		"VSet     : String name, byte[] value",
		"VDelete  : String name",
		"LPush    : String name, byte[] value",
		"LPop     : String name",
		"RPush    : String name, byte[] value",
		"RPop     : String name",
		"SAdd     : String name, byte[] value",
		"SRem     : String name, byte[] value",
		"Del      : String name",
		"Batch    : List<Command> commands"
	));
}

private static void defineEnum(
	String outputDir, String baseName, Set<String> commands) throws IOException {

	String path = outputDir + "/" + baseName + ".java";
	PrintWriter writer = new PrintWriter(path, StandardCharsets.UTF_8);
	writer.println("package dev.dstruct.command;");
	writer.println();
	printGeneratedAt(writer);
	writer.printf("public enum %s {\n", baseName);
	Iterator<String> iterator = commands.iterator();
	while (iterator.hasNext()) {
		String command = iterator.next();
		writer.print('\t');
		writer.print(command.toUpperCase());
		if (iterator.hasNext()) {
			writer.println(',');
		} else {
			writer.println(';');
		}
	}
	writer.println();
	writer.println("\tpublic static CommandType ivalueOf(String name) {");
	writer.println("\t\tfor (CommandType value : values()) {");
	writer.println("\t\t\tif (value.name().equalsIgnoreCase(name)) {");
	writer.println("\t\t\t\treturn value;");
	writer.println("\t\t\t}");
	writer.println("\t\t}");
	writer.println("\t\treturn null;");
	writer.println("\t}");

	writer.println('}');
	writer.close();
}

private static void defineCommands(
	String outputDir, String baseName, List<String> types)
	throws IOException {
	String path = outputDir + "/" + baseName + ".java";
	PrintWriter writer = new PrintWriter(path, StandardCharsets.UTF_8);

	writer.println("package dev.dstruct.command;");
	writer.println("import java.util.List;");
	writer.println();
	printGeneratedAt(writer);
	writer.println("public sealed interface " + baseName + " {");
	writer.println("\tdefault String name() {return \"\";}");
	writer.println("\tdefault boolean isPersisted() { return true; }");
	defineVisitor(writer, baseName, types);
	for (String type : types) {
		String className = type.split(":")[0].trim();
		String fields = type.split(":")[1].trim();
		defineType(writer, baseName, className, fields);
	}
	writer.println("\t<R> R accept(Visitor<R> visitor);");
	writer.println("}");
	writer.close();
}

private static void printGeneratedAt(PrintWriter writer) {
	writer.println("/* generated at " + instant + " */");
}

private static void defineSerde(
	String outputDir, String baseName, List<String> types)
	throws IOException {

	String path = outputDir + "/" + baseName + ".java";
	PrintWriter writer = new PrintWriter(path, StandardCharsets.UTF_8);

	writer.println("package dev.dstruct.command;");
	writer.println();
	writer.println("import dev.dstruct.wal.BufferedChannel;");
	writer.println("import java.nio.ByteBuffer;");
	writer.println("import java.io.IOException;");
	writer.println("import java.nio.charset.StandardCharsets;");
	writer.println("import java.util.List;");
	for (String type : types) {
		String className = type.split(":")[0].trim();
		writer.println("import dev.dstruct.command.Command." + className + ";");
	}
	writer.println();

	printGeneratedAt(writer);
	writer.println("public final class " + baseName + " {");
	writer.println();

	typeMethod(types, writer);

	writer.println(
		"\tpublic static Command deserialize(BufferedChannel bufferedChannel) throws IOException {");
	writer.println("\t\tif (!bufferedChannel.hasRemaining()) return null;");
	writer.println("\t\tshort type = bufferedChannel.getShort();");
	writer.println("\t\treturn switch (type) {");

	for (int i = 0; i < types.size(); i++) {
		String className = types.get(i).split(":")[0].trim();
		if (className.equals("Batch"))
			continue;
		writer.printf("\t\t\tcase %d -> deserialize%s(bufferedChannel);%n", i + 1, className);
	}
	writer.println("\t\t\tdefault -> null;");
	writer.println("\t\t};");
	writer.println("\t}");

	writer.println("\tpublic static ByteBuffer serialize(Command command) {");
	writer.println("\t\tif (command == null) return null;");
	writer.println("\t\treturn switch (command) {");
	for (String s : types) {
		String className = s.split(":")[0].trim();
		writer.printf("\t\t\tcase %s a -> serialize%s(a);%n", className, className);
	}
	writer.println("\t\t\tdefault -> null;");
	writer.println("\t\t};");
	writer.println("\t}");

	for (String type : types) {
		String className = type.split(":")[0].trim();
		String fields = type.split(":")[1].trim();
		if (className.equals("Batch")) {
			serializeBatch(writer);
			deserializeBatch(writer);
		} else {
			serialize(writer, baseName, className, fields);
			deserialize(writer, baseName, className, fields);
		}
	}

	writer.println("}");
	writer.close();

}

private static void deserializeBatch(PrintWriter writer) {
	// nothing
}

private static void serializeBatch(PrintWriter writer) {

	writer.println("""
			private static ByteBuffer serializeBatch(Batch batch) {
				if (batch == null) return null;
				List<Command> commands = batch.commands();
				if (commands == null) return null;
				ByteBuffer[] byteBuffers = new ByteBuffer[commands.size()];
				int capacity = 0;
				for (int i = 0; i < commands.size(); i++) {
					ByteBuffer buffer = serialize(commands.get(i));
					byteBuffers[i] = buffer;
					capacity += buffer.capacity();
				}
				ByteBuffer byteBuffer = ByteBuffer.allocateDirect(capacity);
				for (ByteBuffer buffer : byteBuffers) {
					byteBuffer.put(buffer);
				}
				return byteBuffer.flip();
			}
		""");

}

private static void typeMethod(List<String> types, PrintWriter writer) {
	writer.println("\tpublic static short type(Command command) {");
	writer.println("\t\treturn switch(command) {");
	int t = 1;
	for (String type : types) {
		String className = type.split(":")[0].trim();
		writer.println("\t\t\tcase " + className + " ignore -> " + t++ + ";");
	}
	writer.println("\t\t\tdefault -> -1;");
	writer.println("\t\t};");
	writer.println("\t}");
}

private static void deserialize(PrintWriter writer, String baseName, String className,
	String fields) {
	writer.printf(
		"\tprivate static %s deserialize%s(BufferedChannel bufferedChannel) throws IOException {%n",
		className, className);
	writer.println("\t\tif (bufferedChannel == null) return null;");
	String[] field = fields.split(",");
	writer.println("\t\tbyte[] bytes;");
	for (String typeField : field) {
		String[] split = typeField.strip().split(" ");
		String type = split[0].strip();
		String name = split[1].strip();
		writer.println("\t\tif (!bufferedChannel.hasRemaining()) return null;");
		writer.printf("\t\tint %sLength = bufferedChannel.getInt();%n", name);
		writer.printf("\t\t%s %s = null;%n", type, name);
		writer.printf("\t\tif (%sLength > 0) {%n", name);
		writer.println("\t\t\tif (!bufferedChannel.hasRemaining()) return null;");
		writer.printf("\t\t\tbytes = new byte[%sLength];\n", name);
		writer.println("\t\t\tbufferedChannel.get(bytes);");
		if (type.equals("String"))
			writer.printf("\t\t\t%s = new String(bytes, StandardCharsets.UTF_8);%n", name);
		else if (type.equals("byte[]"))
			writer.printf("\t\t\t%s = bytes;%n", name);
		writer.println("\t\t}");
	}
	writer.printf("\t\treturn new %s(", className);
	for (int i = 0; i < field.length; i++) {
		String[] split = field[i].strip().split(" ");
		String name = split[1].strip();
		writer.print(name);
		if (i + 1 < field.length) {
			writer.print(", ");
		}
	}
	writer.println(");");
	writer.println("\t}");
}

private static void serialize(PrintWriter writer, String baseName, String className,
	String fields) {
	writer.printf("\tprivate static ByteBuffer serialize%s(%s c) {%n", className, className);
	writer.printf("\t\tshort type = type(c);%n");
	String[] field = fields.split(",");
	for (String typeField : field) {
		String[] split = typeField.strip().split(" ");
		String type = split[0].strip();
		String name = split[1].strip();
		writer.printf("\t\t%s %s = c.%s();%n", type, name, name);
		writer.printf("\t\tint %sLength = %s == null ? 0 : %s.length", name, name, name);
		if (type.equals("String"))
			writer.println("();");
		else if (type.equals("byte[]"))
			writer.println(";");
		writer.printf("\t\tint %sByteLength = Integer.BYTES + %sLength;%n", name, name);
	}
	writer.print("\t\tByteBuffer byteBuffer = ByteBuffer.allocateDirect(Short.BYTES + ");

	for (int i = 0; i < field.length; i++) {
		String[] split = field[i].strip().split(" ");
		String name = split[1].strip();
		writer.printf("%sByteLength", name);
		if (i + 1 < field.length) {
			writer.print(" + ");
		}
	}
	writer.println(");");
	writer.println("\t\tbyteBuffer.putShort(type);");

	for (String s : field) {
		String[] split = s.strip().split(" ");
		String type = split[0].strip();
		String name = split[1].strip();
		writer.printf("\t\tbyteBuffer.putInt(%sLength);%n", name);
		writer.printf("\t\tif (%sLength > 0) byteBuffer.put(%s", name, name);
		if (type.equals("String"))
			writer.println(".getBytes(StandardCharsets.UTF_8));");
		else if (type.equals("byte[]"))
			writer.println(");");
	}
	writer.println("\t\treturn byteBuffer.flip();");
	writer.println("\t}");
}

private static void defineType(PrintWriter writer, String baseName, String className,
	String fieldList) {

	writer.println("\trecord " + className + "(" + fieldList + ") implements " + baseName + " {");
	// Visitor pattern.
	writer.println("\t\t@Override");
	writer.println("\t\tpublic <R> R accept(Visitor<R> visitor) {");
	writer.println("\t\t  return visitor.visit" + className + baseName + "(this);");
	writer.println("\t\t}");
	// isPersisted
	if (readCommands.contains(className)) {
		writer.println("\t\t@Override");
		writer.println("\t\tpublic boolean isPersisted() {");
		writer.println("\t\t\treturn false;");
		writer.println("\t\t}");
	}

	writer.println("\t}");
}

private static void defineVisitor(
	PrintWriter writer, String baseName, List<String> types) {
	writer.println("\tinterface Visitor<R> {");

	for (String type : types) {
		String typeName = type.split(":")[0].trim();
		writer.println("\t\tR visit" + typeName + baseName + "(" +
			typeName + " " + baseName.toLowerCase() + ");");
	}

	writer.println("\t}");
}