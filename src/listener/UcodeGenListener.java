package listener;

import generated.MiniCBaseListener;
import generated.MiniCParser;
import generated.MiniCParser.ParamsContext;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import static listener.UcodeGenListenerHelper.*;
import static listener.UcodeSymbolTable.Type;

public class UcodeGenListener extends MiniCBaseListener implements ParseTreeListener{

    ParseTreeProperty<String> newTexts = new ParseTreeProperty<String>();
    UcodeSymbolTable symbolTable = new UcodeSymbolTable();
    int tab = 0;
    int label = 0;
    int stacksize = 0;
    int tempstack = 0;
    String first_global = "";
    // program	: decl+

    @Override
    public void enterFun_decl(MiniCParser.Fun_declContext ctx) {
        symbolTable.initFunDecl();

        String fname = getFunName(ctx);
        ParamsContext params;

        if (fname.equals("main")) { //함수가 main인 경우, args 파라미터를 테이블에 삽입
            symbolTable.putLocalVar("args", Type.INTARRAY);
        } else { //main이 아닌 경우
            symbolTable.putFunSpecStr(ctx); //함수의 선언부?를 bytecode로 하여 테이블에 담는 함수 실행
            params = (ParamsContext) ctx.getChild(3);
            symbolTable.putParams(params); //파라미터를 테이블에 넣는 함수 실행
        }
    }


    // var_decl	: type_spec IDENT ';' | type_spec IDENT '=' LITERAL ';'|type_spec IDENT '[' LITERAL ']' ';'
    @Override
    public void enterVar_decl(MiniCParser.Var_declContext ctx) {
        String varName = ctx.IDENT().getText();

        if (isArrayDecl(ctx)) {
            symbolTable.putGlobalVar(varName, Type.INTARRAY);
        } else if (isDeclWithInit(ctx)) {
            symbolTable.putGlobalVarWithInitVal(varName, Type.INT, initVal(ctx));
        } else { // simple decl
            symbolTable.putGlobalVar(varName, Type.INT);
        }

    }


    @Override
    public void enterLocal_decl(MiniCParser.Local_declContext ctx) {
        if (isArrayDecl(ctx)) {
            symbolTable.putLocalVar(getLocalVarName(ctx), Type.INTARRAY);
        } else if (isDeclWithInit(ctx)) {
            symbolTable.putLocalVarWithInitVal(getLocalVarName(ctx), Type.INT, initVal(ctx));
        } else { // simple decl
            symbolTable.putLocalVar(getLocalVarName(ctx), Type.INT);
        }
    }


    @Override
    public void exitProgram(MiniCParser.ProgramContext ctx) {
        String classProlog = getFunProlog();

        String fun_decl = "", var_decl = "";

        for (int i = 0; i < ctx.getChildCount(); i++) {
            if (isFunDecl(ctx, i))
                fun_decl += newTexts.get(ctx.decl(i));
            else
                var_decl += newTexts.get(ctx.decl(i));
        }

        newTexts.put(ctx, var_decl + fun_decl+ classProlog);

        System.out.println(newTexts.get(ctx));
    }


    // decl	: var_decl | fun_decl
    @Override
    public void exitDecl(MiniCParser.DeclContext ctx) {
        String decl = "";
        if (ctx.getChildCount() == 1) {
            if (ctx.var_decl() != null)                //var_decl
                decl += newTexts.get(ctx.var_decl());
            else                            //fun_decl
                decl += newTexts.get(ctx.fun_decl());
        }
        newTexts.put(ctx, decl);
    }

    // stmt	: expr_stmt | compound_stmt | if_stmt | while_stmt | return_stmt
    @Override
    public void exitStmt(MiniCParser.StmtContext ctx) {
        String stmt = "";
        if (ctx.getChildCount() > 0) { //각자 stmt에 맞는 함수를 실행하여 반환된 값을 newText에 넣음.
            if (ctx.expr_stmt() != null)                // expr_stmt
                stmt += newTexts.get(ctx.expr_stmt());
            else if (ctx.compound_stmt() != null)    // compound_stmt
                stmt += newTexts.get(ctx.compound_stmt());
            else if (ctx.if_stmt() != null)
                stmt += newTexts.get(ctx.if_stmt());
            else if (ctx.while_stmt() != null)
                stmt += newTexts.get(ctx.while_stmt());
            else if (ctx.return_stmt() != null)
                stmt += newTexts.get(ctx.return_stmt());
        }
        newTexts.put(ctx, stmt);
    }

    // expr_stmt	: expr ';'
    @Override
    public void exitExpr_stmt(MiniCParser.Expr_stmtContext ctx) {
        String stmt = "";
        if (ctx.getChildCount() == 2) {
            stmt += newTexts.get(ctx.expr());    // expr
        }
        newTexts.put(ctx, stmt);
    }


    // while_stmt	: WHILE '(' expr ')' stmt
    @Override
    public void exitWhile_stmt(MiniCParser.While_stmtContext ctx) {
        String stmt = "";
        String condExpr = newTexts.get(ctx.expr()); // while의 조건문
        String loopStmt = newTexts.get(ctx.stmt()); // 반복할 stmt

        String lLoop = symbolTable.newLabel(); // 되돌아갈 라벨
        String lBreak = symbolTable.newLabel(); // 조건에 만족하지 않을 때 빠져나갈 라벨

        stmt += lLoop + ":" + "\n" //먼저 되돌아갈 라벨을 명시하여 되돌아왔을 때 조건검사 하도록 함
                + condExpr // 조건 검사 실행
                + "ifeq " + lBreak + "\n" //조건 검사 결과가 0이라면 빠져나감
                + loopStmt // 조건검사 결과가 0이 아닌 경우 실행할 stmt
                + "goto " + lLoop + "\n" // 다시 조건 검사를 하도록 loop라벨로 돌아감
                + lBreak + ":" + "\n"; //조건 검사 결과가 0일 때 빠져나갈 라벨

        newTexts.put(ctx, stmt);
    }

    @Override
    public void exitFun_decl(MiniCParser.Fun_declContext ctx) {
//        symbolTable.putFunSpecStr(ctx);
        String fHeader = funcHeader(ctx, getFunName(ctx));  // 함수의 선언부를 가져오고 메모리 할당까지 함
        String compStmt = newTexts.get(ctx.compound_stmt()); // stmt부분을 가져옴
        String endF = "";
        if (isVoidF(ctx) && ctx.compound_stmt().stmt(ctx.compound_stmt().stmt().size() - 1).return_stmt() == null) { //void인 경우 return문이 없을 때 return 하는 코드 추가
            endF += "return\n";
        }
        endF += ".end method\n"; //함수의 마지막 부분
        newTexts.put(ctx, fHeader + compStmt + endF);
        stacksize = 0;
        tempstack = 0;
    }


    private String funcHeader(MiniCParser.Fun_declContext ctx, String fname) {
        return ".method public static " + symbolTable.getFunSpecStr(fname) + "\n"
                + "\t" + ".limit stack " + stacksize + "\n"
                + "\t" + ".limit locals " + getLocalVarSize(symbolTable) + "\n";
        //  + "\t" + ".limit stack " + getStackSize(ctx) + "\n"
        //                + "\t" + ".limit locals " + getLocalVarSize(ctx) + "\n";
    }


    @Override
    public void exitVar_decl(MiniCParser.Var_declContext ctx) {
        String varName = ctx.IDENT().getText();
        String varDecl = "";

        if (isDeclWithInit(ctx)) {
            varDecl += "putfield " + varName + "\n";
            // v. initialization => Later! skip now..:
        }
        newTexts.put(ctx, varDecl);
    }


    @Override
    public void exitLocal_decl(MiniCParser.Local_declContext ctx) {
        String varDecl = "";

        if (isDeclWithInit(ctx)) {
            String vId = symbolTable.getVarId(ctx);
            varDecl += "ldc " + ctx.LITERAL().getText() + "\n"
                    + "istore " + vId + "\n";
        }else if(isArrayDecl(ctx)) {
            String vId = symbolTable.getVarId(ctx);
            varDecl += "ldc " + ctx.LITERAL().getText() + "\n"
                    + "newarray int\n" +
                    "astore " + vId + "\n";
        }
        newTexts.put(ctx, varDecl);
    }


    // compound_stmt	: '{' local_decl* stmt* '}'
    @Override
    public void exitCompound_stmt(MiniCParser.Compound_stmtContext ctx) {
        String stmt = "";
        for (int i = 0; i < ctx.local_decl().size(); i++) { //n개의 local변수 선언 코드들을 가져옴
            stmt += newTexts.get(ctx.local_decl(i));
        }

        for (int i = 0; i < ctx.stmt().size(); i++) { // n개의 stmt부분을 가져옴
            stmt += newTexts.get(ctx.stmt(i));
        }

        newTexts.put(ctx, stmt);
    }

    // if_stmt	: IF '(' expr ')' stmt | IF '(' expr ')' stmt ELSE stmt;
    @Override
    public void exitIf_stmt(MiniCParser.If_stmtContext ctx) {
        String stmt = "";
        String condExpr = newTexts.get(ctx.expr());
        String thenStmt = newTexts.get(ctx.stmt(0));

        String lend = symbolTable.newLabel();
        String lelse = symbolTable.newLabel();


        if (noElse(ctx)) {
            stmt += condExpr + "\n"
                    + "ifeq " + lend + "\n"
                    + thenStmt + "\n"
                    + lend + ":" + "\n";
        } else {
            String elseStmt = newTexts.get(ctx.stmt(1));
            stmt += condExpr + "\n"
                    + "ifeq " + lelse + "\n"
                    + thenStmt + "\n"
                    + "goto " + lend + "\n"
                    + lelse + ": " + elseStmt + "\n"
                    + lend + ":" + "\n";
        }

        newTexts.put(ctx, stmt);
    }


    // return_stmt	: RETURN ';' | RETURN expr ';'
    @Override
    public void exitReturn_stmt(MiniCParser.Return_stmtContext ctx) {
        String stmt = "";
        if (isIntReturn(ctx)) { // int return인 경우
            String expr = newTexts.get(ctx.expr()); // 리턴할 expr을 받아옴
            stmt += expr + "    retv \n"; // expr과 함께 리턴
        } else if (isVoidReturn(ctx)) {
            stmt += "    ret \n"; //리턴문 추가
        }
        newTexts.put(ctx, stmt);
    }


    @Override
    public void exitExpr(MiniCParser.ExprContext ctx) {
        String expr = "";

        if (ctx.getChildCount() <= 0) {
            newTexts.put(ctx, "");
            return;
        }

        if (ctx.getChildCount() == 1) { // IDENT | LITERAL
            if (ctx.IDENT() != null) {
                String idName = ctx.IDENT().getText();
                if (symbolTable.getVarType(idName) == Type.INT) {
                    tempstack += 1;
                    stacksize = Math.max(stacksize, tempstack);
                    expr += "lod " + symbolTable.getVarId(idName) + " \n";
                } else if (symbolTable.getVarType(idName) == Type.INTARRAY) {
                    tempstack += 1;
                    stacksize = Math.max(stacksize, tempstack);
                    expr += "lda " + symbolTable.getVarId(idName) + " \n";
                }
                //else	// Type int array => Later! skip now..
                //	expr += "           lda " + symbolTable.get(ctx.IDENT().getText()).value + " \n";
            } else if (ctx.LITERAL() != null) {
                tempstack += 1;
                stacksize = Math.max(stacksize, tempstack);
                String literalStr = ctx.LITERAL().getText();
                expr += "ldc " + literalStr + " \n";
            }
        } else if (ctx.getChildCount() == 2) { // UnaryOperation
            expr = handleUnaryExpr(ctx, expr);
            if (ctx.getChild(0).getText().equals("++") || ctx.getChild(0).getText().equals("--")) { // ++, --인 경우 istore해야 하므로 해당 코드 추가함
                expr += "lod " +  newTexts.get(ctx.expr(0)) + " \n";
                tempstack -= 1;
            }
//            expr = handleUnaryExpr(ctx, newTexts.get(ctx) + expr);
        } else if (ctx.getChildCount() == 3) {
            if (ctx.getChild(0).getText().equals("(")) {        // '(' expr ')'
                expr = newTexts.get(ctx.expr(0));

            } else if (ctx.getChild(1).getText().equals("=")) {    // IDENT '=' expr
                expr = newTexts.get(ctx.expr(0)) + "\n"
                        + "str " + symbolTable.getVarId(ctx.IDENT().getText()) + " \n";
                tempstack -= 1;

            } else {                                            // binary operation
                expr = handleBinExpr(ctx, expr);

            }
        }
        // IDENT '(' args ')' |  IDENT '[' expr ']'
        else if (ctx.getChildCount() == 4) {
            if (ctx.args() != null) {        // function calls
                expr = handleFunCall(ctx, expr);
            } else { // expr
                expr = handleArray(ctx, expr);
                expr += "ldi\n";
                tempstack -= 1;
            }
        }
        // IDENT '[' expr ']' '=' expr
        else { // Arrays: TODO			*/
            expr = handleArray(ctx, expr)
                    + newTexts.get(ctx.expr(1)) +
                    System.lineSeparator()
                    + "sti\n";
            tempstack -= 3;
        }
        newTexts.put(ctx, expr);
    }

    private String handleArray(MiniCParser.ExprContext ctx, String expr){
        tempstack += 1;
        stacksize = Math.max(stacksize, tempstack);
        String index = newTexts.get(ctx.expr(0));
        String id = ctx.IDENT().getText();
        expr += index + System.lineSeparator() + "lod " + symbolTable.getVarId(id) + "\n"
                + "add" + "\n";

        return expr;
    }

    private String handleUnaryExpr(MiniCParser.ExprContext ctx, String expr) {
        String l1 = symbolTable.newLabel();
        String l2 = symbolTable.newLabel();
        String lend = symbolTable.newLabel();
        String s1 = newTexts.get(ctx.expr(0));
        expr += s1;
        switch (ctx.getChild(0).getText()) {
            case "-":
                expr += "neg \n";
                break;
            case "--":
                expr += "dec" + "\n" + "str" + s1 ;
                break;
            case "++":
                expr += "inc" + "\n" + "str" + s1 ;
                break;
            case "!":
                expr += "notop";
                break;
        }
        return expr;
    }


    private String handleBinExpr(MiniCParser.ExprContext ctx, String expr) {
        String l2 = symbolTable.newLabel();
        String lend = symbolTable.newLabel();

        expr += newTexts.get(ctx.expr(0));
        expr += newTexts.get(ctx.expr(1));

        switch (ctx.getChild(1).getText()) {
            case "*":
                expr += "mult \n";
                break;
            case "/":
                expr += "div \n";
                break;
            case "%":
                expr += "mod \n";
                break;
            case "+":        // expr(0) expr(1) iadd
                expr += "add \n";
                break;
            case "-":
                expr += "sub \n";
                break;
//            case "==":
//                expr += "if_cmpne ";
//                break;
//            case "!=":
//                expr += "if_cmpeq ";
//                break;
            case "==":
                expr += "eq";
                break;
            case "!=":
                expr += "ne";
                break;
            case "<=": // 뺐을 때 0보다 작거나 같은 경우
                expr += "le";
                break;
            case "<": //뺐을 때 0보다 작은 경우
                expr += "lt";
                break;

            case ">=": // 뺏을 때 0보다 크거나 같은 경우
                expr += "ge";
                break;

            case ">": //뺐을 때 0보다 큰 경우
                expr += "gt";
                break;

            case "and":
                expr += "and";
                break;
            case "or":
                expr += "or";
                break;

        }
        tempstack -= 1;
        return expr;
    }

    private String handleFunCall(MiniCParser.ExprContext ctx, String expr) {
        String fname = getFunName(ctx);
        String s1 = newTexts.get(ctx.args());
        if (fname.equals("_print")) {        // System.out.println
            // TODO : ucode에서 print문 어떻게 할지 확인
            expr = "getstatic java/lang/System/out Ljava/io/PrintStream; " + "\n"
                    + newTexts.get(ctx.args())
                    + "invokevirtual " + symbolTable.getFunSpecStr("_print") + "\n";
        } else {
            expr += "ldp\n" + s1 + "\n" + "call" + fname + "\n";
        }

        return expr;

    }

    // args	: expr (',' expr)* | ;
    @Override
    public void exitArgs(MiniCParser.ArgsContext ctx) {

        String argsStr = "\n";

        for (int i = 0; i < ctx.expr().size(); i++) {
            argsStr += newTexts.get(ctx.expr(i));
        }
        newTexts.put(ctx, argsStr);
    }

}

