package com.eduflow.backend.demo;

import java.sql.*;
import java.util.*;
import org.springframework.context.*;
import org.springframework.core.env.Environment;

/** Runs before datasource/JPA/settings initialization, not just before the seed runner. */
public class DemoSeedGuard implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    public static final String NAMESPACE="EDUPRIME_DEMO_V1";
    public static final String CONFIRM="EDUPRIME_SYNTHETIC_ONLY";
    public void initialize(ConfigurableApplicationContext context) {
        var env=context.getEnvironment();
        if (!validateSettings(env)) return;
        try(var connection=DriverManager.getConnection(env.getRequiredProperty("spring.datasource.url"),env.getRequiredProperty("spring.datasource.username"),env.getRequiredProperty("spring.datasource.password"))) {
            inspect(connection);
        } catch(SQLException ex) { throw new IllegalStateException("Demo database preflight failed; use a new isolated schema imported from the baseline",ex); }
    }
    public static boolean validateSettings(Environment env) {
        var profiles=Set.of(env.getActiveProfiles());
        boolean enabled=env.getProperty("eduflow.demo.seed.enabled",Boolean.class,false);
        if(!enabled&&!profiles.contains("demo"))return false;
        if(profiles.contains("prod")||profiles.contains("production"))throw new IllegalStateException("Demo execution is forbidden under production profiles");
        if(!profiles.contains("demo"))throw new IllegalStateException("Explicit demo profile is required");
        if(enabled&&!CONFIRM.equals(env.getProperty("eduflow.demo.seed.confirm")))throw new IllegalStateException("Explicit demo confirmation is required");
        String url=env.getRequiredProperty("spring.datasource.url");
        if(!url.matches("jdbc:mysql://(?:127\\.0\\.0\\.1|localhost):[0-9]+/eduflow_demo_[a-z0-9_]+(?:\\?.*)?"))throw new IllegalStateException("Demo requires an isolated loopback MySQL eduflow_demo_* schema and explicit port");
        if(!"validate".equals(env.getProperty("spring.jpa.hibernate.ddl-auto")))throw new IllegalStateException("Import a reviewed baseline; demo schema handling must be validate");
        if(!"127.0.0.1".equals(env.getProperty("server.address")))throw new IllegalStateException("Demo must bind to loopback");
        if(enabled&&(env.getProperty("app.notifications.scheduler-enabled",Boolean.class,true)||env.getProperty("app.exams.expiry-enabled",Boolean.class,true)))throw new IllegalStateException("Disable background schedulers during historical demo replay");
        if(enabled)for(String role:List.of("ADMIN","TEACHER","STUDENT","PARENT"))password(env,role);
        return true;
    }
    public static String password(Environment env,String role) {
        String value=env.getRequiredProperty("DEMO_"+role+"_PASSWORD");
        if(value.length()<16||value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72||!value.matches("(?s).*[A-Z].*")||!value.matches("(?s).*[a-z].*")||!value.matches("(?s).*[0-9].*")||!value.matches("(?s).*[^A-Za-z0-9].*"))throw new IllegalStateException("DEMO_"+role+"_PASSWORD must be 16+ characters, at most 72 UTF-8 bytes, with upper/lower/digit/symbol");
        return value;
    }
    public static void inspect(Connection connection)throws SQLException {
        long markers=count(connection,"select count(*) from audit_logs where action='DEMO_DATASET_COMPLETED' and entity_type='DEMO'");
        if(markers==1) {
            if(count(connection,"select count(*) from institute_settings where institute_code='"+NAMESPACE+"'")!=1||count(connection,"select count(*) from users where email not like '%@eduprime.example'")!=0)
                throw new IllegalStateException("Demo database ownership is mixed or unknown; refusing to seed");
            return;
        }
        if(markers!=0)throw new IllegalStateException("Conflicting demo completion markers");
        var tables=new ArrayList<String>();
        try(var statement=connection.createStatement();var rows=statement.executeQuery("select table_name from information_schema.tables where table_schema=database() and table_type='BASE TABLE'")){while(rows.next())tables.add(rows.getString(1));}
        if(tables.size()!=48)throw new IllegalStateException("Demo requires the complete reviewed 48-table baseline");
        for(String table:tables) {
            if(!table.matches("[a-z0-9_]+"))throw new IllegalStateException("Unexpected demo table identifier");
            if(Set.of("institute_settings","notification_coordinators").contains(table))continue;
            if(count(connection,"select count(*) from `"+table+"`")!=0)throw new IllegalStateException("Existing unowned data found in "+table+"; use a NEW demo database. Unknown databases cannot be confirmed into use.");
        }
        if(count(connection,"select count(*) from institute_settings where id<>1 or institute_code<>'EDUFLOW'")!=0)throw new IllegalStateException("Existing institute settings are not a fresh baseline");
    }
    private static long count(Connection c,String sql)throws SQLException{try(var s=c.createStatement();var r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
}
