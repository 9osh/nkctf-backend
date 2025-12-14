package cn.edu.ndky.nkctf;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("cn.edu.ndky.nkctf.mapper")
public class NkctfApplication {

	public static void main(String[] args) {
		SpringApplication.run(NkctfApplication.class, args);
	}

}
