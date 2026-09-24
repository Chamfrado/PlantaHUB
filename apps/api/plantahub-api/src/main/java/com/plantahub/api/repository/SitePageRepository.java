package com.plantahub.api.repository;

import com.plantahub.api.domain.site.SitePage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SitePageRepository extends JpaRepository<SitePage, String> {

    List<SitePage> findAllByOrderBySlugAsc();
}
