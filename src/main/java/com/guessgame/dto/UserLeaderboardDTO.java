package com.guessgame.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserLeaderboardDTO {
    private String username;
    private int score;

    public UserLeaderboardDTO(String username, int score) {
        this.username = username;
        this.score = score;
    }

}
