package com.gems.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "microservices")
public class MicroserviceConfig {
    
    private ServiceConfig auth;
    private ServiceConfig admin;
    private ServiceConfig education;
    
    public static class ServiceConfig {
        private String url;
        private String id;
        private String path;
        
        public String getUrl() {
            return url;
        }
        
        public void setUrl(String url) {
            this.url = url;
        }
        
        public String getId() {
            return id;
        }
        
        public void setId(String id) {
            this.id = id;
        }
        
        public String getPath() {
            return path;
        }
        
        public void setPath(String path) {
            this.path = path;
        }
    }
    
    public ServiceConfig getAuth() {
        return auth;
    }
    
    public void setAuth(ServiceConfig auth) {
        this.auth = auth;
    }
    
    public ServiceConfig getAdmin() {
        return admin;
    }
    
    public void setAdmin(ServiceConfig admin) {
        this.admin = admin;
    }
    
    public ServiceConfig getEducation() {
        return education;
    }
    
    public void setEducation(ServiceConfig education) {
        this.education = education;
    }
}
