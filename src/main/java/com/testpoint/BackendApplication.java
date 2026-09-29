package com.testpoint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) throws IOException {
		loadEnv();
		SpringApplication.run(BackendApplication.class, args);
	}

	static void loadEnv() throws IOException {
		Path cwd = Path.of("").toAbsolutePath();
		Path[] candidates = {cwd.resolve(".env"), cwd.resolve("backend").resolve(".env")};
		for (Path candidate : candidates) {
			if (!Files.isRegularFile(candidate)) {
				continue;
			}
			for (String raw : Files.readAllLines(candidate)) {
				String line = raw.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}
				int split = line.indexOf('=');
				if (split < 1) {
					continue;
				}
				String key = line.substring(0, split).trim();
				String value = line.substring(split + 1).trim();
				if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
					value = value.substring(1, value.length() - 1);
				}
				if (System.getenv(key) == null && System.getProperty(key) == null) {
					System.setProperty(key, value);
				}
			}
			return;
		}
	}
}
