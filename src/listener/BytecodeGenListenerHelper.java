package listener;

import generated.MiniCParser;
import generated.MiniCParser.*;

public class BytecodeGenListenerHelper {

    // <boolean functions>

    static boolean isFunDecl(ProgramContext ctx, int i) {
        return ctx.getChild(i).getChild(0) instanceof Fun_declContext;
    }

    // type_spec IDENT '[' ']'
    static boolean isArrayParamDecl(ParamContext param) {
        return param.getChildCount() == 4;
    }

    // global vars
    static int initVal(Var_declContext ctx) {
        return Integer.parseInt(ctx.LITERAL().getText());
    }

    // var_decl	: type_spec IDENT '=' LITERAL ';
    static boolean isDeclWithInit(Var_declContext ctx) {
        return ctx.getChildCount() == 5;
    }

    // var_decl	: type_spec IDENT '[' LITERAL ']' ';'
    static boolean isArrayDecl(Var_declContext ctx) {
        return ctx.getChildCount() == 6;
    }

    // <local vars>
    // local_decl	: type_spec IDENT '[' LITERAL ']' ';'
    static int initVal(Local_declContext ctx) {
        return Integer.parseInt(ctx.LITERAL().getText());
    }

    static boolean isArrayDecl(Local_declContext ctx) {
        return ctx.getChildCount() == 6;
    }

    static boolean isDeclWithInit(Local_declContext ctx) {
        return ctx.getChildCount() == 5;
    }

    static boolean isVoidF(Fun_declContext ctx) {
        if("void".equals(ctx.type_spec().getText()))
        	return true;
        else
        	return false;
    }

    static boolean isIntReturn(Return_stmtContext ctx) {
        return ctx.getChildCount() == 3;
    }


    static boolean isVoidReturn(Return_stmtContext ctx) {
        return ctx.getChildCount() == 2;
    }

    // <information extraction>
  //  static String getStackSize(Fun_declContext ctx) {
   //     return "32";
   // }

   // static String getLocalVarSize(Fun_declContext ctx) {
   //     return "32";
   // }

    static int getLocalVarSize(SymbolTable symbolTable) {
        return symbolTable.get_lsymbolCount();
    }
    static String getTypeText(Type_specContext typespec) {
    	if(typespec.VOID() != null)
    		return "V";
    	else if(typespec.INT() != null)
    		return "I";
    	return null;
    }

    // params
    static String getParamName(ParamContext param) {
        return param.IDENT().getText();
    }

    static String getParamTypesText(ParamsContext params) {
        String typeText = "";

        for (int i = 0; i < params.param().size(); i++) {
            if(isArrayParamDecl(params.param(i))){
                typeText += "[I";
            }else{
                MiniCParser.Type_specContext typespec = (MiniCParser.Type_specContext) params.param(i).getChild(0);
                typeText += getTypeText(typespec); // + ";";
            }
        }
        return typeText;
    }

    static String getLocalVarName(Local_declContext local_decl) {
        return local_decl.IDENT().getText();
    }

    static String getFunName(Fun_declContext ctx) {
        return ctx.IDENT().getText();
    }

    static String getFunName(ExprContext ctx) {
//        if (ctx.getChild(1).getText() != "(") {
//            return null;
//        }
        return ctx.IDENT().getText();
    }

    static boolean noElse(If_stmtContext ctx) {
        return ctx.getChildCount() <= 5;
    }

    static String getFunProlog() {
		String init =
				".class public " + getCurrentClassName() + "\n" +
						".super java/lang/Object\n" +
						".method public <init>()V\n" +
						"aload_0\n" +
						"invokenonvirtual java/lang/Object/<init>()V\n" +
						"return\n" +
						".end method" + "\n";
		return init;
    }

    static String getCurrentClassName() {
        return "Test";
    }
}
