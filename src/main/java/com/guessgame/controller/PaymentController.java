package com.guessgame.controller;

import com.guessgame.entity.User;
import com.guessgame.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payment/momo")
@RequiredArgsConstructor
public class PaymentController {
    private final UserRepository userRepository;

    private static final int TURNS_TO_BUY = 5;

    /**
     * Khởi tạo thanh toán MOMO (giả lập).
     *
     * @param userDetails Thông tin người dùng đã đăng nhập.
     * @return Map chứa thông tin đơn hàng và link thanh toán giả lập.
     */
    @PostMapping("/create")
    public Map<String, Object> createPayment(@AuthenticationPrincipal UserDetails userDetails) {
        return Map.of(
                "orderId", System.currentTimeMillis(),
                "payUrl", "https://momo.vn/fake-payment?orderId=123456", // link giả lập!
                "message", "Vui lòng bấm 'Xác nhận thanh toán' để mua lượt!"
        );
    }

    /**
     * Xác nhận thanh toán MOMO (giả lập).
     *
     * @param userDetails Thông tin người dùng đã đăng nhập.
     * @param orderId     ID đơn hàng (giả lập).
     * @return Map chứa thông báo thành công và số lượt chơi còn lại.
     */
    @PostMapping("/confirm")
    public Map<String, Object> confirmPayment(@AuthenticationPrincipal UserDetails userDetails,
                                              @RequestParam Long orderId) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow();
        currentUser.setTurns(currentUser.getTurns() + TURNS_TO_BUY);
        userRepository.save(currentUser);

        return Map.of(
                "message", "Bạn đã mua thành công " + TURNS_TO_BUY + " lượt chơi bằng MOMO (giả lập)." + "Tổng số lượt chơi hiện tại: " + currentUser.getTurns(),
                "turnsLeft", currentUser.getTurns()
        );
    }

}
