package com.axiomalaska.sos.injector.db;

import com.axiomalaska.sos.data.DocumentMember;
import com.axiomalaska.sos.data.DocumentMemberImp;
import com.google.common.base.Strings;

public class DatabaseSosInjectorHelper {
    public static Double getDouble(Object obj) {
        if (obj instanceof Double) {
            return (Double) obj;
        } else if (obj instanceof String) {
            return Double.valueOf((String) obj);
        }
        return null;
    }
    
    public static void requireString(String name, String value) {
        if (Strings.isNullOrEmpty(value)){
            throw new RuntimeException(name + " cannot be null or empty!");
        }
    }
    
    public static void requireNonNull(String name, Object value){
        if (value == null) {
            throw new IllegalStateException(name + " must not be null!");
        }
    }
    
    public static DocumentMember makeDocument(String name, String arcrole, String url,
            String format, String description) {
        DocumentMemberImp document = new DocumentMemberImp();
        if (!Strings.isNullOrEmpty(name)) {
            document.setName(name);    
        }
        if (!Strings.isNullOrEmpty(arcrole)) {
            document.setArcrole(arcrole);
        }
        if (!Strings.isNullOrEmpty(url)) {
            document.setOnlineResource(url);
        }
        if (!Strings.isNullOrEmpty(format)) {
            document.setFormat(format);
        }
        if (!Strings.isNullOrEmpty(description)) {
            document.setDescription(description);
        }
        return document;
    }
}
