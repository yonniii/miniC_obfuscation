package listener;

import generated.MiniCParser;

public class UcodeGenListenerHelper {

    // <boolean functions>

    static boolean isFunDecl(MiniCParser.ProgramContext ctx, int i) {
        return ctx.getChild(i).getChild(0) instanceof MiniCParser.Fun_declContext;
    }

    // type_spec IDENT '[' ']'
    static boolean isArrayParamDecl(MiniCParser.ParamContext param) {
        return param.getChildCount() == 4;
    }

    // global vars
    static int initVal(MiniCParser.Var_declContext ctx) {
        return Integer.parseInt(ctx.LITERAL().getText());
    }

    // var_decl	: type_spec IDENT '=' LITERAL ';
    static boolean isDeclWithInit(MiniCParser.Var_declContext ctx) {
        return ctx.getChildCount() == 5;
    }

    // var_decl	: type_spec IDENT '[' LITERAL ']' ';'
    static boolean isArrayDecl(MiniCParser.Var_declContext ctx) {
        return ctx.getChildCount() == 6;
    }

    // <local vars>
    // local_decl	: type_spec IDENT '[' LITERAL ']' ';'
    static int initVal(MiniCParser.Local_declContext ctx) {
        return Integer.parseInt(ctx.LITERAL().getText());
    }

    static boolean isArrayDecl(MiniCParser.Local_declContext ctx) {
        return ctx.getChildCount() == 6;
    }

    static boolean isDeclWithInit(MiniCParser.Local_declContext ctx) {
        return ctx.getChildCount() == 5;
    }

    static boolean isVoidF(MiniCParser.Fun_declContext ctx) {
        if ("void".equals(ctx.type_spec().getText()))
            return true;
        else
            return false;
    }

    static boolean isIntReturn(MiniCParser.Return_stmtContext ctx) {
        return ctx.getChildCount() == 3;
    }


    static boolean isVoidReturn(MiniCParser.Return_stmtContext ctx) {
        return ctx.getChildCount() == 2;
    }

    // <information extraction>
    //  static String getStackSize(Fun_declContext ctx) {
    //     return "32";
    // }

    // static String getLocalVarSize(Fun_declContext ctx) {
    //     return "32";
    // }

    static int getLocalVarSize(UcodeSymbolTable symbolTable) {
        return symbolTable.get_lsymbolCount();
    }

    static String getTypeText(MiniCParser.Type_specContext typespec) {
        if (typespec.VOID() != null)
            return "V";
        else if (typespec.INT() != null)
            return "I";
        return null;
    }

    // params
    static String getParamName(MiniCParser.ParamContext param) {
        return param.IDENT().getText();
    }

    static String getParamTypesText(MiniCParser.ParamsContext params) {
        String typeText = "";

        for (int i = 0; i < params.param().size(); i++) {
            if (isArrayParamDecl(params.param(i))) {
                typeText += "[I";
            } else {
                MiniCParser.Type_specContext typespec = (MiniCParser.Type_specContext) params.param(i).getChild(0);
                typeText += getTypeText(typespec); // + ";";
            }
        }
        return typeText;
    }

    static String getLocalVarName(MiniCParser.Local_declContext local_decl) {
        return local_decl.IDENT().getText();
    }

    static String getFunName(MiniCParser.Fun_declContext ctx) {
        return ctx.IDENT().getText();
    }

    static String getFunName(MiniCParser.ExprContext ctx) {
//        if (ctx.getChild(1).getText() != "(") {
//            return null;
//        }
        return ctx.IDENT().getText();
    }

    static boolean noElse(MiniCParser.If_stmtContext ctx) {
        return ctx.getChildCount() <= 5;
    }

    static String getFunProlog() {
        String init =
                "    bgn 0 \n" +
                        "    ldp \n"+"    call main \n"+"    end\n";
        return init;
    }

    static String getCurrentClassName() {
        return "Test";
    }
}
