package com.example.javamate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = JavaMateApplication.class)
@ActiveProfiles("test")
class JavaMateApplicationTests {

        @Test
        void contextLoads() {
        }

}
