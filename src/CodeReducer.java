
import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;

public class CodeReducer {
	private static String resultPath="codelog.txt";
	public static final boolean needPrintResult=false;
	
	 private static final Map<String, String> formatterOptions = DefaultCodeFormatterConstants.getEclipseDefaultSettings();
	    static {
	        formatterOptions.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_7);
	        formatterOptions.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_7);
	        formatterOptions.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_7);
	        formatterOptions.put(DefaultCodeFormatterConstants.FORMATTER_TAB_CHAR, JavaCore.SPACE);
	        formatterOptions.put(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, "2");
	        formatterOptions.put(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, "200");
	        formatterOptions.put(DefaultCodeFormatterConstants.FORMATTER_JOIN_LINES_IN_COMMENTS, DefaultCodeFormatterConstants.FALSE);
	        formatterOptions.put(
	            DefaultCodeFormatterConstants.FORMATTER_ALIGNMENT_FOR_ENUM_CONSTANTS,
	            DefaultCodeFormatterConstants.createAlignmentValue(
	            true,
	            DefaultCodeFormatterConstants.WRAP_ONE_PER_LINE,
	            DefaultCodeFormatterConstants.INDENT_ON_COLUMN));
	    }
	    
	public static void main(String[] args) throws IOException{
		String inputDir = args[0];
		long time=System.currentTimeMillis();
		removeASTClazzListByDir(inputDir);
//		removePrivateByFile("./Demo.java",null);
//		getASTClazzByFile("./Demo.java");
		appendToResultFile("privateMethodCount:"+CodeReduceASTVistor.privateMethodCount
				+"\nprivateFieldCount:"+CodeReduceASTVistor.privateFieldCount
				+"\ninitUselessCount:"+CodeReduceASTVistor.initUselessCount+"\n");
		appendToResultFile("all over cost"+(System.currentTimeMillis()-time)+"\n");
	
		}
	
	private static void removeASTClazzListByDir(String dirPath){
		File dirFile=new File(dirPath);
		if(dirFile.exists() && dirFile.isDirectory()){
			HashMap<String,ASTClass> astDirectoryClasses=new HashMap<String,ASTClass>();
			File[] tempList = dirFile.listFiles();
			for(File file:tempList){
				if(file.isFile()&&file.getAbsolutePath().endsWith(".java")){
					HashMap<String,ASTClass> astFileClasses=getASTClazzByFile(file.getAbsolutePath());
					if(astFileClasses!=null){
						astDirectoryClasses.putAll(astFileClasses);
					}
				}
				//文件夹递归处理
				if(file.isDirectory()){
					removeASTClazzListByDir(file.getAbsolutePath());
				}
			}
			for(File file:tempList){
				if(file.isFile()&&file.getAbsolutePath().endsWith(".java")){
					try{
						removePrivateByFile(file.getAbsolutePath(),astDirectoryClasses);
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
			
			
		}
	}
	
	public static void removePrivateByFile(String path,HashMap<String,ASTClass> astDirectoryClasses){
	    String source=getSourceByPath(path);
		CompilationUnit cu = getCompilationUnit(source, AST.JLS4);
		CodeReduceASTVistor visitor=new CodeReduceASTVistor(astDirectoryClasses);
		cu.recordModifications();
		cu.accept(visitor);
		if(visitor.needModify){
			saveCuDiffToFile(path,source,cu);
		}
	}
	
	public static void saveCuDiffToFile(String path,String source,CompilationUnit cu){
	    Document document = new Document(source);
		TextEdit edits = cu.rewrite(document, formatterOptions); //树上的变化生成了像diff一样的东西
		try {
			edits.apply(document);
			toFile(document.get(),path);
		} catch (MalformedTreeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} //应用diff

	}
	
	
	
	
	public static void toFile(String source, String outputPath)
	{
		try{
	          // Create file 
			  FileOutputStream fstream = new FileOutputStream(new File(outputPath));
	          OutputStreamWriter out = new OutputStreamWriter(fstream,"utf-8");
	          out.write(source);
	          //Close the output stream
	          out.close();
	    }catch (Exception e){//Catch exception if any
	          System.err.println("Error: " + e.getMessage());
	    }
	}
	
	public static void appendToResultFile(String source)
	{
		if(needPrintResult){
			try{
		          // Create file 
				 FileWriter writer = new FileWriter(resultPath, true);
				 writer.write(source);
		          //Close the output stream
				 writer.close();
		    }catch (Exception e){//Catch exception if any
		          System.err.println("Error: " + e.getMessage());
		    }
		}
	}
	
	
	public static HashMap<String,ASTClass> getASTClazzByFile(String path){
		String source=getSourceByPath(path);
		CompilationUnit cu=getCompilationUnit(source,AST.JLS4);
	    PackageDeclaration packageDec=cu.getPackage();
	    if(packageDec!=null && packageDec.getName()!=null){
	    	CodeGetASTVisitor visitor = new CodeGetASTVisitor(); 
	    	cu.accept(visitor);
	    	return visitor.classMap;
	    }
	    return null;
	}
	
	private static String getSourceByPath(String path){
	    try {
	    	 File file=new File(path);
	    	 FileInputStream fileIn = new FileInputStream(file);
	         // CharsetDecoder
	         InputStreamReader inRead = new InputStreamReader(fileIn,"utf-8");
	         int size = (int) (file.length());
	         char[] input = new char[size];
	         inRead.read(input);
	         String source = new String(input).trim();
	         inRead.close();
	         return source;
	    } catch (FileNotFoundException e) {
	      e.printStackTrace();
	    } catch (IOException e) {
	      e.printStackTrace();
	    }
	    return null;
	}
	
	private static CompilationUnit getCompilationUnit(String input,int jls){
	    ASTParser astParser = ASTParser.newParser(jls);
	    Map<String, String> options = JavaCore.getOptions();
	    astParser.setSource(input.toCharArray());
	    astParser.setKind(ASTParser.K_COMPILATION_UNIT);
	    astParser.setResolveBindings(true);
	    astParser.setCompilerOptions(options);
	    astParser.setStatementsRecovery(true);
	    astParser.setBindingsRecovery(true);
	    CompilationUnit cu = (CompilationUnit) astParser.createAST(null); //这个参数是IProgessMonitor,用于GUI的进度显示,我们不需要，填个null. 返回值是AST的根结点
	    return cu;
	}

}
