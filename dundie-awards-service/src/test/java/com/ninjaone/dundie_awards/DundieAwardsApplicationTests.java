package com.ninjaone.dundie_awards;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import({TestcontainersConfiguration.class, SynchronousActivityConfiguration.class})
class DundieAwardsApplicationTests {

	@Test
	void contextLoads() {
	}

}
