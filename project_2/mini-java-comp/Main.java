import syntaxtree.*;
import visitor.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.LinkedHashMap; // import the HashMap class
import java.util.LinkedHashSet; // import the HashMap class
import java.util.Set;

public class Main {
    public static void main(String[] args) throws Exception {
        if(args.length != 1){
            System.err.println("Usage: java Main <inputFile>");
            System.exit(1);
        }

        FileInputStream fis = null;
        try{
            fis = new FileInputStream(args[0]);
            MiniJavaParser parser = new MiniJavaParser(fis);

            Goal root = parser.Goal();

            System.err.println("Program parsed successfully.");

            MyVisitor eval = new MyVisitor();
            
            root.accept(eval, null);

            MyVisitor2 eval2 = new MyVisitor2(eval);
            
            root.accept(eval2, null);


            Set<String> allKeys = eval.classes.keySet();
            int i=0;
            int offsetv=0;
            int offsetf=0;
            LinkedHashSet<String> Functions=new LinkedHashSet();  
            LinkedHashSet<String> Variables=new LinkedHashSet();  
            for(String key : allKeys){
                i++;
                if(i==1)continue;
                System.out.printf("-----------Class %s-----------\n",key);

                if(eval.classes.get(key)==key){
                    offsetf=0;
                    offsetv=0;
                }
                Set<String> alldeckeys = eval.declarations.keySet();

                Functions.clear();
                Variables.clear();
                for(String key1 : alldeckeys)
                {
                    if(!key.equals(key1.split("\\.")[0]))continue;
                    if(eval.argumentcheck.containsKey(key1))
                    {
                        Functions.add(key1+" : "+offsetf);
                        offsetf+=8;
                    }
                    else
                    {
                        if(eval.declarations.get(key1).equals("int[]"))
                        {
                            Variables.add(key1+" : "+offsetv);
                            offsetv+=8;
                        }
                        else if(eval.declarations.get(key1).equals("int"))
                        {
                            Variables.add(key1+" : "+offsetv);
                            offsetv+=4;
                        }
                        else if(eval.declarations.get(key1).equals("boolean"))
                        {
                            Variables.add(key1+" : "+offsetv);
                            offsetv+=1;    
                        }
                        else 
                        {
                            Variables.add(key1+" : "+offsetv);
                            offsetv+=8;    
                        }
                    }
                }
                System.out.printf("---Variables---\n");

                for(String key2 : Variables)
                {
                    System.out.println(key2);
                }
                System.out.printf("---Methods---\n");

                for(String key2 : Functions)
                {
                    System.out.println(key2);
                }
                
            }
        }
        catch(ParseException ex){
            System.out.println(ex.getMessage());
        }
        catch(FileNotFoundException ex){
            System.err.println(ex.getMessage());
        }
        finally{
            try{
                if(fis != null) fis.close();
            }
            catch(IOException ex){
                System.err.println(ex.getMessage());
            }
        }
    }
}


class MyVisitor extends GJDepthFirst<String, Void>{


    public LinkedHashMap<String, String> declarations=new LinkedHashMap<String, String>();
    public LinkedHashMap<String, String> argumentcheck=new LinkedHashMap<String, String>();
    public LinkedHashMap<String,String> classes=new LinkedHashMap<String, String>();

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    @Override
    public String visit(MainClass n, Void argu) throws Exception {
        String classname = n.f1.accept(this, null);

        if(classes.containsKey(classname))throw new Exception("Redeclaration");
        classes.put(classname,classname);

        NodeListOptional decls = n.f14;
        for(int i=0;i < decls.size() ;i++){
            VarDeclaration vardecl = (VarDeclaration) decls.elementAt(i);
            if(argumentcheck.containsKey(classname+"."+vardecl.accept(this,null)))throw new Exception("Redeclaration");
            argumentcheck.put(classname+"."+vardecl.accept(this,null),visit(vardecl.f0,argu));

        }

        return null;
    }
        /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    @Override
    public String visit(ClassDeclaration n, Void argu) throws Exception {
        String classname = n.f1.accept(this, null);

        if(classes.containsKey(classname))throw new Exception("Redeclaration");
        classes.put(classname,classname);
        
        
        NodeListOptional decls = n.f3;
        for(int i=0;i < decls.size() ;i++){
            VarDeclaration vardecl = (VarDeclaration) decls.elementAt(i);
            if(declarations.containsKey(classname+"."+vardecl.accept(this,null)))throw new Exception("Redeclaration");
            declarations.put(classname+"."+vardecl.accept(this,null),visit(vardecl.f0,argu));

        }

        NodeListOptional methbs = n.f4;
        for(int i=0;i < methbs.size() ;i++){
            MethodDeclaration methdecl =(MethodDeclaration) methbs.elementAt(i);
            declarations.put(classname+"."+methdecl.accept(this,null),visit(methdecl.f1,argu));


            String arguments=methdecl.f4.present() ? methdecl.f4.accept(this, null) : "";
            
            if(argumentcheck.containsKey(classname+"."+methdecl.accept(this,null)))throw new Exception("Redeclaration");
            argumentcheck.put(classname+"."+methdecl.accept(this,null),arguments);


            if(arguments.length()!=0){

                String[] argumentList = (arguments).split(",[ ]*");
                for(int k=0;k<argumentList.length;k++)
                {   
                    String[] split2=argumentList[k].split(" ");
                    if(argumentcheck.containsKey(classname+"."+methdecl.accept(this,null)+"."+split2[1]))throw new Exception("Redeclaration");
                    argumentcheck.put(classname+"."+methdecl.accept(this,null)+"."+split2[1],split2[0]);
                }  
            }    
           
            NodeListOptional fdecls = methdecl.f7;
            
            for(int j=0;j < fdecls.size() ;j++){
                VarDeclaration fvardecl = (VarDeclaration) fdecls.elementAt(j);
                if(argumentcheck.containsKey(classname+"."+methdecl.accept(this,null)+"."+fvardecl.accept(this,null)))throw new Exception("Redeclaration");
                argumentcheck.put(classname+"."+methdecl.accept(this,null)+"."+fvardecl.accept(this,null),visit(fvardecl.f0,argu));
            }
        }


        return null;
    }
    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    @Override
    public String visit(ClassExtendsDeclaration n, Void argu) throws Exception {
        String classname = n.f1.accept(this, null);

        if(classes.containsKey(classname))throw new Exception("Redeclaration");
        classes.put(classname,n.f3.accept(this,null));

        NodeListOptional decls = n.f5;
        for(int i=0;i < decls.size() ;i++){
            VarDeclaration vardecl = (VarDeclaration) decls.elementAt(i);
            
            if(declarations.containsKey(classname+"."+vardecl.accept(this,null)))throw new Exception("Redeclaration");
            declarations.put(classname+"."+vardecl.accept(this,null),visit(vardecl.f0,argu));

        }
        
        NodeListOptional methbs = n.f6;
        for(int i=0;i < methbs.size() ;i++){
            MethodDeclaration methdecl =(MethodDeclaration) methbs.elementAt(i);

                
                if(!declarations.containsKey(n.f3.accept(this,null)+"."+methdecl.accept(this,null)))
                declarations.put(classname+"."+methdecl.accept(this,null),visit(methdecl.f1,argu));

            
                String arguments=methdecl.f4.present() ? methdecl.f4.accept(this, null) : "";

                if(argumentcheck.containsKey(classname+"."+methdecl.accept(this,null)))throw new Exception("Redeclaration");
                argumentcheck.put(classname+"."+methdecl.accept(this,null),arguments);



                if(arguments.length()!=0){


                    String[] argumentList = (arguments).split(",[ ]*");
                 

                    for(int k=0;k<argumentList.length;k++)
                    {   
                        String[] split2=argumentList[k].split(" ");

                        if(argumentcheck.containsKey(classname+"."+methdecl.accept(this,null)+"."+split2[1]))throw new Exception("Redeclaration");
                            
                        if(!argumentcheck.containsKey(n.f3.accept(this,null)+"."+methdecl.accept(this,null)))
                        {
                            argumentcheck.put(classname+"."+methdecl.accept(this,null)+"."+split2[1],split2[0]);
                        }else{
                            if(!argumentcheck.get(n.f3.accept(this,null)+"."+methdecl.accept(this,null)).equals(arguments))throw new Exception("Redeclaration");
                            argumentcheck.put(classname+"."+methdecl.accept(this,null)+"."+split2[1],split2[0]);
                        }

                    }  
                }
            NodeListOptional fdecls = methdecl.f7;
            for(int j=0;j < fdecls.size() ;j++){
                VarDeclaration fvardecl = (VarDeclaration) fdecls.elementAt(j);
                if(argumentcheck.containsKey(classname+"."+methdecl.accept(this,null)+"."+fvardecl.accept(this,null)))throw new Exception("Redeclaration");
                argumentcheck.put(classname+"."+methdecl.accept(this,null)+"."+fvardecl.accept(this,null),visit(fvardecl.f0,argu));           

        }
        }
        return null;
        
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    @Override
    public String visit(MethodDeclaration n, Void argu) throws Exception {

        String myType = n.f1.accept(this, null);
        String myName = n.f2.accept(this, null);
        return myName;
    }


       /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    @Override
    public String visit(FormalParameterList n, Void argu) throws Exception {
        String ret = n.f0.accept(this, null);

        if (n.f1 != null) {
            ret += n.f1.accept(this, null);
        }

        return ret;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public String visit(FormalParameterTerm n, Void argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    @Override
    public String visit(FormalParameterTail n, Void argu) throws Exception {
        String ret = "";
        for ( Node node: n.f0.nodes) {
            ret += ", " + node.accept(this, null);
        }

        return ret;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */

    @Override
    public String visit(FormalParameter n, Void argu) throws Exception{
        String type = n.f0.accept(this, null);
        String name = n.f1.accept(this, null);
        return type + " " + name;
    }

    @Override
    public String visit(ArrayType n, Void argu) {
        return "int[]";
    }
    @Override
    public String visit(BooleanType n, Void argu) {
        return "boolean";
    }

    public String visit(IntegerType n, Void argu) {
        return "int";
    }

    @Override
    public String visit(Identifier n, Void argu) {
        return n.f0.toString();
    }

    @Override
    public String visit(VarDeclaration n, Void argu) throws Exception{
        return n.f1.accept(this,null);
    }
}



class MyVisitor2 extends GJDepthFirst<String, String>{


    MyVisitor mvisitor;
    
    public MyVisitor2(MyVisitor eval)
    {
        mvisitor=eval;
    }
    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    @Override
    public String visit(MainClass n, String argu) throws Exception {
        String classname = n.f1.accept(this, null);

        NodeListOptional stmts = n.f15;
        for(int i=0;i < stmts.size() ;i++){
            Statement stmt = (Statement) stmts.elementAt(i);
            stmt.accept(this,classname);
        }

        return null;
    }
        /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    @Override
    public String visit(ClassDeclaration n, String argu) throws Exception {
        String classname = n.f1.accept(this, null);
        super.visit(n,classname);
        return null;
    }
    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    @Override
    public String visit(ClassExtendsDeclaration n, String argu) throws Exception {
        String classname = n.f1.accept(this, null);
        super.visit(n,classname);
        return null;
        
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    @Override
    public String visit(MethodDeclaration n, String argu) throws Exception {

        String myType = n.f1.accept(this, null);
        String myName = n.f2.accept(this, null);
        
        NodeListOptional stmts = n.f8;
        for(int i=0;i < stmts.size() ;i++){
            Statement stmt = (Statement) stmts.elementAt(i);
            stmt.accept(this,argu+"."+myName);
        }

        String type2=getType(n.f10.accept(this,argu+"."+myName),argu+"."+myName);
        if(!myType.equals(type2))throw new Exception("Different return type");

        super.visit(n,argu+"."+myName);
        return myName;
    }


       /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    @Override
    public String visit(FormalParameterList n, String argu) throws Exception {
        String ret = n.f0.accept(this, null);

        if (n.f1 != null) {
            ret += n.f1.accept(this, null);
        }

        return ret;
    }

    /**
     * f0 -> FormalParameter()
     * f1 -> FormalParameterTail()
     */
    public String visit(FormalParameterTerm n, String argu) throws Exception {
        return n.f1.accept(this, argu);
    }

    /**
     * f0 -> ","
     * f1 -> FormalParameter()
     */
    @Override
    public String visit(FormalParameterTail n, String argu) throws Exception {
        String ret = "";
        for ( Node node: n.f0.nodes) {
            ret += ", " + node.accept(this, null);
        }

        return ret;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */   

    @Override
    public String visit(FormalParameter n, String argu) throws Exception{
        String type = n.f0.accept(this, null);
        String name = n.f1.accept(this, null);
        return type + " " + name;
    }

        /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
    @Override
    public String visit(AssignmentStatement n, String argu) throws Exception {

        String  type1=getType(n.f0.accept(this,argu),argu);
        if(type1==null)throw new Exception("UNDEFIEND");  
        String  type2=getType(n.f2.accept(this,argu),argu);  

        if(!type1.equals(type2)&&!mvisitor.classes.get(type2).equals(type1)&&!mvisitor.classes.get(type1).equals(type2))throw new Exception("Not compatible types");
        return type1;
    }

    @Override
    public String visit(ArrayType n, String argu) {
        return "int[]";
    }
    @Override
    public String visit(BooleanType n, String argu) {
        return "boolean";
    }

    public String visit(IntegerType n, String argu) {
        return "int";
    }

    @Override
    public String visit(Identifier n, String argu) {
        return n.f0.toString();
    }

    @Override
    public String visit(VarDeclaration n, String argu) throws Exception{
        return n.f1.accept(this,null);
    }



       /**
    * f0 -> <INTEGER_LITERAL>
    */
    
    public String visit(IntegerLiteral n,String argu) throws Exception {
        return "int";
    }

    /**
      * f0 -> "true"
     */
    public String visit(TrueLiteral n,String argu) throws Exception {
        return "boolean";
    }

    /**
    * f0 -> "false"
    */
    public String visit(FalseLiteral n,String argu) throws Exception {
        return "boolean";
    }

    /**
    * f0 -> "this"                
    */
    public String visit(ThisExpression n,String argu) throws Exception {
        return argu.split("\\.")[0];
    }

    /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    public String visit(ArrayAllocationExpression n,String argu) throws Exception {
        return "int[]";
    }

    /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
    public String visit(AllocationExpression n,String argu) throws Exception {
        return n.f1.accept(this,null);
    }

    /*
    * f0 -> "!"                               
    * f1 -> PrimaryExpression()
    */
    public String visit(NotExpression n,String argu) throws Exception {
        String  p1=getType(n.f1.accept(this,argu),argu);  
        return p1;
    }
    /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
    public String visit(BracketExpression n,String argu) throws Exception {
        return n.f1.accept(this,argu);
    }

       /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
   public String visit(MessageSend n,String argu) throws Exception {


    String temp=getType(n.f0.accept(this,argu),argu);
    String arguments=null;
    String rval=null;
    int tflag=0;
    int unflag=0;
    while(!temp.equals(mvisitor.classes.get(temp))||tflag==0){
        if(temp.equals(mvisitor.classes.get(temp)))tflag++;

        if(mvisitor.argumentcheck.get(temp+"."+n.f2.accept(this,argu))!=null){
            arguments=mvisitor.argumentcheck.get(temp+"."+n.f2.accept(this,argu));
            rval=mvisitor.declarations.get(temp+"."+n.f2.accept(this,argu));
            if(rval==null){temp=mvisitor.classes.get(temp);continue;}
            unflag=1;
            break;
        }
        temp=mvisitor.classes.get(temp);
    }
    if(unflag==0) throw new Exception("Undefined function call");

    if(arguments.length()==0 && (n.f4.accept(this,argu))!=null)throw new Exception("Different arguments");

    if(arguments.length()!=0){
        String[] argumentList = (arguments).split(", ");
        String[] argumentList2 = (n.f4.accept(this,argu)).split(",[ ]*");
        
        if(argumentList.length!=argumentList2.length)throw new Exception("Different arguments");

        
        for(int k=0;k<argumentList.length;k++)
        {   

            int flag=0;
            String[] split1=argumentList[k].split(" ");
            String tempclass=split1[0];
            int flag2=0;
            
        
            while(!tempclass.equals(mvisitor.classes.get(tempclass))||flag2==0){
                if(tempclass.equals("int")||tempclass.equals("boolean")||tempclass.equals("int[]"))break;
                if(tempclass.equals(mvisitor.classes.get(tempclass)))flag2++;
                if(mvisitor.argumentcheck.get(argu.split("\\.")[0]+"."+argumentList2[k])==null&&mvisitor.declarations.get(argu.split("\\.")[0]+"."+argumentList2[k])==null)
                {
                    if(tempclass.equals(argumentList2[k])){flag=1; break;}

                }
                else
                if(tempclass.equals(mvisitor.argumentcheck.get(argu.split("\\.")[0]+"."+argumentList2[k]))||tempclass.equals(mvisitor.declarations.get(argu.split("\\.")[0]+"."+argumentList2[k]))){flag=1; break;}
               
                
                tempclass=mvisitor.classes.get(tempclass);
                if(tempclass==null)break;
            }
            String tempcl1=tempclass;
            tempclass=getType(argumentList2[k],argu);
            flag2=0;
           
            while(!tempclass.equals(mvisitor.classes.get(tempclass))||flag2==0){
                if(tempclass.equals("int")||tempclass.equals("boolean")||tempclass.equals("int[]"))break;
                if(tempclass.equals(mvisitor.classes.get(tempclass)))flag2++;
                if(mvisitor.argumentcheck.get(argu.split("\\.")[0]+"."+tempclass)==null&&mvisitor.declarations.get(argu.split("\\.")[0]+"."+tempclass)==null)
                {
                    if(split1[0].equals(tempclass)){flag=1; break;}
                    
                }
                else
                if(split1[0].equals(mvisitor.argumentcheck.get(argu.split("\\.")[0]+"."+tempclass))||split1[0].equals(mvisitor.declarations.get(argu.split("\\.")[0]+"."+tempclass))){flag=1; break;}
                
                
                tempclass=mvisitor.classes.get(tempclass);
                if(tempclass==null)break;
            }
            if(tempcl1.equals(tempclass))continue;
            if(flag==0)throw new Exception("Different arguments");
        }

    }
    return rval;
    }



   /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
    public String visit(ExpressionList n,String argu) throws Exception {
        String tmp=n.f0.accept(this,argu)+n.f1.accept(this,argu);
        return tmp;
     }
  
     /**
      * f0 -> ( ExpressionTerm() )*
      */
     public String visit(ExpressionTail n,String argu) throws Exception {
        
        
        NodeListOptional nd=n.f0;
        String str="";
        for(int i=0;i<nd.size();i++)
        {
            ExpressionTerm exprterm = (ExpressionTerm) nd.elementAt(i);
            str+=","+exprterm.accept(this,argu);
        }
        return str ;
     }
  
     /**
      * f0 -> ","
      * f1 -> Expression()
      */
     public String visit(ExpressionTerm n,String argu) throws Exception {
        return n.f1.accept(this,argu);
     }

     public String visit(PrimaryExpression n,String argu) throws Exception {
        return n.f0.accept(this,argu);
     }

        /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
   public String visit(ArrayLength n,String argu) throws Exception {
    return "int";
    }
       /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
    public String visit(PlusExpression n,String argu) throws Exception {
        String  p1=getType(n.f0.accept(this,argu),argu);  
        String p2=getType(n.f2.accept(this,argu),argu);  
        if(!p1.equals(p2))throw new Exception("Different types");
        return p1;
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
   public String visit(MinusExpression n,String argu) throws Exception {
    String  p1=getType(n.f0.accept(this,argu),argu);  
    String p2=getType(n.f2.accept(this,argu),argu);  
    if(!p1.equals(p2))throw new Exception("Different types");
    return p1;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
   public String visit(TimesExpression n,String argu) throws Exception {
    String  p1=getType(n.f0.accept(this,argu),argu);  
    String p2=getType(n.f2.accept(this,argu),argu);  

    if(!p1.equals(p2))throw new Exception("Different types");
    return p1;
   }
      /**
    * f0 -> PrimaryExpression()
    * f1 -> "&&"
    * f2 -> PrimaryExpression()
    */
    public String visit(AndExpression n,String argu) throws Exception {
        String  p1=getType(n.f0.accept(this,argu),argu);  
        String p2=getType(n.f2.accept(this,argu),argu);  
        if((!p1.equals(p2))||!p1.equals("boolean"))throw new Exception("Different types");
        return "boolean";
    }
  
     /**
      * f0 -> PrimaryExpression()
      * f1 -> "<"
      * f2 -> PrimaryExpression()
      */
    public String visit(CompareExpression n,String argu) throws Exception {
        String  p1=getType(n.f0.accept(this,argu),argu);  
        String p2=getType(n.f2.accept(this,argu),argu);  
        if((!p1.equals(p2))||!p1.equals("int"))throw new Exception("Different types");
        return "boolean";
    }

    /**
    * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
   public String visit(ArrayAssignmentStatement n,String argu) throws Exception {
     
    String a=n.f0.accept(this,null);
        
    String type1=getType(a,argu); 
    if(!type1.equals("int[]"))throw new Exception("UNDEFIEND");

    String type3=getType(n.f2.accept(this,argu),argu);  
    
    if(!type3.equals("int"))throw new Exception("Bad array expr");
    

    
    String type2=getType(n.f5.accept(this,argu),argu); 


    if(!type2.equals("int"))throw new Exception("Not compatible types");
    return type1;

   }

      /**
    * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
    public String visit(PrintStatement n,String argu) throws Exception {
        
        String type3=getType(n.f2.accept(this,argu),argu); 
        if(!type3.equals("int") && !type3.equals("int[]")&& !type3.equals("boolean"))throw new Exception("Bad print expr");
        return type3;
    }

         /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
   public String visit(ArrayLookup n,String argu) throws Exception {
        String  p1=getType(n.f0.accept(this,argu),argu);  
        String p2=getType(n.f2.accept(this,argu),argu);  
        if(!p1.equals("int[]")||!p2.equals("int"))throw new Exception("Different types");
        return "int";
    }


    public String getType(String f,String argu)
    {
        String type=mvisitor.argumentcheck.get(argu+"."+f);
        if(type==null)
        {
            String[] temp=(argu+"."+f).split("\\.");
            if(temp.length!=0)
            {
                type=mvisitor.declarations.get(temp[0]+"."+temp[temp.length-1]);
            }      
        }
        if(type == null)
        {
            
                String[] temp=(argu).split("\\.");
                int flag=0;
                if(temp.length!=0)
                while(!temp[0].equals(mvisitor.classes.get(temp[0]))||flag==0)
                    {   
                        type=mvisitor.declarations.get(mvisitor.classes.get(temp[0])+"."+f);
                        if(type!=null)break;
                        temp[0]=mvisitor.classes.get(temp[0]);
                        if(temp[0].equals(mvisitor.classes.get(temp[0])))
                        flag=1;
                    }
        }
        if(type == null)type=f;  
        return type;
    }
       /**
    * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> "else"
    * f6 -> Statement()
    */
   public String visit(IfStatement n,String argu) throws Exception {
        String type1=getType(n.f2.accept(this,argu),argu);
        if(!type1.equals("boolean")&&!type1.equals("int"))throw new Exception("Bad while expr");
        n.f4.accept(this,argu);
        n.f6.accept(this,argu);
        return null;
    }

    /**
    * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    *f3 -> ")"
    * f4 -> Statement()
    */
    public String visit(WhileStatement n,String argu) throws Exception {
        String type1=getType(n.f2.accept(this,argu),argu);
        if(!type1.equals("boolean")&&!type1.equals("int"))throw new Exception("Bad while expr");
        n.f4.accept(this,argu);
        return null;
    }

}

