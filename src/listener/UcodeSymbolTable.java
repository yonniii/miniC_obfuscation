package listener;

import generated.MiniCParser;
import generated.MiniCParser.Fun_declContext;
import generated.MiniCParser.Local_declContext;
import generated.MiniCParser.Var_declContext;

import java.util.HashMap;
import java.util.Map;

import static listener.UcodeGenListenerHelper.*;


public class UcodeSymbolTable {
    enum Type {
        INT, INTARRAY, VOID, ERROR
    }

    static public class VarInfo {
        Type type;
        int id;
        int initVal;

        public VarInfo(Type type, int id, int initVal) {
            this.type = type;
            this.id = id;
            this.initVal = initVal;
        }

        public VarInfo(Type type, int id) {
            this.type = type;
            this.id = id;
            this.initVal = 0;
        }
    }

    static public class FInfo {
        public String sigStr;
    }

    private Map<String, VarInfo> _lsymtable = new HashMap<>();    // local v.
    private Map<String, VarInfo> _gsymtable = new HashMap<>();    // global v.
    private Map<String, FInfo> _fsymtable = new HashMap<>();    // function


    private int _globalVarID = 0;
    private int _localVarID = 1;
    private int _labelID = 0;
    private int _tempVarID = 0;

    UcodeSymbolTable() {
        initFunDecl();
        initFunTable();
    }

    void initFunDecl() {        // at each func decl
        _lsymtable.clear();
        _localVarID = 1;
        _labelID = 0;
        _tempVarID = 32;
    }

    void putLocalVar(String varname, Type type) { // 로컬 변수 테이블에 변수를 저장하는 함수
        _lsymtable.put(varname, new VarInfo(type, _localVarID++)); //id변수의 숫자를 1 증가시킴
    }

    void putLocalVarWithSize(String varname, Type type, int offset) { // 로컬 변수 테이블에 변수를 저장하는 함수
        _lsymtable.put(varname, new VarInfo(type, _localVarID)); //id변수의 숫자를 1 증가시킴
        _localVarID += offset;
    }

    void putGlobalVarWithSize(String varname, Type type, int offset) { // 글로벌 변수 테이블에 변수를 저장하는 함수
        _gsymtable.put(varname, new VarInfo(type, _globalVarID)); //id변수의 숫자를 1 증가시킴
        _globalVarID += offset;
    }

    void putGlobalVar(String varname, Type type) { // 글로벌 변수 테이블에 변수를 저장하는 함수
        _gsymtable.put(varname, new VarInfo(type, _globalVarID++)); //id변수의 숫자를 1 증가시킴
    }

    void putLocalVarWithInitVal(String varname, Type type, int initVar) {  // 로컬 변수 테이블에 변수를 저장하는 함수, 초기화한 값이 함께 저장됨 id변수의 숫자를 1 증가시킴
        _lsymtable.put(varname, new VarInfo(type, _localVarID++, initVar));
    }

    void putGlobalVarWithInitVal(String varname, Type type, int initVar) {// 글로벌 변수 테이블에 변수를 저장하는 함수, 초기화한 값이 함께 저장됨 id변수의 숫자를 1 증가시킴
        _gsymtable.put(varname, new VarInfo(type, _globalVarID++, initVar));
    }

    void putParams(MiniCParser.ParamsContext params) { //params에서 param 리스트에 담긴 param을 테이블에 담는 함수
        for (int i = 0; i < params.param().size(); i++) {
            MiniCParser.ParamContext p = params.param(i);
            if(isArrayParamDecl(p)){
                putLocalVar(p.IDENT().getText(), Type.INTARRAY);
            }else{
                putLocalVar(p.IDENT().getText(), getTypeObj(p.type_spec().getText()));
            }
        }
    }

    Type getTypeObj(String type) { // String으로 된 타입을 enum으로 변환시키는 함수
        if ("int".equals(type)) {
            return Type.INT;
        } else if ("void".equals(type)) {
            return Type.VOID;
        }
        return Type.ERROR;
    }

    private void initFunTable() {
        FInfo printlninfo = new FInfo();
        printlninfo.sigStr = "java/io/PrintStream/println(I)V";

        FInfo maininfo = new FInfo();
        maininfo.sigStr = "main([Ljava/lang/String;)V";
        _fsymtable.put("_print", printlninfo);
        _fsymtable.put("main", maininfo);
    }

    public String getFunSpecStr(String fname) { //함수 symbol이 담긴 테이블에서 파라미터로 주어진 함수의 sig를 반환하는 함수
        FInfo fun = (FInfo) _fsymtable.get(fname);
        if (fun != null) {
            return fun.sigStr;
        }
        return null;
    }

    public String getFunSpecStr(Fun_declContext ctx) {
        FInfo fun = (FInfo) _fsymtable.get(getFunName(ctx));
        if (fun != null) {
            return fun.sigStr;
        }
        return null;
    }

    public String putFunSpecStr(Fun_declContext ctx) {
        String fname = getFunName(ctx);
        String argtype = "";
        String rtype = "";
        String res = "";

        argtype = getParamTypesText(ctx.params());
        rtype = getTypeText(ctx.type_spec());

        res = fname + "(" + argtype + ")" + rtype;

        FInfo finfo = new FInfo();
        finfo.sigStr = res;
        _fsymtable.put(fname, finfo);

        return res;
    }

    String getVarId(String name) {
        VarInfo lvar = (VarInfo) _lsymtable.get(name);
        if (lvar != null) {
            return "2\t"+Integer.toString(lvar.id);
        }

        VarInfo gvar = (VarInfo) _gsymtable.get(name);
        if (gvar != null) {
            return "1\t"+Integer.toString(gvar.id);
        }

        return null;
    }

    Type getVarType(String name) {
        VarInfo lvar = (VarInfo) _lsymtable.get(name);
        if (lvar != null) {
            return lvar.type;
        }

        VarInfo gvar = (VarInfo) _gsymtable.get(name);
        if (gvar != null) {
            return gvar.type;
        }

        return Type.ERROR;
    }

    String newLabel() {
        return "$$" + _labelID++;
    }

    String newTempVar() {
        String id = "";
        return id + _tempVarID--;
    }

    // global
    public String getVarId(Var_declContext ctx) {
        String sname = "";
        sname += getVarId(ctx.IDENT().getText());
        return sname;
    }

    // local
    public String getVarId(Local_declContext ctx) {
        String sname = "";
        sname += getVarId(ctx.IDENT().getText());
        return sname;
    }
    public int get_lsymbolCount(){
        return _localVarID-1;
    }
    public int get_gsymbolCount() {
        return _globalVarID;
    }
}

