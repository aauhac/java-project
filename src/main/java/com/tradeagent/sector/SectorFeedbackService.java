package com.tradeagent.sector;

import org.springframework.stereotype.Service;

@Service
public class SectorFeedbackService {

    public String buildSectorFeedback(PortfolioSectorDiagnosticDto dto) {
        if (dto.strongExposure().compareTo(dto.weakExposure()) > 0) {
            return "현재 포트폴리오는 강한 섹터에 더 많이 노출되어 있습니다.";
        }
        if (dto.strongExposure().compareTo(dto.weakExposure()) < 0) {
            return "현재 포트폴리오는 강한 섹터보다 약한 섹터에 더 많이 노출되어 있습니다.";
        }
        return "현재 포트폴리오는 강한 섹터와 약한 섹터 노출이 비슷한 수준입니다.";
    }
}
