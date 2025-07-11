package com.guessgame.controller;

import com.guessgame.dto.UserLeaderboardDTO;
import com.guessgame.entity.User;
import com.guessgame.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GameController {
    private final UserRepository userRepository;

    private final Random random = new Random();

    private static final int TURNS_TO_BUY = 5; // Số lượt mua

    private static final int POINTS_PER_WIN = 1; // Số điểm cộng cho mỗi lượt chơi

    private static final int MAX_SERVER_NUMBER = 5; // Số lớn nhất mà máy chủ có thể chọn

    private static final double WIN_PROBABILITY = 0.05; // Xác suất thắng của người chơi

    /**
     * Người dùng đoán số do máy chủ chọn.
     *
     * @param userDetails Thông tin người dùng đã đăng nhập.
     * @param number      Số mà người dùng đoán.
     * @return Map<String, Object>
     * Thông báo kết quả đoán, số do máy chủ chọn, điểm số và lượt chơi còn lại.
     */
    @Transactional
    @PostMapping("/guess")
    public Map<String, Object> guess(@AuthenticationPrincipal UserDetails userDetails,
                                     @RequestParam int number) {
        User currentUser = findUserByUserDetailsWithLock(userDetails);
        int turnsLeft = currentUser.getTurns();
        if (turnsLeft <= 0) {
            return Map.of("message", "Bạn không còn lượt chơi nào!");
        }

        currentUser.setTurns(currentUser.getTurns() - 1);

        // Áp dụng xác suất thắng của người chơi là 5%
        int serverNumber = random.nextInt(MAX_SERVER_NUMBER) + 1;
        boolean isUserWin = random.nextDouble() < WIN_PROBABILITY || number == serverNumber;
        if (isUserWin) {
            currentUser.setScore(currentUser.getScore() + POINTS_PER_WIN);
        }

        userRepository.save(currentUser);
        String message = isUserWin
                ? String.format("Chúc mừng! Bạn đã đoán đúng số: %d, bạn được cộng %d điểm.", serverNumber, POINTS_PER_WIN)
                : String.format("Rất tiếc! Bạn đã đoán sai số: %d, số đúng là %d", number, serverNumber);

        return Map.of(
                "message", message,
                "serverNumber", serverNumber,
                "score", currentUser.getScore(),
                "turns", currentUser.getTurns()
        );
    }

    /**
     * Mua thêm lượt chơi cho người dùng hiện tại.
     *
     * @param userDetails Thông tin người dùng đã đăng nhập.
     * @return Map<String, Object>
     * Thông báo thành công và số lượt chơi còn lại.
     */
    @PostMapping("/buy-turns")
    public Map<String, Object> buyAdditionalTurns(@AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = findUserByUserDetails(userDetails);

        currentUser.setTurns(currentUser.getTurns() + TURNS_TO_BUY);
        userRepository.save(currentUser);

        return Map.of("message", "Bạn đã mua thành công " + TURNS_TO_BUY + " lượt chơi.",
                "turnsLeft", currentUser.getTurns());
    }

    /**
     * Lấy danh sách người dùng hàng đầu theo điểm số.
     *
     * @return List<UserLeaderboardDTO> Danh sách người dùng với tên và điểm số, sắp xếp theo điểm số giảm dần.
     */
    @GetMapping("/leaderboard")
    public List<UserLeaderboardDTO> getLeaderboard() {
        return userRepository.findTop10ByOrderByScoreDesc()
                .stream()
                .map(user -> new UserLeaderboardDTO(user.getUsername(), user.getScore()))
                .toList();
    }

    /**
     * Tìm người dùng theo thông tin đăng nhập.
     *
     * @param userDetails Thông tin người dùng đã đăng nhập.
     * @return User
     */
    private User findUserByUserDetails(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tìm thấy"));
    }

    /**
     * Tìm người dùng theo thông tin đăng nhập với khóa.
     *
     * @param userDetails Thông tin người dùng đã đăng nhập.
     * @return User
     */
    private User findUserByUserDetailsWithLock(UserDetails userDetails) {
        return userRepository.findByUsernameForUpdate(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Người dùng không tìm thấy"));
    }
}
