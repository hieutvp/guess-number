package com.guessgame.controller;

import com.guessgame.dto.GuessRequest;
import com.guessgame.dto.UserLeaderboardDTO;
import com.guessgame.entity.User;
import com.guessgame.exception.GuessNumberException;
import com.guessgame.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.*;

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
     * Xử lý dự đoán số của người dùng.
     *
     * @param userDetails Thông tin người dùng đã đăng nhập.
     * @param request     Chứa số dự đoán của người dùng.
     * @return ResponseEntity chứa thông báo kết quả dự đoán, số máy chủ đã chọn, điểm số và lượt chơi còn lại.
     * Nếu số dự đoán không hợp lệ (không nằm trong khoảng từ 1 đến 5) hoặc người dùng không còn lượt chơi, ném ra GuessNumberException.
     * @throws GuessNumberException nếu số dự đoán không hợp lệ hoặc người dùng không còn lượt chơi.
     *                              * @Transactional để đảm bảo tính nhất quán trong việc cập nhật lượt chơi và điểm số của người dùng.
     */
    @Transactional
    @PostMapping("/guess")
    public ResponseEntity<?> guess(@AuthenticationPrincipal UserDetails userDetails,
                                   @RequestBody GuessRequest request) {
        User currentUser = findUserByUserDetailsWithLock(userDetails);
        int turnsLeft = currentUser.getTurns();
        if (turnsLeft <= 0) {
            throw new GuessNumberException("Bạn không còn lượt chơi nào! Vui lòng mua thêm lượt chơi.");
        }

        int number = request.getNumber();
        if (number < 1 || number > 5) {
            throw new GuessNumberException("Số dự đoán phải nằm trong khoảng từ 1 đến 5!");
        }

        currentUser.setTurns(currentUser.getTurns() - 1);

        // Máy chủ chọn một số ngẫu nhiên từ 1 đến MAX_SERVER_NUMBER
        int serverNumber = random.nextInt(MAX_SERVER_NUMBER) + 1;
        // Người chơi thắng nếu số dự đoán trùng với số máy chủ hoặc theo xác suất thắng
        boolean isUserWin = random.nextDouble() < WIN_PROBABILITY || number == serverNumber;
        if (isUserWin) {
            currentUser.setScore(currentUser.getScore() + POINTS_PER_WIN);
        }

        userRepository.save(currentUser);
        String message = isUserWin
                ? String.format("Chúc mừng! Bạn đã đoán đúng số: %d, bạn được cộng %d điểm.", serverNumber, POINTS_PER_WIN)
                : String.format("Rất tiếc! Bạn đã đoán sai số: %d, số đúng là %d", number, serverNumber);

        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        response.put("serverNumber", serverNumber);
        response.put("score", currentUser.getScore());
        response.put("turns", currentUser.getTurns());

        return ResponseEntity.ok(response);
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
