package studyMate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import studyMate.dto.ApiResponse;
import studyMate.dto.pomodoro.TimerReqDto;
import studyMate.dto.pomodoro.TimerResDto;
import studyMate.entity.User;
import studyMate.service.TimerService;

@Controller
@RequiredArgsConstructor
public class TimerWebSocketController {

    private final TimerService timerService;

    @MessageMapping("/timer/start")
    @SendTo("/topic/timer")
    public TimerResDto startTimer(TimerReqDto dto, SimpMessageHeaderAccessor headerAccessor) {
        User user = (User) headerAccessor.getSessionAttributes().get("user");
        if (user == null) {
            return TimerResDto.builder()
                    .success(false)
                    .message("인증 정보가 없습니다.")
                    .build();
        }
        
        return timerService.startTimer(user, dto);
    }

    @MessageMapping("/timer/stop")
    @SendTo("/topic/timer")
    public TimerResDto stopTimer(SimpMessageHeaderAccessor headerAccessor) {
        User user = (User) headerAccessor.getSessionAttributes().get("user");
        if (user == null) {
            return TimerResDto.builder()
                    .success(false)
                    .message("인증 정보가 없습니다.")
                    .build();
        }
        
        return timerService.stopTimer(user);
    }

    @MessageMapping("/timer/pause")
    @SendTo("/topic/timer")
    public TimerResDto pauseTimer(SimpMessageHeaderAccessor headerAccessor) {
        User user = (User) headerAccessor.getSessionAttributes().get("user");
        if (user == null) {
            return TimerResDto.builder()
                    .success(false)
                    .message("인증 정보가 없습니다.")
                    .build();
        }
        
        return timerService.pauseTimer(user);
    }

    @MessageMapping("/timer/switch")
    @SendTo("/topic/timer")
    public TimerResDto switchTimer(SimpMessageHeaderAccessor headerAccessor) {
        User user = (User) headerAccessor.getSessionAttributes().get("user");
        if (user == null) {
            return TimerResDto.builder()
                    .success(false)
                    .message("인증 정보가 없습니다.")
                    .build();
        }
        
        return timerService.switchTimer(user);
    }
} 