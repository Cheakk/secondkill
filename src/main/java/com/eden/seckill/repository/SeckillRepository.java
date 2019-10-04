package com.eden.seckill.repository;
import org.springframework.data.jpa.repository.JpaRepository;

import com.eden.seckill.common.entity.Seckill;

public interface SeckillRepository extends JpaRepository<Seckill, Long> {
	
	
}
