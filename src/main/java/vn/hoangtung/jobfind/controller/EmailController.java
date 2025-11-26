package vn.hoangtung.jobfind.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.hoangtung.jobfind.domain.response.RestResponse;
import vn.hoangtung.jobfind.domain.response.email.ResEmailJob;
import vn.hoangtung.jobfind.service.EmailService;
import vn.hoangtung.jobfind.service.SubscriberService;
import vn.hoangtung.jobfind.util.annotation.ApiMessage;

@RestController
@RequestMapping("/api/v1")
public class EmailController {

    private final EmailService emailService;
    private final SubscriberService subscriberService;

    public EmailController(EmailService emailService, SubscriberService subscriberService) {
        this.emailService = emailService;
        this.subscriberService = subscriberService;
    }

    @GetMapping("/email")
    @ApiMessage("Send simple email")
    public RestResponse<String> sendSimpleEmail() {
        String username = "Tung Hoang";

        // --- TẠO DỮ LIỆU GIẢ (MOCK DATA) ---
        ResEmailJob job1 = new ResEmailJob();
        job1.setName("Senior Java Developer");
        job1.setSalary(2500.0);

        job1.setCompany(new ResEmailJob.CompanyEmail("Google Vietnam"));
        job1.setSkills(List.of(new ResEmailJob.SkillEmail("Java"), new ResEmailJob.SkillEmail("Spring")));

        ResEmailJob job2 = new ResEmailJob();
        job2.setName("React Frontend Dev");
        job2.setSalary(1800.0);

        job2.setCompany(new ResEmailJob.CompanyEmail("Facebook"));
        job2.setSkills(List.of(new ResEmailJob.SkillEmail("React"), new ResEmailJob.SkillEmail("TypeScript")));

        // Truyền List Object
        Object value = List.of(job1, job2);

        this.emailService.sendEmailFromTemplateSync("tungthcstt@gmail.com", "Test Email Template", "job", username,
                value);

        // Logic gửi subscriber thật (nếu cần test thì giữ lại, không thì comment)
        // this.subscriberService.sendSubscribersEmailJobs();

        // Trả về RestResponse để tránh lỗi ClassCastException
        RestResponse<String> res = new RestResponse<>();
        res.setData("Email sent successfully");
        return res;
    }
}