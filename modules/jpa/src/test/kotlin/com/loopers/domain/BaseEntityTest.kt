package com.loopers.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("BaseEntity equals/hashCode")
class BaseEntityTest {
    private class TestEntityA : BaseEntity() {
        fun assignId(value: Long) {
            id = value
        }
    }

    private class TestEntityB : BaseEntity() {
        fun assignId(value: Long) {
            id = value
        }
    }

    @Nested
    @DisplayName("equals")
    inner class Equals {
        @Test
        @DisplayName("동일 id를 가진 같은 타입 엔티티는 동등하다")
        fun equals_sameIdSameType_true() {
            val a = TestEntityA().apply { assignId(1L) }
            val b = TestEntityA().apply { assignId(1L) }

            assertThat(a).isEqualTo(b)
        }

        @Test
        @DisplayName("다른 id를 가진 엔티티는 동등하지 않다")
        fun equals_differentId_false() {
            val a = TestEntityA().apply { assignId(1L) }
            val b = TestEntityA().apply { assignId(2L) }

            assertThat(a).isNotEqualTo(b)
        }

        @Test
        @DisplayName("id가 null인 두 엔티티는 동등하지 않다")
        fun equals_bothIdNull_false() {
            val a = TestEntityA()
            val b = TestEntityA()

            assertThat(a).isNotEqualTo(b)
        }

        @Test
        @DisplayName("같은 객체는 자기 자신과 동등하다 (reflexive)")
        fun equals_sameInstance_true() {
            val a = TestEntityA()

            assertThat(a).isEqualTo(a)
        }

        @Test
        @DisplayName("다른 타입 엔티티는 같은 id여도 동등하지 않다")
        fun equals_differentType_false() {
            val a = TestEntityA().apply { assignId(1L) }
            val b = TestEntityB().apply { assignId(1L) }

            assertThat(a).isNotEqualTo(b)
        }

        @Test
        @DisplayName("null과 비교하면 동등하지 않다")
        fun equals_null_false() {
            val a = TestEntityA().apply { assignId(1L) }

            assertThat(a).isNotEqualTo(null)
        }
    }

    @Nested
    @DisplayName("hashCode")
    inner class HashCode {
        @Test
        @DisplayName("id가 변경되어도 hashCode는 동일하다")
        fun hashCode_idChange_consistent() {
            val entity = TestEntityA()
            val hashBefore = entity.hashCode()

            entity.assignId(1L)
            val hashAfter = entity.hashCode()

            assertThat(hashBefore).isEqualTo(hashAfter)
        }

        @Test
        @DisplayName("같은 타입의 엔티티는 동일한 hashCode를 가진다")
        fun hashCode_sameType_equal() {
            val a = TestEntityA().apply { assignId(1L) }
            val b = TestEntityA().apply { assignId(2L) }

            assertThat(a.hashCode()).isEqualTo(b.hashCode())
        }
    }
}
