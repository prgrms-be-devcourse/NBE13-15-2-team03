package com.kdt.reservation.point;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {
    private final PointRepository pointRepository;
    private final PointHistoryRepository pointHistoryRepository;


    public Point use() {
        return null;
    }

    public Point earn() {
        return null;
    }

    public Point refund() {
        return null;
    }
}
