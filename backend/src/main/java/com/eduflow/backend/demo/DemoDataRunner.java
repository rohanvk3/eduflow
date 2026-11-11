package com.eduflow.backend.demo;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
@Component @Profile("demo") @ConditionalOnProperty(name="eduflow.demo.seed.enabled",havingValue="true")
public class DemoDataRunner implements ApplicationRunner {
    final DemoDataSeeder seeder;final Environment env;final ObjectMapper mapper;final ConfigurableApplicationContext context;
    public DemoDataRunner(DemoDataSeeder seeder,Environment env,ObjectMapper mapper,ConfigurableApplicationContext context){this.seeder=seeder;this.env=env;this.mapper=mapper;this.context=context;}
    public void run(ApplicationArguments args)throws Exception{
        var result=seeder.seed(); // Proxy returns only after the atomic database transaction commits.
        String json=mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        String location=env.getProperty("eduflow.demo.report-path","");
        if(!location.isBlank()){Path path=Path.of(location).toAbsolutePath();Files.createDirectories(path.getParent());Files.writeString(path,json);}
        System.out.println("EDUPRIME_DEMO_VERIFIED\n"+json);
        if(env.getProperty("eduflow.demo.seed.exit-after-run",Boolean.class,true))SpringApplication.exit(context);
    }
}
