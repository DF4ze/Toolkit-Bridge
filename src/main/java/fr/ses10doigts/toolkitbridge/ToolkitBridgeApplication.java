package fr.ses10doigts.toolkitbridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ToolkitBridgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToolkitBridgeApplication.class, args);
    }

}
