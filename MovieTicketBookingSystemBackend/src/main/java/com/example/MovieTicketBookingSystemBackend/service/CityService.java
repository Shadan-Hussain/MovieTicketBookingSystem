package com.example.MovieTicketBookingSystemBackend.service;

import com.example.MovieTicketBookingSystemBackend.dto.CityResponse;
import com.example.MovieTicketBookingSystemBackend.model.City;
import com.example.MovieTicketBookingSystemBackend.repository.CityRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CityService {

    private final CityRepository cityRepository;

    public CityService(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    public List<CityResponse> listCities() {
        return cityRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private CityResponse toResponse(City c) {
        return new CityResponse(c.getCityId(), c.getName(), c.getStateCode());
    }
}
