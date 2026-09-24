package com.plantahub.api.repository;

import com.plantahub.api.domain.site.SiteSettingsRow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteSettingsRepository extends JpaRepository<SiteSettingsRow, Short> {
}
