package org.example.schoolweb.domain.pledge.repository

import org.example.schoolweb.domain.pledge.entity.PledgeProgress
import org.springframework.data.jpa.repository.JpaRepository

interface PledgeProgressRepository : JpaRepository<PledgeProgress, Long>
