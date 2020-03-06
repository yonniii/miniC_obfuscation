package listener;

import generated.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.*;

import java.util.ArrayList;
import java.util.HashMap;

public class MiniCPrintListener extends MiniCBaseListener {

    class Var {
        String id;
        String type;
        int index;

        public Var(String id, String type, int index) {
            this.id = id;
            this.type = type;
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        @Override
        public String toString() {
            return (type + " " + id + " " + index);
        }
    }
    ArrayList<Var> localVars;
    HashMap<String, String> newVar;

    ParseTreeProperty<String> newTexts = new ParseTreeProperty<String>();
    int depth = 0;
    int ifDepth = 0;
    int nonCompCount = 0;

    @Override
    public void exitProgram(MiniCParser.ProgramContext ctx) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            newTexts.put(ctx, ctx.decl(i).getText()); //ParseTree인 newText에 decl을 넣음
            System.out.print(newTexts.get(ctx.getChild(i))); //ctx의 child에 들어갔다가 나오면서 출력
        }
    }
//    @Override
//    public void enterProgram(MiniCParser.ProgramContext ctx) {
//        for (int i = 0; i < ctx.getChildCount(); i++) {
//            newTexts.put(ctx, ctx.decl(i).getText());
//            System.out.println(newTexts.get(ctx.getChild(i)));
//        }
//    }

    @Override
    public void exitDecl(MiniCParser.DeclContext ctx) {
        newTexts.put(ctx, newTexts.get(ctx.getChild(0))); //newText에 자식을 꺼내오고 다시 삽입
    }

    @Override
    public void exitVar_decl(MiniCParser.Var_declContext ctx) {
        String type = null, id = null, literal = null;
        int childCount = ctx.getChildCount();
        type = newTexts.get(ctx.type_spec()); //newtexts로부터 typeSpec을 받아옴
        id = ctx.IDENT().getText(); //Terminal Ident를 받아옴
        localVars.add(new Var(id, type, localVars.size()));
        if (childCount == 3) { //자식이 3개라면 다음과 같이 삽입
            newTexts.put(ctx, type + " " + id + ";\n");
        } else if (childCount == 5) { //자식이 5개라며 다음과 같이 삽입
            literal = ctx.LITERAL().getText(); //Terminal Literal을 받아옴
            newTexts.put(ctx, type + " " + id + " = " + literal + ";\n");
        } else if (childCount == 6) { // 자식이 6개라면 다음과 같이 삽입
            literal = ctx.LITERAL().getText();
            newTexts.put(ctx, type + " " + id + " [ " + literal + "];\n");
        }
    }

    @Override
    public void exitType_spec(MiniCParser.Type_specContext ctx) {
        if (ctx.getChild(0).equals(ctx.VOID())) { //각 터미널에 맞는 토큰을 받아와서 newtexts에 삽입
            newTexts.put(ctx, ctx.VOID().getText());
        } else {
            newTexts.put(ctx, ctx.INT().getText());
        }

    }
    @Override
    public void enterFun_decl(MiniCParser.Fun_declContext ctx) {
        depth = 1; // funDecl의 depth는 1부터 시작
        localVars = new ArrayList<Var>();
        newVar = new HashMap<>();
    }
    @Override
    public void exitFun_decl(MiniCParser.Fun_declContext ctx) {
        String type = null, ident = null, params = null, compoundstmt = null;
        if (ctx.getChildCount() == 6) { //각각의 터미널과 논터미널을 받아와서 newText에 삽입
            type = newTexts.get(ctx.type_spec());
            ident = ctx.IDENT().getText();
            params = newTexts.get(ctx.params());
            compoundstmt = newTexts.get(ctx.compound_stmt());
            newTexts.put(ctx, type + " " + ident + "(" + params + ")" + compoundstmt);
        }
    }


    @Override
    public void exitParams(MiniCParser.ParamsContext ctx) {
        String param = ""; //파라미터를 하나씩 붙여나가서 한번에 newText에 삽입하기 위해 사용
        if (ctx.getChild(0) == null) { //파라미터가 없는 경우
            newTexts.put(ctx, "");
        } else if (ctx.getChildCount() == 1) { //파라미터가 하나인 경우
            newTexts.put(ctx, ctx.VOID().getText());
        } else { //파라미터가 두개 이상인 경우
            for (int i = 0; i < ctx.getChildCount(); i++) {
                if (i % 2 == 0) { //짝수번째는 파라미터를 받아서 넣음
                    param += newTexts.get(ctx.param(i / 2));
                } else {
                    param += ", "; //홀수번째는 반점을 넣음
                }
            }
            newTexts.put(ctx, param); //삽입
        }
    }


    @Override
    public void exitParam(MiniCParser.ParamContext ctx) { //파라미터 하나에 대해 동작
        String type = newTexts.get(ctx.type_spec());
        String id = ctx.IDENT().getText(); //typespec과 ident받아옴
        if (ctx.getChildCount() == 2) { //각각의 경우에 맞게 삽입
            newTexts.put(ctx, type + " " + id);
            newVar.put(id, "temp_"+id);
            localVars.add(new Var("temp_"+id, type, localVars.size()));
        } else {
            newTexts.put(ctx, type + " " + id + "[]");
        }
    }


    @Override
    public void exitStmt(MiniCParser.StmtContext ctx) { //stmt를 빠져나오며

        if (ctx.getChild(0) == ctx.expr_stmt()) { //각각의 경우에 맞게  stmt를 받아와서 put
            newTexts.put(ctx, newTexts.get(ctx.expr_stmt()));
        } else if (ctx.getChild(0) == ctx.compound_stmt()) {
            newTexts.put(ctx, newTexts.get(ctx.compound_stmt()));
            depth--; // compoundstmt를 빠져나오면 depth를 감소시킴
        } else if (ctx.getChild(0) == ctx.if_stmt()) {
            newTexts.put(ctx, newTexts.get(ctx.if_stmt()));
        } else if (ctx.getChild(0) == ctx.while_stmt()) {
            newTexts.put(ctx, newTexts.get(ctx.while_stmt()));
        } else if (ctx.getChild(0) == ctx.return_stmt()) {
            newTexts.put(ctx, newTexts.get(ctx.return_stmt()));
        }

    }

    @Override
    public void enterStmt(MiniCParser.StmtContext ctx) { //stmt를 들어가며. depth를 증가시키기 위해
        if (ctx.getChild(0) == ctx.expr_stmt()) {
            newTexts.put(ctx, ctx.expr_stmt().getText());
        } else if (ctx.getChild(0) == ctx.compound_stmt()) {
            depth++; // compoundstmt일 땐 depth를 증가시킨다.
            newTexts.put(ctx, ctx.compound_stmt().getText());
        } else if (ctx.getChild(0) == ctx.if_stmt()) {
            newTexts.put(ctx, ctx.if_stmt().getText());
        } else if (ctx.getChild(0) == ctx.while_stmt()) {
            newTexts.put(ctx, ctx.while_stmt().getText());
        } else if (ctx.getChild(0) == ctx.return_stmt()) {
            newTexts.put(ctx, ctx.return_stmt().getText());
        }
    }

    @Override
    public void exitExpr_stmt(MiniCParser.Expr_stmtContext ctx) { //expr stmt일 때
        String cont = newTexts.get(ctx.expr());
        newTexts.put(ctx, cont+ ";\n"); //expr를 받아와서 ;와 \n을 붙여서 put
    }

    boolean isBinaryOperation(MiniCParser.ExprContext ctx) { //이진연산인지 판단
        return ctx.getChildCount() == 3 &&
                ctx.getChild(1) != ctx.expr();
    }

    private boolean isUnaryOp(MiniCParser.ExprContext ctx) { //단항연산인지 판단
        return ctx.getChildCount() == 2;
    }

    private boolean isBracket(MiniCParser.ExprContext ctx) { //괄호인지 판단
        return "(".equals(ctx.getChild(0).getText());
    }

    @Override
    public void exitExpr(MiniCParser.ExprContext ctx) { //각각의 경우에 맞추어 형식을 맞춰서 newtext에 put
        String s1 = "", s2 = "", op = "";
        if (isIdOp(ctx)) { //ident가 가장 먼저오는 규칙일 때
            if (ctx.getChildCount() == 1) {
                newTexts.put(ctx, getVarId(ctx.IDENT().getText()));
            } else if (ctx.getChildCount() == 3) {
                s1 = getVarId(ctx.IDENT().getText());
                s2 = newTexts.get(ctx.expr(0));
                newTexts.put(ctx, s1 + " = " + s2);
            } else if (ctx.getChildCount() == 4) {
                s1 = getVarId(ctx.IDENT().getText());
                if (ctx.getChild(2) == ctx.expr()) {
                    s2 = newTexts.get(ctx.expr(0));
                    newTexts.put(ctx, s1 + "[" + s2 + "]");
                } else {
                    s2 = newTexts.get(ctx.args());
                    newTexts.put(ctx, s1 + "(" + s2 + ")");
                }
            } else {
                s1 = getVarId(ctx.IDENT().getText());
                s2 = newTexts.get(ctx.expr(0));
                String s3 = newTexts.get(ctx.expr(1));
                newTexts.put(ctx, s1 + ctx.getChild(1).getText() + s2 +
                        ctx.getChild(3).getText() + ctx.getChild(4).getText() + s3);
            }
        } else if (isBinaryOperation(ctx)) {
            s1 = newTexts.get(ctx.expr(0));
            s2 = newTexts.get(ctx.expr(1));
            op = ctx.getChild(1).getText();
            newTexts.put(ctx, s1 + " " + op + " " + s2);
        } else if (isUnaryOp(ctx)) {
            op = ctx.getChild(0).getText();
            s1 = newTexts.get(ctx.expr(0));
            newTexts.put(ctx, op + s1);
        } else if (isBracket(ctx)) {
            s1 = newTexts.get(ctx.expr(0));
            newTexts.put(ctx, "(" + s1 + ")");
        } else if (ctx.getChildCount() == 1) {
            newTexts.put(ctx, ctx.LITERAL().getText());
        }

    }

    private boolean isIdOp(MiniCParser.ExprContext ctx) { //ident 인지 판단
        return ctx.getChild(0) == ctx.IDENT();
    }
    @Override
    public void enterWhile_stmt(MiniCParser.While_stmtContext ctx) { //while 에 들어갈 때
        StringBuilder sb = new StringBuilder();
        if(getVarIndex("temp_while")!= -1)
            localVars.add(new Var("temp_while","int",localVars.size()));
        for (int i = 0; i < depth - 1; i++) { //while(~~)를 put하기 전에 공백을 삽입한다
            sb.append("....");
        }
        newTexts.put(ctx, sb.toString());

    }

    @Override
    public void exitWhile_stmt(MiniCParser.While_stmtContext ctx) { //while에서 나오며
        String whle = ctx.WHILE().getText(); //각각 nonT와 T에 대하여 받아오고 put

        String expt = newTexts.get(ctx.expr());
        String stmt = newTexts.get(ctx.stmt());
        if(! (ctx.stmt().getChild(0) instanceof MiniCParser.Compound_stmtContext )){ //괄호로 둘러쌓인 compound stmt가 아닌 경우 괄호 추가하는 메소드실행
            stmt = addWS(stmt);
        }

        String obfus = String.format("temp_while = 0;\n");
        newTexts.put(ctx, whle + " (" + expt + ")" +stmt);
    }

    @Override
    public void exitCompound_stmt(MiniCParser.Compound_stmtContext ctx) { //compound stmt인 경우
        StringBuilder stmt = new StringBuilder("\n") ;
        for (int i = 0; i < depth -1; i++) {
            stmt.append("...."); // depth에 맞게 공백 append
        } //공백을 삽입하고 괄호와 개행을 알맞게 삽입하여 newtexts에 삽입한다.
        stmt.append("{\n");
        int localCnt = 0;
        for (int i=0; i<ctx.local_decl().size(); i++){
            for (int j = 0; j < depth; j++) {
                stmt.append("....");
            }
            stmt.append(newTexts.get(ctx.local_decl(i)));
        }
        StringBuilder decl = new StringBuilder();
        StringBuilder forLoop = new StringBuilder();
        newVar.forEach((key,value) -> {
            decl.append(String.format("\t%s %s = 0;\n",getVarType(key),value));
            forLoop.append(String.format("\tfor (int i = 0; i < %s; i++)\n\t\t%s++;\n",key,value));
        });
        stmt.append(decl.toString());
        stmt.append(forLoop.toString());
        for (int i = 0; i < ctx.stmt().size(); i++) {
            for (int j = 0; j < depth; j++) {
                stmt.append("....");
            }
        }
//        for (int i = 0; i < depth -1; i++) {
//            stmt.append("....");
//        }
        stmt.append("}\n");
        newTexts.put(ctx, stmt.toString());
    }

    public String opaqueObfus(int stmtCnt){
        if(stmtCnt < 3){

        }else{

        }
        return "";
    }

    @Override
    public void exitLocal_decl(MiniCParser.Local_declContext ctx) { //localdecl인 경우
        int chdCnt = ctx.getChildCount();
        String type = null, ident = null, literal = null;
        type = newTexts.get(ctx.type_spec());
        ident = ctx.IDENT().getText(); //규칙에 맞게 알맞게 삽입
        if (chdCnt == 3) {
            newTexts.put(ctx, type + " " + ident + ";\n");
        } else if (chdCnt == 5) {
            literal = ctx.LITERAL().getText();
            newTexts.put(ctx, type + " " + ident + " = " + literal + ";\n");
        } else if (chdCnt == 6) {
            literal = ctx.LITERAL().getText();
            newTexts.put(ctx, type + " " + ident + "[" + literal + "];\n");
        }
    }


    @Override
    public void enterIf_stmt(MiniCParser.If_stmtContext ctx) { //if stmt에 들어갈 경우
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth -1; i++) { //depth에 맞게 공백을 삽입한다
            sb.append("....");
        }
        ifDepth = depth; //else를 위해서 depth를 저장해둠
        newTexts.put(ctx, sb.toString()); //공백을 put
    }


    @Override
    public void exitIf_stmt(MiniCParser.If_stmtContext ctx) { //if stmt에서 exit할 경우
        String iif = ctx.IF().getText();
        String expr = newTexts.get(ctx.expr());
        String stmt1 = newTexts.get(ctx.stmt(0));
        StringBuilder sb = new StringBuilder();
        if(! (ctx.stmt(0).getChild(0) instanceof MiniCParser.Compound_stmtContext )){
            stmt1 = addWS(stmt1); //compound stmt가 아닌 경우 공백과 괄호 삽입하는 메소드 호출
        }
        if (ctx.getChildCount() == 5) { //각각 규칙에 맞게 삽입
            sb.append(iif + " (" + expr + ")" + stmt1);
            newTexts.put(ctx, sb.toString());
        } else {
            String eelse = ctx.ELSE().getText();
            String stmt2 = newTexts.get(ctx.stmt(1));
            if(! (ctx.stmt(1).getChild(0) instanceof MiniCParser.Compound_stmtContext ) ){
                stmt2 = addWS(stmt2);
            }

            sb.append(iif + " (" + expr + ")" + stmt1 );
            for (int i = 0; i < ifDepth; i++) { //else에 대하여 공백 삽입하는 과정
                sb.append("....");
            }
            sb.append(eelse + stmt2);
            newTexts.put(ctx, sb.toString());
        }
    }

    private String addWS(String stmt){ //괄호가 없는 stmt에 대하여 공백과 괄호를 삽입하는 메소드
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (int i = 0; i < depth; i++) {
            sb.append("....");
        }
        sb.append("{\n");
        for (int i = 0; i < depth +1; i++) {
            sb.append("....");
        }
        sb.append(stmt);
        for (int i = 0; i < depth; i++) {
            sb.append("....");
        }
        sb.append("}\n");
        return sb.toString();
    }

    @Override
    public void exitReturn_stmt(MiniCParser.Return_stmtContext ctx) { //return stmt에 대한 규칙
        String rtn = ctx.RETURN().getText(); //각각 nonT와 T에 대하여 받아오고 put
        if (ctx.getChildCount() == 2) {
            newTexts.put(ctx, rtn + ";\n");
        } else {
            String expr = newTexts.get(ctx.expr());
            newTexts.put(ctx, rtn + " " + expr + ";\n");
        }
    }

    @Override
    public void exitArgs(MiniCParser.ArgsContext ctx) { //args에 대한 규칙
        if (ctx.getChild(0) == null) { //각각 nonT와 T에 대하여 받아오고 put
            newTexts.put(ctx, "");
        } else {
            String args = "";
            for (int i = 0; i < ctx.getChildCount(); i++) {
                if (i % 2 == 0) {
                    args += newTexts.get(ctx.expr(i / 2));
                } else {
                    args += ", ";
                }
            }
            newTexts.put(ctx, args);
        }
    }

//    @Override
//    public void exitEveryRule(ParserRuleContext ctx) {
//    }
//
//    @Override
//    public void visitTerminal(TerminalNode node) {
//    }

    public String getVarId(String id){
        if(newVar.containsKey(id)){
            id = newVar.get(id);
        }
        for (Var i : localVars) {
            if (id.equals(i.id)) {
                return i.id;
            }
        }
        return id;
    }

    public String getVarType(String id){
        if(newVar.containsKey(id)){
            id = newVar.get(id);
        }
        for (Var i : localVars) {
            if (id.equals(i.id)) {
                return i.type;
            }
        }
        return null;
    }

    public int getVarIndex(String id) {
        if(newVar.containsKey(id)){
            id = newVar.get(id);
        }
        for (Var i : localVars) {
            if (id.equals(i.id)) {
                return i.index;
            }
        }
        return -1;
    }



    @Override
    public void visitErrorNode(ErrorNode node) {
        System.out.println("error");
    }

}
