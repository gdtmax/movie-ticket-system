package edu.nyu.cs6103.movietickets.shared.dto;
public record AdminTheaterRequest(long theaterId, String name, String location,
        int rowCount, int seatsPerRow) { }
