package listener;

import generated.MiniCLexer;
import generated.MiniCParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

public class Translator {
	enum OPTIONS {
		PRETTYPRINT, BYTECODEGEN, UCODEGEN, ERROR
	}
	private static OPTIONS getOption(String[] args){
		if (args.length < 1)
			return OPTIONS.PRETTYPRINT;

		if (args[0].startsWith("-p") 
				|| args[0].startsWith("-P"))
			return OPTIONS.PRETTYPRINT;
		
		if (args[0].startsWith("-b") 
				|| args[0].startsWith("-B"))
			return OPTIONS.BYTECODEGEN;
		
		if (args[0].startsWith("-u") 
				|| args[0].startsWith("-U"))
			return OPTIONS.UCODEGEN;
		
		return OPTIONS.ERROR;
	}
	
	public static void main(String[] args) throws Exception
	{
		CharStream codeCharStream = CharStreams.fromFileName("test.c");
		MiniCLexer lexer = new MiniCLexer(codeCharStream);
		CommonTokenStream tokens = new CommonTokenStream( lexer );
		MiniCParser parser = new MiniCParser( tokens );
		ParseTree tree = parser.program();
		
		ParseTreeWalker walker = new ParseTreeWalker();
		switch (getOption(args)) {
			case PRETTYPRINT :
				walker.walk(new MiniCPrintListener(), tree );
				break;
			case BYTECODEGEN:
				walker.walk(new BytecodeGenListener(), tree );
//				System.out.println("-------------------");
//				walker.walk(new BytecodeGenListner_y(), tree );
				break;
			case UCODEGEN:
				walker.walk(new UcodeGenListener(), tree );
//				break;
			default:
				break;
		}
	}
}