package zerobase.weather.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import zerobase.weather.domain.Memo;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
// @Rollback(false)
class JdbcMemoRepositoryTest {
    @Autowired
    JdbcMemoRepository jdbcMemoRepository;

    @Test
    void insetMemoTest() {
        // given
        Memo newMemo = new Memo(1, "this is new memo~");
        // when
        jdbcMemoRepository.save(newMemo);
        // then
        Optional<Memo> result = jdbcMemoRepository.findById(1);
        assertEquals(result.get().getText(), "this is new memo~");

    }

}