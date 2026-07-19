package com.clicker.repository;

import com.clicker.domain.ProxyPort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProxyPortRepository extends JpaRepository<ProxyPort, Long> {

    List<ProxyPort> findByPoolKey(String poolKey);

    void deleteByPoolKey(String poolKey);

    List<ProxyPort> findAllByOrderByCreatedAtAsc();
}
