package com.tencent.javaparser.ast;

public enum AccessSpecifier {

    PUBLIC("public"),
    PRIVATE("private"),
    PROTECTED("protected"),
    DEFAULT("");

    private String codeRepresenation;

    private AccessSpecifier(String codeRepresentation) {
        this.codeRepresenation = codeRepresentation;
    }

    public String getCodeRepresenation(){
        return this.codeRepresenation;
    }
}
