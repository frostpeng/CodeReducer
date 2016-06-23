import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
  
public class CodeGetASTVisitor extends ASTVisitor {  
	public HashMap<String,ASTClass> classMap;
	private String fullClassName;
	public CodeGetASTVisitor(){
	}
	
    @Override  
    public boolean visit(TypeDeclaration node) {
    	ASTClass clazz=new ASTClass();
    	if(node!=null){
    		fullClassName=node.getName().toString();
    		ASTNode parentNode=node.getParent();
    		while(!(parentNode instanceof CompilationUnit)){
    			if(parentNode instanceof TypeDeclaration){
    				TypeDeclaration typeD=(TypeDeclaration)parentNode;
    				fullClassName=typeD.getName().toString()+"."+fullClassName;
    			}else{
    				//类中其它的方法中定义的新类从这里走，加上一个#.，表示外部无法使用，只是为了区分开来
    				fullClassName="#."+fullClassName;
    			}
    			parentNode=parentNode.getParent();
    		}
    		CompilationUnit cu=(CompilationUnit)parentNode;
    		fullClassName=cu.getPackage().getName()+"."+fullClassName;
    		if(node.getSuperclassType()!=null){
    			String superName=node.getSuperclassType().toString();
    			clazz.parentName=superName;
//    			if(superName.contains(".")){
//	    			String[] parentNameList=superName.split("\\.");
//	    			if(parentNameList.length>0){
//	    				clazz.parentName=parentNameList[parentNameList.length-1];
//	    			}
//    			}else{
//    				clazz.parentName=superName;
//    			}
    		}
    		if(node.getMethods()!=null){
        		if(clazz.funcs==null){
        			clazz.funcs=new ArrayList<String>();
        		}
        		for(MethodDeclaration method:node.getMethods()){
        			clazz.funcs.add(method.getName().toString());
        		}
        	}
    		clazz.packageName=cu.getPackage().getName().toString();
    		clazz.clazzName=fullClassName;
        	if(classMap==null){
        		classMap=new HashMap<String,ASTClass>();
        	}
        	classMap.put(node.getName().toString(),clazz);
    	}
    	
        return true;  
    }  
} 