package com.smartrent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootTest
@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class })
class SmartRentApplicationTests {

    @Test
    void contextLoads() {
        // Test chỉ đảm bảo context load thành công
		assert true;
    }
}
