package org.example.schoolweb.domain.pledge.repository

import org.example.schoolweb.domain.pledge.entity.Pledge
import org.springframework.data.jpa.repository.JpaRepository

interface PledgeRepository : JpaRepository<Pledge, Long> {
    fun findAllByOrderByDisplayOrderAscIdAsc(): List<Pledge>
}
