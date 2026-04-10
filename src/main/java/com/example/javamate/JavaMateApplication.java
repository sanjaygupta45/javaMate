package com.example.javamate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
		org.springframework.ai.model.mistralai.autoconfigure.MistralAiChatAutoConfiguration.class,
		org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration.class
})
public class JavaMateApplication {

	public static void main(String[] args) {
		SpringApplication.run(JavaMateApplication.class, args);
	}

}
