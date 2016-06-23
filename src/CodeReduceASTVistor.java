import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import com.tencent.javaparser.ast.ModifierSet;

public class CodeReduceASTVistor extends ASTVisitor { 
	public static int privateMethodCount=0;
	public static int privateFieldCount=0;
	public static int initUselessCount=0;
	private HashMap<String,ASTClass> astDirectoryClasses;
	public boolean needModify=false;
	private StringBuilder changeBuilder=new StringBuilder("");
	
	public CodeReduceASTVistor(HashMap<String,ASTClass> astDirectoryClasses){
		this.astDirectoryClasses=astDirectoryClasses;
	}
	
	
	@Override  
    public boolean visit(TypeDeclaration node) {
		if(node!=null){
			changeBuilder.append("className:"+node.getName().toString()+"\n");
			if(node.getMethods()!=null){
				for(MethodDeclaration method:node.getMethods()){
					int iModifier=method.getModifiers();
//					if(!ModifierSet.isStatic(iModifier)){
//						addSuperToMethod(method,node);
//					}
					if(ModifierSet.isPrivate(iModifier) && canMethodModify(node.getName().toString(),method.getName().toString())){
						//没法直接set，只能用remove的方式了
						List modifiers=method.modifiers();
						for(Object obj:modifiers){
							if(obj instanceof Modifier){
								Modifier modifer =(Modifier)obj;
								if(modifer.isPrivate()){
									if(CodeReducer.needPrintResult){
										changeBuilder.append("before:\n");
										changeBuilder.append(method.toString()+"\n");
									}
									modifiers.remove(obj);
									if(CodeReducer.needPrintResult){
										changeBuilder.append("after:\n");
										changeBuilder.append(method.toString()+"\n");
										privateMethodCount++;
									}
									needModify=true;
									break;
								}
							}
						}
					}
        		}
			}
			if(node.getFields()!=null){
				for(FieldDeclaration field:node.getFields()){
					int iModifier=field.getModifiers();
					if(ModifierSet.isPrivate(iModifier)){
						List modifiers=field.modifiers();
						for(Object obj:modifiers){
							if(obj instanceof Modifier){
								Modifier modifer =(Modifier)obj;
								if(modifer.isPrivate()){
									if(CodeReducer.needPrintResult){
										changeBuilder.append("before:\n");
										changeBuilder.append(field.toString()+"\n");
									}
									modifiers.remove(obj);
									if(CodeReducer.needPrintResult){
										changeBuilder.append("after:\n");
										changeBuilder.append(field.toString()+"\n");
										privateFieldCount++;
									}
									needModify=true;
									break;
								}
							}
						}
					}
					if(!ModifierSet.isFinal(iModifier) && !node.isInterface()){
						List fragments=field.fragments();
						Type type=field.getType();
						for(Object obj:fragments){
	 						VariableDeclarationFragment f=(VariableDeclarationFragment)obj;
	 						Expression ex=f.getInitializer();
	 						if(ex!=null && type instanceof PrimitiveType){
		 							if(ex instanceof NumberLiteral && ("0".equals(((NumberLiteral) ex).getToken())
		 									||"0.0f".equals(((NumberLiteral) ex).getToken())||"0.0F".equals(((NumberLiteral) ex).getToken())
		 									||"0.0d".equals(((NumberLiteral) ex).getToken())||"0.0D".equals(((NumberLiteral) ex).getToken())
		 									||"0.0l".equals(((NumberLiteral) ex).getToken())||"0.0L".equals(((NumberLiteral) ex).getToken()))){
		 								removeInitUselessCode(field,f);
		 							}
		 							if(ex instanceof BooleanLiteral && ((BooleanLiteral) ex).booleanValue()==false){
		 								removeInitUselessCode(field,f);
		 							}
	 						}
	 						if(ex!=null && type instanceof SimpleType && ex instanceof NullLiteral){
	 							removeInitUselessCode(field,f);
	 						}
						}
					}
					
					
        		}
			}
			if(needModify){
				CodeReducer.appendToResultFile(changeBuilder.toString());
			}
		}
		return true;
	}
	
	
	private void removeInitUselessCode(FieldDeclaration field,VariableDeclarationFragment f){
		if(CodeReducer.needPrintResult){
			changeBuilder.append("before:\n");
			changeBuilder.append(field.toString()+"\n");
		}
		f.setInitializer(null);
		if(CodeReducer.needPrintResult){
			changeBuilder.append("after:\n");
			changeBuilder.append(field.toString()+"\n");
			initUselessCount++;
		}
		needModify=true;
		
	}
	
	private boolean canMethodModify(String className,String methodName){
		if(astDirectoryClasses==null ||astDirectoryClasses.values()==null){
			return true;
		}
		ASTClass currentClazz=astDirectoryClasses.get(className);
		for(ASTClass clazz:astDirectoryClasses.values()){
			if(clazz.clazzName.equals(currentClazz.parentName)||currentClazz.clazzName.equals(clazz.parentName)){
				if(clazz.funcs.contains(methodName)){
					return false;
				}
			}
		}
		return true;
	}
	
	private void addSuperToMethod(MethodDeclaration method,TypeDeclaration node){
		if(method!=null && method.getBody()!=null && method.getBody().statements()!=null){
			for(Object obj:method.getBody().statements()){
				if(obj instanceof ExpressionStatement){
					ExpressionStatement statement=(ExpressionStatement)obj;
					if(statement.getExpression()!=null && statement.getExpression()  instanceof MethodInvocation){
						MethodInvocation invovation=(MethodInvocation)statement.getExpression();
						String mName=invovation.getName().toString();
						if(mName!=null && invovation.getExpression()==null){
							boolean needSuper=true;
							TypeDeclaration currentNode=node;
							while(currentNode!=null){
								for(Object mObj:currentNode.getMethods()){
									MethodDeclaration mMthod=(MethodDeclaration)mObj;
									if(mMthod!=null && mName.equals(mMthod.getName().toString())){
										needSuper=false;
									}
								}
								ASTNode parentNode=currentNode.getParent();
								while(parentNode!=null && !(parentNode instanceof TypeDeclaration)){
									parentNode=parentNode.getParent();
								}
								if(parentNode!=currentNode){
									currentNode=(TypeDeclaration)parentNode;
								}else{
									currentNode=null;
								}
							}
							if(needSuper){
								AST ast = node.getAST();  
								SuperMethodInvocation superInvocation=ast.newSuperMethodInvocation();
								superInvocation.setName(ast.newSimpleName(invovation.getName().toString()));
								List argments=invovation.arguments();
								superInvocation.arguments().addAll(ASTNode.copySubtrees(superInvocation.getAST(), invovation.arguments()));
								statement.setExpression(superInvocation);
								needModify=true;
							}
						}
					}
				}
			}
		}
	}
}
