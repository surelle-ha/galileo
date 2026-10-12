package com.galileo.api;

import com.galileo.api.role.Permission;
import com.galileo.api.role.RoleName;
import com.galileo.api.role.RoleResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CacheSerializationTests {

    @Autowired
    private RedisCacheConfiguration cacheConfiguration;

    @Test
    void roleCatalogueRoundTripsThroughTheRedisJsonSerializer() {
        var original = new ArrayList<>(List.of(new RoleResponse(
                1L,
                RoleName.ADMIN,
                "Full platform administration",
                Set.of(Permission.USER_READ, Permission.ROLE_ASSIGN)
        )));

        var serialization = cacheConfiguration.getValueSerializationPair();
        Object restored = serialization.read(serialization.write(original));

        assertThat(restored).isInstanceOf(ArrayList.class);
        assertThat((List<?>) restored).hasSize(1);
        assertThat(((List<?>) restored).getFirst()).isEqualTo(original.getFirst());
    }
}
